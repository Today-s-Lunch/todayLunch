package com.example.todaylunch

import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil.setContentView
import com.example.todaylunch.databinding.ActivityMapBinding
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.GoogleMapOptions
import com.google.android.gms.maps.MapView
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions

class MapActivity : AppCompatActivity(), OnMapReadyCallback {

    private val binding: ActivityMapBinding by lazy { ActivityMapBinding.inflate(layoutInflater) }
    private lateinit var map: GoogleMap
    private var mapView: MapView? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d("MapDebug", "MapActivity onCreate called")
        setContentView(binding.root)

        // MapView 초기화
        mapView = binding.mapView.also { mapView ->
            mapView.onCreate(savedInstanceState)
            mapView.getMapAsync(this)
        }

        // 지도 설정
        val options = GoogleMapOptions().apply {
            mapToolbarEnabled(true)
            compassEnabled(true)
            zoomControlsEnabled(true)
        }
    }

    override fun onMapReady(googleMap: GoogleMap) {
        Log.d("MapDebug", "onMapReady called")
        map = googleMap

        try {
            // 지도 초기 설정
            map.apply {
                // UI 설정
                uiSettings.apply {
                    isZoomControlsEnabled = true
                    isCompassEnabled = true
                    isMapToolbarEnabled = true
                }

                // 초기 위치 설정
                val location = LatLng(37.545169, 126.964268)
                moveCamera(CameraUpdateFactory.newLatLngZoom(location, 15f))
                addMarker(MarkerOptions().position(location).title("현재 위치"))

                Log.d("MapDebug", "Camera moved to location: $location")
            }
        } catch (e: Exception) {
            Log.e("MapDebug", "Error setting up map: ${e.message}")
        }
    }

    // 생명주기 메서드들
    override fun onStart() {
        super.onStart()
        mapView?.onStart()
    }

    override fun onResume() {
        super.onResume()
        mapView?.onResume()
        Log.d("MapDebug", "MapView onResume called")
    }

    override fun onPause() {
        mapView?.onPause()
        super.onPause()
        Log.d("MapDebug", "MapView onPause called")
    }

    override fun onStop() {
        super.onStop()
        mapView?.onStop()
    }

    override fun onDestroy() {
        mapView?.onDestroy()
        super.onDestroy()
        Log.d("MapDebug", "MapView onDestroy called")
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        mapView?.onSaveInstanceState(outState)
    }

    override fun onLowMemory() {
        super.onLowMemory()
        mapView?.onLowMemory()
        Log.d("MapDebug", "MapView onLowMemory called")
    }
}