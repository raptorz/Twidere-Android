/*
 *             Twidere - Twitter client for Android
 *
 *  Copyright (C) 2012-2017 Mariotaku Lee <mariotaku.lee@gmail.com>
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

package org.mariotaku.twidere.view.holder.message

import androidx.recyclerview.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import org.mariotaku.ktextension.spannable
import org.mariotaku.twidere.R
import org.mariotaku.twidere.adapter.MessagesEntriesAdapter
import org.mariotaku.twidere.databinding.ListItemMessageEntryBinding
import org.mariotaku.twidere.extension.loadProfileImage
import org.mariotaku.twidere.extension.model.getSummaryText
import org.mariotaku.twidere.extension.model.getTitle
import org.mariotaku.twidere.extension.model.notificationDisabled
import org.mariotaku.twidere.extension.model.timestamp
import org.mariotaku.twidere.model.ParcelableMessageConversation
import org.mariotaku.twidere.model.ParcelableMessageConversation.ConversationType

/**
 * Created by mariotaku on 2017/2/9.
 */

class MessageEntryViewHolder private constructor(
    private val binding: ListItemMessageEntryBinding,
    val adapter: MessagesEntriesAdapter
) : RecyclerView.ViewHolder(binding.root) {

    constructor(adapter: MessagesEntriesAdapter, itemView: View) : this(
        ListItemMessageEntryBinding.bind(itemView), adapter
    )

    init {
        val textSize = adapter.textSize
        binding.name.setPrimaryTextSize(textSize * 1.05f)
        binding.name.setSecondaryTextSize(textSize * 0.95f)
        binding.text.textSize = textSize
        binding.time.textSize = textSize * 0.85f

        binding.profileImage.style = adapter.profileImageStyle

        binding.root.setOnClickListener {
            adapter.listener?.onConversationClick(layoutPosition)
        }
        binding.root.setOnLongClickListener {
            adapter.listener?.onConversationLongClick(layoutPosition) ?: false
        }
        binding.profileImage.setOnClickListener {
            adapter.listener?.onProfileImageClick(layoutPosition)
        }
    }

    fun display(conversation: ParcelableMessageConversation) {
        if (adapter.drawAccountColors) {
            binding.content.drawEnd(conversation.account_color)
        } else {
            binding.content.drawEnd()
        }
        val (name, secondaryName) = conversation.getTitle(binding.root.context,
                adapter.userColorNameManager, adapter.nameFirst)
        binding.time.time = conversation.timestamp
        binding.name.name = name
        binding.name.screenName = secondaryName
        binding.name.updateText(adapter.bidiFormatter)
        binding.text.spannable = conversation.getSummaryText(binding.root.context,
                adapter.userColorNameManager, adapter.nameFirst)
        if (conversation.is_outgoing) {
            binding.readIndicator.visibility = View.VISIBLE
            binding.readIndicator.setImageResource(R.drawable.ic_message_type_outgoing)
        } else {
            binding.readIndicator.visibility = View.GONE
        }
        if (conversation.conversation_type == ConversationType.ONE_TO_ONE) {
            binding.typeIndicator.visibility = View.GONE
        } else {
            binding.typeIndicator.visibility = View.VISIBLE
        }
        if (conversation.notificationDisabled) {
            binding.stateIndicator.visibility = View.VISIBLE
            binding.stateIndicator.setImageResource(R.drawable.ic_message_type_speaker_muted)
        } else {
            binding.stateIndicator.visibility = View.GONE
        }
        adapter.requestManager.loadProfileImage(adapter.context, conversation,
                adapter.profileImageStyle, binding.profileImage.cornerRadius,
                binding.profileImage.cornerRadiusRatio).into(binding.profileImage)
        if (conversation.unread_count > 0) {
            binding.unreadCount.visibility = View.VISIBLE
            binding.unreadCount.text = conversation.unread_count.toString()
        } else {
            binding.unreadCount.visibility = View.GONE
        }
    }

    companion object {
        const val layoutResource = R.layout.list_item_message_entry

        fun create(parent: ViewGroup, adapter: MessagesEntriesAdapter): MessageEntryViewHolder {
            val binding = ListItemMessageEntryBinding.inflate(
                LayoutInflater.from(parent.context), parent, false
            )
            return MessageEntryViewHolder(binding, adapter)
        }
    }

}

