package com.example.todaylunch

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
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

        // 이전에 선택된 필터들을 먼저 가져와서 selectedCriteria 초기화
        selectedCriteria = mutableMapOf(
            "type" to (intent.getStringExtra("type") ?: ""),
            "distance" to (intent.getStringExtra("distance") ?: ""),
            "cookingTime" to (intent.getStringExtra("cookingTime") ?: ""),
            "avgPrice" to (intent.getStringExtra("avgPrice") ?: ""),
            "waitTime" to (intent.getStringExtra("waitTime") ?: "")
        ).filterValues { it.isNotEmpty() }.toMutableMap()

        setupButtonListeners()
        setupSearchButtonListener()
        restorePreviousSelections()

        binding.closebutton.setOnClickListener {
            finish()
        }

        // 이전 검색어 가져오기
        val currentSearchText = intent.getStringExtra("currentSearchText") ?: ""
        binding.searchEditText.setText(currentSearchText) // 검색 EditText에 이전 검색어 설정

        if (currentSearchText.isNotEmpty()) {
            binding.searchEditText.setSelection(currentSearchText.length)
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

            // 검색어 전달
            val searchText = binding.searchEditText.text.toString()
            intent.putExtra("searchText", searchText)

            startActivity(intent)
        }
    }

    // 이전 선택 상태를 복원하는 함수 추가
    private fun restorePreviousSelections() {
        val buttonGroups = mapOf(
            binding.typeOptions to "type",
            binding.distanceOptions to "distance",
            binding.cookingTimeOptions to "cookingTime",
            binding.priceOptions to "avgPrice",
            binding.waitTimeOptions to "waitTime"
        )

        // 각 그룹에 대해
        for ((group, key) in buttonGroups) {
            // 해당 키에 대한 선택된 값이 있다면
            val selectedValue = selectedCriteria[key]
            if (selectedValue != null) {
                // 그룹 내의 모든 버튼을 확인
                for (i in 0 until group.childCount) {
                    val button = group.getChildAt(i) as MaterialButton
                    if (button.text.toString() == selectedValue) {
                        // 일치하는 버튼을 찾으면 선택 상태로 설정
                        button.tag = true
                        button.backgroundTintList =
                            resources.getColorStateList(R.color.selectedButtonTint, null)
                        button.setTextColor(resources.getColor(android.R.color.black, null))
                        break
                    }
                }
            }
        }
    }
}