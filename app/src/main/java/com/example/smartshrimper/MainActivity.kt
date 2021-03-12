package com.example.smartshrimper

import android.os.Bundle
import android.os.Handler
import android.os.HandlerThread
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.tabs.TabLayout
import androidx.viewpager.widget.ViewPager
import androidx.appcompat.app.AppCompatActivity
import com.example.smartshrimper.ui.main.Gauge
import com.example.smartshrimper.ui.main.SectionsPagerAdapter
import kotlinx.coroutines.delay


interface SendChannel<in E> {
    suspend fun send(element: E)
    fun close(): Boolean
}

interface ReceiveChannel<out E> {
    suspend fun receive(): E
}

interface Channel<E> : SendChannel<E>, ReceiveChannel<E>

val streamBuffer = RingBuffer(20)


class MainActivity : AppCompatActivity() {
//    private lateinit var binding: ActivityMainBinding
    private lateinit var revCounter: Gauge


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
//        binding = ActivityMainBinding.inflate(layoutInflater)
//        setContentView(binding.root)

        setContentView(R.layout.activity_main)
        val sectionsPagerAdapter = SectionsPagerAdapter(this, supportFragmentManager)
        val viewPager: ViewPager = findViewById(R.id.view_pager)
        viewPager.adapter = sectionsPagerAdapter
        val tabs: TabLayout = findViewById(R.id.tabs)
        tabs.setupWithViewPager(viewPager)
        val fab: FloatingActionButton = findViewById(R.id.fab)

        fab.setOnClickListener { view ->
            Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
                    .setAction("Action", null).show()

        }

        revCounter = findViewById(R.id.revCounter)

    }

    override suspend fun onResume() {
        super.onResume()
        while (true) {
            revCounter.moveToValue(streamBuffer.readNext().toFloat())
            delay(200L)
        }


    }
}