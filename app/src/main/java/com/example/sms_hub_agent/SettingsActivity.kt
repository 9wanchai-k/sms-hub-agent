package com.example.sms_hub_agent

import androidx.compose.material3.ExperimentalMaterial3Api
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.sms_hub_agent.model.AppSettings
import com.example.sms_hub_agent.model.SmsPayload
import com.example.sms_hub_agent.repository.SettingsRepository
import com.example.sms_hub_agent.service.WebhookService
import com.example.sms_hub_agent.ui.theme.SmshubagentTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.*

class SettingsActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            SmshubagentTheme {
                SettingsScreen()
            }
        }
    }
}

class SettingsViewModel(private val repository: SettingsRepository) : ViewModel() {
    var settings by mutableStateOf(AppSettings())
        private set

    init {
        loadSettings()
    }

    fun loadSettings() {
        settings = repository.loadSettings()
    }

    fun updateSettings(newSettings: AppSettings) {
        settings = newSettings
    }

    fun saveSettings() {
        repository.saveSettings(settings)
    }

    fun getLastForwardTime(): Long {
        return repository.getLastForwardTime()
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen() {
    val context = LocalContext.current
    val repository = remember { SettingsRepository(context) }
    val viewModel: SettingsViewModel = viewModel(
        factory = object : androidx.lifecycle.ViewModelProvider.Factory {
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                @Suppress("UNCHECKED_CAST")
                return SettingsViewModel(repository) as T
            }
        }
    )

    var webhookUrl by remember { mutableStateOf(viewModel.settings.webhookUrl) }
    var username by remember { mutableStateOf(viewModel.settings.authUsername) }
    var password by remember { mutableStateOf(viewModel.settings.authPassword) }
    var hmacEnabled by remember { mutableStateOf(viewModel.settings.hmacEnabled) }
    var hmacSecret by remember { mutableStateOf(viewModel.settings.hmacSecret) }
    var passwordVisible by remember { mutableStateOf(false) }
    var hmacSecretVisible by remember { mutableStateOf(false) }
    var isTestingWebhook by remember { mutableStateOf(false) }

    val scope = rememberCoroutineScope()
    val lastForwardTime = remember { viewModel.getLastForwardTime() }
    val timeFormatter = remember { SimpleDateFormat("HH:mm:ss", Locale.getDefault()) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("SMS Hub Agent Settings") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Webhook Configuration Section
            Text(
                text = "Webhook Configuration",
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.primary
            )

            OutlinedTextField(
                value = webhookUrl,
                onValueChange = { webhookUrl = it },
                label = { Text("Webhook URL") },
                leadingIcon = { Icon(Icons.Default.Language, "Webhook URL") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            // Authentication Section
            Text(
                text = "Authentication",
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.primary
            )

            OutlinedTextField(
                value = username,
                onValueChange = { username = it },
                label = { Text("Username") },
                leadingIcon = { Icon(Icons.Default.Person, "Username") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                label = { Text("Password") },
                leadingIcon = { Icon(Icons.Default.Lock, "Password") },
                trailingIcon = {
                    IconButton(onClick = { passwordVisible = !passwordVisible }) {
                        Icon(
                            if (passwordVisible) Icons.Default.Visibility
                            else Icons.Default.VisibilityOff,
                            "Toggle password visibility"
                        )
                    }
                },
                visualTransformation = if (passwordVisible) VisualTransformation.None
                else PasswordVisualTransformation(),
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            // Security Section
            Text(
                text = "Security",
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.primary
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Enable HMAC Signature",
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.align(androidx.compose.ui.Alignment.CenterVertically)
                )
                Switch(
                    checked = hmacEnabled,
                    onCheckedChange = { hmacEnabled = it }
                )
            }

            AnimatedVisibility(
                visible = hmacEnabled,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                OutlinedTextField(
                    value = hmacSecret,
                    onValueChange = { hmacSecret = it },
                    label = { Text("HMAC Secret Key") },
                    leadingIcon = { Icon(Icons.Default.Key, "HMAC Secret") },
                    trailingIcon = {
                        IconButton(onClick = { hmacSecretVisible = !hmacSecretVisible }) {
                            Icon(
                                if (hmacSecretVisible) Icons.Default.Visibility
                                else Icons.Default.VisibilityOff,
                                "Toggle secret visibility"
                            )
                        }
                    },
                    visualTransformation = if (hmacSecretVisible) VisualTransformation.None
                    else PasswordVisualTransformation(),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Action Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = {
                        scope.launch {
                            isTestingWebhook = true
                            val testSettings = AppSettings(
                                webhookUrl = webhookUrl,
                                authUsername = username,
                                authPassword = password,
                                hmacEnabled = hmacEnabled,
                                hmacSecret = hmacSecret
                            )

                            if (!testSettings.isValid()) {
                                Toast.makeText(
                                    context,
                                    "Please fill in all required fields",
                                    Toast.LENGTH_SHORT
                                ).show()
                                isTestingWebhook = false
                                return@launch
                            }

                            val success = withContext(Dispatchers.IO) {
                                val testPayload = SmsPayload(
                                    sender = "+66812345678",
                                    message = "Test message from SMS Hub Agent",
                                    timestamp = System.currentTimeMillis(),
                                    device = Build.MODEL,
                                    osVersion = Build.VERSION.SDK_INT
                                )
                                WebhookService().sendSmsToWebhook(testPayload, testSettings)
                            }

                            isTestingWebhook = false
                            Toast.makeText(
                                context,
                                if (success) "Test successful!" else "Test failed. Check logs.",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    },
                    enabled = !isTestingWebhook,
                    modifier = Modifier.weight(1f)
                ) {
                    if (isTestingWebhook) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                    }
                    Text("Test")
                }

                Button(
                    onClick = {
                        val newSettings = AppSettings(
                            webhookUrl = webhookUrl,
                            authUsername = username,
                            authPassword = password,
                            hmacEnabled = hmacEnabled,
                            hmacSecret = hmacSecret
                        )

                        if (!newSettings.isValid()) {
                            Toast.makeText(
                                context,
                                "Please fill in all required fields",
                                Toast.LENGTH_SHORT
                            ).show()
                            return@Button
                        }

                        viewModel.updateSettings(newSettings)
                        viewModel.saveSettings()
                        Toast.makeText(
                            context,
                            "Settings saved successfully",
                            Toast.LENGTH_SHORT
                        ).show()
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.Save, "Save", modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Save")
                }
            }

            // Status Card
            if (lastForwardTime > 0) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            Icons.Default.CheckCircle,
                            "Success",
                            tint = MaterialTheme.colorScheme.secondary
                        )
                        Text(
                            text = "Last forwarded at ${timeFormatter.format(Date(lastForwardTime))}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    }
                }
            }
        }
    }
}
