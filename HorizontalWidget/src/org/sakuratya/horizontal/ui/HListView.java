package org.sakuratya.horizontal.ui;

import android.content.Context;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.util.StateSet;
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
	
	/**
     * When arrow scrolling, need a certain amount of pixels to preview next
     * items.  This is usually the fading edge, but if that is small enough,
     * we want to make sure we preview at least this many pixels.
     */
    private static final int MIN_SCROLL_PREVIEW_PIXELS = 2;
    
    /**
     * When arrow scrolling, ListView will never scroll more than this factor
     * times the height of the list.
     */
    private static final float MAX_SCROLL_FACTOR = 0.33f;
    
    private Rect mSelectRect = new Rect();
    
    /**
     * The selection's left padding
     */
    int mSelectionLeftPadding = 0;

    /**
     * The selection's top padding
     */
    int mSelectionTopPadding = 0;

    /**
     * The selection's right padding
     */
    int mSelectionRightPadding = 0;

    /**
     * The selection's bottom padding
     */
    int mSelectionBottomPadding = 0;
    
    /**
     * The drawable used to draw the selector
     */
    Drawable mSelector;
    
    /**
     * The select child's view (from the adapter's getView) is enabled.
     */
    private boolean mIsChildViewEnabled;
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
		int amountToScroll = amountToScroll(direction, nextSelectedPosition);
		
		boolean needToRedraw = false;
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

	private int amountToScroll(int direction, int nextSelectedPosition) {
		final int listLeft = 0;
		final int listRight = getWidth();
		
		final int numChildren = getChildCount();
		if(direction == FOCUS_RIGHT) {
			int indexToMakeVisible = numChildren - 1;
			if(nextSelectedPosition != INVALID_POSITION) {
				indexToMakeVisible = nextSelectedPosition - mFirstPosition;
			}
			
			final int positionToMakeVisible = mFirstPosition + indexToMakeVisible;
			final View viewToMakeVisible = getChildAt(indexToMakeVisible);
			
			int goalRight = listRight;
			if(positionToMakeVisible < mAdapter.getCount() - 1) {
				goalRight -= getArrowScrollPreviewLength();
			}
			if(viewToMakeVisible.getRight() < goalRight) {
				return 0;
			}
			
			if(nextSelectedPosition != INVALID_POSITION && (goalRight - viewToMakeVisible.getLeft() > getMaxScrollAmount())) {
				// item already has enough of it visible, changing selection is good enough
				return 0;
			}
			int amountToScroll = viewToMakeVisible.getRight() - goalRight;
			if(mFirstPosition + numChildren == mAdapter.getCount()) {
				final int max = getChildAt(numChildren-1).getRight() - goalRight;
				amountToScroll = Math.min(amountToScroll, max);
			}
			return Math.min(getMaxScrollAmount(), amountToScroll);
		} else if(direction == FOCUS_LEFT) {
			int indexToMakeVisible = 0;
			if(nextSelectedPosition != INVALID_POSITION) {
				indexToMakeVisible = nextSelectedPosition - mFirstPosition;
			}
			
			final int positionToMakeVisible = mFirstPosition + indexToMakeVisible;
			final View viewToMakeVisible = getChildAt(indexToMakeVisible);
			
			int goalLeft = listLeft;
			if(positionToMakeVisible > 0) {
				goalLeft += getArrowScrollPreviewLength();
			}
			
			if(viewToMakeVisible.getLeft() > goalLeft) {
				// item is fully visible.
				return 0;
			}
			if(nextSelectedPosition != INVALID_POSITION && (viewToMakeVisible.getRight() - goalLeft > getMaxScrollAmount())) {
				// item already has enough of it visible, changing selection is good enough
				return 0;
			}
			
			int amountToScroll = goalLeft - viewToMakeVisible.getLeft();
			if(mFirstPosition == 0) {
				final int max = goalLeft - getChildAt(0).getLeft();
				amountToScroll = Math.min(amountToScroll, max);
			}
			return Math.min(getMaxScrollAmount(), amountToScroll);
			
		}
		return 0;
	}
	
	private int getArrowScrollPreviewLength() {
		return Math.max(MIN_SCROLL_PREVIEW_PIXELS, getHorizontalFadingEdgeLength());
	}
	 /**
     * @return The maximum amount a list view will scroll in response to
     *   an arrow event.
     */
    public int getMaxScrollAmount() {
        return (int) (MAX_SCROLL_FACTOR * (getRight() - getLeft()));
    }
    
    protected void positionSelector(int position, View sel) {
    	if(position!=INVALID_POSITION) {
    		mSelectedPosition = position;
    	}
    	final Rect selectRect = mSelectRect;
    	selectRect.set(sel.getLeft(), sel.getTop(), sel.getRight(), sel.getBottom());
    	final boolean isChildViewEnabled = mIsChildViewEnabled;
    	if(sel.isEnabled()!= isChildViewEnabled ) {
    		mIsChildViewEnabled = !isChildViewEnabled;
    		if(getSelectedItemPosition() != INVALID_POSITION) {
    			refreshDrawableState();
    		}
    	}
    }
    
    protected void positionSelector(int left, int top, int right, int bottom) {
    	mSelectRect.set(left - mSelectionLeftPadding, top - mSelectionTopPadding, right + mSelectionRightPadding, bottom + mSelectionBottomPadding);
    }
    
    /**
     * Indicates whether this view is in a state where the selector should be drawn. This will
     * happen if we have focus but are not in touch mode, or we are in the middle of displaying
     * the pressed state for an item.
     *
     * @return True if the selector should be shown
     */
    protected boolean shouldShowSelector() {
    	return (hasFocus() && !isInTouchMode());
    }
    protected void updateSelectorState() {
    	if(mSelector!=null) {
    		if(shouldShowSelector()) {
    			mSelector.setState(getDrawableState());
    		} else {
    			mSelector.setState(new int[] { 0 });
    		}
    	}
    }
    
    
	@Override
	protected void drawableStateChanged() {
		super.drawableStateChanged();
		updateSelectorState();
	}

	@Override
	public void requestLayout() {
		if(!mInLayout) {
			super.requestLayout();
		}
	}
	
	
}
