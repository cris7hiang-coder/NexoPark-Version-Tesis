package com.example.parking.Login

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.firestore
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.example.parking.R
class MyFirebaseMessagingService : FirebaseMessagingService() {

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return
        Firebase.firestore.collection("users")
            .document(uid)
            .update("fcmToken", token)
            .addOnSuccessListener {
                Log.d("FCM", "Token FCM actualizado correctamente en users/$uid")
            }
            .addOnFailureListener { e ->
                Log.e("FCM", "Error guardando token: ${e.message}")
            }
    }


    override fun onMessageReceived(message: RemoteMessage) {
        val title = message.notification?.title ?: "Aviso"
        val body = message.notification?.body ?: ""

        // Crear canal si no existe
        createNotificationChannel(applicationContext)

        // Mostrar notificación
        showNotification(title, body)
    }

    private fun showNotification(title: String, body: String) {
        val channelId = "reservas_channel"
        val builder = NotificationCompat.Builder(applicationContext, channelId)
            .setSmallIcon(R.drawable.ic_parking_marker)
            .setContentTitle(title)
            .setContentText(body)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) return

        NotificationManagerCompat.from(applicationContext)
            .notify(System.currentTimeMillis().toInt(), builder.build())
    }

    private fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channelId = "reservas_channel"
            val name = "Reservas"
            val descriptionText = "Notificaciones de reservas"
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel(channelId, name, importance).apply {
                description = descriptionText
            }
            val notificationManager =
                context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }
}

