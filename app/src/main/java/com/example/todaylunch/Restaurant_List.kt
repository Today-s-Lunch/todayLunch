package com.example.todaylunch

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.todaylunch.databinding.ActivityRandomBinding
import com.example.todaylunch.databinding.ActivityRestaurantListBinding
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.bitmap.CenterCrop
import com.bumptech.glide.load.resource.bitmap.RoundedCorners
import com.google.firebase.Firebase
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.database.database
import kotlin.random.Random

class Restaurant_List : AppCompatActivity() {
    val binding: ActivityRestaurantListBinding by lazy {
        ActivityRestaurantListBinding.inflate(
            layoutInflater
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(binding.root)
        loadRestaurants()
    }

    inner class RestaurantAdapter(private val restaurantList: List<Restaurant>) :
        RecyclerView.Adapter<RestaurantAdapter.RestaurantViewHolder>() {

        inner class RestaurantViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            val nameTextView: TextView = itemView.findViewById(R.id.res_name)
            val costTextView: TextView = itemView.findViewById(R.id.avg_waiting)
            val waitTextView: TextView = itemView.findViewById(R.id.avg_cost)
            val cookTextView: TextView = itemView.findViewById(R.id.cooking_time)
            val imageView: ImageView = itemView.findViewById(R.id.res_photo)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RestaurantViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_restaurant, parent, false)
            return RestaurantViewHolder(view)
        }
        fun convertTime(timeString: String): String {
            // 'm'과 's'를 분과 초로 분리
            val timePattern = Regex("(\\d+)(m|s)")
            val matchResult = timePattern.find(timeString)

            return if (matchResult != null) {
                val value = matchResult.groupValues[1].toInt()
                when (matchResult.groupValues[2]) {
                    "m" -> "$value 분"  // 'm'이 분이면, 분으로 표시
                    "s" -> "$value 초"  // 's'가 초이면, 초로 표시
                    else -> timeString  // 그 외에는 원본 그대로 반환
                }
            } else {
                timeString  // 변환 불가한 경우 원본 문자열 그대로 반환
            }
        }
        override fun onBindViewHolder(holder: RestaurantViewHolder, position: Int) {
            val restaurant = restaurantList[position]
            holder.nameTextView.text = restaurant.Name

            holder.nameTextView.textSize = 16f // 기본 폰트 크기

            holder.costTextView.text = restaurant.avgcost
                .replace("over", "원 이상")
                .replace("under", "원 이하")

            holder.waitTextView.text = convertTime(restaurant.waittime)
            holder.cookTextView.text = convertTime(restaurant.maketime)
            Glide.with(holder.imageView.context)
                .load(restaurant.photourl)
                .override(100, 100)
                .transform(CenterCrop(), RoundedCorners(20)) // 필요시 모서리 둥글게 처리
                .into(holder.imageView)


        }

        override fun getItemCount() = restaurantList.size
    }

    private fun loadRestaurants() {
        val db = Firebase.database.reference
        val rList = mutableListOf<Restaurant>()

        // Firebase에서 데이터를 비동기적으로 가져옴
        db.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                // 각 레코드를 순회하여 Restaurant 객체를 가져옴
                for (data in snapshot.children) {
                    // 데이터에서 Restaurant 객체로 변환
                    val restaurant = data.getValue(Restaurant::class.java)
                    if (restaurant != null) {
                        rList.add(restaurant)
                        Log.d("Firebase", "Restaurant added: ${restaurant.Name}")
                    }
                }

                // RecyclerView 업데이트: 데이터를 모두 받아온 후에 RecyclerView 업데이트
                val adapter = RestaurantAdapter(rList)
                binding.restaurantList.layoutManager = LinearLayoutManager(this@Restaurant_List)
                binding.restaurantList.adapter = adapter

                // 구분선 추가
                val divider = DividerItemDecoration(
                    binding.restaurantList.context,
                    LinearLayoutManager.VERTICAL
                )
                binding.restaurantList.addItemDecoration(divider)
            }

            override fun onCancelled(error: DatabaseError) {
                // 오류 처리
                Log.e("Firebase", "데이터 로딩 실패: ${error.message}")
            }
        })
    }


    data class Restaurant(
        val Name: String="",
        val waittime: String="",
        val Longitude: String="",
        val Latitude: String="",
        val photourl: String="",
        val link: String="",
        val Address: String="",
        val type: String="",
        val maketime: String="",
        val avgcost: String="",
        val Number: String="",
        val MenuKeywords: String = ""
    )
    }

