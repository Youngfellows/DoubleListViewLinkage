package cn.studyou.doublelistviewlinkage.Adapter;


import android.util.Log;
import android.util.SparseArray;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;

import cn.studyou.doublelistviewlinkage.View.PinnedHeaderListView;

public abstract class SectionedBaseAdapter extends BaseAdapter implements PinnedHeaderListView.PinnedSectionedHeaderAdapter {

    protected String TAG = this.getClass().getSimpleName();

    private static int HEADER_VIEW_TYPE = 0;
    private static int ITEM_VIEW_TYPE = 0;

    /**
     * Holds the calculated values of @{link getPositionInSectionForPosition}
     */
    private SparseArray<Integer> mSectionPositionCache;
    /**
     * Holds the calculated values of @{link getSectionForPosition}
     */
    private SparseArray<Integer> mSectionCache;
    /**
     * Holds the calculated values of @{link getCountForSection}
     */
    private SparseArray<Integer> mSectionCountCache;

    /**
     * Caches the item count
     */
    private int mCount;
    /**
     * Caches the section count
     */
    private int mSectionCount;

    public SectionedBaseAdapter() {
        super();
        mSectionCache = new SparseArray<Integer>();
        mSectionPositionCache = new SparseArray<Integer>();
        mSectionCountCache = new SparseArray<Integer>();
        mCount = -1;
        mSectionCount = -1;
    }

    @Override
    public void notifyDataSetChanged() {
        mSectionCache.clear();
        mSectionPositionCache.clear();
        mSectionCountCache.clear();
        mCount = -1;
        mSectionCount = -1;
        super.notifyDataSetChanged();
    }

    @Override
    public void notifyDataSetInvalidated() {
        mSectionCache.clear();
        mSectionPositionCache.clear();
        mSectionCountCache.clear();
        mCount = -1;
        mSectionCount = -1;
        super.notifyDataSetInvalidated();
    }

    @Override
    public final int getCount() {//总item个数
        if (mCount >= 0) {
            Log.d(TAG, "getCount: mCount:" + mCount);
            return mCount;
        }
        int count = 0;
        for (int i = 0; i < internalGetSectionCount(); i++) {//左边Item个数,大类个数
            count += internalGetCountForSection(i);//每个大类下,右边子Item个数
            count++; // for the header view,悬浮的头,每个大类
        }
        mCount = count;
        Log.d(TAG, "getCount: mCount:" + mCount);
        return count;
    }

    @Override
    public final Object getItem(int position) {//设置每个item的数据
        int section = getSectionForPosition(position);
        int position2 = getPositionInSectionForPosition(position);
        Log.d(TAG, "getItem: section:" + section + ",position2:" + position2 + ",position:" + position);
        return getItem(section, position2);
    }

    @Override
    public final long getItemId(int position) {
        Log.d(TAG, "getItemId: position:" + position);
        int section = getSectionForPosition(position);
        int position1 = getPositionInSectionForPosition(position);
        Log.d(TAG, "getItemId: section:" + section + ",position1:" + position1 + ",position:" + position);
        return getItemId(section, position1);
    }

    @Override
    public final View getView(int position, View convertView, ViewGroup parent) {
        int section = getSectionForPosition(position);//大类位置
        Log.d(TAG, "getView: section: " + section + ",position:" + position);
        if (isSectionHeader(position)) {//是头
            return getSectionHeaderView(section, convertView, parent);//加载大类头布局
        }
        int position1 = getPositionInSectionForPosition(position);//大类中子类的位置
        Log.d(TAG, "getView: section:" + section + ",position1:" + position1 + ",position:" + position);
        return getItemView(section, position1, convertView, parent);//加载子类item布局
    }

    @Override
    public final int getItemViewType(int position) {
        int section = getSectionForPosition(position);
        Log.d(TAG, "getItemViewType: section: " + section + ",position:" + position);
        if (isSectionHeader(position)) {//大类的头type=1
            return getItemViewTypeCount() + getSectionHeaderViewType(section);
        }
        int position1 = getPositionInSectionForPosition(position);
        Log.d(TAG, "getItemViewType: section:" + section + ",position1:" + position1 + ",position:" + position);
        return getItemViewType(section, position1);//不是头type=0
    }

    @Override
    public final int getViewTypeCount() {
        Log.d(TAG, "getViewTypeCount: ");
        return getItemViewTypeCount() + getSectionHeaderViewTypeCount();
    }

