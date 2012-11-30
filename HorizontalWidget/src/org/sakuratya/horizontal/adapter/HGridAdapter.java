package org.sakuratya.horizontal.adapter;


import java.util.ArrayList;
import android.widget.BaseAdapter;

public abstract class HGridAdapter<E> extends BaseAdapter {
	
	protected ArrayList<E> mList;
	
	/**
	 * Be careful to change list, don't forget invoke setList to ensure other data's correct
	 * @return the list
	 */
	public ArrayList<E> getList() {
		return mList;
	}
	/**
	 * Derived class should override this method to ensure other data has changed with list change.
	 * Invoke this method will result to {@link HGridAdapter#notifyDataSetChanged()} being called.
	 * @param list
	 */
	public void setList(ArrayList<E> list) {
		mList = list;
		notifyDataSetChanged();
	}
	
	public abstract int getSectionIndex(int position);
	
	/**
	 * Return the section count by the sectionIndex.
	 * @param position is the section's index in {@link HGridAdapter#mList}
	 * @return the section count.
	 */
	public abstract int getSectionCount(int sectionIndex);

	public boolean hasSection() {
		return mList.size() > 1 ? true:false;
	}
	
	public int getTotalSectionNum() {
		return mList.size();
	}
	
	public abstract String getLabelText(int sectionIndex);
}
