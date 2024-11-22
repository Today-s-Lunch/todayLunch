package com.example.todaylunch

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.text.SpannableString
import android.text.Spanned
import android.text.style.ForegroundColorSpan
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.todaylunch.databinding.ActivitySignupBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.ValueEventListener
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase

class SignupActivity : AppCompatActivity() {
    private val binding: ActivitySignupBinding by lazy { ActivitySignupBinding.inflate(layoutInflater) }
    private lateinit var database: DatabaseReference
    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)

        // Firebase 초기화
        database = Firebase.database.reference
        auth = Firebase.auth

        // 점심 색상만 변경
        val text = "오늘의 점심은\n회원가입"
        val spannableString = SpannableString(text).apply {
            setSpan(
                ForegroundColorSpan(Color.parseColor("#FF6A50")),
                4,
                6,
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
            )
        }
        binding.Today.text = spannableString

        // 회원가입 버튼 클릭 리스너
        binding.signUpBtn.setOnClickListener {
            val studentId = binding.studentId.text.toString().trim()
            val password = binding.pwd.text.toString().trim()
            val confirmPassword = binding.confirmpwd.text.toString().trim()
            val nickname = binding.nickname.text.toString().trim()

            if (validateInputs(studentId, password, confirmPassword, nickname)) {
                signUp(studentId, password, nickname)
            }
        }

        // LoginActivity 이동
        binding.goToLogin.setOnClickListener {
            val intent = Intent(this, LoginActivity::class.java)
            startActivity(intent)
        }
    }

    private fun validateInputs(
        studentId: String,
        password: String,
        confirmPassword: String,
        nickname: String
    ): Boolean {
        when {
            studentId.isEmpty() -> {
                showToast("학번을 입력해주세요")
                return false
            }
            !studentId.matches(Regex("^\\d{7}$")) -> {
                showToast("올바른 학번 형식이 아닙니다")
                return false
            }
            password.isEmpty() -> {
                showToast("비밀번호를 입력해주세요")
                return false
            }
            password.length < 6 -> {
                showToast("비밀번호는 최소 6자리 이상이어야 합니다")
                return false
            }
            password != confirmPassword -> {
                showToast("비밀번호가 일치하지 않습니다")
                return false
            }
            nickname.isEmpty() -> {
                showToast("닉네임을 입력해주세요")
                return false
            }
            nickname.length > 10 -> {
                showToast("닉네임은 10자 이하여야 합니다")
                return false
            }
        }
        return true
    }

    private fun signUp(studentId: String, password: String, nickname: String) {
        // 학번 중복 체크
        database.child("users").orderByChild("studentId").equalTo(studentId)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    if (snapshot.exists()) {
                        Log.d("SignUp", "학번 $studentId 회원가입 실패: 이미 존재하는 학번")
                        showToast("이미 등록된 학번입니다")
                    } else {
                        // Firebase Authentication으로 계정 생성
                        auth.createUserWithEmailAndPassword("$studentId@example.com", password)
                            .addOnCompleteListener { task ->
                                if (task.isSuccessful) {
                                    val userId = auth.currentUser?.uid ?: return@addOnCompleteListener
                                    Log.d("SignUp", "학번 $studentId 회원가입 성공")

                                    // Realtime Database에 사용자 정보 저장
                                    val userMap = hashMapOf(
                                        "studentId" to studentId,
                                        "nickname" to nickname
                                    )

                                    database.child("users").child(userId).setValue(userMap)
                                        .addOnSuccessListener {
                                            Log.d("SignUp", "학번 $studentId 데이터베이스 저장 성공")
                                            showToast("회원가입이 완료되었습니다")
                                            // 회원가입 완료 후 LoginActivity로 이동
                                            val intent = Intent(this@SignupActivity, LoginActivity::class.java)
                                            startActivity(intent)
                                            finish()
                                        }
                                        .addOnFailureListener { e ->
                                            Log.e("SignUp", "학번 $studentId 데이터베이스 저장 실패: ${e.message}")
                                            showToast("회원가입 실패: ${e.message}")
                                        }
                                } else {
                                    Log.e("SignUp", "학번 $studentId 계정 생성 실패: ${task.exception?.message}")
                                    showToast("계정 생성 실패: ${task.exception?.message}")
                                }
                            }
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    Log.e("SignUp", "데이터베이스 오류: ${error.message}")
                    showToast("데이터베이스 오류: ${error.message}")
                }
            })
    }

    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }
}