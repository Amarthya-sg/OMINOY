/*
 * RetrogradeApplicationComponent.kt
 *
 * Copyright (C) 2017 Retrograde Project
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package come.asg.lemudroid.app

import come.asg.lemudroid.app.shared.library.CoreUpdateWork
import come.asg.lemudroid.app.shared.library.LibraryIndexWork
import come.asg.lemudroid.app.shared.game.RetroAchievementsPendingUploadWork
import come.asg.lemudroid.app.shared.game.RetroAchievementsSyncWork
import come.asg.lemudroid.app.shared.savesync.SaveSyncWork
import come.asg.lemudroid.app.shared.storage.cache.CacheCleanerWork
import come.asg.lemudroid.app.tv.LemuroidTVApplicationModule
import come.asg.lemudroid.app.tv.channel.ChannelUpdateWork
import come.asg.lemudroid.lib.injection.AndroidWorkerInjectionModule
import come.asg.lemudroid.lib.injection.PerApp
import dagger.Component
import dagger.android.AndroidInjector
import dagger.android.support.AndroidSupportInjectionModule

@Component(
    modules = [
        AndroidSupportInjectionModule::class,
        AndroidWorkerInjectionModule::class,
        LemuroidApplicationModule::class,
        LibraryIndexWork.Module::class,
        RetroAchievementsPendingUploadWork.Module::class,
        RetroAchievementsSyncWork.Module::class,
        SaveSyncWork.Module::class,
        ChannelUpdateWork.Module::class,
        CoreUpdateWork.Module::class,
        CacheCleanerWork.Module::class,
        LemuroidTVApplicationModule::class,
    ],
)
@PerApp
interface LemuroidApplicationComponent : AndroidInjector<LemuroidApplication> {
    @Component.Builder
    abstract class Builder : AndroidInjector.Builder<LemuroidApplication>()
}
