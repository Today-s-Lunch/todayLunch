package com.example.todaylunch

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.example.todaylunch.databinding.FragmentMypageInfoBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.ktx.Firebase

class Mypage_info : Fragment() {
    private var _binding: FragmentMypageInfoBinding? = null
    private val binding get() = _binding!!
    private val auth: FirebaseAuth by lazy { Firebase.auth }
    private val database = FirebaseDatabase.getInstance()

    companion object {
        private const val TAG = "Mypage_info"
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentMypageInfoBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // 현재 사용자 정보 로드
        loadUserInfo()

        // 로그아웃 버튼 클릭 리스너
        binding.logoutBtn.setOnClickListener {
            auth.signOut()
            val intent = Intent(requireActivity(), LoginActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            }
            startActivity(intent)
            requireActivity().finish()
        }

        // 정보 수정 버튼 클릭 리스너
        binding.editInfoBtn.setOnClickListener {
            // 정보 수정 화면으로 이동하는 코드 구현
        }
    }

    private fun loadUserInfo() {
        val currentUser = auth.currentUser
        val userId = currentUser?.uid

        if (userId != null) {
            database.reference.child("users").child(userId)
                .addListenerForSingleValueEvent(object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        val nickname = snapshot.child("nickname").getValue(String::class.java)
                        val studentId = snapshot.child("studentId").getValue(String::class.java)

                        binding.nickname.text = nickname ?: ""
                        binding.studentId.text = studentId ?: ""
                        binding.passwordText.text = "**********"  // 비밀번호는 항상 마스킹 처리
                    }

                    override fun onCancelled(error: DatabaseError) {
                        Log.e(TAG, "사용자 정보 로드 실패: ${error.message}")
                    }
                })
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}