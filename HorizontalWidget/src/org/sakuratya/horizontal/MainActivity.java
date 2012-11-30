package org.sakuratya.horizontal;

import java.util.ArrayList;

import org.sakuratya.horizontal.adapter.HAdapter;
import org.sakuratya.horizontal.adapter.HGridAdapterImpl;
import org.sakuratya.horizontal.model.ItemList;
import org.sakuratya.horizontal.ui.HGridView;
import org.sakuratya.horizontal.ui.HGridView.OnScrollListener;

import android.app.Activity;
import android.graphics.Paint;
import android.graphics.drawable.ColorDrawable;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.util.SparseArray;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.GridView;

public class MainActivity extends Activity implements OnItemClickListener, OnScrollListener, OnItemSelectedListener {

    private static final String TAG = "MainActivity";

	private HorizontalListView mHListView;

    private ArrayList<String> testList = new ArrayList<String>();
    
    private ArrayList<ItemList> mItemLists = new ArrayList<ItemList>(); 
    
    private HGridView mHGridView;
    
    private GridView mNormalGridView;
    
    private SimulatorTask mSimulatorTask;
    HGridAdapterImpl mHGridViewAdapter;
	@Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        
        
//        buildHList();
        
        buildHGridView();
        
//        buildNormalGrid();
        mSimulatorTask = new SimulatorTask();
        mSimulatorTask.execute();
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
        	item.title = "section"+i;
        	for(int j=0; j < 200; j++) {
        		item.objects.add("title: "+j);
        	}
        	mItemLists.add(item);
        }
        
        Paint labelTextPaint = new Paint();
        labelTextPaint.setTextSize(30);
        labelTextPaint.setColor(0xffffffff);
        
        
        mHGridViewAdapter = new HGridAdapterImpl(this, mItemLists);
        
//        mHGridView.setLabelTextPaint(labelTextPaint);
//        mHGridView.setLabelTextMargin(10, 10, 10, 10);
//        mHGridView.setLabelDrawableOffset(10, 10, 70, 800);
//        mHGridView.setLabelBackgroundDrawableOffset(0, 0, 100, 800);
//        mHGridView.setHorizontalSpacing(81);
//        mHGridView.setVerticalSpacing(2);
//        mHGridView.setRowHeight(252);
        mHGridView.setFadingEdgeLength(137);
        mHGridView.setHorizontalFadingEdgeEnabled(true);
//        mHGridView.setLabelDrawableResId(R.drawable.label);
//        mHGridView.setLabelBackgroundDrawableResId(R.drawable.label_bg);
//        mHGridView.setRows(3);
//        mHGridView.setSelector(R.drawable.selector);
        mHGridView.setAdapter(mHGridViewAdapter);
        Log.d(TAG, "Focusable: " + mHGridView.isFocusable());
        mHGridView.setFocusable(true);
        mHGridView.setOnScrollListener(this);
        mHGridView.setOnItemClickListener(this);
        mHGridView.setOnItemSelectedListener(this);
        mHGridView.requestFocus();
	}
	int mode = SimulatorTask.TASK_CHANGE_SELECTION;
	int count = 0;
	class SimulatorTask extends AsyncTask<Void, Void, Void> {
		
		public static final int TASK_CHANGE_SELECTION = 0;
		public static final int TASK_CHANGE_DATA = 1;
		
		
		
		

		private String[] addtionStrSet = new String[] {"Alpha", "Bravo", "Charlie", "Delta", "Echo", "Foxtrot", "Golf", "Hotel"};
		@Override
		protected Void doInBackground(Void... params) {
			try {
				for(int i=0; i<5; i++) {
					Log.d(TAG, "count down: " + i);
					Thread.sleep(1000);
					if(i==2 && mode == TASK_CHANGE_DATA) {
						mHGridViewAdapter.additionStr = addtionStrSet[count-1];
						mHGridViewAdapter.modifyDataSet();
						Log.d(TAG, "DataSet has changed");
					}	
				}
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			return null;
		}

		@Override
		protected void onPostExecute(Void result) {
			if(mode==TASK_CHANGE_DATA) {
				mHGridView.getAdapter().notifyDataSetChanged();
				mode = TASK_CHANGE_SELECTION;
			}else if(mode==TASK_CHANGE_SELECTION) {
				int position = (int) (Math.random() * (mHGridViewAdapter.getCount() -1));
				mHGridView.setSelection(position);
				mode = TASK_CHANGE_DATA;
			}
			count++;
			if(count<7) {
				mSimulatorTask = new SimulatorTask();
				mSimulatorTask.execute();
			}
			super.onPostExecute(result);
		}
		
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
		mSimulatorTask.cancel(true);
		Log.d(TAG, "click item is "+ position);
	}

	@Override
	public void onItemSelected(AdapterView<?> parent, View view, int position,
			long id) {
		Log.d(TAG, "item "+ position + " selected");
	}

	@Override
	public void onNothingSelected(AdapterView<?> parent) {
		// TODO Auto-generated method stub
		Log.d(TAG, "nothing selected");
	}
	
}
