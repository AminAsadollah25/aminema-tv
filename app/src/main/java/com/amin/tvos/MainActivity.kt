package com.amin.tvos

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.amin.tvos.ui.home.HomeScreen
import com.amin.tvos.ui.home.HomeViewModel
import com.amin.tvos.ui.settings.SettingsScreen
import com.amin.tvos.ui.theme.AminTvTheme
import com.amin.tvos.ui.theme.Ink
import com.amin.tvos.ui.theme.TextPrimary

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            AminTvTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = Ink,
                    contentColor = TextPrimary
                ) {
                    val navController = rememberNavController()
                    NavHost(
                        navController = navController,
                        startDestination = "home",
                        modifier = Modifier.fillMaxSize().background(Ink)
                    ) {
                        composable("home") {
                            val vm: HomeViewModel = viewModel()
                            // Refresh library when returning from the browser
                            val lifecycleOwner = LocalLifecycleOwner.current
                            DisposableEffect(lifecycleOwner) {
                                val observer = LifecycleEventObserver { _, event ->
                                    if (event == Lifecycle.Event.ON_RESUME) vm.refresh()
                                }
                                lifecycleOwner.lifecycle.addObserver(observer)
                                onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
                            }
                            HomeScreen(
                                onOpenSettings = { navController.navigate("settings") },
                                viewModel = vm
                            )
                        }
                        composable("settings") {
                            SettingsScreen(onBack = { navController.popBackStack() })
                        }
                    }
                }
            }
        }
    }
}
