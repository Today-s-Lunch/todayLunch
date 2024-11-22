package com.example.todaylunch

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
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

class MapActivity : AppCompatActivity() {


    private val binding: ActivityMapBinding by lazy { ActivityMapBinding.inflate(layoutInflater) }
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var adapter: RestaurantAdapter

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

        // 사용자 위치 가져오기 및 데이터 로드
        getUserLocation()
        binding.upbar.backButton.setOnClickListener { onBackPressedDispatcher.onBackPressed() }
        binding.downbar.homeButton.setOnClickListener {
            startActivity(Intent(this, StartActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            })
        }
        binding.downbar.mapButton.setOnClickListener {
            startActivity(Intent(this, MapActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            })
        }
    }

    private fun setupRecyclerView() {
        adapter = RestaurantAdapter(emptyList()) // 초기 데이터는 빈 리스트
        binding.restaurantList.apply {
            layoutManager = LinearLayoutManager(this@MapActivity)
            adapter = this@MapActivity.adapter
        }
    }

    private fun setupBottomSheet() {
        val bottomSheetBehavior = BottomSheetBehavior.from(binding.bottomSheet)
        bottomSheetBehavior.state = BottomSheetBehavior.STATE_COLLAPSED
        bottomSheetBehavior.peekHeight = 200
    }

    private fun getUserLocation() {
        val useTestLocation = true // 테스트 좌표 사용 여부

        if (useTestLocation) {

            val testLat =  37.545169 // 숙대좌표

            val testLng = 126.970833

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

                    val latitude = restaurant?.Latitude?.toDoubleOrNull()
                    val longitude = restaurant?.Longitude?.toDoubleOrNull()

                    // 유효하지 않은 좌표를 가진 데이터는 필터링
                    if (latitude == null || longitude == null) {
                        Log.e("InvalidRestaurant", "Invalid location for: ${restaurant?.Name}")
                        return@mapNotNull null
                    }

                    // 유효한 데이터만 반환
                    restaurant
                }

                lifecycleScope.launch {
                    val sortedList = restaurants
                        .map { restaurant ->
                            val distance = calculateDistance(
                                userLat, userLng,
                                restaurant.Latitude.toDoubleOrNull() ?: 0.0,
                                restaurant.Longitude.toDoubleOrNull() ?: 0.0
                            )
                            val walkingTime = calculateWalkingTime(distance)

                            Log.d(
                                "DistanceDebug",
                                "Name: ${restaurant.Name}, Distance: $distance meters, Walking Time: $walkingTime mins"
                            )

                            restaurant to walkingTime
                        }
                        .sortedBy { it.second } // 도보 시간 기준 정렬
                        .take(5) // 상위 5개 선택

                    sortedList.forEach {
                        val (restaurant, walkingTime) = it
                        Log.d(
                            "SortedRestaurant",
                            "Name: ${restaurant.Name}, Walking Time: $walkingTime mins"
                        )
                    }

                    // RecyclerView에 업데이트
                    updateRecyclerView(sortedList.map { it.first })
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("FirebaseError", error.message)
            }
        })
    }

    private fun updateRecyclerView(nearbyRestaurants: List<Restaurant>) {
        adapter.updateList(nearbyRestaurants)
    }

    private fun calculateWalkingTime(distanceInMeters: Double): Int {
        val walkingSpeed = 1.25 // m/s (평균 보행 속도)

        // 신호등 대기 시간 계산 (100m당 1개의 교차로 가정)
        val intersectionCount = (distanceInMeters / 100).toInt()
        val trafficLightDelay = intersectionCount * 30 // 교차로당 30초 대기 가정

        // 도보 시간 계산
        val walkingTimeInSeconds = (distanceInMeters / walkingSpeed).toInt() + trafficLightDelay
        return (walkingTimeInSeconds / 60).toInt() // 분 단위로 반환
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

        // 디버깅용 로그 추가
        Log.d("CalculateDistance", "userLat: $userLat, userLon: $userLon")
        Log.d("CalculateDistance", "targetLat: $targetLat, targetLon: $targetLon")
        Log.d("CalculateDistance", "Δφ: $Δφ, Δλ: $Δλ")

        val a = sin(Δφ / 2).pow(2) + cos(φ1) * cos(φ2) * sin(Δλ / 2).pow(2)
        val c = 2 * atan2(sqrt(a), sqrt(1 - a))
        val distance = R * c

        Log.d("CalculateDistance", "Calculated Distance: $distance meters")
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
            restaurantList = newList // 새로운 리스트로 덮어씌움
            notifyDataSetChanged() // 변경 사항 알림
            Log.d("AdapterDebug", "RecyclerView updated with list size: ${restaurantList.size}")
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
            holder.waitTextView.text = convertTime(restaurant.waittime)
            holder.cookTextView.text = convertTime(restaurant.maketime)


            Glide.with(holder.imageView.context)
                .load(restaurant.photourl)
                .transform(CenterCrop(), RoundedCorners(20))
                .into(holder.imageView)

            holder.itemView.setOnClickListener {
                val restaurantId = (restaurant.Number.toIntOrNull() ?: 1)
                val intent = Intent(holder.itemView.context, Restaurant_Detail::class.java).apply {
                    putExtra("restaurantId", restaurantId.toString())
                }
                holder.itemView.context.startActivity(intent)
            }
        }

        override fun getItemCount() = restaurantList.size

        private fun convertTime(timeString: String): String {
            val timePattern = Regex("(\\d+)(m|s)")
            val matchResult = timePattern.find(timeString)
            return matchResult?.let {
                val value = it.groupValues[1].toInt()
                when (it.groupValues[2]) {
                    "m" -> "$value 분"
                    "s" -> "$value 초"
                    else -> timeString
                }
            } ?: timeString
        }
        private fun getUserLocation() { //우ㅣ치임의설정

            val testLat = 37.554722 // 테스트 위도
            val testLng = 126.970833 // 테스트 경도

            Log.d("TestLocation", "Using test location: ($testLat, $testLng)")
            fetchNearbyRestaurants(testLat, testLng)
        }
    }
}
