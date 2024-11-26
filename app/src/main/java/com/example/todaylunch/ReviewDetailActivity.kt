package com.example.todaylunch

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.todaylunch.databinding.ActivityReviewDetailBinding
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

class ReviewDetailActivity : AppCompatActivity() {
    private lateinit var binding: ActivityReviewDetailBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityReviewDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Intent에서 데이터 받기
        val reviewId = intent.getStringExtra("reviewId") ?: ""
        val restaurantName = intent.getStringExtra("restaurantName") ?: ""
        val rating = intent.getFloatExtra("rating", 0f)
        val content = intent.getStringExtra("content") ?: ""

        // UI 업데이트
        binding.apply {
            restaurantNameText.text = "${restaurantName}에 \n작성한 리뷰"
            ratingBar.rating = rating
            reviewEditText.setText(content)
        }

        // 수정 버튼 클릭 리스너
        binding.modifyBtn.setOnClickListener {
            // 수정 로직 구현 필요
        }

        // 삭제 버튼 클릭 리스너
        binding.deleteBtn.setOnClickListener {
            deleteReview(reviewId)
        }
        
        // 언더바
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

    private fun deleteReview(reviewId: String) {
        FirebaseDatabase.getInstance().reference
            .child("reviews")
            .child(reviewId)
            .removeValue()
            .addOnSuccessListener {
                Toast.makeText(this, "리뷰가 삭제되었습니다.", Toast.LENGTH_SHORT).show()
                finish()
            }
            .addOnFailureListener {
                Toast.makeText(this, "리뷰 삭제에 실패했습니다.", Toast.LENGTH_SHORT).show()
            }
    }
}