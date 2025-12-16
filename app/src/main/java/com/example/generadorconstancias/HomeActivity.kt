package com.example.generadorconstancias

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.bottomnavigation.BottomNavigationView

class HomeActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home)

        val viewPager = findViewById<ViewPager2>(R.id.viewPager)
        val bottomNav = findViewById<BottomNavigationView>(R.id.bottomNav)

        // Asignar adaptador al ViewPager
        viewPager.adapter = ViewPagerAdapter(this)

        // Cuando el usuario cambie de pantalla deslizando
        viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                super.onPageSelected(position)

                when (position) {
                    0 -> bottomNav.selectedItemId = R.id.nav_home
                    1 -> bottomNav.selectedItemId = R.id.nav_historial
                    2 -> bottomNav.selectedItemId = R.id.nav_graficas
                    3 -> bottomNav.selectedItemId = R.id.nav_servicios  // ← CORREGIR: era 4, debe ser 3
                }
            }
        })

        // Cuando el usuario toque el menú inferior
        bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home -> viewPager.currentItem = 0
                R.id.nav_historial -> viewPager.currentItem = 1
                R.id.nav_graficas -> viewPager.currentItem = 2
                R.id.nav_servicios -> viewPager.currentItem = 3  // ← YA ESTÁ CORRECTO
            }
            true
        }
    }
}