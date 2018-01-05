package com.hoogerdijknicknam.parkingfinder

import com.android.volley.Request
import com.android.volley.RequestQueue
import com.android.volley.Response
import com.android.volley.toolbox.JsonArrayRequest
import org.json.JSONArray
import org.json.JSONException

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
    fun requestParking(parkingRequestListener: ParkingRequestListener) {
        retrieveTableGebied(API_QUERY_GEBIED, Response.Listener { response ->
            stage1(response, parkingRequestListener)
        }, Response.ErrorListener { return@ErrorListener })
    }

    fun addOpenHours(parking: Parking, parkingRequestListener: ParkingRequestListener) {
        // todo implement
        parkingRequestListener.onReceived(parking)
    }

    fun addCosts(parking: Parking, parkingRequestListener: ParkingRequestListener) {
        // todo implement
        parkingRequestListener.onReceived(parking)
    }

    /**
     * Retrieve GEBIED
     */
    private fun stage1(response: JSONArray, parkingRequestListener: ParkingRequestListener) {
        forEachGebied@ for (i in 0 until response.length()) {
            val jGebied = response.getJSONObject(i)

            val areaid = jGebied.getString("areaid")
            val areadesc = try {
                jGebied.getString("areadesc")
            } catch (e: JSONException) {
                ""
            }
            val usageid = jGebied.getString("usageid")

            stage2(areaid, areadesc, usageid, parkingRequestListener)
        }
    }

    /**
     * Retrieve location
     */
    private fun stage2(areaid: String, areadesc: String, usageid: String, parkingRequestListener: ParkingRequestListener) {
        val query = "?\$query=SELECT location WHERE areaid=\"$areaid\""
        when (usageid) {
            USAGE_ID_GARAGE -> retrieveTableGeoParkeerGarages(query, Response.Listener { response ->
                if (response.length() <= 0) return@Listener
                val location = geoResultToLatLng(response)
                stage3(areaid, areadesc, location, parkingRequestListener)
            }, Response.ErrorListener { return@ErrorListener })

            USAGE_ID_P_EN_R -> retrieveTableGeoPenR(query, Response.Listener { response ->
                if (response.length() <= 0) return@Listener
                val location = geoResultToLatLng(response)
                stage3(areaid, areadesc, location, parkingRequestListener)
            }, Response.ErrorListener { return@ErrorListener })

            USAGE_ID_CARPOOL -> retrieveTableGeoCarpool(query, Response.Listener { response ->
                if (response.length() <= 0) return@Listener
                val location = geoResultToLatLng(response)
                stage3(areaid, areadesc, location, parkingRequestListener)
            }, Response.ErrorListener { return@ErrorListener })

            else -> retrieveGebiedGeometrie(areaid, object : AreaRetrievalListener {
                override fun onSuccessful(area: List<LatLng>) {
                    stage4(areaid, areadesc, area.average(), area, parkingRequestListener)
                }

                override fun onFailed() {
                    return
                }
            })
        }
    }

    /**
     * Retrieve area
     */
    private fun stage3(areaid: String, areadesc: String, location: LatLng, parkingRequestListener: ParkingRequestListener) {
        retrieveGebiedGeometrie(areaid, object : AreaRetrievalListener {
            override fun onSuccessful(area: List<LatLng>) {
                stage4(areaid, areadesc, location, area, parkingRequestListener)
            }

            override fun onFailed() {
                stage4(areaid, areadesc, location, null, parkingRequestListener)
            }
        })
    }

    private fun stage4(areaid: String, areadesc: String, location: LatLng, area: List<LatLng>?, parkingRequestListener: ParkingRequestListener) {
        parkingRequestListener.onReceived(Parking(areaid, areadesc, location, area))
    }

    private fun geoResultToLatLng(response: JSONArray): LatLng {
        val jLocation = response.getJSONObject(0).getJSONObject("location").getJSONArray("coordinates")
        return LatLng(jLocation.getDouble(1), jLocation.getDouble(0))
    }

    private fun retrieveGebiedGeometrie(areaid: String, areaRetrievalListener: AreaRetrievalListener) {
        retrieveTableGeometrieGebied("?\$query=SELECT areageometryastext WHERE areaid=\"$areaid\"", Response.Listener { response ->
            if (response.length() <= 0) {
                areaRetrievalListener.onFailed()
                return@Listener
            }
            val geoString: String
            try {
                geoString = response.getJSONObject(0).getString("areageometryastext")
            } catch (e: JSONException) {
                areaRetrievalListener.onFailed()
                return@Listener
            }
            val area = ArrayList<LatLng>()
            when {
                geoString.startsWith("POINT") -> {
                    val c = geoString.removePrefix("POINT").removeSurrounding("(", ")").split(" ")
                    try {
                        area.add(stringsToLatLng(c))
                    } catch (e: NumberFormatException) {
                        areaRetrievalListener.onFailed()
                        return@Listener
                    }
                }
                geoString.startsWith("POLYGON") -> {
                    val cs = geoString.removePrefix("POLYGON").removeSurrounding("((", "))").split(", ")
                    cs.forEach { it ->
                        val c = it.split(" ")
                        try {
                            area.add(stringsToLatLng(c))
                        } catch (e: NumberFormatException) {
                        }
                    }
                }
                geoString.startsWith("MULTIPOLYGON") -> {
                    val css = geoString.removePrefix("MULTIPOLYGON").removeSurrounding("(", ")").split("), (")
                    css.forEach { it ->
                        var cs = it.replace("(", "")
                        cs = it.replace(")", "")
                        cs.split(", ").forEach { itAgain ->
                            val c = itAgain.split(" ")
                            try {
                                area.add(stringsToLatLng(c))
                            } catch (e: NumberFormatException) {

                            }
                        }
                    }
                }
                else -> areaRetrievalListener.onFailed()
            }
            if (area.size > 0)
                areaRetrievalListener.onSuccessful(area)
            else
                areaRetrievalListener.onFailed()
        }, Response.ErrorListener { areaRetrievalListener.onFailed() })
    }

    private fun stringsToLatLng(strings: List<String>): LatLng {
        val doubles = strings.map { it.toDouble() }
        return LatLng(doubles.max()!!, doubles.min()!!)
    }

    private fun retrieveTableGebied(query: String = "", responseListener: Response.Listener<JSONArray>, errorListener: Response.ErrorListener) {
        retrieveTable(API_RESOURCE_GEBIED + query, responseListener, errorListener)
    }

    private fun retrieveTableGeometrieGebied(query: String = "", responseListener: Response.Listener<JSONArray>, errorListener: Response.ErrorListener) {
        retrieveTable(API_RESOURCE_GEOMETRIE_GEBIED + query, responseListener, errorListener)
    }

    private fun retrieveTableGeoParkeerGarages(query: String = "", responseListener: Response.Listener<JSONArray>, errorListener: Response.ErrorListener) {
        retrieveTable(API_RESOURCE_GEO_PARKEER_GARAGES + query, responseListener, errorListener)
    }

    private fun retrieveTableGeoPenR(query: String = "", responseListener: Response.Listener<JSONArray>, errorListener: Response.ErrorListener) {
        retrieveTable(API_RESOURCE_GEO_PENR + query, responseListener, errorListener)
    }

    private fun retrieveTableGeoCarpool(query: String = "", responseListener: Response.Listener<JSONArray>, errorListener: Response.ErrorListener) {
        retrieveTable(API_RESOURCE_GEO_CARPOOL + query, responseListener, errorListener)
    }

    private fun retrieveTable(resource: String, responseListener: Response.Listener<JSONArray>, errorListener: Response.ErrorListener) {
        val request = JsonArrayRequest(Request.Method.GET, API_URL + resource, null, responseListener, errorListener)
        requestQueue.add(request)
    }

    interface ParkingRequestListener {
        fun onReceived(parking: Parking)
    }

    private interface AreaRetrievalListener {
        fun onSuccessful(area: List<LatLng>)
        fun onFailed()
    }
}