package com.hoogerdijknicknam.parkingfinder

import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.os.Bundle
import android.support.v4.app.ActivityCompat
import android.support.v4.content.ContextCompat
import android.support.v7.app.AppCompatActivity
import android.text.format.DateFormat
import android.util.Log
import com.android.volley.toolbox.Volley
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions


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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (savedInstanceState != null) {
            fresh = false
            lastKnownLocation = savedInstanceState.getParcelable(KEY_LOCATION)
            cameraPosition = savedInstanceState.getParcelable(KEY_CAMERA_POSITION)
        } else {
            RDWOpenDataSubscriptionService.start(Volley.newRequestQueue(this))
        }

        setContentView(R.layout.activity_maps)
        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this)
        val mapFragment = supportFragmentManager
                .findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)
    }

    override fun onSaveInstanceState(outState: Bundle?) {
        try {
            outState?.putParcelable(KEY_CAMERA_POSITION, mMap.cameraPosition)
        } catch (e: UninitializedPropertyAccessException) {

        }
        outState?.putParcelable(KEY_LOCATION, lastKnownLocation)
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
        updateLocationUI()
        getDeviceLocation()

        RDWOpenDataSubscriptionService.subscribe(this)
    }

    override fun onReceive(parking: Parking) {
        addMarker(parking)
    }

    private fun addMarker(parking: Parking) {
        val bounds = mMap.projection.visibleRegion.latLngBounds

        if (bounds.contains(LatLng(parking.location.latitude, parking.location.longitude)) && mMap.cameraPosition.zoom >= ZOOM_THRESHOLD) {
            if (!visibleMarkers.containsKey(parking.areaId)) {
                val options = MarkerOptions()
                        .title(parking.areaDesc)
                        .position(LatLng(parking.location.latitude, parking.location.longitude))
                if (parking.startTime != null && parking.endTime != null)
                    options.snippet("${DateFormat.getTimeFormat(this).format(parking.startTime)} - ${DateFormat.getTimeFormat(this).format(parking.endTime)}")
                val marker = mMap.addMarker(options)
                marker.tag = parking
                visibleMarkers.put(parking.areaId, marker)
            }
        } else {
            visibleMarkers.remove(parking.areaId)?.remove()
        }
    }

    private fun onInfoWindowClick(marker: Marker) {
        val intent = Intent(this, ParkingDetailActivity::class.java)
        intent.putExtra(KEY_PARKING, marker.tag as Parking)
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

    private fun getDeviceLocation() {
        try {
            if (hasLocationPermission()) {
                val locationResult = fusedLocationProviderClient.lastLocation
                locationResult.addOnCompleteListener(this) { task ->
                    if (task.isSuccessful) {
                        lastKnownLocation = task.result
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