package org.cleanroom.pokemongo.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import org.cleanroom.pokemongo.R
import org.cleanroom.pokemongo.data.repository.GameStateRepository
import org.cleanroom.pokemongo.data.repository.PokedexDatabase
import org.cleanroom.pokemongo.ui.MainActivity

class BuddyAppWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        val repo = GameStateRepository.getInstance(context)
        val buddy = repo.buddy.value
        val buddyPokemon = repo.pokemonBox.value.find { it.id == buddy.pokemonInstanceId }
        val species = if (buddyPokemon != null) PokedexDatabase.getById(buddyPokemon.speciesId) else null

        for (appWidgetId in appWidgetIds) {
            val views = RemoteViews(context.packageName, R.layout.widget_buddy)

            if (species != null) {
                views.setTextViewText(R.id.widget_buddy_name, buddyPokemon?.nickname ?: species.name)
                views.setTextViewText(
                    R.id.widget_buddy_candy_distance,
                    "Candy: %.1f / %.1f km".format(buddy.affectionKmProgress, species.buddyKmPerCandy)
                )
                views.setProgressBar(R.id.widget_buddy_hunger_bar, 100, buddy.hungerPercent, false)
                views.setTextViewText(R.id.widget_buddy_hearts_text, "Hearts today: ${buddy.getTotalHeartsToday()} / 8")
            } else {
                views.setTextViewText(R.id.widget_buddy_name, "No Buddy Assigned")
                views.setTextViewText(R.id.widget_buddy_candy_distance, "Tap to assign a buddy")
                views.setProgressBar(R.id.widget_buddy_hunger_bar, 100, 0, false)
            }

            val intent = Intent(context, MainActivity::class.java).apply {
                putExtra("NAVIGATE_TO", "BUDDY")
            }
            val pendingIntent = PendingIntent.getActivity(
                context, appWidgetId, intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.widget_buddy_root, pendingIntent)

            appWidgetManager.updateAppWidget(appWidgetId, views)
        }
    }
}
