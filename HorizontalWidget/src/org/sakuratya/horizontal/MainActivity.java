package org.sakuratya.horizontal;

import java.util.ArrayList;

import org.sakuratya.horizontal.adapter.HAdapter;
import org.sakuratya.horizontal.adapter.HGridAdapterImpl;
import org.sakuratya.horizontal.model.ItemList;
import org.sakuratya.horizontal.ui.HGridView;

import android.app.Activity;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.widget.GridView;

public class MainActivity extends Activity {

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
        	for(int j=0; j < 200; j++) {
        		item.objects.add("title: "+j);
        	}
        	mItemLists.add(item);
        }
        
        HGridAdapterImpl adapter = new HGridAdapterImpl(this, mItemLists);
        
        mHGridView.setHorizontalSpacing(22);
        mHGridView.setVerticalSpacing(20);
        mHGridView.setRowHeight(200);
        mHGridView.setRows(3);
        
        mHGridView.setAdapter(adapter);
	}

	
}
