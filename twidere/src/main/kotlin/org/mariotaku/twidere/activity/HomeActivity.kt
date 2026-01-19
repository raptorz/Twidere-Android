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

package org.mariotaku.twidere.activity

import android.accounts.Account
import android.accounts.AccountManager
import android.accounts.OnAccountsUpdateListener
import android.annotation.SuppressLint
import android.app.Dialog
import android.app.PendingIntent
import android.app.SearchManager
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.content.SharedPreferences
import android.content.SharedPreferences.OnSharedPreferenceChangeListener
import android.content.res.Configuration
import android.graphics.Rect
import android.graphics.drawable.Drawable
import android.os.AsyncTask
import android.os.Build
import android.os.Bundle
import android.util.SparseIntArray
import android.view.Gravity
import android.view.KeyEvent
import android.view.MenuItem
import android.view.View
import android.view.View.OnClickListener
import android.view.View.OnLongClickListener
import android.view.ViewGroup.MarginLayoutParams
import android.widget.RelativeLayout
import androidx.annotation.StringRes
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatDelegate
import androidx.appcompat.widget.TintTypedArray
import androidx.core.app.NotificationCompat
import androidx.core.view.*
import androidx.drawerlayout.widget.DrawerLayout
import androidx.drawerlayout.widget.DrawerLayoutAccessor
import androidx.fragment.app.Fragment
import androidx.viewpager.widget.ViewPager.OnPageChangeListener
import com.getkeepsafe.taptargetview.TapTarget
import com.getkeepsafe.taptargetview.TapTargetView
import com.squareup.otto.Subscribe
import nl.komponents.kovenant.task
import org.mariotaku.twidere.databinding.ActivityHomeBinding
import org.mariotaku.twidere.databinding.ActivityHomeContentBinding
import org.mariotaku.chameleon.ChameleonUtils
import org.mariotaku.kpreferences.contains
import org.mariotaku.kpreferences.get
import org.mariotaku.kpreferences.set
import org.mariotaku.ktextension.addOnAccountsUpdatedListenerSafe
import org.mariotaku.ktextension.coerceInOr
import org.mariotaku.ktextension.contains
import org.mariotaku.ktextension.removeOnAccountsUpdatedListenerSafe
import org.mariotaku.sqliteqb.library.Expression
import org.mariotaku.twidere.Constants.*
import org.mariotaku.twidere.R
import org.mariotaku.twidere.activity.iface.IControlBarActivity.ControlBarShowHideHelper
import org.mariotaku.twidere.adapter.SupportTabsAdapter
import org.mariotaku.twidere.annotation.CustomTabType
import org.mariotaku.twidere.annotation.FilterScope
import org.mariotaku.twidere.annotation.NavbarStyle
import org.mariotaku.twidere.annotation.ReadPositionTag
import org.mariotaku.twidere.constant.*
import org.mariotaku.twidere.extension.applyTheme
import org.mariotaku.twidere.extension.model.notificationBuilder
import org.mariotaku.twidere.extension.onShow
import org.mariotaku.twidere.fragment.AccountsDashboardFragment
import org.mariotaku.twidere.fragment.BaseDialogFragment
import org.mariotaku.twidere.fragment.iface.IFloatingActionButtonFragment
import org.mariotaku.twidere.fragment.iface.RefreshScrollTopInterface
import org.mariotaku.twidere.fragment.iface.SupportFragmentCallback
import org.mariotaku.twidere.graphic.EmptyDrawable
import org.mariotaku.twidere.model.AccountDetails
import org.mariotaku.twidere.model.SupportTabSpec
import org.mariotaku.twidere.model.Tab
import org.mariotaku.twidere.model.UserKey
import org.mariotaku.twidere.model.event.UnreadCountUpdatedEvent
import org.mariotaku.twidere.model.notification.NotificationChannelSpec
import org.mariotaku.twidere.provider.TwidereDataStore.Activities
import org.mariotaku.twidere.provider.TwidereDataStore.Messages.Conversations
import org.mariotaku.twidere.provider.TwidereDataStore.Statuses
import org.mariotaku.twidere.receiver.NotificationReceiver
import org.mariotaku.twidere.util.*
import org.mariotaku.twidere.util.KeyboardShortcutsHandler.KeyboardShortcutCallback
import org.mariotaku.twidere.util.premium.ExtraFeaturesService
import org.mariotaku.twidere.view.HomeDrawerLayout
import org.mariotaku.twidere.view.TabPagerIndicator
import java.lang.ref.WeakReference
import kotlin.math.floor

