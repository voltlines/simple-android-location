package com.huseyinbulbul.whereami

import android.Manifest
import android.annotation.SuppressLint
import android.app.*
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.location.Location
import android.location.LocationListener
import android.os.*
import android.util.Log
import androidx.core.app.ActivityCompat
import com.google.android.gms.location.*
import com.google.android.gms.tasks.OnSuccessListener

class LocationService: Service(),OnSuccessListener<Location> {
    companion object{
        var listener: LocationServiceListener? = null
    }

    private var client: FusedLocationProviderClient? = null
    private var locationRequest: LocationRequest? = null
    private var callback: LocationCallback? = null
    private var deviceManager: android.location.LocationManager? = null
    private var deviceListener: LocationListener? = null

    override fun onBind(intent: Intent?): IBinder? {
        return LocationServiceBinder()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        showForeground(intent)
        prepareAndStartFuseLocation()
        prepareAndStartCoreLocation()
        return START_NOT_STICKY
    }


    override fun onSuccess(p0: Location?) {

    }

    fun showForeground(intent: Intent?) {
        //create notification for api 26 and above
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val intent = Intent(this, MainActivity::class.java)
            val pendingIntent = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT)
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            val importance = NotificationManager.IMPORTANCE_HIGH
            val notificationChannel = NotificationChannel("whereami", "location", importance)
            notificationChannel.enableLights(true)
            notificationChannel.lightColor = Color.RED
            notificationChannel.enableVibration(true)
            notificationChannel.vibrationPattern = longArrayOf(100, 200, 300, 400, 500, 400, 300, 200, 400)
            notificationManager.createNotificationChannel(notificationChannel)

            val builder = Notification.Builder(this, "whereami")
                .setContentTitle(getString(R.string.app_name))
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentText("where are you")
                .setAutoCancel(true)
                .setContentIntent(pendingIntent)

            val notification = builder.build()
            startForeground(1, notification)
        } else {
            intent?.let {
                startService(it)
            }
        }
    }

    @SuppressLint("MissingPermission")
    private fun prepareAndStartFuseLocation(){
        client = LocationServices.getFusedLocationProviderClient(this)
        callback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult?) {
                locationResult?.lastLocation?.let {
                    stopCoreLocation()
                    haveNewLocation(it)
                }
            }
        }

        locationRequest = LocationRequest()
        locationRequest?.apply {
            priority = LocationRequest.PRIORITY_HIGH_ACCURACY
            interval = 5000
        }

        client?.let {
            val hasFineLocationPermission =
                ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
            val hasCoarseLocationPermission =
                ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION)

            if(hasFineLocationPermission == PackageManager.PERMISSION_GRANTED &&
                hasCoarseLocationPermission == PackageManager.PERMISSION_GRANTED) {
                it.requestLocationUpdates(locationRequest, callback, null)
            }
        }
    }

    @SuppressLint("MissingPermission")
    private fun prepareAndStartCoreLocation(){
        deviceManager = getSystemService(Context.LOCATION_SERVICE) as android.location.LocationManager
        deviceListener = object: LocationListener {
            override fun onLocationChanged(location: Location?) {
                stopCoreLocation()
                location?.let {
                    haveNewLocation(it)
                }
            }

            override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {

            }

            override fun onProviderEnabled(provider: String?) {

            }

            override fun onProviderDisabled(provider: String?) {

            }
        }

        val hasFineLocationPermission = ActivityCompat.checkSelfPermission(
            this, Manifest.permission.ACCESS_FINE_LOCATION
        )
        val hasCoarseLocationPermission = ActivityCompat.checkSelfPermission(
            this, Manifest.permission.ACCESS_COARSE_LOCATION
        )

        if(hasFineLocationPermission == PackageManager.PERMISSION_GRANTED &&
            hasCoarseLocationPermission == PackageManager.PERMISSION_GRANTED) {
            deviceManager?.let { locationManager ->
                locationManager.requestLocationUpdates(
                    android.location.LocationManager.NETWORK_PROVIDER, 5000, 15f, deviceListener
                )
                locationManager.requestLocationUpdates(
                    android.location.LocationManager.GPS_PROVIDER, 5000, 15f, deviceListener
                )
                /**
                 * device location manager does not update callbacks on some devices
                 * if device does not move so we may end up with no location
                 * at the beginning for a while
                 * to prevent this get last known location if it is exists
                 */
                var lastLocation = locationManager.getLastKnownLocation(android.location.LocationManager.GPS_PROVIDER)
                lastLocation?.let {
                    haveNewLocation(lastLocation)
                } ?: run {
                    lastLocation = locationManager.getLastKnownLocation(android.location.LocationManager.NETWORK_PROVIDER)
                    lastLocation?.let {
                        haveNewLocation(lastLocation)
                    }
                }
            }
        }
    }

    private fun stopCoreLocation(){
        deviceManager?.let {locationManager ->
            locationManager.removeUpdates(deviceListener)
        }
    }

    private fun haveNewLocation(lastLocation: Location){
        if(BuildConfig.DEBUG) {
            Log.i("LocationLocation", "${lastLocation.latitude} , ${lastLocation.longitude}")
        }

        listener?.let {
            it.newLocation(lastLocation)
        }
    }



    inner class LocationServiceBinder : Binder() {

        // Return this instance of LocationService so clients can call public methods.
        val service: LocationService
            get() = this@LocationService
    }

    interface LocationServiceListener{
        fun newLocation(location: Location)
    }
}