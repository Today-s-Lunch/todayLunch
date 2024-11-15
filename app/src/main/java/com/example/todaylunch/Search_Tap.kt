package com.example.todaylunch

import android.app.Dialog
import android.os.Bundle
import android.view.GestureDetector
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.DialogFragment
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import android.view.WindowManager // 이 부분 추가
import android.view.Gravity
import android.view.MotionEvent
import android.view.animation.Animation
import android.view.animation.TranslateAnimation
import androidx.fragment.app.Fragment
import com.example.todaylunch.databinding.FragmentSearchTapBinding

class Search_Tap : Fragment() {

    private var _binding: FragmentSearchTapBinding? = null
    private val binding get() = _binding
    private lateinit var gestureDetector: GestureDetector

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentSearchTapBinding.inflate(inflater, container, false)
        setupGestureDetector()
        return _binding?.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun setupGestureDetector() {
        gestureDetector = GestureDetector(requireContext(), object : GestureDetector.SimpleOnGestureListener() {
            override fun onFling(
                e1: MotionEvent?,
                e2: MotionEvent,
                velocityX: Float,
                velocityY: Float
            ): Boolean {
                if (e1 != null && e2 != null) {
                    if (e1.y - e2.y > 100 && velocityY < -1000) {
                        closeSearch()
                        return true
                    }
                }
                return false
            }
        })

        // GestureDetector를 검색 컨테이너에 연결
        binding?.searchContainer?.setOnTouchListener { _, event ->
            gestureDetector.onTouchEvent(event)
            true
        }
    }

    private fun closeSearch() {
        binding?.let { binding ->
            slideUp(binding.searchContainer) {
                parentFragmentManager.beginTransaction()
                    .remove(this@Search_Tap)
                    .commit()
            }
        }
    }

    private fun slideUp(view: View, onEnd: (() -> Unit)? = null) {
        val animate = TranslateAnimation(
            0f, 0f, 0f, -view.height.toFloat()
        )
        animate.duration = 500
        animate.fillAfter = true
        view.startAnimation(animate)
        animate.setAnimationListener(object : android.view.animation.Animation.AnimationListener {
            override fun onAnimationStart(animation: android.view.animation.Animation?) {}
            override fun onAnimationEnd(animation: android.view.animation.Animation?) {
                view.visibility = View.GONE
                onEnd?.invoke()
            }

            override fun onAnimationRepeat(animation: android.view.animation.Animation?) {}
        })
    }

    fun toggleSearch() {
        binding?.let { binding ->
            if (binding.searchContainer.visibility == View.VISIBLE) {
                closeSearch()
            } else {
                slideDown(binding.searchContainer)
            }
        }
    }

    private fun slideDown(view: View) {
        view.visibility = View.VISIBLE
        val animate = TranslateAnimation(
            0f, 0f, -view.height.toFloat(), 0f
        )
        animate.duration = 500
        animate.fillAfter = true
        view.startAnimation(animate)
    }
    }

