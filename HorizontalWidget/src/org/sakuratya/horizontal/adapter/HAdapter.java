package org.sakuratya.horizontal.adapter;

import java.util.ArrayList;

import org.sakuratya.horizontal.R;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

public class HAdapter extends BaseAdapter {
	
	private ArrayList<String> mList = new ArrayList<String>();

	private Context mContext;
	
	public HAdapter(Context context, ArrayList<String> list) {
		mContext = context;
		mList = list;
	}
	
	@Override
	public int getCount() {
		// TODO Auto-generated method stub
		return mList.size();
	}

	@Override
	public Object getItem(int position) {
		// TODO Auto-generated method stub
		return mList.get(position);
	}

	@Override
	public long getItemId(int position) {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public View getView(int position, View convertView, ViewGroup root) {
		Holder holder;
		if(convertView==null) {
			convertView = LayoutInflater.from(mContext).inflate(R.layout.item_view, null);
			holder = new Holder();
			convertView.setTag(holder);
		} else {
			holder = (Holder) convertView.getTag();
		}
		holder.title = (TextView) convertView.findViewById(R.id.item_title);
		holder.title.setText(mList.get(position));
		if(position % 2 ==0) {
			convertView.setBackgroundColor(0xFFfcfcfc);
		} else {
			convertView.setBackgroundColor(0xFF989898);
		}
		return convertView;
	}
	
	static class Holder {
		TextView title;
	}

}
