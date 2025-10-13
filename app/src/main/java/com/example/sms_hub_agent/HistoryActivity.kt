package com.example.sms_hub_agent

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.sms_hub_agent.data.AppDatabase
import com.example.sms_hub_agent.data.SmsHistory
import com.example.sms_hub_agent.repository.HistoryRepository
import com.example.sms_hub_agent.ui.theme.SmshubagentTheme
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class HistoryActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            SmshubagentTheme {
                HistoryScreen()
            }
        }
    }
}

class HistoryViewModel(private val repository: HistoryRepository) : ViewModel() {
    val historyList: Flow<List<SmsHistory>> = repository.getAllHistory()
    val successCount: Flow<Int> = repository.getSuccessCount()
    val failedCount: Flow<Int> = repository.getFailedCount()

    suspend fun deleteAll() {
        repository.deleteAll()
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen() {
    val context = LocalContext.current
    val repository = remember {
        HistoryRepository(AppDatabase.getDatabase(context).smsHistoryDao())
    }
    val viewModel: HistoryViewModel = viewModel(
        factory = object : androidx.lifecycle.ViewModelProvider.Factory {
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                @Suppress("UNCHECKED_CAST")
                return HistoryViewModel(repository) as T
            }
        }
    )

    val historyList by viewModel.historyList.collectAsState(initial = emptyList())
    val successCount by viewModel.successCount.collectAsState(initial = 0)
    val failedCount by viewModel.failedCount.collectAsState(initial = 0)
    val scope = rememberCoroutineScope()
    var showDeleteDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("SMS History") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                ),
                actions = {
                    if (historyList.isNotEmpty()) {
                        IconButton(onClick = { showDeleteDialog = true }) {
                            Icon(Icons.Default.Delete, "Delete all")
                        }
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Statistics Card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            Icons.Default.CheckCircle,
                            "Success",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(32.dp)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "$successCount",
                            style = MaterialTheme.typography.headlineMedium,
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                        Text(
                            text = "Success",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            Icons.Default.Error,
                            "Failed",
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(32.dp)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "$failedCount",
                            style = MaterialTheme.typography.headlineMedium,
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                        Text(
                            text = "Failed",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            Icons.Default.Message,
                            "Total",
                            tint = MaterialTheme.colorScheme.tertiary,
                            modifier = Modifier.size(32.dp)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "${historyList.size}",
                            style = MaterialTheme.typography.headlineMedium,
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                        Text(
                            text = "Total",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    }
                }
            }

            // History List
            if (historyList.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            Icons.Default.History,
                            "No history",
                            modifier = Modifier.size(64.dp),
                            tint = MaterialTheme.colorScheme.outline
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "No SMS history yet",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.outline
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(historyList, key = { it.id }) { history ->
                        HistoryItem(history)
                    }
                }
            }
        }
    }

    // Delete confirmation dialog
    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Delete All History?") },
            text = { Text("This action cannot be undone. All SMS history will be permanently deleted.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        scope.launch {
                            viewModel.deleteAll()
                            showDeleteDialog = false
                        }
                    }
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun HistoryItem(history: SmsHistory) {
    val timeFormatter = remember { SimpleDateFormat("MMM dd, HH:mm:ss", Locale.getDefault()) }
    var expanded by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = when (history.status) {
                "success" -> MaterialTheme.colorScheme.primaryContainer
                "failed" -> MaterialTheme.colorScheme.errorContainer
                else -> MaterialTheme.colorScheme.surfaceVariant
            }
        ),
        onClick = { expanded = !expanded }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = if (history.status == "success")
                            Icons.Default.CheckCircle
                        else
                            Icons.Default.Error,
                        contentDescription = history.status,
                        tint = if (history.status == "success")
                            MaterialTheme.colorScheme.onPrimaryContainer
                        else
                            MaterialTheme.colorScheme.onErrorContainer,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = history.sender,
                            style = MaterialTheme.typography.titleMedium,
                            color = if (history.status == "success")
                                MaterialTheme.colorScheme.onPrimaryContainer
                            else
                                MaterialTheme.colorScheme.onErrorContainer
                        )
                        Text(
                            text = timeFormatter.format(Date(history.forwardedAt)),
                            style = MaterialTheme.typography.bodySmall,
                            color = if (history.status == "success")
                                MaterialTheme.colorScheme.onPrimaryContainer
                            else
                                MaterialTheme.colorScheme.onErrorContainer
                        )
                    }
                }
                Icon(
                    imageVector = if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = if (expanded) "Collapse" else "Expand",
                    tint = if (history.status == "success")
                        MaterialTheme.colorScheme.onPrimaryContainer
                    else
                        MaterialTheme.colorScheme.onErrorContainer
                )
            }

            AnimatedVisibility(
                visible = expanded,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 12.dp)
                ) {
                    Divider(
                        modifier = Modifier.padding(vertical = 8.dp),
                        color = if (history.status == "success")
                            MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.3f)
                        else
                            MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.3f)
                    )

                    DetailRow(
                        label = "Message",
                        value = history.message,
                        isSuccess = history.status == "success"
                    )
                    DetailRow(
                        label = "Device",
                        value = "${history.device} (API ${history.osVersion})",
                        isSuccess = history.status == "success"
                    )
                    DetailRow(
                        label = "Webhook",
                        value = history.webhookUrl,
                        isSuccess = history.status == "success"
                    )
                    if (history.errorMessage != null) {
                        DetailRow(
                            label = "Error",
                            value = history.errorMessage,
                            isSuccess = false
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun DetailRow(label: String, value: String, isSuccess: Boolean) {
    Column(modifier = Modifier.padding(vertical = 4.dp)) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = if (isSuccess)
                MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
            else
                MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.7f)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            color = if (isSuccess)
                MaterialTheme.colorScheme.onPrimaryContainer
            else
                MaterialTheme.colorScheme.onErrorContainer,
            maxLines = if (label == "Message") 3 else 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}
