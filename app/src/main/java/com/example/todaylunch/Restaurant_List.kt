package com.example.todaylunch

import android.content.Intent
import android.content.res.ColorStateList
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
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.bitmap.CenterCrop
import com.bumptech.glide.load.resource.bitmap.RoundedCorners
import com.example.todaylunch.databinding.ActivityRestaurantListBinding
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.material.button.MaterialButton
import com.google.android.material.internal.ViewUtils.dpToPx
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.URL
import kotlin.math.*
import android.Manifest
import android.content.pm.PackageManager


class Restaurant_List : AppCompatActivity() {
    private val binding: ActivityRestaurantListBinding by lazy {
        ActivityRestaurantListBinding.inflate(layoutInflater)
    }
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private var userLat: Double = 0.0
    private var userLon: Double = 0.0
    private val API_KEY = "AIzaSyAYeZk5HR1t7izfsHrPM35RXHi_SiZZWUo"
    private lateinit var selectedFilters: Map<String, String>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)



        // 필터값 가져오기
        selectedFilters = mapOf(
            "type" to intent.getStringExtra("type").orEmpty(),
            "distance" to intent.getStringExtra("distance").orEmpty(),
            "cookingTime" to intent.getStringExtra("cookingTime").orEmpty(),
            "avgPrice" to intent.getStringExtra("avgPrice").orEmpty(),
            "waitTime" to intent.getStringExtra("waitTime").orEmpty() // 대기시간 추가
        ).filter { it.value.isNotEmpty() }
        Log.d("SelectedFiltersDebug", "Filters: $selectedFilters") // 추가된 로그
        checkLocationPermissionAndFetchLocation()
        loadRestaurants()

        binding.underbar.backButton.setOnClickListener { onBackPressedDispatcher.onBackPressed() }
        binding.underbar.homeButton.setOnClickListener {
            startActivity(Intent(this, StartActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            })
        }
    }

    inner class RestaurantAdapter(private val restaurantList: List<Restaurant>) :
        RecyclerView.Adapter<RestaurantAdapter.RestaurantViewHolder>() {

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
            holder.costTextView.text = restaurant.avgcost.replace("under", "원 이하").replace("over", "원 이상")
            holder.waitTextView.text = convertTime(restaurant.waittime)
            holder.cookTextView.text = convertTime(restaurant.maketime)
            Glide.with(holder.imageView.context)
                .load(restaurant.photourl)
                .transform(CenterCrop(), RoundedCorners(20))
                .into(holder.imageView)
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
    }
    private fun checkLocationPermissionAndFetchLocation() {
        val isTesting = true // 테스트 모드 플래그
        if (isTesting) {
            setMockLocation()
        } else {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                fetchUserLocation()
            } else {
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                    LOCATION_PERMISSION_REQUEST_CODE
                )
            }
        }
    }
    private fun setMockLocation() { // 수동위치
        userLat = 37.545169 // 숙대좌표
        userLon = 126.964268
        Log.d("MockLocation", "Lat: $userLat, Lon: $userLon")
        loadRestaurants()
    }
    private fun fetchUserLocation(){
        try {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                fusedLocationClient.lastLocation.addOnSuccessListener { location ->
                    if (location != null) {
                        userLat = location.latitude
                        userLon = location.longitude
                        Log.d("UserLocation", "Lat: $userLat, Lon: $userLon")
                        loadRestaurants() // 위치 정보를 기반으로 식당 목록 가져오기
                    } else {
                        Log.e("UserLocation", "Unable to fetch location")
                    }
                }.addOnFailureListener { e ->
                    Log.e("UserLocation", "Failed to get location: ${e.message}")
                }
            } else {
                Log.e("Permission", "Permission not granted")
            }
        } catch (e: SecurityException) {
            Log.e("Permission", "SecurityException: ${e.message}")
        }
    }
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // 권한이 허용된 경우 위치 정보 가져오기
                fetchUserLocation()
            } else {
                // 권한이 거부된 경우 로그 표시
                Log.e("Permission", "Location permission denied")
            }
        }
    }



    private fun loadRestaurants() {
        if (!::selectedFilters.isInitialized) {
            Log.e("Error", "selectedFilters is not initialized")
            return
        }

        displayFilters(selectedFilters)
        val db = FirebaseDatabase.getInstance().reference
        db.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val restaurants = snapshot.children.mapNotNull {
                    it.getValue(Restaurant::class.java)
                }
                lifecycleScope.launch {
                    val filteredList = restaurants.filter { restaurant ->
                        filterByType(restaurant) &&
                                filterByCookingTime(restaurant) &&
                                filterByPrice(restaurant) &&
                                filterByWaitTime(restaurant) &&
                                filterByDistanceAsync(restaurant)
                    }
                    Log.d("FilteredList", "Filtered: ${filteredList.map { it.Name }}")

                    val adapter = RestaurantAdapter(filteredList)
                    binding.restaurantList.apply {
                        layoutManager = LinearLayoutManager(this@Restaurant_List)
                        this.adapter = adapter
                        addItemDecoration(DividerItemDecoration(context, LinearLayoutManager.VERTICAL))
                    }
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("FirebaseError", error.message)
            }
        })
    }

    private fun filterByType(restaurant: Restaurant): Boolean {
        val typeFilter = selectedFilters["type"] ?: return true

        // 조건 필터와 JSON 데이터 값 매핑
        val typeMapping = mapOf(
            "한식" to "Korean",
            "중식" to "Chinese",
            "양식" to "Western",
            "일식" to "Japanese",
            "분식" to "Bunsik",
            "간편식" to "Light",
            "아시안" to "Vietnamese"
        )

        val mappedType = typeMapping[typeFilter] ?: return true

        Log.d(
            "Filtering",
            "Restaurant: ${restaurant.Name}, Filter: $mappedType, Actual: ${restaurant.type}"
        )

        // 레스토랑의 type 필드가 필터 값과 일치하는지 확인
        return restaurant.type.trim().equals(mappedType, ignoreCase = true)
    }

    private suspend fun filterByDistanceAsync(restaurant: Restaurant): Boolean {
        val distanceFilter = selectedFilters["distance"] ?: return true
        val maxDistanceMinutes = when (distanceFilter) {
            "도보 5분" -> 5
            "도보 10분" -> 10
            "도보 15분" -> 15
            else -> Int.MAX_VALUE
        }

        return withContext(Dispatchers.IO) {
            val walkingTime = getWalkingTimeFromGoogleMapsAPI(
                originLat = userLat,
                originLon = userLon,
                destLat = restaurant.Latitude.toDoubleOrNull() ?: 0.0,
                destLon = restaurant.Longitude.toDoubleOrNull() ?: 0.0
            )
            Log.d("WalkingTime", "Restaurant: ${restaurant.Name}, Walking Time: $walkingTime minutes")
            walkingTime <= maxDistanceMinutes
        }
    }
    private fun getWalkingTimeFromGoogleMapsAPI( originLat: Double,
                                                 originLon: Double,
                                                 destLat: Double,
                                                 destLon: Double
    ): Int {
        return try {
            val url = URL(
                "https://maps.googleapis.com/maps/api/distancematrix/json?origins=$originLat,$originLon&destinations=$destLat,$destLon&mode=walking&key=$API_KEY"
            )
            Log.d("DistanceMatrixAPI Request", url.toString())
            val response = url.readText()
            val json = JSONObject(response)
            val duration = json
                .getJSONArray("rows")
                .getJSONObject(0)
                .getJSONArray("elements")
                .getJSONObject(0)
                .getJSONObject("duration")
                .getInt("value") // Duration in seconds
            Log.d("DistanceMatrixAPI Response", json.toString())
            duration / 60 // Convert to minutes
        } catch (e: Exception) {
            Log.e("DistanceMatrixAPI", "Error: ${e.message}")
            Int.MAX_VALUE
        }
    }/*originLat: Double, originLon: Double, destLat: Double, destLon: Double): Int {
        return try {
            // URL 생성
            val url = URL(
                "https://maps.googleapis.com/maps/api/directions/json?origin=$originLat,$originLon&destination=$destLat,$destLon&mode=walking&key=$API_KEY"
            )
            Log.d("API Request URL", url.toString())


            // API 호출 및 응답 받기
            val response = url.readText()
            val json = JSONObject(response)

            // JSON 응답에서 경로(route) 및 다리(legs) 정보 추출
            val routes = json.optJSONArray("routes") ?: throw Exception("No routes found in response")
            val legs = routes.optJSONObject(0)?.optJSONArray("legs") ?: throw Exception("No legs found in route")
            val duration = legs.optJSONObject(0)?.optJSONObject("duration")?.optInt("value")
                ?: throw Exception("Duration not found in response")

            // 초 단위 시간을 분으로 변환하여 반환
            duration / 60 // Convert to minutes
        } catch (e: Exception) {
            // 로그 출력 및 디폴트 값 반환
            Log.e("GoogleMapsAPI", "Error fetching walking time: ${e.message}")
            Int.MAX_VALUE
        }*/


    companion object {
        const val LOCATION_PERMISSION_REQUEST_CODE = 1001
    }


    private fun calculateDistance(userLat: Double, userLon: Double, targetLat: Double, targetLon: Double): Double {
        if (userLat == 0.0 || userLon == 0.0 || targetLat == 0.0 || targetLon == 0.0) {
            return Double.MAX_VALUE
        }
        val R = 6371e3 // meters
        val φ1 = Math.toRadians(userLat)
        val φ2 = Math.toRadians(targetLat)
        val Δφ = Math.toRadians(targetLat - userLat)
        val Δλ = Math.toRadians(targetLon - userLon)

        val a = sin(Δφ / 2).pow(2) + cos(φ1) * cos(φ2) * sin(Δλ / 2).pow(2)
        val c = 2 * atan2(sqrt(a), sqrt(1 - a))
        return (R * c) / 1000 // kilometers
    }

    private fun filterByCookingTime(restaurant: Restaurant): Boolean {
        val cookingTimeFilter = selectedFilters["cookingTime"] ?: return true
        val maxTimeInSeconds = when (cookingTimeFilter) {
            "조리 30초" -> 30
            "조리 5분" -> 5 * 60
            "조리 10분" -> 10 * 60
            else -> Int.MAX_VALUE
        }

        val timePattern = Regex("(\\d+)(m|s)")
        val matchResult = timePattern.find(restaurant.maketime)
        val restaurantTimeInSeconds = matchResult?.let { result ->
            val value = result.groupValues[1].toInt()
            when (result.groupValues[2]) {
                "m" -> value * 60 // 분 -> 초 변환
                "s" -> value // 초 그대로
                else -> Int.MAX_VALUE
            }
        } ?: Int.MAX_VALUE

        Log.d(
            "Filtering",
            "Restaurant: ${restaurant.Name}, Filter: $maxTimeInSeconds, Actual: $restaurantTimeInSeconds"
        )

        // 선택된 시간보다 작거나 같은 레스토랑만 반환
        return restaurantTimeInSeconds <= maxTimeInSeconds
    }

    private fun filterByPrice(restaurant: Restaurant): Boolean {
        val priceFilter = selectedFilters["avgPrice"] ?: return true
        Log.d("Filtering", "Price Filter: $priceFilter")
        // 필터 조건과 JSON 값 매핑 (순위 기반)
        val priceMapping = mapOf(
            "5000원 이하" to 1,
            "10000원 이하" to 2,
            "10000원 이상" to 3
        )

        val avgCostMapping = mapOf(
            "5000under" to 1,
            "10000under" to 2,
            "10000over" to 3
        )

        // 필터의 순위와 레스토랑의 순위를 가져옴
        val filterRank = priceMapping[priceFilter] ?: return true
        val restaurantRank = avgCostMapping[restaurant.avgcost.trim()] ?: Int.MAX_VALUE

        // 디버그 로그 출력
        Log.d(
            "Filtering",
            "Restaurant: ${restaurant.Name}, Filter: $priceFilter, " +
                    "Filter Rank: $filterRank, Restaurant AvgCost: ${restaurant.avgcost.trim()}, " +
                    "Restaurant Rank: $restaurantRank"
        )

        // 레스토랑의 가격대가 필터 조건보다 같거나 낮은 순위인지 확인
        return restaurantRank <= filterRank
    }



    private fun displayFilters(selectedFilters: Map<String, String>) {
        val flexboxLayout = binding.filterConditions
        flexboxLayout.removeAllViews() // 기존 필터 초기화

        for ((key, value) in selectedFilters) {
            Log.d("IntentDebug", "Key: $key, Value: $value")

            val button = MaterialButton(this).apply {
                layoutParams = ViewGroup.MarginLayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT, // 너비를 70dp로 설정
                    dpToPx(38)
                ).apply {
                    setMargins(dpToPx(4), dpToPx(4), dpToPx(4), dpToPx(4)) // 버튼 간격
                }
                text = value // 필터 조건 텍스트 설정
                setTextColor(getColor(android.R.color.black)) // 버튼 텍스트 색상

                // 스타일 적용 (배경색 및 코너 반경)



                strokeColor =getColorStateList(R.color.red1)
                strokeWidth= (2).toInt()
                backgroundTintList = getColorStateList(R.color.selectedButtonTint)
                shapeAppearanceModel = shapeAppearanceModel.toBuilder()
                    .setAllCornerSizes(40f) // 모서리 둥글게
                    .build()

                // 버튼 클릭 시 동작 (필요 시 필터 제거)
                //setOnClickListener {
                  //  removeFilter(key)
                   // displayFilters(selectedFilters) // 버튼 재표시
                    //loadRestaurants() // 필터 변경 후 데이터 갱신
               // }
            }
            flexboxLayout.addView(button)
        }
    }
    private fun dpToPx(dp: Int): Int {
        val density = resources.displayMetrics.density
        return (dp * density).toInt()
    }
    // 필터 제거 (선택된 필터를 삭제)
    private fun removeFilter(filterKey: String) {
        (selectedFilters as MutableMap).remove(filterKey) // 필터 삭제
    }
    private fun filterByWaitTime(restaurant: Restaurant): Boolean {
        val waitTimeFilter = selectedFilters["waitTime"] ?: return true
        val maxWaitTimeInMinutes = when (waitTimeFilter) {
            "0분 대기" -> 0
            "5분 대기" -> 5
            "20분 대기" -> 20
            else -> Int.MAX_VALUE
        }

        val timePattern = Regex("(\\d+)(m|s)")
        val matchResult = timePattern.find(restaurant.waittime)
        val restaurantWaitTimeInMinutes = matchResult?.let { result ->
            val value = result.groupValues[1].toInt()
            when (result.groupValues[2]) {
                "m" -> value // 분 그대로
                "s" -> value / 60 // 초를 분으로 변환
                else -> Int.MAX_VALUE
            }
        } ?: Int.MAX_VALUE

        Log.d(
            "Filtering",
            "Restaurant: ${restaurant.Name}, Filter: $maxWaitTimeInMinutes, Actual: $restaurantWaitTimeInMinutes"
        )

        // 선택된 대기시간보다 작거나 같은 레스토랑만 반환
        return restaurantWaitTimeInMinutes <= maxWaitTimeInMinutes
    }
    data class Restaurant(
        val Name: String = "",
        val waittime: String = "",
        val Longitude: String = "",
        val Latitude: String = "",
        val photourl: String = "",
        val link: String = "",
        val Address: String = "",
        val type: String = "",
        val maketime: String = "",
        val avgcost: String = "",
        val Number: String = "",
        val MenuKeywords: String = ""
    )
}
