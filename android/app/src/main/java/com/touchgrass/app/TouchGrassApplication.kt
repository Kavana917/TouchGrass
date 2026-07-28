package com.touchgrass.app

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

/**
 * Application entry point.
 *
 * [HiltAndroidApp] triggers Hilt's code generation and creates the
 * application-level dependency container that everything else hangs off.
 *
 * Also supplies WorkManager's configuration so the watchdog worker can have
 * dependencies injected. This requires the default WorkManager initialiser
 * to be removed in the manifest — otherwise WorkManager initialises itself
 * first with the default factory and the injected worker fails to construct.
 */
@HiltAndroidApp
class TouchGrassApplication : Application(), Configuration.Provider {

    @Inject lateinit var workerFactory: HiltWorkerFactory

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()
}
