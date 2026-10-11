package com.example.checklistapp.widget

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.action.ActionParameters
import androidx.glance.action.actionParametersOf
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.action.ActionCallback
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.lazy.LazyColumn
import androidx.glance.appwidget.lazy.items
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.width
import androidx.glance.appwidget.Image
import androidx.glance.appwidget.ImageProvider
import androidx.glance.layout.VerticalAlignment
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.example.checklistapp.ChecklistItem
import com.example.checklistapp.data.ChecklistRepository
import com.example.checklistapp.MainActivity

class ChecklistGlanceWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = ChecklistGlanceWidget
}

object ChecklistGlanceWidget : GlanceAppWidget() {

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val appWidgetId = GlanceAppWidgetManager(context).getAppWidgetId(id)
        val listId = WidgetPrefs.getListId(context, appWidgetId)

        val repo = ChecklistRepository(context)
        val list = listId?.let { repo.getList(it) }
        val items = listId?.let { repo.getItemsOnce(it) } ?: emptyList()

        provideContent {
            GlanceTheme {
                Column(
                    modifier = GlanceModifier.fillMaxSize()
                        .background(ColorProvider(Color(0xFFF5F5F5)))
                ) {
                    Row(
                        modifier = GlanceModifier.fillMaxWidth().height(42.dp)
                            .background(ColorProvider(Color(0xFF1565C0)))
                            .padding(horizontal = 12.dp),
                        verticalAlignment = VerticalAlignment.CenterVertically
                    ) {
                        Text(
                            "${list?.emoji ?: "📋"} ${list?.name ?: "Checklist"}",
                            modifier = GlanceModifier.defaultWeight()
                                .clickable(actionStartActivity<MainActivity>()),
                            style = TextStyle(
                                color = ColorProvider(Color.White),
                                fontSize = 14.sp, fontWeight = FontWeight.Bold
                            ),
                            maxLines = 1
                        )
                    }

                    if (items.isEmpty()) {
                        Box(
                            modifier = GlanceModifier.fillMaxWidth().defaultWeight(),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                "Nothing here yet",
                                style = TextStyle(color = ColorProvider(Color(0xFF757575)))
                            )
                        }
                    } else {
                        LazyColumn(modifier = GlanceModifier.defaultWeight()) {
                            items(items = items, itemId = { it.id }) { item ->
                                WidgetRow(item)
                            }
                        }
                    }
                }
            }
        }
    }

    @Composable
    private fun WidgetRow(item: ChecklistItem) {
        Row(
            modifier = GlanceModifier.fillMaxWidth().height(40.dp)
                .background(ColorProvider(Color.White))
                .padding(horizontal = 12.dp),
            verticalAlignment = VerticalAlignment.CenterVertically
        ) {
            Image(
                provider = ImageProvider(
                    if (item.isChecked) android.R.drawable.checkbox_on_background
                    else android.R.drawable.checkbox_off_background
                ),
                contentDescription = "Toggle ${item.text}",
                modifier = GlanceModifier.clickable(
                    actionRunCallback<ToggleItemAction>(
                        actionParametersOf(ToggleItemAction.itemIdKey to item.id)
                    )
                )
            )
            Spacer(GlanceModifier.width(10.dp))
            Text(
                item.text,
                style = TextStyle(color = ColorProvider(Color(0xFF333333)), fontSize = 14.sp),
                maxLines = 1
            )
        }
    }
}

class ToggleItemAction : ActionCallback {

    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        val itemId = parameters[itemIdKey] ?: return
        val repo = ChecklistRepository(context)
        repo.getItemOnce(itemId)?.let { repo.setChecked(it, !it.isChecked) }
        ChecklistGlanceWidget.update(context, glanceId)
    }

    companion object {
        val itemIdKey = ActionParameters.Key<Long>("item_id")
    }
}
