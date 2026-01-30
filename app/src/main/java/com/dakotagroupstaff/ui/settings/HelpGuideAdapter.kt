package com.dakotagroupstaff.ui.settings

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.Animation
import android.view.animation.RotateAnimation
import androidx.recyclerview.widget.RecyclerView
import com.dakotagroupstaff.databinding.ItemHelpGuideBinding

class HelpGuideAdapter(
    private val items: List<HelpGuideItem>
) : RecyclerView.Adapter<HelpGuideAdapter.ViewHolder>() {

    inner class ViewHolder(private val binding: ItemHelpGuideBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(item: HelpGuideItem) {
            binding.tvTitle.text = item.title
            binding.tvDescription.text = item.description

            // Set initial state
            binding.tvDescription.visibility = if (item.isExpanded) View.VISIBLE else View.GONE
            rotateIcon(binding.ivExpandIcon, item.isExpanded, false)

            // Handle click to expand/collapse
            binding.headerLayout.setOnClickListener {
                item.isExpanded = !item.isExpanded
                
                // Animate description visibility
                binding.tvDescription.visibility = if (item.isExpanded) View.VISIBLE else View.GONE
                
                // Rotate icon
                rotateIcon(binding.ivExpandIcon, item.isExpanded, true)
            }
        }

        private fun rotateIcon(view: View, isExpanded: Boolean, animate: Boolean) {
            val fromDegrees = if (isExpanded) 0f else 180f
            val toDegrees = if (isExpanded) 180f else 0f

            if (animate) {
                val rotate = RotateAnimation(
                    fromDegrees, toDegrees,
                    Animation.RELATIVE_TO_SELF, 0.5f,
                    Animation.RELATIVE_TO_SELF, 0.5f
                )
                rotate.duration = 200
                rotate.fillAfter = true
                view.startAnimation(rotate)
            } else {
                view.rotation = toDegrees
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemHelpGuideBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount(): Int = items.size
}
