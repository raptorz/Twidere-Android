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

import androidx.recyclerview.widget.RecyclerView
import android.view.View
import org.mariotaku.twidere.databinding.ListItemLoadIndicatorBinding
import org.mariotaku.twidere.R

/**
 * Created by mariotaku on 14/11/19.
 */
class LoadIndicatorViewHolder private constructor(
    private val binding: ListItemLoadIndicatorBinding
) : RecyclerView.ViewHolder(binding.root) {
    private val loadProgress by lazy { binding.loadProgress }

    fun setLoadProgressVisible(visible: Boolean) {
        loadProgress.visibility = if (visible) View.VISIBLE else View.GONE
    }

    @Deprecated("Use create() factory method instead", ReplaceWith("LoadIndicatorViewHolder.create(view)"))
    constructor(view: View) : this(ListItemLoadIndicatorBinding.bind(view))

    companion object {
        const val layoutResource = R.layout.list_item_load_indicator
        
        fun create(view: View): LoadIndicatorViewHolder {
            return LoadIndicatorViewHolder(ListItemLoadIndicatorBinding.bind(view))
        }
    }
}
