/*
 * Twidere - Twitter client for Android
 *
 *  Copyright (C) 2012-2014 Mariotaku Lee <mariotaku.lee@gmail.com>
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

import androidx.recyclerview.widget.RecyclerView
import android.view.View
import android.view.View.OnClickListener
import org.mariotaku.twidere.R
import org.mariotaku.twidere.adapter.iface.IGapSupportedAdapter
import org.mariotaku.twidere.databinding.CardItemGapBinding

/**
 * Created by mariotaku on 14/12/3.
 */
class GapViewHolder(
        private val adapter: IGapSupportedAdapter,
        private val binding: CardItemGapBinding
) : RecyclerView.ViewHolder(binding.root), OnClickListener {

    init {
        binding.root.setOnClickListener(this)
    }

    override fun onClick(v: View) {
        adapter.gapClickListener?.onGapClick(this, layoutPosition)
        display(true)
    }

    fun display(showProgress: Boolean) {
        if (showProgress) {
            binding.gapText.visibility = View.INVISIBLE
            binding.gapProgress.visibility = View.VISIBLE
            binding.gapProgress.spin()
        } else {
            binding.gapText.visibility = View.VISIBLE
            binding.gapProgress.visibility = View.INVISIBLE
        }
    }

    companion object {
        const val layoutResource = R.layout.card_item_gap
        
        fun create(adapter: IGapSupportedAdapter, parent: android.view.ViewGroup): GapViewHolder {
            val binding = CardItemGapBinding.inflate(
                android.view.LayoutInflater.from(parent.context), parent, false
            )
            return GapViewHolder(adapter, binding)
        }
        
        fun create(adapter: IGapSupportedAdapter, view: View): GapViewHolder {
            val binding = CardItemGapBinding.bind(view)
            return GapViewHolder(adapter, binding)
        }
        
        @Deprecated("Use create() method with View Binding", ReplaceWith("create(adapter, view)"))
        fun fromView(itemView: View, adapter: IGapSupportedAdapter): GapViewHolder {
            val binding = CardItemGapBinding.bind(itemView)
            return GapViewHolder(adapter, binding)
        }
    }
}
