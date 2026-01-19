/*
 * Twidere - Twitter client for Android
 *
 *  Copyright (C) 2012-2015 Mariotaku Lee <mariotaku.lee@gmail.com>
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

import android.content.Context
import android.graphics.Rect
import android.graphics.drawable.Drawable
import android.os.Bundle
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout.OnRefreshListener
import android.view.*
import android.widget.AbsListView
import android.widget.ImageView
import android.widget.ListAdapter
import android.widget.TextView
import com.bumptech.glide.RequestManager
import org.mariotaku.twidere.R
import org.mariotaku.twidere.databinding.FragmentContentListviewBinding
import org.mariotaku.twidere.databinding.LayoutContentFragmentCommonBinding
import org.mariotaku.twidere.activity.iface.IControlBarActivity
import org.mariotaku.twidere.activity.iface.IControlBarActivity.ControlBarOffsetListener
import org.mariotaku.twidere.adapter.iface.ILoadMoreSupportAdapter
import org.mariotaku.twidere.fragment.iface.RefreshScrollTopInterface
import org.mariotaku.twidere.util.ContentScrollHandler.ContentListSupport
import org.mariotaku.twidere.util.ListViewScrollHandler
import org.mariotaku.twidere.util.ThemeUtils
import org.mariotaku.twidere.util.TwidereColorUtils
import kotlin.math.roundToInt

/**
 * Created by mariotaku on 15/4/16.
 */
