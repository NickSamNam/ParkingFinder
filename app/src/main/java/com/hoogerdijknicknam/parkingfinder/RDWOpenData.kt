package com.hoogerdijknicknam.parkingfinder

import com.android.volley.Request
import com.android.volley.RequestQueue
import com.android.volley.Response
import com.android.volley.toolbox.JsonArrayRequest
import org.json.JSONArray

/**
 * Created by snick on 15-12-2017.
 */

private const val API_URL = "https://opendata.rdw.nl/resource/"
private const val API_RESOURCE_GEBIED = "8u4d-s4q7.json"
private const val API_QUERY_GEBIED = "?\$query=SELECT areaid, areadesc, usageid"
private const val API_RESOURCE_GEOMETRIE_GEBIED = "e6d2-rh45.json"
private const val API_RESOURCE_GEO_PARKEER_GARAGES = "7m7r-i29f.json"
private const val API_RESOURCE_GEO_PENR = "mtdk-3sae.json"
private const val API_RESOURCE_GEO_CARPOOL = "d85t-gfew.json"
private const val USAGE_ID_GARAGE = "GARAGEP"
private const val USAGE_ID_P_EN_R = "PARKRIDE"
private const val USAGE_ID_CARPOOL = "CARPOOL"

class RDWOpenDataRetriever(private val requestQueue: RequestQueue) {
    fun retrieveAllParking(parkingRetrievalListener: ParkingRetrievalListener) {
        retrieveTableGebied(API_QUERY_GEBIED, Response.Listener { response ->
            val parkings = ArrayList<Parking>()
            forEachGebied@ for (i in 0 until response.length()) {
                val gebied = response.getJSONObject(i)
                val areaid = gebied.getString("areaid")
                val areadesc = gebied.getString("areadesc")
                val usageid = gebied.getString("usageid")

                retrieveGebiedLocation(areaid, usageid, object : LocationRetrievalListener {
                    override fun onSuccessful(location: LatLng) {
                        parkings.add(Parking(areadesc, location, 0f, null, null, null))
                    }

                    override fun onFailed() {
                        return@onFailed
                    }
                })
            }
        }, Response.ErrorListener { parkingRetrievalListener.onFailed() })
    }

    private fun retrieveGebiedLocation(areaid: String, usageid: String, locationRetrievalListener: LocationRetrievalListener) {
        val query = "?\$query=SELECT location WHERE areaid=$areaid"
        when (usageid) {
            USAGE_ID_GARAGE -> retrieveTableParkeerGarages(query, Response.Listener {

            }, Response.ErrorListener { locationRetrievalListener.onFailed() })

            USAGE_ID_P_EN_R -> retrieveTableParkeerGarages(query, Response.Listener {

            }, Response.ErrorListener { locationRetrievalListener.onFailed() })

            USAGE_ID_CARPOOL -> retrieveTableCarpool(query, Response.Listener {

            }, Response.ErrorListener { locationRetrievalListener.onFailed() })

            else -> retrieveGebiedGeometrie(areaid, Response.Listener {

            }, Response.ErrorListener { locationRetrievalListener.onFailed() })
        }
    }

    private fun retrieveGebiedGeometrie(areaid: String, responseListener: Response.Listener<JSONArray>, errorListener: Response.ErrorListener) {
        retrieveTableGeometrieGebied("?\$query=SELECT areageometryastext WHERE areaid=$areaid", responseListener, errorListener)
    }

    private fun retrieveTableGebied(query: String = "", responseListener: Response.Listener<JSONArray>, errorListener: Response.ErrorListener) {
        retrieveTable(API_RESOURCE_GEBIED + query, responseListener, errorListener)
    }

    private fun retrieveTableGeometrieGebied(query: String = "", responseListener: Response.Listener<JSONArray>, errorListener: Response.ErrorListener) {
        retrieveTable(API_RESOURCE_GEOMETRIE_GEBIED + query, responseListener, errorListener)
    }

    private fun retrieveTableParkeerGarages(query: String = "", responseListener: Response.Listener<JSONArray>, errorListener: Response.ErrorListener) {
        retrieveTable(API_RESOURCE_GEO_PARKEER_GARAGES + query, responseListener, errorListener)
    }

    private fun retrieveTableGeoPenR(query: String = "", responseListener: Response.Listener<JSONArray>, errorListener: Response.ErrorListener) {
        retrieveTable(API_RESOURCE_GEO_PENR + query, responseListener, errorListener)
    }

    private fun retrieveTableCarpool(query: String = "", responseListener: Response.Listener<JSONArray>, errorListener: Response.ErrorListener) {
        retrieveTable(API_RESOURCE_GEO_CARPOOL + query, responseListener, errorListener)
    }

    private fun retrieveTable(resource: String, responseListener: Response.Listener<JSONArray>, errorListener: Response.ErrorListener) {
        val request = JsonArrayRequest(Request.Method.GET, API_URL + resource, null, responseListener, errorListener)
        requestQueue.add(request)
    }

    interface ParkingRetrievalListener {
        fun onSuccesful(parkings: List<Parking>)
        fun onFailed()
    }

    private interface LocationRetrievalListener {
        fun onSuccessful(location: LatLng)
        fun onFailed()
    }
}