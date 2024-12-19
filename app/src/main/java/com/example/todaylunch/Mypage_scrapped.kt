package com.example.todaylunch

import android.content.Intent
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
    private val scrappedAdapter = ScrappedAdapter(this)
    private val database = FirebaseDatabase.getInstance()
    private val auth = FirebaseAuth.getInstance()

    companion object {
        private const val TAG = "Mypage_scrapped"
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentMypageScrappedBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupRecyclerView()
        loadScrappedRestaurants()
    }

    private fun setupRecyclerView() {
        binding.recyclerView.adapter = scrappedAdapter
    }

    internal  fun updateEmptyView(isEmpty: Boolean) {
        if (isEmpty) {
            binding.recyclerView.visibility = View.GONE
            binding.emptyView.visibility = View.VISIBLE
        } else {
            binding.recyclerView.visibility = View.VISIBLE
            binding.emptyView.visibility = View.GONE
        }
    }

    private fun loadScrappedRestaurants() {
        val currentUser = auth.currentUser?.uid ?: run {
            Log.e(TAG, "로그인된 사용자가 없습니다")
            return
        }

        database.reference.child("user_scraps").child(currentUser)
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val restaurantIds = snapshot.children
                        .filter { it.getValue(Boolean::class.java) == true }
                        .map { it.key ?: "" }
                        .filter { it.isNotEmpty() }

                    if (restaurantIds.isEmpty()) {
                        updateEmptyView(true)
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
                                        it.isBookmarked = true
                                        restaurants.add(it)
                                    }

                                    completedRequests++
                                    if (completedRequests == restaurantIds.size) {
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

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}


class ScrappedAdapter(private val fragment: Mypage_scrapped) : RecyclerView.Adapter<ScrappedAdapter.ScrappedViewHolder>() {
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

                root.setOnClickListener {
                    val intent = Intent(root.context, Restaurant_Detail::class.java)
                    intent.putExtra("restaurantId", restaurant.Number)
                    root.context.startActivity(intent)
                }

                bookmarkIcon.setOnClickListener {
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

        val newBookmarkState = !restaurant.isBookmarked
        restaurantRef.setValue(newBookmarkState).addOnCompleteListener { task ->
            if (task.isSuccessful) {
                restaurant.isBookmarked = newBookmarkState
                if (!newBookmarkState) {
                    val updatedList = restaurants.filter { it.Number != restaurant.Number }
                    submitList(updatedList)
                }
            } else {
                Log.e("ScrappedAdapter", "북마크 상태 업데이트 실패: ${task.exception?.message}")
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ScrappedViewHolder {
        val binding = ItemMypageScrappedBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ScrappedViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ScrappedViewHolder, position: Int) {
        holder.bind(restaurants[position])
    }

    override fun getItemCount() = restaurants.size

    fun submitList(newList: List<Restaurant>) {
        restaurants = newList
        notifyDataSetChanged()

        // 빈 리스트 상태 확인 후 UI 업데이트
        fragment.updateEmptyView(restaurants.isEmpty())
    }

    private fun formatPrice(price: String): String {
        return when (price) {
            "5000under" -> "5,000원 이하"
            "10000under" -> "10,000원 이하"
            "10000over" -> "10,000원 이상"
            else -> price
        }
    }

    private fun formatTime(time: String): String {
        return when {
            time.endsWith("s") -> "${time.removeSuffix("s")}초"
            time.endsWith("m") -> "${time.removeSuffix("m")}분"
            else -> time
        }
    }
}
