package com.example.todaylunch

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import com.example.todaylunch.databinding.ActivitySearchBinding
import com.google.android.material.button.MaterialButton

class SearchActivity : AppCompatActivity() {


    private lateinit var binding: ActivitySearchBinding
    private var selectedCriteria: MutableMap<String, String> = mutableMapOf()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySearchBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupButtonListeners()
        setupSearchButtonListener()

        binding.closebutton.setOnClickListener {
            finish()
        }
    }

    private fun setupButtonListeners() {
        // 각 버튼 그룹과 필터 키 연결
        val buttonGroups = listOf(
            binding.typeOptions to "type",
            binding.distanceOptions to "distance",
            binding.cookingTimeOptions to "cookingTime",
            binding.priceOptions to "avgPrice",
            binding.waitTimeOptions to "waitTime" // 대기시간 추가
        )

        // 각 그룹의 버튼 클릭 리스너 설정
        for ((group, key) in buttonGroups) {
            for (i in 0 until group.childCount) {
                val button = group.getChildAt(i) as MaterialButton
                button.setOnClickListener {
                    handleButtonSelection(group, button, key)
                }
            }
        }
    }

    private fun handleButtonSelection(group: ViewGroup, selectedButton: MaterialButton, key: String) {
        // 현재 선택된 버튼이 이미 활성화 상태인지 확인
        if (selectedButton.tag == true) {
            // 선택 취소: 상태를 초기화
            selectedButton.tag = false
            selectedButton.backgroundTintList = resources.getColorStateList(android.R.color.white, null) // 기본 배경색
            selectedButton.setTextColor(resources.getColor(android.R.color.black, null)) // 기본 텍스트 색상
            selectedCriteria.remove(key) // 선택 기준에서 제거
        } else {
            // 기존 선택 상태 초기화 (같은 그룹 내)
            for (i in 0 until group.childCount) {
                val button = group.getChildAt(i) as MaterialButton
                button.tag = false
                button.backgroundTintList = resources.getColorStateList(android.R.color.white, null) // 기본 배경색
                button.setTextColor(resources.getColor(android.R.color.black, null)) // 기본 텍스트 색상
            }

            // 현재 버튼 선택 활성화
            selectedButton.tag = true
            selectedButton.backgroundTintList = resources.getColorStateList(R.color.selectedButtonTint, null) // 선택된 배경색
            selectedButton.setTextColor(resources.getColor(android.R.color.black, null)) // 선택된 텍스트 색상
            selectedCriteria[key] = selectedButton.text.toString() // 선택 기준 저장
        }
    }

    private fun setupSearchButtonListener() {
        binding.searchIcon.setOnClickListener {
            // `Restaurant_List`로 선택된 기준 전달
            val intent = Intent(this, Restaurant_List::class.java)
            for ((key, value) in selectedCriteria) {
                intent.putExtra(key, value)

            }
            startActivity(intent)
        }
    }
}