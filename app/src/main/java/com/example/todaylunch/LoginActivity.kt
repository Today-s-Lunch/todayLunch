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
import com.example.todaylunch.databinding.ActivityLoginBinding
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.ValueEventListener
import com.google.firebase.database.database

class LoginActivity : AppCompatActivity() {
    private val binding: ActivityLoginBinding by lazy { ActivityLoginBinding.inflate(layoutInflater) }
    private lateinit var auth: FirebaseAuth
    private lateinit var database: DatabaseReference

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)

        // Firebase 초기화
        auth = Firebase.auth
        database = Firebase.database.reference

        // 점심 색상만 변경
        val text = "오늘의 점심은"
        val spannableString = SpannableString(text).apply {
            setSpan(ForegroundColorSpan(Color.parseColor("#FF6A50")), 4, 6, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        }
        binding.Today.text = spannableString

        binding.goToSignup.setOnClickListener {
            // SignUpActivity로 이동
            val intent = Intent(this, SignupActivity::class.java)
            startActivity(intent)
        }

        binding.loginBtn.setOnClickListener {
            val studentId = binding.studentId.text.toString().trim()
            val password = binding.pwd.text.toString().trim()

            // 입력값 검증
            if (validateInput(studentId, password)) {
                loginUser(studentId, password)
            }
        }
    }

    private fun validateInput(studentId: String, password: String): Boolean {
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
        }
        return true
    }

    private fun loginUser(studentId: String, password: String) {
        val email = "$studentId@example.com"  // Firebase는 이메일 형식을 요구

        auth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    // 로그인 성공
                    val user = auth.currentUser
                    Log.d("Login", "학번 $studentId 로그인 성공")
                    loadUserData(user?.uid)
                } else {
                    // 로그인 실패
                    Log.e("Login", "학번 $studentId 로그인 실패: ${task.exception?.message}")
                    showToast("로그인 실패: 학번 또는 비밀번호를 확인해주세요")
                }
            }
    }

    private fun loadUserData(userId: String?) {
        if (userId == null) return

        // Realtime Database에서 사용자 정보 가져오기
        database.child("users").child(userId)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    if (snapshot.exists()) {
                        // 로그인 성공 후 StartActivity로 이동
                        val intent = Intent(this@LoginActivity, StartActivity::class.java).apply {
                            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                        }
                        startActivity(intent)
                        finish()
                    } else {
                        showToast("사용자 정보를 찾을 수 없습니다")
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    showToast("데이터베이스 오류: ${error.message}")
                }
            })
    }

    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    // 자동 로그인 처리
    override fun onStart() {
        super.onStart()
        // 이미 로그인된 사용자가 있는지 확인
        val currentUser = auth.currentUser
        if (currentUser != null) {
            // 이메일 주소에서 학번 추출 -> Log 창 출력
            val studentId = currentUser.email?.split("@")?.get(0)
            Log.d("Login", "학번 $studentId 자동 로그인")
            // 자동으로 StartActivity로 이동
            startActivity(Intent(this, StartActivity::class.java))
            finish()
        }
    }
}