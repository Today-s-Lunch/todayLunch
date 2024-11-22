package com.example.todaylunch

import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.todaylunch.databinding.ActivityMypageBinding

class MypageActivity : AppCompatActivity() {
    val binding: ActivityMypageBinding by lazy { ActivityMypageBinding.inflate(layoutInflater) }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_mypage)
    }
}