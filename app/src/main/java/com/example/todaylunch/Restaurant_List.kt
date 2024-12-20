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
import com.example.todaylunch.databinding.ActivityRestaurantDetailBinding
import com.example.todaylunch.databinding.ActivityRestaurantListBinding
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.material.button.MaterialButton
import com.google.android.material.internal.ViewUtils.dpToPx
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.URL
import kotlin.math.*
import android.Manifest
import android.content.pm.PackageManager
import android.widget.ArrayAdapter
import android.widget.Spinner
import android.widget.AdapterView
import com.google.firebase.database.DatabaseReference


class Restaurant_List : AppCompatActivity() {
    private val binding: ActivityRestaurantListBinding by lazy {
        ActivityRestaurantListBinding.inflate(layoutInflater)
    }
    private lateinit var recyclerViewAdapter: RestaurantAdapter
    private var restaurantList: MutableList<Restaurant> = mutableListOf()
    private var FilterList: MutableList<Restaurant> = mutableListOf()
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private var userLat: Double = 0.0
    private var userLon: Double = 0.0
    private val API_KEY = "AIzaSyAYeZk5HR1t7izfsHrPM35RXHi_SiZZWUo"
    private lateinit var selectedFilters: Map<String, String>
    private var searchText: String = "" // 검색어를 저장할 변수

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)
        setupRecyclerView()
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)



        // 필터값 가져오기
        selectedFilters = mapOf(
            "type" to intent.getStringExtra("type").orEmpty(),
            "distance" to intent.getStringExtra("distance").orEmpty(),
            "cookingTime" to intent.getStringExtra("cookingTime").orEmpty(),
            "avgPrice" to intent.getStringExtra("avgPrice").orEmpty(),
            "waitTime" to intent.getStringExtra("waitTime").orEmpty() // 대기시간 추가
        ).filter { it.value.isNotEmpty() }
        Log.d("intentDebug", "Received Intent: ${intent.getStringExtra("type")}")

        // 입력한 검색어
        searchText = intent.getStringExtra("searchText").orEmpty()

        // 검색어가 있으면 검색바에 표시, 없으면 기본 텍스트 표시
        if (searchText.isNotEmpty()) {
            binding.searchtxt.text = searchText
        } else {
            binding.searchtxt.text = "조건을 설정해주세요"
        }

        Log.d("SelectedFiltersDebug", "Filters: $selectedFilters") // 추가된 로그
        checkLocationPermissionAndFetchLocation()
        loadRestaurants()
        val sortSpinner: Spinner = findViewById(R.id.sortSpinner)
        setupSortSpinner(sortSpinner)



        // 검색 버튼 클릭 시 검색 탭 열기/닫기
        binding.searchopen.setOnClickListener {
            val intent = Intent(this, SearchActivity::class.java).apply {
                // 검색어 전달
                putExtra("currentSearchText", searchText)
                putExtra("type", selectedFilters["type"].orEmpty())
                putExtra("distance", selectedFilters["distance"].orEmpty())
                putExtra("cookingTime", selectedFilters["cookingTime"].orEmpty())
                putExtra("avgPrice", selectedFilters["avgPrice"].orEmpty())
                putExtra("waitTime", selectedFilters["waitTime"].orEmpty())
                putExtra("shouldReset", false) // 플래그 추가
            }


            startActivity(intent)
        }

        val MYPAGE = Intent(this, MypageActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
        }

        binding.underbar.backButton.setOnClickListener {

            onBackPressedDispatcher.onBackPressed()
        }
        binding.underbar.homeButton.setOnClickListener {
            startActivity(Intent(this, StartActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            })
        }
        binding.underbar.myPageButton.setOnClickListener {
            startActivity(MYPAGE)
        }
    }
    private fun setupRecyclerView() {
        val recyclerView: RecyclerView = findViewById(R.id.restaurant_list)
        recyclerViewAdapter = RestaurantAdapter(restaurantList)
        recyclerView.adapter = recyclerViewAdapter

        binding.restaurantList.apply {
            layoutManager = LinearLayoutManager(this@Restaurant_List)
            adapter = recyclerViewAdapter
            addItemDecoration(DividerItemDecoration(context, LinearLayoutManager.VERTICAL))
            isNestedScrollingEnabled = false // 스크롤 충돌 방지
        }
    }

    private fun setupSortSpinner(sortSpinner: Spinner) {
        val sortOptions = listOf("이름순", "거리순","별점순")

        // Spinner와 연결할 ArrayAdapter 생성
        val spinnerAdapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_item,
            sortOptions
        ).apply {
            setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        }

        // Spinner에 어댑터 설정
        sortSpinner.adapter = spinnerAdapter

        // Spinner 아이템 선택 이벤트 처리
        sortSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {

            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                when (position) {
                    0 -> {
                        FilterList.sortBy { it.Name }
                        Log.d("SortingDebug", "Sorted by Name: ${FilterList.map { it.Name }}")
                    }
                    1 -> {
                        FilterList.sortBy {
                            val distance = calculateDistance(
                                userLat, userLon,
                                it.Latitude.toDoubleOrNull() ?: 0.0,
                                it.Longitude.toDoubleOrNull() ?: 0.0
                            )
                            Log.d("DistanceDebug", "Restaurant: ${it.Name}, Distance: $distance")
                            distance
                        }
                        Log.d("SortingDebug", "Sorted by Distance: ${FilterList.map { it.Name }}")
                    }
                    2 -> { // 별점순
                        FilterList.sortByDescending { it.rating }
                    }
                }
                updateRecyclerView(FilterList)
                Log.d("AdapterDebug", "RecyclerView updated with sorted list: ${FilterList.map { it.Name }}")
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
    }

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
            holder.costTextView.text = restaurant.avgcost.replace("under", "원 이하").replace("over", "원 이상")
            holder.waitTextView.text = convertTime(restaurant.waittime)
            holder.cookTextView.text = convertTime(restaurant.maketime)
            Glide.with(holder.imageView.context)
                .load(restaurant.photourl)
                .transform(CenterCrop(), RoundedCorners(20))
                .into(holder.imageView)

            holder.itemView.setOnClickListener {
                val restaurantId = (restaurant.Number.toIntOrNull() ?: 1)
                val intent = Intent(holder.itemView.context, Restaurant_Detail::class.java).apply {
                    putExtra("restaurantId", restaurantId.toString())  // Number 필드를 ID로 사용
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
        userLat = 37.544997// 숙대좌표
        userLon = 126.965905
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

        // 필터 조건 표시
        displayFilters(selectedFilters)

        lifecycleScope.launch {
            val db = FirebaseDatabase.getInstance().getReference("restaurants")
            try {
                // Firebase에서 데이터를 비동기로 가져오기
                val restaurants = withContext(Dispatchers.IO) {
                    fetchRestaurantsFromFirebase(db)
                }

                // 필터 적용
                val filteredList = withContext(Dispatchers.Default) {
                    restaurants.filter { restaurant ->
                        filterByType(restaurant) &&
                                filterByCookingTime(restaurant) &&
                                filterByPrice(restaurant) &&
                                filterByWaitTime(restaurant) &&
                                filterByDistance(restaurant) &&
                                filterBySearchText(restaurant)
                    }
                }

                Log.d("FilteredList", "Filtered: ${filteredList.map { it.Name }}")
                FilterList = filteredList.toMutableList()
                FilterList.sortBy { it.Name } // 초깃값 이름순 정렬
                updateRecyclerView(FilterList)

            } catch (e: Exception) {
                Log.e("LoadError", "Failed to load restaurants: ${e.message}")
            }
        }
    }


    private suspend fun fetchRestaurantsFromFirebase(db: DatabaseReference): List<Restaurant> {
        return withContext(Dispatchers.IO) {
            val snapshot = db.get().await() // await를 활용해 데이터를 기다립니다
            snapshot.children.mapNotNull { it.getValue(Restaurant::class.java) }
        }
    }

    private fun updateRecyclerView(filteredList: List<Restaurant>) {
        lifecycleScope.launch(Dispatchers.Main) {
            recyclerViewAdapter.updateList(filteredList)
            Log.d("AdapterDebug", "RecyclerView updated with filtered list size: ${filteredList.size}")
        }
    }

    private fun filterByType(restaurant: Restaurant): Boolean {
        val typeFilter = selectedFilters["type"] ?: return true
        val selectedTypes = typeFilter.split(",") // 다중 선택 값 처리

        val typeMapping = mapOf(
            "한식" to "Korean",
            "중식" to "Chinese",
            "양식" to "Western",
            "일식" to "Japanese",
            "분식" to "Bunsik",
            "간편식" to "Light",
            "아시안" to "Vietnamese"
        )

        val mappedTypes = selectedTypes.mapNotNull { typeMapping[it] }

        // 레스토랑의 type 값이 선택된 타입 중 하나와 일치하면 true 반환
        return mappedTypes.any { it.equals(restaurant.type.trim(), ignoreCase = true) }
    }



    companion object {
        const val LOCATION_PERMISSION_REQUEST_CODE = 1001
    }



    private fun filterByDistance(restaurant: Restaurant): Boolean {
        val distanceFilter = selectedFilters["distance"] ?: return true // 필터 조건 없으면 무조건 통과
        val maxDistanceMinutes = when (distanceFilter) {
            "도보 5분" -> 5
            "도보 10분" -> 10
            "도보 15분" -> 15
            else -> Int.MAX_VALUE
        }

        // 거리 계산
        val distance = calculateDistance(
            userLat, userLon,
            restaurant.Latitude.toDoubleOrNull() ?: 0.0,
            restaurant.Longitude.toDoubleOrNull() ?: 0.0
        )

        // 도보 시간 계산 (신호등 대기 포함)
        val walkingTime = calculateWalkingTime(distance)
        Log.d("DistanceFilter", "Restaurant: ${restaurant.Name}, Distance: $distance meters, Walking Time: $walkingTime minutes")

        // 최대 거리 조건을 만족하는지 반환
        return walkingTime <= maxDistanceMinutes
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

        return R * c // 미터 단위 거리 반환
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

    // 검색어 필터링 함수 추가
    private fun filterBySearchText(restaurant: Restaurant): Boolean {
        // 검색어가 비어있으면 true 반환 (필터링하지 않음)
        if (searchText.isEmpty()) return true

        // 식당 이름 확인
        if (restaurant.Name.contains(searchText)) return true

        // 메뉴 키워드를 공백으로 분리하여 각각 확인
        val menuKeywords = restaurant.menuKeywords.split(" ")
        return menuKeywords.any { keyword ->
            keyword.contains(searchText)
        }
    }

    private fun displayFilters(selectedFilters: Map<String, String>) {

        val flexboxLayout = binding.filterConditions
        flexboxLayout.removeAllViews() // 기존 버튼 초기화

        // 조건별로 동적 버튼 생성
        for ((key, value) in selectedFilters) {
            if (value.isNullOrEmpty()) continue // 값이 없으면 스킵

            // 조건 값으로 버튼 생성 (쉼표로 구분된 값 처리)
            val values = value.split(",")
            for (item in values) {
                val button = MaterialButton(this).apply {
                    layoutParams = ViewGroup.MarginLayoutParams(
                        ViewGroup.LayoutParams.WRAP_CONTENT,
                        dpToPx(38)
                    ).apply {
                        setMargins(dpToPx(4), dpToPx(4), dpToPx(4), dpToPx(4)) // 버튼 간격
                    }
                    text = item // 조건값만 버튼에 표시
                    setTextColor(getColor(android.R.color.black))

                    // 버튼 스타일 설정
                    strokeColor = getColorStateList(R.color.red1)
                    strokeWidth = 2
                    backgroundTintList = getColorStateList(R.color.selectedButtonTint)
                    shapeAppearanceModel = shapeAppearanceModel.toBuilder()
                        .setAllCornerSizes(40f)
                        .build()


                }
                flexboxLayout.addView(button) // 버튼 추가
            }
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
        var rating: Float = 0f // 평균 별점 추가
    )
    //ORSM이용??
    /*private fun getWalkingTimeFromOSRM(
        originLat: Double,
        originLon: Double,
        destLat: Double,
        destLon: Double
    ): Int {
        val baseUrl = "http://router.project-osrm.org/route/v1/foot"
        val url = "$baseUrl/$originLon,$originLat;$destLon,$destLat?overview=false&steps=false"

        return try {
            Log.d("OSRM API Request", "URL: $url")
            val response = URL(url).readText() // API 호출
            Log.d("OSRM API Response", "Response: $response")

            val json = JSONObject(response)

            // OSRM 응답에서 경로 정보 가져오기
            val routes = json.getJSONArray("routes")
            if (routes.length() > 0) {
                // 첫 번째 경로의 legs[0] → duration 값 추출
                val legs = routes.getJSONObject(0).getJSONArray("legs")
                if (legs.length() > 0) {
                    val duration = legs.getJSONObject(0).getDouble("duration") // 초 단위
                    Log.d("OSRM", "Duration: ${duration / 60} minutes")
                    (duration / 60).toInt() // 초 → 분 변환 후 반환
                } else {
                    Log.e("OSRM", "No legs found in route")
                    Int.MAX_VALUE
                }
            } else {
                Log.e("OSRM", "No routes found")
                Int.MAX_VALUE
            }
        } catch (e: Exception) {
            Log.e("OSRM", "Error fetching walking time: ${e.message}")
            Int.MAX_VALUE // 실패 시 기본값 반환
        }
    }*/

}
