package org.sakuratya.horizontal.ui;

import android.content.Context;
import android.util.AttributeSet;
import android.view.KeyEvent;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListAdapter;

public class HListView extends AdapterView<ListAdapter> {
	
	protected boolean mDataChanged;
	
	protected boolean mInLayout;

	private int mSelectedPosition;
	private int mFirstPosition;

	protected ListAdapter mAdapter;
	
	public HListView(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
		// TODO Auto-generated constructor stub
	}

	public HListView(Context context, AttributeSet attrs) {
		super(context, attrs);
		// TODO Auto-generated constructor stub
	}

	public HListView(Context context) {
		super(context);
		// TODO Auto-generated constructor stub
	}

	@Override
	public ListAdapter getAdapter() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void setAdapter(ListAdapter adapter) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public View getSelectedView() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void setSelection(int position) {
		// TODO Auto-generated method stub
		
	}
	
	protected void layoutChildren() {
		
	}

	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		return commonKey(keyCode, 1, event);
	}

	@Override
	public boolean onKeyUp(int keyCode, KeyEvent event) {
		return commonKey(keyCode, 1, event);
	}
	
	private boolean commonKey(int keyCode, int repeatCount, KeyEvent event) {
		if (mDataChanged) {
            layoutChildren();
        }
		
		boolean handled = false;
        int action = event.getAction();
        if(action != KeyEvent.ACTION_UP) {
        	switch(action) {
            case KeyEvent.KEYCODE_DPAD_LEFT:
            	if(!handled) {
            		handled = arrowScroll(FOCUS_LEFT);
            	}
            	break;
            case KeyEvent.KEYCODE_DPAD_RIGHT:
            	if(!handled) {
            		handled = arrowScroll(FOCUS_RIGHT);
            	}
            	break;
            case KeyEvent.KEYCODE_DPAD_UP:
            	break;
            case KeyEvent.KEYCODE_DPAD_DOWN:
            	break;
            case KeyEvent.KEYCODE_DPAD_CENTER:
            	break;
            }
        }
        if(handled) {
        	return true;
        }
        switch (action) {
        case KeyEvent.ACTION_DOWN:
            return super.onKeyDown(keyCode, event);

        case KeyEvent.ACTION_UP:
            return super.onKeyUp(keyCode, event);

        case KeyEvent.ACTION_MULTIPLE:
            return super.onKeyMultiple(keyCode, repeatCount, event);

        default: // shouldn't happen
            return false;
        }
	}
	
	private boolean arrowScroll(int direction) {
		try {
			mInLayout = true;
			boolean handled = arrowScrollImpl(direction);
			//yet we have nothing to do with 'handled'
			return handled;
		} finally {
			mInLayout = false;
		}
	}
	
	private boolean arrowScrollImpl(int direction) {
		if(getChildCount()<=0) {
			return false;
		}
		
		View selectedView = getSelectedView();
		int selectedPos = mSelectedPosition;
		
		int nextSelectedPosition = lookForSelectablePositionOnScreen(direction);
		if(nextSelectedPosition != INVALID_POSITION) {
			
		}
		return false;
	}
	
	private int lookForSelectablePositionOnScreen(int direction) {
		final int firstPosition = mFirstPosition;
		if(direction==FOCUS_RIGHT) {
			int startPos = (mSelectedPosition != INVALID_POSITION) ? mSelectedPosition + 1 : firstPosition;
			if(startPos > mAdapter.getCount()) {
				return INVALID_POSITION;
			}
			if(startPos < firstPosition) {
				startPos = firstPosition;
			}
			
			final int lastVisiblePosition = getLastVisiblePosition();
			final ListAdapter adapter = getAdapter();
			for(int pos = startPos; pos <= lastVisiblePosition; pos++) {
				if(adapter.isEnabled(pos) && getChildAt(pos - firstPosition).getVisibility() == View.VISIBLE) {
					return pos;
				}
			}
		} else if(direction==FOCUS_LEFT) {
			int lastPos = firstPosition + getChildCount() - 1;
			int startPos = (mSelectedPosition != INVALID_POSITION) ? mSelectedPosition -1 : lastPos;
			if(startPos < 0 || startPos > mAdapter.getCount()) {
				return INVALID_POSITION;
			}
			if(startPos > lastPos) {
				startPos = lastPos;
			}
			final ListAdapter adapter = getAdapter();
			for(int pos = startPos; pos <= firstPosition; pos--) {
				if(adapter.isEnabled(pos) && getChildAt(pos - firstPosition).getVisibility() == View.VISIBLE) {
					return pos;
				}
			}
		}
		return INVALID_POSITION;
	}

	@Override
	public void requestLayout() {
		if(!mInLayout) {
			super.requestLayout();
		}
	}
	
	
}
