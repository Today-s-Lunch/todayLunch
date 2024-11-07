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


        gestureDetector = GestureDetector(this, object : GestureDetector.SimpleOnGestureListener() {
            override fun onScroll(
                e1: MotionEvent?,
                e2: MotionEvent,
                distanceX: Float,
                distanceY: Float
            ): Boolean {
                if (distanceY > 0) {
                    hideFragment()  // 위로 스크롤하면 Fragment를 숨김
                    return true
                }
                return false
            }
        })

        binding.searchopen.setOnClickListener {
            showFragment()
        }

        // Close fragment button click
        binding.closeFragmentButton.setOnClickListener {
            hideFragment()
        }

        // Listen for touch events to detect scroll
        binding.nestedScrollView.setOnTouchListener { v, event ->
            gestureDetector.onTouchEvent(event)
        }
    }
    private fun showFragment() {
        binding.topFragmentContainer.visibility = View.VISIBLE
        val slideDown = AnimationUtils.loadAnimation(this, R.anim.slide_down)
        binding.topFragmentContainer.startAnimation(slideDown)
        Log.d("sat","blue")
    }

    // Hide fragment with slide up animation
    private fun hideFragment() {
        val slideUp = AnimationUtils.loadAnimation(this, R.anim.slide_up)
        binding.topFragmentContainer.startAnimation(slideUp)
        binding.topFragmentContainer.postDelayed({
            binding.topFragmentContainer.visibility = View.GONE
        }, 300)  // Wait for animation to complete before hiding
    }


}