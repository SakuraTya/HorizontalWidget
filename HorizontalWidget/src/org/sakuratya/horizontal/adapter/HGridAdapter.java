package org.sakuratya.horizontal.adapter;


import android.util.SparseArray;
import android.widget.BaseAdapter;

public abstract class HGridAdapter<E> extends BaseAdapter {
	
	private SparseArray<E> mList;
	
	public abstract int getSectionIndex(int position);
	
	public abstract int getSectionCount(int position);

	public boolean hasSection() {
		return mList.size() > 1 ? true:false;
	}
}
