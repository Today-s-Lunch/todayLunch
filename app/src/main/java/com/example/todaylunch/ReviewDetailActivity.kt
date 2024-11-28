package com.example.todaylunch

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.todaylunch.databinding.ActivityReviewDetailBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

class ReviewDetailActivity : AppCompatActivity() {
    private lateinit var binding: ActivityReviewDetailBinding
    private var restaurantId: String? = null // 클래스 필드로 선언

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityReviewDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Intent에서 데이터 받기
        val reviewId = intent.getStringExtra("reviewId") ?: ""
        val restaurantName = intent.getStringExtra("restaurantName") ?: ""
        val rating = intent.getFloatExtra("rating", 0f)
        val content = intent.getStringExtra("content") ?: ""

        if (reviewId.isEmpty()) {
            Toast.makeText(this, "리뷰 정보를 불러올 수 없습니다.", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        // restaurantId 가져오기
        fetchRestaurantIdFromReviewId(reviewId) { fetchedRestaurantId ->
            if (fetchedRestaurantId == null) {
                Toast.makeText(this, "식당 정보를 불러올 수 없습니다.", Toast.LENGTH_SHORT).show()
                finish()
                return@fetchRestaurantIdFromReviewId
            }

            restaurantId = fetchedRestaurantId // 클래스 필드에 저장

            // UI 업데이트
            binding.apply {
                // RatingBar를 읽기 전용으로 설정
                ratingBar.setIsIndicator(true)

                // EditText를 읽기 전용으로 설정
                reviewEditText.apply {
                    isFocusable = false // 포커스를 받을 수 없게 설정
                    isClickable = false // 클릭할 수 없게 설정
                }

                restaurantNameText.text = "${restaurantName}에 \n작성한 리뷰"
                ratingBar.rating = rating
                reviewEditText.setText(content)
            }

            // 수정 버튼 클릭 리스너
            binding.modifyBtn.setOnClickListener {
                val intent = Intent(this@ReviewDetailActivity, ReviewModify::class.java).apply {
                    putExtra("reviewId", reviewId) // reviewId 전달
                    putExtra("restaurantName", restaurantName) // restaurantName 전달
                    putExtra("rating", rating) // rating 전달
                    putExtra("content", content) // content 전달
                    putExtra("restaurantId", restaurantId) // restaurantId 전달
                }
                startActivity(intent)
            }

            // 삭제 버튼 클릭 리스너
            binding.deleteBtn.setOnClickListener {
                restaurantId?.let {
                    deleteReview(it, reviewId)
                }
            }
        }

        // 언더바 설정
        setupUnderbar()
    }

    private fun deleteReview(restaurantId: String, reviewId: String) {
        val databaseRef = FirebaseDatabase.getInstance().reference
        val userId = FirebaseAuth.getInstance().currentUser?.uid

        if (userId.isNullOrEmpty()) {
            Toast.makeText(this, "사용자 정보를 가져올 수 없습니다.", Toast.LENGTH_SHORT).show()
            return
        }

        // `reviews` 노드에서 삭제
        val reviewsRef = databaseRef.child("reviews").child(restaurantId).child(reviewId)

        // `user_reviews` 노드에서 삭제
        val userReviewsRef = databaseRef.child("user_reviews").child(userId).child(restaurantId).child(reviewId)

        reviewsRef.removeValue()
            .addOnSuccessListener {
                Log.d("DeleteReview", "reviews에서 삭제 성공")
                // `user_reviews`에서도 삭제
                userReviewsRef.removeValue()
                    .addOnSuccessListener {
                        Log.d("DeleteReview", "user_reviews에서 삭제 성공")
                        Toast.makeText(this, "리뷰가 성공적으로 삭제되었습니다.", Toast.LENGTH_SHORT).show()
                        finish()
                    }
                    .addOnFailureListener { e ->
                        Log.e("DeleteReview", "user_reviews 삭제 실패: ${e.message}")
                        Toast.makeText(this, "user_reviews 삭제에 실패했습니다: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
            }
            .addOnFailureListener { e ->
                Log.e("DeleteReview", "reviews 삭제 실패: ${e.message}")
                Toast.makeText(this, "reviews 삭제에 실패했습니다: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun fetchRestaurantIdFromReviewId(reviewId: String, onResult: (String?) -> Unit) {
        val reviewsRef = FirebaseDatabase.getInstance().reference.child("reviews")

        reviewsRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                for (restaurantSnapshot in snapshot.children) {
                    val restaurantId = restaurantSnapshot.key ?: continue
                    for (reviewSnapshot in restaurantSnapshot.children) {
                        if (reviewSnapshot.key == reviewId) {
                            onResult(restaurantId)
                            return
                        }
                    }
                }
                onResult(null) // restaurantId를 찾을 수 없는 경우
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("FirebaseError", "Failed to fetch restaurantId: ${error.message}")
                onResult(null) // 오류 발생 시
            }
        })
    }

    private fun setupUnderbar() {
        val MYPAGE = Intent(this, MypageActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val HOME = Intent(this, StartActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        binding.underbar.backButton.setOnClickListener { onBackPressedDispatcher.onBackPressed() }
        binding.underbar.homeButton.setOnClickListener {
            startActivity(HOME)
        }
        binding.underbar.myPageButton.setOnClickListener {
            startActivity(MYPAGE)
        }
    }
}
