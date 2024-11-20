package com.example.todaylunch

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView

import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.todaylunch.databinding.ActivityMainBinding
import com.example.todaylunch.databinding.ActivityMapBinding
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.GoogleMapOptions
import com.google.android.gms.maps.MapView
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.material.bottomsheet.BottomSheetBehavior

class MapActivity : AppCompatActivity(), OnMapReadyCallback {

    val binding: ActivityMapBinding by lazy { ActivityMapBinding.inflate(layoutInflater) }
    private lateinit var map: GoogleMap
    private val API_KEY = "AIzaSyBrfb7yxin3xvGpVzLF-h6okBHnsl82IHM" // API Key 직접 선언


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d("MapDebug", "MapView onCreate called")


        setContentView(binding.root)


        // MapView 초기화
        Log.d("MapDebug", "MapView onCreate called")

        // API Key를 GoogleMapOptions에 직접 전달
        val options = GoogleMapOptions().apply {
            mapToolbarEnabled(true) // 필요한 옵션 추가
        }

        binding.mapView.onCreate(savedInstanceState)
        // MapView를 동적으로 생성하면서 API Key 전달
        binding.mapView.getMapAsync(this)
        Log.d("MapDebug", "MapView initialized and getMapAsync called")



        // Google Maps API Key 설정


        // MapView 준비

    }

    override fun onMapReady(googleMap: GoogleMap) {
        Log.d("MapDebug", "onMapReady called")
        map = googleMap
        val location = LatLng(37.545169, 126.964268)
        map.moveCamera(CameraUpdateFactory.newLatLngZoom(location, 15f))
        Log.d("MapDebug", "Camera moved to location: $location")

    }

    override fun onResume() {
        super.onResume()
        binding.mapView.onResume()
        Log.d("MapDebug", "MapView onResume called")
    }

    override fun onPause() {
        super.onPause()
        binding.mapView.onPause()
        Log.d("MapDebug", "MapView onPause called")
    }

    override fun onDestroy() {
        super.onDestroy()
        binding.mapView.onDestroy()
        Log.d("MapDebug", "MapView onDestroy called")
    }

    override fun onLowMemory() {
        super.onLowMemory()
        binding.mapView.onLowMemory()
        Log.d("MapDebug", "MapView onLowMemory called")
    }
}