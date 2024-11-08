package com.example.todaylunch

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.text.SpannableString
import android.text.Spanned
import android.text.style.ForegroundColorSpan
import androidx.appcompat.app.AppCompatActivity
import com.example.todaylunch.databinding.ActivityMainBinding
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {
    val binding: ActivityMainBinding by lazy { ActivityMainBinding.inflate(layoutInflater) }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root) //화면에 레이아웃이 표시

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
            startActivity(Intent(this@MainActivity, StartActivity::class.java))
            finish()
        }
    }
}