package com.example.generadorconstancias

import androidx.fragment.app.Fragment

import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter

class ViewPagerAdapter(activity: FragmentActivity) : FragmentStateAdapter(activity) {

    override fun getItemCount() = 4

    override fun createFragment(position: Int): Fragment {
        return when (position) {
            0 -> FragmentHome()
            1 -> FragmentHistorial()
            2 -> FragmentGraficas()
            3 -> ServiciosFragment()
            else -> FragmentHome()
        }
    }
}
