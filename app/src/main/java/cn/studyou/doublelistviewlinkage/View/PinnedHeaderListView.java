package cn.studyou.doublelistviewlinkage.View;

import android.content.Context;
import android.graphics.Canvas;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.AbsListView.OnScrollListener;
import android.widget.AdapterView;
import android.widget.HeaderViewListAdapter;
import android.widget.ListAdapter;
import android.widget.ListView;

import cn.studyou.doublelistviewlinkage.Adapter.SectionedBaseAdapter;

public class PinnedHeaderListView extends ListView implements OnScrollListener {

    private String TAG = this.getClass().getSimpleName();

    private OnScrollListener mOnScrollListener;

    public static interface PinnedSectionedHeaderAdapter {

        /**
         * 是否是头
         *
         * @param position item位置
         * @return
         */
        public boolean isSectionHeader(int position);

        /**
         * 获取该postion位置的大类item的position位置
         *
         * @param position item位置
         * @return
         */
        public int getSectionForPosition(int position);

        /**
         * 加载头布局
         *
         * @param section     大类的位置
         * @param convertView
         * @param parent
         * @return
         */
        public View getSectionHeaderView(int section, View convertView, ViewGroup parent);

        /**
         * 获取大类头item type
         *
         * @param section 大类位置
         * @return
         */
        public int getSectionHeaderViewType(int section);


        /**
         * 获取总item条目个数
         *
         * @return
         */
        public int getCount();

    }

    private PinnedSectionedHeaderAdapter mAdapter;

    /**
     * 当前位置header头
     */
    private View mCurrentHeader;

    /**
     * 当前位置头header 类型
     */
    private int mCurrentHeaderViewType = 0;
    private float mHeaderOffset;
    private boolean mShouldPin = true;

    /**
     * 当前头位置(大类的位置)
     */
    private int mCurrentSection = 0;
    private int mWidthMode;
    private int mHeightMode;

    public PinnedHeaderListView(Context context) {
        super(context);
        super.setOnScrollListener(this);//设置滚动监听
    }

    public PinnedHeaderListView(Context context, AttributeSet attrs) {
        super(context, attrs);
        super.setOnScrollListener(this);//设置滚动监听
    }

