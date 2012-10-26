package org.sakuratya.horizontal.ui;

import java.util.ArrayList;

import android.content.Context;
import android.database.DataSetObserver;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ListAdapter;

public class HListView extends AdapterView<ListAdapter> {
	/**
     * Used to indicate a no preference for a position type.
     */
    static final int NO_POSITION = -1;
    
    /**
     * Regular layout - usually an unsolicited layout from the view system
     */
    static final int LAYOUT_NORMAL = 0;

    /**
     * Show the first item
     */
    static final int LAYOUT_FORCE_TOP = 1;

    /**
     * Force the selected item to be on somewhere on the screen
     */
    static final int LAYOUT_SET_SELECTION = 2;

    /**
     * Show the last item
     */
    static final int LAYOUT_FORCE_BOTTOM = 3;

    /**
     * Make a mSelectedItem appear in a specific location and build the rest of
     * the views from there. The top is specified by mSpecificTop.
     */
    static final int LAYOUT_SPECIFIC = 4;

    /**
     * Layout to sync as a result of a data change. Restore mSyncPosition to have its top
     * at mSpecificTop
     */
    static final int LAYOUT_SYNC = 5;

    /**
     * Layout as a result of using the navigation keys
     */
    static final int LAYOUT_MOVE_SELECTION = 6;
	
	protected boolean mDataChanged;
	
	protected boolean mInLayout;

	private int mSelectedPosition;
	
	private int mNextSelectedPosition;
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
    
    protected RecycleBin mRecycler = new RecycleBin();
    
    final boolean[] mIsScrap = new boolean[1];
    /**
     * Subclasses must retain their measure spec from onMeasure() into this member
     */
    int mHeightMeasureSpec = 0;
    
    private int mSelectedLeft;
    
    private boolean mDrawSelectorOnTop = false;
    
    private AdapterDataSetObserver mDataSetObserver;
    
    /**
     * This view's padding
     */
    protected Rect mListPadding = new Rect();
    
    protected int mLayoutMode;
    
	public HListView(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
	}

	public HListView(Context context, AttributeSet attrs) {
		super(context, attrs);
	}

	public HListView(Context context) {
		super(context);
	}

	@Override
	public ListAdapter getAdapter() {
		return mAdapter;
	}
	
	public static class LayoutParams extends ViewGroup.LayoutParams {
		
		public int scrappedFromPosition;

		public LayoutParams(Context arg0, AttributeSet arg1) {
			super(arg0, arg1);
		}

		public LayoutParams(int arg0, int arg1) {
			super(arg0, arg1);
		}

		public LayoutParams(android.view.ViewGroup.LayoutParams arg0) {
			super(arg0);
		}
		
	}

	@Override
	public void setAdapter(ListAdapter adapter) {
		if(mAdapter!=null && mDataSetObserver!=null) {
			mAdapter.unregisterDataSetObserver(mDataSetObserver);
		}
		resetList();
		mRecycler.clear();
		if(mAdapter!=null) {
			mDataSetObserver = new AdapterDataSetObserver();
			int position = lookForSelectablePosition(0);
			setSelectedPositionInt(position);
		}
		requestLayout();
	}

	@Override
	public View getSelectedView() {
		if(mAdapter.getCount() > 0 && mSelectedPosition > 0) {
			return getChildAt(mSelectedPosition);
		} else {
			return null;
		}
	}

	@Override
	public void setSelection(int position) {
		// TODO Auto-generated method stub
		
	}
	
	@Override
	protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
		super.onMeasure(widthMeasureSpec, heightMeasureSpec);
		int widthMode = MeasureSpec.getMode(widthMeasureSpec);
		int widthSize = MeasureSpec.getSize(widthMeasureSpec);
		int heightMode = MeasureSpec.getMode(heightMeasureSpec);
		int heightSize = MeasureSpec.getSize(heightMeasureSpec);
		
