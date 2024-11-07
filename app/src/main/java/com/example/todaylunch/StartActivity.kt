package com.example.todaylunch

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.View
import android.view.animation.AnimationUtils
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.GestureDetectorCompat
import com.example.todaylunch.databinding.ActivityStartBinding


class StartActivity : AppCompatActivity() {
    private lateinit var gestureDetector: GestureDetector

    lateinit var binding : ActivityStartBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityStartBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.random.setOnClickListener {
            val intent = Intent(this, RandomActivity::class.java)
            startActivity(intent)
        }
        binding.school.setOnClickListener {
            val intent = Intent(this, SchoolActivity::class.java)
            startActivity(intent)
        }


        val HOME = Intent(this, StartActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        binding.searchopen.setOnClickListener {
            Search_Tap().show(supportFragmentManager, "TopSheetDialog")
        }

        binding.underbarMapinclude.homeButton.setOnClickListener {
            startActivity(HOME)
        }

        binding.underbarMapinclude.mapButton.setOnClickListener {
            startActivity(HOME )
        }

        binding.underbarMapinclude.myPageButton.setOnClickListener {
            startActivity(HOME)
        }


    }



}