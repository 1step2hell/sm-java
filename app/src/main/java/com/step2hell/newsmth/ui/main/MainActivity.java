package com.step2hell.newsmth.ui.main;

import android.content.Intent;
import android.content.res.ColorStateList;
import android.content.res.Configuration;
import android.content.res.TypedArray;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.internal.NavigationMenuPresenter;
import android.support.design.internal.NavigationMenuView;
import android.support.design.widget.NavigationView;
import android.support.v4.content.ContextCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.util.TypedValue;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import com.step2hell.newsmth.R;
import com.step2hell.newsmth.ui.BaseActivity;
import com.step2hell.newsmth.ui.SettingsActivity;
import com.step2hell.newsmth.util.RxBus;

import java.lang.reflect.Field;

import io.reactivex.disposables.Disposable;
import io.reactivex.functions.Consumer;

/**
 * Todo: MVVM, Design main page.
 */
public class MainActivity extends BaseActivity {

    private DrawerLayout mDrawerLayout;
    private NavigationView mNavigationView;
    private ActionBarDrawerToggle mDrawerToggle;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        setupToolbar();
        initDrawerNavigation();
    }

    private void initDrawerNavigation() {
        mDrawerLayout = (DrawerLayout) findViewById(R.id.drawer);
//        mDrawerLayout.setDrawerShadow(R.drawable.drawer_shadow, Gravity.START);
//        mDrawerLayout.setScrimColor(Color.TRANSPARENT);

        mDrawerToggle = new ActionBarDrawerToggle(
                this, mDrawerLayout, mToolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        mDrawerLayout.addDrawerListener(mDrawerToggle);
        mDrawerToggle.syncState();

        mNavigationView = (NavigationView) findViewById(R.id.main_navigation);
        mNavigationView.setNavigationItemSelectedListener(new NavigationItemSelectedListener());
        mNavigationView.setItemIconTintList(ColorStateList.valueOf(ContextCompat.getColor(this, R.color.colorAccent)));
        setupNavigationDivider();

        // Test Rxbus
        RxBus.INSTANCE.publish(1);
        Disposable disposable = RxBus.INSTANCE.listen(Integer.class).subscribe(new Consumer<Integer>() {
            @Override
            public void accept(Integer integer) throws Exception {
                Log.e("Bob", "listen:" + integer);
            }
        });
        RxBus.INSTANCE.registerBus(this,disposable);
        RxBus.INSTANCE.publish(2);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        RxBus.INSTANCE.unregisterBus(this);
    }

    /**
     * 通过反射修改原生NavaigationView的divider（也可以不使用NavigationView，自己写个RecyclerView作为drawer）
     */
    @SuppressWarnings("RestrictTo")
    private void setupNavigationDivider() {

        /**
         * https://github.com/android/platform_frameworks_support/blob/master/design/res/layout/design_navigation_item.xml
         */
        TypedValue typedValue = new TypedValue();
        getTheme().resolveAttribute(android.R.attr.listPreferredItemPaddingLeft, typedValue, true);
        int[] attribute = new int[]{android.R.attr.listPreferredItemPaddingLeft};
        TypedArray array = obtainStyledAttributes(typedValue.resourceId, attribute);
        final int mNavigationMenuItemViewPaddingLeft = array.getDimensionPixelSize(0 /* index */, -1 /* default size */);
        array.recycle();

        /**
         * https://github.com/android/platform_frameworks_support/blob/master/design/src/android/support/design/internal/NavigationMenuItemView.java
         * mIconSize = context.getResources().getDimensionPixelSize(R.dimen.design_navigation_icon_size);
         */
        final int mNavigationMenuIconSize = MainActivity.this.getResources().getDimensionPixelSize(android.support.design.R.dimen.design_navigation_icon_size);

        /**
         * https://github.com/android/platform_frameworks_support/blob/master/design/res/layout/design_navigation_menu_item.xml
         */
        final int mNavigationMenuIconPadding = MainActivity.this.getResources().getDimensionPixelSize(android.support.design.R.dimen.design_navigation_icon_padding);

        try {
            Field fieldByPresenter = mNavigationView.getClass().getDeclaredField("mPresenter");
            fieldByPresenter.setAccessible(true);
            NavigationMenuPresenter menuPresenter = (NavigationMenuPresenter) fieldByPresenter.get(mNavigationView);
            Field fieldByMenuView = menuPresenter.getClass().getDeclaredField("mMenuView");
            fieldByMenuView.setAccessible(true);
            final NavigationMenuView mMenuView = (NavigationMenuView) fieldByMenuView.get(menuPresenter);
            mMenuView.addOnChildAttachStateChangeListener(new RecyclerView.OnChildAttachStateChangeListener() {
                @Override
                public void onChildViewAttachedToWindow(View view) {
                    RecyclerView.ViewHolder viewHolder = mMenuView.getChildViewHolder(view);
                    if (viewHolder != null && "SeparatorViewHolder".equals(viewHolder.getClass().getSimpleName()) && viewHolder.itemView != null) {
                        if (viewHolder.itemView instanceof FrameLayout) {
                            FrameLayout frameLayout = (FrameLayout) viewHolder.itemView;
                            View line = frameLayout.getChildAt(0);
//                            line.setBackgroundColor(ContextCompat.getColor(MainActivity.this, R.color.colorAccent)); // change color here, or reset background.
                            ViewGroup.LayoutParams params = line.getLayoutParams();
//                            params.height = 1; // change height here.
                            /**
                             * make divider alignLeft with normal menu title, so I compute the MarginStart.
                             */
                            ((FrameLayout.MarginLayoutParams) params).setMarginStart(mNavigationMenuItemViewPaddingLeft + mNavigationMenuIconSize + mNavigationMenuIconPadding);
                            line.setLayoutParams(params);
                        }
                    }
                }

                @Override
                public void onChildViewDetachedFromWindow(View view) {

                }
            });
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (NoSuchFieldException e) {
            e.printStackTrace();
        }
    }

    /* The click listner for item of NavigationView */
    private class NavigationItemSelectedListener implements NavigationView.OnNavigationItemSelectedListener {
        @Override
        public boolean onNavigationItemSelected(@NonNull MenuItem item) {
            selectItem(item.getItemId());
            return true;
        }

        private void selectItem(int id) {
            String title;
            switch (id) {
                case R.id.navigation_preference:
                    title = getString(R.string.navigation_preference);
                    break;
                case R.id.navigation_menu1:
                    title = getString(R.string.navigation_menu1);
                    break;
                case R.id.navigation_menu2:
                    title = getString(R.string.navigation_menu2);
                    break;
                case R.id.navigation_menu3:
                    title = getString(R.string.navigation_menu3);
                    break;
                case R.id.navigation_menu4:
                    title = getString(R.string.navigation_menu4);
                    break;
                case R.id.navigation_about:
                    title = getString(R.string.navigation_about);
                    break;
                default:
                    title = null;
                    break;
            }
            Log.e("Bob", "title:" + title);
            mDrawerLayout.closeDrawers();
            startActivity(new Intent(MainActivity.this, SettingsActivity.class));
        }
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        mDrawerToggle.onConfigurationChanged(newConfig);
    }


    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Pass the event to ActionBarDrawerToggle, if it returns
        // true, then it has handled the app icon touch event
        if (mDrawerToggle.onOptionsItemSelected(item)) {
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onBackPressed() {
        if (mDrawerLayout.isDrawerOpen(mNavigationView)) {
            mDrawerLayout.closeDrawer(mNavigationView);
        } else {
            super.onBackPressed();
        }
    }


    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        if (intent != null) {
            boolean isExit = intent.getBooleanExtra(TAG_EXIT, false);
            if (isExit) {
                firstTapTime = 0;
                this.finish();
            }
        }
    }

}
