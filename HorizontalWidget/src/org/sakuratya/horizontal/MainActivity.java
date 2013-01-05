package org.sakuratya.horizontal;

import java.util.ArrayList;

import org.sakuratya.horizontal.adapter.HGridAdapterImpl;
import org.sakuratya.horizontal.model.ItemList;
import org.sakuratya.horizontal.ui.HGridView;
import org.sakuratya.horizontal.ui.HGridView.OnScrollListener;

import android.app.Activity;
import android.graphics.Paint;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.TextView;

public class MainActivity extends Activity implements OnItemClickListener, OnScrollListener, OnItemSelectedListener {

    private static final String TAG = "MainActivity";

    private ArrayList<ItemList> mItemLists = new ArrayList<ItemList>(); 
    
    private HGridView mHGridView;
    
    HGridAdapterImpl mHGridViewAdapter;
	@Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        

        buildHGridView();

    }
	
	private void buildHGridView() {
		mHGridView = (HGridView) findViewById(R.id.h_list_view);
        
        for(int i=0; i < 10; i++) {
        	ItemList item = new ItemList();
        	item.objects = new ArrayList<String>();
        	item.title = "section"+i;
        	for(int j=0; j < 20; j++) {
        		item.objects.add("title: "+j);
        		item.count++;
        	}
        	mItemLists.add(item);
        }
        
        Paint labelTextPaint = new Paint();
        labelTextPaint.setTextSize(30);
        labelTextPaint.setColor(0xffffffff);
        
        
        mHGridViewAdapter = new HGridAdapterImpl(this, mItemLists);

        mHGridView.setAdapter(mHGridViewAdapter);
        mHGridView.setOnScrollListener(this);
        mHGridView.setOnItemClickListener(this);
        mHGridView.setOnItemSelectedListener(this);
        mHGridView.requestFocus();
	}

	@Override
	public void onScrollStateChanged(HGridView view, int scrollState) {
		String state = scrollState == OnScrollListener.SCROLL_STATE_IDLE ? "Idle" : "moving";
		Log.d(TAG, "scroll state changed! current state is " + state);
	}

	@Override
	public void onScroll(HGridView view, int firstVisibleItem,
			int visibleItemCount, int totalItemCount) {
//		Log.d(TAG, "OnScroll firstVisibleItem is "+ firstVisibleItem + " visibleItemCount has " + visibleItemCount);
	}

	@Override
	public void onItemClick(AdapterView<?> parent, View view, int position,
			long id) {
		Log.d(TAG, "click item is "+ position);
	}

	@Override
	public void onItemSelected(AdapterView<?> parent, View view, int position,
			long id) {
		Log.d(TAG, "item "+ position + " selected");
		TextView title = (TextView) view.findViewById(R.id.list_item_title);
		String titleText = title.getText().toString();
		Log.d(TAG, "item:"+position+" 's title is "+titleText);
	}

	@Override
	public void onNothingSelected(AdapterView<?> parent) {
		// TODO Auto-generated method stub
		Log.d(TAG, "nothing selected");
	}
	
}
