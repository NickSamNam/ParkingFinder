package com.hoogerdijknicknam.parkingfinder

import android.app.AlarmManager
import android.app.PendingIntent
import android.app.TimePickerDialog
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import kotlinx.android.synthetic.main.activity_parking_detail.*
import java.util.*

const val KEY_PARKING = "PARKING"
const val KEY_PARKING_ID = "PARKING_ID"
const val KEY_NOTIFY = "NOTIFY"
const val KEY_TIME_HOUR = "TIME_HOUR"
const val KEY_TIME_MINUTE = "TIME_MINUTE"
const val TAG_TIME_PICKER_DIALOG_FRAGMENT = "TPDF"
const val TAG_ALARM_LEAVE = "LEAVE_NOW"

class ParkingDetailActivity : AppCompatActivity() {
    private var parking: Parking? = null
    private lateinit var prefs: SharedPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_parking_detail)

        parking = RDWOpenDataSubscriptionService.backLog.find { it.areaId == intent.extras.getString(KEY_PARKING_ID) }
        prefs = getSharedPreferences(KEY_PARKING, Context.MODE_PRIVATE)

        val subscribeBtn: Button = findViewById(R.id.parkingDetail_SubscribeBtn)
        val tpdf = TimePickerDialogFragment()
        tpdf.addTimeSetListener(TimePickerDialog.OnTimeSetListener { view, hourOfDay, minute ->
            if (saveLocation() && saveTime(hourOfDay, minute)) {
                subscribeBtn.tag = true
                subscribeBtn.setText(R.string.unsubscribe)
                parkingDetail_tv_time.text = "%02d:%02d".format(hourOfDay, minute)
                parkingDetail_btn_notify.visibility = View.VISIBLE
                if (parkingDetail_btn_notify.tag as Boolean) {
                    parkingDetail_btn_notify.setImageResource(R.drawable.ic_notifications_active)
                } else {
                    parkingDetail_btn_notify.setImageResource(R.drawable.ic_notifications)
                }
            } else {
                Toast.makeText(this, R.string.toast_operation_fail, Toast.LENGTH_SHORT).show()
            }
        })

        if (parkingDetail_btn_notify.tag == null) {
            parkingDetail_btn_notify.tag = isNotifyOn()
        }
        if (subscribeBtn.tag == null) {
            subscribeBtn.tag = isLocationSaved()
            if (subscribeBtn.tag as Boolean) {
                subscribeBtn.setText(R.string.unsubscribe)
                val time = loadTime()
                if (time.first >= 0 && time.second >= 0) {
                    parkingDetail_tv_time.text = "%02d:%02d".format(time.first, time.second)
                }
                parkingDetail_btn_notify.visibility = View.VISIBLE
                if (parkingDetail_btn_notify.tag as Boolean) {
                    parkingDetail_btn_notify.setImageResource(R.drawable.ic_notifications_active)
                } else {
                    parkingDetail_btn_notify.setImageResource(R.drawable.ic_notifications)
                }
            } else {
                subscribeBtn.setText(R.string.Subscribe)
                parkingDetail_btn_notify.visibility = View.GONE
            }
        }

        parkingDetail_btn_notify.setOnClickListener {
            parkingDetail_btn_notify.tag = !(parkingDetail_btn_notify.tag as Boolean)
            val alarmMgr = getSystemService(Context.ALARM_SERVICE) as AlarmManager
            val intent = Intent(this, ParkingAlarmReceiver::class.java)
            val pendingIntent = PendingIntent.getBroadcast(this, 0, intent, 0)
            if (parkingDetail_btn_notify.tag as Boolean) {
                val time = loadTime()
                if (time.first >= 0 && time.second >= 0 && notifyOn()) {
                    val cal = Calendar.getInstance()
                    cal.set(Calendar.HOUR_OF_DAY, time.first)
                    cal.set(Calendar.MINUTE, time.second)
                    cal.set(Calendar.SECOND, 0)
                    alarmMgr.set(AlarmManager.RTC_WAKEUP, cal.timeInMillis, TAG_ALARM_LEAVE, AlarmManager.OnAlarmListener {
                        notifyOff(); parkingDetail_btn_notify.setImageResource(R.drawable.ic_notifications); pendingIntent.send()
                    }, null)
                    Log.i("ALARM", "SET")
                    parkingDetail_btn_notify.setImageResource(R.drawable.ic_notifications_active)
                    Toast.makeText(this, R.string.toast_notify_on, Toast.LENGTH_LONG).show()
                } else {
                    Toast.makeText(this, R.string.toast_operation_fail, Toast.LENGTH_SHORT).show()
                }
            } else {
                if (notifyOff()) {
                    parkingDetail_btn_notify.setImageResource(R.drawable.ic_notifications)
                    alarmMgr.cancel(pendingIntent)
                } else {
                    Toast.makeText(this, R.string.toast_operation_fail, Toast.LENGTH_SHORT).show()
                }
            }
        }

        subscribeBtn.setOnClickListener {
            if (subscribeBtn.tag == false) {
                tpdf.show(supportFragmentManager, TAG_TIME_PICKER_DIALOG_FRAGMENT)
            } else {
                if (unsaveLocation()) {
                    subscribeBtn.tag = false
                    subscribeBtn.setText(R.string.Subscribe)
                    parkingDetail_tv_time.text = ""
                    parkingDetail_btn_notify.tag = false
                    parkingDetail_btn_notify.visibility = View.GONE
                    val alarmMgr = getSystemService(Context.ALARM_SERVICE) as AlarmManager
                    val intent = Intent(this, ParkingAlarmReceiver::class.java)
                    val pendingIntent = PendingIntent.getBroadcast(this, 0, intent, 0)
                    alarmMgr.cancel(pendingIntent)
                } else {
                    Toast.makeText(this, R.string.toast_operation_fail, Toast.LENGTH_SHORT).show()
                }
            }
        }

        parkingDetail_titleTV.text = parking?.areaDesc

        val directionsBtn: Button = findViewById(R.id.parkingDetail_routeBtn)
        directionsBtn.setOnClickListener {
            val intent = Intent(this, MapsActivity::class.java)
            intent.putExtra(KEY_PARKING, parking?.areaId)
            startActivity(intent)
        }
    }

    private fun saveLocation() = prefs.edit().putString(KEY_PARKING_ID, parking?.areaId).commit()

    private fun unsaveLocation() = prefs.edit().remove(KEY_PARKING_ID).commit()

    private fun isLocationSaved() = prefs.getString(KEY_PARKING_ID, "") == parking?.areaId

    private fun notifyOn() = prefs.edit().putBoolean(KEY_NOTIFY, true).commit()

    private fun notifyOff() = prefs.edit().putBoolean(KEY_NOTIFY, false).commit()

    private fun isNotifyOn() = if (prefs.getString(KEY_PARKING_ID, "") == parking?.areaId) prefs.getBoolean(KEY_NOTIFY, false) else false

    private fun saveTime(hour: Int, minute: Int) = prefs.edit().putInt(KEY_TIME_HOUR, hour).putInt(KEY_TIME_MINUTE, minute).commit()

    private fun loadTime() = Pair(prefs.getInt(KEY_TIME_HOUR, -1), prefs.getInt(KEY_TIME_MINUTE, -1))

    fun setTitle(title: String) {
        val titleTV: TextView = findViewById(R.id.parkingDetail_titleTV)
        titleTV.text = title
    }

    fun setAddress(address: String) {
        val addressTV: TextView = findViewById(R.id.parkingDetail_addressTV)
        addressTV.text = address
    }

    fun setOpening(open: String) {
        val openTV: TextView = findViewById(R.id.parkingDetail_openingTimeTV)
        openTV.text = open
    }

    fun setImage(image: String) {
        val imageV: ImageView = findViewById(R.id.parkingDetail_Img)
    }
}
