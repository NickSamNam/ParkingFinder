package com.hoogerdijknicknam.parkingfinder

import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.location.Location
import android.os.Bundle
import android.support.v4.app.ActivityCompat
import android.support.v4.content.ContextCompat
import android.support.v7.app.AppCompatActivity
import android.text.format.DateFormat
import android.util.Log
import com.android.volley.Request
import com.android.volley.toolbox.Volley
import com.google.android.gms.location.*
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.*
import com.google.android.gms.maps.model.LatLng
import org.json.JSONObject
import java.util.*


private const val PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION = 0
private const val ZOOM_DEFAULT = 18f
private const val ZOOM_THRESHOLD = 10
private const val KEY_LOCATION = "LOCATION"
private const val KEY_CAMERA_POSITION = "CAMERA_POSITION"

class MapsActivity : AppCompatActivity(), OnMapReadyCallback, RDWOpenDataSubscriptionService.Subscription {
    private var fresh = true
    private lateinit var mMap: GoogleMap
    private var lastKnownLocation: Location? = null
    private var cameraPosition: CameraPosition? = null
    private lateinit var fusedLocationProviderClient: FusedLocationProviderClient
    private val defaultLocation = LatLng(32.676149, -117.157703)
    private val visibleMarkers = HashMap<String, Marker>()
    private var parking: Parking? = null
    private var routeLines: MutableList<Polyline>? = null
    private var route: List<List<LatLng>>? = null
    private lateinit var locationCallback: LocationCallback

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (savedInstanceState != null) {
            fresh = false
            lastKnownLocation = savedInstanceState.getParcelable(KEY_LOCATION)
            cameraPosition = savedInstanceState.getParcelable(KEY_CAMERA_POSITION)
            parking = savedInstanceState.getParcelable(KEY_PARKING)
        } else {
            RDWOpenDataSubscriptionService.start(Volley.newRequestQueue(this))
            parking = RDWOpenDataSubscriptionService.backLog.find { it.areaId == intent.extras?.getString(KEY_PARKING) }
        }


