package com.example.todaylunch

import android.content.Intent
import android.os.Bundle
import android.text.Spannable
import android.text.SpannableString
import android.text.style.ForegroundColorSpan
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.example.todaylunch.databinding.ActivityReviewModifyBinding
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

class ReviewModify : AppCompatActivity() {
    private lateinit var binding: ActivityReviewModifyBinding
    private lateinit var database: DatabaseReference
    private var restaurantId: String = ""
    private var reviewId: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityReviewModifyBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Intent에서 데이터 받기
        restaurantId = intent.getStringExtra("restaurantId") ?: ""
        reviewId = intent.getStringExtra("reviewId") ?: ""
        val restaurantName = intent.getStringExtra("restaurantName") ?: ""
        val rating = intent.getFloatExtra("rating", 0f)
        val content = intent.getStringExtra("content") ?: ""

        if (restaurantId.isEmpty() || reviewId.isEmpty()) {
            Toast.makeText(this, "리뷰 정보를 불러올 수 없습니다.", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        // SpannableString 사용: "수정" 부분만 색상 변경
        val text = "${restaurantName}에 \n작성한 리뷰 수정"
        val spannable = SpannableString(text)
        val startIndex = text.indexOf("수정")
        val endIndex = startIndex + "수정".length

        if (startIndex != -1) {
            spannable.setSpan(
                ForegroundColorSpan(ContextCompat.getColor(this, R.color.red3)),
                startIndex,
                endIndex,
                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
            )
        }

        database = FirebaseDatabase.getInstance().reference

        // UI 업데이트
        binding.apply {
            modifyname.text = spannable
            ratingBar.rating = rating
            reviewEditText.setText(content)
        }

        // 수정 완료 버튼 클릭 리스너
        binding.modifydone.setOnClickListener {
            val newRating = binding.ratingBar.rating
            val newContent = binding.reviewEditText.text.toString().trim()
            updateReview(newRating, newContent)
            lifecycleScope.launch {
                updateRestaurantRating(restaurantId.toString())
            }

        }

        // 언더바 버튼 동작 설정
        val myPageIntent = Intent(this, MypageActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val homeIntent = Intent(this, StartActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
        }

        binding.underbar.backButton.setOnClickListener { onBackPressedDispatcher.onBackPressed() }
        binding.underbar.homeButton.setOnClickListener {
            startActivity(homeIntent)
        }
        binding.underbar.myPageButton.setOnClickListener {
            startActivity(myPageIntent)
        }
    }

    private fun updateReview(newRating: Float, newContent: String) {
        val updatedData = mapOf(
            "rating" to newRating,
            "content" to newContent
        )

        Log.d("UpdateReview", "Attempting to update data: $updatedData")
        Log.d("UpdateReview", "Database Path: reviews/$restaurantId/$reviewId")

        database.child("reviews").child(restaurantId).child(reviewId)
            .updateChildren(updatedData)
            .addOnSuccessListener {
                Log.d("UpdateReview", "Review updated successfully.")
                Toast.makeText(this, "리뷰가 수정되었습니다.", Toast.LENGTH_SHORT).show()
                val intent = Intent(this, MypageActivity::class.java).apply {
                    putExtra("fragmentId", "review") // 작성한 리뷰 Fragment로 이동
                }
                startActivity(intent)
            }
            .addOnFailureListener { e ->
                Log.e("UpdateReview", "리뷰 수정 실패: ${e.message}")
                Toast.makeText(this, "리뷰 수정에 실패했습니다: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }
    private suspend fun updateRestaurantRating(restaurantId: String) {
        val reviewsRef = FirebaseDatabase.getInstance().reference.child("reviews").child(restaurantId)
        val restaurantRef = FirebaseDatabase.getInstance().reference.child("restaurants").child(restaurantId)

        try {
            // Firebase 데이터를 비동기로 가져오기
            val snapshot = withContext(Dispatchers.IO) { reviewsRef.get().await() }

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
            withContext(Dispatchers.IO) {
                restaurantRef.child("rating").setValue(averageRating).await()
            }

            Log.d("UpdateRating", "Rating updated for restaurant $restaurantId: $averageRating")
        } catch (e: Exception) {
            Log.e("UpdateRating", "Failed to update rating: ${e.message}")
        }
    }
}