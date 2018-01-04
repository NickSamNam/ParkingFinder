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
private const val CHANNEL_ID = "COMEWITHMEIFYOUWANTTOLIVE"

class PushNotifications {
    companion object {
        fun notifyUser(context: Context) {
            val intent = Intent(context, ParkingDetailActivity::class.java)
            intent.putExtra(KEY_PARKING_ID, context.getSharedPreferences(KEY_PARKING, Context.MODE_PRIVATE).getString(KEY_PARKING_ID, ""))
            val contentIntent = PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT)

            val notification = NotificationCompat.Builder(context, CHANNEL_ID)

            notification
                    .setDefaults(Notification.DEFAULT_ALL)
                    .setWhen(System.currentTimeMillis())
                    .setSmallIcon(R.drawable.ic_parking)
                    .setContentTitle(context.getString(R.string.notification_title))
                    .setContentText(context.getString(R.string.notification_text))
                    .setContentIntent(contentIntent)
                    .setAutoCancel(true)

            val nManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            nManager.notify(1, notification.build())
        }
    }
}