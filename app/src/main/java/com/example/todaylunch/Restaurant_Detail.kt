package com.example.todaylunch

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.todaylunch.databinding.ActivityRestaurantDetailBinding
import com.example.todaylunch.databinding.ItemReviewBinding
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.database
import com.google.firebase.auth.auth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

class Restaurant_Detail : AppCompatActivity() {
    val binding: ActivityRestaurantDetailBinding by lazy { ActivityRestaurantDetailBinding.inflate(layoutInflater) }
    private var isScraped = false  // 스크랩 상태를 추적하는 변수
    private lateinit var reviewAdapter: ReviewAdapter
    private val auth = FirebaseAuth.getInstance()
    private val database = FirebaseDatabase.getInstance()
    private lateinit var restaurantId: String

    data class Restaurant(
        val Number: String = "",
        val Name: String = "",
        val Longitude: String = "",
        val Latitude: String = "",
        val Address: String = "",
        val type: String = "",
        val avgcost: String = "",
        val maketime: String = "",
        val waittime: String = "",
        val link: String = "",
        val photourl: String = "",
        val menuKeywords: String = ""
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)
        
        //로그인한 사용자 받아오기
        val currentUserId = auth.currentUser?.uid ?: return

        // RecyclerView 초기화
        setupRecyclerView()

        restaurantId = intent.getStringExtra("restaurantId") ?: "1"

        // 스크랩 상태 확인
        checkScrapStatus(currentUserId, restaurantId)

        // 스크랩 버튼 클릭 리스너
        binding.scrapButton.setOnClickListener {
            toggleScrap(currentUserId, restaurantId)
        }

        //식당 로딩 함수
        loadRestaurantData(restaurantId)
        //리뷰 로딩 함수
        loadReviews(restaurantId)

        //-----------------------------------------------------------언더바 설정
        // 버튼 클릭 리스너 설정
        binding.underbar.backButton.setOnClickListener {
            onBackPressedDispatcher.onBackPressed() // 이전 페이지로 이동
        }

        val goToStartActivity = Intent(this, StartActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
        }

        binding.underbar.homeButton.setOnClickListener {
            startActivity(goToStartActivity)
        }

        binding.underbar.myPageButton.setOnClickListener {
            val MYPAGE = Intent(this, MypageActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            }
            startActivity(MYPAGE)
        }

        //리뷰 작성하기로 화면이동
        binding.reviewButton.setOnClickListener {
            // ReviewActivity로 이동
            val intent = Intent(this, ReviewActivity::class.java)
            intent.putExtra("restaurantId", restaurantId)  // 식당 ID
            startActivity(intent)
        }