    public final int getSectionForPosition(int position) {//获取该postion位置的大类item的position位置
        // first try to retrieve values from cache
        Integer cachedSection = mSectionCache.get(position);//从map中获取该位置的大类的postion
        Log.d(TAG, "getSectionForPosition: cachedSection:" + cachedSection + ",position:" + position);
        if (cachedSection != null) {
            return cachedSection;
        }
        int sectionStart = 0;
        for (int i = 0; i < internalGetSectionCount(); i++) {//遍历左边大类的个数
            int sectionCount = internalGetCountForSection(i);//获取每个大类子item个数
            int sectionEnd = sectionStart + sectionCount + 1;//每个大类子类结束的item的postion位置
            Log.d(TAG, "getSectionForPosition: position:" + position + ",大类i:" + i + ",子item count:" + sectionCount + ",sectionStart:" + sectionStart + ",sectionEnd:" + sectionEnd);
            if (position >= sectionStart && position < sectionEnd) {
                mSectionCache.put(position, i);//保存该position位置的大类的position值
                Log.d(TAG, "getSectionForPosition: put(" + position + "," + i + ")");
                return i;
            }
            sectionStart = sectionEnd;//每个大类开始的item的postion位置
        }
        return 0;
    }

    public int getPositionInSectionForPosition(int position) {//获取该postion位置的子类item的position位置
        // first try to retrieve values from cache
        Integer cachedPosition = mSectionPositionCache.get(position);//从map中获取该位置的子类的postion位置
        Log.w(TAG, "getPositionInSectionForPosition: cachedPosition:" + cachedPosition + ",position:" + position);
        if (cachedPosition != null) {
            return cachedPosition;
        }
        int sectionStart = 0;
        for (int i = 0; i < internalGetSectionCount(); i++) {//遍历左边大类的个数
            int sectionCount = internalGetCountForSection(i);//获取每个大类子item个数
            int sectionEnd = sectionStart + sectionCount + 1;//每个大类子类结束的item的position位置
            Log.w(TAG, "getPositionInSectionForPosition: position:" + position + ",大类i:" + i + ",子item count:" + sectionCount + ",sectionStart:" + sectionStart + ",sectionEnd:" + sectionEnd);
            if (position >= sectionStart && position < sectionEnd) {
                int positionInSection = position - sectionStart - 1;
                mSectionPositionCache.put(position, positionInSection);//保存该位置下的该子类的postion位置
                Log.w(TAG, "getPositionInSectionForPosition: put(" + position + "," + positionInSection + ")");
                return positionInSection;//返回子类的position位置
            }
            sectionStart = sectionEnd;
        }
        return 0;
    }

    public final boolean isSectionHeader(int position) {//该位置是否是大类的头
        int sectionStart = 0;
        for (int i = 0; i < internalGetSectionCount(); i++) {//遍历大类
            if (position == sectionStart) {//当前位置是大类的开始postion位置
                return true;
            } else if (position < sectionStart) {
                return false;
            }
            sectionStart += internalGetCountForSection(i) + 1;//获取列表中大类开始的item位置position
        }
        return false;
    }

    public int getItemViewType(int section, int position) {
        return ITEM_VIEW_TYPE;
    }

    public int getItemViewTypeCount() {
        return 1;
    }

    public int getSectionHeaderViewType(int section) {
        return HEADER_VIEW_TYPE;
    }

    public int getSectionHeaderViewTypeCount() {
        return 1;
    }

    public abstract Object getItem(int section, int position);//设置每个item的数据

    public abstract long getItemId(int section, int position);//设置每个item的id

    public abstract int getSectionCount();//大类Item个数,左边Item的个数

    public abstract int getCountForSection(int section);//每个大类下的子item个数

    public abstract View getItemView(int section, int position, View convertView, ViewGroup parent);//加载子类item布局

    public abstract View getSectionHeaderView(int section, View convertView, ViewGroup parent);//加载头布局

    private int internalGetCountForSection(int section) {
        Integer cachedSectionCount = mSectionCountCache.get(section);//从map中获取每个大类下的子item个数
        if (cachedSectionCount != null) {
            return cachedSectionCount;
        }
        int sectionCount = getCountForSection(section);//返回每个大类下的子item个数
        mSectionCountCache.put(section, sectionCount);//缓存每个大类的子item个数
        return sectionCount;
    }

    private int internalGetSectionCount() {
        if (mSectionCount >= 0) {
            return mSectionCount;
        }
        mSectionCount = getSectionCount();//大类Item个数,左边Item的个数
        return mSectionCount;
    }

}
