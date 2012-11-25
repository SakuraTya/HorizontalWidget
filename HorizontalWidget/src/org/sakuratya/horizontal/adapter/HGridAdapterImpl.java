package org.sakuratya.horizontal.adapter;

import java.util.ArrayList;
import java.util.Arrays;

import org.sakuratya.horizontal.R;
import org.sakuratya.horizontal.model.ItemList;

import android.content.Context;
import android.util.SparseArray;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

public class HGridAdapterImpl extends HGridAdapter<ItemList> {
	
	private Context mContext;
	
	private int mSize;
	
	private SparseArray<String> mTrueArray;
	
	public HGridAdapterImpl(Context context, ArrayList<ItemList> list) {
		mContext = context;
		if(list != null && list.size()>0) {
			mSectionIndexArray = new int[list.size()];
			mTrueArray = new SparseArray<String>();
			mList = list;
			for(int i=0; i<list.size(); i++) {
				mSectionIndexArray[i] = mSize;
				mTrueArray.put(mSize++, "separator");
				for(int j=0; j < list.get(i).objects.size(); j++) {
					mTrueArray.put(mSize++, list.get(i).objects.get(j));
				}
			}
		}
	}

	@Override
	public int getCount() {
		return mSize;
	}

	@Override
	public String getItem(int position) {
		return mTrueArray.get(position);
	}

	@Override
	public long getItemId(int position) {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		Holder holder = null;
		if(convertView == null) {
			convertView = LayoutInflater.from(mContext).inflate(R.layout.item_view,null);
			holder = new Holder();
			holder.title = (TextView) convertView.findViewById(R.id.item_title);
			convertView.setTag(holder);
		} else {
			holder = (Holder) convertView.getTag();
		}
		String text = getItem(position);
		holder.title.setText(text);
		return convertView;
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
			return true;
		} else {
			return false;
		}
	}
	
	static class Holder {
		TextView title;
	}

}
