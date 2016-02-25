package com.duapps.rec.main;

import android.support.design.widget.AppBarLayout;
import android.support.design.widget.CollapsingToolbarLayout;
import android.support.design.widget.CoordinatorLayout;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;

import com.duapps.rec.utils.LogHelper;

/**
 * Created by liyan36 on 2016-02-25.
 * 该类依赖于CoordinateLayout、CollapsingToolbarLayout、
 * AppBarLayout以及上方可折叠View和下方RecyclerView的对象
 * UI结构如下：
 * CoordinateLayout
 * ----RecyclerView // 下方RecyclerView，多用于展示数据列表
 * ----AppBarLayout
 * -------CollapsingToolbarLayout
 * ----------RecyclerView // 上方可折叠View，此处使用RecyclerView为例
 * ----------ToolbarLayout
 */
public class CollapsingToolBarLayoutHelper {

    private static final String TAG = "CollapLayoutHelper";

    private CoordinatorLayout mCoordinatorLayout;
    private AppBarLayout mAppBarLayout;
    private CollapsingToolbarLayout mCollapsingToolbarLayout;
    private RecyclerView mAboveCollapedRV;
    private RecyclerView mBelowRV;

    private RecyclerView.AdapterDataObserver mLocalVideoDataObserver;
    private int mInitialBelowRVTop;
    private int mLocalVideoItemLimits;
    private int mAutoExpandItemLimits; // 滑动到顶部自动展开的数据条目限制
    private boolean mOnScroll = true;

    public CollapsingLayoutHelper(CoordinatorLayout coordinatorLayout, AppBarLayout appBarLayout,
                                  CollapsingToolbarLayout collapsingToolbarLayout,
                                  RecyclerView aboveCollapsedRV, RecyclerView belowRV) {
        mCoordinatorLayout = coordinatorLayout;
        mCollapsingToolbarLayout = collapsingToolbarLayout;
        mAppBarLayout = appBarLayout;
        mCollapsingToolbarLayout = collapsingToolbarLayout;
        mAboveCollapedRV = aboveCollapsedRV;
        mBelowRV = belowRV;
    }

