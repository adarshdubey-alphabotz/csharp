package org.cleanroom.pokemongo.service

import android.app.*
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import kotlinx.coroutines.*
import org.cleanroom.pokemongo.data.activation.ActivationManager
import org.cleanroom.pokemongo.data.automation.AutomationEngine
import org.cleanroom.pokemongo.data.models.ItemType
import org.cleanroom.pokemongo.data.repository.CatchResult
import org.cleanroom.pokemongo.data.repository.GameStateRepository
import org.cleanroom.pokemongo.data.repository.PokedexDatabase
import org.cleanroom.pokemongo.data.repository.ThrowQuality

class PokemonGoPlusService : Service() {

    private val serviceScope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    private var autocatcherJob: Job? = null

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val notification = createNotification("Pokémon GO Plus Connected", "Auto-scanning for Pokémon and PokéStops")
        startForeground(NOTIFICATION_ID, notification)

        startAutoCatcher()
        return START_STICKY
    }

    private fun startAutoCatcher() {
        autocatcherJob?.cancel()
        autocatcherJob = serviceScope.launch {
            val repo = GameStateRepository.getInstance(applicationContext)
            val activation = ActivationManager.getInstance(applicationContext)
            val engine = AutomationEngine.getInstance(applicationContext, repo, activation)

            while (isActive) {
                delay(10000) // Poll every 10 seconds

                // 1. Check for Auto-Catch
                val catchEvent = engine.performAutoCatchTick()
                when (catchEvent) {
                    is org.cleanroom.pokemongo.data.automation.AutoCatchEvent.Captured -> {
                        sendEventNotification(
                            "Pokémon Caught!",
                            "Auto-Catcher caught a ${catchEvent.speciesName} (CP ${catchEvent.cp}) using ${catchEvent.ballUsed.displayName}!"
                        )
                    }
                    is org.cleanroom.pokemongo.data.automation.AutoCatchEvent.Escaped -> {
                        sendEventNotification(
                            "Pokémon Escaped",
                            "A wild ${catchEvent.speciesName} ran away."
                        )
                    }
                    is org.cleanroom.pokemongo.data.automation.AutoCatchEvent.OutOfBalls -> {
                        sendEventNotification(
                            "Out of Poké Balls",
                            "Auto-Catcher is out of Poké Balls in your Bag."
                        )
                    }
                    null, is org.cleanroom.pokemongo.data.automation.AutoCatchEvent.BagFull -> {}
                }

                // 2. Check for Auto-Spin
                val spinEvent = engine.performAutoSpinTick()
                when (spinEvent) {
                    is org.cleanroom.pokemongo.data.automation.AutoSpinEvent.Spun -> {
                        sendEventNotification(
                            "PokéStop Spun",
                            "Auto-spun ${spinEvent.pokestopName}: +${spinEvent.itemsCount} items, +${spinEvent.xp} XP!"
                        )
                    }
                    null -> {}
                }

                // 3. Check for Auto-Feed
                val feedEvent = engine.performAutoFeedTick()
                when (feedEvent) {
                    is org.cleanroom.pokemongo.data.automation.AutoFeedEvent.Fed -> {
                        sendEventNotification(
                            "Buddy Fed",
                            "Auto-fed your buddy with ${feedEvent.berryUsed.displayName}! Hunger is now ${feedEvent.newHunger}%."
                        )
                    }
                    null -> {}
                }
            }
        }
    }

    private fun sendEventNotification(title: String, message: String) {
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(title)
            .setContentText(message)
            .setSmallIcon(android.R.drawable.stat_notify_chat)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .build()
        notificationManager.notify((2000..3000).random(), notification)
    }

    private fun createNotification(title: String, content: String): Notification {
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(title)
            .setContentText(content)
            .setSmallIcon(android.R.drawable.stat_sys_data_bluetooth)
            .setOngoing(true)
            .build()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Pokémon GO Plus Companion",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Companion accessory auto-catcher and spinner notifications"
            }
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    override fun onDestroy() {
        autocatcherJob?.cancel()
        serviceScope.cancel()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    companion object {
        const val CHANNEL_ID = "pokemon_go_plus_channel"
        const val NOTIFICATION_ID = 1002

        fun start(context: Context) {
            val intent = Intent(context, PokemonGoPlusService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stop(context: Context) {
            val intent = Intent(context, PokemonGoPlusService::class.java)
            context.stopService(intent)
        }
    }
}
