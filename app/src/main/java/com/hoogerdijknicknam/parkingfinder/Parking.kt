package com.hoogerdijknicknam.parkingfinder

import android.os.Parcel
import android.os.ParcelFormatException
import android.os.Parcelable
import com.hoogerdijknicknam.parkingfinder.Support.DayOfWeek
import java.util.*

/**
 * Created by Ricky on 13/12/2017.
 */

data class Parking(val areaId: String, val areaDesc: String, val location: LatLng, val area: List<LatLng>?) : Parcelable {
    var openHours: Map<DayOfWeek, Pair<Date, Date>>? = null

    constructor(parcel: Parcel) : this(
            parcel.readString(),
            parcel.readString(),
            parcel.readParcelable<LatLng>(LatLng::class.java.classLoader),
            try {
                parcel.createTypedArrayList(LatLng.CREATOR)
            } catch (e: ParcelFormatException) {
                null
            }) {
        try {
            parcel.readMap(openHours, Date::class.java.classLoader)
        } catch (e: ParcelFormatException) {

        }
    }

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeString(areaId)
        parcel.writeString(areaDesc)
        parcel.writeParcelable(location, 0)
        if (area != null) parcel.writeTypedList(area)
        if (openHours != null) parcel.writeMap(openHours)
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

fun Iterable<com.hoogerdijknicknam.parkingfinder.LatLng>.average(): LatLng {
    val avgLat = this.map { it -> it.latitude }.average()
    val avgLng = this.map { it -> it.longitude }.average()
    return LatLng(avgLat, avgLng)
}