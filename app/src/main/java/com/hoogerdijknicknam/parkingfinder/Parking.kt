package com.hoogerdijknicknam.parkingfinder

import android.os.Parcel
import android.os.Parcelable
import java.util.*

/**
 * Created by Ricky on 13/12/2017.
 */

data class Parking(val areaDesc: String, val location: LatLng, val price: Float, val startTime: Date, val endTime: Date, val geoData: List<LatLng>) : Parcelable {
    constructor(parcel: Parcel) : this(
            parcel.readString(),
            parcel.readParcelable<LatLng>(LatLng::class.java.classLoader),
            parcel.readFloat(),
            Date(parcel.readLong()),
            Date(parcel.readLong()),
            parcel.createTypedArrayList(LatLng.CREATOR))

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeString(areaDesc)
        parcel.writeParcelable(location, 0)
        parcel.writeFloat(price)
        parcel.writeLong(startTime.time)
        parcel.writeLong(endTime.time)
        parcel.writeTypedList(geoData)
    }

    override fun describeContents() = 0

    companion object CREATOR : Parcelable.Creator<Parking> {
        override fun createFromParcel(parcel: Parcel) = Parking(parcel)

        override fun newArray(size: Int): Array<Parking?> = arrayOfNulls(size)
    }
}

data class LatLng(val latitude: Double, val longitude: Double) : Parcelable {
    constructor(parcel: Parcel) : this(
            parcel.readDouble(),
            parcel.readDouble())

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeDouble(latitude)
        parcel.writeDouble(longitude)
    }

    override fun describeContents() = 0

    companion object CREATOR : Parcelable.Creator<LatLng> {
        override fun createFromParcel(parcel: Parcel) = LatLng(parcel)

        override fun newArray(size: Int): Array<LatLng?> = arrayOfNulls(size)
    }
}