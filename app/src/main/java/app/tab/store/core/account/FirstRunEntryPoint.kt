package app.tab.store.core.account

import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

/** Lets a composable reach [FirstRunStore] without its own view model. */
@EntryPoint
@InstallIn(SingletonComponent::class)
interface FirstRunEntryPoint {
    fun firstRun(): FirstRunStore
}
