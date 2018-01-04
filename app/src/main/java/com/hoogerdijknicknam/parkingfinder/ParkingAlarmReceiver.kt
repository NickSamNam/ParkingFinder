package com.hoogerdijknicknam.parkingfinder

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log

/**
 * Created by snick on 4-1-2018.
 */
class ParkingAlarmReceiver: BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent?) {
        Log.i("ALARM", "IM WOKE")
    }
}