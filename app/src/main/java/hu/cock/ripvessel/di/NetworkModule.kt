package hu.cock.ripvessel.di

import android.content.Context
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import hu.cock.ripvessel.network.createAuthenticatedClient
import hu.gyulakiri.ripvessel.api.AuthV2Api
import hu.gyulakiri.ripvessel.api.ContentV3Api
import hu.gyulakiri.ripvessel.api.CreatorV3Api
import hu.gyulakiri.ripvessel.api.DeliveryV3Api
import hu.gyulakiri.ripvessel.api.SubscriptionsV3Api
import okhttp3.OkHttpClient
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    @Provides
    @Singleton
    fun provideOkHttpClient(@ApplicationContext context: Context): OkHttpClient {
        return createAuthenticatedClient(context)
    }

    @Provides
    @Singleton
    fun provideAuthV2Api(client: OkHttpClient): AuthV2Api {
        return AuthV2Api(client = client)
    }

    @Provides
    @Singleton
    fun provideContentV3Api(client: OkHttpClient): ContentV3Api {
        return ContentV3Api(client = client)
    }

    @Provides
    @Singleton
    fun provideCreatorV3Api(client: OkHttpClient): CreatorV3Api {
        return CreatorV3Api(client = client)
    }

    @Provides
    @Singleton
    fun provideSubscriptionsV3Api(client: OkHttpClient): SubscriptionsV3Api {
        return SubscriptionsV3Api(client = client)
    }

    @Provides
    @Singleton
    fun provideDeliveryV3Api(client: OkHttpClient): DeliveryV3Api {
        return DeliveryV3Api(client = client)
    }
} 