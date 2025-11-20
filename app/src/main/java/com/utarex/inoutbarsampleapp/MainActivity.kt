package com.utarex.inoutbarsampleapp

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.utarex.inoutbarsampleapp.databinding.ActivityMainBinding
import kotlin.random.Random

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    private val handler = Handler(Looper.getMainLooper())
    private val updateInterval: Long = 1000 // 1초

    private val updateRunnable = object : Runnable {
        override fun run() {
            updateInOutCount()
            handler.postDelayed(this, updateInterval)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        enableEdgeToEdge()

        // ViewBinding 초기화
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // 시스템 바 inset 적용
        ViewCompat.setOnApplyWindowInsetsListener(binding.main) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // 초기값 세팅 (예시)
        binding.inoutBar.inValue = 5
        binding.inoutBar.outValue = 3
        binding.inoutBar.countValue = 7
        binding.inoutBar.lastEntry = "14:20"
        binding.inoutBar.lastExit = "14:10"

        // 주기적 업데이트 시작
        handler.post(updateRunnable)
    }

    override fun onDestroy() {
        super.onDestroy()
        handler.removeCallbacks(updateRunnable)
    }

    // -----------------------------
    // 샘플용: 랜덤 값 자동 갱신
    // -----------------------------
    private fun updateInOutCount() {
        val inVal = Random.nextInt(0, 20)
        val outVal = Random.nextInt(0, 20)
        val countVal = Random.nextInt(0, 20)

        binding.inoutBar.inValue = inVal
        binding.inoutBar.outValue = outVal
        binding.inoutBar.countValue = countVal

        // lastEntry/Exit는 현재 시각 샘플
        val now = java.text.SimpleDateFormat("HH:mm:ss").format(System.currentTimeMillis())
        binding.inoutBar.lastEntry = "In $now"
        binding.inoutBar.lastExit = "Out $now"
    }
}
