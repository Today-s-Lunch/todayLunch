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

        // 기존 인텐트 데이터 초기화
        clearIntentData()

        // 버튼 리스너 및 검색 설정
        setupButtonListeners()
        setupSearchButtonListener()
        resetAllSelections()

        // 닫기 버튼
        binding.closebutton.setOnClickListener {
            finish()
        }

        // 이전 검색어 초기화
        val currentSearchText = intent.getStringExtra("currentSearchText") ?: ""
        binding.searchEditText.setText(currentSearchText)
        if (currentSearchText.isNotEmpty()) {
            binding.searchEditText.setSelection(currentSearchText.length)
        }
    }

    private fun clearIntentData() {
        // 이전 Intent Extras를 초기화
        intent.removeExtra("type")
        intent.removeExtra("distance")
        intent.removeExtra("cookingTime")
        intent.removeExtra("avgPrice")
        intent.removeExtra("waitTime")
        intent.removeExtra("currentSearchText")

        selectedCriteria.clear() // 선택된 조건 초기화
    }

    private fun resetAllSelections() {
        // 모든 버튼 초기화
        val buttonGroups = listOf(
            binding.typeOptions,
            binding.distanceOptions,
            binding.cookingTimeOptions,
            binding.priceOptions,
            binding.waitTimeOptions
        )

        for (group in buttonGroups) {
            for (i in 0 until group.childCount) {
                val button = group.getChildAt(i) as MaterialButton
                button.tag = false // 선택 상태 초기화
                button.backgroundTintList = resources.getColorStateList(android.R.color.white, null)
                button.setTextColor(resources.getColor(android.R.color.black, null))
            }
        }
    }

    private fun setupButtonListeners() {
        val buttonGroups = listOf(
            binding.typeOptions to "type",
            binding.distanceOptions to "distance",
            binding.cookingTimeOptions to "cookingTime",
            binding.priceOptions to "avgPrice",
            binding.waitTimeOptions to "waitTime"
        )

        for ((group, key) in buttonGroups) {
            for (i in 0 until group.childCount) {
                val button = group.getChildAt(i) as MaterialButton

                if (key == "type") {
                    button.setOnClickListener {
                        handleMultiSelection(group, button, key)
                    }
                } else {
                    button.setOnClickListener {
                        handleSingleSelection(group, button, key)
                    }
                }
            }
        }
    }

    private fun handleMultiSelection(group: ViewGroup, selectedButton: MaterialButton, key: String) {
        val currentSelection = selectedCriteria[key]?.split(",")?.toMutableList() ?: mutableListOf()

        if (selectedButton.tag == true) {
            selectedButton.tag = false
            selectedButton.backgroundTintList = resources.getColorStateList(android.R.color.white, null)
            selectedButton.setTextColor(resources.getColor(android.R.color.black, null))
            currentSelection.remove(selectedButton.text.toString())
        } else {
            selectedButton.tag = true
            selectedButton.backgroundTintList = resources.getColorStateList(R.color.selectedButtonTint, null)
            selectedButton.setTextColor(resources.getColor(android.R.color.black, null))
            currentSelection.add(selectedButton.text.toString())
        }

        if (currentSelection.isEmpty()) {
            selectedCriteria.remove(key)
        } else {
            selectedCriteria[key] = currentSelection.joinToString(",")
        }
        updateRecommendationText() // 조건 업데이트
    }

    private fun handleSingleSelection(group: ViewGroup, selectedButton: MaterialButton, key: String) {
        if (selectedButton.tag == true) {
            selectedButton.tag = false
            selectedButton.backgroundTintList = resources.getColorStateList(android.R.color.white, null)
            selectedButton.setTextColor(resources.getColor(android.R.color.black, null))
            selectedCriteria.remove(key)
        } else {
            for (i in 0 until group.childCount) {
                val button = group.getChildAt(i) as MaterialButton
                button.tag = false
                button.backgroundTintList = resources.getColorStateList(android.R.color.white, null)
                button.setTextColor(resources.getColor(android.R.color.black, null))
            }

            selectedButton.tag = true
            selectedButton.backgroundTintList = resources.getColorStateList(R.color.selectedButtonTint, null)
            selectedButton.setTextColor(resources.getColor(android.R.color.black, null))
            selectedCriteria[key] = selectedButton.text.toString()
        }
        updateRecommendationText() // 조건 업데이트
    }
    private fun updateRecommendationText() {
        val recommendationText = if (selectedCriteria.isNotEmpty()) {
            selectedCriteria.entries.joinToString(", ") { (key, value) ->
                when (key) {
                    "type" -> "종류: $value"
                    "distance" -> "거리: $value"
                    "cookingTime" -> "조리시간: $value"
                    "avgPrice" -> "가격대: $value"
                    "waitTime" -> "대기시간: $value"
                    else -> ""
                }
            }
        } else {
            "딱 맞는 메뉴를 추천해드려요"
        }

        binding.textView.text = recommendationText
    }

    private fun setupSearchButtonListener() {
        binding.searchIcon.setOnClickListener {
            val intent = Intent(this, Restaurant_List::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
                for ((key, value) in selectedCriteria) {
                    putExtra(key, value)
                }
                putExtra("searchText", binding.searchEditText.text.toString())
            }
            startActivity(intent)
        }
    }
    override fun onResume() {
        super.onResume()

        // 액티비티가 다시 활성화될 때 초기화
        clearIntentData() // Intent 초기화
        resetAllSelections() // 조건 초기화
        binding.searchEditText.setText("") // 검색어 초기화
        binding.textView.text="딱 맞는 메뉴를 추천해드려요"
    }
}