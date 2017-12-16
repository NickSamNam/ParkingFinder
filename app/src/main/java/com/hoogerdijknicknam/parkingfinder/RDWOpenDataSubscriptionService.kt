package com.hoogerdijknicknam.parkingfinder

import com.android.volley.RequestQueue

/**
 * Created by snick on 16-12-2017.
 */
object RDWOpenDataSubscriptionService {
    private var started = false
    private val subscribers = ArrayList<Subscription>()
    val backLog = ArrayList<Parking>()

    fun start(requestQueue: RequestQueue) {
        if (started) return
        val retriever = RDWOpenDataRetriever(requestQueue)
        retriever.requestParking(object : RDWOpenDataRetriever.ParkingRequestListener {
            override fun onReceived(parking: Parking) {
                backLog += parking
                subscribers.forEach { it.onReceive(parking) }
            }
        })
        started = true
    }

    fun subscribe(subscription: Subscription) {
        subscribers += subscription
    }

    fun unsubscribe(subscription: Subscription) {
        subscribers -= subscription
    }

    interface Subscription {
        fun onReceive(parking: Parking)
    }
}