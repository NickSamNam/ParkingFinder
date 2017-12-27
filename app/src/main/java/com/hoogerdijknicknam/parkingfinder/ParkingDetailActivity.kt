package com.hoogerdijknicknam.parkingfinder

import android.content.Intent
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import kotlinx.android.synthetic.main.activity_parking_detail.*

const val KEY_PARKING = "PARKING"

class ParkingDetailActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_parking_detail)


        val subscribeBtn: Button = findViewById(R.id.parkingDetail_SubscribeBtn)
        subscribeBtn.setOnClickListener() {
            Toast.makeText(this, "It works i think", Toast.LENGTH_SHORT).show()
        }

        val parking = RDWOpenDataSubscriptionService.backLog.find { it.areaId == intent.extras.getString(KEY_PARKING) }
        if (parking != null) {
            parkingDetail_titleTV.text = parking.areaDesc
        }
        val directionsBtn: Button = findViewById(R.id.parkingDetail_routeBtn)
        directionsBtn.setOnClickListener() {
            val intent = Intent(this, MapsActivity::class.java)
            intent.putExtra(KEY_PARKING, parking?.areaId)
            startActivity(intent)
        }
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
