package org.cleanroom.pokemongo.service

import android.app.*
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import kotlinx.coroutines.*
import org.cleanroom.pokemongo.data.repository.GameStateRepository

class AdventureSyncService : Service() {

    private val serviceScope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    private var trackingJob: Job? = null

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val notification = createNotification("Adventure Sync is active", "Tracking distance for eggs and buddy")
        startForeground(NOTIFICATION_ID, notification)

        startDistanceTracking()
        return START_STICKY
    }

    private fun startDistanceTracking() {
        trackingJob?.cancel()
        trackingJob = serviceScope.launch {
            val repo = GameStateRepository.getInstance(applicationContext)
            while (isActive) {
                delay(10000) // 10 seconds interval matching reverse engineered LocationForegroundService

                // Accumulate distance (simulating real walking ~0.025 km per 10s = 9 km/h)
                val deltaKm = 0.025f
                repo.addWalkingDistance(deltaKm)

                val buddy = repo.buddy.value
                val eggs = repo.eggs.value
                val incubating = eggs.filter { it.incubatorId != null && !it.isHatched }

                val statusText = "Walked: %.2f km | Active Eggs: %d".format(
                    repo.trainerProfile.value.totalKmWalked,
                    incubating.size
                )
                updateNotification(statusText)

                // Check if any egg reached ready to hatch
                if (repo.pendingHatchEgg.value != null) {
                    sendHatchNotification()
                }
            }
        }
    }

    private fun updateNotification(content: String) {
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val notification = createNotification("Adventure Sync Active", content)
        notificationManager.notify(NOTIFICATION_ID, notification)
    }

    private fun sendHatchNotification() {
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Oh?")
            .setContentText("An Egg is ready to hatch! Tap to reveal your Pokémon.")
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build()
        notificationManager.notify(2001, notification)
    }

    private fun createNotification(title: String, content: String): Notification {
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(title)
            .setContentText(content)
            .setSmallIcon(android.R.drawable.ic_dialog_map)
            .setOngoing(true)
            .build()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Adventure Sync Service",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Foreground tracking of walking distance for Pokémon GO Adventure Sync"
            }
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    override fun onDestroy() {
        serviceScope.cancel()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    companion object {
        const val CHANNEL_ID = "adventure_sync_channel"
        const val NOTIFICATION_ID = 1001

        fun start(context: Context) {
            val intent = Intent(context, AdventureSyncService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stop(context: Context) {
            val intent = Intent(context, AdventureSyncService::class.java)
            context.stopService(intent)
        }
    }
}
