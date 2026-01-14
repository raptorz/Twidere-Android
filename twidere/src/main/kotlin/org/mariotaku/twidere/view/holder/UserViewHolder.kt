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

import androidx.recyclerview.widget.RecyclerView.ViewHolder
import android.view.View
import android.view.View.OnClickListener
import android.view.View.OnLongClickListener
import android.widget.RelativeLayout
import org.mariotaku.ktextension.hideIfEmpty
import org.mariotaku.ktextension.spannable
import org.mariotaku.twidere.R
import org.mariotaku.twidere.adapter.iface.IUsersAdapter
import org.mariotaku.twidere.adapter.iface.IUsersAdapter.*
import org.mariotaku.twidere.databinding.ListItemUserBinding
import org.mariotaku.twidere.extension.loadProfileImage
import org.mariotaku.twidere.extension.model.hasSameHost
import org.mariotaku.twidere.model.ParcelableUser
import org.mariotaku.twidere.util.Utils
import org.mariotaku.twidere.util.Utils.getUserTypeIconRes
import java.util.*

class UserViewHolder private constructor(
        private val binding: ListItemUserBinding,
        private val adapter: IUsersAdapter<*>,
        private val simple: Boolean = false,
        private val showFollow: Boolean = false
) : ViewHolder(binding.root), OnClickListener, OnLongClickListener {

    private val itemContent = binding.itemContent
    private val profileImageView = binding.profileImage
    private val profileTypeView = binding.profileType
    private val nameView = binding.name
    private val externalIndicator = binding.externalIndicator
    private val descriptionView = binding.description
    private val locationView = binding.location
    private val urlView = binding.url
    private val statusesCountView = binding.statusesCount
    private val followersCountView = binding.followersCount
    private val friendsCountView = binding.friendsCount

    private val acceptRequestButton = binding.acceptRequest
    private val denyRequestButton = binding.denyRequest
    private val unblockButton = binding.unblock
    private val unmuteButton = binding.unmute
    private val followButton = binding.follow
    private val actionsProgressContainer = binding.actionsProgressContainer
    private val actionsContainer = binding.actionsContainer
    private val processingRequestProgress = binding.processingRequest
    private val countsContainer = binding.countsContainer

    private var userClickListener: UserClickListener? = null
    private var requestClickListener: RequestClickListener? = null

    private var friendshipClickListener: FriendshipClickListener? = null

    constructor(
            itemView: View,
            adapter: IUsersAdapter<*>,
            simple: Boolean = false,
            showFollow: Boolean = false
    ) : this(ListItemUserBinding.bind(itemView), adapter, simple, showFollow)

    companion object {
        fun create(
                binding: ListItemUserBinding,
                adapter: IUsersAdapter<*>,
                simple: Boolean = false,
                showFollow: Boolean = false
        ): UserViewHolder {
            return UserViewHolder(binding, adapter, simple, showFollow)
        }
    }

    init {
        if (simple) {
            externalIndicator.visibility = View.GONE
            descriptionView.visibility = View.GONE
            locationView.visibility = View.GONE
            urlView.visibility = View.GONE
            countsContainer.visibility = View.GONE

            binding.profileImageContainer.layoutParams.apply {
                (this as RelativeLayout.LayoutParams).clearVerticalRules()
                this.addRule(RelativeLayout.CENTER_VERTICAL)
            }
            nameView.layoutParams.apply {
                (this as RelativeLayout.LayoutParams).clearVerticalRules()
                this.addRule(RelativeLayout.CENTER_VERTICAL)
            }
            actionsProgressContainer.layoutParams.apply {
                (this as RelativeLayout.LayoutParams).clearVerticalRules()
                this.addRule(RelativeLayout.CENTER_VERTICAL)
            }
        }
    }

    fun display(user: ParcelableUser) {
        val context = binding.root.context
        val manager = adapter.userColorNameManager
        val twitter = adapter.twitterWrapper

        itemContent.drawStart(manager.getUserColor(user.key))

        val userTypeRes = getUserTypeIconRes(user.is_verified, user.is_protected)
        if (userTypeRes != 0) {
            profileTypeView.setImageResource(userTypeRes)
        } else {
            profileTypeView.setImageDrawable(null)
        }
        nameView.name = manager.getUserNickname(user.key, user.name)
        nameView.screenName = "@${user.screen_name}"
        nameView.updateText(adapter.bidiFormatter)

        if (adapter.profileImageEnabled) {
            profileImageView.visibility = View.VISIBLE
            adapter.requestManager.loadProfileImage(context, user, adapter.profileImageStyle,
                    profileImageView.cornerRadius, profileImageView.cornerRadiusRatio,
                    adapter.profileImageSize).into(profileImageView)
        } else {
            profileImageView.visibility = View.GONE
        }

        val accountKey = user.account_key
        if (accountKey != null && twitter.isUpdatingRelationship(accountKey, user.key)) {
            processingRequestProgress.visibility = View.VISIBLE
            actionsContainer.visibility = View.GONE
        } else {
            processingRequestProgress.visibility = View.GONE
            actionsContainer.visibility = View.VISIBLE
        }
        if (accountKey != null && user.key.hasSameHost(accountKey)) {
            externalIndicator.visibility = View.GONE
        } else {
            externalIndicator.visibility = View.VISIBLE
            externalIndicator.text = context.getString(R.string.external_user_host_format, user.key.host)
        }

        followButton.setImageResource(if (user.is_following) R.drawable.ic_action_confirm else R.drawable.ic_action_add)
        followButton.isActivated = user.is_following

        val isMySelf = accountKey == user.key

        if (requestClickListener != null && !isMySelf) {
            acceptRequestButton.visibility = View.VISIBLE
            denyRequestButton.visibility = View.VISIBLE
        } else {
            acceptRequestButton.visibility = View.GONE
            denyRequestButton.visibility = View.GONE
        }
        if (friendshipClickListener != null && !isMySelf) {
            if (user.extras?.blocking == true) {
                followButton.visibility = View.GONE
                unblockButton.visibility = View.VISIBLE
            } else {
                if (showFollow) {
                    followButton.visibility = View.VISIBLE
                } else {
                    followButton.visibility = View.GONE
                }
                unblockButton.visibility = View.GONE
            }
            unmuteButton.visibility = if (user.extras?.muting == true) View.VISIBLE else View.GONE
        } else {
            followButton.visibility = View.GONE
            unblockButton.visibility = View.GONE
            unmuteButton.visibility = View.GONE
        }

        if (!simple) {
            descriptionView.spannable = user.description_unescaped
            descriptionView.hideIfEmpty()
            locationView.spannable = user.location
            locationView.hideIfEmpty()
            urlView.spannable = user.url_expanded
            urlView.hideIfEmpty()
            val locale = Locale.getDefault()
            statusesCountView.text = Utils.getLocalizedNumber(locale, user.statuses_count)
            followersCountView.text = Utils.getLocalizedNumber(locale, user.followers_count)
            friendsCountView.text = Utils.getLocalizedNumber(locale, user.friends_count)
        }
    }

    override fun onClick(v: View) {
        when (v.id) {
            R.id.itemContent -> {
                userClickListener?.onUserClick(this, layoutPosition)
            }
            R.id.acceptRequest -> {
                requestClickListener?.onAcceptClicked(this, layoutPosition)
            }
            R.id.denyRequest -> {
                requestClickListener?.onDenyClicked(this, layoutPosition)
            }
            R.id.follow -> {
                friendshipClickListener?.onFollowClicked(this, layoutPosition)
            }
            R.id.unblock -> {
                friendshipClickListener?.onUnblockClicked(this, layoutPosition)
            }
            R.id.unmute -> {
                friendshipClickListener?.onUnmuteClicked(this, layoutPosition)
            }
        }
    }

    override fun onLongClick(v: View): Boolean {
        when (v.id) {
            R.id.itemContent -> {
                return userClickListener?.onUserLongClick(this, layoutPosition) ?: false
            }
        }
        return false
    }

    fun setOnClickListeners() {
        setUserClickListener(adapter.userClickListener)
        setActionClickListeners(adapter.requestClickListener, adapter.friendshipClickListener)
    }

    private fun setActionClickListeners(requestClickListener: RequestClickListener?,
            friendshipClickListener: FriendshipClickListener?) {
        this.requestClickListener = requestClickListener
        this.friendshipClickListener = friendshipClickListener
        if (requestClickListener != null || friendshipClickListener != null) {
            nameView.twoLine = true
            actionsProgressContainer.visibility = View.VISIBLE
        } else {
            nameView.twoLine = false
            actionsProgressContainer.visibility = View.GONE
        }
        nameView.updateText()
        acceptRequestButton.setOnClickListener(this)
        denyRequestButton.setOnClickListener(this)
        followButton.setOnClickListener(this)
        unblockButton.setOnClickListener(this)
        unmuteButton.setOnClickListener(this)
    }

    fun setTextSize(textSize: Float) {
        descriptionView.textSize = textSize
        externalIndicator.textSize = textSize
        nameView.setPrimaryTextSize(textSize)
        nameView.setSecondaryTextSize(textSize * 0.75f)
        locationView.textSize = textSize
        urlView.textSize = textSize
        statusesCountView.textSize = textSize
        followersCountView.textSize = textSize
        friendsCountView.textSize = textSize
    }

    fun setUserClickListener(listener: UserClickListener?) {
        userClickListener = listener
        itemContent.setOnClickListener(this)
        itemContent.setOnLongClickListener(this)
    }

    fun setupViewOptions() {
        profileImageView.style = adapter.profileImageStyle
        setTextSize(adapter.textSize)
    }

    private fun RelativeLayout.LayoutParams.clearVerticalRules() {
        intArrayOf(RelativeLayout.ABOVE, RelativeLayout.BELOW, RelativeLayout.ALIGN_BASELINE,
                RelativeLayout.ALIGN_TOP, RelativeLayout.ALIGN_BOTTOM).forEach { verb ->
            addRule(verb, 0)
        }
    }

}
