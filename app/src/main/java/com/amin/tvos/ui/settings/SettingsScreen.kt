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
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
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
    var servicePendingRemoval by remember { mutableStateOf<com.amin.tvos.data.model.StreamingService?>(null) }
    var confirmLogout by remember { mutableStateOf(false) }
    var confirmClearHistory by remember { mutableStateOf(false) }

    fun toast(msg: String) = Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()

    androidx.compose.runtime.CompositionLocalProvider(
        LocalLayoutDirection provides LayoutDirection.Rtl
    ) {
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
                    contentDescription = "بازگشت",
                    modifier = Modifier.padding(12.dp)
                )
            }
            Spacer(Modifier.width(20.dp))
            Text("تنظیمات", style = MaterialTheme.typography.displayMedium)
        }
        Spacer(Modifier.height(28.dp))

        // ---------- Manage services ----------
        Text("مدیریت سرویس‌ها", style = MaterialTheme.typography.headlineMedium)
        Text(
            "سرویس‌های متصل به Aminema را اینجا مدیریت کنید.",
            color = TextSecondary
        )
        Spacer(Modifier.height(12.dp))
        services.forEach { service ->
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.fillMaxWidth(0.62f).padding(vertical = 6.dp)
            ) {
                Column(Modifier.weight(1f)) {
                    Text(service.name, style = MaterialTheme.typography.titleLarge)
                    androidx.compose.runtime.CompositionLocalProvider(
                        LocalLayoutDirection provides LayoutDirection.Ltr
                    ) {
                        Text(
                            service.url,
                            modifier = Modifier.fillMaxWidth(),
                            color = TextSecondary,
                            style = MaterialTheme.typography.bodyMedium,
                            textAlign = TextAlign.End
                        )
                    }
                }
                FocusableCard(onClick = { servicePendingRemoval = service }) {
                    Icon(
                        Icons.Filled.Delete,
                        contentDescription = "حذف ${service.name}",
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
                Text("افزودن سرویس")
            }
        }

        Spacer(Modifier.height(32.dp))

        // ---------- User Agent ----------
        Text("حالت مرورگر", style = MaterialTheme.typography.headlineMedium)
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
        Text("بزرگ‌نمایی ورود و QR", style = MaterialTheme.typography.headlineMedium)
        Text(
            "فقط صفحه‌های ورود و QR تغییر اندازه می‌دهند؛ فیلم و کاتالوگ روی ۱۰۰٪ می‌مانند.",
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
                Text("پیش‌فرض (۸۵٪)", Modifier.padding(horizontal = 18.dp, vertical = 12.dp))
            }
        }

        Spacer(Modifier.height(32.dp))

        // ---------- Startup intro ----------
        val playIntro by viewModel.playIntro.collectAsState()
        val muteIntro by viewModel.muteIntro.collectAsState()
        Text("اینتروی شروع", style = MaterialTheme.typography.headlineMedium)
        Text(
            "در هر اجرای تازه یک‌بار پخش می‌شود؛ با OK، بازگشت یا کلیک موس رد می‌شود.",
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
                    Text("پخش اینترو")
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
                    Text("پخش بی‌صدا")
                }
            }
        }

        Spacer(Modifier.height(32.dp))

        // ---------- Privacy / storage ----------
        Text("حریم خصوصی و حافظه", style = MaterialTheme.typography.headlineMedium)
        Spacer(Modifier.height(12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            FocusableCard(onClick = { confirmLogout = true }) {
                Text("پاک‌کردن ورودها و خروج", Modifier.padding(horizontal = 18.dp, vertical = 12.dp))
            }
            FocusableCard(onClick = { viewModel.clearCache { toast("حافظه موقت پاک شد") } }) {
                Text("پاک‌کردن حافظه موقت", Modifier.padding(horizontal = 18.dp, vertical = 12.dp))
            }
            FocusableCard(onClick = { confirmClearHistory = true }) {
                Text("پاک‌کردن تاریخچه", Modifier.padding(horizontal = 18.dp, vertical = 12.dp))
            }
        }

        Spacer(Modifier.height(32.dp))

        // ---------- About ----------
        Text("درباره Aminema", style = MaterialTheme.typography.headlineMedium)
        Spacer(Modifier.height(8.dp))
        Text("Aminema  •  v${BuildConfig.VERSION_NAME}", style = MaterialTheme.typography.titleMedium)
        Text(
            "هاب سرگرمی شخصی شما؛ محتوا را میزبانی یا بازنشر نمی‌کند و فقط سرویس‌های " +
                "خودتان را در مرورگر بهینه تلویزیون باز می‌کند.",
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
            title = { Text("افزودن سرویس") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = name, onValueChange = { name = it },
                        label = { Text("نام") }, singleLine = true
                    )
                    androidx.compose.runtime.CompositionLocalProvider(
                        LocalLayoutDirection provides LayoutDirection.Ltr
                    ) {
                        OutlinedTextField(
                            value = url,
                            onValueChange = { url = it },
                            label = { Text("URL (https://…)") },
                            singleLine = true
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.addService(name, url)
                    showAddDialog = false
                }) { Text("افزودن", color = CinemaRed) }
            },
            dismissButton = {
                TextButton(onClick = { showAddDialog = false }) { Text("انصراف") }
            }
        )
    }

    servicePendingRemoval?.let { service ->
        AlertDialog(
            onDismissRequest = { servicePendingRemoval = null },
            title = { Text("حذف ${service.name}؟") },
            text = { Text("این سرویس از فهرست Aminema حذف می‌شود.") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.removeService(service.id)
                    servicePendingRemoval = null
                }) { Text("حذف", color = CinemaRed) }
            },
            dismissButton = {
                TextButton(onClick = { servicePendingRemoval = null }) { Text("انصراف") }
            }
        )
    }
    if (confirmLogout) {
        AlertDialog(
            onDismissRequest = { confirmLogout = false },
            title = { Text("خروج از همه سرویس‌ها؟") },
            text = { Text("کوکی‌ها و نشست‌های ورود پاک می‌شوند و باید دوباره وارد شوید.") },
            confirmButton = {
                TextButton(onClick = {
                    confirmLogout = false
                    viewModel.clearCookies { toast("از همه سرویس‌ها خارج شدید") }
                }) { Text("خروج", color = CinemaRed) }
            },
            dismissButton = {
                TextButton(onClick = { confirmLogout = false }) { Text("انصراف") }
            }
        )
    }
    if (confirmClearHistory) {
        AlertDialog(
            onDismissRequest = { confirmClearHistory = false },
            title = { Text("تاریخچه پاک شود؟") },
            text = { Text("موارد اخیراً بازشده و ادامه تماشا پاک می‌شوند؛ علاقه‌مندی‌ها باقی می‌مانند.") },
            confirmButton = {
                TextButton(onClick = {
                    confirmClearHistory = false
                    viewModel.clearHistory()
                    toast("تاریخچه پاک شد")
                }) { Text("پاک‌کردن", color = CinemaRed) }
            },
            dismissButton = {
                TextButton(onClick = { confirmClearHistory = false }) { Text("انصراف") }
            }
        )
    }
    }
}
