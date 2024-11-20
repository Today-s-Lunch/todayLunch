package com.example.todaylunch

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import com.example.todaylunch.databinding.ActivityRestaurantDetailBinding
import com.google.firebase.Firebase
import com.google.firebase.database.database

class Restaurant_Detail : AppCompatActivity() {
    val binding: ActivityRestaurantDetailBinding by lazy { ActivityRestaurantDetailBinding.inflate(layoutInflater) }
    private var isScraped = false  // 스크랩 상태를 추적하는 변수

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

        val restaurantId = intent.getStringExtra("restaurantId") ?: "1"
        loadRestaurantData(restaurantId)

        binding.reviewButton.setOnClickListener {
            // TODO: 리뷰 작성 화면으로 이동
        }

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

        // 스크랩 버튼 클릭 리스너
        binding.scrapButton.setOnClickListener {
            isScraped = !isScraped  // 상태 토글

            // 상태에 따라 이미지 변경
            if (isScraped) {
                binding.scrapButton.setImageResource(R.drawable.scrap_on)
            } else {
                binding.scrapButton.setImageResource(R.drawable.scrap_off)
            }
        }

        //리뷰 작성하기로 화면이동
        binding.reviewButton.setOnClickListener {
            // ReviewActivity로 이동
            val intent = Intent(this, ReviewActivity::class.java)
            startActivity(intent)
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
}