    public PinnedHeaderListView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        super.setOnScrollListener(this);//设置滚动监听
    }

    public void setPinHeaders(boolean shouldPin) {
        mShouldPin = shouldPin;
    }

    @Override
    public void setAdapter(ListAdapter adapter) {
        mCurrentHeader = null;
        mAdapter = (PinnedSectionedHeaderAdapter) adapter;
        super.setAdapter(adapter);
    }

    @Override
    public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount) {
        Log.d(TAG, "onScroll: firstVisibleItem:" + firstVisibleItem + ",visibleItemCount:" + visibleItemCount + ",totalItemCount:" + totalItemCount);
        if (mOnScrollListener != null) {
            mOnScrollListener.onScroll(view, firstVisibleItem, visibleItemCount, totalItemCount);
        }

        int headerViewsCount = getHeaderViewsCount();
        if (mAdapter == null || mAdapter.getCount() == 0 || !mShouldPin || (firstVisibleItem < headerViewsCount)) {
            mCurrentHeader = null;
            mHeaderOffset = 0.0f;
            for (int i = firstVisibleItem; i < firstVisibleItem + visibleItemCount; i++) {
                View header = getChildAt(i);
                if (header != null) {
                    header.setVisibility(VISIBLE);
                }
            }
            return;
        }

        firstVisibleItem -= headerViewsCount;
        Log.d(TAG, "onScroll: firstVisibleItem:" + firstVisibleItem + ",headerViewsCount:" + headerViewsCount);

        int section = mAdapter.getSectionForPosition(firstVisibleItem);//获取该postion位置的大类item的position位置
        int viewType = mAdapter.getSectionHeaderViewType(section);//大类头类型
        mCurrentHeader = getSectionHeaderView(section, mCurrentHeaderViewType != viewType ? null : mCurrentHeader);
        ensurePinnedHeaderLayout(mCurrentHeader);
        mCurrentHeaderViewType = viewType;

        mHeaderOffset = 0.0f;

        //可见页item是否是头
        for (int i = firstVisibleItem; i < firstVisibleItem + visibleItemCount; i++) {
            if (mAdapter.isSectionHeader(i)) {//是头
                View header = getChildAt(i - firstVisibleItem);
                float headerTop = header.getTop();
                float pinnedHeaderHeight = mCurrentHeader.getMeasuredHeight();
                header.setVisibility(VISIBLE);//设置头可见
                if (pinnedHeaderHeight >= headerTop && headerTop > 0) {
                    mHeaderOffset = headerTop - header.getHeight();
                } else if (headerTop <= 0) {
                    header.setVisibility(INVISIBLE);
                }
            }
        }

        invalidate();//刷新头
    }

    @Override
    public void onScrollStateChanged(AbsListView view, int scrollState) {
        if (mOnScrollListener != null) {
            mOnScrollListener.onScrollStateChanged(view, scrollState);
        }
    }

    /**
     * 获取头布局view
     *
     * @param section
     * @param oldView
     * @return
     */
    private View getSectionHeaderView(int section, View oldView) {
        boolean shouldLayout = section != mCurrentSection || oldView == null;

        View view = mAdapter.getSectionHeaderView(section, oldView, this);//加载头布局view
        if (shouldLayout) {
            // a new section, thus a new header. We should lay it out again
            ensurePinnedHeaderLayout(view);
            mCurrentSection = section;//当前头布局在大类中的位置
        }
        return view;
    }

    private void ensurePinnedHeaderLayout(View header) {
        if (header.isLayoutRequested()) {
            int widthSpec = MeasureSpec.makeMeasureSpec(getMeasuredWidth(), mWidthMode);//宽

            int heightSpec;
            ViewGroup.LayoutParams layoutParams = header.getLayoutParams();
            if (layoutParams != null && layoutParams.height > 0) {
                heightSpec = MeasureSpec.makeMeasureSpec(layoutParams.height, MeasureSpec.EXACTLY);//高
            } else {
                heightSpec = MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED);
            }
            header.measure(widthSpec, heightSpec);//测量
            header.layout(0, 0, header.getMeasuredWidth(), header.getMeasuredHeight());//从新布局头
        }
    }

    @Override
    protected void dispatchDraw(Canvas canvas) {
        super.dispatchDraw(canvas);
        if (mAdapter == null || !mShouldPin || mCurrentHeader == null)
            return;
        int saveCount = canvas.save();
        canvas.translate(0, mHeaderOffset);
        canvas.clipRect(0, 0, getWidth(), mCurrentHeader.getMeasuredHeight()); // needed
        // for
        // <
        // HONEYCOMB
        mCurrentHeader.draw(canvas);//绘制头布局
        canvas.restoreToCount(saveCount);
    }

    @Override
    public void setOnScrollListener(OnScrollListener l) {
        mOnScrollListener = l;
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);

        mWidthMode = MeasureSpec.getMode(widthMeasureSpec);
        mHeightMode = MeasureSpec.getMode(heightMeasureSpec);
    }

    public void setOnItemClickListener(OnItemClickListener listener) {
        super.setOnItemClickListener(listener);
    }

    public static abstract class OnItemClickListener implements AdapterView.OnItemClickListener {
        @Override
        public void onItemClick(AdapterView<?> adapterView, View view, int rawPosition, long id) {
            SectionedBaseAdapter adapter;
            if (adapterView.getAdapter().getClass().equals(HeaderViewListAdapter.class)) {
                HeaderViewListAdapter wrapperAdapter = (HeaderViewListAdapter) adapterView.getAdapter();
                adapter = (SectionedBaseAdapter) wrapperAdapter.getWrappedAdapter();
            } else {
                adapter = (SectionedBaseAdapter) adapterView.getAdapter();
            }
            int section = adapter.getSectionForPosition(rawPosition);
            int position = adapter.getPositionInSectionForPosition(rawPosition);

            if (position == -1) {
                onSectionClick(adapterView, view, section, id);
            } else {
                onItemClick(adapterView, view, section, position, id);
            }
        }

        public abstract void onItemClick(AdapterView<?> adapterView, View view, int section, int position, long id);

        public abstract void onSectionClick(AdapterView<?> adapterView, View view, int section, long id);

    }
}
