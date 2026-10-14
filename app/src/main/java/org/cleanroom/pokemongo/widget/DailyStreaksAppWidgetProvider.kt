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
import java.text.SimpleDateFormat
import java.util.*

class DailyStreaksAppWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        val repo = GameStateRepository.getInstance(context)
        val streaks = repo.dailyStreaks.value
        val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())

        for (appWidgetId in appWidgetIds) {
            val views = RemoteViews(context.packageName, R.layout.widget_streaks)

            val catchStatus = if (streaks.isCatchCompletedToday(today)) "Catch: Day ${streaks.catchStreak}/7 (Done)" else "Catch: Day ${streaks.catchStreak}/7 (Pending)"
            val spinStatus = if (streaks.isSpinCompletedToday(today)) "Spin: Day ${streaks.spinStreak}/7 (Done)" else "Spin: Day ${streaks.spinStreak}/7 (Pending)"

            views.setTextViewText(R.id.widget_catch_streak_text, catchStatus)
            views.setTextViewText(R.id.widget_spin_streak_text, spinStatus)

            val intent = Intent(context, MainActivity::class.java).apply {
                putExtra("NAVIGATE_TO", "PROFILE")
            }
            val pendingIntent = PendingIntent.getActivity(
                context, appWidgetId, intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.widget_streaks_root, pendingIntent)

            appWidgetManager.updateAppWidget(appWidgetId, views)
        }
    }
}
