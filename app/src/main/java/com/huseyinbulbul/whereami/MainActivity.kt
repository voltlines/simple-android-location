package com.huseyinbulbul.whereami

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import kotlinx.android.synthetic.main.activity_main.*


class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        checkPermission()
    }

    override fun onRequestPermissionsResult(requestCode: Int,
                                            permissions: Array<out String>,
                                            grantResults: IntArray) {
        if(requestCode == 10001){
            checkPermission()
        }else {
            super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        }
    }

    override fun onDestroy() {
        stopService(Intent(this, LocationService::class.java))
        super.onDestroy()
    }

    fun checkPermission(){
        if(ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED){
            requestPermission()
        }else {
            startLocation()
        }
    }

    fun requestPermission(){
        val arr = arrayOf(String())
        arr[0] = Manifest.permission.ACCESS_FINE_LOCATION
        ActivityCompat.requestPermissions(this, arr, 10001)
    }

    fun startLocation(){
        startForegroundService(Intent(this, LocationService::class.java))
            LocationService.listener = object: LocationService.LocationServiceListener{
                                            override fun newLocation(location: Location) {
                                                tv_locations.text = "${tv_locations.text} + ${location.latitude}, ${location.longitude} \n"
                                            }
        }
    }
}
