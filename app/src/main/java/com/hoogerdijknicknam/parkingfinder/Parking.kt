package com.hoogerdijknicknam.parkingfinder

import android.graphics.Point
import android.os.Parcel
import android.os.Parcelable
import java.util.*

/**
 * Created by Ricky on 13/12/2017.
 */

// Pair doubles: first double: longitude, second double = latitude.
data class Parking(val areaDesc: String, val longitude: Double, val latitude: Double, val price: Float, val startTime: Date, val endTime: Date, val geoData: List<Pair<Double, Double>>) : Parcelable {
    constructor(parcel: Parcel) : this(
            parcel.readString(),
            parcel.readDouble(),
            parcel.readDouble(),
            parcel.readFloat(),
            TODO("startTime"),
            TODO("endTime"),
            TODO("geoData")) {
    }

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeLong(startTime.time)
        parcel.writeLong(endTime.time)
        parcel.writeString(areaDesc)
        parcel.writeDouble(longitude)
        parcel.writeDouble(latitude)
        parcel.writeFloat(price)
    }

    override fun describeContents(): Int {
        return 0
    }

    companion object CREATOR : Parcelable.Creator<Parking> {
        override fun createFromParcel(parcel: Parcel): Parking {
            return Parking(parcel)
        }

        override fun newArray(size: Int): Array<Parking?> {
            return arrayOfNulls(size)
        }
    }


}