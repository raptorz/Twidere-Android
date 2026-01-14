package org.mariotaku.twidere.view.holder

import androidx.recyclerview.widget.RecyclerView
import android.view.View
import android.view.ViewGroup
import android.view.LayoutInflater
import android.widget.TextView
import org.mariotaku.ktextension.spannable
import org.mariotaku.twidere.R
import org.mariotaku.twidere.adapter.iface.IUserListsAdapter
import org.mariotaku.twidere.databinding.ListItemSimpleUserListBinding
import org.mariotaku.twidere.extension.loadProfileImage
import org.mariotaku.twidere.model.ParcelableUserList
import org.mariotaku.twidere.view.ProfileImageView

/**
 * Created by mariotaku on 2016/12/1.
 */

class SimpleUserListViewHolder private constructor(
    private val binding: ListItemSimpleUserListBinding,
    val adapter: IUserListsAdapter<*>
) : RecyclerView.ViewHolder(binding.root) {

    val createdByView: TextView = binding.createdBy
    val nameView: TextView = binding.name
    val profileImageView: ProfileImageView = binding.profileImage

    init {
        profileImageView.style = adapter.profileImageStyle
    }

    constructor(adapter: IUserListsAdapter<*>, itemView: View) : this(
        ListItemSimpleUserListBinding.bind(itemView), adapter
    )

    companion object {
        const val layoutResource = R.layout.list_item_simple_user_list

        fun create(parent: ViewGroup, adapter: IUserListsAdapter<*>): SimpleUserListViewHolder {
            val binding = ListItemSimpleUserListBinding.inflate(
                LayoutInflater.from(parent.context), parent, false
            )
            return SimpleUserListViewHolder(binding, adapter)
        }
    }

    fun display(userList: ParcelableUserList) {
        nameView.spannable = userList.name
        createdByView.spannable = createdByView.context.getString(R.string.created_by,
                adapter.userColorNameManager.getDisplayName(userList, false))
        if (adapter.profileImageEnabled) {
            profileImageView.visibility = View.VISIBLE
            val context = itemView.context
            adapter.requestManager.loadProfileImage(context, userList, adapter.profileImageStyle,
                    profileImageView.cornerRadius, profileImageView.cornerRadiusRatio).into(profileImageView)
        } else {
            profileImageView.visibility = View.GONE
        }
    }
}
