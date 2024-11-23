package com.example.todaylunch

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.bitmap.CenterCrop
import com.bumptech.glide.load.resource.bitmap.RoundedCorners
import com.example.todaylunch.databinding.ActivityMapBinding
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.firebase.database.*
import kotlinx.coroutines.launch
import kotlin.math.*
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.BitmapDescriptorFactory

class MapActivity : AppCompatActivity(), OnMapReadyCallback {

    private val binding: ActivityMapBinding by lazy { ActivityMapBinding.inflate(layoutInflater) }
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var adapter: RestaurantAdapter
    private lateinit var googleMap: GoogleMap
    private lateinit var bottomSheetBehavior: BottomSheetBehavior<View>

    private val db: DatabaseReference by lazy {
        FirebaseDatabase.getInstance().getReference("restaurants")
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)

        // FusedLocationClient 초기화
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        // RecyclerView 초기화
        setupRecyclerView()

        // BottomSheet 초기화
        setupBottomSheet()

        // Google Map 초기화
        binding.mapView.onCreate(savedInstanceState)
        binding.mapView.getMapAsync(this)

        // 사용자 위치 가져오기 및 데이터 로드
        getUserLocation()
        val goToStartActivity = Intent(this, StartActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        binding.downbar.homeButton.setOnClickListener {
            startActivity(goToStartActivity)
        }

        binding.downbar.myPageButton.setOnClickListener {
            startActivity(goToStartActivity)
        }
        val goTomapActivity = Intent(this, MapActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        binding.downbar.mapButton.setOnClickListener{
            startActivity(goTomapActivity)
        }
        binding.upbar.backButton.setOnClickListener {onBackPressedDispatcher.onBackPressed()}
    }

    override fun onMapReady(map: GoogleMap) {
        googleMap = map

        // 지도 기본 설정
        googleMap.uiSettings.isZoomControlsEnabled = true
        googleMap.uiSettings.isCompassEnabled = true
    }

    private fun setupRecyclerView() {
        adapter = RestaurantAdapter(emptyList()) // 초기 데이터는 빈 리스트
        binding.restaurantList.apply {
            layoutManager = LinearLayoutManager(this@MapActivity)
            adapter = this@MapActivity.adapter
        }
    }

    private fun setupBottomSheet() {
        bottomSheetBehavior = BottomSheetBehavior.from(binding.bottomSheet)
        bottomSheetBehavior.state = BottomSheetBehavior.STATE_COLLAPSED
        bottomSheetBehavior.peekHeight = 200
        bottomSheetBehavior.isHideable = false // BottomSheet가 숨겨지지 않도록 설정

        val drawable = GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            cornerRadius = 16f // 둥근 모서리 설정
            setColor(ContextCompat.getColor(this@MapActivity, R.color.white)) // 배경 색상 설정
        }

        binding.bottomSheet.background = drawable // 배경 적용

        // 상태 변화 디버깅
        bottomSheetBehavior.addBottomSheetCallback(object : BottomSheetBehavior.BottomSheetCallback() {
            override fun onStateChanged(bottomSheet: View, newState: Int) {
                Log.d("BottomSheetState", "State changed to: $newState")
            }

            override fun onSlide(bottomSheet: View, slideOffset: Float) {
                Log.d("BottomSheetSlide", "Slide offset: $slideOffset")
            }
        })
    }

