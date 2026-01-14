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

package org.mariotaku.twidere.view.holder

import android.view.View
import android.widget.CompoundButton
import org.mariotaku.twidere.adapter.SelectableUsersAdapter
import org.mariotaku.twidere.databinding.ListItemSimpleUserBinding
import org.mariotaku.twidere.model.ParcelableUser
import org.mariotaku.twidere.util.ThemeUtils
import org.mariotaku.twidere.util.support.ViewSupport

class SelectableUserViewHolder(
        binding: ListItemSimpleUserBinding?,
        itemView: View?,
        adapter: SelectableUsersAdapter
) : SimpleUserViewHolder<SelectableUsersAdapter>(binding, itemView, adapter) {
    
    constructor(itemView: View, adapter: SelectableUsersAdapter) : this(null, itemView, adapter)
    constructor(binding: ListItemSimpleUserBinding, adapter: SelectableUsersAdapter) : this(binding, null, adapter)
    private val checkChangedListener = CompoundButton.OnCheckedChangeListener { _, value ->
        adapter.setItemChecked(layoutPosition, value)
    }

    init {
        val view = binding?.root ?: itemView!!
        ViewSupport.setBackground(view, ThemeUtils.getSelectableItemBackgroundDrawable(view.context))
        checkBox.visibility = View.VISIBLE
        view.setOnClickListener {
            checkBox.toggle()
        }
    }

    override fun displayUser(user: ParcelableUser) {
        super.displayUser(user)
        checkBox.setOnCheckedChangeListener(null)
        val key = adapter.getUserKey(layoutPosition)
        val locked = adapter.isItemLocked(key)
        if (locked) {
            itemView.isEnabled = false
            checkBox.isEnabled = false
            checkBox.isChecked = adapter.getLockedState(key)
        } else {
            itemView.isEnabled = true
            checkBox.isEnabled = true
            checkBox.isChecked = adapter.isItemChecked(key)
        }
        checkBox.setOnCheckedChangeListener(checkChangedListener)
    }

}