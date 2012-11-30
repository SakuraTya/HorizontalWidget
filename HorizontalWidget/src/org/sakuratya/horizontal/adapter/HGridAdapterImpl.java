package org.sakuratya.horizontal.adapter;

import java.util.ArrayList;
import java.util.Arrays;

import org.sakuratya.horizontal.R;
import org.sakuratya.horizontal.model.ItemList;

import android.content.Context;
import android.text.TextUtils;
import android.util.Log;
import android.util.SparseArray;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

public class HGridAdapterImpl extends HGridAdapter<ItemList> {
	
	private final static String TAG = "HGridAdapterImpl";
	
	private Context mContext;
	
	private int mSize = 0;
	
	private SparseArray<String> mTrueArray;
	
	public HGridAdapterImpl(Context context, ArrayList<ItemList> list) {
		mContext = context;
		if(list != null && list.size()>0) {
			mTrueArray = new SparseArray<String>();
			mList = list;
			for(int i=0; i<list.size(); i++) {
				for(int j=0; j < list.get(i).objects.size(); j++) {
					mTrueArray.put(mSize++, list.get(i).objects.get(j));
				}
			}
		}
	}

	@Override
	public int getCount() {
		return mTrueArray.size();
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
			size += mList.get(i).objects.size();
			if(size > position) {
				return i;
			}
		}
		return 0;
	}
	
	@Override
	public int getSectionCount(int sectionIndex) {
		return mList.get(sectionIndex).objects.size();
	}
	
	static class Holder {
		TextView title;
	}

	@Override
	public String getLabelText(int sectionIndex) {
		String title = mList.get(sectionIndex).title;
		if(TextUtils.isEmpty(title)) {
			return null;
		}
		char[] labelText = new char[title.length() * 2 -1];
		int pos = 0;
		for(int i=0; i< title.length(); i++) {
			labelText[pos++] = title.charAt(i);
			if(pos<labelText.length) {
				labelText[pos++] = '\n';
			}
		}
		return String.valueOf(labelText);
	}
	
	public String additionStr = "";
	
	public void modifyDataSet() {
		for(int j=0; j<mList.size(); j++) {
			ItemList item = mList.get(j);
			int size = item.objects.size();
			item.objects = new ArrayList<String>();
			for(int k=0; k<size; k++) {
				item.objects.add(additionStr+"\n Object: " + k + "\n Section: "+j);
			}
			
		}
		mTrueArray = new SparseArray<String>();
		mSize=0;
		for(int i=0; i<mList.size(); i++) {
			for(int j=0; j < mList.get(i).objects.size(); j++) {
				mTrueArray.put(mSize++, mList.get(i).objects.get(j));
			}
		}
	}
	
}
