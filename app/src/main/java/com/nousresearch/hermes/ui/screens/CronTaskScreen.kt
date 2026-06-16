package com.nousresearch.hermes.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.nousresearch.hermes.data.model.CronJob
import com.nousresearch.hermes.ui.theme.*
import com.nousresearch.hermes.viewmodel.ConnectionViewModel
import com.nousresearch.hermes.viewmodel.CronTaskViewModel

@Composable
fun CronTaskScreen(
    onNavigateBack: () -> Unit,
    viewModel: CronTaskViewModel = hiltViewModel(),
    connectionViewModel: ConnectionViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsState()
    val connectionState by connectionViewModel.state.collectAsState()
    var showCreate by remember { mutableStateOf(false) }

    LaunchedEffect(connectionState.config) { viewModel.refresh(connectionState.config) }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            Column {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Brush.verticalGradient(listOf(Color(0xFF0D0B2B), MaterialTheme.colorScheme.background)))
                        .statusBarsPadding()
                        .padding(horizontal = 8.dp, vertical = 8.dp),
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = onNavigateBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Ink100)
                        }
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(Brush.linearGradient(listOf(Indigo500, Indigo700))),
                            contentAlignment = Alignment.Center,
                        ) { Icon(Icons.Default.Schedule, null, Modifier.size(18.dp), tint = Color.White) }
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text("Cron & Tasks", style = MaterialTheme.typography.titleMedium, color = Ink50)
                            Text("Scheduled automations", style = MaterialTheme.typography.labelSmall, color = Ink200)
                        }
                        IconButton(onClick = { viewModel.refresh(connectionState.config) }) {
                            Icon(Icons.Default.Refresh, null, tint = Ink200)
                        }
                    }
                }
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
            }
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { showCreate = true },
                icon = { Icon(Icons.Default.Add, null) },
                text = { Text("Schedule", fontWeight = FontWeight.SemiBold) },
                containerColor = Indigo500,
                contentColor = Color.White,
                shape = RoundedCornerShape(16.dp),
            )
        },
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            if (state.error != null) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f))
                        .padding(12.dp),
                ) {
                    Icon(Icons.Default.Info, null, Modifier.size(16.dp), tint = Amber400)
                    Spacer(Modifier.width(8.dp))
                    Text("Gateway cron API coming soon · ${state.error}", style = MaterialTheme.typography.bodySmall, color = Ink200)
                }
            }
            if (state.isLoading) LinearProgressIndicator(Modifier.fillMaxWidth(), color = Indigo400, trackColor = Ink700)

            if (state.jobs.isEmpty() && !state.isLoading) {
                CronEmptyState(modifier = Modifier.align(Alignment.CenterHorizontally).padding(top = 80.dp))
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    items(state.jobs, key = { it.id }) { job ->
                        CronJobCard(
                            job = job,
                            onRun = { viewModel.runNow(connectionState.config, job.id) },
                            onDelete = { viewModel.delete(connectionState.config, job.id) },
                        )
                    }
                    item { Spacer(Modifier.height(80.dp)) }
                }
            }
        }
    }

    if (showCreate) {
        CreateCronJobDialog(
            isCreating = state.isCreating,
            onDismiss  = { showCreate = false },
            onCreate   = { name, schedule, prompt, deliver ->
                viewModel.create(connectionState.config, name, schedule, prompt, deliver)
                showCreate = false
            },
        )
    }
}

@Composable
private fun CronJobCard(job: CronJob, onRun: () -> Unit, onDelete: () -> Unit) {
    val enabled = job.enabled
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(
            modifier = Modifier
                .size(42.dp)
                .clip(RoundedCornerShape(12.dp))
                .background((if (enabled) Indigo500 else Ink600).copy(alpha = 0.2f)),
            contentAlignment = Alignment.Center,
        ) { Icon(Icons.Default.Schedule, null, Modifier.size(20.dp), tint = if (enabled) Indigo400 else Ink600) }
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(job.name ?: job.id.take(12), style = MaterialTheme.typography.titleSmall, color = Ink50, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Box(modifier = Modifier.clip(RoundedCornerShape(6.dp)).background(Ink750).padding(horizontal = 7.dp, vertical = 2.dp)) {
                    Text(job.schedule, style = MaterialTheme.typography.labelSmall, color = Cyan400, fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace)
                }
            }
            Text(job.prompt, style = MaterialTheme.typography.bodySmall, color = Ink200, maxLines = 2, overflow = TextOverflow.Ellipsis)
            job.nextRunAt?.let { Text("Next: $it", style = MaterialTheme.typography.labelSmall, color = Ink600) }
        }
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            IconButton(onClick = onRun, modifier = Modifier.size(36.dp)) {
                Icon(Icons.Default.PlayArrow, "Run now", Modifier.size(18.dp), tint = Green400)
            }
            IconButton(onClick = onDelete, modifier = Modifier.size(36.dp)) {
                Icon(Icons.Default.Delete, "Delete", Modifier.size(18.dp), tint = Red400.copy(alpha = 0.7f))
            }
        }
    }
}

@Composable
private fun CronEmptyState(modifier: Modifier = Modifier) {
    Column(modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Box(
            modifier = Modifier.size(72.dp).clip(RoundedCornerShape(22.dp)).background(Brush.linearGradient(listOf(Indigo700.copy(alpha = 0.4f), Ink800))),
            contentAlignment = Alignment.Center,
        ) { Icon(Icons.Default.Schedule, null, Modifier.size(32.dp), tint = Indigo400) }
        Text("No scheduled tasks", style = MaterialTheme.typography.titleMedium, color = Ink100)
        Text("Tap Schedule to automate Hermes", style = MaterialTheme.typography.bodySmall, color = Ink200)
    }
}

@Composable
private fun CreateCronJobDialog(
    isCreating: Boolean,
    onDismiss: () -> Unit,
    onCreate: (String, String, String, String?) -> Unit,
) {
    var name     by remember { mutableStateOf("") }
    var schedule by remember { mutableStateOf("every 1h") }
    var prompt   by remember { mutableStateOf("") }
    var deliver  by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor   = MaterialTheme.colorScheme.surface,
        shape            = RoundedCornerShape(24.dp),
        title = {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Icon(Icons.Default.Schedule, null, Modifier.size(22.dp), tint = Indigo400)
                Text("Create scheduled task", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(name, { name = it }, label = { Text("Name (optional)") }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp), colors = hermesTextFieldColors(), singleLine = true)
                OutlinedTextField(schedule, { schedule = it }, label = { Text("Schedule") }, placeholder = { Text("every 2h  ·  0 9 * * *  ·  30m") }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp), colors = hermesTextFieldColors(), singleLine = true)
                OutlinedTextField(prompt, { prompt = it }, label = { Text("Prompt / task") }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp), colors = hermesTextFieldColors(), minLines = 3)
                OutlinedTextField(deliver, { deliver = it }, label = { Text("Deliver target (optional)") }, placeholder = { Text("telegram  ·  origin") }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp), colors = hermesTextFieldColors(), singleLine = true)
            }
        },
        confirmButton = {
            Button(
                enabled = schedule.isNotBlank() && prompt.isNotBlank() && !isCreating,
                onClick = { onCreate(name, schedule, prompt, deliver.ifBlank { null }) },
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Indigo500, contentColor = Color.White),
                modifier = Modifier.fillMaxWidth(),
            ) { Text(if (isCreating) "Creating…" else "Create task", fontWeight = FontWeight.SemiBold) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, modifier = Modifier.fillMaxWidth()) { Text("Cancel") }
        },
    )
}
