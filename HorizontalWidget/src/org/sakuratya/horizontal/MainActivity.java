package org.sakuratya.horizontal;

import java.util.ArrayList;

import org.sakuratya.horizontal.adapter.HAdapter;
import org.sakuratya.horizontal.adapter.HGridAdapterImpl;
import org.sakuratya.horizontal.model.ItemList;
import org.sakuratya.horizontal.ui.HGridView;

import android.app.Activity;
import android.graphics.drawable.ColorDrawable;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.widget.GridView;

public class MainActivity extends Activity {

    private static final String TAG = "MainActivity";

	private HorizontalListView mHListView;

    private ArrayList<String> testList = new ArrayList<String>();
    
    private ArrayList<ItemList> mItemLists = new ArrayList<ItemList>(); 
    
    private HGridView mHGridView;
    
    private GridView mNormalGridView;
    
	@Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        
        
//        buildHList();
        
        buildHGridView();
        
//        buildNormalGrid();
        new SimulatorTask().execute();
    }
	
	private void buildNormalGrid() {
		mNormalGridView = (GridView) findViewById(R.id.normal_gridview);
		for(int i=0; i<1000; i++) {
        	testList.add("item:" + i);
        }
		HAdapter adapter = new HAdapter(this, testList);
		mNormalGridView.setAdapter(adapter);
	}

	private void buildHList() {
		mHListView = (HorizontalListView) findViewById(R.id.h_list_view);
        
        for(int i=0; i<1000; i++) {
        	testList.add("item:" + i);
        }
        HAdapter adapter = new HAdapter(this, testList);
        mHListView.setAdapter(adapter);
        mHListView.setFocusable(true);
        ColorDrawable c = new ColorDrawable(0xffff0000);
//        mHListView.setSelector(c);
	}
	
	private void buildHGridView() {
		mHGridView = (HGridView) findViewById(R.id.h_list_view);
        
        for(int i=0; i < 10; i++) {
        	ItemList item = new ItemList();
        	item.objects = new ArrayList<String>();
        	item.title = ""+i;
        	for(int j=0; j < 20; j++) {
        		item.objects.add("title: "+j);
        	}
        	mItemLists.add(item);
        }
        
        HGridAdapterImpl adapter = new HGridAdapterImpl(this, mItemLists);
        
        mHGridView.setHorizontalSpacing(81);
        mHGridView.setVerticalSpacing(2);
        mHGridView.setRowHeight(252);
        mHGridView.setFadingEdgeLength(137);
        mHGridView.setHorizontalFadingEdgeEnabled(true);
        mHGridView.setLabelDrawableResId(R.drawable.label);
        mHGridView.setLabelBackgroundDrawableResId(R.drawable.label_bg);
        mHGridView.setRows(3);
        mHGridView.setSelector(R.drawable.selector);
        mHGridView.setAdapter(adapter);
        mHGridView.setFocusable(true);
        Log.d(TAG, "Focusable: " + mHGridView.isFocusable());
        mHGridView.requestFocus();
	}

	class SimulatorTask extends AsyncTask<Void, Void, Void> {

		@Override
		protected Void doInBackground(Void... params) {
			try {
				for(int i=0; i<5; i++) {
					Log.d(TAG, "count down: " + i);
					Thread.sleep(1000);
					
				}
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			return null;
		}

		@Override
		protected void onPostExecute(Void result) {
			mHGridView.setSelection(190);
			super.onPostExecute(result);
		}
		
	}
	
}
