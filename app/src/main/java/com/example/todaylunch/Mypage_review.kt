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
import com.example.todaylunch.databinding.FragmentMypageReviewBinding
import com.example.todaylunch.databinding.ItemMypageReviewBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

data class Review(
    val reviewId: String = "",        // 리뷰 고유 ID
    val restaurantId: String = "",    // 식당 ID
    val restaurantName: String = "",  // 식당 이름
    val content: String = "",         // 리뷰 내용
    val createdAt: String = "",       // 작성일
    val rating: Float = 0f,           // 별점
    val userId: String = ""           // 작성자 ID
)

class Mypage_review : Fragment() {
    private var _binding: FragmentMypageReviewBinding? = null
    private val binding get() = _binding!!
    private val reviewAdapter = ReviewAdapter()
    private val database = FirebaseDatabase.getInstance()
    private val auth = FirebaseAuth.getInstance()

    companion object {
        private const val TAG = "Mypage_review"
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentMypageReviewBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupRecyclerView()
        loadMyReviews()
    }

    private fun setupRecyclerView() {
        binding.recyclerView.apply {
            adapter = reviewAdapter
//            layoutManager = LinearLayoutManager(context)
//            addItemDecoration(DividerItemDecoration(context, LinearLayoutManager.VERTICAL))
        }
    }

