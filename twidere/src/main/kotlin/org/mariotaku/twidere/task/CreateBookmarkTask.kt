package org.mariotaku.twidere.task

import android.content.Context
import android.widget.Toast
import org.mariotaku.microblog.library.MicroBlogException
import org.mariotaku.microblog.library.mastodon.Mastodon
import org.mariotaku.twidere.R
import org.mariotaku.twidere.annotation.AccountType
import org.mariotaku.twidere.extension.getErrorMessage
import org.mariotaku.twidere.extension.model.api.mastodon.toParcelable
import org.mariotaku.twidere.extension.model.newMicroBlogInstance
import org.mariotaku.twidere.model.AccountDetails
import org.mariotaku.twidere.model.Draft
import org.mariotaku.twidere.model.ParcelableStatus
import org.mariotaku.twidere.model.UserKey
import org.mariotaku.twidere.model.draft.StatusObjectActionExtras
import org.mariotaku.twidere.model.event.StatusListChangedEvent
import org.mariotaku.twidere.task.twitter.UpdateStatusTask
import org.mariotaku.twidere.util.DataStoreUtils
import org.mariotaku.twidere.util.updateStatusInfo

class CreateBookmarkTask(context: Context, accountKey: UserKey, private val status: ParcelableStatus) :
        AbsAccountRequestTask<Any?, ParcelableStatus, Any?>(context, accountKey) {

    private val statusId = status.id

    override fun onExecute(account: AccountDetails, params: Any?): ParcelableStatus {
        val resolver = context.contentResolver
        val result = when (account.type) {
            AccountType.MASTODON -> {
                val mastodon = account.newMicroBlogInstance(context, cls = Mastodon::class.java)
                mastodon.bookmarkStatus(statusId).toParcelable(account)
            }
            else -> {
                throw MicroBlogException("Bookmark not supported for this account type")
            }
        }

        resolver.updateStatusInfo(DataStoreUtils.STATUSES_ACTIVITIES_URIS, null,
                account.key, statusId, ParcelableStatus::class.java) { status ->
            if (result.id != status.id) return@updateStatusInfo status
            status.is_bookmark = true
            return@updateStatusInfo status
        }
        return result
    }

    override fun beforeExecute() {
        bus.post(StatusListChangedEvent())
    }

    override fun afterExecute(callback: Any?, result: ParcelableStatus?, exception: MicroBlogException?) {
        if (result != null) {
            Toast.makeText(context, R.string.message_toast_status_bookmarked, Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(context, exception?.getErrorMessage(context), Toast.LENGTH_SHORT).show()
        }
        bus.post(StatusListChangedEvent())
    }

    override fun createDraft() = UpdateStatusTask.createDraft(Draft.Action.BOOKMARK) {
        account_keys = arrayOf(accountKey)
        action_extras = StatusObjectActionExtras().also { extras ->
            extras.status = this@CreateBookmarkTask.status
        }
    }
}
