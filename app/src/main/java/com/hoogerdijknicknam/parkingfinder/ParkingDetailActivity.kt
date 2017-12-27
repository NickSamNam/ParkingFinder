package com.hoogerdijknicknam.parkingfinder

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import kotlinx.android.synthetic.main.activity_parking_detail.*

const val KEY_PARKING = "PARKING"
const val KEY_PARKING_ID = "PARKING_ID"

class ParkingDetailActivity : AppCompatActivity() {
    private var parking: Parking? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_parking_detail)

        parking = RDWOpenDataSubscriptionService.backLog.find { it.areaId == intent.extras.getString(KEY_PARKING_ID) }

        val subscribeBtn: Button = findViewById(R.id.parkingDetail_SubscribeBtn)

        if (subscribeBtn.tag == null) {
            subscribeBtn.tag = isLocationSaved()
            if (subscribeBtn.tag as Boolean) {
                subscribeBtn.setText(R.string.unsubscribe)
            } else {
                subscribeBtn.setText(R.string.Subscribe)
            }
        }

        subscribeBtn.setOnClickListener {
            if (subscribeBtn.tag == false) {
                if (saveLocation()) {
                    subscribeBtn.tag = true
                    subscribeBtn.setText(R.string.unsubscribe)
                } else {
                    Toast.makeText(this, R.string.toast_operation_fail, Toast.LENGTH_SHORT).show()
                }
            } else {
                if (unsaveLocation()) {
                    subscribeBtn.tag = false
                    subscribeBtn.setText(R.string.Subscribe)
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

    private fun saveLocation(): Boolean {
        val prefs = getSharedPreferences(KEY_PARKING, Context.MODE_PRIVATE)
        return prefs.edit().putString(KEY_PARKING_ID, parking?.areaId).commit()
    }

    private fun unsaveLocation(): Boolean {
        val prefs = getSharedPreferences(KEY_PARKING, Context.MODE_PRIVATE)
        return prefs.edit().remove(KEY_PARKING_ID).commit()
    }

    private fun isLocationSaved(): Boolean {
        val prefs = getSharedPreferences(KEY_PARKING, Context.MODE_PRIVATE)
        return prefs.getString(KEY_PARKING_ID, "") == parking?.areaId
    }

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
