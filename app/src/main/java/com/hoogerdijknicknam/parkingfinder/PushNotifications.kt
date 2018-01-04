package com.hoogerdijknicknam.parkingfinder

import android.app.Notification
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.support.v4.app.NotificationCompat


/**
 * Created by Ricky on 04/01/2018.
 */
class PushNotifications {


    fun notifyUser(context: Context){
        val intent = Intent(context, MapsActivity::class.java)
        val contentIntent = PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT)

        val notification = NotificationCompat.Builder(context)

        notification.setDefaults(Notification.DEFAULT_ALL)
                .setWhen(System.currentTimeMillis())
                .setSmallIcon(R.drawable.ic_notifications)
                .setTicker("Something")
                .setContentTitle("Test")
                .setContentText("Lorem ipsum dolor sit amet, consectetuer adipiscing elit. Aenean commodo ligula eget dolor. Aenean massa.")
                .setDefaults(Notification.DEFAULT_LIGHTS)
                .setContentIntent(contentIntent)
                .setContentInfo("Info")

        val nManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        nManager.notify(1, notification.build())
    }
}