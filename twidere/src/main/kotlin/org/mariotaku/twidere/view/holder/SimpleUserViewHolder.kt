package org.mariotaku.twidere.view.holder

import androidx.recyclerview.widget.RecyclerView
import android.view.View
import android.widget.CheckBox
import android.widget.TextView
import org.mariotaku.ktextension.spannable
import org.mariotaku.twidere.R
import org.mariotaku.twidere.adapter.iface.IContentAdapter
import org.mariotaku.twidere.extension.loadProfileImage
import org.mariotaku.twidere.model.ParcelableUser
import org.mariotaku.twidere.view.ProfileImageView
import org.mariotaku.twidere.databinding.ListItemSimpleUserBinding

/**
 * Created by mariotaku on 2016/12/1.
 */

open class SimpleUserViewHolder<out A : IContentAdapter>(
        private val binding: ListItemSimpleUserBinding?,
        private val itemView: View?,
        val adapter: A
) : RecyclerView.ViewHolder(binding?.root ?: itemView!!) {

    private val rootView: View = binding?.root ?: itemView!!
    
    val nameView: TextView = binding?.name ?: rootView.findViewById(R.id.name)
    val secondaryNameView: TextView = binding?.screenName ?: rootView.findViewById(R.id.screenName)
    val profileImageView: ProfileImageView = binding?.profileImage ?: rootView.findViewById(R.id.profileImage)
    val checkBox: CheckBox = binding?.checkBox ?: rootView.findViewById(R.id.checkBox)

    init {
        profileImageView.style = adapter.profileImageStyle
    }

    open fun displayUser(user: ParcelableUser) {
        nameView.spannable = user.name
        secondaryNameView.spannable = "@${user.screen_name}"
        if (adapter.profileImageEnabled) {
            val context = rootView.context
            adapter.requestManager.loadProfileImage(context, user, adapter.profileImageStyle,
                    profileImageView.cornerRadius, profileImageView.cornerRadiusRatio,
                    adapter.profileImageSize).into(profileImageView)
            profileImageView.visibility = View.VISIBLE
        } else {
            profileImageView.visibility = View.GONE
        }
    }

    companion object {
        const val layoutResource = R.layout.list_item_simple_user
        
        @Deprecated("Use create() method with View Binding", ReplaceWith("create(parent, adapter)"))
        fun <A : IContentAdapter> fromView(itemView: View, adapter: A): SimpleUserViewHolder<A> {
            return SimpleUserViewHolder(null, itemView, adapter)
        }
        
        fun <A : IContentAdapter> create(parent: android.view.ViewGroup, adapter: A): SimpleUserViewHolder<A> {
            val binding = ListItemSimpleUserBinding.inflate(
                android.view.LayoutInflater.from(parent.context), parent, false
            )
            return SimpleUserViewHolder(binding, null, adapter)
        }
    }
}
