package org.sakuratya.horizontal.adapter;


import java.util.ArrayList;

import android.widget.BaseAdapter;

public abstract class HGridAdapter<E> extends BaseAdapter {
	
	protected ArrayList<E> mList;
	
	public abstract int getSectionIndex(int position);
	
	/**
	 * Return the section count excluded separator specified by the position.
	 * @param position is the section's index in {@link HGridAdapter#mList}
	 * @return the section count excluded separator.
	 */
	public abstract int getSectionCount(int position);

	public boolean hasSection() {
		return mList.size() > 1 ? true:false;
	}
}
