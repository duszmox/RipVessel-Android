package hu.cock.ripvessel.di

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ViewModelComponent
import dagger.hilt.android.scopes.ViewModelScoped

@Module
@InstallIn(ViewModelComponent::class)
object ViewModelModule {
    @Provides
    @ViewModelScoped
    fun providePostId(): String {
        // This will be overridden by the SavedStateHandle
        return ""
    }

    @Provides
    @ViewModelScoped
    fun provideCreatorId(): String {
        // This will be overridden by the SavedStateHandle
        return ""
    }

    @Provides
    @ViewModelScoped
    fun provideChannelId(): String? {
        // This will be overridden by the SavedStateHandle
        return null
    }
} 