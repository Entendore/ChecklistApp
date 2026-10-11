package com.example.checklistapp.ui.checklist

import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Card
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.checklistapp.ChecklistItem
import com.example.checklistapp.Priority
import com.example.checklistapp.Recurrence
import com.example.checklistapp.TaskList
import com.example.checklistapp.data.ChecklistRepository
import kotlinx.coroutines.launch

enum class ChecklistFilter { ALL, ACTIVE, COMPLETED }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChecklistScreen(listId: Long, onBack: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val haptic = LocalHapticFeedback.current
    val vm: ChecklistViewModel = viewModel(key = "list_$listId") {
        ChecklistViewModel(ChecklistRepository(context.applicationContext), listId)
    }
    val list by vm.list.collectAsStateWithLifecycle()
    val items by vm.items.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }

    var filter by remember { mutableStateOf(ChecklistFilter.ALL) }
    var query by remember { mutableStateOf("") }
    var showItemDialog by remember { mutableStateOf(false) }
    var dialogTarget by remember { mutableStateOf<ChecklistItem?>(null) }
    var quickTarget by remember { mutableStateOf<ChecklistItem?>(null) }
    var moveTarget by remember { mutableStateOf<ChecklistItem?>(null) }
    var menuOpen by remember { mutableStateOf(false) }

    val display = remember { mutableStateListOf<ChecklistItem>() }
    val canReorder = filter == ChecklistFilter.ALL && query.isBlank()

    LaunchedEffect(items, filter, query) {
        val q = query.lowercase()
        display.clear()
        display.addAll(items.filter { item ->
            val okFilter = when (filter) {
                ChecklistFilter.ALL -> true
                ChecklistFilter.ACTIVE -> !item.isChecked
                ChecklistFilter.COMPLETED -> item.isChecked
            }
            okFilter && (q.isEmpty() ||
                    item.text.lowercase().contains(q) ||
                    item.notes.lowercase().contains(q))
        })
    }

    var draggingId by remember { mutableStateOf<Long?>(null) }
    val dragOffsetY = remember { mutableFloatStateOf(0f) }
    var rowHeightPx by remember { mutableFloatStateOf(72f) }

    val done = items.count { it.isChecked }
    val total = items.size

    fun deleteWithUndo(item: ChecklistItem) = scope.launch {
        val (deleted, index) = vm.delete(item)
        val result = snackbar.showSnackbar(
            "\"${deleted.text}\" deleted", actionLabel = "UNDO",
            duration = SnackbarDuration.Long
        )
        if (result == SnackbarResult.ActionPerformed) vm.undoDelete(deleted, index)
    }

    fun share() {
        val l = list ?: return
        if (items.isEmpty()) {
            Toast.makeText(context, "Nothing to share yet", Toast.LENGTH_SHORT).show(); return
        }
        val text = buildString {
            appendLine("${l.emoji} ${l.name} ($done/$total done)")
            appendLine()
            items.forEach {
                appendLine(if (it.isChecked) "☑ ${it.text}" else "☐ ${it.text}")
                it.subtasks.forEach { s ->
                    appendLine("    ${if (s.isChecked) "☑" else "☐"} ${s.text}")
                }
            }
        }
        context.startActivity(
            Intent.createChooser(
                Intent(Intent.ACTION_SEND).apply {
                    type = "text/plain"; putExtra(Intent.EXTRA_TEXT, text)
                },
                "Share checklist via"
            )
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                title = {
                    Column {
                        Text("${list?.emoji ?: ""} ${list?.name ?: ""}")
                        Text(
                            when {
                                total == 0 -> "Empty list"
                                done == total -> "All done 🎉"
                                else -> "$done / $total done"
                            },
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { menuOpen = true }) {
                        Icon(Icons.Default.MoreVert, contentDescription = "Menu")
                    }
                    DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                        DropdownMenuItem(text = { Text("Sort: A → Z") },
                            onClick = { menuOpen = false; scope.launch { vm.sortByName() } })
                        DropdownMenuItem(text = { Text("Sort: Priority") },
                            onClick = { menuOpen = false; scope.launch { vm.sortByPriority() } })
                        DropdownMenuItem(text = { Text("Sort: Due date") },
                            onClick = { menuOpen = false; scope.launch { vm.sortByDueDate() } })
                        HorizontalDivider()
                        DropdownMenuItem(text = { Text("Clear completed") },
                            onClick = { menuOpen = false; scope.launch { vm.clearCompleted() } })
                        DropdownMenuItem(text = { Text("Uncheck all") },
                            onClick = { menuOpen = false; scope.launch { vm.uncheckAll() } })
                        DropdownMenuItem(text = { Text("Share list") },
                            onClick = { menuOpen = false; share() })
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbar) },
        floatingActionButton = {
            FloatingActionButton(onClick = { dialogTarget = null; showItemDialog = true }) {
                Icon(Icons.Default.Add, contentDescription = "Add item")
            }
        }
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {

            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp)
                    .testTag("searchField"),
                placeholder = { Text("Search items…") },
                leadingIcon = { Icon(Icons.Default.Search, null) },
                trailingIcon = {
                    if (query.isNotEmpty()) {
                        IconButton({ query = "" }) { Icon(Icons.Default.Close, "Clear search") }
                    }
                },
                singleLine = true
            )

            if (total > 0) {
                Card(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp)) {
                    Column(Modifier.padding(14.dp)) {
                        Text(
                            "$done of $total completed · " +
                                    (if (total == 0) 0 else done * 100 / total) + "%",
                            style = MaterialTheme.typography.labelLarge
                        )
                        LinearProgressIndicator(
                            progress = { if (total == 0) 0f else done.toFloat() / total },
                            modifier = Modifier.fillMaxWidth().padding(top = 10.dp)
                        )
                    }
                }
            }

            Row(
                Modifier.padding(horizontal = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = filter == ChecklistFilter.ALL,
                    onClick = { filter = ChecklistFilter.ALL }, label = { Text("All") })
                FilterChip(
                    selected = filter == ChecklistFilter.ACTIVE,
                    onClick = { filter = ChecklistFilter.ACTIVE }, label = { Text("Active") })
                FilterChip(
                    selected = filter == ChecklistFilter.COMPLETED,
                    onClick = { filter = ChecklistFilter.COMPLETED }, label = { Text("Done") })
            }

            if (display.isEmpty()) {
                Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                    Text(
                        when {
                            query.isNotBlank() -> "No matches for \"$query\""
                            filter == ChecklistFilter.ACTIVE -> "No active tasks 🎉"
                            filter == ChecklistFilter.COMPLETED -> "Nothing completed yet."
                            else -> "This list is empty.\nTap + to add your first task!"
                        },
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                LazyColumn(
                    Modifier.weight(1f).fillMaxWidth(),
                    contentPadding = PaddingValues(top = 4.dp, bottom = 96.dp)
                ) {
                    itemsIndexed(display, key = { _, it -> it.id }) { _, item ->
                        ItemRow(
                            item = item,
                            isDragging = draggingId == item.id,
                            dragOffsetY = dragOffsetY.floatValue,
                            canDrag = canReorder,
                            onRowMeasured = { rowHeightPx = it },
                            onDragStart = {
                                draggingId = item.id; dragOffsetY.floatValue = 0f
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            },
                            onDrag = { dy ->
                                dragOffsetY.floatValue += dy
                                while (true) {
                                    val cur = display.indexOfFirst { it.id == draggingId }
                                    if (cur == -1) break
                                    val offset = dragOffsetY.floatValue
                                    if (offset > rowHeightPx) {
                                        if (cur + 1 >= display.size) {
                                            dragOffsetY.floatValue = 0f; break
                                        }
                                        val moved = display.removeAt(cur)
                                        display.add(cur + 1, moved)
                                        draggingId = moved.id
                                        dragOffsetY.floatValue -= rowHeightPx
                                    } else if (offset < -rowHeightPx) {
                                        if (cur - 1 < 0) {
                                            dragOffsetY.floatValue = 0f; break
                                        }
                                        val moved = display.removeAt(cur)
                                        display.add(cur - 1, moved)
                                        draggingId = moved.id
                                        dragOffsetY.floatValue += rowHeightPx
                                    } else break
                                }
                            },
                            onDragEnd = {
                                draggingId = null; dragOffsetY.floatValue = 0f
                                if (canReorder) scope.launch { vm.reorder(display.toList()) }
                            },
                            onToggle = { i, c ->
                                scope.launch {
                                    vm.toggle(i, c)?.let { next ->
                                        snackbar.showSnackbar(
                                            "🔁 \"${i.text}\" repeats ${i.recurrence.label.lowercase()}" +
                                                    " — next due ${ChecklistItem.formatDeadline(next)}",
                                            duration = SnackbarDuration.Long
                                        )
                                    }
                                }
                            },
                            onEdit = { dialogTarget = item; showItemDialog = true },
                            onLongPress = { quickTarget = item },
                            onDelete = { deleteWithUndo(item) }
                        )
                    }
                }
            }
        }
    }

    if (showItemDialog) {
        ItemDialog(
            existing = dialogTarget,
            onSave = { text, notes, p, r, due, rem, subs, atts ->
                showItemDialog = false
                scope.launch {
                    val target = dialogTarget
                    if (target == null) {
                        val newItem = ChecklistItem(
                            listId = listId, text = text, notes = notes,
                            priority = p, recurrence = r, dueDate = due,
                            reminderOffsetMinutes = rem
                        ).apply {
                            subtasks.addAll(subs); attachments.addAll(atts)
                        }
                        vm.add(newItem)
                    } else {
                        target.text = text; target.notes = notes
                        target.priority = p; target.recurrence = r
                        target.dueDate = due; target.reminderOffsetMinutes = rem
                        vm.saveDetails(target, subs, atts)
                    }
                }
            },
            onDismiss = { showItemDialog = false }
        )
    }

    quickTarget?.let { target ->
        ModalBottomSheet(onDismissRequest = { quickTarget = null }) {
            ListItem(headlineContent = { Text("✏️ Edit") },
                modifier = Modifier.clickable {
                    quickTarget = null; dialogTarget = target; showItemDialog = true
                })
            ListItem(headlineContent = { Text("📄 Duplicate") },
                modifier = Modifier.clickable {
                    quickTarget = null; scope.launch { vm.duplicate(target) }
                })
            ListItem(headlineContent = { Text("📤 Move to another list") },
                modifier = Modifier.clickable { quickTarget = null; moveTarget = target })
            ListItem(
                headlineContent = { Text("🗑 Delete", color = MaterialTheme.colorScheme.error) },
                modifier = Modifier.clickable { quickTarget = null; deleteWithUndo(target) })
            Spacer(Modifier.height(24.dp))
        }
    }

    moveTarget?.let { item ->
        var others by remember(item) { mutableStateOf<List<TaskList>>(emptyList()) }
        LaunchedEffect(item) { others = vm.otherLists() }
        AlertDialog(
            onDismissRequest = { moveTarget = null },
            title = { Text("Move \"${item.text}\" to…") },
            text = {
                if (others.isEmpty()) Text("Create another list first")
                else Column {
                    others.forEach { l ->
                        TextButton(onClick = {
                            moveTarget = null
                            scope.launch { vm.move(item, l.id) }
                        }) { Text("${l.emoji} ${l.name}") }
                    }
                }
            },
            confirmButton = {},
            dismissButton = { TextButton({ moveTarget = null }) { Text("Cancel") } }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ItemRow(
    item: ChecklistItem,
    isDragging: Boolean,
    dragOffsetY: Float,
    canDrag: Boolean,
    onRowMeasured: (Float) -> Unit,
    onDragStart: () -> Unit,
    onDrag: (Float) -> Unit,
    onDragEnd: () -> Unit,
    onToggle: (ChecklistItem, Boolean) -> Unit,
    onEdit: () -> Unit,
    onLongPress: () -> Unit,
    onDelete: () -> Unit
) {
    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = { value ->
            if (value == SwipeToDismissBoxValue.EndToStart) { onDelete(); true } else false
        }
    )

    SwipeToDismissBox(
        state = dismissState,
        enableDismissFromStartToEnd = false,
        backgroundContent = {
            Box(
                Modifier.fillMaxSize()
                    .background(MaterialTheme.colorScheme.error, RoundedCornerShape(14.dp))
                    .padding(end = 20.dp),
                contentAlignment = Alignment.CenterEnd
            ) {
                Icon(Icons.Default.Delete, "Delete", tint = MaterialTheme.colorScheme.onError)
            }
        }
    ) {
        Card(
            modifier = Modifier
                .padding(horizontal = 12.dp, vertical = 5.dp)
                .zIndex(if (isDragging) 1f else 0f)
                .animateItem()
                .graphicsLayer { translationY = if (isDragging) dragOffsetY else 0f }
                .onSizeChanged { onRowMeasured(it.height.toFloat().coerceAtLeast(1f)) }
                .pointerInput(canDrag) {
                    if (!canDrag) return@pointerInput
                    detectDragGesturesAfterLongPress(
                        onDragStart = { onDragStart() },
                        onDrag = { change, amount -> change.consume(); onDrag(amount.y) },
                        onDragEnd = { onDragEnd() },
                        onDragCancel = { onDragEnd() }
                    )
                }
                .testTag("row_${item.text}")
        ) {
            Row(
                Modifier.padding(start = 10.dp, top = 10.dp, bottom = 10.dp, end = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    Modifier.width(5.dp).height(42.dp)
                        .background(
                            when (item.priority) {
                                Priority.HIGH -> Color(0xFFE53935)
                                Priority.MEDIUM -> Color(0xFFFB8C00)
                                Priority.LOW -> Color(0xFF43A047)
                            },
                            RoundedCornerShape(3.dp)
                        )
                )

                Checkbox(
                    checked = item.isChecked,
                    onCheckedChange = { onToggle(item, it) },
                    modifier = Modifier.testTag("checkbox_${item.text}")
                )

                Column(Modifier.weight(1f).padding(start = 4.dp)) {
                    Text(
                        item.text,
                        style = MaterialTheme.typography.bodyLarge,
                        textDecoration = if (item.isChecked) TextDecoration.LineThrough else null,
                        color = if (item.isChecked)
                            MaterialTheme.colorScheme.onSurface.copy(alpha = 0.45f)
                        else MaterialTheme.colorScheme.onSurface,
                        maxLines = 2
                    )

                    val due = item.dueDate
                    if (!item.isChecked && due != null) {
                        val (label, color) = when {
                            item.isOverdue() ->
                                "⚠ Overdue · ${ChecklistItem.formatDeadline(due)}" to Color(0xFFD32F2F)
                            item.isDueToday() ->
                                "⏰ ${ChecklistItem.formatDeadline(due)}" to Color(0xFFEF6C00)
                            else ->
                                "📅 ${ChecklistItem.formatDeadline(due)}" to Color(0xFF757575)
                        }
                        Text(label, style = MaterialTheme.typography.labelSmall, color = color)
                    }

                    if (item.recurrence != Recurrence.NONE) {
                        Text(
                            "🔁 Repeats ${item.recurrence.label.lowercase()}",
                            style = MaterialTheme.typography.labelSmall
                        )
                    }

                    val meta = buildString {
                        if (item.subtasks.isNotEmpty())
                            append("☑ ${item.subtasksDone}/${item.subtasks.size}")
                        if (item.attachments.isNotEmpty())
                            append(if (isEmpty()) "📎 ${item.attachments.size}"
                            else " · 📎 ${item.attachments.size}")
                        if (!item.isChecked && item.reminderOffsetMinutes != null)
                            append(if (isEmpty()) "🔔" else " · 🔔")
                    }
                    if (meta.isNotEmpty()) {
                        Text(meta, style = MaterialTheme.typography.labelSmall)
                    }
                }

                IconButton(onClick = onEdit) { Icon(Icons.Default.Edit, "Edit ${item.text}") }
                IconButton(onClick = onDelete) { Icon(Icons.Default.Delete, "Delete ${item.text}") }
            }
        }
    }
}
