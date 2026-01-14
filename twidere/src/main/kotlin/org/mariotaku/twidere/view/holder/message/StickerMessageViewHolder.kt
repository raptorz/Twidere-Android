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
import android.widget.RelativeLayout
import org.mariotaku.twidere.R
import org.mariotaku.twidere.adapter.MessagesConversationAdapter
import org.mariotaku.twidere.databinding.ListItemMessageConversationStickerBinding
import org.mariotaku.twidere.model.ParcelableMessage
import org.mariotaku.twidere.model.message.StickerExtras
import org.mariotaku.twidere.view.FixedTextView
import org.mariotaku.twidere.view.ProfileImageView

/**
 * Created by mariotaku on 2017/2/9.
 */

class StickerMessageViewHolder private constructor(
    private val binding: ListItemMessageConversationStickerBinding,
    adapter: MessagesConversationAdapter
) : AbsMessageViewHolder(binding.root, adapter) {

    constructor(adapter: MessagesConversationAdapter, itemView: View) : this(
        ListItemMessageConversationStickerBinding.bind(itemView), adapter
    )

    override val date: FixedTextView get() = binding.date
    override val messageContent: RelativeLayout get() = binding.messageContent
    override val profileImage: ProfileImageView get() = binding.profileImage
    override val nameTime: FixedTextView get() = binding.nameTime

    private val stickerIcon get() = binding.stickerIcon

    override fun display(message: ParcelableMessage, showDate: Boolean) {
        super.display(message, showDate)
        val extras = message.extras as StickerExtras
        adapter.requestManager.load(extras.url).into(stickerIcon)
        stickerIcon.contentDescription = extras.displayName
    }

    companion object {
        const val layoutResource = R.layout.list_item_message_conversation_sticker

        fun create(parent: ViewGroup, adapter: MessagesConversationAdapter): StickerMessageViewHolder {
            val binding = ListItemMessageConversationStickerBinding.inflate(
                LayoutInflater.from(parent.context), parent, false
            )
            return StickerMessageViewHolder(binding, adapter)
        }
    }
}
