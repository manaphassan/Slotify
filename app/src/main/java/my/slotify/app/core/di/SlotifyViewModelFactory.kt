package my.slotify.app.core.di

import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewmodel.CreationExtras
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import my.slotify.app.MainViewModel
import my.slotify.app.SlotifyApplication
import my.slotify.app.feature.dashboard.DashboardViewModel
import my.slotify.app.feature.finance.FinanceViewModel
import my.slotify.app.feature.logistics.LogisticsViewModel
import my.slotify.app.feature.profile.ProfileViewModel
import my.slotify.app.feature.rewards.RewardsViewModel
import my.slotify.app.feature.timetable.TimetableViewModel

private val CreationExtras.container: AppContainer
    get() = (this[APPLICATION_KEY] as SlotifyApplication).container

/** Creates every Slotify ViewModel from the [AppContainer]. */
val SlotifyViewModelFactory: ViewModelProvider.Factory = viewModelFactory {
    initializer { MainViewModel(container.settings, container.students) }
    initializer {
        DashboardViewModel(container.appContext, container.students, container.schoolwork, container.rewards)
    }
    initializer { TimetableViewModel(container.students, createSavedStateHandle()) }
    initializer { RewardsViewModel(container.students, container.rewards) }
    initializer { FinanceViewModel(container.students, container.finance, container.rewards) }
    initializer { ProfileViewModel(container.students, container.schoolwork) }
    initializer { LogisticsViewModel(container.appContext, createSavedStateHandle()) }
}
