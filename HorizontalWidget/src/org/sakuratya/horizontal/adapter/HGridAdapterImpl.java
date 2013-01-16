package org.sakuratya.horizontal.adapter;

import java.util.ArrayList;

import org.sakuratya.horizontal.R;
import org.sakuratya.horizontal.model.ItemList;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

public class HGridAdapterImpl extends HGridAdapter<ItemList> {
	
	private final static String TAG = "HGridAdapterImpl";
	
	private Context mContext;
	
	private int mSize = 0;
	
	public HGridAdapterImpl(Context context, ArrayList<ItemList> list) {
		mContext = context;
		if(list != null && list.size()>0) {
			mList = list;
			for(int i=0; i<list.size(); i++) {
				mSize += list.get(i).count;
			}
		}
	}

	@Override
	public int getCount() {
		return mSize;
	}

	@Override
	public String getItem(int position) {
		int size = 0;
		for(int i=0; i<mList.size(); i++) {
			final int sectionCount = mList.get(i).count;
			if(size +sectionCount > position) {
				int indexOfCurrentSection = position - size;
				return mList.get(i).objects.get(indexOfCurrentSection);
			}
			size += sectionCount;
		}
		return null;
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
			convertView = LayoutInflater.from(mContext).inflate(R.layout.list_view_item,null);
			holder = new Holder();
			holder.title = (TextView) convertView.findViewById(R.id.list_item_title);
			holder.preview = (ImageView) convertView.findViewById(R.id.list_item_preview_img);
			holder.quality = (ImageView) convertView.findViewById(R.id.list_item_quality_label);
			convertView.setTag(holder);
		} else {
			holder = (Holder) convertView.getTag();
		}
		String text = getItem(position);
		if(position==0 || position ==8 || position == 6 || position == 7) {
			holder.preview.setImageResource(R.drawable.selector);
			holder.title.setText("disabled:"+position);
		} else {
			holder.preview.setImageDrawable(null);
			holder.title.setText(text);
		}
		
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
		ImageView preview;
		ImageView quality;
	}

	@Override
	public String getLabelText(int sectionIndex) {
		return mList.get(sectionIndex).title;
	}
	
	public String additionStr = "";

	@Override
	public boolean isEnabled(int position) {
		if(position==0 || position ==8 || position == 6 || position == 7) {
			return false;
		} else {
			return true;
		}
	}
	
}
