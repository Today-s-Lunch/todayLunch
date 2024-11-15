package com.example.todaylunch

import android.app.Dialog
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Looper
import android.os.Handler
import android.view.GestureDetector
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.animation.AnimationUtils
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.GestureDetectorCompat
import com.example.todaylunch.databinding.ActivityStartBinding
import com.example.todaylunch.databinding.LoadingScreenBinding


class StartActivity : AppCompatActivity() {
    private lateinit var gestureDetector: GestureDetector
    private lateinit var loadingDialog: LoadingDialog
    private lateinit var searchFragment: Search_Tap
    private var isSearchVisible = false

    lateinit var binding: ActivityStartBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityStartBinding.inflate(layoutInflater)
        setContentView(binding.root)
        searchFragment = Search_Tap()
        supportFragmentManager.beginTransaction()
            .add(R.id.fragmentContainer, searchFragment!!)
            .hide(searchFragment!!) // 처음에는 숨기기
            .commit()

        // 검색 버튼 클릭 시 검색 탭 열기/닫기
        binding.searchopen.setOnClickListener {
            toggleSearchFragment()
        }
        loadingDialog = LoadingDialog(this)
        binding.random.setOnClickListener {
            // 로딩 다이얼로그 표시
            loadingDialog.show()

            // 3초 후에 RandomActivity로 이동
            Handler(Looper.getMainLooper()).postDelayed({
                loadingDialog.dismiss() // 로딩 다이얼로그 닫기
                val intent = Intent(this, RandomActivity::class.java)
                startActivity(intent)
            }, 3000) // 3초 지연
        }
        binding.school.setOnClickListener {
            val intent = Intent(this, SchoolActivity::class.java)
            startActivity(intent)
        }

        // 언더바 설정
        val HOME = Intent(this, StartActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
        }


        binding.underbarMapinclude.homeButton.setOnClickListener {
            startActivity(HOME)
        }

        binding.underbarMapinclude.mapButton.setOnClickListener {
            startActivity(HOME)
        }

        binding.underbarMapinclude.myPageButton.setOnClickListener {
            startActivity(HOME)
        }
    }

    private fun toggleSearchFragment() {
        val transaction = supportFragmentManager.beginTransaction()

        if (isSearchVisible) {
            // 검색 탭 숨기기
            transaction.hide(searchFragment).commitNow()
            isSearchVisible = false
        } else {
            // 검색 탭 표시
            if (!searchFragment.isAdded) { // 이미 추가되어 있는지 확인
                transaction.add(R.id.fragmentContainer, searchFragment).commitNow()
            } else {
                transaction.show(searchFragment).commitNow()
            }
            isSearchVisible = true
        }
    }

    class LoadingDialog(context: Context) {
        private val dialog: Dialog = Dialog(context)
        private val binding: LoadingScreenBinding =
            LoadingScreenBinding.inflate(LayoutInflater.from(context))

        init {
            dialog.setContentView(binding.root)
            dialog.setCancelable(false)
            dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
        }

        fun show() {
            dialog.show()
        }

        fun dismiss() {
            dialog.dismiss()
        }
    }
}
