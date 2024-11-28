package com.example.todaylunch

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.text.SpannableString
import android.text.Spanned
import android.text.style.ForegroundColorSpan
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import com.example.todaylunch.databinding.ActivityMainBinding
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import com.google.firebase.database.FirebaseDatabase


class MainActivity : AppCompatActivity() {
    val binding: ActivityMainBinding by lazy { ActivityMainBinding.inflate(layoutInflater) }
    private var mFirebaseAnalytics: FirebaseAnalytics? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root) //화면에 레이아웃이 표시
        initializeRestaurantRatings()
        // 점심 색상만 변경
        val text = "오늘의 점심은"
        val spannableString = SpannableString(text).apply {
            setSpan(ForegroundColorSpan(Color.parseColor("#FF6A50")), 4, 6, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        }
        binding.Today.text = spannableString

        //3초 후에 home 화면으로 이동
        GlobalScope.launch {
            for (seconds in 3 downTo 1) {
                runOnUiThread {
                    binding.count.text = "${seconds}초 후에 앱이 시작됩니다"
                }
                delay(1000)  // 1초 간격으로 텍스트 업데이트
            }
            startActivity(Intent(this@MainActivity, LoginActivity::class.java))
            finish()
        }

        mFirebaseAnalytics = FirebaseAnalytics.getInstance(this)
    }
    private fun initializeRestaurantRatings() {// 별점데이터 초기화 //이미 작성된 리뷰들 테스트 위해서임. 실제 사용할때는 필요없음
        val database = FirebaseDatabase.getInstance().reference
        val restaurantsRef = database.child("restaurants")
        val reviewsRef = database.child("reviews")

        // restaurants 노드를 가져옴
        restaurantsRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                // 각 레스토랑에 대해 처리
                for (restaurantSnapshot in snapshot.children) {
                    val restaurantId = restaurantSnapshot.key ?: continue

                    // 해당 레스토랑의 리뷰 가져오기
                    reviewsRef.child(restaurantId).addListenerForSingleValueEvent(object : ValueEventListener {
                        override fun onDataChange(reviewSnapshot: DataSnapshot) {
                            var totalRating = 0.0
                            var reviewCount = 0

                            // 리뷰 데이터 합산
                            for (review in reviewSnapshot.children) {
                                val rating = review.child("rating").getValue(Float::class.java) ?: 0f
                                totalRating += rating
                                reviewCount++
                            }

                            // 평균 별점 계산
                            val averageRating = if (reviewCount > 0) totalRating / reviewCount else 0.0

                            // restaurants 노드에 rating 필드 업데이트
                            restaurantsRef.child(restaurantId).child("rating").setValue(averageRating)
                                .addOnSuccessListener {
                                    Log.d("InitializeRating", "Updated rating for restaurant $restaurantId: $averageRating")
                                }
                                .addOnFailureListener { e ->
                                    Log.e("InitializeRating", "Failed to update rating for restaurant $restaurantId", e)
                                }
                        }

                        override fun onCancelled(error: DatabaseError) {
                            Log.e("InitializeRating", "Failed to fetch reviews for restaurant $restaurantId: ${error.message}")
                        }
                    })
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("InitializeRating", "Failed to fetch restaurants: ${error.message}")
            }
        })
    }
}