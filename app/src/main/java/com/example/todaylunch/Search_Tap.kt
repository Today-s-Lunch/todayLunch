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

class Search_Tap : DialogFragment() {
    private lateinit var gestureDetector: GestureDetector
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_search__tap, container, false)
        gestureDetector = GestureDetector(context, SwipeGestureListener())
        view?.setOnTouchListener { _, event ->
            gestureDetector.onTouchEvent(event)
            true
        }
        return view
    }

    override fun onStart() {
        super.onStart()
        dialog?.window?.apply {
            setLayout(WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.WRAP_CONTENT)
            setGravity(Gravity.TOP) // 상단에 위치하도록 설정
            setWindowAnimations(R.style.TopSlideAnimation) // 애니메이션 설정
        }
    }


        private inner class SwipeGestureListener : GestureDetector.SimpleOnGestureListener() {
            private val SWIPE_THRESHOLD = 100
            private val SWIPE_VELOCITY_THRESHOLD = 100

            override fun onFling(
                e1: MotionEvent?,
                e2: MotionEvent,
                velocityX: Float,
                velocityY: Float
            ): Boolean {
                val diffY = e2.y - (e1?.y ?: 0f)
                if (diffY < -SWIPE_THRESHOLD && Math.abs(velocityY) > SWIPE_VELOCITY_THRESHOLD) {
                    dismiss() // 위로 스와이프하면 다이얼로그 닫기
                    return true
                }
                return false
            }
        }
    }