        setContentView(R.layout.activity_maps)
        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this)
        val mapFragment = supportFragmentManager
                .findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)

        locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult?) {
                lastKnownLocation = locationResult?.lastLocation
                drawRoute()
            }
        }
    }

    override fun onResume() {
        registerLocationUpdates()
        super.onResume()
    }

    override fun onPause() {
        deregisterLocationUpdates()
        super.onPause()
    }

    override fun onSaveInstanceState(outState: Bundle?) {
        try {
            outState?.putParcelable(KEY_CAMERA_POSITION, mMap.cameraPosition)
        } catch (e: UninitializedPropertyAccessException) {

        }
        outState?.putParcelable(KEY_LOCATION, lastKnownLocation)
        outState?.putParcelable(KEY_PARKING, parking)
        super.onSaveInstanceState(outState)
    }

    override fun onDestroy() {
        RDWOpenDataSubscriptionService.unsubscribe(this)
        super.onDestroy()
    }

    /**
     * Manipulates the map once available.
     * This callback is triggered when the map is ready to be used.
     * This is where we can add markers or lines, add listeners or move the camera. In this case,
     * we just add a marker near Sydney, Australia.
     * If Google Play services is not installed on the device, the user will be prompted to install
     * it inside the SupportMapFragment. This method will only be triggered once the user has
     * installed Google Play services and returned to the app.
     */
    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap
        mMap.setOnInfoWindowClickListener(::onInfoWindowClick)
        mMap.setOnCameraIdleListener { RDWOpenDataSubscriptionService.backLog.forEach { it -> addMarker(it) } }
        RDWOpenDataSubscriptionService.backLog.forEach { addMarker(it) }

        if (fresh) {
            mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(defaultLocation, ZOOM_DEFAULT))
            getLocationPermission()
        }

        if (parking != null) {
            getDeviceLocation(ResponseListener { createRoute() })
        }

        updateLocationUI()
        getDeviceLocation()

        RDWOpenDataSubscriptionService.subscribe(this)
    }

    private fun createRoute() {
        if (lastKnownLocation == null || parking == null) return

        val trafficMode = "mode=driving"

        val str_origin = "origin=" + lastKnownLocation!!.latitude + "," + lastKnownLocation!!.longitude

        val str_dest = "destination=" + parking!!.location.latitude + "," + parking!!.location.longitude

        // Url building
        val parameters = "$str_origin&$str_dest&$trafficMode"

        val output = "json"

        val url = "https://maps.googleapis.com/maps/api/directions/" + output + "?" + parameters + "&key=" + getString(R.string.google_maps_key)
        Log.i("URL", url)

        VolleyManager.getInstance(this).JsonObjectRequest(Request.Method.GET, url, null, { result ->
            val response = result as JSONObject
            val dataParser = RouteDataParser()
            route = dataParser.parseRoutesInfo(response)
            drawRoute()
        })
    }

    private var northEastBound: LatLng? = null
    private var southWestBound: LatLng? = null

    private fun drawRoute() {
        if (route == null || route!!.isEmpty()) return
        // Bounds
        for (i in 0 until route!![0].size) {
            if (i % 2 == 0) {
                if (northEastBound != null) {
                    if (route!![0][i].longitude > northEastBound!!.longitude) {
                        val tempLat = northEastBound!!.latitude
                        northEastBound = LatLng(tempLat, route!![0][i].longitude)
                    }
                    if (route!![0][i].latitude > northEastBound!!.latitude) {
                        val tempLong = northEastBound!!.longitude
                        northEastBound = LatLng(route!![0][i].latitude, tempLong)
                    }
                } else {
                    northEastBound = route!![0][i]
                }
            } else {
                if (southWestBound != null) {
                    if (route!![0][i].longitude < southWestBound!!.longitude) {
                        val tempLat = southWestBound!!.latitude
                        southWestBound = LatLng(tempLat, route!![0][i].longitude)
                    }
                    if (route!![0][i].latitude < southWestBound!!.latitude) {
                        val tempLong = southWestBound!!.longitude
                        southWestBound = LatLng(route!![0][i].latitude, tempLong)
                    }
                } else {
                    southWestBound = route!![0][i]
                }
            }
        }
        val bounds = LatLngBounds(southWestBound, northEastBound)
        val padding = 150
        mMap.animateCamera(CameraUpdateFactory.newLatLngBounds(bounds, padding))

        // Polyline creation
        val routeLinesOptions = ArrayList<PolylineOptions>()
        var prevLine: PolylineOptions? = null

        for (i in 1 until route!!.size) {
            val leg = route!![i]
            for (p in leg) {
                if (prevLine != null) {
                    val prevP = prevLine.points[0]
                    val dP = FloatArray(1)
                    Location.distanceBetween(prevP.latitude, prevP.longitude, p.latitude, p.longitude, dP)
                    if (dP[0] < 10) continue
                    prevLine.add(p)
                    routeLinesOptions.add(prevLine)
                }
                prevLine = PolylineOptions().add(p)
            }
        }

        // Polyline removal
        if (routeLines != null) {
            for (routeLine in routeLines!!) {
                routeLine.remove()
            }
            routeLines!!.clear()
        } else {
            routeLines = ArrayList()
        }

        // Polyline adding
        var visited = false
        Collections.reverse(routeLinesOptions)
        for (p in routeLinesOptions) {
            p.width(10f)
            if (lastKnownLocation == null) {
                p.color(Color.RED)
            } else {
                if (visited) {
                    p.color(Color.GRAY)
                } else {
                    val polyEnd = p.points[p.points.size - 1]
                    val dP = FloatArray(1)
                    Location.distanceBetween(lastKnownLocation!!.getLatitude(), lastKnownLocation!!.getLongitude(), polyEnd.latitude, polyEnd.longitude, dP)
                    if (dP[0] <= 30) {
                        visited = true
                        p.color(Color.GRAY)
                    } else
                        p.color(Color.RED)
                }
            }
            routeLines!!.add(mMap.addPolyline(p))
        }
    }

    @SuppressLint("MissingPermission")
    private fun registerLocationUpdates() {
        if (!hasLocationPermission()) return

        val locationRequest = LocationRequest()
                .setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY)
                .setInterval(10000)
                .setSmallestDisplacement(10f)
                .setMaxWaitTime(1000)
                .setFastestInterval(10000)

        fusedLocationProviderClient.requestLocationUpdates(locationRequest, locationCallback, null)
    }

    private fun deregisterLocationUpdates() {
        fusedLocationProviderClient.removeLocationUpdates(locationCallback)
    }

    override fun onReceive(parking: Parking) {
        addMarker(parking)
    }

    private fun addMarker(parking: Parking) {
        val bounds = mMap.projection.visibleRegion.latLngBounds

        if (bounds.contains(LatLng(parking.location.latitude, parking.location.longitude)) && mMap.cameraPosition.zoom >= ZOOM_THRESHOLD && (this.parking == null || this.parking == parking)) {
            if (!visibleMarkers.containsKey(parking.areaId)) {
                val options = MarkerOptions()
                        .title(parking.areaDesc)
                        .position(LatLng(parking.location.latitude, parking.location.longitude))
                if (parking.startTime != null && parking.endTime != null)
                    options.snippet("${DateFormat.getTimeFormat(this).format(parking.startTime)} - ${DateFormat.getTimeFormat(this).format(parking.endTime)}")
                val marker = mMap.addMarker(options)
                marker.tag = parking.areaId
                visibleMarkers.put(parking.areaId, marker)
            }
        } else {
            visibleMarkers.remove(parking.areaId)?.remove()
        }
    }

    private fun onInfoWindowClick(marker: Marker) {
        val intent = Intent(this, ParkingDetailActivity::class.java)
        intent.putExtra(KEY_PARKING_ID, marker.tag as String)
        startActivity(intent)
    }

    private fun updateLocationUI() {
        try {
            if (hasLocationPermission()) {
                mMap.isMyLocationEnabled = true
                mMap.uiSettings.isMyLocationButtonEnabled = true
            } else {
                mMap.isMyLocationEnabled = false
                mMap.uiSettings.isMyLocationButtonEnabled = false
                lastKnownLocation = null
            }
        } catch (e: SecurityException) {
            Log.e("Exception: %s", e.message)
        }
    }

    private fun getDeviceLocation(responseListener: ResponseListener? = null) {
        try {
            if (hasLocationPermission()) {
                val locationResult = fusedLocationProviderClient.lastLocation
                locationResult.addOnCompleteListener(this) { task ->
                    if (task.isSuccessful) {
                        lastKnownLocation = task.result
                        responseListener?.getResult(lastKnownLocation)
                        if (cameraPosition == null)
                            mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(
                                    LatLng(lastKnownLocation!!.latitude,
                                            lastKnownLocation!!.longitude), ZOOM_DEFAULT))
                    } else {
                        if (cameraPosition == null)
                            mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(defaultLocation, ZOOM_DEFAULT))
                        mMap.uiSettings.isMyLocationButtonEnabled = false
                    }
                }
            }
        } catch (e: SecurityException) {
            Log.e("Exception: %s", e.message)
        }

    }

    private fun hasLocationPermission() = ContextCompat.checkSelfPermission(this.applicationContext,
            android.Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED

    private fun getLocationPermission() {
        if (!hasLocationPermission()) {
            ActivityCompat.requestPermissions(this,
                    arrayOf(android.Manifest.permission.ACCESS_FINE_LOCATION),
                    PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION)
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        when (requestCode) {
            PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    updateLocationUI()
                    getDeviceLocation()
                }
            }
        }
    }
}