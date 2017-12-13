package com.hoogerdijknicknam.parkingfinder

import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast

class ParkingDetailActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_parking_detail)

        val directionsBtn: Button = findViewById(R.id.parkingDetail_routeBtn)
        directionsBtn.setOnClickListener(){
            Toast.makeText(this, "It works i think", Toast.LENGTH_SHORT).show()
        }

        val subscribeBtn: Button = findViewById(R.id.parkingDetail_SubscribeBtn)
        subscribeBtn.setOnClickListener(){
            Toast.makeText(this, "It works i think", Toast.LENGTH_SHORT).show()
        }
    }

    fun setTitle(title: String){
        val titleTV: TextView = findViewById(R.id.parkingDetail_titleTV)
        titleTV.setText(title)
    }

    fun setAddress(address: String){
        val addressTV: TextView = findViewById(R.id.parkingDetail_addressTV)
        addressTV.setText(address)
    }

    fun setOpening(open: String){
        val openTV: TextView = findViewById(R.id.parkingDetail_openingTimeTV)
        openTV.setText(open)
    }

    fun setImage(image: String){
        val imageV: ImageView = findViewById(R.id.parkingDetail_Img)
    }
}
