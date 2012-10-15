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
import android.widget.AbsListView;
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
    
    private int mLayoutMode;
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
			mRecycler.addScrapView(child, -1);
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
		int childHeightSpec = ViewGroup.getChildMeasureSpec(heightMeasureSpec, 0, p.height);
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
			return 0;
		}
		int returnedWidth = 0;
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
			int childrenLeft = 0;
			int childrenRight = getRight();
			
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
            }
		} finally {
			mInLayout = false;
		}
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

	
	/**
     * Scroll the children by amount, adding a view at the end and removing
     * views that fall off as necessary.
     *
     * @param amount The amount (positive or negative) to scroll.
     */
	private void scrollListItemBy(int amount) {
		offsetChildrenLeftAndRight(amount);
		final int listRight = getWidth();
		final int listLeft = 0;
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
		setupChild(child, beforePosition, edgeOfNewChild, false, 0, false, mIsScrap[0]);
		return child;
	}
	
	private View addViewAfter(View theView, int position) {
		int afterPosition = position + 1;
		View child = obtainView(afterPosition, mIsScrap);
		int edgeOfNewChild = theView.getRight();
		setupChild(child, afterPosition, edgeOfNewChild, true, 0, false, mIsScrap[0]);
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
			int childHeightSpec = ViewGroup.getChildMeasureSpec(mHeightMeasureSpec, 0, p.height);
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
		private ArrayList<View> mScrapViews = new ArrayList<View>();
		
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
		mSelectedLeft = 0;
		mSelectRect.setEmpty();
		invalidate();
	}
}
