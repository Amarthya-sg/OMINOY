package come.asg.lemudroid.app.shared.game

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.ListenableWorker
import androidx.work.NetworkType
import androidx.work.Constraints
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import come.asg.lemudroid.app.mobile.feature.settings.retroachievements.readWebApiCredentials
import come.asg.lemudroid.app.mobile.shared.NotificationsManager
import come.asg.lemudroid.app.utils.android.createSyncForegroundInfo
import come.asg.lemudroid.lib.injection.AndroidWorkerInjection
import come.asg.lemudroid.lib.injection.WorkerKey
import come.asg.lemudroid.lib.library.db.RetrogradeDatabase
import dagger.Binds
import dagger.android.AndroidInjector
import dagger.multibindings.IntoMap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import timber.log.Timber
import javax.inject.Inject

class RetroAchievementsSyncWork(context: Context, workerParams: WorkerParameters) :
    CoroutineWorker(context, workerParams) {
    @Inject lateinit var retrogradeDb: RetrogradeDatabase

    override suspend fun doWork(): Result {
        AndroidWorkerInjection.inject(this)
        val credentials = readWebApiCredentials(applicationContext) ?: return Result.success()
        val notifications = NotificationsManager(applicationContext)
        setForeground(createSyncForegroundInfo(
            NotificationsManager.RETRO_ACHIEVEMENTS_NOTIFICATION_ID,
            notifications.retroAchievementsSyncNotification(),
        ))

        val games = withContext(Dispatchers.IO) {
            retrogradeDb.gameDao().selectAll().filter { it.systemId in SUPPORTED_SYSTEMS }
        }
        var completed = 0
        var synced = 0
        var skipped = 0
        val attempts = applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

        games.forEach { game ->
            if (isStopped) return Result.success()
            notifications.showRetroAchievementsNotification(
                notifications.retroAchievementsSyncNotification(
                    completed = completed,
                    total = games.size,
                    current = "Preparing ${game.displayName()}",
                ),
            )
            val attemptKey = "${game.id}:${game.fileUri}"
            if (attempts.getInt(attemptKey, 0) >= MAX_ATTEMPTS) {
                completed++
                skipped++
                notifications.showRetroAchievementsNotification(
                    notifications.retroAchievementsSyncNotification(completed, games.size, game.displayName()),
                )
                return@forEach
            }
            var result: RetroAchievementsEarnedStore.LocalGameSyncResult? = null
            repeat(MAX_ATTEMPTS - attempts.getInt(attemptKey, 0)) { attempt ->
                if (result?.found == true) return@repeat
                result = runCatching {
                    RetroAchievementsEarnedStore.syncLocalGame(
                        context = applicationContext,
                        libraryGameId = game.id,
                        fileUri = game.fileUri,
                        systemId = game.systemId,
                        gameTitle = game.displayName(),
                        username = credentials.username,
                        apiKey = credentials.apiKey,
                        onArtworkProgress = { artworkCompleted, artworkTotal ->
                            notifications.showRetroAchievementsNotification(
                                notifications.retroAchievementsSyncNotification(
                                    completed = completed,
                                    total = games.size,
                                    current = game.displayName(),
                                    artworkCompleted = artworkCompleted,
                                    artworkTotal = artworkTotal,
                                ),
                            )
                        },
                    )
                }.getOrElse { error ->
                    Timber.w(error, "RetroAchievements sync failed for ${game.displayName()}")
                    RetroAchievementsEarnedStore.LocalGameSyncResult(false, error = error.javaClass.simpleName)
                }
                if (result?.found == false && result?.error == "not_found") {
                    attempts.edit().putInt(attemptKey, attempts.getInt(attemptKey, 0) + 1 + attempt).apply()
                } else if (result?.found == true) {
                    attempts.edit().remove(attemptKey).apply()
                }
            }
            completed++
            if (result?.found == true && result?.sync?.success == true) synced++ else skipped++
            notifications.showRetroAchievementsNotification(
                notifications.retroAchievementsSyncNotification(completed, games.size, game.displayName()),
            )
        }
        notifications.showRetroAchievementsNotification(notifications.retroAchievementsCompleteNotification(synced, skipped))
        RetroAchievementsEarnedStore.exportToSelectedDataFolder(applicationContext)
        return Result.success()
    }

    @dagger.Module(subcomponents = [Subcomponent::class])
    abstract class Module {
        @Binds @IntoMap @WorkerKey(RetroAchievementsSyncWork::class)
        abstract fun bindMyWorkerFactory(builder: Subcomponent.Builder): AndroidInjector.Factory<out ListenableWorker>
    }

    @dagger.Subcomponent
    interface Subcomponent : AndroidInjector<RetroAchievementsSyncWork> {
        @dagger.Subcomponent.Builder
        abstract class Builder : AndroidInjector.Builder<RetroAchievementsSyncWork>()
    }

    companion object {
        const val UNIQUE_WORK_ID = "RetroAchievementsSyncWork"
        private const val PREFS = "retro_achievements_sync_attempts"
        private const val MAX_ATTEMPTS = 3
        private val SUPPORTED_SYSTEMS = setOf("gb", "gbc", "gba")

        fun enqueue(context: Context) {
            val request = OneTimeWorkRequestBuilder<RetroAchievementsSyncWork>()
                .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
                .build()
            WorkManager.getInstance(context.applicationContext).enqueueUniqueWork(
                UNIQUE_WORK_ID,
                ExistingWorkPolicy.REPLACE,
                request,
            )
        }

        fun cancel(context: Context) {
            WorkManager.getInstance(context.applicationContext).cancelUniqueWork(UNIQUE_WORK_ID)
        }
    }
}
