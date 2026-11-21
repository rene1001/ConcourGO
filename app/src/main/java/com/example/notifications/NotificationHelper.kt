package com.example.notifications

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.example.MainActivity
import com.example.R

object NotificationHelper {

    const val CHANNEL_ID = "concoursgo_daily"
    private const val NOTIFICATION_ID = 2025

    fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = context.getString(R.string.notif_channel_name)
            val descriptionText = context.getString(R.string.notif_channel_desc)
            val channel = NotificationChannel(
                CHANNEL_ID,
                name,
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = descriptionText
            }
            val notificationManager =
                context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    fun hasNotificationPermission(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
    }

    fun showMotivationalNotification(
        context: Context,
        streak: Int,
        totalXp: Int,
        isTest: Boolean = false
    ): Boolean {
        createNotificationChannel(context)
        if (!hasNotificationPermission(context)) return false

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val (title, body) = buildMotivationalCopy(context, streak, totalXp, isTest)

        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)

        return try {
            NotificationManagerCompat.from(context).notify(NOTIFICATION_ID, builder.build())
            true
        } catch (_: SecurityException) {
            false
        }
    }

    private fun buildMotivationalCopy(
        context: Context,
        streak: Int,
        totalXp: Int,
        isTest: Boolean
    ): Pair<String, String> {
        if (isTest) {
            return "Rappel ConcoursGo activé !" to
                "Tu as $totalXp XP et une série de $streak jour(s). Continue sur ta lancée pour décrocher ton concours !"
        }
        val messages = when {
            streak >= 3 -> listOf(
                "Protège ta série de $streak jours !" to
                    "Une seule série de 6 QCM aujourd'hui suffit pour garder ta flamme et grimper dans le classement ($totalXp XP).",
                "Série en feu : $streak jours d'affilée !" to
                    "Révise 5 minutes sur ConcoursGo et reste en tête pour les concours directs."
            )
            streak in 1..2 -> listOf(
                "Continue ta série ($streak j) !" to
                    "Tu es à $totalXp XP. Fais un niveau de QCM aujourd'hui pour bâtir une habitude gagnante.",
                "Rendez-vous révision ConcoursGo" to
                    "Actualités AES, droit, sciences ou logique : choisis ton prochain niveau maintenant."
            )
            else -> listOf(
                context.getString(R.string.notif_default_title) to
                    context.getString(R.string.notif_default_body),
                "Un peu de QCM chaque jour !" to
                    "Lance une session rapide (même hors-ligne) pour cumuler tes premiers XP."
            )
        }
        val index = ((System.currentTimeMillis() / 86_400_000L) % messages.size).toInt()
        return messages[index]
    }
}
