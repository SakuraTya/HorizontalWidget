package org.sakuratya.horizontal;

import java.util.ArrayList;
import org.sakuratya.horizontal.HorizontalListView;
import org.sakuratya.horizontal.adapter.HAdapter;
import org.sakuratya.horizontal.ui.HListView;

import android.os.Bundle;
import android.app.Activity;
import android.view.Menu;

public class MainActivity extends Activity {

    private HListView mHListView;

    private ArrayList<String> testList = new ArrayList<String>();
    
	@Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        
        mHListView = (HListView) findViewById(R.id.h_list_view);
        
        for(int i=0; i<1000; i++) {
        	testList.add("item:" + i);
        }
        HAdapter adapter = new HAdapter(this, testList);
        mHListView.setAdapter(adapter);
    }

	
}
