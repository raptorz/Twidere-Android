/*
 * 				Twidere - Twitter client for Android
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

package org.mariotaku.twidere.fragment

import android.app.Dialog
import android.content.Context
import android.content.DialogInterface
import android.content.SharedPreferences
import android.content.SharedPreferences.OnSharedPreferenceChangeListener
import android.os.Bundle
import androidx.appcompat.app.AlertDialog
import android.text.Editable
import android.text.TextWatcher
import android.view.*
import android.widget.AbsListView.MultiChoiceModeListener
import android.widget.AdapterView
import android.widget.CheckBox
import android.widget.CompoundButton
import android.widget.CompoundButton.OnCheckedChangeListener
import android.widget.EditText
import android.widget.ListView
import android.widget.TextView
import com.bumptech.glide.RequestManager
import org.mariotaku.ktextension.empty
import org.mariotaku.ktextension.string
import org.mariotaku.twidere.R
import org.mariotaku.twidere.TwidereConstants.HOST_MAPPING_PREFERENCES_NAME
import org.mariotaku.twidere.adapter.ArrayAdapter
import org.mariotaku.twidere.extension.applyOnShow
import org.mariotaku.twidere.extension.applyTheme
import org.mariotaku.twidere.extension.positive
import org.mariotaku.twidere.util.net.TwidereDns

class HostMappingsListFragment : AbsContentListViewFragment<HostMappingsListFragment.HostMappingAdapter>(),
        AdapterView.OnItemClickListener, MultiChoiceModeListener, OnSharedPreferenceChangeListener {

    private lateinit var hostMapping: SharedPreferences

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        setHasOptionsMenu(true)
        hostMapping = requireActivity().getSharedPreferences(HOST_MAPPING_PREFERENCES_NAME, Context.MODE_PRIVATE)
        hostMapping.registerOnSharedPreferenceChangeListener(this)
        binding.listView.choiceMode = ListView.CHOICE_MODE_MULTIPLE_MODAL
        binding.listView.setMultiChoiceModeListener(this)
        reloadHostMappings()
    }

    override fun onCreateAdapter(context: Context, requestManager: RequestManager): HostMappingAdapter {
        return HostMappingAdapter(requireActivity())
    }

    override fun onCreateActionMode(mode: ActionMode, menu: Menu): Boolean {
        mode.menuInflater.inflate(R.menu.action_multi_select_items, menu)
        return true
    }

    override fun onPrepareActionMode(mode: ActionMode, menu: Menu): Boolean {
        updateTitle(mode)
        return true
    }

    override fun onActionItemClicked(mode: ActionMode, item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.delete -> {
                val array = binding.listView.checkedItemPositions ?: return false
                (dns as? TwidereDns)?.beginMappingTransaction {
                    (0 until array.size()).filter {
                        array.valueAt(it)
                    }.forEach {
                        remove(adapter.getItem(it).first)
                    }
                }
                reloadHostMappings()
            }
            else -> {
                return false
            }
        }
        mode.finish()
        return true
    }

    override fun onDestroyActionMode(mode: ActionMode) {

    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.menu_host_mapping, menu)
    }

    override fun onItemClick(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
        val (host, address) = adapter.getItem(position)
        val args = Bundle()
        args.putString(EXTRA_HOST, host)
        args.putString(EXTRA_ADDRESS, address)
        args.putBoolean(EXTRA_EXCLUDED, host == address)
        args.putBoolean(EXTRA_EDIT_MODE, true)
        val df = AddMappingDialogFragment()
        df.arguments = args
        parentFragmentManager.let { df.show(it, "add_mapping") }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.add -> {
                val df = AddMappingDialogFragment()
                parentFragmentManager.let { df.show(it, "add_mapping") }
            }
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onItemCheckedStateChanged(mode: ActionMode, position: Int, id: Long,
            checked: Boolean) {
        updateTitle(mode)
    }

    override fun onSharedPreferenceChanged(preferences: SharedPreferences, key: String) {
        reloadHostMappings()
    }

    fun reloadHostMappings() {
        adapter.clear()
        adapter.addAll(hostMapping.all.mapNotNull { entry ->
            val value = entry.value?.toString() ?: return@mapNotNull null
            return@mapNotNull Pair(entry.key, value)
        })
        if (adapter.isEmpty) {
            showEmpty(R.drawable.ic_info_info_generic, getString(R.string.add_host_mapping))
        } else {
            showContent()
        }
    }

    private fun updateTitle(mode: ActionMode?) {
        if (binding.listView == null || mode == null || activity == null) return
        val count = binding.listView.checkedItemCount
        mode.title = resources.getQuantityString(R.plurals.Nitems_selected, count, count)
    }

    class AddMappingDialogFragment : BaseDialogFragment(), TextWatcher, OnCheckedChangeListener {


        override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {

        }

        override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {
            updateButton()
        }

        override fun afterTextChanged(s: Editable) {

        }

        override fun onCheckedChanged(buttonView: CompoundButton, isChecked: Boolean) {
            updateAddressField()
            updateButton()
        }

        override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
            val context = activity
            val builder = AlertDialog.Builder(requireContext())
            builder.setView(R.layout.dialog_add_host_mapping)
            builder.setTitle(R.string.add_host_mapping)
            builder.positive(android.R.string.ok, this::onPositiveClick)
            builder.setNegativeButton(android.R.string.cancel, null)
            val dialog = builder.create()
            dialog.applyOnShow {
                applyTheme()
                val editHostView = dialog.findViewById<EditText>(R.id.editHost)
                val editAddressView = dialog.findViewById<EditText>(R.id.editAddress)
                val isExcludedView = dialog.findViewById<CheckBox>(R.id.isExcluded)
                editHostView?.addTextChangedListener(this@AddMappingDialogFragment)
                editAddressView?.addTextChangedListener(this@AddMappingDialogFragment)
                isExcludedView?.setOnCheckedChangeListener(this@AddMappingDialogFragment)
                val args = arguments
                if (args != null) {
                    editHostView?.isEnabled = !args.getBoolean(EXTRA_EDIT_MODE, false)
                    if (savedInstanceState == null) {
                        editHostView?.setText(args.getCharSequence(EXTRA_HOST))
                        editAddressView?.setText(args.getCharSequence(EXTRA_ADDRESS))
                        isExcludedView?.isChecked = args.getBoolean(EXTRA_EXCLUDED)
                    }
                }
                updateButton()
            }
            return dialog
        }

        private fun onPositiveClick(dialog: Dialog) {
            val alertDialog = dialog as AlertDialog
            val editHostView = alertDialog.findViewById<EditText>(R.id.editHost)
            val editAddressView = alertDialog.findViewById<EditText>(R.id.editAddress)
            val isExcludedView = alertDialog.findViewById<CheckBox>(R.id.isExcluded)
            val host = editHostView?.string.takeUnless(String?::isNullOrEmpty) ?: return
            val address = (if (isExcludedView?.isChecked == true) {
                host
            } else {
                editAddressView?.string
            }).takeUnless(String?::isNullOrEmpty) ?: return
            (dns as? TwidereDns)?.putMapping(host, address)
        }

        override fun onSaveInstanceState(outState: Bundle) {
            (dialog as? AlertDialog)?.let { alertDialog ->
                val editHostView = alertDialog.findViewById<EditText>(R.id.editHost)
                val editAddressView = alertDialog.findViewById<EditText>(R.id.editAddress)
                val isExcludedView = alertDialog.findViewById<CheckBox>(R.id.isExcluded)
                outState.putCharSequence(EXTRA_HOST, editHostView?.text)
                outState.putCharSequence(EXTRA_ADDRESS, editAddressView?.text)
                outState.putCharSequence(EXTRA_EXCLUDED, if (isExcludedView?.isChecked == true) "true" else "false")
            }
            super.onSaveInstanceState(outState)
        }

        private fun updateAddressField() {
            val alertDialog = dialog as AlertDialog
            val editAddressView = alertDialog.findViewById<EditText>(R.id.editAddress)
            val isExcludedView = alertDialog.findViewById<CheckBox>(R.id.isExcluded)
            editAddressView?.visibility = if (isExcludedView?.isChecked == true) View.GONE else View.VISIBLE
        }

        private fun updateButton() {
            val alertDialog = dialog as AlertDialog
            val editHostView = alertDialog.findViewById<EditText>(R.id.editHost)
            val editAddressView = alertDialog.findViewById<EditText>(R.id.editAddress)
            val isExcludedView = alertDialog.findViewById<CheckBox>(R.id.isExcluded)
            val hostValid = editHostView?.empty != true
            val addressValid = editAddressView?.empty != true || (isExcludedView?.isChecked == true)
            val positiveButton = alertDialog.getButton(DialogInterface.BUTTON_POSITIVE)
            positiveButton.isEnabled = hostValid && addressValid
        }
    }

    class HostMappingAdapter(context: Context) : ArrayAdapter<Pair<String, String>>(context,
            android.R.layout.simple_list_item_activated_2) {

        override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
            val view = super.getView(position, convertView, parent)
            val text1 = view.findViewById<TextView>(android.R.id.text1)
            val text2 = view.findViewById<TextView>(android.R.id.text2)
            val (key, value) = getItem(position)
            text1.text = key
            if (key == value) {
                text2.setText(R.string.excluded)
            } else {
                text2.text = value
            }
            return view
        }
    }

    companion object {

        private const val EXTRA_EDIT_MODE = "edit_mode"
        private const val EXTRA_HOST = "host"
        private const val EXTRA_ADDRESS = "address"
        private const val EXTRA_EXCLUDED = "excluded"
    }

}