    private fun loadMyReviews() {
        val currentUser = auth.currentUser?.uid ?: run {
            Log.e(TAG, "로그인된 사용자가 없습니다")
            updateEmptyView(true)
            return
        }

        // user_reviews에서 현재 사용자의 리뷰 목록 가져오기
        database.reference.child("user_reviews").child(currentUser)
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val reviews = mutableListOf<Review>()
                    var completedRequests = 0
                    var totalRequests = 0

                    // 리뷰가 없는 경우 처리
                    if (!snapshot.exists()) {
                        updateEmptyView(true)
                        reviewAdapter.submitList(emptyList())
                        return
                    }

                    // 각 레스토랑의 리뷰들을 가져옴
                    snapshot.children.forEach { restaurantSnapshot ->
                        val restaurantId = restaurantSnapshot.key ?: return@forEach

                        restaurantSnapshot.children.forEach { reviewSnapshot ->
                            totalRequests++
                            val reviewId = reviewSnapshot.key ?: return@forEach

                            // reviews/{restaurantId}/{reviewId} 경로에서 리뷰 정보 가져오기
                            database.reference.child("reviews")
                                .child(restaurantId)
                                .child(reviewId)
                                .addListenerForSingleValueEvent(object : ValueEventListener {
                                    override fun onDataChange(reviewDataSnapshot: DataSnapshot) {
                                        // 레스토랑 정보 가져오기
                                        database.reference.child("restaurants")
                                            .child(restaurantId)
                                            .addListenerForSingleValueEvent(object : ValueEventListener {
                                                override fun onDataChange(restaurantSnapshot: DataSnapshot) {
                                                    val review = Review(
                                                        reviewId = reviewId,
                                                        restaurantId = restaurantId,
                                                        restaurantName = restaurantSnapshot.child("Name").getValue(String::class.java) ?: "",
                                                        content = reviewDataSnapshot.child("content").getValue(String::class.java) ?: "",
                                                        createdAt = reviewDataSnapshot.child("createdAt").getValue(String::class.java) ?: "",
                                                        rating = reviewDataSnapshot.child("rating").getValue(Double::class.java)?.toFloat() ?: 0f,
                                                        userId = reviewDataSnapshot.child("userId").getValue(String::class.java) ?: ""
                                                    )
                                                    reviews.add(review)

                                                    completedRequests++
                                                    if (completedRequests == totalRequests) {
                                                        val sortedList = reviews.sortedByDescending { it.createdAt }
                                                        updateEmptyView(sortedList.isEmpty())
                                                        reviewAdapter.submitList(sortedList)
                                                    }
                                                }

                                                override fun onCancelled(error: DatabaseError) {
                                                    Log.e(TAG, "레스토랑 정보 로딩 실패: ${error.message}")
                                                    completedRequests++
                                                    checkAndUpdateList(completedRequests, totalRequests, reviews)
                                                }
                                            })
                                    }

                                    override fun onCancelled(error: DatabaseError) {
                                        Log.e(TAG, "리뷰 정보 로딩 실패: ${error.message}")
                                        completedRequests++
                                        checkAndUpdateList(completedRequests, totalRequests, reviews)
                                    }
                                })
                        }
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    Log.e(TAG, "리뷰 목록 로딩 실패: ${error.message}")
                    updateEmptyView(true)
                }
            })
    }

    private fun checkAndUpdateList(completed: Int, total: Int, reviews: List<Review>) {
        if (completed == total) {
            val sortedList = reviews.sortedByDescending { it.createdAt }
            updateEmptyView(sortedList.isEmpty())
            reviewAdapter.submitList(sortedList)
        }
    }

    private fun updateEmptyView(isEmpty: Boolean) {
        if (isEmpty) {
            binding.recyclerView.visibility = View.GONE
            binding.emptyView.visibility = View.VISIBLE
        } else {
            binding.recyclerView.visibility = View.VISIBLE
            binding.emptyView.visibility = View.GONE
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

class ReviewAdapter : RecyclerView.Adapter<ReviewAdapter.ReviewViewHolder>() {
    private var reviews = listOf<Review>()

    inner class ReviewViewHolder(private val binding: ItemMypageReviewBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(review: Review) {
            binding.apply {
                restaurantName.text = review.restaurantName
                ratingBar.rating = review.rating
                reviewText.text = review.content

                // DetailActivity로 넘어갈 때 필요한 데이터들을 모두 전달
                root.setOnClickListener {
                    val intent = Intent(root.context, ReviewDetailActivity::class.java).apply {
                        putExtra("reviewId", review.reviewId)           // 리뷰 ID
                        putExtra("restaurantName", review.restaurantName) // 레스토랑 이름
                        putExtra("rating", review.rating)               // 별점
                        putExtra("content", review.content)             // 리뷰 내용
                        putExtra("restaurantId", review.restaurantId)   // 레스토랑 ID
                        putExtra("createdAt", review.createdAt)         // 작성 시간
                    }
                    root.context.startActivity(intent)
                }
                deleteButton.setOnClickListener {
                    val position = adapterPosition
                    if (position != RecyclerView.NO_POSITION) { // 유효한 position인지 확인
                        val review = reviews[position]
                        deleteReview(review.restaurantId, review.reviewId, position)
                    } else {
                        Log.e("DeleteReview", "Invalid adapter position")
                    }
                }
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ReviewViewHolder {
        val binding = ItemMypageReviewBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ReviewViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ReviewViewHolder, position: Int) {
        holder.bind(reviews[position])
    }

    override fun getItemCount() = reviews.size
    private fun deleteReview(restaurantId: String, reviewId: String, position: Int) {
        val databaseRef = FirebaseDatabase.getInstance().reference
        val userId = FirebaseAuth.getInstance().currentUser?.uid

        if (userId.isNullOrEmpty()) {
            Log.e("DeleteReview", "사용자 정보 없음")
            return
        }

        val reviewsRef = databaseRef.child("reviews").child(restaurantId).child(reviewId)
        val userReviewsRef = databaseRef.child("user_reviews").child(userId).child(restaurantId).child(reviewId)

        // Firebase에서 삭제
        reviewsRef.removeValue()
            .addOnSuccessListener {
                userReviewsRef.removeValue()
                    .addOnSuccessListener {
                        Log.d("DeleteReview", "리뷰 삭제 성공")
                        removeItem(position)
                        updateRestaurantRating(restaurantId)
                    }
                    .addOnFailureListener { e ->
                        Log.e("DeleteReview", "user_reviews 삭제 실패: ${e.message}")
                    }
            }
            .addOnFailureListener { e ->
                Log.e("DeleteReview", "reviews 삭제 실패: ${e.message}")
            }
    }
    fun removeItem(position: Int) {
        if (position in reviews.indices) {
            val updatedList = reviews.toMutableList()
            updatedList.removeAt(position)
            reviews = updatedList
            notifyItemRemoved(position)

            // 리스트가 비어있으면 UI 갱신

        }
    }
    private fun updateRestaurantRating(restaurantId: String) {
        val reviewsRef = FirebaseDatabase.getInstance().reference.child("reviews").child(restaurantId)
        val restaurantRef = FirebaseDatabase.getInstance().reference.child("restaurants").child(restaurantId)

        reviewsRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                var totalRating = 0.0
                var reviewCount = 0

                // 모든 리뷰의 rating 값 합산
                for (reviewSnapshot in snapshot.children) {
                    val rating = reviewSnapshot.child("rating").getValue(Float::class.java) ?: 0f
                    totalRating += rating
                    reviewCount++
                }

                // 평균 별점 계산
                val averageRating = if (reviewCount > 0) totalRating / reviewCount else 0.0

                // restaurants 노드에 평균 별점 업데이트
                restaurantRef.child("rating").setValue(averageRating)
                    .addOnSuccessListener {
                        Log.d("UpdateRating", "Rating updated for restaurant $restaurantId: $averageRating")
                    }
                    .addOnFailureListener { e ->
                        Log.e("UpdateRating", "Failed to update rating: ${e.message}")
                    }
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("FirebaseError", "Failed to fetch reviews: ${error.message}")
            }
        })
    }

    fun submitList(newList: List<Review>) {
        reviews = newList
        notifyDataSetChanged()
    }
}