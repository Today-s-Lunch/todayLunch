package com.example.todaylunch

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.todaylunch.databinding.ActivityReviewBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ReviewActivity : AppCompatActivity() {
    val binding: ActivityReviewBinding by lazy { ActivityReviewBinding.inflate(layoutInflater) }
    private val database = FirebaseDatabase.getInstance().reference
    private val TAG = "ReviewActivity"  // Log 태그 추가
    private val auth = FirebaseAuth.getInstance()
    private var restaurantId: String? = null
    private var restaurantName: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)

        restaurantId = intent.getStringExtra("restaurantId")
        binding.ratingBar.rating = 0f

        setToSubmit()

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
            startActivity(goToStartActivity)
        }
    }

    private fun setToSubmit() {
        binding.reviewsubmitBtn.setOnClickListener {
            val user = auth.currentUser
            if(restaurantId == null || user == null) {
                Toast.makeText(this, "식당 정보를 불러올 수 없습니다.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            val review = binding.reviewEditText.text.toString().trim()

            val rating = binding.ratingBar.rating
            if (rating == 0f) {
                Toast.makeText(this, "별점을 선택해주세요.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            submitReview(user.uid, rating, review)
            updateRestaurantRating(restaurantId.toString())
        }
    }

    private fun submitReview(userId: String, rating: Float, review: String) {
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val currentDate = dateFormat.format(Date())

        val reviewKey = database.child("reviews").child(restaurantId!!).push().key
        if (reviewKey == null) {
            Log.e(TAG, "리뷰 키 생성 실패")
            return
        }

        // 먼저 리뷰 데이터를 저장
        val reviewRef = database.child("reviews").child(restaurantId!!).child(reviewKey)
        val userReviewRef = database.child("user_reviews").child(userId).child(restaurantId!!).child(reviewKey)

        val reviewData = if (review.isEmpty()) {
            mapOf(
                "createdAt" to currentDate,
                "rating" to rating,
                "userId" to userId
            )
        } else {
            mapOf(
                "content" to review,
                "createdAt" to currentDate,
                "rating" to rating,
                "userId" to userId
            )
        }

        Log.d(TAG, "리뷰 데이터 저장 시도 - ReviewKey: $reviewKey")
        Log.d(TAG, "리뷰 데이터: $reviewData")

        // 리뷰 데이터 저장
        reviewRef.setValue(reviewData)
            .addOnSuccessListener {
                // 리뷰 데이터 저장 성공 후 user_reviews에 참조 추가
                userReviewRef.setValue(true)
                    .addOnSuccessListener {
                        Log.i(TAG, "리뷰 등록 성공 - Restaurant: $restaurantName, ReviewKey: $reviewKey")
                        Toast.makeText(this, "리뷰가 성공적으로 등록되었습니다.", Toast.LENGTH_SHORT).show()
                        finish()
                    }
                    .addOnFailureListener { e ->
                        Log.e(TAG, "user_reviews 참조 추가 실패", e)
                        Toast.makeText(this, "리뷰 등록에 실패했습니다: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "리뷰 데이터 저장 실패 - Restaurant: $restaurantName", e)
                Toast.makeText(this, "리뷰 등록에 실패했습니다: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }
    private fun updateRestaurantRating(restaurantId: String) { //별점 수정
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
}