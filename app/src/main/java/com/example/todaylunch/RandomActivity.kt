package com.example.todaylunch

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.text.SpannableString
import android.text.Spanned
import android.text.style.ForegroundColorSpan
import androidx.appcompat.app.AppCompatActivity
import com.example.todaylunch.databinding.ActivityRandomBinding
import kotlin.random.Random

class RandomActivity : AppCompatActivity() {
    val binding: ActivityRandomBinding by lazy { ActivityRandomBinding.inflate(layoutInflater) }

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

        // 임시 식당 리스트 (식당 번호와 이름 쌍으로 구성)
        val restaurantList = listOf(
            Pair(1, "달볶이"),
            Pair(2, "샤브21"),
            Pair(3, "육쌈냉면"),
            Pair(4, "빨봉"),
            Pair(5, "돈카춘"),
            Pair(6, "행복은 간장밥")
        )

        // 1부터 6까지 랜덤 번호 생성 (Firebase 연결 전 임시 사용)
        val randomId = Random.nextInt(1, 7)

        // 해당 번호의 식당을 리스트에서 찾아서 이름을 가져오기
        val randomRestaurant = restaurantList.find { it.first == randomId }?.second ?: "식당 정보 없음"
        binding.Food.text = randomRestaurant

        // Firebase 연결 시(예시)
        // Firebase에 ID 쿼리로 보내서 식당 이름 받아오기
        // val db = Firebase.database.reference
        // db.child("restaurants").child(randomId.toString()).get().addOnSuccessListener { snapshot ->
        //     val restaurantName = snapshot.child("name").value.toString()
        //     binding.Food.text = restaurantName
        // }.addOnFailureListener {
        //     binding.Food.text = "식당 정보를 불러오지 못했습니다."
        // }
    }
}