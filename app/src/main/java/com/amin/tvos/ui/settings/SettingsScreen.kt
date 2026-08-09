package com.amin.tvos.ui.settings

import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.amin.tvos.BuildConfig
import com.amin.tvos.data.model.UserAgentMode
import com.amin.tvos.ui.components.FocusableCard
import com.amin.tvos.ui.home.UpdateBanner
import com.amin.tvos.ui.theme.CinemaRed
import com.amin.tvos.ui.theme.TextSecondary
import com.amin.tvos.update.UpdateState

@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    viewModel: SettingsViewModel = viewModel()
) {
    val context = LocalContext.current
    val services by viewModel.services.collectAsState()
    val uaMode by viewModel.userAgentMode.collectAsState()
    val updateState by viewModel.updateState.collectAsState()
    var showAddDialog by remember { mutableStateOf(false) }

    fun toast(msg: String) = Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 48.dp, vertical = 28.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            FocusableCard(onClick = onBack) {
                Icon(
                    Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    modifier = Modifier.padding(12.dp)
                )
            }
            Spacer(Modifier.width(20.dp))
            Text("Settings", style = MaterialTheme.typography.displayMedium)
        }
        Spacer(Modifier.height(28.dp))

        // ---------- Manage services ----------
        Text("Manage Services", style = MaterialTheme.typography.headlineMedium)
        Text(
            "Services live in services.json — add here or edit the file directly.",
            color = TextSecondary
        )
        Spacer(Modifier.height(12.dp))
        services.forEach { service ->
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp)
            ) {
                Column(Modifier.weight(1f)) {
                    Text(service.name, style = MaterialTheme.typography.titleLarge)
                    Text(service.url, color = TextSecondary, style = MaterialTheme.typography.bodyMedium)
                }
                FocusableCard(onClick = { viewModel.removeService(service.id) }) {
                    Icon(
                        Icons.Filled.Delete,
                        contentDescription = "Remove ${service.name}",
                        tint = CinemaRed,
                        modifier = Modifier.padding(12.dp)
                    )
                }
            }
        }
        Spacer(Modifier.height(8.dp))
        FocusableCard(onClick = { showAddDialog = true }) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(horizontal = 18.dp, vertical = 12.dp)
            ) {
                Icon(Icons.Filled.Add, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Add Service")
            }
        }

        Spacer(Modifier.height(32.dp))

        // ---------- User Agent ----------
        Text("Browser User Agent", style = MaterialTheme.typography.headlineMedium)
        Spacer(Modifier.height(12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            UserAgentMode.entries.forEach { mode ->
                FocusableCard(onClick = { viewModel.setUserAgent(mode) }) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 18.dp, vertical = 12.dp)
                    ) {
                        if (mode == uaMode) {
                            Icon(Icons.Filled.Check, contentDescription = null, tint = CinemaRed)
                            Spacer(Modifier.width(8.dp))
                        }
                        Text(mode.label)
                    }
                }
            }
        }

        Spacer(Modifier.height(32.dp))

        // ---------- Login / QR zoom ----------
        val zoom by viewModel.browserZoom.collectAsState()
        Text("Login / QR Zoom", style = MaterialTheme.typography.headlineMedium)
        Text(
            "Only login and QR pages use this scale. Catalog and video pages stay at 100%.",
            color = TextSecondary
        )
        Spacer(Modifier.height(12.dp))
        Row(
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            FocusableCard(onClick = { viewModel.changeBrowserZoom(-5) }) {
                Text("−", style = MaterialTheme.typography.headlineMedium,
                    modifier = Modifier.padding(horizontal = 22.dp, vertical = 8.dp))
            }
            Text(
                "$zoom%",
                style = MaterialTheme.typography.headlineMedium,
                color = CinemaRed
            )
            FocusableCard(onClick = { viewModel.changeBrowserZoom(+5) }) {
                Text("+", style = MaterialTheme.typography.headlineMedium,
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp))
            }
            FocusableCard(onClick = { viewModel.resetBrowserZoom() }) {
                Text("Reset (85%)", Modifier.padding(horizontal = 18.dp, vertical = 12.dp))
            }
        }

        Spacer(Modifier.height(32.dp))

        // ---------- Startup intro ----------
        val playIntro by viewModel.playIntro.collectAsState()
        val muteIntro by viewModel.muteIntro.collectAsState()
        Text("Startup Intro", style = MaterialTheme.typography.headlineMedium)
        Text(
            "The Aminema intro plays once per cold start. OK, Back or a mouse click skips it.",
            color = TextSecondary
        )
        Spacer(Modifier.height(12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            FocusableCard(onClick = { viewModel.setPlayIntro(!playIntro) }) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 18.dp, vertical = 12.dp)
                ) {
                    if (playIntro) {
                        Icon(Icons.Filled.Check, contentDescription = null, tint = CinemaRed)
                        Spacer(Modifier.width(8.dp))
                    }
                    Text("Play intro")
                }
            }
            FocusableCard(onClick = { viewModel.setMuteIntro(!muteIntro) }) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 18.dp, vertical = 12.dp)
                ) {
                    if (muteIntro) {
                        Icon(Icons.Filled.Check, contentDescription = null, tint = CinemaRed)
                        Spacer(Modifier.width(8.dp))
                    }
                    Text("Mute intro")
                }
            }
        }

        Spacer(Modifier.height(32.dp))

        // ---------- Privacy / storage ----------
        Text("Privacy & Storage", style = MaterialTheme.typography.headlineMedium)
        Spacer(Modifier.height(12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            FocusableCard(onClick = { viewModel.clearCookies { toast("Cookies cleared — you are logged out") } }) {
                Text("Clear Cookies / Logout", Modifier.padding(horizontal = 18.dp, vertical = 12.dp))
            }
            FocusableCard(onClick = { viewModel.clearCache { toast("Cache cleared") } }) {
                Text("Clear Cache", Modifier.padding(horizontal = 18.dp, vertical = 12.dp))
            }
            FocusableCard(onClick = { viewModel.clearHistory(); toast("History cleared") }) {
                Text("Clear History", Modifier.padding(horizontal = 18.dp, vertical = 12.dp))
            }
        }

        Spacer(Modifier.height(32.dp))

        // ---------- About ----------
        Text("About", style = MaterialTheme.typography.headlineMedium)
        Spacer(Modifier.height(8.dp))
        Text("Aminema  •  v${BuildConfig.VERSION_NAME}", style = MaterialTheme.typography.titleMedium)
        Text(
            "A personal entertainment dashboard. Not a streaming service — it only opens " +
                "your own subscribed websites in an optimized TV browser. No content is " +
                "hosted, scraped, or redistributed.",
            color = TextSecondary
        )
        Spacer(Modifier.height(16.dp))
        FocusableCard(onClick = {
            viewModel.checkForUpdate { release ->
                // A found release is rendered by the shared UpdateState directly below.
                if (release == null) {
                    toast("شما آخرین نسخه را دارید")
                }
            }
        }) {
            Text("بررسی بروزرسانی", Modifier.padding(horizontal = 18.dp, vertical = 12.dp))
        }
        if (updateState is UpdateState.Available ||
            updateState is UpdateState.Downloading ||
            updateState is UpdateState.Failed
        ) {
            Spacer(Modifier.height(14.dp))
            UpdateBanner(
                state = updateState,
                onInstall = viewModel::downloadAndInstall,
                onSkip = viewModel::skipUpdate,
                modifier = Modifier.fillMaxWidth()
            )
        }
        Spacer(Modifier.height(32.dp))
    }

    if (showAddDialog) {
        var name by remember { mutableStateOf("") }
        var url by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            title = { Text("Add Service") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = name, onValueChange = { name = it },
                        label = { Text("Name") }, singleLine = true
                    )
                    OutlinedTextField(
                        value = url, onValueChange = { url = it },
                        label = { Text("URL (https://…)") }, singleLine = true
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.addService(name, url)
                    showAddDialog = false
                }) { Text("Add", color = CinemaRed) }
            },
            dismissButton = {
                TextButton(onClick = { showAddDialog = false }) { Text("Cancel") }
            }
        )
    }
}