    /**
     * 初始化处理首页未满屏禁止滚动所需数据
     * note:该方法在Activity onCreate()方法时调用
     * 两种情况：
     * 1.当数据未满屏时，不需要折叠；
     * 2.默认使用系统的折叠；
     */
    public void attachActivity() {
        mCoordinatorLayout.getViewTreeObserver()
                .addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
                    @Override
                    public void onGlobalLayout() {
                        mCoordinatorLayout.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                        int screenBottom = mCoordinatorLayout.getMeasuredHeight();
                        LogHelper.d(TAG, "appbar top=" + mAppBarLayout.getTop()
                                + " " + mCollapsingToolbarLayout.getChildAt(1).getMeasuredHeight());
                        if (mBelowRV.getChildCount() > 0) { // 当下方展示的数据条数大于0时
                            if (mBelowRV.getChildAt(0) != null) {
                                int itemHeight = mBelowRV.getChildAt(0).getHeight();
                                mInitialBelowRVTop = ((ViewGroup) mBelowRV.getParent()).getTop();// TODO:
                                mLocalVideoItemLimits = (screenBottom - mInitialBelowRVTop) / itemHeight;
                                mAutoExpandItemLimits = (screenBottom - mAppBarLayout.getTop() - mCollapsingToolbarLayout.getChildAt(1).getMeasuredHeight()) / itemHeight;
                                handleScrollEvent();
                            }
                        } else {
                            toogleScrolling(false);
                            mOnScroll = false;
                        }
                    }
                });
        mLocalVideoDataObserver = new RecyclerView.AdapterDataObserver() {

            @Override
            public void onItemRangeInserted(int positionStart, int itemCount) {
                super.onItemRangeInserted(positionStart, itemCount);
                handleScrollEvent();
            }

            @Override
            public void onItemRangeRemoved(int positionStart, int itemCount) {
                super.onItemRangeRemoved(positionStart, itemCount);
                handleScrollEvent();
            }

        };
        mBelowRV.getAdapter().registerAdapterDataObserver(mLocalVideoDataObserver);
        // 当RecyclerView滑动到顶部时，展开AppBar
        mBelowRV.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(RecyclerView recyclerView, int newState) {
                if (newState == RecyclerView.SCROLL_STATE_IDLE) { // 滑动事件结束
                    if (recyclerView.getLayoutManager() instanceof LinearLayoutManager) {
                        LinearLayoutManager lm = (LinearLayoutManager) recyclerView.getLayoutManager();
                        // 判定recyclerview回到顶部
                        int itemCount = lm.getChildCount();
                        if (lm.findFirstCompletelyVisibleItemPosition() == 0 &&
                                itemCount > mAutoExpandItemLimits) {
                            mAppBarLayout.setExpanded(true);
                        }
                    } else {
                        LogHelper.w(TAG,
                                "The return values of getLayoutManger() "
                                        + "must be the instance of LinearLayoutManager! ");
                    }
                }
            }
        });

    }

    /**
     * 处理屏幕是否滚动
     */
    private void handleScrollEvent() {
        int itemCount = mBelowRV.getChildCount();
        // 此处针对初始视频数为0时，itemHeight = 0，limits == 0的情况
        if (mLocalVideoItemLimits == 0 && itemCount > 0) {
            attachActivity();
            return;
        }

        if (mLocalVideoItemLimits >= itemCount) { // forbid scroll
            if (mInitialBelowRVTop != mBelowRV.getTop() && mOnScroll) {
                // 当用户折叠了appbar，删除了position = mLocalVideoItemLimits +1 的video时，
                // 需要将appbar先展开再进行禁止滚动;此处只有之前为滚动状态且在临界处删除video时运行
                mAppBarLayout.setExpanded(true);
                mAppBarLayout.addOnOffsetChangedListener(new AppBarLayout.OnOffsetChangedListener() {
                    @Override
                    public void onOffsetChanged(AppBarLayout appBarLayout, int verticalOffset) {
                        if (verticalOffset == 0) {
                            toogleScrolling(false);
                            mOnScroll = false;
                            mAppBarLayout.removeOnOffsetChangedListener(this);
                        }
                    }
                });

            } else {
                if (mOnScroll) {
                    toogleScrolling(false);
                    mOnScroll = false;
                }
            }
        } else {
            if (!mOnScroll) {
                toogleScrolling(true);
                mOnScroll = true;
            }
        }
    }

    /**
     * 禁止本地视频数据未满屏时，可滚动
     *
     * @param onScroll true : enable scroll; false : unable scroll
     */
    private void toogleScrolling(boolean onScroll) {
        LogHelper.d(TAG, "turn on or off scroll : " + onScroll);
//        CollapsingToolbarLayout mToolbar = (CollapsingToolbarLayout) findViewById(R.id.collapsing_toolbar);

        AppBarLayout.LayoutParams toolbarLayoutParams = (AppBarLayout.LayoutParams)
                mCollapsingToolbarLayout.getLayoutParams();
        toolbarLayoutParams.setScrollFlags(onScroll
                ? AppBarLayout.LayoutParams.SCROLL_FLAG_SCROLL
                | AppBarLayout.LayoutParams.SCROLL_FLAG_EXIT_UNTIL_COLLAPSED : 0);
        mCollapsingToolbarLayout.setLayoutParams(toolbarLayoutParams);

        CoordinatorLayout.LayoutParams appBarLayoutParams = (CoordinatorLayout.LayoutParams)
                mAppBarLayout.getLayoutParams();
        appBarLayoutParams.setBehavior(onScroll ? new AppBarLayout.Behavior() : null);

        mAppBarLayout.setLayoutParams(appBarLayoutParams);
    }

    /**
     * 当关联的Activity destroy的时候，注销RecyclerView的DataObserver
     * 清除RecyclerView的ScrollListener
     */
    public void detachActivity() {
        if (mBelowRV.getAdapter() != null
                && mLocalVideoDataObserver != null) {
            mBelowRV.getAdapter().unregisterAdapterDataObserver(mLocalVideoDataObserver);
        }
        mBelowRV.clearOnScrollListeners();
    }

}
