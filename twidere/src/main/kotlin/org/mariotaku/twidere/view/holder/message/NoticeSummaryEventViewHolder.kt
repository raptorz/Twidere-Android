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

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import org.mariotaku.ktextension.spannable
import org.mariotaku.twidere.R
import org.mariotaku.twidere.adapter.MessagesConversationAdapter
import org.mariotaku.twidere.databinding.ListItemMessageConversationNoticeBinding
import org.mariotaku.twidere.extension.model.getSummaryText
import org.mariotaku.twidere.model.ParcelableMessage
import org.mariotaku.twidere.view.FixedTextView

class NoticeSummaryEventViewHolder private constructor(
    private val binding: ListItemMessageConversationNoticeBinding,
    adapter: MessagesConversationAdapter
) : AbsMessageViewHolder(binding.root, adapter) {

    constructor(adapter: MessagesConversationAdapter, itemView: View) : this(
        ListItemMessageConversationNoticeBinding.bind(itemView), adapter
    )

    override val messageContent: View get() = binding.root
    override val date: FixedTextView get() = binding.date

    private val text get() = binding.text

    override fun display(message: ParcelableMessage, showDate: Boolean) {
        super.display(message, showDate)
        text.spannable = message.getSummaryText(adapter.context, adapter.userColorNameManager,
                adapter.conversation, adapter.nameFirst)
    }

    override fun setMessageContentGravity(view: View, outgoing: Boolean) {
        // No-op
    }

    companion object {
        const val layoutResource = R.layout.list_item_message_conversation_notice

        fun create(parent: ViewGroup, adapter: MessagesConversationAdapter): NoticeSummaryEventViewHolder {
            val binding = ListItemMessageConversationNoticeBinding.inflate(
                LayoutInflater.from(parent.context), parent, false
            )
            return NoticeSummaryEventViewHolder(binding, adapter)
        }
    }
}