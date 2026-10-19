package com.gyminit

import android.app.Activity
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

class MainActivity : ComponentActivity() {
    private val vm: AppViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent { GymApp(vm) }
    }

    override fun onResume() {
        super.onResume()
        vm.onAppResumed()
    }
}

@Composable
fun GymApp(vm: AppViewModel) {
    val theme = THEMES[vm.themeIdx]
    val view = LocalView.current

    SideEffect {
        val window = (view.context as? Activity)?.window
        if (window != null) {
            val controller = WindowCompat.getInsetsController(window, view)
            controller.isAppearanceLightStatusBars = !theme.dark
            controller.isAppearanceLightNavigationBars = !theme.dark
        }
    }

    BackHandler(enabled = vm.screen != Screen.Home || vm.pickerOpen) {
        if (vm.pickerOpen) vm.pickerOpen = false else vm.back()
    }

    CompositionLocalProvider(LocalTheme provides theme) {
        Box(
            Modifier
                .fillMaxSize()
                .background(theme.bg)
                .statusBarsPadding()
                .navigationBarsPadding()
                .imePadding(),
        ) {
            when (vm.screen) {
                Screen.Workout -> WorkoutScreen(vm)
                Screen.Home -> HomeScreen(vm)
                Screen.Stopwatch -> StopwatchScreen(vm)
                Screen.Timer -> TimerScreen(vm)
            }
        }
    }
}
