package com.pankaj.mobisy

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.IntentSender
import android.location.Location
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.fragment.app.FragmentActivity
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.GoogleApiAvailability
import com.google.android.gms.common.api.GoogleApiClient
import com.google.android.gms.location.*
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import java.util.*


open class MainActivity : FragmentActivity(), OnMapReadyCallback,
        ActivityCompat.OnRequestPermissionsResultCallback,
        PermissionUtil.PermissionResultCallback, GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener {

    private val TAG = MainActivity::class.java.simpleName
    private val PLAY_SERVICES_REQUEST = 1000
    private val REQUEST_CHECK_SETTINGS = 2000
    private var isPermissionGranted: Boolean = false
    private var mMap: GoogleMap? = null
    private var permissions = ArrayList<String>()
    private var permissionUtils: PermissionUtil? = null
    private var mLastLocation: Location? = null
    private var mGoogleApiClient: GoogleApiClient? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_maps)
        val mapFragment = supportFragmentManager
                .findFragmentById(R.id.map) as SupportMapFragment?
        mapFragment?.getMapAsync(this)

        permissionUtils = PermissionUtil(this@MainActivity)

        permissions.add(Manifest.permission.ACCESS_FINE_LOCATION)
        permissions.add(Manifest.permission.ACCESS_COARSE_LOCATION)

        permissionUtils?.check_permission(permissions, "Need GPS permission for getting your location", 1)

        // check availability of play services
        if (checkPlayServices()) {

            // Building the GoogleApi client
            buildGoogleApiClient()
        }
    }

    /**
     * Method to verify google play services on the device
     */

    private fun checkPlayServices(): Boolean {

        val googleApiAvailability = GoogleApiAvailability.getInstance()

        val resultCode = googleApiAvailability.isGooglePlayServicesAvailable(this)

        if (resultCode != ConnectionResult.SUCCESS) {
            if (googleApiAvailability.isUserResolvableError(resultCode)) {
                googleApiAvailability.getErrorDialog(this, resultCode,
                        PLAY_SERVICES_REQUEST).show()
            } else {
                Toast.makeText(applicationContext,
                        "This device is not supported.", Toast.LENGTH_LONG)
                        .show()
                finish()
            }
            return false
        }
        return true
    }

    /**
     * Creating google api client object
     */

    @Synchronized
    protected fun buildGoogleApiClient() {
        mGoogleApiClient = GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API).build()

        mGoogleApiClient?.connect()

        val mLocationRequest = LocationRequest()
        mLocationRequest.interval = 10000
        mLocationRequest.fastestInterval = 5000
        mLocationRequest.priority = LocationRequest.PRIORITY_HIGH_ACCURACY

        val builder = LocationSettingsRequest.Builder()
                .addLocationRequest(mLocationRequest)

        val result = LocationServices.SettingsApi.checkLocationSettings(mGoogleApiClient, builder.build())

        result.setResultCallback { locationSettingsResult ->
            val status = locationSettingsResult.status

            when (status.statusCode) {
                LocationSettingsStatusCodes.SUCCESS -> {
                    // All location settings are satisfied. The client can initialize location requests here
                    getLocation()
                }
                LocationSettingsStatusCodes.RESOLUTION_REQUIRED -> try {
                    // Show the dialog by calling startResolutionForResult(),
                    // and check the result in onActivityResult().
                    status.startResolutionForResult(this@MainActivity, REQUEST_CHECK_SETTINGS)

                } catch (e: IntentSender.SendIntentException) {
                    // Ignore the error.
                }

                LocationSettingsStatusCodes.SETTINGS_CHANGE_UNAVAILABLE -> {
                }
            }
        }


    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        // redirects to utils
        permissionUtils?.onRequestPermissionsResult(requestCode, permissions, grantResults)

    }

    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap
    }

    private fun addCurrentLocationMarker(latitude: Double, longitude: Double) {
        if (latitude == 0.0 || longitude == 0.0) return
        val bangalore = LatLng(latitude, longitude)
        mMap?.addMarker(MarkerOptions().position(bangalore))
        mMap?.moveCamera(CameraUpdateFactory.newLatLng(bangalore))
        mMap?.moveCamera(CameraUpdateFactory.zoomTo(12.0f))
        mMap?.addMarker(MarkerOptions().position(bangalore)
                .title("Emp1").snippet("Time"))
        mMap?.animateCamera(CameraUpdateFactory.newLatLngZoom(
                bangalore, 15f))
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        val states = LocationSettingsStates.fromIntent(data)
        when (requestCode) {
            REQUEST_CHECK_SETTINGS -> when (resultCode) {
                Activity.RESULT_OK ->
                    // All required changes were successfully made
                    getLocation()
                Activity.RESULT_CANCELED -> {
                }
                else -> {
                }
            }// The user was asked to change settings, but chose not to
        }
    }

    /**
     * Method to display the location on UI
     */

    private fun getLocation() {
        if (isPermissionGranted) {
            try {
                mLastLocation = LocationServices.FusedLocationApi
                        .getLastLocation(mGoogleApiClient)
                val lat = mLastLocation?.latitude ?: 0.0
                val lng = mLastLocation?.longitude ?: 0.0
                addCurrentLocationMarker(lat, lng)
            } catch (e: SecurityException) {
                e.printStackTrace()
            }
        }
    }

    override fun onConnected(p0: Bundle?) {
        // Once connected with google api, get the location
        isPermissionGranted = true
        getLocation()
    }

    override fun onConnectionSuspended(p0: Int) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun onConnectionFailed(result: ConnectionResult) {
        Log.i(TAG, "Connection failed: ConnectionResult.getErrorCode() = " + result.errorCode)
    }

    override fun PermissionGranted(request_code: Int) {
        Log.i("PERMISSION", "GRANTED")
        isPermissionGranted = true
    }

    override fun PartialPermissionGranted(request_code: Int, granted_permissions: ArrayList<String>) {
        Log.i("PERMISSION PARTIALLY", "GRANTED")
    }

    override fun PermissionDenied(request_code: Int) {
        Log.i("PERMISSION", "DENIED")
    }

    override fun NeverAskAgain(request_code: Int) {
        Log.i("PERMISSION", "NEVER ASK AGAIN")
    }
}