        //리뷰 더보기 버튼 클릭 리스너
        binding.reviewMore.setOnClickListener {
            reviewAdapter.toggleShowAll()
            binding.reviewMore.visibility = if (reviewAdapter.showAllReviews) View.GONE else View.VISIBLE
        }
    }

    override fun onResume() {
        super.onResume()
        // 리뷰 작성 후에 바로 리뷰 보이도록
        loadReviews(restaurantId)
    }

    private fun setupRecyclerView() {
        reviewAdapter = ReviewAdapter()
        binding.reviewRecyclerView.apply {
            layoutManager = LinearLayoutManager(this@Restaurant_Detail)
            adapter = reviewAdapter
        }
    }

    private fun formatTime(time: String): String {
        return when {
            time.endsWith("s") -> "${time.removeSuffix("s")}초"
            time.endsWith("m") -> "${time.removeSuffix("m")}분"
            else -> time
        }
    }

    private fun formatPrice(price: String): String {
        return when (price) {
            "5000under" -> "5,000원 이하"
            "10000under" -> "10,000원 이하"
            "10000over" -> "10,000원 이상"
            else -> price
        }
    }

    private fun loadRestaurantData(restaurantId: String) {
        val db = Firebase.database.reference.child("restaurants")

        db.child(restaurantId).get().addOnSuccessListener { snapshot ->
            val restaurant = snapshot.getValue(Restaurant::class.java)

            restaurant?.let {
                // 이미지 로딩
                Glide.with(this)
                    .load(it.photourl)
                    .error(ColorDrawable(Color.GRAY))
                    .into(binding.restaurantImage)

                // 식당명
                binding.restaurantName.text = it.Name
                // 조리시간
                binding.cookingTime.text = formatTime(it.maketime)
                // 가격대
                binding.priceRange.text = formatPrice(it.avgcost)
                // 대기시간
                binding.waitTime.text = formatTime(it.waittime)
                // 상세정보 버튼 클릭 리스너
                binding.detailButton.setOnClickListener { _ ->
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(it.link))
                    startActivity(intent)
                }
            }
        }.addOnFailureListener { exception ->
            Toast.makeText(this, "데이터를 불러오는데 실패했습니다.", Toast.LENGTH_SHORT).show()
            Log.e("RestaurantDetailActivity", "Error loading restaurant data", exception)
        }
    }

    companion object {
        fun start(context: Context, restaurantId: String) {
            val intent = Intent(context, ActivityRestaurantDetailBinding::class.java).apply {
                putExtra("restaurantId", restaurantId)
            }
            context.startActivity(intent)
        }
    }

    inner class ReviewAdapter : RecyclerView.Adapter<ReviewAdapter.ReviewViewHolder>() {
        private var reviews = listOf<ReviewItem>()
        var showAllReviews = false
            private set

        fun setReviews(newReviews: List<ReviewItem>) {
            reviews = newReviews
            notifyDataSetChanged()
        }

        fun toggleShowAll() {
            showAllReviews = !showAllReviews
            notifyDataSetChanged()
        }

        override fun getItemCount(): Int =
            if (showAllReviews) reviews.size else minOf(reviews.size, 4)

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ReviewViewHolder {
            val binding = ItemReviewBinding.inflate(LayoutInflater.from(parent.context), parent, false)
            return ReviewViewHolder(binding)
        }

        override fun onBindViewHolder(holder: ReviewViewHolder, position: Int) {
            holder.bind(reviews[position])
        }

        inner class ReviewViewHolder(private val binding: ItemReviewBinding)
            : RecyclerView.ViewHolder(binding.root) {
            fun bind(review: ReviewItem) {
                binding.nickname.text = review.nickname
                binding.ratingBar.rating = review.rating
                binding.content.text = review.content
                binding.createdAt.text = review.createdAt
            }
        }
    }

    data class ReviewItem(
        val id: String,
        val userId: String,
        val nickname: String,
        val rating: Float,
        val content: String,
        val createdAt: String
    )
    
    //리뷰 불러오기
    private fun loadReviews(restaurantId: String) {
        val db = Firebase.database.reference
        val reviewsList = mutableListOf<ReviewItem>()

        reviewAdapter.setReviews(emptyList())

        // reviews/restaurantId 경로에서 리뷰 데이터 가져오기
        db.child("reviews").child(restaurantId).get()
            .addOnSuccessListener { restaurantReviews ->
                if (!restaurantReviews.exists()) {
                    // 리뷰가 없는 경우
                    binding.reviewMore.visibility = View.GONE
                    binding.noReviewText.visibility = View.VISIBLE
                    binding.noReviewText.text = "아직 작성된 리뷰가 없습니다."
                    binding.ratingBar.rating = 0f
                    return@addOnSuccessListener
                }

                // 리뷰 데이터 처리
                restaurantReviews.children.forEach { reviewSnapshot ->
                    val review = reviewSnapshot.value as Map<*, *>
                    val userId = review["userId"] as String

                    // users/userId 경로에서 사용자 정보 가져오기
                    db.child("users").child(userId).get()
                        .addOnSuccessListener { userSnapshot ->
                            val nickname = userSnapshot.child("nickname").value as String

                            reviewsList.add(
                                ReviewItem(
                                    id = reviewSnapshot.key ?: "",
                                    userId = userId,
                                    nickname = nickname,
                                    rating = (review["rating"] as Number).toFloat(),
                                    content = review["content"] as String,
                                    createdAt = review["createdAt"] as String
                                )
                            )

                            if (reviewsList.size == restaurantReviews.childrenCount.toInt()) {
                                reviewsList.sortByDescending { it.createdAt }
                                reviewAdapter.setReviews(reviewsList)
                            }
                        }
                }
            }
            .addOnFailureListener { exception ->
                Log.e("Restaurant_Detail", "Error loading reviews", exception)
                Toast.makeText(this, "리뷰를 불러오는데 실패했습니다.", Toast.LENGTH_SHORT).show()
            }

        // restaurants/restaurantId/rating 경로에서 평균 별점 가져오기
        db.child("restaurants").child(restaurantId).child("rating").get()
            .addOnSuccessListener { ratingSnapshot ->
                val averageRating = ratingSnapshot.getValue(Float::class.java) ?: 0f
                binding.ratingBar.rating = averageRating // RatingBar 업데이트
            }
            .addOnFailureListener { exception ->
                Log.e("Restaurant_Detail", "Error loading rating", exception)
            }
    }

    private fun checkScrapStatus(userId: String, restaurantId: String) {
        val scrapRef = database.reference.child("user_scraps")
            .child(userId)
            .child(restaurantId)

        scrapRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                isScraped = snapshot.getValue(Boolean::class.java) ?: false
                // UI 업데이트
                updateScrapButtonUI()
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("Restaurant_Detail", "스크랩 상태 확인 실패", error.toException())
            }
        })
    }

    private fun toggleScrap(userId: String, restaurantId: String) {
        val scrapRef = database.reference.child("user_scraps")
            .child(userId)
            .child(restaurantId)

        // 현재 상태의 반대로 설정
        isScraped = !isScraped

        scrapRef.setValue(isScraped)
            .addOnSuccessListener {
                // UI 업데이트
                updateScrapButtonUI()
            }
            .addOnFailureListener { e ->
                Log.e("Restaurant_Detail", "스크랩 상태 변경 실패", e)
                // 실패 시 상태 되돌리기
                isScraped = !isScraped
                updateScrapButtonUI()
                Toast.makeText(this, "스크랩 상태 변경에 실패했습니다.", Toast.LENGTH_SHORT).show()
            }
    }

    private fun updateScrapButtonUI() {
        binding.scrapButton.setImageResource(
            if (isScraped) R.drawable.scrap_on
            else R.drawable.scrap_off
        )
    }

}