package com.example.todaylunch

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.text.SpannableString
import android.text.Spanned
import android.text.style.ForegroundColorSpan
import androidx.appcompat.app.AppCompatActivity
import com.example.todaylunch.databinding.ActivitySignupBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DatabaseReference

class SignupActivity : AppCompatActivity() {
    val binding: ActivitySignupBinding by lazy { ActivitySignupBinding.inflate(layoutInflater) }
    private lateinit var database: DatabaseReference
    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)

        // 점심 색상만 변경
        val text = "오늘의 점심은\n회원가입"
        val spannableString = SpannableString(text).apply {
            setSpan(ForegroundColorSpan(Color.parseColor("#FF6A50")), 4, 6, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        }
        binding.Today.text = spannableString

        binding.goToLogin.setOnClickListener {
            // LoginActivity 이동
            val intent = Intent(this, LoginActivity::class.java)
            startActivity(intent)
        }
    }
}