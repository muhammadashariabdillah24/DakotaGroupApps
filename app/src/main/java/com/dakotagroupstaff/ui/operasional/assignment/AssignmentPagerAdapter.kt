package com.dakotagroupstaff.ui.operasional.assignment

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter

class AssignmentPagerAdapter(
    fragmentActivity: FragmentActivity
) : FragmentStateAdapter(fragmentActivity) {
    
    override fun getItemCount(): Int = 2
    
    override fun createFragment(position: Int): Fragment {
        return when (position) {
            0 -> DownPaymentFragment()
            1 -> AdditionalCostFragment()
            else -> throw IllegalArgumentException("Invalid position: $position")
        }
    }
}