abstract class AbsContentListViewFragment<A : ListAdapter> : BaseFragment(),
        OnRefreshListener, RefreshScrollTopInterface, ControlBarOffsetListener, ContentListSupport<A>,
        AbsListView.OnScrollListener {

    protected lateinit var binding: FragmentContentListviewBinding
        private set

    protected val progressContainer by lazy { binding.root.findViewById<View>(R.id.progressContainer) }
    protected val errorContainer by lazy { binding.root.findViewById<View>(R.id.errorContainer) }
    protected val errorIcon by lazy { binding.root.findViewById<ImageView>(R.id.errorIcon) }
    protected val errorText by lazy { binding.root.findViewById<TextView>(R.id.errorText) }

    override lateinit var adapter: A

    var refreshEnabled: Boolean
        get() = binding.swipeLayout.isEnabled
        set(value) {
            binding.swipeLayout.isEnabled = value
        }

    protected open val overrideDivider: Drawable?
        get() = context?.let {
            ThemeUtils.getDrawableFromThemeAttribute(it, android.R.attr.listDivider)
        }

    protected val isProgressShowing: Boolean
        get() = progressContainer.visibility == View.VISIBLE

    // Data fields
    private val systemWindowsInsets = Rect()

    private lateinit var scrollHandler: ListViewScrollHandler<A>

    override fun onControlBarOffsetChanged(activity: IControlBarActivity, offset: Float) {
        updateRefreshProgressOffset()
    }

    override fun onRefresh() {
        triggerRefresh()
    }

    override fun setUserVisibleHint(isVisibleToUser: Boolean) {
        super.setUserVisibleHint(isVisibleToUser)
        updateRefreshProgressOffset()
    }

    override fun scrollToStart(): Boolean {
        binding.listView.setSelectionFromTop(0, 0)
        setControlVisible(true)
        return true
    }

    override fun setControlVisible(visible: Boolean) {
        val activity = activity
        if (activity is IControlBarActivity) {
            activity.setControlBarVisibleAnimate(visible)
        }
    }

    override var refreshing: Boolean
        get() = false
        set(refreshing) {
            val currentRefreshing = binding.swipeLayout.isRefreshing
            if (!currentRefreshing) {
                updateRefreshProgressOffset()
            }
            if (refreshing == currentRefreshing) return
            binding.swipeLayout.isRefreshing = refreshing
        }

    override fun onLoadMoreContents(@ILoadMoreSupportAdapter.IndicatorPosition position: Long) {
        refreshEnabled = false
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        if (context is IControlBarActivity) {
            context.registerControlBarOffsetListener(this)
        }
    }

    override fun onDetach() {
        val activity = activity
        if (activity is IControlBarActivity) {
            activity.unregisterControlBarOffsetListener(this)
        }
        super.onDetach()
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        binding = FragmentContentListviewBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        val backgroundColor = ThemeUtils.getColorBackground(requireContext())
        val colorRes = TwidereColorUtils.getContrastYIQ(backgroundColor,
                R.color.bg_refresh_progress_color_light, R.color.bg_refresh_progress_color_dark)
        binding.swipeLayout.setOnRefreshListener(this)
        binding.swipeLayout.setProgressBackgroundColorSchemeResource(colorRes)
        adapter = onCreateAdapter(requireContext(), requestManager)
        binding.listView.setOnTouchListener { _, event ->
            if (event.actionMasked == MotionEvent.ACTION_DOWN) {
                updateRefreshProgressOffset()
            }
            false
        }
        binding.listView.adapter = adapter
        binding.listView.clipToPadding = false
        overrideDivider?.let { binding.listView.divider = it }
        scrollHandler = ListViewScrollHandler(this, ListViewScrollHandler.ListViewCallback(binding.listView)).apply {
            this.touchSlop = ViewConfiguration.get(context).scaledTouchSlop
            this.onScrollListener = this@AbsContentListViewFragment
        }
    }


    override fun onStart() {
        super.onStart()
        binding.listView.setOnScrollListener(scrollHandler)
    }

    override fun onStop() {
        binding.listView.setOnScrollListener(scrollHandler)
        super.onStop()
    }

    override fun onApplySystemWindowInsets(insets: Rect) {
        binding.listView.setPadding(insets.left, insets.top, insets.right, insets.bottom)
        errorContainer.setPadding(insets.left, insets.top, insets.right, insets.bottom)
        progressContainer.setPadding(insets.left, insets.top, insets.right, insets.bottom)
        systemWindowsInsets.set(insets)
        updateRefreshProgressOffset()
    }

    override fun triggerRefresh(): Boolean {
        return false
    }

    protected abstract fun onCreateAdapter(context: Context, requestManager: RequestManager): A

    protected fun showContent() {
        errorContainer.visibility = View.GONE
        progressContainer.visibility = View.GONE
        binding.swipeLayout.visibility = View.VISIBLE
    }

    protected fun showProgress() {
        errorContainer.visibility = View.GONE
        progressContainer.visibility = View.VISIBLE
        binding.swipeLayout.visibility = View.GONE
    }

    protected fun showError(icon: Int, text: CharSequence) {
        errorContainer.visibility = View.VISIBLE
        progressContainer.visibility = View.GONE
        binding.swipeLayout.visibility = View.GONE
        errorIcon.setImageResource(icon)
        errorText.text = text
    }

    protected fun showEmpty(icon: Int, text: CharSequence) {
        errorContainer.visibility = View.VISIBLE
        progressContainer.visibility = View.GONE
        binding.swipeLayout.visibility = View.VISIBLE
        errorIcon.setImageResource(icon)
        errorText.text = text
    }

    protected fun updateRefreshProgressOffset() {
        val activity = activity
        if (activity !is IControlBarActivity || systemWindowsInsets.top == 0 || binding.swipeLayout == null
                || refreshing) {
            return
        }
        val density = resources.displayMetrics.density
        val progressCircleDiameter = binding.swipeLayout.progressCircleDiameter
        val controlBarOffsetPixels =
            ((activity.controlBarHeight * (1 - activity.controlBarOffset)).takeIf { !it.isNaN() }
                ?: 0f)
                .roundToInt()
        val swipeStart = systemWindowsInsets.top - controlBarOffsetPixels - progressCircleDiameter
        // 64: SwipeRefreshLayout.DEFAULT_CIRCLE_TARGET
        val swipeDistance = (64 * density).roundToInt()
        binding.swipeLayout.setProgressViewOffset(false, swipeStart, swipeStart + swipeDistance)
    }

    override val reachingStart: Boolean
        get() = binding.listView.firstVisiblePosition <= 0

    override val reachingEnd: Boolean
        get() = binding.listView.lastVisiblePosition >= binding.listView.count - 1

    override fun onScrollStateChanged(view: AbsListView, scrollState: Int) {

    }

    override fun onScroll(view: AbsListView, firstVisibleItem: Int, visibleItemCount: Int, totalItemCount: Int) {

    }
}
