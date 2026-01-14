/*
 * Twidere - Twitter client for Android
 *
 *  Copyright (C) 2012-2015 Mariotaku Lee <mariotaku.lee@gmail.com>
 *
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.mariotaku.twidere.view.holder

import android.graphics.PorterDuff
import androidx.core.view.MarginLayoutParamsCompat
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import org.mariotaku.ktextension.applyFontFamily
import org.mariotaku.ktextension.spannable
import org.mariotaku.twidere.R
import org.mariotaku.twidere.adapter.ParcelableActivitiesAdapter
import org.mariotaku.twidere.adapter.iface.IActivitiesAdapter
import org.mariotaku.twidere.databinding.ListItemActivitySummaryCompactBinding
import org.mariotaku.twidere.extension.loadProfileImage
import org.mariotaku.twidere.model.ActivityTitleSummaryMessage
import org.mariotaku.twidere.model.ParcelableActivity
import org.mariotaku.twidere.model.ParcelableLiteUser
import kotlin.math.min

/**
 * Created by mariotaku on 15/1/3.
 */
class ActivityTitleSummaryViewHolder private constructor(
    private val binding: ListItemActivitySummaryCompactBinding,
    private val adapter: ParcelableActivitiesAdapter
) : ViewHolder(binding.root), View.OnClickListener {

    constructor(adapter: ParcelableActivitiesAdapter, itemView: View) : this(
        ListItemActivitySummaryCompactBinding.bind(itemView), adapter
    )

    private val profileImageViews = arrayOf(
        binding.activityProfileImage0,
        binding.activityProfileImage1,
        binding.activityProfileImage2,
        binding.activityProfileImage3,
        binding.activityProfileImage4
    )

    private var activityEventListener: IActivitiesAdapter.ActivityEventListener? = null

    init {
        val resources = adapter.context.resources
        val lp = binding.title.layoutParams as ViewGroup.MarginLayoutParams
        val spacing = resources.getDimensionPixelSize(R.dimen.element_spacing_small)
        lp.leftMargin = spacing
        MarginLayoutParamsCompat.setMarginStart(lp, spacing)
        binding.time.showAbsoluteTime = adapter.showAbsoluteTime
        binding.title.applyFontFamily(adapter.lightFont)
        binding.summary.applyFontFamily(adapter.lightFont)
        binding.time.applyFontFamily(adapter.lightFont)
    }

    fun displayActivity(activity: ParcelableActivity) {
        val context = adapter.context
        val sources = (activity.after_filtered_sources ?: activity.sources_lite).takeIf {
            it.isNotEmpty()
        } ?: run {
            showNotSupported()
            return
        }
        val message = ActivityTitleSummaryMessage.get(context, adapter.userColorNameManager,
                activity, sources, binding.activityType.defaultColor, adapter.useStarsForLikes,
                adapter.isNameFirst)
        if (message == null) {
            showNotSupported()
            return
        }
        binding.activityType.setColorFilter(message.color, PorterDuff.Mode.SRC_ATOP)
        binding.activityType.setImageResource(message.icon)
        binding.title.spannable = message.title
        binding.summary.spannable = message.summary
        binding.summary.visibility = if (binding.summary.length() > 0) View.VISIBLE else View.GONE
        binding.time.time = activity.timestamp
        if (adapter.showAccountsColor) {
            binding.itemContent.drawEnd(activity.account_color)
        } else {
            binding.itemContent.drawEnd()
        }
        displayUserProfileImages(sources)
    }

    private fun showNotSupported() {

    }

    fun setupViewOptions() {
        val textSize = adapter.textSize
        binding.title.textSize = textSize
        binding.summary.textSize = textSize * 0.85f
        binding.time.textSize = textSize * 0.80f

        profileImageViews.forEach {
            it.style = adapter.profileImageStyle
        }
    }

    private fun displayUserProfileImages(users: Array<ParcelableLiteUser>?) {
        val shouldDisplayImages = adapter.profileImageEnabled
        binding.profileImagesContainer.visibility = if (shouldDisplayImages) View.VISIBLE else View.GONE
        binding.profileImageSpace.visibility = if (shouldDisplayImages) View.VISIBLE else View.GONE
        if (!shouldDisplayImages) return
        if (users == null) {
            for (view in profileImageViews) {
                view.visibility = View.GONE
            }
            return
        }
        val length = min(profileImageViews.size, users.size)
        for (i in profileImageViews.indices) {
            val view = profileImageViews[i]
            view.setImageDrawable(null)
            if (i < length) {
                view.visibility = View.VISIBLE
                val context = adapter.context
                adapter.requestManager.loadProfileImage(context, users[i], adapter.profileImageStyle)
                        .into(view)
            } else {
                view.visibility = View.GONE
            }
        }
        if (users.size > profileImageViews.size) {
            val moreNumber = users.size - profileImageViews.size
            binding.activityProfileImageMoreNumber.visibility = View.VISIBLE
            binding.activityProfileImageMoreNumber.setText(moreNumber.toString())
        } else {
            binding.activityProfileImageMoreNumber.visibility = View.GONE
        }
    }

    fun setOnClickListeners() {
        setActivityEventListener(adapter.activityEventListener!!)
    }

    fun setActivityEventListener(listener: IActivitiesAdapter.ActivityEventListener) {
        activityEventListener = listener
        binding.itemContent.setOnClickListener(this)
        //        ((View) itemContent).setOnLongClickListener(this);

    }

    override fun onClick(v: View) {
        if (activityEventListener == null) return
        val position = layoutPosition
        when (v.id) {
            R.id.itemContent -> {
                activityEventListener!!.onActivityClick(this, position)
            }
        }
    }

    companion object {
        const val layoutResource = R.layout.list_item_activity_summary_compact

        fun create(parent: ViewGroup, adapter: ParcelableActivitiesAdapter): ActivityTitleSummaryViewHolder {
            val binding = ListItemActivitySummaryCompactBinding.inflate(
                LayoutInflater.from(parent.context), parent, false
            )
            return ActivityTitleSummaryViewHolder(binding, adapter)
        }
    }

}