    private fun getUserLocation() {
        val useTestLocation = true // 테스트 좌표 사용 여부

        if (useTestLocation) {
            val testLat = 37.544679 // 숙대입구역
            val testLng = 126.970884
            Log.d("TestLocation", "Using test location: ($testLat, $testLng)")
            fetchNearbyRestaurants(testLat, testLng)
        } else {
            if (ActivityCompat.checkSelfPermission(
                    this,
                    Manifest.permission.ACCESS_FINE_LOCATION
                ) == PackageManager.PERMISSION_GRANTED
            ) {
                fusedLocationClient.lastLocation.addOnSuccessListener { location ->
                    if (location != null) {
                        fetchNearbyRestaurants(location.latitude, location.longitude)
                    } else {
                        Log.e("LocationError", "Failed to get user location.")
                    }
                }
            } else {
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                    LOCATION_PERMISSION_REQUEST_CODE
                )
            }
        }
    }

    private fun fetchNearbyRestaurants(userLat: Double, userLng: Double) {
        db.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val restaurants = snapshot.children.mapNotNull { restaurantSnapshot ->
                    val restaurant = restaurantSnapshot.getValue(Restaurant::class.java)

                    if (restaurant?.Latitude.isNullOrEmpty() || restaurant?.Longitude.isNullOrEmpty()) {
                        Log.e("InvalidData", "Skipping restaurant with invalid coordinates: ${restaurant?.Name}")
                        return@mapNotNull null
                    }

                    restaurant
                }

                lifecycleScope.launch {
                    // 거리 계산 및 정렬
                    val sortedList = restaurants
                        .map { restaurant ->
                            val latitude = restaurant.Latitude.toDoubleOrNull() ?: 0.0
                            val longitude = restaurant.Longitude.toDoubleOrNull() ?: 0.0
                            val distance = calculateDistance(userLat, userLng, latitude, longitude)

                            // 거리 로그 출력
                            Log.d("Distance", "Name: ${restaurant.Name}, Distance: $distance meters")

                            // 거리와 레스토랑 데이터를 Pair로 반환
                            Pair(distance, restaurant)
                        }
                        .sortedBy { it.first } // 거리로 정렬
                        .take(5) // 상위 5개 선택
                        .map { it.second } // 정렬된 레스토랑 데이터만 가져옴

                    // 지도에 마커 추가
                    addMarkers(userLat, userLng, sortedList)

                    // RecyclerView 업데이트
                    updateRecyclerView(sortedList)
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("FirebaseError", error.message)
            }
        })
    }

    private fun addMarkers(userLat: Double, userLng: Double, restaurants: List<Restaurant>) {
        // 내 위치 마커 추가
        googleMap.addMarker(
            MarkerOptions()
                .position(LatLng(userLat, userLng))
                .title("내 위치")
                .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_BLUE))
        )
        googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(LatLng(userLat, userLng), 15f))

        // 레스토랑 마커 추가
        restaurants.forEach { restaurant ->
            val latitude = restaurant.Latitude.toDoubleOrNull()
            val longitude = restaurant.Longitude.toDoubleOrNull()

            if (latitude != null && longitude != null) {
                googleMap.addMarker(
                    MarkerOptions()
                        .position(LatLng(latitude, longitude))
                        .title(restaurant.Name)
                )
            }
        }
    }

    private fun updateRecyclerView(nearbyRestaurants: List<Restaurant>) {
        adapter.updateList(nearbyRestaurants)
    }

    private fun calculateDistance(
        userLat: Double,
        userLon: Double,
        targetLat: Double,
        targetLon: Double
    ): Double {
        val R = 6371e3 // 지구 반지름 (미터)
        val φ1 = Math.toRadians(userLat)
        val φ2 = Math.toRadians(targetLat)
        val Δφ = Math.toRadians(targetLat - userLat)
        val Δλ = Math.toRadians(targetLon - userLon)

        val a = sin(Δφ / 2).pow(2) + cos(φ1) * cos(φ2) * sin(Δλ / 2).pow(2)
        val c = 2 * atan2(sqrt(a), sqrt(1 - a))
        val distance = R * c


        return distance
    }

    companion object {
        private const val LOCATION_PERMISSION_REQUEST_CODE = 1000
    }

    data class Restaurant(
        val Name: String = "",
        val Address: String = "",
        val Latitude: String = "",
        val Longitude: String = "",
        val avgcost: String = "",
        val link: String = "",
        val maketime: String = "",
        val waittime: String = "",
        val menuKeywords: String = "",
        val photourl: String = "",
        val type: String = "",
        val Number: String = "",
    )

    inner class RestaurantAdapter(private var restaurantList: List<Restaurant>) :
        RecyclerView.Adapter<RestaurantAdapter.RestaurantViewHolder>() {

        fun updateList(newList: List<Restaurant>) {
            restaurantList = newList
            notifyDataSetChanged()
        }

        inner class RestaurantViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            val nameTextView: TextView = itemView.findViewById(R.id.res_name)
            val costTextView: TextView = itemView.findViewById(R.id.avg_cost)
            val waitTextView: TextView = itemView.findViewById(R.id.avg_waiting)
            val cookTextView: TextView = itemView.findViewById(R.id.cooking_time)
            val imageView: ImageView = itemView.findViewById(R.id.res_photo)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RestaurantViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_restaurant, parent, false)
            return RestaurantViewHolder(view)
        }

        override fun onBindViewHolder(holder: RestaurantViewHolder, position: Int) {
            val restaurant = restaurantList[position]
            holder.nameTextView.text = restaurant.Name
            holder.costTextView.text =
                restaurant.avgcost.replace("under", "원 이하").replace("over", "원 이상")
            holder.waitTextView.text = restaurant.waittime
            holder.cookTextView.text = restaurant.maketime

            Glide.with(holder.imageView.context)
                .load(restaurant.photourl)
                .transform(CenterCrop(), RoundedCorners(20))
                .into(holder.imageView)

            holder.itemView.setOnClickListener {//detail 연결
                val restaurantId = (restaurant.Number.toIntOrNull() ?: 1)
                val intent = Intent(holder.itemView.context, Restaurant_Detail::class.java).apply {
                    putExtra("restaurantId", restaurantId.toString())  // Number 필드를 ID로 사용
                }
                holder.itemView.context.startActivity(intent)
            }
        }

        override fun getItemCount() = restaurantList.size
    }
}