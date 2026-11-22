package com.example.checklistapp.ui.checklist

import android.Manifest
import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.Context
import android.net.Uri
import android.os.Build
import android.provider.OpenableColumns
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.checklistapp.Attachment
import com.example.checklistapp.AttachmentStore
import com.example.checklistapp.ChecklistItem
import com.example.checklistapp.Priority
import com.example.checklistapp.Recurrence
import com.example.checklistapp.Subtask
import java.io.File
import java.util.Calendar

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ItemDialog(
    existing: ChecklistItem?,
    onSave: (text: String, notes: String, priority: Priority, recurrence: Recurrence,
             deadline: Long?, reminderMinutes: Int?,
             subtasks: List<Subtask>, attachments: List<Attachment>) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current

    var title by remember { mutableStateOf(existing?.text ?: "") }
    var notes by remember { mutableStateOf(existing?.notes ?: "") }
    var priority by remember { mutableStateOf(existing?.priority ?: Priority.MEDIUM) }
    var recurrence by remember { mutableStateOf(existing?.recurrence ?: Recurrence.NONE) }
    var reminder by remember { mutableStateOf(existing?.reminderOffsetMinutes) }
    var deadline by remember { mutableStateOf(existing?.dueDate) }
    val subs = remember {
        mutableStateListOf<Subtask>().apply { existing?.subtasks?.forEach { add(it.copy()) } }
    }
    val atts = remember {
        mutableStateListOf<Attachment>().apply { existing?.attachments?.forEach { add(it.copy()) } }
    }
    var subInput by remember { mutableStateOf("") }

    val notifPermission = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { }

    fun stageAttachment(uri: Uri, mime: String?) {
        val name = queryDisplayName(context, uri)
        val rel = AttachmentStore.stageFromUri(context, uri, name)
        if (rel == null) {
            Toast.makeText(context, "Couldn't attach file", Toast.LENGTH_SHORT).show()
        } else {
            atts.add(
                Attachment(
                    fileName = name, localPath = rel,
                    mimeType = mime ?: "application/octet-stream"
                )
            )
        }
    }

    val pickImage = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia()
    ) { uri -> uri?.let { stageAttachment(it, "image/*") } }

    val pickFile = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri -> uri?.let { stageAttachment(it, context.contentResolver.getType(it)) } }

    fun pickDate() {
        val cal = Calendar.getInstance().apply { deadline?.let { timeInMillis = it } }
        DatePickerDialog(context, { _, y, m, d ->
            val hour = deadline?.let { hourOf(it) } ?: 17
            val minute = deadline?.let { minuteOf(it) } ?: 0
            deadline = Calendar.getInstance().apply {
                set(y, m, d, hour, minute, 0); set(Calendar.MILLISECOND, 0)
            }.timeInMillis
        }, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH)).show()
    }

    fun pickTime() {
        val cal = Calendar.getInstance().apply { deadline?.let { timeInMillis = it } }
        TimePickerDialog(context, { _, h, min ->
            val day = deadline ?: System.currentTimeMillis()
            deadline = Calendar.getInstance().apply {
                timeInMillis = day
                set(Calendar.HOUR_OF_DAY, h); set(Calendar.MINUTE, min)
                set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
            }.timeInMillis
        }, cal.get(Calendar.HOUR_OF_DAY), cal.get(Calendar.MINUTE), true).show()
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (existing == null) "New Item" else "Edit Item") },
        text = {
            Column(
                Modifier.verticalScroll(rememberScrollState()).heightIn(max = 460.dp)
            ) {
                OutlinedTextField(
                    value = title, onValueChange = { title = it },
                    label = { Text("Item title") }, singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("itemTitleInput")
                )
                OutlinedTextField(
                    value = notes, onValueChange = { notes = it },
                    label = { Text("Notes (optional)") }, minLines = 2, maxLines = 4,
                    modifier = Modifier.fillMaxWidth().padding(top = 10.dp)
                )

                SectionLabel("PRIORITY")
                ChipRow(
                    listOf(Priority.HIGH to "🔴 High", Priority.MEDIUM to "🟠 Medium",
                        Priority.LOW to "🟢 Low"),
                    selected = priority, onSelect = { priority = it })

                SectionLabel("REPEATS")
                ChipRow(
                    listOf(
                        Recurrence.NONE to "None", Recurrence.DAILY to "🔁 Daily",
                        Recurrence.WEEKLY to "🔁 Weekly", Recurrence.MONTHLY to "🔁 Monthly"),
                    selected = recurrence, onSelect = { recurrence = it })

                SectionLabel("DEADLINE")
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = { pickDate() }, modifier = Modifier.weight(1f)) { Text("📅 Date") }
                    OutlinedButton(onClick = { pickTime() }, modifier = Modifier.weight(1f)) { Text("🕐 Time") }
                    OutlinedButton(onClick = { deadline = null }, modifier = Modifier.weight(1f)) { Text("Clear") }
                }
                deadline?.let {
                    Text(
                        "Deadline: ${ChecklistItem.formatDeadline(it)}",
                        style = MaterialTheme.typography.labelLarge,
                        modifier = Modifier.padding(top = 6.dp)
                    )
                }

                SectionLabel("REMINDER")
                ChipRow(
                    listOf(
                        null to "Off", 0 to "At deadline", 30 to "30m before",
                        60 to "1h before", 1440 to "1 day before"),
                    selected = reminder, onSelect = { reminder = it })

                SectionLabel("SUBTASKS")
                Row(verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(
                        value = subInput, onValueChange = { subInput = it },
                        label = { Text("Add a subtask") }, singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(onClick = {
                        if (subInput.isNotBlank()) {
                            subs.add(Subtask(text = subInput.trim())); subInput = ""
                        }
                    }) { Icon(Icons.Default.Add, "Add subtask") }
                }
                subs.forEachIndexed { i, s ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(
                            checked = s.isChecked,
                            onCheckedChange = { subs[i] = s.copy(isChecked = it) })
                        Text(s.text, modifier = Modifier.weight(1f))
                        IconButton(onClick = { subs.removeAt(i) }) {
                            Icon(Icons.Default.Close, "Remove subtask")
                        }
                    }
                }

                SectionLabel("ATTACHMENTS")
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(
                        onClick = {
                            pickImage.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                            )
                        },
                        modifier = Modifier.weight(1f)
                    ) { Text("🖼 Photo") }
                    OutlinedButton(
                        onClick = { pickFile.launch(arrayOf("*/*")) },
                        modifier = Modifier.weight(1f)
                    ) { Text("📄 File") }
                }
                atts.forEachIndexed { i, a ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(top = 4.dp)
                    ) {
                        if (a.mimeType.startsWith("image/")) {
                            AsyncImage(
                                model = File(context.filesDir, a.localPath),
                                contentDescription = a.fileName,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.size(36.dp).clip(RoundedCornerShape(6.dp))
                            )
                            Spacer(Modifier.width(8.dp))
                        }
                        Text(
                            "📎 ${a.fileName}", maxLines = 1, overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f),
                            style = MaterialTheme.typography.bodySmall
                        )
                        IconButton(onClick = { atts.removeAt(i) }) {
                            Icon(Icons.Default.Close, "Remove attachment")
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                enabled = title.isNotBlank(),
                onClick = {
                    if (reminder != null && deadline != null && Build.VERSION.SDK_INT >= 33) {
                        notifPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
                    }
                    onSave(
                        title.trim(), notes.trim(), priority, recurrence,
                        deadline, reminder, subs.toList(), atts.toList()
                    )
                }
            ) { Text("Save") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@Composable
private fun SectionLabel(text: String) {
    Text(text, style = MaterialTheme.typography.labelSmall,
        modifier = Modifier.padding(top = 16.dp))
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun <T> ChipRow(options: List<Pair<T, String>>, selected: T, onSelect: (T) -> Unit) {
    FlowRow(Modifier.padding(top = 4.dp)) {
        options.forEach { (value, label) ->
            FilterChip(
                selected = value == selected,
                onClick = { onSelect(value) },
                label = { Text(label) },
                modifier = Modifier.padding(end = 6.dp, bottom = 6.dp)
            )
        }
    }
}

private fun hourOf(millis: Long): Int =
    Calendar.getInstance().apply { timeInMillis = millis }.get(Calendar.HOUR_OF_DAY)

private fun minuteOf(millis: Long): Int =
    Calendar.getInstance().apply { timeInMillis = millis }.get(Calendar.MINUTE)

private fun queryDisplayName(context: Context, uri: Uri): String {
    var name = "file"
    try {
        context.contentResolver.query(uri, null, null, null, null)?.use { c ->
            val idx = c.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            if (idx >= 0 && c.moveToFirst()) name = c.getString(idx) ?: name
        }
    } catch (_: Exception) {}
    return name
}
