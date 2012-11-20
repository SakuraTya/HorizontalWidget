package org.sakuratya.horizontal.adapter;

import java.util.ArrayList;
import java.util.Arrays;

import org.sakuratya.horizontal.model.ItemList;

import android.content.Context;
import android.util.SparseArray;
import android.view.View;
import android.view.ViewGroup;

public class HGridAdapterImpl extends HGridAdapter<ItemList> {
	
	private Context mContext;
	
	private int mSize;
	
	private int[] mSectionIndexArray;	
	
	public HGridAdapterImpl(Context context, ArrayList<ItemList> list) {
		mContext = context;
		if(list != null && list.size()>0) {
			mSectionIndexArray = new int[list.size()];
			mList = new SparseArray<ItemList>(list.size());
			for(int i=0; i<list.size(); i++) {
				mSectionIndexArray[i] = mSize;
				mList.put(i, list.get(i));
				mSize+=list.get(i).objects.size() + 1;
			}
		}
	}

	@Override
	public int getCount() {
		return mSize;
	}

	@Override
	public Object getItem(int position) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public long getItemId(int position) {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public int getSectionIndex(int position) {
		int size = 0;
		for(int i=0; i<mList.size(); i++) {
			size += mList.get(i).objects.size() + 1;
			if(size > position) {
				return i;
			}
		}
		return 0;
	}

	@Override
	public int getSectionCount(int position) {
		return mList.get(position).objects.size();
	}

	@Override
	public boolean isEnabled(int position) {
		if(Arrays.binarySearch(mSectionIndexArray, position)<0) {
			return false;
		} else {
			return true;
		}
	}

}
