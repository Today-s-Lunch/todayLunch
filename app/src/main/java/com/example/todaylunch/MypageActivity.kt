package com.example.todaylunch

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.example.todaylunch.databinding.ActivityMypageBinding
import com.google.android.material.tabs.TabLayoutMediator
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

class MypageActivity : AppCompatActivity() {
    private val binding: ActivityMypageBinding by lazy { ActivityMypageBinding.inflate(layoutInflater) }
    private lateinit var viewPagerAdapter: MypageViewPagerAdapter
    private val auth: FirebaseAuth by lazy { Firebase.auth }
    private val database = FirebaseDatabase.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)  // binding.root로 수정

        loadUserNickname()
        setupViewPager()
        setupTabLayout()

        val MYPAGE = Intent(this, MypageActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val HOME = Intent(this, StartActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        binding.underbar.backButton.setOnClickListener { onBackPressedDispatcher.onBackPressed() }
        binding.underbar.homeButton.setOnClickListener {
            startActivity(HOME)
        }
        binding.underbar.myPageButton.setOnClickListener {
            startActivity(MYPAGE)
        }
    }

    private fun loadUserNickname() {
        val currentUser = auth.currentUser?.uid ?: return

        database.reference.child("users").child(currentUser)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val nickname = snapshot.child("nickname").getValue(String::class.java)
                    binding.titleText.text = "${nickname}님의 마이페이지"
                }

                override fun onCancelled(error: DatabaseError) {
                    Log.e("MypageActivity", "사용자 정보 로딩 실패: ${error.message}")
                }
            })
    }



    private fun setupViewPager() {
        viewPagerAdapter = MypageViewPagerAdapter(this)
        binding.viewPager.adapter = viewPagerAdapter
    }

    private fun setupTabLayout() {
        TabLayoutMediator(binding.tabLayout, binding.viewPager) { tab, position ->
            tab.text = when (position) {
                0 -> "내 정보"
                1 -> "스크랩한 식당"
                2 -> "작성한 리뷰"
                else -> ""
            }
        }.attach()
    }
}

class MypageViewPagerAdapter(activity: FragmentActivity) : FragmentStateAdapter(activity) {
    override fun getItemCount(): Int = 3

    override fun createFragment(position: Int): Fragment {
        return when (position) {
            0 -> Mypage_info()
            1 -> Mypage_scrapped()
            2 -> Mypage_review()
            else -> throw IllegalArgumentException("Invalid position")
        }
    }
}