class HomeActivity : BaseActivity(), OnClickListener, OnPageChangeListener, SupportFragmentCallback,
        OnLongClickListener, DrawerLayout.DrawerListener {

    private val accountUpdatedListener = AccountUpdatedListener(this)

    private var selectedAccountToSearch: AccountDetails? = null

    private lateinit var multiSelectHandler: MultiSelectEventHandler
    private lateinit var pagerAdapter: SupportTabsAdapter
    private lateinit var drawerToggle: ActionBarDrawerToggle

    private lateinit var binding: ActivityHomeBinding
    private lateinit var contentBinding: ActivityHomeContentBinding
    private lateinit var emptyTabHint: View

    private var propertiesInitialized = false
    private var actionsButtonBottomMargin: Int = 0

    private var updateUnreadCountTask: UpdateUnreadCountTask? = null
    private val readStateChangeListener = OnSharedPreferenceChangeListener { _, _ -> updateUnreadCount() }
    private val controlBarShowHideHelper = ControlBarShowHideHelper(this)

    override val controlBarHeight: Int
        get() {
            return contentBinding.mainTabs.height - contentBinding.mainTabs.stripHeight
        }

    override val currentVisibleFragment: Fragment?
        get() {
            val currentItem = contentBinding.mainPager.currentItem
            if (currentItem < 0 || currentItem >= pagerAdapter.count) return null
            return pagerAdapter.instantiateItem(contentBinding.mainPager, currentItem)
        }

    private val homeDrawerToggleDelegate = object : ActionBarDrawerToggle.Delegate {
        override fun setActionBarUpIndicator(upDrawable: Drawable, @StringRes contentDescRes: Int) {
            contentBinding.drawerToggleButton.setImageDrawable(upDrawable)
            contentBinding.drawerToggleButton.setColorFilter(ChameleonUtils.getColorDependent(overrideTheme.colorToolbar))
            contentBinding.drawerToggleButton.contentDescription = getString(contentDescRes)
        }

        override fun setActionBarDescription(@StringRes contentDescRes: Int) {
            contentBinding.drawerToggleButton.contentDescription = getString(contentDescRes)
        }

        @SuppressLint("RestrictedApi")
        override fun getThemeUpIndicator(): Drawable {
            val a = TintTypedArray.obtainStyledAttributes(actionBarThemedContext, null,
                    HOME_AS_UP_ATTRS)
            val result = a.getDrawable(0)
            a.recycle()
            return result
        }

        override fun getActionBarThemedContext(): Context {
            return contentBinding.toolbar.context
        }

        override fun isNavigationVisible(): Boolean {
            return true
        }
    }

    private val keyboardShortcutRecipient: Fragment?
        get() = when {
            binding.homeMenu.isDrawerOpen(GravityCompat.START) -> leftDrawerFragment
            binding.homeMenu.isDrawerOpen(GravityCompat.END) -> null
            else -> currentVisibleFragment
        }

    private val activatedAccountKeys: Array<UserKey>
        get() = DataStoreUtils.getActivatedAccountKeys(this)

    private val leftDrawerFragment: Fragment?
        get() = supportFragmentManager.findFragmentById(R.id.leftDrawer)

    private val isDrawerOpen: Boolean
        get() {
            return binding.homeMenu.isDrawerOpen(GravityCompat.START) || binding.homeMenu.isDrawerOpen(GravityCompat.END)
        }

    /**
     * Called when the context is first created.
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        multiSelectHandler = MultiSelectEventHandler(this)
        multiSelectHandler.dispatchOnCreate()
        DataStoreUtils.prepareDatabase(this)
        if (!DataStoreUtils.hasAccount(this)) {
            val signInIntent = Intent(INTENT_ACTION_TWITTER_LOGIN)
            signInIntent.setClass(this, SignInActivity::class.java)
            startActivity(signInIntent)
            finish()
            if (defaultAutoRefreshAskedKey !in kPreferences) {
                // Assume first install
                kPreferences[defaultAutoRefreshAskedKey] = false
            }
            return
        } else {
            notifyAccountsChanged()
        }
        supportRequestWindowFeature(AppCompatDelegate.FEATURE_ACTION_MODE_OVERLAY)
        binding = ActivityHomeBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        val homeContentView = binding.homeMenu.findViewById<View>(R.id.homeContent)
        contentBinding = ActivityHomeContentBinding.bind(homeContentView)
        emptyTabHint = binding.homeMenu.findViewById(R.id.emptyTabHint)
        
        setSupportActionBar(contentBinding.toolbar)

        drawerToggle = ActionBarDrawerToggle(this, binding.homeMenu, R.string.open_accounts_dashboard,
                R.string.close_accounts_dashboard)
        pagerAdapter = SupportTabsAdapter(this, supportFragmentManager, contentBinding.mainTabs)
        propertiesInitialized = true

        ThemeUtils.setCompatContentViewOverlay(window, EmptyDrawable())

        val refreshOnStart = preferences[refreshOnStartKey]
        var tabDisplayOptionInt = Utils.getTabDisplayOptionInt(this)

        ViewCompat.setOnApplyWindowInsetsListener(contentBinding.homeContent, this)
        binding.homeMenu.fitsSystemWindows = Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP ||
                preferences[navbarStyleKey] != NavbarStyle.TRANSPARENT
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP || !ViewCompat.getFitsSystemWindows(binding.homeMenu)) {
            ViewCompat.setOnApplyWindowInsetsListener(binding.homeMenu, null)
        }

        contentBinding.mainPager.adapter = pagerAdapter
        contentBinding.mainTabs.setViewPager(contentBinding.mainPager)
        contentBinding.mainTabs.setOnPageChangeListener(this)
        if (tabDisplayOptionInt == 0) {
            tabDisplayOptionInt = TabPagerIndicator.DisplayOption.ICON
        }
        contentBinding.mainTabs.setTabDisplayOption(tabDisplayOptionInt)
        contentBinding.mainTabs.setTabExpandEnabled(TabPagerIndicator.DisplayOption.LABEL !in tabDisplayOptionInt)
        contentBinding.mainTabs.setDisplayBadge(preferences[unreadCountKey])
         contentBinding.mainTabs.updateAppearance()

         contentBinding.drawerToggleButton.visibility = View.VISIBLE

         if (preferences[fabVisibleKey]) {
             contentBinding.actionsButton.visibility = View.VISIBLE
         } else {
             contentBinding.actionsButton.visibility = View.GONE
         }
        actionsButtonBottomMargin = (contentBinding.actionsButton.layoutParams as MarginLayoutParams).bottomMargin

        contentBinding.homeContent.addOnLayoutChangeListener { _, _, top, _, _, _, oldTop, _, _ ->
            if (top != oldTop) {
                val fragment = leftDrawerFragment
                if (fragment is AccountsDashboardFragment) {
                    fragment.setStatusBarHeight(top)
                }
            }
        }

        contentBinding.actionsButton.setOnClickListener(this)
        contentBinding.actionsButton.setOnLongClickListener(this)
        contentBinding.drawerToggleButton.setOnClickListener(this)
        emptyTabHint.setOnClickListener(this)

        setupSlidingMenu()
        setupBars()
        showPromotionOffer()
        initUnreadCount()
        setupHomeTabs()
        updateActionsButton()

        if (savedInstanceState == null) {
            if (refreshOnStart) {
                twitterWrapper.refreshAll(activatedAccountKeys)
            }
            if (intent.getBooleanExtra(EXTRA_OPEN_ACCOUNTS_DRAWER, false)) {
                openAccountsDrawer()
            }
        }

        val initialTabPosition = handleIntent(intent, savedInstanceState == null)
        setTabPosition(initialTabPosition)

        if (!showDrawerTutorial() && !kPreferences[defaultAutoRefreshAskedKey]) {
            showAutoRefreshConfirm()
        }
    }

    override fun onPostCreate(savedInstanceState: Bundle?) {
        super.onPostCreate(savedInstanceState)
        // Sync the toggle state after onRestoreInstanceState has occurred.
        drawerToggle.syncState()
    }

    override fun onStart() {
        super.onStart()
        multiSelectHandler.dispatchOnStart()
        AccountManager.get(this).addOnAccountsUpdatedListenerSafe(accountUpdatedListener, updateImmediately = false)
        bus.register(this)

        readStateManager.registerOnSharedPreferenceChangeListener(readStateChangeListener)
        updateUnreadCount()
    }

    override fun onResume() {
        super.onResume()
        invalidateOptionsMenu()
        updateActionsButton()
    }

    override fun onStop() {
        multiSelectHandler.dispatchOnStop()
        readStateManager.unregisterOnSharedPreferenceChangeListener(readStateChangeListener)
        bus.unregister(this)
        AccountManager.get(this).removeOnAccountsUpdatedListenerSafe(accountUpdatedListener)
        preferences.edit().putInt(KEY_SAVED_TAB_POSITION, contentBinding.mainPager.currentItem).apply()
        timelineSyncManager?.commit()
        super.onStop()
    }

    override fun onDestroy() {
        if (isFinishing) {
            // Delete unused items in databases.
            val context = applicationContext
            task { DataStoreUtils.cleanDatabasesByItemLimit(context) }
        }
        super.onDestroy()
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        // Pass any configuration change to the binding.drawer toggle
        drawerToggle.onConfigurationChanged(newConfig)
    }

    override fun onAttachFragment(fragment: Fragment) {
        super.onAttachFragment(fragment)
        updateActionsButton()
    }

    override fun onClick(v: View) {
        when (v) {
            contentBinding.actionsButton -> {
                triggerActionsClick()
            }
            emptyTabHint -> {
                startActivityForResult(IntentUtils.settings("tabs"), REQUEST_SETTINGS)
            }
            contentBinding.drawerToggleButton -> {
                if (binding.homeMenu.isDrawerOpen(GravityCompat.START) || binding.homeMenu.isDrawerOpen(GravityCompat.END)) {
                    binding.homeMenu.closeDrawers()
                } else {
                    binding.homeMenu.openDrawer(GravityCompat.START)
                }
            }
        }
    }

    override fun onLongClick(v: View): Boolean {
        when (v) {
            contentBinding.actionsButton -> {
                Utils.showMenuItemToast(v, v.contentDescription, true)
                return true
            }
        }
        return false
    }

    override fun onPageScrolled(position: Int, positionOffset: Float, positionOffsetPixels: Int) {
    }

    override fun onPageSelected(position: Int) {
        if (binding.homeMenu.isDrawerOpen(GravityCompat.START)) {
            binding.homeMenu.closeDrawers()
        }
        updateActionsButton()
    }

    override fun onPageScrollStateChanged(state: Int) {
        setControlBarVisibleAnimate(true)
    }

    override fun onSearchRequested(): Boolean {
        startActivity(Intent(this, QuickSearchBarActivity::class.java))
        return true
    }

    override fun getSystemWindowInsets(caller: Fragment, insets: Rect): Boolean {
        if (caller === leftDrawerFragment) return super.getSystemWindowInsets(caller, insets)
        if (contentBinding.mainTabs == null || contentBinding.homeContent == null || contentBinding.toolbar == null || !contentBinding.toolbar.isVisible) return false
        val height = contentBinding.mainTabs.height
        if (preferences[tabPositionKey] == SharedPreferenceConstants.VALUE_TAB_POSITION_TOP) {
            if (height != 0) {
                insets.top = height
            } else {
                insets.top = ThemeUtils.getActionBarHeight(this)
            }
            insets.bottom = systemWindowsInsets?.bottom ?: 0
        } else {
            if (height != 0) {
                insets.bottom = height
            } else {
                insets.bottom = ThemeUtils.getActionBarHeight(this)
            }
        }
        return true
    }

    @SuppressLint("RestrictedApi")
    override fun onApplyWindowInsets(v: View, insets: WindowInsetsCompat): WindowInsetsCompat {
        super.onApplyWindowInsets(v, insets)
        val fragment = leftDrawerFragment
        if (fragment is AccountsDashboardFragment) {
            fragment.requestApplyInsets()
        }
        binding.homeMenu.setChildInsets(insets.unwrapped, insets.systemWindowInsetTop > 0)
        if (!ViewCompat.getFitsSystemWindows(binding.homeMenu)) {
            contentBinding.homeContent.setPadding(0, insets.systemWindowInsetTop, 0, 0)
        }
        (contentBinding.toolbar.layoutParams as? MarginLayoutParams)?.bottomMargin = insets.systemWindowInsetBottom
        (contentBinding.actionsButton.layoutParams as? MarginLayoutParams)?.bottomMargin =
                actionsButtonBottomMargin + if (preferences[tabPositionKey] == SharedPreferenceConstants.VALUE_TAB_POSITION_TOP) {
                    insets.systemWindowInsetBottom
                } else {
                    0
                }
        return insets
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        val tabPosition = handleIntent(intent, false)
        if (tabPosition >= 0) {
            contentBinding.mainPager.currentItem = tabPosition.coerceInOr(0 until pagerAdapter.count, 0)
        }
    }

    override fun setControlBarVisibleAnimate(visible: Boolean,
            listener: ControlBarShowHideHelper.ControlBarAnimationListener?) {
        controlBarShowHideHelper.setControlBarVisibleAnimate(visible, listener)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (drawerToggle.onOptionsItemSelected(item)) {
            return true
        }
        when (item.itemId) {
            android.R.id.home -> {
                val fm = supportFragmentManager
                val count = fm.backStackEntryCount
                if (binding.homeMenu.isDrawerOpen(GravityCompat.START) || binding.homeMenu.isDrawerOpen(GravityCompat.END)) {
                    binding.homeMenu.closeDrawers()
                    return true
                } else if (count == 0) {
                    binding.homeMenu.openDrawer(GravityCompat.START)
                    return true
                }
                return true
            }
            R.id.search -> {
                openSearchView(selectedAccountToSearch)
                return true
            }
            R.id.actions -> {
                triggerActionsClick()
                return true
            }
        }
        return super.onOptionsItemSelected(item)
    }

    override fun handleKeyboardShortcutSingle(handler: KeyboardShortcutsHandler, keyCode: Int,
            event: KeyEvent, metaState: Int): Boolean {
        if (handleFragmentKeyboardShortcutSingle(handler, keyCode, event, metaState)) return true
        var action = handler.getKeyAction(KeyboardShortcutConstants.CONTEXT_TAG_HOME, keyCode, event, metaState)
        if (action != null) {
            when (action) {
                KeyboardShortcutConstants.ACTION_HOME_ACCOUNTS_DASHBOARD -> {
                    if (binding.homeMenu.isDrawerOpen(GravityCompat.START)) {
                        binding.homeMenu.closeDrawers()
                    } else {
                        binding.homeMenu.openDrawer(GravityCompat.START)
                        setControlBarVisibleAnimate(true)
                    }
                    return true
                }
            }
        }
        action = handler.getKeyAction(KeyboardShortcutConstants.CONTEXT_TAG_NAVIGATION, keyCode, event, metaState)
        if (action != null) {
            when (action) {
                KeyboardShortcutConstants.ACTION_NAVIGATION_PREVIOUS_TAB -> {
                    val previous = contentBinding.mainPager.currentItem - 1
                    if (previous < 0 && DrawerLayoutAccessor.findDrawerWithGravity(binding.homeMenu, Gravity.START) != null) {
                        binding.homeMenu.openDrawer(GravityCompat.START)
                        setControlBarVisibleAnimate(true)
                    } else if (previous < pagerAdapter.count) {
                        if (binding.homeMenu.isDrawerOpen(GravityCompat.END)) {
                            binding.homeMenu.closeDrawers()
                        } else {
                            contentBinding.mainPager.setCurrentItem(previous, true)
                        }
                    }
                    return true
                }
                KeyboardShortcutConstants.ACTION_NAVIGATION_NEXT_TAB -> {
                    val next = contentBinding.mainPager.currentItem + 1
                    if (next >= pagerAdapter.count && DrawerLayoutAccessor.findDrawerWithGravity(binding.homeMenu, Gravity.END) != null) {
                        binding.homeMenu.openDrawer(GravityCompat.END)
                        setControlBarVisibleAnimate(true)
                    } else if (next >= 0) {
                        if (binding.homeMenu.isDrawerOpen(GravityCompat.START)) {
                            binding.homeMenu.closeDrawers()
                        } else {
                            contentBinding.mainPager.setCurrentItem(next, true)
                        }
                    }
                    return true
                }
            }
        }
        return handler.handleKey(this, null, keyCode, event, metaState)
    }

    override fun isKeyboardShortcutHandled(handler: KeyboardShortcutsHandler, keyCode: Int,
            event: KeyEvent, metaState: Int): Boolean {
        if (isFragmentKeyboardShortcutHandled(handler, keyCode, event, metaState)) return true
        return super.isKeyboardShortcutHandled(handler, keyCode, event, metaState)
    }

    override fun handleKeyboardShortcutRepeat(handler: KeyboardShortcutsHandler, keyCode: Int,
            repeatCount: Int, event: KeyEvent, metaState: Int): Boolean {
        if (handleFragmentKeyboardShortcutRepeat(handler, keyCode, repeatCount, event, metaState))
            return true
        return super.handleKeyboardShortcutRepeat(handler, keyCode, repeatCount, event, metaState)
    }

    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        when (event.keyCode) {
            KeyEvent.KEYCODE_MENU -> {
                if (event.action != KeyEvent.ACTION_UP) return true
                if (isDrawerOpen) {
                    binding.homeMenu.closeDrawers()
                } else {
                    binding.homeMenu.openDrawer(GravityCompat.START)
                }
                return true
            }
        }
        return super.dispatchKeyEvent(event)
    }

    override fun onKeyUp(keyCode: Int, event: KeyEvent): Boolean {
        when (keyCode) {
            KeyEvent.KEYCODE_BACK -> {
                if (isDrawerOpen) {
                    binding.homeMenu.closeDrawers()
                    return true
                }
            }
        }
        return super.onKeyUp(keyCode, event)
    }

    override fun triggerRefresh(position: Int): Boolean {
        val f = pagerAdapter.instantiateItem(contentBinding.mainPager, position)
        if (f.activity == null || f.isDetached) return false
        if (f !is RefreshScrollTopInterface) return false
        return f.triggerRefresh()
    }

    fun notifyAccountsChanged() {
    }

    @Subscribe
    fun notifyUnreadCountUpdated(event: UnreadCountUpdatedEvent) {
        updateUnreadCount()
    }

    fun updateUnreadCount() {
        if (contentBinding.mainTabs == null || updateUnreadCountTask != null && updateUnreadCountTask!!.status == AsyncTask.Status.RUNNING)
            return
        updateUnreadCountTask = UpdateUnreadCountTask(this, preferences, readStateManager, contentBinding.mainTabs,
                pagerAdapter.tabs.toTypedArray()).apply { execute() }
        contentBinding.mainTabs.setDisplayBadge(preferences.getBoolean(SharedPreferenceConstants.KEY_UNREAD_COUNT, true))
    }

    val tabs: List<SupportTabSpec>
        get() = pagerAdapter.tabs

    override var controlBarOffset: Float
        get() {
            if (contentBinding.mainTabs.columns > 1 || !contentBinding.toolbar.isVisible) {
                val lp = contentBinding.actionsButton.layoutParams
                val total: Float
                total = if (lp is MarginLayoutParams) {
                    (lp.bottomMargin + contentBinding.actionsButton.height).toFloat()
                } else {
                    contentBinding.actionsButton.height.toFloat()
                }
                return 1 - contentBinding.actionsButton.translationY / total
            }
            val totalHeight = controlBarHeight.toFloat()
            return 1 + if (preferences[tabPositionKey] == SharedPreferenceConstants.VALUE_TAB_POSITION_TOP) {
                contentBinding.toolbar.translationY
            }  else {
                -contentBinding.toolbar.translationY
            } / totalHeight
        }
        set(offset) {
            if (preferences[tabPositionKey] == SharedPreferenceConstants.VALUE_TAB_POSITION_TOP) {
                val translationY = if (contentBinding.mainTabs.columns > 1 || !contentBinding.toolbar.isVisible) {
                    0
                } else {
                    (controlBarHeight * (offset - 1)).toInt()
                }
                contentBinding.toolbar.translationY = translationY.toFloat()
                contentBinding.windowOverlay.translationY = translationY.toFloat()
                val lp = contentBinding.actionsButton.layoutParams
                if (lp is MarginLayoutParams) {
                    contentBinding.actionsButton.translationY = (lp.bottomMargin + contentBinding.actionsButton.height) * (1 - offset)
                } else {
                    contentBinding.actionsButton.translationY = contentBinding.actionsButton.height * (1 - offset)
                }
                notifyControlBarOffsetChanged()
            } else {
                val layoutparams = contentBinding.toolbar.layoutParams
                val toolbarMarginBottom = if (layoutparams is MarginLayoutParams) {
                    layoutparams.bottomMargin
                } else {
                    0
                }
                val translationY = if (contentBinding.mainTabs.columns > 1 || !contentBinding.toolbar.isVisible) {
                    0
                } else {
                    ((contentBinding.toolbar.height + toolbarMarginBottom) * (offset - 1)).toInt()
                }
                contentBinding.toolbar.translationY = -translationY.toFloat()
                contentBinding.windowOverlay.translationY = -translationY.toFloat()
                val lp = contentBinding.actionsButton.layoutParams
                if (lp is MarginLayoutParams) {
                    contentBinding.actionsButton.translationY = (lp.bottomMargin + contentBinding.toolbar.height + contentBinding.actionsButton.height + toolbarMarginBottom) * (1 - offset)
                } else {
                    contentBinding.actionsButton.translationY = contentBinding.actionsButton.height * (1 - offset)
                }
                notifyControlBarOffsetChanged()
            }
        }

    override fun onDrawerSlide(drawerView: View, slideOffset: Float) {

    }

    override fun onDrawerOpened(drawerView: View) {

    }

    override fun onDrawerClosed(drawerView: View) {

    }

    override fun onDrawerStateChanged(newState: Int) {
        val fragment = leftDrawerFragment
        if (newState == DrawerLayout.STATE_DRAGGING && fragment is AccountsDashboardFragment) {
            fragment.loadAccounts()
        }
    }

    override fun getDrawerToggleDelegate(): ActionBarDrawerToggle.Delegate? {
        return homeDrawerToggleDelegate
    }

    fun closeAccountsDrawer() {
        binding.homeMenu.closeDrawers()
    }

    private fun openSearchView(account: AccountDetails?) {
        selectedAccountToSearch = account
        onSearchRequested()
    }

    private fun handleFragmentKeyboardShortcutRepeat(handler: KeyboardShortcutsHandler,
            keyCode: Int, repeatCount: Int,
            event: KeyEvent, metaState: Int): Boolean {
        val fragment = keyboardShortcutRecipient
        if (fragment is KeyboardShortcutCallback) {
            return fragment.handleKeyboardShortcutRepeat(handler, keyCode,
                    repeatCount, event, metaState)
        }
        return false
    }

    private fun handleFragmentKeyboardShortcutSingle(handler: KeyboardShortcutsHandler,
            keyCode: Int, event: KeyEvent,
            metaState: Int): Boolean {
        val fragment = keyboardShortcutRecipient
        if (fragment is KeyboardShortcutCallback) {
            return fragment.handleKeyboardShortcutSingle(handler, keyCode,
                    event, metaState)
        }
        return false
    }

    private fun isFragmentKeyboardShortcutHandled(handler: KeyboardShortcutsHandler,
            keyCode: Int, event: KeyEvent, metaState: Int): Boolean {
        val fragment = keyboardShortcutRecipient
        if (fragment is KeyboardShortcutCallback) {
            return fragment.isKeyboardShortcutHandled(handler, keyCode,
                    event, metaState)
        }
        return false
    }

    private fun handleIntent(intent: Intent, handleExtraIntent: Boolean): Int {
        // use package's class loader to prevent BadParcelException
        intent.setExtrasClassLoader(classLoader)
        // reset intent
        setIntent(Intent(this, HomeActivity::class.java))
        val action = intent.action
        if (Intent.ACTION_SEARCH == action) {
            val query = intent.getStringExtra(SearchManager.QUERY)
            val appSearchData = intent.getBundleExtra(SearchManager.APP_DATA)
            val accountKey = if (appSearchData != null && appSearchData.containsKey(EXTRA_ACCOUNT_KEY)) {
                appSearchData.getParcelable(EXTRA_ACCOUNT_KEY)
            } else {
                Utils.getDefaultAccountKey(this)
            }
            if (query != null) {
                IntentUtils.openSearch(this, accountKey, query)
            }
            return -1
        }
        val refreshOnStart = preferences.getBoolean(SharedPreferenceConstants.KEY_REFRESH_ON_START, false)
        if (handleExtraIntent && refreshOnStart) {
            twitterWrapper.refreshAll()
        }

        val uri = intent.data
        @CustomTabType
        val tabType = if (uri != null) Utils.matchTabType(uri) else null
        var initialTab = -1
        if (tabType != null) {
            val accountKey = uri?.getQueryParameter(QUERY_PARAM_ACCOUNT_KEY)?.let(UserKey::valueOf)
            val adapter = pagerAdapter
            for (i in 0 until adapter.count) {
                val tab = adapter.get(i)
                if (tabType == Tab.getTypeAlias(tab.type)) {
                    val args = tab.args
                    if (args != null && CustomTabUtils.hasAccountKey(this, args,
                            activatedAccountKeys, accountKey)) {
                        initialTab = i
                        break
                    }
                }
            }
            if (initialTab == -1 && !handleExtraIntent) {
                // Tab not found, open account specific page
                when (tabType) {
                    CustomTabType.NOTIFICATIONS_TIMELINE -> {
                        IntentUtils.openInteractions(this, accountKey)
                        return -1
                    }
                    CustomTabType.DIRECT_MESSAGES -> {
                        IntentUtils.openDirectMessages(this, accountKey)
                        return -1
                    }
                }
            }
        }
        return initialTab
    }

    private fun initUnreadCount() {
        for (i in 0 until contentBinding.mainTabs.count) {
            contentBinding.mainTabs.setBadge(i, 0)
        }
    }

    private fun openAccountsDrawer() {
        binding.homeMenu.openDrawer(GravityCompat.START)
    }

    private fun showDrawerTutorial(): Boolean {
        if (preferences[drawerTutorialCompleted]) return false
        val targetSize = resources.getDimensionPixelSize(R.dimen.element_size_mlarge)
        val height = resources.displayMetrics.heightPixels
        val listener: TapTargetView.Listener = object : TapTargetView.Listener() {
            override fun onTargetClick(view: TapTargetView?) {
                if (!binding.homeMenu.isDrawerOpen(GravityCompat.START)) {
                    binding.homeMenu.openDrawer(GravityCompat.START)
                }
                super.onTargetClick(view)
            }

            override fun onTargetDismissed(view: TapTargetView?, userInitiated: Boolean) {
                preferences[drawerTutorialCompleted] = true
                showAutoRefreshConfirm()

            }
        }
        val target = Rect(0, 0, targetSize, targetSize)
        target.offsetTo(0, height / 2 - targetSize / 2)
        TapTargetView.showFor(this, TapTarget.forBounds(target,
                getString(R.string.hint_accounts_dashboard_title),
                getString(R.string.hint_accounts_dashboard_message))
                .apply {
                    outerCircleColor(R.color.branding_color)
                    dimColor(android.R.color.black)
                }, listener)

        return true
    }

    private fun showAutoRefreshConfirm() {
        if (isFinishing) return
        executeAfterFragmentResumed { activity ->
            val df = AutoRefreshConfirmDialogFragment()
            df.show(activity.supportFragmentManager, "auto_refresh_confirm")
        }
    }

    private fun setTabPosition(initialTab: Int) {
        val rememberPosition = preferences.getBoolean(SharedPreferenceConstants.KEY_REMEMBER_POSITION, true)
        if (initialTab >= 0) {
            contentBinding.mainPager.currentItem = initialTab.coerceInOr(0 until pagerAdapter.count, 0)
        } else if (rememberPosition) {
            val position = preferences.getInt(SharedPreferenceConstants.KEY_SAVED_TAB_POSITION, 0)
            contentBinding.mainPager.currentItem = position.coerceInOr(0 until pagerAdapter.count, 0)
        }
    }

    private fun setupBars() {
        val backgroundOption = currentThemeBackgroundOption
        val isTransparent = ThemeUtils.isTransparentBackground(backgroundOption)
        val actionBarAlpha = if (isTransparent) {
            ThemeUtils.getActionBarAlpha(preferences[themeBackgroundAlphaKey])
        } else {
            0xFF
        }
        contentBinding.actionsButton.alpha = actionBarAlpha / 255f
    }

    private fun setupHomeTabs() {
        pagerAdapter.clear()
        pagerAdapter.addAll(CustomTabUtils.getHomeTabs(this))
        val hasNoTab = pagerAdapter.count == 0
        emptyTabHint.visibility = if (hasNoTab) View.VISIBLE else View.GONE
        contentBinding.mainPager.visibility = if (hasNoTab) View.GONE else View.VISIBLE
        if (pagerAdapter.count > 1 && hasMultiColumns()) {
            contentBinding.mainPager.pageMargin = resources.getDimensionPixelOffset(R.dimen.home_page_margin)
            contentBinding.mainPager.setPageMarginDrawable(ThemeUtils.getDrawableFromThemeAttribute(this, R.attr.dividerVertical))
            pagerAdapter.hasMultipleColumns = true
            pagerAdapter.preferredColumnWidth = when (preferences[multiColumnWidthKey]) {
                "narrow" -> resources.getDimension(R.dimen.preferred_tab_column_width_narrow)
                "wide" -> resources.getDimension(R.dimen.preferred_tab_column_width_wide)
                else -> resources.getDimension(R.dimen.preferred_tab_column_width_normal)
            }
            contentBinding.mainTabs.columns = floor(1.0 / pagerAdapter.getPageWidth(0)).toInt()
        } else {
            contentBinding.mainPager.pageMargin = 0
            contentBinding.mainPager.setPageMarginDrawable(null)
            pagerAdapter.hasMultipleColumns = false
            contentBinding.mainTabs.columns = 1
        }
        if (pagerAdapter.count == 1 && preferences[autoHideTabs]) {
            contentBinding.toolbar.isVisible = false
            contentBinding.actionsButton.updateLayoutParams<RelativeLayout.LayoutParams> {
                addRule(RelativeLayout.ALIGN_PARENT_BOTTOM, RelativeLayout.TRUE)
            }
        } else if (preferences[tabPositionKey] == SharedPreferenceConstants.VALUE_TAB_POSITION_TOP) {
            contentBinding.toolbar.updateLayoutParams<RelativeLayout.LayoutParams> {
                addRule(RelativeLayout.ALIGN_PARENT_TOP, RelativeLayout.TRUE)
            }
            contentBinding.actionsButton.updateLayoutParams<RelativeLayout.LayoutParams> {
                addRule(RelativeLayout.ALIGN_PARENT_BOTTOM, RelativeLayout.TRUE)
            }
        } else {
            contentBinding.toolbar.updateLayoutParams<RelativeLayout.LayoutParams> {
                addRule(RelativeLayout.ALIGN_PARENT_BOTTOM, RelativeLayout.TRUE)
            }
            contentBinding.actionsButton.updateLayoutParams<RelativeLayout.LayoutParams> {
                addRule(RelativeLayout.ABOVE, contentBinding.toolbar.id)
            }
        }
    }

    private fun setupSlidingMenu() {
        binding.homeMenu.setDrawerShadow(R.drawable.drawer_shadow_start, GravityCompat.START)
        binding.homeMenu.addDrawerListener(drawerToggle)
        binding.homeMenu.addDrawerListener(this)
        // Lock drawer gesture to prevent conflict with Android back gesture
        // Drawer can still be opened/closed via the hamburger button
        binding.homeMenu.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED, GravityCompat.START)
        binding.homeMenu.setShouldDisableDecider(HomeDrawerLayout.ShouldDisableDecider { e ->
            val fragment = leftDrawerFragment
            if (fragment is AccountsDashboardFragment) {
                return@ShouldDisableDecider fragment.shouldDisableDrawerSlide(e)
            }
            false
        })
    }

    private fun showPromotionOffer() {
        // Skip if app doesn't support extra features
        if (!extraFeaturesService.isSupported()) return
        // Skip if already bought enhanced features pack or have set promotions options
        if (extraFeaturesService.isEnabled(ExtraFeaturesService.FEATURE_FEATURES_PACK)
                || promotionsEnabledKey in preferences) {
            return
        }

        val intent = Intent(this, PremiumDashboardActivity::class.java)
        val contentIntent = PendingIntent.getActivity(this, 0, intent, 0)
        val builder = NotificationChannelSpec.appNotices.notificationBuilder(this)
        builder.setAutoCancel(true)
        builder.setSmallIcon(R.drawable.ic_stat_gift)
        builder.setTicker(getString(R.string.message_ticker_promotions_reward))
        builder.setContentTitle(getString(R.string.title_promotions_reward))
        builder.setContentText(getString(R.string.message_ticker_promotions_reward))
        builder.setContentIntent(contentIntent)
        builder.setStyle(NotificationCompat.BigTextStyle(builder)
                .setBigContentTitle(getString(R.string.title_promotions_reward))
                .bigText(getString(R.string.message_promotions_reward)))
        builder.addAction(R.drawable.ic_action_confirm, getString(R.string.action_enable),
                PendingIntent.getBroadcast(this, 0, Intent(this,
                        NotificationReceiver::class.java).setAction(BROADCAST_PROMOTIONS_ACCEPTED)
                        .putExtra(EXTRA_NOTIFICATION_ID, NOTIFICATION_ID_PROMOTIONS_OFFER),
                        PendingIntent.FLAG_ONE_SHOT))
        builder.addAction(R.drawable.ic_action_cancel, getString(R.string.action_no_thanks),
                PendingIntent.getBroadcast(this, 0, Intent(this,
                        NotificationReceiver::class.java).setAction(BROADCAST_PROMOTIONS_DENIED)
                        .putExtra(EXTRA_NOTIFICATION_ID, NOTIFICATION_ID_PROMOTIONS_OFFER),
                        PendingIntent.FLAG_ONE_SHOT))
        notificationManager.notify(NOTIFICATION_ID_PROMOTIONS_OFFER, builder.build())
    }

    private fun triggerActionsClick() {
        val position = contentBinding.mainPager.currentItem
        if (pagerAdapter.count == 0) return
        val fragment = pagerAdapter.instantiateItem(contentBinding.mainPager, position) as? IFloatingActionButtonFragment
        val handled = fragment?.onActionClick("home") ?: false
        if (!handled) {
            startActivity(Intent(INTENT_ACTION_COMPOSE))
        }
    }

    private fun updateActionsButton() {
        if (!propertiesInitialized) return
        val fragment = run {
            if (pagerAdapter.count == 0) return@run null
            val position = contentBinding.mainPager.currentItem
            val f = pagerAdapter.instantiateItem(contentBinding.mainPager, position) as? IFloatingActionButtonFragment
            if (f is Fragment && (f.isDetached || f.host == null)) {
                return@run null
            }
            return@run f
        }
        val info = fragment?.getActionInfo("home") ?: run {
            contentBinding.actionsButton.setImageResource(R.drawable.ic_action_status_compose)
            contentBinding.actionsButton.contentDescription = getString(R.string.action_compose)
            return
        }

        contentBinding.actionsButton.setImageResource(info.icon)
        contentBinding.actionsButton.contentDescription = info.title
    }


    private fun hasMultiColumns(): Boolean {
        if (!DeviceUtils.isDeviceTablet(this) || !DeviceUtils.isScreenTablet(this)) return false
        if (resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE) {
            return preferences.getBoolean("multi_column_tabs_landscape", resources.getBoolean(R.bool.default_multi_column_tabs_land))
        }
        return preferences.getBoolean("multi_column_tabs_portrait", resources.getBoolean(R.bool.default_multi_column_tabs_port))
    }


    private class AccountUpdatedListener(private val activity: HomeActivity) : OnAccountsUpdateListener {

        override fun onAccountsUpdated(accounts: Array<out Account>?) {
            activity.notifyAccountsChanged()
            activity.updateUnreadCount()
        }
    }

    private class UpdateUnreadCountTask(
            context: Context,
            private val preferences: SharedPreferences,
            private val readStateManager: ReadStateManager,
            indicator: TabPagerIndicator,
            private val tabs: Array<SupportTabSpec>
    ) : AsyncTask<Any, UpdateUnreadCountTask.TabBadge, SparseIntArray>() {

        private val activatedKeys = DataStoreUtils.getActivatedAccountKeys(context)
        private val contextRef = WeakReference(context)
        private val indicatorRef = WeakReference(indicator)

        override fun doInBackground(vararg params: Any): SparseIntArray {
            val result = SparseIntArray()
            val context = contextRef.get() ?: return result
            tabs.forEachIndexed { i, spec ->
                if (spec.type == null) {
                    publishProgress(TabBadge(i, -1))
                    return@forEachIndexed
                }
                when (spec.type) {
                    CustomTabType.HOME_TIMELINE -> {
                        val accountKeys = Utils.getAccountKeys(context, spec.args) ?: activatedKeys
                        val position = accountKeys.map {
                            val tag = Utils.getReadPositionTagWithAccount(ReadPositionTag.HOME_TIMELINE, it)
                            readStateManager.getPosition(tag)
                        }.fold(0L, Math::max)
                        val count = DataStoreUtils.getStatusesCount(context, preferences,
                                Statuses.CONTENT_URI, spec.args, Statuses.TIMESTAMP, position,
                                true, accountKeys, FilterScope.HOME)
                        result.put(i, count)
                        publishProgress(TabBadge(i, count))
                    }
                    CustomTabType.NOTIFICATIONS_TIMELINE -> {
                        val accountKeys = Utils.getAccountKeys(context, spec.args) ?: activatedKeys
                        val position = accountKeys.map {
                            val tag = Utils.getReadPositionTagWithAccount(ReadPositionTag.ACTIVITIES_ABOUT_ME, it)
                            readStateManager.getPosition(tag)
                        }.fold(0L, Math::max)
                        val count = DataStoreUtils.getInteractionsCount(context, preferences,
                                spec.args, accountKeys, position, Activities.TIMESTAMP,
                                FilterScope.INTERACTIONS)
                        result.put(i, count)
                        publishProgress(TabBadge(i, count))
                    }
                    CustomTabType.DIRECT_MESSAGES -> {
                        val accountKeys = Utils.getAccountKeys(context, spec.args) ?: activatedKeys
                        val projection = (Conversations.COLUMNS + Conversations.UNREAD_COUNT).map {
                            TwidereQueryBuilder.mapConversationsProjection(it)
                        }.toTypedArray()
                        val unreadHaving = Expression.greaterThan(Conversations.UNREAD_COUNT, 0)
                        val count = context.contentResolver.getUnreadMessagesEntriesCursorReference(projection,
                                accountKeys, extraHaving = unreadHaving)?.use { (cur) ->
                            return@use cur.count
                        } ?: -1
                        result.put(i, count)
                        publishProgress(TabBadge(i, count))
                    }
                    else -> {
                        publishProgress(TabBadge(i, -1))
                    }
                }
            }
            return result
        }

        override fun onPostExecute(result: SparseIntArray) {
            val indicator = indicatorRef.get() ?: return
            indicator.clearBadge()
            for (i in 0 until result.size()) {
                indicator.setBadge(result.keyAt(i), result.valueAt(i))
            }
        }

        override fun onProgressUpdate(vararg values: TabBadge) {
            val indicator = indicatorRef.get() ?: return
            for (value in values) {
                indicator.setBadge(value.index, value.count)
            }
        }

        internal class TabBadge(var index: Int, var count: Int)
    }

    class AutoRefreshConfirmDialogFragment : BaseDialogFragment() {
        override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
            val builder = AlertDialog.Builder(requireContext())
            builder.setTitle(R.string.auto_refresh)
            builder.setMessage(R.string.message_auto_refresh_confirm)
            builder.setPositiveButton(android.R.string.ok) { _, _ ->
                kPreferences[defaultAutoRefreshKey] = true
            }
            builder.setNegativeButton(R.string.action_no_thanks) { _, _ ->
                kPreferences[defaultAutoRefreshKey] = false
            }
            val dialog = builder.create()
            dialog.onShow { it.applyTheme() }
            return dialog
        }

        override fun onDismiss(dialog: DialogInterface) {
            kPreferences[defaultAutoRefreshAskedKey] = true
            super.onDismiss(dialog)
        }
    }

    companion object {
        private val HOME_AS_UP_ATTRS = intArrayOf(android.R.attr.homeAsUpIndicator)
    }


}
