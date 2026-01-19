/*
 *                 Twidere - Twitter client for Android
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
import android.os.Bundle
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.ItemDecoration
import android.view.*
import android.widget.ImageView
import android.widget.TextView
import com.bumptech.glide.RequestManager
import org.mariotaku.twidere.R
import org.mariotaku.twidere.databinding.FragmentContentRecyclerviewBinding
import org.mariotaku.twidere.activity.iface.IControlBarActivity
import org.mariotaku.twidere.activity.iface.IControlBarActivity.ControlBarShowHideHelper
import org.mariotaku.twidere.adapter.LoadMoreSupportAdapter
import org.mariotaku.twidere.adapter.iface.ILoadMoreSupportAdapter
import org.mariotaku.twidere.adapter.iface.ILoadMoreSupportAdapter.IndicatorPosition
import org.mariotaku.twidere.fragment.iface.RefreshScrollTopInterface
import org.mariotaku.twidere.util.*
import org.mariotaku.twidere.view.ExtendedSwipeRefreshLayout
import org.mariotaku.twidere.view.HeaderDrawerLayout
import org.mariotaku.twidere.view.iface.IExtendedView

/**
 * Created by mariotaku on 15/10/26.
 */
abstract class AbsContentRecyclerViewFragment<A : LoadMoreSupportAdapter<RecyclerView.ViewHolder>,
        L : RecyclerView.LayoutManager> : BaseFragment(), SwipeRefreshLayout.OnRefreshListener,
        HeaderDrawerLayout.DrawerCallback, RefreshScrollTopInterface, IControlBarActivity.ControlBarOffsetListener,
        ContentScrollHandler.ContentListSupport<A>, ControlBarShowHideHelper.ControlBarAnimationListener {

    lateinit var layoutManager: L
        protected set
    override lateinit var adapter: A
        protected set
    var itemDecoration: ItemDecoration? = null
        private set

    protected lateinit var binding: FragmentContentRecyclerviewBinding
        private set

    protected val progressContainer by lazy { binding.root.findViewById<View>(R.id.progressContainer) }
    protected val errorContainer by lazy { binding.root.findViewById<View>(R.id.errorContainer) }
    protected val errorIcon by lazy { binding.root.findViewById<ImageView>(R.id.errorIcon) }
    protected val errorText by lazy { binding.root.findViewById<TextView>(R.id.errorText) }

    // Callbacks and listeners
    private lateinit var drawerCallback: SimpleDrawerCallback
    lateinit var scrollListener: RecyclerViewScrollHandler<A>
    // Data fields
    private val systemWindowsInsets = Rect()

    private val refreshCompleteListener: RefreshCompleteListener?
        get() = parentFragment as? RefreshCompleteListener

    val isProgressShowing: Boolean
        get() = progressContainer.visibility == View.VISIBLE

    override var refreshing: Boolean
        get () = binding.swipeLayout.isRefreshing
        set(value) {
            if (isProgressShowing) return
            val currentRefreshing = binding.swipeLayout.isRefreshing
            if (!currentRefreshing) {
                updateRefreshProgressOffset()
            }
            if (!value) {
                refreshCompleteListener?.onRefreshComplete(this)
            }
            if (value == currentRefreshing) return
            val layoutRefreshing = value && adapter.loadMoreIndicatorPosition != ILoadMoreSupportAdapter.NONE
            binding.swipeLayout.isRefreshing = layoutRefreshing
        }

    override fun canScroll(dy: Float): Boolean {
        return drawerCallback.canScroll(dy)
    }

    override fun cancelTouch() {
        drawerCallback.cancelTouch()
    }

    override fun fling(velocity: Float) {
        drawerCallback.fling(velocity)
    }

    override fun isScrollContent(x: Float, y: Float): Boolean {
        return drawerCallback.isScrollContent(x, y)
    }

    override fun onControlBarOffsetChanged(activity: IControlBarActivity, offset: Float) {
        updateRefreshProgressOffset()
    }

    final override fun onRefresh() {
        if (!triggerRefresh()) {
            refreshing = false
        }
    }

    override fun setUserVisibleHint(isVisibleToUser: Boolean) {
        super.setUserVisibleHint(isVisibleToUser)
        updateRefreshProgressOffset()
    }

    override fun scrollBy(dy: Float) {
        drawerCallback.scrollBy(dy)
    }

    override fun scrollToStart(): Boolean {
        scrollToPositionWithOffset(0, 0)
        binding.recyclerView.stopScroll()
        setControlVisible(true)
        return true
    }

    protected abstract fun scrollToPositionWithOffset(position: Int, offset: Int)

    override fun onControlBarVisibleAnimationFinish(visible: Boolean) {
        updateRefreshProgressOffset()
    }

    override fun setControlVisible(visible: Boolean) {
        val activity = activity
        if (activity is IControlBarActivity) {
            //TODO hide only if top > actionBar.height
            val manager = layoutManager
            if (manager.childCount == 0) return
            val firstView = manager.getChildAt(0) ?: return
            if (manager.getPosition(firstView) != 0) {
                activity.setControlBarVisibleAnimate(visible, this)
                return
            }
            val top = firstView.top
            activity.setControlBarVisibleAnimate(visible || top > 0, this)
        }
    }

    override fun shouldLayoutHeaderBottom(): Boolean {
        return drawerCallback.shouldLayoutHeaderBottom()
    }

    override fun topChanged(offset: Int) {
        drawerCallback.topChanged(offset)
    }

    var refreshEnabled: Boolean
        get() = binding.swipeLayout.isEnabled
        set(value) {
            binding.swipeLayout.isEnabled = value
        }

    override fun onLoadMoreContents(@IndicatorPosition position: Long) {
        setLoadMoreIndicatorPosition(position)
        refreshEnabled = position == ILoadMoreSupportAdapter.NONE
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        if (context is IControlBarActivity) {
            context.registerControlBarOffsetListener(this)
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        binding = FragmentContentRecyclerviewBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        drawerCallback = SimpleDrawerCallback(binding.recyclerView)

        val backgroundColor = ThemeUtils.getColorBackground(requireContext())
        val colorRes = TwidereColorUtils.getContrastYIQ(backgroundColor,
                R.color.bg_refresh_progress_color_light, R.color.bg_refresh_progress_color_dark)
        binding.swipeLayout.setOnRefreshListener(this)
        binding.swipeLayout.setProgressBackgroundColorSchemeResource(colorRes)
        adapter = onCreateAdapter(requireContext(), requestManager)
        layoutManager = onCreateLayoutManager(requireContext())
        scrollListener = RecyclerViewScrollHandler(this, RecyclerViewScrollHandler.RecyclerViewCallback(binding.recyclerView))

        binding.recyclerView.layoutManager = layoutManager
        binding.recyclerView.setHasFixedSize(true)
        val swipeLayout = binding.swipeLayout
        if (swipeLayout is ExtendedSwipeRefreshLayout) {
            swipeLayout.touchInterceptor = object : IExtendedView.TouchInterceptor {
                override fun dispatchTouchEvent(view: View, event: MotionEvent): Boolean {
                    scrollListener.touchListener.onTouch(view, event)
                    return false
                }

                override fun onInterceptTouchEvent(view: View, event: MotionEvent): Boolean {
                    if (event.actionMasked == MotionEvent.ACTION_DOWN) {
                        updateRefreshProgressOffset()
                    }
                    return false
                }

                override fun onTouchEvent(view: View, event: MotionEvent): Boolean {
                    return false
                }

            }
        } else {
            binding.recyclerView.setOnTouchListener(scrollListener.touchListener)
        }
        setupRecyclerView(requireContext(), binding.recyclerView)
        binding.recyclerView.adapter = adapter

        scrollListener.touchSlop = ViewConfiguration.get(context).scaledTouchSlop

    }

    protected open fun setupRecyclerView(context: Context, recyclerView: RecyclerView) {
        itemDecoration = onCreateItemDecoration(context, recyclerView, layoutManager)
        itemDecoration?.let {
            recyclerView.addItemDecoration(it)
        }
    }

    protected abstract fun onCreateLayoutManager(context: Context): L

    override fun onStart() {
        super.onStart()
        binding.recyclerView.addOnScrollListener(scrollListener)
    }

    override fun onStop() {
        binding.recyclerView.removeOnScrollListener(scrollListener)
        super.onStop()
    }

    override fun onDetach() {
        val activity = activity
        if (activity is IControlBarActivity) {
            activity.unregisterControlBarOffsetListener(this)
        }
        super.onDetach()
    }

    protected open val extraContentPadding: Rect
        get() = Rect()

    override fun onApplySystemWindowInsets(insets: Rect) {
        val extraPadding = extraContentPadding
        binding.recyclerView.setPadding(insets.left + extraPadding.left, insets.top + extraPadding.top,
                insets.right + extraPadding.right, insets.bottom + extraPadding.bottom)
        errorContainer.setPadding(insets.left, insets.top, insets.right, insets.bottom)
        progressContainer.setPadding(insets.left, insets.top, insets.right, insets.bottom)
        systemWindowsInsets.set(insets)
        updateRefreshProgressOffset()
    }

    open fun setLoadMoreIndicatorPosition(@IndicatorPosition position: Long) {
        adapter.loadMoreIndicatorPosition = position
    }

    override fun triggerRefresh(): Boolean {
        return false
    }

    protected abstract fun onCreateAdapter(context: Context, requestManager: RequestManager): A

    protected open fun onCreateItemDecoration(context: Context, recyclerView: RecyclerView,
                                              layoutManager: L): ItemDecoration? {
        return null
    }

    protected fun showContent() {
        errorContainer.visibility = View.GONE
        progressContainer.visibility = View.GONE
        binding.recyclerView.visibility = View.VISIBLE
    }

    protected fun showProgress() {
        errorContainer.visibility = View.GONE
        progressContainer.visibility = View.VISIBLE
        binding.recyclerView.visibility = View.GONE
    }

    protected fun showError(icon: Int, text: CharSequence) {
        errorContainer.visibility = View.VISIBLE
        progressContainer.visibility = View.GONE
        binding.recyclerView.visibility = View.GONE
        errorIcon.setImageResource(icon)
        errorText.text = text
    }

    protected fun showEmpty(icon: Int, text: CharSequence) {
        errorContainer.visibility = View.VISIBLE
        progressContainer.visibility = View.GONE
        binding.recyclerView.visibility = View.VISIBLE
        errorIcon.setImageResource(icon)
        errorText.text = text
    }

    protected fun updateRefreshProgressOffset() {
        val insets = this.systemWindowsInsets
        if (insets.top == 0 || binding.swipeLayout == null || binding.swipeLayout.isRefreshing) {
            return
        }
        val progressCircleDiameter = binding.swipeLayout.progressCircleDiameter
        if (progressCircleDiameter == 0) return
        val progressViewStart = 0 - progressCircleDiameter
        val progressViewEnd = insets.top + resources.getDimensionPixelSize(R.dimen.element_spacing_normal)
        binding.swipeLayout.setProgressViewOffset(false, progressViewStart, progressViewEnd)
    }

    interface RefreshCompleteListener {
        fun onRefreshComplete(fragment: AbsContentRecyclerViewFragment<*, *>)
    }
}
