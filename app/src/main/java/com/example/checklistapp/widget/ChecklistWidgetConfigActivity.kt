package com.example.checklistapp.widget

import android.appwidget.AppWidgetManager
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.checklistapp.data.ChecklistRepository
import com.example.checklistapp.TaskList
import com.example.checklistapp.ui.theme.ChecklistTheme
import kotlinx.coroutines.launch

class ChecklistWidgetConfigActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val appWidgetId = intent?.extras?.getInt(
            AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID
        ) ?: AppWidgetManager.INVALID_APPWIDGET_ID
        if (appWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID) { finish(); return }
        setResult(RESULT_CANCELED)

        setContent {
            ChecklistTheme {
                WidgetConfigContent(appWidgetId) { finish() }
            }
        }
    }

    @Composable
    private fun WidgetConfigContent(appWidgetId: Int, close: () -> Unit) {
        val context = applicationContext
        val scope = rememberCoroutineScope()
        var lists by remember { mutableStateOf<List<TaskList>>(emptyList()) }
        var loaded by remember { mutableStateOf(false) }
        var selectedId by remember { mutableStateOf(-1L) }

        LaunchedEffect(Unit) {
            lists = ChecklistRepository(context).getListsOnce()
            selectedId = lists.firstOrNull()?.id ?: -1L
            loaded = true
        }

        Surface(Modifier.fillMaxSize()) {
            when {
                !loaded -> Box(Modifier.fillMaxSize(), Alignment.Center) { CircularProgressIndicator() }

                lists.isEmpty() -> {
                    LaunchedEffect(Unit) {
                        Toast.makeText(context, "Create a list first", Toast.LENGTH_SHORT).show()
                        close()
                    }
                }

                else -> Column(
                    Modifier.fillMaxSize().padding(20.dp).verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text("Pin which list?", style = MaterialTheme.typography.titleLarge)
                    lists.forEach { list ->
                        Row(
                            Modifier.fillMaxWidth()
                                .clickable { selectedId = list.id }
                                .padding(vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = selectedId == list.id,
                                onClick = { selectedId = list.id }
                            )
                            Spacer(Modifier.width(8.dp))
                            Text("${list.emoji} ${list.name}")
                        }
                    }
                    Spacer(Modifier.weight(1f))
                    Button(
                        enabled = selectedId != -1L,
                        onClick = {
                            WidgetPrefs.saveListId(context, appWidgetId, selectedId)
                            scope.launch { ChecklistGlanceWidget.updateAll(context) }
                            setResult(
                                RESULT_OK,
                                Intent().putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
                            )
                            close()
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) { Text("Add widget") }
                }
            }
        }
    }
}
