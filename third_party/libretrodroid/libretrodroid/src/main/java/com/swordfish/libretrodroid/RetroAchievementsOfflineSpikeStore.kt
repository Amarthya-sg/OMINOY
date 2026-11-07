package com.swordfish.libretrodroid

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import java.io.File
import java.nio.charset.StandardCharsets
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import java.security.MessageDigest

/**
 * Test-only persistence for the offline RetroAchievements feasibility spike.
 * The file is private to the app and encrypted with an Android Keystore AES key.
 * No token, password, or raw request URL is written to the inventory log.
 */
internal class RetroAchievementsOfflineSpikeStore(context: Context) {
    data class CachedResponse(val statusCode: Int, val body: String)

    private val file = File(context.applicationContext.filesDir, "ra_offline_spike.bin")
    private val keyAlias = "ominoy.ra.offline.spike.v1"
    private val lock = Any()
    private var cache: MutableMap<String, CachedResponse> = mutableMapOf()
    private var inventory: MutableList<String> = mutableListOf()
    private var unlocks: MutableList<String> = mutableListOf()
    private var loaded = false

    fun cachedResponse(key: String): CachedResponse? = synchronized(lock) {
        loadLocked()
        cache[hash(key)]
    }

    fun recordResponse(key: String, statusCode: Int, body: String) = synchronized(lock) {
        loadLocked()
        if (statusCode in 200..299 && body.length <= MAX_RESPONSE_CHARS) {
            cache[hash(key)] = CachedResponse(statusCode, body)
            persistLocked()
        }
    }

    fun recordRequest(method: String, url: String, postData: String?, responseSize: Int? = null) = synchronized(lock) {
        loadLocked()
        val safeUrl = redact(url)
        val safePost = postData?.let(::redact) ?: ""
        inventory += "method=$method url=$safeUrl post=$safePost response_size=${responseSize ?: ""}"
        if (inventory.size > MAX_INVENTORY) inventory = inventory.takeLast(MAX_INVENTORY).toMutableList()
        persistLocked()
    }

    fun recordUnlock(achievementId: Long, gameHash: String, unlockTimeMillis: Long) = synchronized(lock) {
        loadLocked()
        unlocks += "id=$achievementId hash=${hash(gameHash)} unlock_time_ms=$unlockTimeMillis mode=softcore state=pending"
        if (unlocks.size > MAX_UNLOCKS) unlocks = unlocks.takeLast(MAX_UNLOCKS).toMutableList()
        persistLocked()
    }

    fun inventorySnapshot(): List<String> = synchronized(lock) {
        loadLocked()
        inventory.toList()
    }

    fun pendingUnlockSnapshot(): List<String> = synchronized(lock) {
        loadLocked()
        unlocks.toList()
    }

    private fun loadLocked() {
        if (loaded) return
        loaded = true
        if (!file.isFile) return
        try {
            val clear = decrypt(file.readBytes()).toString(StandardCharsets.UTF_8)
            clear.lineSequence().forEach { line ->
                when {
                    line.startsWith("C|") -> {
                        val p = line.split('|', limit = 3)
                        if (p.size == 3) {
                            val encoded = p[2]
                            cache[p[1]] = CachedResponse(
                                encoded.substringBefore(':').toInt(),
                                String(Base64.decode(encoded.substringAfter(':'), Base64.NO_WRAP), StandardCharsets.UTF_8),
                            )
                        }
                    }
                    line.startsWith("I|") -> inventory += line.removePrefix("I|")
                    line.startsWith("U|") -> unlocks += line.removePrefix("U|")
                }
            }
        } catch (_: Exception) {
            cache.clear()
            inventory.clear()
            unlocks.clear()
        }
    }

    private fun persistLocked() {
        val clear = buildString {
            cache.forEach { (key, value) ->
                append("C|").append(key).append('|').append(value.statusCode).append(':')
                    .append(Base64.encodeToString(value.body.toByteArray(StandardCharsets.UTF_8), Base64.NO_WRAP))
                    .append('\n')
            }
            inventory.forEach { append("I|").append(it).append('\n') }
            unlocks.forEach { append("U|").append(it).append('\n') }
        }.toByteArray(StandardCharsets.UTF_8)
        val encrypted = encrypt(clear)
        val temp = File(file.parentFile, file.name + ".tmp")
        temp.writeBytes(encrypted)
        if (!temp.renameTo(file)) {
            file.delete()
            check(temp.renameTo(file)) { "Could not atomically persist RA offline spike data" }
        }
    }

    private fun redact(value: String): String = value
        .replace(Regex("(?i)(api_token|token|t|p|password|pass|v)=([^&\\s]+)"), "$1=<redacted>")
        .replace(Regex("(?i)(username|u)=([^&\\s]+)"), "$1=<redacted>")

    private fun hash(value: String): String = MessageDigest.getInstance("SHA-256")
        .digest(value.toByteArray(StandardCharsets.UTF_8))
        .joinToString("") { "%02x".format(it) }

    private fun key(): SecretKey {
        val store = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        val existing = store.getKey(keyAlias, null) as? SecretKey
        if (existing != null) return existing
        val generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore")
        generator.init(KeyGenParameterSpec.Builder(keyAlias, KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
            .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
            .build())
        return generator.generateKey()
    }

    private fun encrypt(clear: ByteArray): ByteArray {
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, key())
        return cipher.iv + cipher.doFinal(clear)
    }

    private fun decrypt(encrypted: ByteArray): ByteArray {
        require(encrypted.size > GCM_IV_LENGTH)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.DECRYPT_MODE, key(), GCMParameterSpec(128, encrypted.copyOfRange(0, GCM_IV_LENGTH)))
        return cipher.doFinal(encrypted.copyOfRange(GCM_IV_LENGTH, encrypted.size))
    }

    companion object {
        private const val GCM_IV_LENGTH = 12
        private const val MAX_RESPONSE_CHARS = 2_000_000
        private const val MAX_INVENTORY = 2_000
        private const val MAX_UNLOCKS = 500
    }
}
