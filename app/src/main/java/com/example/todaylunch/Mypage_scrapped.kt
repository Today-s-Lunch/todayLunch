package com.example.todaylunch

import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.todaylunch.databinding.FragmentMypageScrappedBinding
import com.example.todaylunch.databinding.ItemMypageScrappedBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

data class Restaurant(
    val Number: String = "",          // 레스토랑 번호
    val Name: String = "",            // 레스토랑 이름
    val Longitude: String = "",       // 경도
    val Latitude: String = "",        // 위도
    val Address: String = "",         // 주소
    val type: String = "",           // 음식 종류
    val avgcost: String = "",        // 평균 가격대
    val menuKeywords: String = "",   // 메뉴 키워드
    val maketime: String = "",       // 조리 시간
    val waittime: String = "",       // 대기 시간
    val link: String = "",           // 네이버 링크
    val photourl: String = "",       // 사진 URL
    var isBookmarked: Boolean = false // 스크랩 여부 (로컬에서만 사용)
)

class Mypage_scrapped : Fragment() {
    private var _binding: FragmentMypageScrappedBinding? = null
    private val binding get() = _binding!!
    private val scrappedAdapter = ScrappedAdapter()
    private val database = FirebaseDatabase.getInstance()
    private val auth = FirebaseAuth.getInstance()

    // 로그 태그
    companion object {
        private const val TAG = "Mypage_scrapped"
    }

    // Fragment의 View를 생성
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentMypageScrappedBinding.inflate(inflater, container, false)
        return binding.root
    }

    // View가 생성된 후 호출되는 메서드
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupRecyclerView()
        loadScrappedRestaurants()
    }

    // 리사이클러뷰 초기 설정(어댑터 연결)
    private fun setupRecyclerView() {
        binding.recyclerView.apply {
            adapter = scrappedAdapter
//            layoutManager = LinearLayoutManager(context)
//            addItemDecoration(DividerItemDecoration(context, LinearLayoutManager.VERTICAL))
        }
    }

    //스크랩한 식당이 없을 경우
    private fun updateEmptyView(isEmpty: Boolean) {
        if (isEmpty) {
            binding.recyclerView.visibility = View.GONE
            binding.emptyView.visibility = View.VISIBLE
        } else {
            binding.recyclerView.visibility = View.VISIBLE
            binding.emptyView.visibility = View.GONE
        }
    }

    // 스크랩된 레스토랑 목록을 불러오는 메서드
    private fun loadScrappedRestaurants() {
        // 현재 로그인한 사용자 확인
        val currentUser = auth.currentUser?.uid ?: run {
            Log.e(TAG, "로그인된 사용자가 없습니다")
            return
        }

        // user_scraps에서 현재 사용자의 스크랩 목록 가져오기
        database.reference.child("user_scraps").child(currentUser)
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    // 스크랩된 레스토랑 ID 목록 가져오기
                    val restaurantIds = snapshot.children
                        .filter { it.getValue(Boolean::class.java) == true }
                        .map { it.key ?: "" }
                        .filter { it.isNotEmpty() }

                    //식당 없을 때 처리
                    if (restaurantIds.isEmpty()) {
                        updateEmptyView(true)  // 스크랩된 레스토랑이 없는 경우
                        scrappedAdapter.submitList(emptyList())
                        return
                    }

                    val restaurants = mutableListOf<Restaurant>()
                    var completedRequests = 0

                    restaurantIds.forEach { restaurantId ->
                        database.reference.child("restaurants").child(restaurantId)
                            .addListenerForSingleValueEvent(object : ValueEventListener {
                                override fun onDataChange(restaurantSnapshot: DataSnapshot) {
                                    val restaurant = restaurantSnapshot.getValue(Restaurant::class.java)
                                    restaurant?.let {
                                        it.isBookmarked = true  // 스크랩된 항목이므로 true로 설정
                                        restaurants.add(it)
                                    }

                                    completedRequests++
                                    if (completedRequests == restaurantIds.size) {
                                        // 이름순으로 정렬
                                        val sortedList = restaurants.sortedBy { it.Name }
                                        scrappedAdapter.submitList(sortedList)
                                    }
                                }

                                override fun onCancelled(error: DatabaseError) {
                                    Log.e(TAG, "레스토랑 정보 로딩 실패 $restaurantId: ${error.message}")
                                    completedRequests++

                                    if (completedRequests == restaurantIds.size) {
                                        val sortedList = restaurants.sortedBy { it.Name }
                                        scrappedAdapter.submitList(sortedList)
                                    }
                                }
                            })
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    Log.e(TAG, "스크랩된 레스토랑 목록 로딩 실패: ${error.message}")
                }
            })
    }

    // Fragment 파괴 시 바인딩 정리
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

class ScrappedAdapter : RecyclerView.Adapter<ScrappedAdapter.ScrappedViewHolder>() {
        private var restaurants = listOf<Restaurant>()

        inner class ScrappedViewHolder(private val binding: ItemMypageScrappedBinding) :
            RecyclerView.ViewHolder(binding.root) {

            fun bind(restaurant: Restaurant) {
                binding.apply {
                    restaurantName.text = restaurant.Name
                    cookingTime.text = formatTime(restaurant.maketime)
                    waitingTime.text = formatTime(restaurant.waittime)
                    avgprice.text = formatPrice(restaurant.avgcost)
                    bookmarkIcon.isSelected = restaurant.isBookmarked

                    // 아이템 클릭 시 상세 정보로 이동
                    root.setOnClickListener {
                        // Intent로 상세 페이지로 이동
                        // restaurant.Number를 사용하여 해당 레스토랑 정보 전달
                    }

                    // 북마크 클릭 시 스크랩 상태 토글
                    bookmarkIcon.setOnClickListener {
                        // Firebase에서 스크랩 상태 업데이트
                        toggleBookmark(restaurant)
                    }
                }
            }
        }

        private fun toggleBookmark(restaurant: Restaurant) {
            val currentUser = FirebaseAuth.getInstance().currentUser?.uid ?: return
            val restaurantRef = FirebaseDatabase.getInstance()
                .reference
                .child("user_scraps")
                .child(currentUser)
                .child(restaurant.Number)

            restaurantRef.setValue(!restaurant.isBookmarked)
        }

        // 새로운 ViewHolder 생성
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ScrappedViewHolder {
            val binding = ItemMypageScrappedBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
            )
            return ScrappedViewHolder(binding)
        }

        // ViewHolder에 데이터 바인딩
        override fun onBindViewHolder(holder: ScrappedViewHolder, position: Int) {
            holder.bind(restaurants[position])
        }

        // 전체 아이템 개수 반환
        override fun getItemCount() = restaurants.size

        // 새로운 데이터 리스트 설정
        fun submitList(newList: List<Restaurant>) {
            restaurants = newList
            notifyDataSetChanged()
        }

        // 가격 형식 변환 (예: "5000under" -> "5,000원 이하")
        private fun formatPrice(price: String): String {
            return when (price) {
                "5000under" -> "5,000원 이하"
                "10000under" -> "10,000원 이하"
                "10000over" -> "10,000원 이상"
                else -> price
            }
        }

        // 시간 형식 변환 (예: "30m" -> "30분", "45s" -> "45초")
        private fun formatTime(time: String): String {
            return when {
                time.endsWith("s") -> "${time.removeSuffix("s")}초"
                time.endsWith("m") -> "${time.removeSuffix("m")}분"
                else -> time
            }
        }
}