		int childWidth = 0;
		int childHeight = 0;
		final int itemCount = mAdapter ==null ? 0: mAdapter.getCount();
		if(itemCount > 0 && (widthMode==MeasureSpec.UNSPECIFIED || heightMode == MeasureSpec.UNSPECIFIED)){
			final View child = obtainView(0, mIsScrap);
			measureScrapChild(child, 0, heightMeasureSpec);
			childWidth = child.getMeasuredWidth();
			childHeight = child.getMeasuredHeight();
			mRecycler.addScrapView(child, -1);
		}
		
		if(heightMode == MeasureSpec.UNSPECIFIED) {
			heightSize = mListPadding.top + mListPadding.bottom + childHeight;
		}
		
		if(widthMode == MeasureSpec.UNSPECIFIED) {
			widthSize = mListPadding.left + mListPadding.right + getHorizontalFadingEdgeLength() * 2;
		}
		
		if(widthMode == MeasureSpec.AT_MOST) {
			widthSize = measureWidthOfChildren(widthMeasureSpec, 0, NO_POSITION, heightSize, -1);
		}
	}
	
	private void measureScrapChild(View child, int position, int heightMeasureSpec) {
		HListView.LayoutParams p = (HListView.LayoutParams) child.getLayoutParams();
		if(p==null) {
			p = new HListView.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.MATCH_PARENT);
			child.setLayoutParams(p);
		}
		int childHeightSpec = ViewGroup.getChildMeasureSpec(heightMeasureSpec, mListPadding.top + mListPadding.bottom, p.height);
		int lpWidth = p.width;
		int childWidthSpec;
		if(lpWidth > 0) {
			childWidthSpec = MeasureSpec.makeMeasureSpec(lpWidth, MeasureSpec.EXACTLY);
		} else {
			childWidthSpec = MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED);
		}
		child.measure(childWidthSpec, childHeightSpec);
	}

	private final int measureWidthOfChildren(int heightMeasureSpec, int startPosition, int endPosition, final int maxWidth, int disallowPartialChildPosition) {
		final ListAdapter adapter = mAdapter;
		if(adapter==null) {
			return mListPadding.left + mListPadding.right;
		}
		int returnedWidth = mListPadding.left + mListPadding.right;
		// The previous height value that was less than maxHeight and contained
		// no partial children
        int prevHeightWithoutPartialChild = 0;
        int i;
        View child;
        // mItemCount - 1 since endPosition parameter is inclusive
        endPosition = (endPosition == NO_POSITION) ? adapter.getCount() - 1 : endPosition;
        final RecycleBin recycleBin = mRecycler;
        final boolean[] isScrap = mIsScrap;
        for(i=startPosition; i< endPosition; ++i) {
        	child = obtainView(i, isScrap);
        	measureScrapChild(child, i, heightMeasureSpec);
        	recycleBin.addScrapView(child, -1);
        	returnedWidth += child.getMeasuredWidth();
        	if(returnedWidth > maxWidth) {
        		return (disallowPartialChildPosition > 0) 
        				&& (i>disallowPartialChildPosition) 
        				&& (prevHeightWithoutPartialChild > 0) 
        				&& (returnedWidth != maxWidth)?prevHeightWithoutPartialChild:maxWidth;
        	}
        	
        	if ((disallowPartialChildPosition >= 0) && (i >= disallowPartialChildPosition)) {
                prevHeightWithoutPartialChild = returnedWidth;
            }
        }
        return returnedWidth;
	}
	
	@Override
	protected void onLayout(boolean changed, int left, int top, int right,
			int bottom) {
		super.onLayout(changed, left, top, right, bottom);
		final Rect listPadding = mListPadding;
		listPadding.left = mSelectionLeftPadding + getPaddingLeft();
		listPadding.right = mSelectionRightPadding + getPaddingRight();
		listPadding.top = mSelectionTopPadding + getPaddingTop();
		listPadding.bottom = mSelectionBottomPadding + getPaddingBottom();
		layoutChildren();
	}

	protected void layoutChildren() {
		if(!mInLayout) {
			mInLayout = true;
		} else {
			return;
		}
		
		try {
			invalidate();
			if(mAdapter==null) {
				resetList();
				return;
			}
			int childrenLeft = mListPadding.left;
			int childrenRight = getRight() - getLeft() - mListPadding.right;
			
			int childrenCount = getChildCount();
			int index = 0;
			int delta = 0;
			
			View sel;
            View oldSel = null;
            View oldFirst = null;
            View newSel = null;
            
            View focusLayoutRestoreView = null;
            																																																																																															
            switch(mLayoutMode) {
            case LAYOUT_MOVE_SELECTION:
            default:
            	index = mSelectedPosition - mFirstPosition;
            	if(index>0 && index < getChildCount()) {
            		oldSel = getChildAt(index);
            	}
            	// Remember the previous first child
            	oldFirst = getChildAt(0);
            	if(mNextSelectedPosition >= 0) {
            		delta = mNextSelectedPosition - mSelectedPosition;
            	}
            	// Caution: newSel might be null
                newSel = getChildAt(index + delta);
            }
            boolean dataChanged = mDataChanged;
            if (dataChanged) {
//                handleDataChanged();
            }
            
            if(mAdapter.getCount()==0) {
            	resetList();
//            	invokeOnItemScrollListener();
                return;
            }
            setSelectedPositionInt(mNextSelectedPosition);
            
            // Pull all children into the RecycleBin.
            // These views will be reused if possible
            final int firstPosition = mFirstPosition;
            final RecycleBin recycleBin = mRecycler;
            
            // Don't put header or footer views into the Recycler. Those are
            // already cached in mHeaderViews;
            if (dataChanged) {
                for (int i = 0; i < childrenCount; i++) {
                    recycleBin.addScrapView(getChildAt(i), firstPosition+i);
                }
            } else {
                recycleBin.fillActiveViews(childrenCount, firstPosition);
            }
            
            // Clear out old views
            detachAllViewsFromParent();
            
            switch(mLayoutMode) {
            case LAYOUT_MOVE_SELECTION:
//            	sel = moveSelection(oldSel, newSel, delta, childrenTop, childrenBottom);
                break;
            default:
            	if(childrenCount==0) {
            		final int position = lookForSelectablePosition(0);
                    setSelectedPositionInt(position);
                    sel = fillFromLeft(childrenLeft);
            	} else {
            		if (mSelectedPosition >= 0 && mSelectedPosition < mAdapter.getCount()) {
                        sel = fillSpecific(mSelectedPosition,
                                oldSel == null ? childrenLeft : oldSel.getTop());
                    } else if (mFirstPosition < mAdapter.getCount()) {
                        sel = fillSpecific(mFirstPosition,
                                oldFirst == null ? childrenLeft : oldFirst.getTop());
                    } else {
                        sel = fillSpecific(0, childrenLeft);
                    }
            	}
            }
            
            // Flush any cached views that did not get reused above
            recycleBin.scrapActiveViews();
            
            
		} finally {
			mInLayout = false;
		}
	}
	
	/**
     * Make sure views are touching the left or right edge, as appropriate for
     * our gravity
     */
    private void adjustViewsLeftOrRight() {
    	final int childCount = getChildCount();
    	int delta;
    	
    	if(childCount > 0) {
    		View child;
    		// Uh-oh -- we came up short. Slide all views up to make them
            // align with the top
            child = getChildAt(0);
            delta = child.getLeft() - mListPadding.left;
            if (delta < 0) {
                // We only are looking to see if we are too low, not too high
                delta = 0;
            }
            if (delta != 0) {
                offsetChildrenLeftAndRight(-delta);
            }
    	}
    }
    
    /**
     * Check if we have dragged the bottom of the list too Left (we have pushed the
     * top element off the top of the screen when we did not need to). Correct by sliding
     * everything back down.
     *
     * @param childCount Number of children
     */
    private void correctTooLeft(int childCount) {
    	// First see if the last item is visible. If it is not, it is OK for the
        // top of the list to be pushed up.
    	int lastPosition = mFirstPosition + childCount - 1;
    	if(lastPosition == mAdapter.getCount() - 1 && childCount > 0) {
    		// Get last child
    		View lastChild = getChildAt(childCount -1);
    		
    		// Get the right edge of last child
    		final int lastRight = lastChild.getRight();
    		
    		// This is right end of our draw area
    		final int end = (getRight() - getLeft()) - mListPadding.right;
    		
    		// This is how far we should draw right edge of the last child from the right edge of draw area
    		int rightOffset = end - lastRight;
    		
    		View first = getChildAt(0);
    		final int firstLeft = first.getLeft();
    		
    		// Make sure we are 1) Too left, and 2) Either there are more rows above the
            // first row or the first row is scrolled off the top of the drawable area
    		if(rightOffset > 0 && (mFirstPosition > 0 || firstLeft < mListPadding.left)) {
    			if(mFirstPosition ==0) {
    				// Don't pull the top too far down
    				rightOffset = Math.min(rightOffset, mListPadding.top - firstLeft);
    			}
    			// Move everything down
				offsetChildrenLeftAndRight(rightOffset);
				
				if(mFirstPosition > 0) {
					// Fill gap that was opened before mFirstPosition with more columns, if possible.
					fillLeft(mFirstPosition, firstLeft);
					// Close up the remaining gap
					adjustViewsLeftOrRight();
				}
    		}
    	}
    }
	/**
     * Put a specific item at a specific location on the screen and then build
     * up and down from there.
     *
     * @param position The reference view to use as the starting point
     * @param left Pixel offset from the left of this view to the left of the
     *        reference view.
     *
     * @return The selected view, or null if the selected view is outside the
     *         visible area.
     */
	private View fillSpecific(int position, int left) {
		boolean tempIsSelected = position == mSelectedPosition;
		View temp = makeAndAddView(position, left, true, mListPadding.top, tempIsSelected);
		 // Possibly changed again in fillLeft if we add rows before this one.
        mFirstPosition = position;
        
        View before;
        View after;
        
        before = fillLeft(position-1, temp.getLeft());
        // This will correct for the top of the first view not touching the top of the list
        adjustViewsLeftOrRight();
        after = fillRight(position+1, temp.getRight());
        int childCount = getChildCount();
        if(childCount > 0) {
        	correctTooLeft(childCount);
        }
        
        if(tempIsSelected) {
        	return temp;
        } else if(before != null) {
        	return before;
        } else {
        	return after;
        }
	}
	
	/**
     * Obtain the view and add it to our list of children. The view can be made
     * fresh, converted from an unused view, or used as is if it was in the
     * recycle bin.
     *
     * @param position Logical position in the list
     * @param x Left or right edge of the view to add
     * @param flow If flow is true, align left edge to x If false, align right
     *        edge to x.
     * @param childrenTop Top edge where children should be positioned
     * @param selected Is this position selected?
     * @return View that was added
     */
	private View makeAndAddView(int position, int x, boolean flow, int childrenTop,
            boolean selected) {
        View child;


        if (!mDataChanged) {
            // Try to use an exsiting view for this position
            child = mRecycler.getActiveView(position);
            if (child != null) {

                // Found it -- we're using an existing child
                // This just needs to be positioned
                setupChild(child, position, x, flow, childrenTop, selected, true);

                return child;
            }
        }

        // Make a new view for this position, or convert an unused view if possible
        child = obtainView(position, mIsScrap);

        // This needs to be positioned and measured
        setupChild(child, position, x, flow, childrenTop, selected, mIsScrap[0]);

        return child;
    }
	
	private View fillFromLeft(int nextLeft) {
		mFirstPosition = Math.min(mFirstPosition, mSelectedPosition);
		mFirstPosition = Math.min(mFirstPosition, mAdapter.getCount() - 1);
		if (mFirstPosition < 0) {
            mFirstPosition = 0;
        }
		return fillRight(mFirstPosition, nextLeft);
	}
	
	/**
     * Fills the list from pos down to the end of the list view.
     *
     * @param pos The first position to put in the list
     *
     * @param nextLeft The location where the left of the item associated with pos
     *        should be drawn
     *
     * @return The view that is currently selected, if it happens to be in the
     *         range that we draw.
     */
	private View fillRight(int pos, int nextLeft) {
		View selectedView = null;
		
		int end = getRight() - getLeft();
		
		while(nextLeft < end && pos > mAdapter.getCount()) {
			boolean selected = pos == mSelectedPosition;
			View child = makeAndAddView(pos, nextLeft, true, 0, selected);
			
			nextLeft = child.getRight();
			
			if(selected) {
				selectedView = child;
			}
			pos++;
		}
		return selectedView;
	}
	
	private View fillLeft(int pos, int nextLeft) {
		View selectedView = null;
		int end = 0;
		return null;
	}
	
	private int lookForSelectablePosition(int position) {
		final ListAdapter adapter= mAdapter;
		final int count = adapter.getCount();
		position = Math.max(0, position);
        while (position < count && !adapter.isEnabled(position)) {
            position++;
        }
        if (position < 0 || position >= count) {
            return INVALID_POSITION;
        }
		return position;
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
			setSelectedPositionInt(nextSelectedPosition);
			needToRedraw = true;
		}
		
		if(amountToScroll > 0) {
			scrollListItemBy(direction==FOCUS_LEFT? -amountToScroll : amountToScroll);
			needToRedraw = true;
		}
		
		if(needToRedraw) {
			if(selectedView!=null) {
				positionSelector(selectedPos, selectedView);
				mSelectedLeft = selectedView.getLeft();
				if (!awakenScrollBars()) {
	                invalidate();
	            }
				return true;
			}
		}
		return false;
	}
	
	private void setSelectedPositionInt(int position) {
		mSelectedPosition = position;
	}
	
	private void setNextSelectedPositionInt(int position) {
		mNextSelectedPosition = position;
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
		final int listLeft = mListPadding.left;
		final int listRight = getWidth() - mListPadding.right;
		
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

	
	/**
     * Scroll the children by amount, adding a view at the end and removing
     * views that fall off as necessary.
     *
     * @param amount The amount (positive or negative) to scroll.
     */
	private void scrollListItemBy(int amount) {
		offsetChildrenLeftAndRight(amount);
		final int listRight = getWidth() - mListPadding.right;
		final int listLeft = mListPadding.left;
		final HListView.RecycleBin recycleBin = mRecycler;
		
		if(amount < 0) {
			int numChildren = getChildCount();
			View last = getChildAt(numChildren -1);
			while(last.getRight()<listRight) {
				final int lastVisiblePosition = mFirstPosition + numChildren - 1;
				if(lastVisiblePosition < mAdapter.getCount() - 1) {
					last = addViewAfter(last, lastVisiblePosition);
					numChildren++;
				} else {
					break;
				}
			}
			// may have brought in the last child of the list that is skinnier
            // than the fading edge, thereby leaving space at the end.  need
            // to shift back
			if(last.getRight() < listRight) {
				offsetChildrenLeftAndRight(listRight - last.getRight());
			}
			
			// top views may be panned off screen
			View first = getChildAt(0);
			while(first.getLeft() < listLeft) {
				detachViewFromParent(first);
				recycleBin.addScrapView(first, mFirstPosition);
				first = getChildAt(0);
                mFirstPosition++;
			}
		} else {
			//shift first items down
			View first = getChildAt(0);
			while((first.getLeft() > listLeft) && (mFirstPosition > 0 )) {
				first = AddViewBefore(first, mFirstPosition);
				mFirstPosition--;
			}
			
			// may have brought the very first child of the list in too far and
            // need to shift it back
            if (first.getLeft() > listLeft) {
            	offsetChildrenLeftAndRight(listLeft - first.getLeft());
            }
            
            int lastIndex = getChildCount() - 1;
            View last = getChildAt(lastIndex);
            
            // right view may be panned off screen
            while(last.getLeft() > listRight) {
            	detachViewFromParent(last);
            	recycleBin.addScrapView(last, mFirstPosition + lastIndex);
            	last = getChildAt(--lastIndex);
            }
		}
	}
	
	private View AddViewBefore(View theView, int position) {
		int beforePosition = position + 1;
		View child = obtainView(beforePosition, mIsScrap);
		int edgeOfNewChild = theView.getLeft();
		setupChild(child, beforePosition, edgeOfNewChild, false, mListPadding.top, false, mIsScrap[0]);
		return child;
	}
	
	private View addViewAfter(View theView, int position) {
		int afterPosition = position + 1;
		View child = obtainView(afterPosition, mIsScrap);
		int edgeOfNewChild = theView.getRight();
		setupChild(child, afterPosition, edgeOfNewChild, true, mListPadding.top, false, mIsScrap[0]);
		return child;
	}
	
	/**
     * Offset the vertical location of all children of this view by the specified number of pixels.
     *
     * @param offset the number of pixels to offset
     *
     */
    public void offsetChildrenLeftAndRight(int offset) {
        final int count = getChildCount();
        for (int i = 0; i < count; i++) {
            final View v = getChildAt(i);
            v.offsetLeftAndRight(offset);
        }
    }
    
	@Override
	public void requestLayout() {
		if(!mInLayout) {
			super.requestLayout();
		}
	}
	
	protected View obtainView(int position, boolean[] isScrap) {
		isScrap[0] = false;
		View scrapView;
		scrapView = mRecycler.getScrapView(position);
		View child;
		
		if(scrapView!=null) {
            child = mAdapter.getView(position, scrapView, this);
            if(child!=scrapView) {
            	mRecycler.addScrapView(scrapView, position);
            } else {
            	isScrap[0] = true;
            	child.onStartTemporaryDetach();
            }
		} else {
			child = mAdapter.getView(position, null, this);
		}
		return child;
	}
	
	private void setupChild(View child, int position, int x, boolean flowRight, int childrenTop, boolean selected, boolean recycled) {
		final boolean isSelected = selected && shouldShowSelector();
		final boolean updateChildSelected = isSelected != child.isSelected();
		final boolean needToMeasure = !recycled || updateChildSelected || child.isLayoutRequested();
		// Respect layout params that are already in the view. Otherwise make some up...
        // noinspection unchecked
		HListView.LayoutParams p = (HListView.LayoutParams) child.getLayoutParams();
		if(p==null) {
			p = new HListView.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.MATCH_PARENT);
		}
		if(recycled) {
			attachViewToParent(child, flowRight?-1:0, p);
		} else {   
			addViewInLayout(child, flowRight?-1:0, p);
		}
		
		if (updateChildSelected) {
            child.setSelected(isSelected);
        }
		
		if(needToMeasure) {
			int childHeightSpec = ViewGroup.getChildMeasureSpec(mHeightMeasureSpec, mListPadding.top + mListPadding.bottom, p.height);
			int lpWidth = p.width;
			int childWidthSpec;
			if(lpWidth > 0) {
				childWidthSpec = MeasureSpec.makeMeasureSpec(lpWidth, MeasureSpec.EXACTLY);
			} else {
				childWidthSpec = MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED);
			}
			child.measure(childWidthSpec, childHeightSpec);
		} else {
			cleanupLayoutState(child);
		}
		
		final int w = child.getMeasuredWidth();
        final int h = child.getMeasuredHeight();
        final int childLeft = flowRight?x:x-w;
        if(needToMeasure) {
        	final int childRight = childLeft + w;
        	final int childBottom = childrenTop + h;
        	child.layout(childLeft, childrenTop, childRight, childBottom);
        } else {
        	child.offsetLeftAndRight(childLeft - child.getLeft());
        	child.offsetTopAndBottom(childrenTop - child.getTop());
        }
	}
	

	public void setDrawSelectorOnTop(boolean onTop) {
		mDrawSelectorOnTop = onTop;
	}
	
	public void setSelector(int resID) {
		setSelector(getResources().getDrawable(resID));
	}
	
	public void setSelector(Drawable sel) {
		if(mSelector!=null) {
			mSelector.setCallback(null);
			unscheduleDrawable(mSelector);
		}
		mSelector = sel;
		Rect padding = new Rect();
		sel.getPadding(padding);
		mSelectionLeftPadding = padding.left;
		mSelectionTopPadding = padding.top;
		mSelectionRightPadding = padding.right;
		mSelectionBottomPadding = padding.bottom;
		sel.setCallback(this);
		updateSelectorState();
	}
	

	@Override
	protected void dispatchDraw(Canvas canvas) {
		final boolean drawSelectorOnTop =  mDrawSelectorOnTop;
		if(!drawSelectorOnTop) {
			drawSelector(canvas);
		}
		super.dispatchDraw(canvas);
		if(drawSelectorOnTop) {
			drawSelector(canvas);
		}
	}
	
	private void drawSelector(Canvas canvas) {
		if(!mSelectRect.isEmpty()) {
			final Drawable selector = mSelector;
			selector.setBounds(mSelectRect);
			selector.draw(canvas);
		}
	}
	
	
	class RecycleBin {
		
		private int mFirstActivePosition;
		private View[] mActiveViews = new View[0];
		private ArrayList<View> mScrapViews = new ArrayList<View>();
		
		public void fillActiveViews(int childCount, int firstActivePosition) {
			if(mActiveViews.length< childCount) {
				mActiveViews = new View[childCount];
			}
			mFirstActivePosition = firstActivePosition;
			
			final View[] activeViews = mActiveViews;
			for(int i=0; i<childCount; i++) {
				View child = getChildAt(i);
				activeViews[i] = child;
			}
		}
		
		public void scrapActiveViews() {
			final View[] activeViews = mActiveViews;
			final ArrayList<View> scrapViews = mScrapViews;
			final int count = mActiveViews.length;
			for(int i=count-1; i>=0; --i) {
				final View victim = activeViews[i];
				if(victim != null) {
					activeViews[i] = null;
					victim.onStartTemporaryDetach();
					scrapViews.add(victim);
				}
			}
			prunceScrapViews();
		}
		
		/**
         * Makes sure that the size of mScrapViews does not exceed the size of mActiveViews.
         * (This can happen if an adapter does not recycle its views).
         */
		private void prunceScrapViews() {
			final int maxViewCount = mActiveViews.length;
			final ArrayList<View> scrapViews = mScrapViews;
			int size = scrapViews.size();
			final int extras = maxViewCount - size;
			size--;
			for(int i=0; i<extras; i++) {
				removeDetachedView(scrapViews.remove(i--), false);
			}
		}
		
		public View getActiveView(int position) {
			int index = position - mFirstActivePosition;
			final View[] activeViews = mActiveViews;
			if(index>0 && index<activeViews.length) {
				final View match = activeViews[index];
				activeViews[index] = null;
				return match;
			}
			return null;
		}
		
		public void addScrapView(View scrap, int position) {
			HListView.LayoutParams lp = (HListView.LayoutParams) scrap.getLayoutParams();

			lp.scrappedFromPosition = position;
			scrap.onStartTemporaryDetach();
			mScrapViews.add(scrap);
		}
		
		public View getScrapView(int position) {
			if(mScrapViews.size() > 0) {
				for(int i=0; i<mScrapViews.size();i++) {
					View view = mScrapViews.get(i);
					if(((HListView.LayoutParams)view.getLayoutParams()).scrappedFromPosition == position) {
						mScrapViews.remove(i);
						return view;
					}
				}
				return mScrapViews.remove(mScrapViews.size() -1 );
			} else {
				return null;
			}
		}
		
		public void clear(){
			final ArrayList<View> scrap = mScrapViews;
			final int scrapCount = scrap.size();
			for(int i=0; i<scrapCount;i++) {
				removeDetachedView(scrap.remove(scrapCount - i - 1), false);
			}
		}
	}
	
	protected class AdapterDataSetObserver extends DataSetObserver {

		@Override
		public void onChanged() {
			mDataChanged = true;
			invalidate();
			requestLayout();
		}

		@Override
		public void onInvalidated() {
			resetList();
			invalidate();
			requestLayout();
		}
		
	}
	
	private void resetList() {
		removeAllViewsInLayout();
		mFirstPosition = 0;
		mDataChanged = false;
		setSelectedPositionInt(INVALID_POSITION);
		mSelectedPosition = INVALID_POSITION;
		mNextSelectedPosition = INVALID_POSITION;
		mSelectedLeft = 0;
		mSelectRect.setEmpty();
		invalidate();
	}
}
