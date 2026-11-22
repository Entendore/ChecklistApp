package com.example.checklistapp.ui.home

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.checklistapp.data.ChecklistRepository
import com.example.checklistapp.data.PrefsManager
import com.example.checklistapp.TaskList
import com.example.checklistapp.util.StatsHelper
import com.example.checklistapp.widget.ChecklistGlanceWidgetReceiver
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class,
    ExperimentalLayoutApi::class)
@Composable
fun HomeScreen(onOpenList: (Long) -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val vm: HomeViewModel = viewModel {
        HomeViewModel(ChecklistRepository(context.applicationContext))
    }
    val lists by vm.lists.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }

    var showCreate by remember { mutableStateOf(false) }
    var editTarget by remember { mutableStateOf<TaskList?>(null) }
    var optionsTarget by remember { mutableStateOf<TaskList?>(null) }
    var deleteTarget by remember { mutableStateOf<TaskList?>(null) }
    var showStats by remember { mutableStateOf(false) }
    var menuOpen by remember { mutableStateOf(false) }
    var pendingRestoreJson by remember { mutableStateOf<String?>(null) }

    val exportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        if (uri != null) scope.launch {
            runCatching {
                context.contentResolver.openOutputStream(uri)?.use {
                    it.write(vm.export().toByteArray())
                }
            }.onSuccess { Toast.makeText(context, "✅ Backup saved", Toast.LENGTH_SHORT).show() }
                .onFailure {
                    Toast.makeText(context, "Backup failed: ${it.message}", Toast.LENGTH_LONG).show()
                }
        }
    }

    val importLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) scope.launch {
            pendingRestoreJson = runCatching {
                context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }
            }.getOrNull()
        }
    }

    LaunchedEffect(Unit) {
        if (PrefsManager.isFirstRun(context)) {
            PrefsManager.markFirstRunDone(context)
            vm.seedSampleData()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("My Checklists") },
                actions = {
                    IconButton(onClick = { menuOpen = true }) {
                        Icon(Icons.Default.MoreVert, contentDescription = "Menu")
                    }
                    DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                        DropdownMenuItem(
                            text = { Text("📊 Statistics") },
                            onClick = { menuOpen = false; showStats = true }
                        )
                        DropdownMenuItem(
                            text = { Text("💾 Backup to File") },
                            onClick = { menuOpen = false; exportLauncher.launch("checklist_backup.json") }
                        )
                        DropdownMenuItem(
                            text = { Text("📂 Restore from File") },
                            onClick = { menuOpen = false; importLauncher.launch(arrayOf("application/json", "text/*")) }
                        )
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbar) },
        floatingActionButton = {
            FloatingActionButton(onClick = { showCreate = true }) {
                Icon(Icons.Default.Add, contentDescription = "Add list")
            }
        }
    ) { padding ->
        if (lists.isEmpty()) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Text(
                    "No lists yet.\nTap + to create one! 👇",
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(lists, key = { it.id }) { list ->
                    ListCard(
                        list = list,
                        onClick = { onOpenList(list.id) },
                        onLongClick = { optionsTarget = list }
                    )
                }
            }
        }
    }

    if (showCreate) {
        ListDialog(
            title = "New List", initialName = "", initialEmoji = "📋",
            onSave = { name, emoji -> vm.addList(name, emoji) },
            onDismiss = { showCreate = false }
        )
    }

    editTarget?.let { target ->
        ListDialog(
            title = "Rename List", initialName = target.name, initialEmoji = target.emoji,
            onSave = { name, emoji -> vm.renameList(target, name, emoji) },
            onDismiss = { editTarget = null }
        )
    }

    optionsTarget?.let { list ->
        AlertDialog(
            onDismissRequest = { optionsTarget = null },
            title = { Text("${list.emoji} ${list.name}") },
            text = {
                Column {
                    TextButton(onClick = { optionsTarget = null; editTarget = list }) { Text("✏️ Rename") }
                    TextButton(onClick = {
                        optionsTarget = null
                        val manager = AppWidgetManager.getInstance(context)
                        if (!manager.isRequestPinAppWidgetSupported) {
                            Toast.makeText(context, "Launcher doesn't support pinning", Toast.LENGTH_SHORT).show()
                        } else {
                            manager.requestPinAppWidget(
                                ComponentName(context, ChecklistGlanceWidgetReceiver::class.java),
                                null, null
                            )
                        }
                    }) { Text("📌 Pin widget to Home Screen") }
                    TextButton(onClick = { optionsTarget = null; deleteTarget = list }) { Text("🗑 Delete list") }
                }
            },
            confirmButton = {},
            dismissButton = { TextButton({ optionsTarget = null }) { Text("Cancel") } }
        )
    }

    deleteTarget?.let { list ->
        AlertDialog(
            onDismissRequest = { deleteTarget = null },
            title = { Text("Delete \"${list.name}\"?") },
            text = { Text("This will delete ${list.totalCount} item(s). You can undo right after.") },
            confirmButton = {
                TextButton(onClick = {
                    deleteTarget = null
                    scope.launch {
                        vm.deleteList(list)
                        val result = snackbar.showSnackbar(
                            "List deleted", actionLabel = "UNDO",
                            duration = SnackbarDuration.Long
                        )
                        if (result == SnackbarResult.ActionPerformed) vm.addList(list.name, list.emoji)
                    }
                }) { Text("Delete") }
            },
            dismissButton = { TextButton({ deleteTarget = null }) { Text("Cancel") } }
        )
    }

    if (showStats) StatsDialog(onLoad = { vm.stats() }, onDismiss = { showStats = false })

    pendingRestoreJson?.let { json ->
        AlertDialog(
            onDismissRequest = { pendingRestoreJson = null },
            title = { Text("Restore backup?") },
            text = { Text("This will replace ALL current lists and history.") },
            confirmButton = {
                TextButton(onClick = {
                    pendingRestoreJson = null
                    scope.launch {
                        val ok = vm.import(json)
                        Toast.makeText(
                            context,
                            if (ok) "✅ Backup restored" else "❌ Invalid backup file",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }) { Text("Replace") }
            },
            dismissButton = { TextButton({ pendingRestoreJson = null }) { Text("Cancel") } }
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ListCard(list: TaskList, onClick: () -> Unit, onLongClick: () -> Unit) {
    Card(modifier = Modifier.combinedClickable(onClick = onClick, onLongClick = onLongClick)) {
        Column(Modifier.padding(16.dp)) {
            Text(list.emoji, fontSize = 30.sp)
            Text(
                list.name,
                style = MaterialTheme.typography.titleMedium,
                maxLines = 1, overflow = TextOverflow.Ellipsis
            )
            if (list.overdueCount > 0) {
                Text(
                    "⚠ ${list.overdueCount} overdue",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.error
                )
            }
            Text(
                if (list.totalCount == 0) "Empty" else "${list.doneCount} / ${list.totalCount} done",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            LinearProgressIndicator(
                progress = { list.percent / 100f },
                modifier = Modifier.fillMaxWidth().padding(top = 10.dp)
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ListDialog(
    title: String, initialName: String, initialEmoji: String,
    onSave: (String, String) -> Unit, onDismiss: () -> Unit
) {
    var name by remember { mutableStateOf(initialName) }
    var emoji by remember { mutableStateOf(initialEmoji) }
    val emojis = remember {
        listOf("📋", "🛒", "✅", "💼", "🏠", "🎯", "💪", "✈️", "📚", "🛠️", "🎁", "🌟")
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column {
                OutlinedTextField(
                    value = name, onValueChange = { name = it },
                    label = { Text("List name") }, singleLine = true
                )
                Text(
                    "ICON", style = MaterialTheme.typography.labelSmall,
                    modifier = Modifier.padding(top = 14.dp)
                )
                FlowRow {
                    emojis.forEach { e ->
                        FilterChip(
                            selected = e == emoji,
                            onClick = { emoji = e },
                            label = { Text(e, fontSize = 20.sp) },
                            modifier = Modifier.padding(end = 6.dp, top = 6.dp)
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                enabled = name.isNotBlank(),
                onClick = { onSave(name.trim(), emoji); onDismiss() }
            ) { Text("Save") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@Composable
fun StatsDialog(onLoad: suspend () -> StatsHelper.Stats, onDismiss: () -> Unit) {
    val stats = remember { mutableStateOf<StatsHelper.Stats?>(null) }
    LaunchedEffect(Unit) { stats.value = onLoad() }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("📊 Statistics") },
        text = {
            stats.value?.let { s ->
                Text(
                    """
                    🗂  Lists: ${s.lists}

                    ✅  Tasks: ${s.done} of ${s.tasks} completed
                    ⚠️  Overdue: ${s.overdue}    ⏰  Due today: ${s.dueToday}

                    📈  Completed this week: ${s.doneThisWeek}
                    🔥  Current streak: ${s.streak} day${if (s.streak == 1) "" else "s"}
                    """.trimIndent()
                )
            } ?: Box(Modifier.fillMaxWidth().padding(24.dp), Alignment.Center) {
                CircularProgressIndicator()
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Nice!") } }
    )
}
