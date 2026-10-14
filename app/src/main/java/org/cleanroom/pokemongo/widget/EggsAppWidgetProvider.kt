package org.cleanroom.pokemongo.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import org.cleanroom.pokemongo.R
import org.cleanroom.pokemongo.data.repository.GameStateRepository
import org.cleanroom.pokemongo.ui.MainActivity

class EggsAppWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        val repo = GameStateRepository.getInstance(context)
        val eggs = repo.eggs.value
        val activeEgg = eggs.find { it.incubatorId != null && !it.isHatched } ?: eggs.firstOrNull()

        for (appWidgetId in appWidgetIds) {
            val views = RemoteViews(context.getPackageName(), R.layout.widget_eggs)

            if (activeEgg != null) {
                views.setTextViewText(
                    R.id.widget_egg_distance_text,
                    "%.1f / %.1f km".format(activeEgg.walkedKm, activeEgg.tier.distanceKm)
                )
                val progress = ((activeEgg.walkedKm / activeEgg.tier.distanceKm) * 100).toInt().coerceIn(0, 100)
                views.setProgressBar(R.id.widget_egg_progress_bar, 100, progress, false)
                views.setTextViewText(R.id.widget_egg_status, if (activeEgg.isReadyToHatch()) "Ready to hatch!" else "Incubating...")
            } else {
                views.setTextViewText(R.id.widget_egg_distance_text, "No active egg")
                views.setProgressBar(R.id.widget_egg_progress_bar, 100, 0, false)
                views.setTextViewText(R.id.widget_egg_status, "Place an egg in incubator!")
            }

            // Click pending intent to launch main app
            val intent = Intent(context, MainActivity::class.java).apply {
                putExtra("NAVIGATE_TO", "EGGS")
            }
            val pendingIntent = PendingIntent.getActivity(
                context, appWidgetId, intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.widget_eggs_root, pendingIntent)

            appWidgetManager.updateAppWidget(appWidgetId, views)
        }
    }
}
