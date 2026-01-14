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

package org.mariotaku.twidere.fragment

import android.app.Dialog
import android.os.Bundle
import androidx.fragment.app.FragmentManager
import androidx.appcompat.app.AlertDialog
import org.mariotaku.ktextension.Bundle
import org.mariotaku.ktextension.set
import org.mariotaku.ktextension.string
import org.mariotaku.twidere.Constants
import org.mariotaku.twidere.R
import org.mariotaku.twidere.constant.IntentConstants.EXTRA_POSITION
import org.mariotaku.twidere.constant.IntentConstants.EXTRA_TEXT
import org.mariotaku.twidere.extension.applyOnShow
import org.mariotaku.twidere.extension.applyTheme
import org.mariotaku.twidere.databinding.DialogComposeEditAltTextBinding

class EditAltTextDialogFragment : BaseDialogFragment() {
    private var binding: DialogComposeEditAltTextBinding? = null

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val builder = AlertDialog.Builder(requireContext())
        builder.setTitle(R.string.edit_description)
        val position = requireArguments().getInt(EXTRA_POSITION)
        builder.setPositiveButton(android.R.string.ok) { dialog, _ ->
            val altText = binding?.editText?.string
            callback?.onSetAltText(position, altText)
        }
        builder.setNegativeButton(android.R.string.cancel, null)
        builder.setNeutralButton(R.string.action_clear) { _, _ ->
            callback?.onSetAltText(position, null)
        }
        binding = DialogComposeEditAltTextBinding.inflate(layoutInflater)
        builder.setView(binding?.root)
        val dialog = builder.create()
        dialog.applyOnShow {
            applyTheme()
            binding?.editText?.setText(requireArguments().getString(EXTRA_TEXT))
        }
        return dialog
    }

    override fun onDestroyView() {
        binding = null
        super.onDestroyView()
    }

    private val callback: EditAltTextCallback?
        get() = targetFragment as? EditAltTextCallback ?: parentFragment as? EditAltTextCallback ?: context as? EditAltTextCallback

    interface EditAltTextCallback {
        fun onSetAltText(position: Int, altText: String?)
    }

    companion object {
        fun show(fm: FragmentManager, position: Int, altText: String?) {
            val df = EditAltTextDialogFragment()
            df.arguments = Bundle {
                this[Constants.EXTRA_TEXT] = altText
                this[Constants.EXTRA_POSITION] = position
            }
            df.show(fm, "edit_alt_text")
        }
    }
}