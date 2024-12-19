package com.example.todaylunch

import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.widget.FrameLayout
import android.widget.Switch
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentTransaction
import com.example.todaylunch.databinding.ActivitySchoolBinding
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class SchoolActivity : AppCompatActivity() {
    val binding: ActivitySchoolBinding by lazy { ActivitySchoolBinding.inflate(layoutInflater) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)
        val currentDate = Calendar.getInstance().time


        val dateFormat = SimpleDateFormat("yyyy년 MM월 dd일 EEEE", Locale("ko", "KR"))

        val formattedDate = dateFormat.format(currentDate)
        binding.date.text = "< "+formattedDate+ " >"
        binding.toggletext2.setTextColor(Color.parseColor("#BBBBBB"))
        replaceFragment(sunhunlunch())

        binding.switchToggle.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                binding.toggletext2.setTextColor(Color.parseColor("#000000"))
                binding.toggletext1.setTextColor(Color.parseColor("#BBBBBB"))
                replaceFragment(myungsinlunch())
            } else {
                binding.toggletext1.setTextColor(Color.parseColor("#000000"))
                binding.toggletext2.setTextColor(Color.parseColor("#BBBBBB"))
                replaceFragment(sunhunlunch())
            }
        }

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
            val MYPAGE = Intent(this, MypageActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            }
            startActivity(MYPAGE)
        }
    }


    // 프래그먼트 교체 함수
    private fun replaceFragment(fragment: Fragment) {
        val transaction: FragmentTransaction = supportFragmentManager.beginTransaction()
        transaction.replace(R.id.fragmentContainer, fragment)
        transaction.commit()
    }


}
