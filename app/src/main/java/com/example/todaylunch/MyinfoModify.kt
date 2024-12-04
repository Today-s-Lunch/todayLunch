package com.example.todaylunch

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.text.InputType
import android.text.SpannableString
import android.text.Spanned
import android.text.style.ForegroundColorSpan
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.adapters.ViewBindingAdapter.setPadding
import com.example.todaylunch.databinding.ActivityMyinfoModifyBinding
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase

class MyinfoModify : AppCompatActivity() {
    private val binding: ActivityMyinfoModifyBinding by lazy { ActivityMyinfoModifyBinding.inflate(layoutInflater) }
    private lateinit var auth: FirebaseAuth
    private lateinit var database: DatabaseReference

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)

        // Firebase 초기화
        auth = FirebaseAuth.getInstance()
        database = FirebaseDatabase.getInstance().reference

        // 현재 사용자 ID 가져오기
        val currentUser = auth.currentUser
        val uid = currentUser?.uid ?: return

        // 현재 사용자 정보 불러오기 및 화면 세팅
        setupUserInfo(uid)

        // 제목 텍스트 스타일 설정
        setupTitleText()

        // 저장 버튼 클릭 리스너
        binding.saveButton.setOnClickListener {
            handleSaveButtonClick(uid)
        }

        //하단바
        val MYPAGE = Intent(this, MypageActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val HOME = Intent(this, StartActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
        }

        // 언더바 버튼 동작 설정
        binding.underbar.backButton.setOnClickListener { onBackPressedDispatcher.onBackPressed() }
        binding.underbar.homeButton.setOnClickListener { startActivity(HOME) }
        binding.underbar.myPageButton.setOnClickListener { startActivity(MYPAGE) }
    }

    private fun setupUserInfo(uid: String) {
        database.child("users").child(uid).get().addOnSuccessListener { snapshot ->
            binding.studentId.text = snapshot.child("studentId").value.toString()
            binding.nicknameEditText.setText(snapshot.child("nickname").value.toString())
        }
    }

    private fun setupTitleText() {
        val spannableString = SpannableString("내정보 수정하기").apply {
            setSpan(ForegroundColorSpan(Color.parseColor("#FA6A50")), 4, 6, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        }
        binding.titleTextView.text = spannableString
    }

    private fun handleSaveButtonClick(uid: String) {
        val newNickname = binding.nicknameEditText.text.toString()
        val newPassword = binding.passwordEditText.text.toString()
        val confirmPassword = binding.confirmPasswordEditText.text.toString()

        // 비밀번호 변경이 있는 경우
        if (newPassword.isNotEmpty() || confirmPassword.isNotEmpty()) {
            // 비밀번호 입력값 검증
            if (!validatePasswordInputs(newPassword, confirmPassword)) return

            // 현재 비밀번호 확인을 위한 다이얼로그 표시
            showPasswordConfirmDialog(uid, newPassword, newNickname)
        } else if (newNickname.isNotEmpty()) {
            // 닉네임만 변경하는 경우
            updateNickname(uid, newNickname)
        }
    }

    private fun validatePasswordInputs(newPassword: String, confirmPassword: String): Boolean {
        when {
            newPassword != confirmPassword -> {
                Toast.makeText(this, "비밀번호가 일치하지 않습니다.", Toast.LENGTH_SHORT).show()
                return false
            }
            newPassword.length < 6 -> {
                Toast.makeText(this, "비밀번호는 6자리 이상이어야 합니다.", Toast.LENGTH_SHORT).show()
                return false
            }
        }
        return true
    }

    private fun showPasswordConfirmDialog(uid: String, newPassword: String, newNickname: String) {
        // EditText를 감쌀 LinearLayout 생성
        val container = LinearLayout(this).apply {
            setPadding(60, 0, 60, 0) // 좌우 60픽셀의 패딩
            orientation = LinearLayout.VERTICAL
        }

        val editText = EditText(this).apply {
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        }

        // EditText를 container에 추가
        container.addView(editText)

        AlertDialog.Builder(this)
            .setTitle("비밀번호 확인")
            .setMessage("비밀번호 변경을 위해 현재 비밀번호를 입력해주세요.")
            .setView(container)  // container를 dialog의 view로 설정
            .setPositiveButton("확인") { _, _ ->
                val currentPassword = editText.text.toString()
                val email = "${binding.studentId.text}@example.com"

                val credential = EmailAuthProvider.getCredential(email, currentPassword)
                auth.currentUser?.reauthenticate(credential)?.addOnCompleteListener { reauthTask ->
                    if (reauthTask.isSuccessful) {
                        updatePassword(newPassword, uid, newNickname)
                    } else {
                        Toast.makeText(this, "현재 비밀번호가 일치하지 않습니다.", Toast.LENGTH_SHORT).show()
                    }
                }
            }
            .setNegativeButton("취소", null)
            .show()
    }

    private fun updatePassword(newPassword: String, uid: String, newNickname: String) {
        auth.currentUser?.updatePassword(newPassword)?.addOnCompleteListener { task ->
            if (task.isSuccessful) {
                if (newNickname.isNotEmpty()) {
                    // 비밀번호와 닉네임 모두 변경하는 경우
                    database.child("users").child(uid).child("nickname").setValue(newNickname)
                        .addOnCompleteListener { nicknameTask ->
                            if (nicknameTask.isSuccessful) {
                                Toast.makeText(this, "정보가 성공적으로 수정되었습니다.", Toast.LENGTH_SHORT).show()
                                navigateToMyPage()
                            } else {
                                Toast.makeText(this, "닉네임 변경에 실패했습니다.", Toast.LENGTH_SHORT).show()
                            }
                        }
                } else {
                    // 비밀번호만 변경하는 경우
                    Toast.makeText(this, "비밀번호가 수정되었습니다.", Toast.LENGTH_SHORT).show()
                    navigateToMyPage()
                }
            } else {
                Toast.makeText(this, "비밀번호 변경에 실패했습니다.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun updateNickname(uid: String, newNickname: String) {
        database.child("users").child(uid).child("nickname").setValue(newNickname)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    // 닉네임만 변경하는 경우
                    Toast.makeText(this, "닉네임이 수정되었습니다.", Toast.LENGTH_SHORT).show()
                    navigateToMyPage()
                } else {
                    Toast.makeText(this, "닉네임 변경에 실패했습니다.", Toast.LENGTH_SHORT).show()
                }
            }
    }

    private fun navigateToMyPage() {
        startActivity(Intent(this, MypageActivity::class.java))
        finish()
    }
}