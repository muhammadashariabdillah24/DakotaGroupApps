package com.dakotagroupstaff.ui.operasional.letterofassign

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter

/**
 * ViewPager adapter for Letter of Assign tabs (Bongkar/Muat)
 */
class LetterOfAssignPagerAdapter(
    fragmentActivity: FragmentActivity
) : FragmentStateAdapter(fragmentActivity) {

    private val fragments = listOf(
        BongkarFragment.newInstance(),
        MuatFragment.newInstance()
    )

    override fun getItemCount(): Int = fragments.size

    override fun createFragment(position: Int): Fragment = fragments[position]

    fun getBongkarFragment(): BongkarFragment = fragments[0] as BongkarFragment
    fun getMuatFragment(): MuatFragment = fragments[1] as MuatFragment
}
