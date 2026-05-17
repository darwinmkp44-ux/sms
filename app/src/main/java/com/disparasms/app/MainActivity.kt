package com.disparasms.app

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.disparasms.app.ui.navigation.AppNavigation
import com.disparasms.app.ui.screen.onboarding.OnboardingScreen
import com.disparasms.app.ui.screen.permissions.PermissionScreen
import com.disparasms.app.ui.screen.splash.SplashScreen
import com.disparasms.app.ui.theme.DisparaSMSTheme
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private const val PREFS_NAME = "disparasms_prefs"
private const val KEY_ONBOARDING_DONE = "onboarding_done"

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        val prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

        setContent {
            DisparaSMSTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    var splashDone by remember { mutableStateOf(false) }
                    var onboardingDone by remember {
                        mutableStateOf(prefs.getBoolean(KEY_ONBOARDING_DONE, false))
                    }

                    if (!splashDone) {
                        SplashScreen()
                        androidx.compose.runtime.LaunchedEffect(Unit) {
                            delay(2000)
                            splashDone = true
                        }
                    } else if (!onboardingDone) {
                        OnboardingScreen(
                            onComplete = {
                                prefs.edit().putBoolean(KEY_ONBOARDING_DONE, true).apply()
                                onboardingDone = true
                            }
                        )
                    } else {
                        var permissionsDone by remember { mutableStateOf(false) }
                        if (!permissionsDone) {
                            PermissionScreen(
                                onAllGranted = { permissionsDone = true }
                            )
                        } else {
                            AppNavigation()
                        }
                    }
                }
            }
        }
    }
}
