package com.example.todaylunch

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.todaylunch.databinding.ActivitySchoolBinding

class SchoolActivity : AppCompatActivity() {
    val binding: ActivitySchoolBinding by lazy { ActivitySchoolBinding.inflate(layoutInflater) }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(binding.root)

        setSupportActionBar(binding.toolbar) //액션 바 연결(툴바)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
    }
}