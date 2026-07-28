package com.touchgrass.app

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

/**
 * Application entry point.
 *
 * [HiltAndroidApp] triggers Hilt's code generation and creates the
 * application-level dependency container that everything else hangs off.
 */
@HiltAndroidApp
class TouchGrassApplication : Application()
