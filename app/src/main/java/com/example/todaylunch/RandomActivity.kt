package com.example.todaylunch

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.text.SpannableString
import android.text.Spanned
import android.text.style.ForegroundColorSpan
import androidx.appcompat.app.AppCompatActivity
import com.example.todaylunch.databinding.ActivityRandomBinding
import com.google.firebase.Firebase
import com.google.firebase.database.database
import kotlin.random.Random

class RandomActivity : AppCompatActivity() {
    val binding: ActivityRandomBinding by lazy { ActivityRandomBinding.inflate(layoutInflater) }
    private var currentRestaurantId: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)

        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        // 점심 색상만 변경
        val text = "오늘의 점심은"
        val spannableString = SpannableString(text).apply {
            setSpan(ForegroundColorSpan(Color.parseColor("#FF6A50")), 4, 6, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        }
        binding.Today.text = spannableString

        // 바로 랜덤 식당 이름 표시
        showRandomRestaurant()

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

    private fun showRandomRestaurant() {
        // Firebase 데이터베이스 레퍼런스 생성
        val db = Firebase.database.reference

        // 1부터 97까지 랜덤 번호 생성
        val randomId = Random.nextInt(1, 97).toString()  // 문자열로 변환
        currentRestaurantId = randomId  // 현재 ID 저장

        // Firebase에서 랜덤 번호에 해당하는 식당 이름 가져오기
        db.child(randomId.toString()).get().addOnSuccessListener { snapshot ->
            var restaurantName = snapshot.child("Name").value?.toString() ?: "식당 정보 없음"
            restaurantName = restaurantName.replace(" ", "\n") // 공백을 줄바꿈으로 변경
            binding.Food.text = restaurantName

            // 데이터 로드 완료 후 클릭 리스너 설정
            binding.see.setOnClickListener {
                val intent = Intent(this, Restaurant_Detail::class.java).apply {
                    putExtra("restaurantId", randomId)  // ID를 인텐트에 추가
                }
                startActivity(intent)
            }
        }.addOnFailureListener {
            binding.Food.text = "식당 정보를 불러오지 못했습니다."
            currentRestaurantId = null
        }
    }
}