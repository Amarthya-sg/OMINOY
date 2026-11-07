package come.asg.lemudroid.app.mobile.feature.settings.retroachievements

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** Credentials used only by the native login2 gameplay client. */
data class RetroAchievementsSessionCredentials(
    val username: String,
    val token: String,
)

/** Credentials used only by OMINOY's documented Web API synchronization layer. */
data class RetroAchievementsWebApiCredentials(
    val username: String,
    val apiKey: String,
)

/**
 * Narrow in-memory bridge from the encrypted account screen to an active game session.
 * The token is cleared as soon as the account logs out or expires.
 */
object RetroAchievementsAccountBridge {
    private val _credentials = MutableStateFlow<RetroAchievementsSessionCredentials?>(null)
    val credentials: StateFlow<RetroAchievementsSessionCredentials?> = _credentials.asStateFlow()

    private val _webApiCredentials = MutableStateFlow<RetroAchievementsWebApiCredentials?>(null)
    val webApiCredentials: StateFlow<RetroAchievementsWebApiCredentials?> = _webApiCredentials.asStateFlow()

    fun set(credentials: RetroAchievementsSessionCredentials) {
        _credentials.value = credentials
    }

    fun setWebApiCredentials(credentials: RetroAchievementsWebApiCredentials?) {
        _webApiCredentials.value = credentials
    }

    fun clear() {
        _credentials.value = null
        _webApiCredentials.value = null
    }
}
