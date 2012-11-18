package org.sakuratya.horizontal.ui;

import java.util.ArrayList;

import org.sakuratya.horizontal.adapter.HGridAdapter;

import android.content.Context;
import android.database.DataSetObserver;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.util.FloatMath;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;

/**
 * These is a horizontal scroll GridView with arrow key action support and a dataset of sections.
 * You need to implement an adapter base on {@link HGridAdapter}. if you have a dataset which is separated by sections. 
 * The {@link HGridAdapter#getSectionIndex(int)} should return its section index(based on 0).
 * @author bob
 *
 */
@SuppressWarnings("rawtypes")
public class HGridView extends AdapterView<HGridAdapter> {
	
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
    
    private static final int LAYOUT_SET_SELECTION = 0;
    private static final int LAYOUT_NORMAL = 1;
    private static final int LAYOUT_MOVE_SELECTION = 2;
    
	private HGridAdapter mAdapter;
	
	private static final int INVALID_POSITION = -1;
	private int mSelectedPosition;
	private int mNextSelectedPosition;
	
	private int mAmountToScroll = 0;
	/**
	 * Set rows per column.
	 */
	private int mRows;
	/**
	 * The position in adapter which is the first child of current list.
	 */
	private int mFirstPosition;
	
	private int mMaxColumn;
	
	private Rect mListPadding;
	
	private int mSelectorLeftPadding;
	private int mSelectorTopPadding;
	private int mSelectorRightPadding;
	private int mSelectorBottomPadding;
	
	private boolean isInLayout = false;
	
	private final RecycleBin mRecycler = new RecycleBin();
	
    /**
     * The horizontal space between each item.
     */
    private int mHorizontalSpacing;
    /**
     * The separator horizontal space to its right column. 
     */
    private int mSeparatorRightSpace;
    /**
     * THe vertical space between each item.
     */
    private int mVerticalSpace;
   
    private int mRowHeight;
    /**
     * current layout mode.
     */
    private int mLayoutMode  = LAYOUT_NORMAL;
    
    private boolean mDataChanged = false;
    
    private View mReferenceViewInSelectedColumn = null;
    private View mReferenceView = null;
    
    final boolean[] mIsScrap = new boolean[1];
    
    protected AdapterDataSetObserver mDataSetObserver;
    
	public HGridView(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
		// TODO Auto-generated constructor stub
	}

	public HGridView(Context context, AttributeSet attrs) {
		super(context, attrs);
		// TODO Auto-generated constructor stub
	}

	public HGridView(Context context) {
		super(context);
		// TODO Auto-generated constructor stub
	}

	@Override
	public HGridAdapter getAdapter() {
		return mAdapter;
	}

	@Override
	public void setAdapter(HGridAdapter adapter) {
		if(mAdapter!=null) {
			mAdapter.unregisterDataSetObserver(mDataSetObserver);
		}
		
		resetList();
		mRecycler.clear();
		mAdapter = adapter;
		
		if(mAdapter!=null) {
			mDataChanged = true;
			mDataSetObserver = new AdapterDataSetObserver();
			mAdapter.registerDataSetObserver(mDataSetObserver);
			
			int position = lookForSelectablePosition(0, true);
			setSelectedPositionInt(position);
			setNextSelectedPositionInt(position);
		} else {
			
		}
		requestLayout();
	}
	
	private int lookForSelectablePosition(int position, boolean lookDown) {
		final HGridAdapter adapter = mAdapter;
		if(adapter == null){
			return INVALID_POSITION;
		}
		if(position < 0 || position >= adapter.getCount()){
			return INVALID_POSITION;
		}
		if(!adapter.isEnabled(position)) {
			return lookForSelectablePosition(position + 1, lookDown);
		}
		return position;
	}

	@Override
	public View getSelectedView() {
		if(mAdapter.getCount() > 0 && mSelectedPosition>=0) {
			return getChildAt(mSelectedPosition - mFirstPosition);
		} else {
			return null;
		}
	}

	@Override
	public void setSelection(int position) {
		// TODO Auto-generated method stub
		
	}
	
	private void setSelectedPositionInt(int position) {
		mSelectedPosition = position;
	}
	
	private void setNextSelectedPositionInt(int position) {
		mNextSelectedPosition = position;
	}
	
//	private void scrollListItemsBy(int delta) {
//		final int listRight = getRight() - mListPadding.right;
//		final int listLeft = mListPadding.left;
//		final RecycleBin recycleBin = mRecycler;
//		if(delta < 0) {
//			// May need to pan views into right space.
//			int numChildren = getChildCount();
//			View last = getChildAt(numChildren -1);
//			int lastVisiblePosition = mFirstPosition + numChildren -1;
//			while(last.getLeft() < listRight) {
//				if(lastVisiblePosition < mAdapter.getCount() -1) {
//					last = addColumnRight(last, lastVisiblePosition);
//					numChildren = getChildCount();
//					lastVisiblePosition = mFirstPosition + numChildren -1;
//				} else {
//					break;
//				}
//			}
//			// We must ensure the last view is not a separator. But two separator without a selectable view
//			// between them is invalid, we can't ensure it will work properly
//			if(!last.isEnabled() && lastVisiblePosition + 1 < mAdapter.getCount()) {
//				addColumnRight(last, lastVisiblePosition);
//			}
//			
//			// May need to pan off views left which is out bound of screen. And fully invisible.
//			View first = getChildAt(0);
//			int nextPosition = mFirstPosition + 1;
//			// We must ensure the first view is not a separator.
//			while(first.getRight() < listLeft && mAdapter.isEnabled(nextPosition)) {
//				detachViewFromParent(first);
//				recycleBin.addScrapView(first);
//				first = getChildAt(0);
//				mFirstPosition++;
//				nextPosition = mFirstPosition + 1;
//			}
//		} else {
////			View first = getChildAt(0);
//			
//		}
//	}
	
	private View fillSpecific(int position, int left) {
		int motionCol = getColumn(position);
		final View temp = makeColumn(motionCol, left, true);
		
		//Possible changed again in fillLeft if we add rows above this one.
		
		mFirstPosition = getPositionRangeByColumn(motionCol)[0];
		
		final View referenceView = mReferenceView;
		// We didn't have anything to layout, bail out
		if(referenceView == null) {
			return null;
		}
		
		final int horizontalSpacing = mHorizontalSpacing;
		
		View before;
		View after;
		
		before = fillLeft(motionCol - 1, referenceView.getLeft() - horizontalSpacing);
		
		after = fillRight(motionCol + 1, referenceView.getRight() + horizontalSpacing);
		
		if(temp != null) {
			return temp;
		} else if( before != null) {
			return before;
		} else {
			return after;
		}
	}
	
	private View fillFromLeft(int nextLeft) {
		mFirstPosition = Math.min(mFirstPosition, mSelectedPosition);
		mFirstPosition = Math.min(mFirstPosition, mAdapter.getCount() - 1);
		if(mFirstPosition < 0) {
			mFirstPosition = 0;
		}
		int col = getColumn(mFirstPosition);
		return fillRight(col, nextLeft);
	}
	
	private View fillLeft(int col, int nextRight) {
		View selectedView = null;
		final int end = mListPadding.left;
		
		while(nextRight > end && col >=0) {
			
			final int colStart = getPositionRangeByColumn(col)[0];
			if(!mAdapter.isEnabled(colStart)) {
				nextRight = nextRight + mHorizontalSpacing - mSeparatorRightSpace;
			}
			View temp = makeColumn(col, nextRight, false);
			if(temp != null) {
				selectedView = temp;
			}

			// mReferenceView will change with each call to makeColumn()
			// do not cache in a local variable outside of this loop
			nextRight = mReferenceView.getLeft() - mHorizontalSpacing;
			
			mFirstPosition = getPositionRangeByColumn(col)[0];
			col--;
		}
		return selectedView;
	}
	
	private View fillRight(int col, int nextLeft) {
		View selectedView = null;
		final int end = getRight() - getLeft() - mListPadding.right;
		
		while(nextLeft < end && col < mMaxColumn) {
			
			final int colStart = getPositionRangeByColumn(col)[0];
			if( !mAdapter.isEnabled(colStart)) {
				nextLeft = nextLeft - mHorizontalSpacing + mSeparatorRightSpace;
			}
			View temp = makeColumn(col, nextLeft, true);
			if(temp != null) {
				selectedView = temp;
			}
			
			nextLeft = mReferenceView.getRight() + mHorizontalSpacing;
			
			col++;
		}
		return selectedView;
	}
	
	private View moveSelection(int amount, int childrenLeft, int childrenRight) {
		final int fadingEdgeLength = getHorizontalFadingEdgeLength();
		final int selectedPosition = mSelectedPosition;
		final int nextSelectedPosition = mNextSelectedPosition;
		final int horizontalSpace = mHorizontalSpacing;
		
		View selectedView;
		View referenceView;
//		int oldCol = getColumn(selectedPosition);
		int col = getColumn(nextSelectedPosition);
		
		final int oldLeft = mReferenceViewInSelectedColumn==null?0:mReferenceViewInSelectedColumn.getLeft();
	
		/*
		 *  We use amount to make column, because there maybe exists a separator, we can't predict how far we should scroll. use the amount that we calculated just before.
		 */
		selectedView = makeColumn(col, oldLeft+amount, true);
		referenceView = mReferenceView;
		
		int columnLeft = col - 1;
		if(columnLeft >= 0) {
			fillLeft(columnLeft, referenceView.getTop() - horizontalSpace);
		}
		int columnRight = col + 1;
		if(columnRight <= mMaxColumn) {
			fillRight(columnRight, referenceView.getRight() + horizontalSpace);
		}
		return null;
	}

	private View makeColumn(int column, int x, boolean flow) {
		final int firstPosition = mFirstPosition;
		final int verticalSpacing = mVerticalSpace;
		int[] positionRange = getPositionRangeByColumn(column);
		View selectedView = null;
		View child = null;
		int nextTop = mListPadding.top + verticalSpacing;
		for(int pos = positionRange[0]; pos <= positionRange[1];pos++) {
			boolean selected = pos == firstPosition;
			
			final int where = flow ? -1 : pos - positionRange[0];
			child = makeAndAddView(pos, x, flow, nextTop, selected, where);
			
			nextTop += mRowHeight;
			if(pos < positionRange[1]) {
				nextTop += verticalSpacing;
			}
			if(selected) {
				selectedView = child;
				
			}
		}
		
		mReferenceView = child;
		
		if(selectedView!=null) {
			mReferenceViewInSelectedColumn = mReferenceView;
		}
		return selectedView;
	}
	
	private View makeAndAddView(int position, int x, boolean flow, int childrenTop, boolean selected, int where) {
		View child;
		if(!mDataChanged) {
			// Try to use an existing view.
			child = mRecycler.getActiveView(position);
			if(child!=null) {
				// Found it! We are using an existing view.
				// Just need to position it.
				setupChild(child, position, x, flow, childrenTop, selected, true, where);
				return child;
			}
			
		}
		// Make a new view for the position, or convert an old view.
		child = obtainView(position, mIsScrap);
		setupChild(child, position, x, flow, childrenTop, selected, mIsScrap[0], where);
		return child;
	}
	
	private void setupChild(View child, int position, int x, boolean flow, int childrenTop, boolean selected, boolean recycled, int where) {
		boolean isSelected = selected && shouldShowSelector();
		boolean updateChildSelected = isSelected == child.isSelected();
		boolean needToMeasure = !recycled || updateChildSelected || child.isLayoutRequested();
		LayoutParams p = (LayoutParams) child.getLayoutParams();
		if(p == null) {
			p = new LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.MATCH_PARENT);
		}
		
		if(recycled) {
			attachViewToParent(child, where, p);
		} else {
			addViewInLayout(child, where, p, true);
		}
		if(updateChildSelected) {
			child.setSelected(isSelected);
			if(isSelected) {
				requestFocus();
			}
		}
		if(needToMeasure) {
			int childWidthSpec = ViewGroup.getChildMeasureSpec(MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED), 0, p.width);
			int childHeightSpec = ViewGroup.getChildMeasureSpec(MeasureSpec.makeMeasureSpec(mRowHeight, MeasureSpec.EXACTLY), 0, p.height);
			child.measure(childWidthSpec, childHeightSpec);
		} else {
			cleanupLayoutState(child);
		}
		
		final int w = child.getMeasuredWidth();
		final int h = child.getMeasuredHeight();
		
		// we do not support gravity
		final int childTop = childrenTop;
		final int childLeft = flow? x : x - w;
		
		if(needToMeasure) {
			final int childRight = childLeft + w;
			final int childBottom = childTop + h;
			child.layout(childLeft, childTop, childRight, childBottom);
		} else {
			child.offsetLeftAndRight(childLeft - child.getLeft());
			child.offsetTopAndBottom(childTop - child.getTop());
		}
		// May be we need drawing cache in the future
//		if(mCachingStarted) {
//			child.setDrawingCacheEnabled(true);
//		}
	}
	
	/**
	 * Get a view and have it show data associated with its position.
	 * This is called when we discovered that the view is already not available for reuse in the recycle bin.
	 * The only choices left are converting an old view or make a new one. 
	 * @param position
	 * @return
	 */
	protected View obtainView(int position, boolean[] isScrap) {
		isScrap[0] = false;
		View scrapView;
		scrapView = mRecycler.getScrapView(position);
		
		View child;
		if(scrapView!=null) {
			child = mAdapter.getView(position, scrapView, this);
			if(child==scrapView) {
				isScrap[0] = true;
				child.onFinishTemporaryDetach();
			} else {
				mRecycler.addScrapView(scrapView);
			}
		} else {
			child = mAdapter.getView(position, null, this);
		}
		return this;
	}
	
	@Override
	protected void onLayout(boolean changed, int left, int top, int right,
			int bottom) {
		super.onLayout(changed, left, top, right, bottom);
		layoutChildren();
	}
	
	protected void layoutChildren() {
		
		if(isInLayout) {
			return;
		}
		isInLayout = true;
		
		try {
			if(mAdapter==null) {
				resetList();
				return;
			}
			final int childrenLeft = mListPadding.left;
			final int childrenRight = getRight() - getLeft() - mListPadding.right;
			final RecycleBin recycleBin = mRecycler;
			final int firstPosition = mFirstPosition;
			int childCount = getChildCount();
			
			int index;
			View sel;
			View oldSel = null;
			View oldFirst = null;
			View newSel = null;
			switch(mLayoutMode) {
			case LAYOUT_SET_SELECTION:
				break;
			case LAYOUT_MOVE_SELECTION:
				break;
			default:
				// Remember the previously selected view.
				index = mSelectedPosition - mFirstPosition;
				if(index >=0 && index< childCount) {
					oldSel = getChildAt(index);
				}
				
				// Remember the previous first child
				oldFirst = getChildAt(0);
			}
			
			if(mDataChanged) {
				for(int i=0; i<childCount;i++) {
					recycleBin.addScrapView(getChildAt(i));
				}
			} else {
				recycleBin.fillActiveViews(childCount, firstPosition);
			}
			
			detachAllViewsFromParent();
			
			switch(mLayoutMode) {
			case LAYOUT_MOVE_SELECTION:
				sel = moveSelection(mAmountToScroll, childrenLeft, childrenRight);
				break;
			default:
				if(childCount == 0) {
					setSelectedPositionInt(mAdapter == null ? INVALID_POSITION : 0 );
					sel = fillFromLeft(childrenLeft);
				} else {
					if(mSelectedPosition >= 0 && mSelectedPosition < mAdapter.getCount()) {
						sel = fillSpecific(mSelectedPosition, oldSel == null ? childrenLeft : oldSel.getLeft());
					} else if(mFirstPosition < mAdapter.getCount()) {
						sel = fillSpecific(mFirstPosition, oldFirst.getLeft());
					} else {
						sel = fillSpecific(0, childrenLeft);
					}
				}
			}
		} finally {
			isInLayout = false;
			mAmountToScroll = 0;
		}
	}

	@Override
	protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
		
		final Rect listPadding = mListPadding;
		listPadding.left = mSelectorLeftPadding + getPaddingLeft();
		listPadding.top = mSelectorTopPadding + getPaddingTop();
		listPadding.right = mSelectorRightPadding + getPaddingRight();
		listPadding.bottom = mSelectorBottomPadding + getPaddingBottom();
	}
	
	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		return commonKey(keyCode, 1, event);
	}

	@Override
	public boolean onKeyUp(int keyCode, KeyEvent event) {
		return commonKey(keyCode, 1, event);
	}

	@Override
	public boolean onKeyMultiple(int keyCode, int repeatCount, KeyEvent event) {
		return commonKey(keyCode, repeatCount, event);
	}
	
	private boolean commonKey(int keyCode, int repeatCount, KeyEvent event) {
		final int action = event.getAction();
		boolean handled = false;
		if(action!=KeyEvent.ACTION_UP) {
			switch(keyCode) {
			case KeyEvent.KEYCODE_DPAD_RIGHT:
				handled = arrowScroll(FOCUS_RIGHT);
				if(!handled) {
					handled = seekForOtherView(FOCUS_RIGHT);
				}
				break;
			case KeyEvent.KEYCODE_DPAD_LEFT:
				handled = arrowScroll(FOCUS_LEFT);
				if(!handled) {
					handled = seekForOtherView(FOCUS_RIGHT);
				}
				break;
			}
		}
		switch(action) {
		case KeyEvent.ACTION_DOWN:
			return super.onKeyDown(keyCode, event);
		case KeyEvent.ACTION_UP:
			return super.onKeyUp(keyCode, event);
		case KeyEvent.ACTION_MULTIPLE:
			return super.onKeyMultiple(keyCode, repeatCount, event);
		default:
			return false;
		}
	}
	
	private boolean seekForOtherView(int direction) {
		return false;
	}

	/**
	 * handle arrow key scroll.
	 * @param direction
	 * @return true if an arrow key is handled. false if there is no room for arrow scroll. 
	 */
	private boolean arrowScroll(int direction) {
		
		int nextSelectedPosition = lookForSelectablePositionOnScreen(direction);
		
		if(nextSelectedPosition==INVALID_POSITION) {
			return false;
		}
		
		int amountToScroll = amountToScroll(direction, nextSelectedPosition);
		
		if(nextSelectedPosition!=INVALID_POSITION) {
			setNextSelectedPositionInt(nextSelectedPosition);
		}
		if(amountToScroll>0) {
			mAmountToScroll = amountToScroll;
			
//			scrollListItemsBy(direction==FOCUS_RIGHT?amountToScroll:-amountToScroll);
		} else {
			mAmountToScroll = 0;
		}
		if(nextSelectedPosition!=INVALID_POSITION) {
			mLayoutMode = LAYOUT_MOVE_SELECTION;
			layoutChildren();
		}
		
		return true;
	}
//	
//	private void doScroll(int delta) {
//		if(delta!=0) {
//			scrollBy(delta, 0);
//		}
//	}
	
	
	private int lookForSelectablePositionOnScreen(int direction) {
		final int firstPosition = mFirstPosition;
		
		if(direction== FOCUS_LEFT) {
			final int lastVisiblePosition = mFirstPosition + getChildCount() - 1;
			final int currentSelectedColumn = mSelectedPosition==INVALID_POSITION ? getColumn(lastVisiblePosition): getColumn(mSelectedPosition);
			final int firstColumn = getColumn(firstPosition);
			if(currentSelectedColumn<=0 && currentSelectedColumn < firstColumn) {
				return INVALID_POSITION;
			}
			final int currentSelectedRow =mSelectedPosition==INVALID_POSITION ? 0 : getRow(mSelectedPosition);
			for(int col=currentSelectedColumn-1; col>=firstColumn;col--) {
				int[] positionRange = getPositionRangeByColumn(col);
				// Skip the separator
				if(mAdapter.isEnabled(positionRange[0])) {
					// If next column has less rows than currentSelectedRow, just return the last row of next column. 
					return currentSelectedRow > positionRange[1] ? positionRange[1] : positionRange[0] + currentSelectedRow;
				}
			}
			
		} else if(direction==FOCUS_RIGHT) {
			final int lastVisiblePosition = mFirstPosition + getChildCount() - 1;
			if(mSelectedPosition >= lastVisiblePosition) {
				return INVALID_POSITION;
			}
			final int currentSelectedColumn = mSelectedPosition==INVALID_POSITION ? getColumn(firstPosition): getColumn(mSelectedPosition);
			final int lastVisibleColumn = getColumn(lastVisiblePosition);
			if(currentSelectedColumn >= lastVisibleColumn) {
				return INVALID_POSITION;
			}
			final int currentSelectedRow =mSelectedPosition==INVALID_POSITION ? 0 : getRow(mSelectedPosition);
			for(int col=currentSelectedColumn+1; col<=lastVisibleColumn;col++) {
				int[] positionRange = getPositionRangeByColumn(col);
				// Skip the separator
				if(mAdapter.isEnabled(positionRange[0])) {
					// If next column has less rows than currentSelectedRow, just return the last row of next column. 
					return currentSelectedRow > positionRange[1] ? positionRange[1] : positionRange[0] + currentSelectedRow;
				}
			}
		} else if(direction==FOCUS_UP) {
			
		} else {
			
		}
		return INVALID_POSITION;
	}
	
	/**
	 * Get zero based column according to given position.
	 * Note that a separator is also take a column.
	 * @param position
	 * @return a zero based column index.
	 */
	private int getColumn(int position) {
		if(position>=0 && position < mAdapter.getCount()) {
			int column = 0;
			if(mAdapter.hasSection()) {
				final int sectionIndex = mAdapter.getSectionIndex(position);
				int itemCount = 0;
				for(int i=0; i < sectionIndex; i++) {
					int sectionCount = mAdapter.getSectionCount(i);
					column += (int)FloatMath.ceil((float)sectionCount / (float) mRows);
					itemCount += sectionCount;
				}
				column += mAdapter.isEnabled(position) ? sectionIndex: sectionIndex + (int)FloatMath.ceil((float)(position - (itemCount + sectionIndex)) /(float)mRows);
			} else {
				column = (int)FloatMath.floor((float)position /(float) mRows);
			}
			return column;
		}
		return INVALID_POSITION;
	}
	
	/**
	 * Get a zero based row index.
	 * Note a separator's row index is 0.
	 * @param position
	 * @return
	 */
	private int getRow(int position) {
		if(position>=0 && position < mAdapter.getCount()) {
			if(!mAdapter.isEnabled(position)) {
				return 0;
			}
			int row = 0;
			if(mAdapter.hasSection()) {
				final int sectionIndex = mAdapter.getSectionIndex(position);
				int prevSectionItemCount = 0;
				for(int i=0; i<sectionIndex;i++) {
					prevSectionItemCount += mAdapter.getSectionCount(i);
				}
				int currentSectionCount = position - prevSectionItemCount - sectionIndex;
				row = currentSectionCount - (currentSectionCount / mRows) * mRows;
			} else {
				row = position - (position / mRows) * mRows; 
			}
			return row;
		}
		return INVALID_POSITION;
	}
	
	private int[] getPositionRangeByColumn(int column) {
		int[] positionRange = new int[2];
		final int lastItem = mAdapter.getCount() -1;
		final int lastSectionIndex = mAdapter.getSectionIndex(lastItem);
		int currentSectionIndex = 0;
		int itemCount = 0;
		int col = 0;
		for(int i=0; i<lastSectionIndex; i++) {
			int sectionCount = mAdapter.getSectionCount(i); 
			col += (sectionCount / mRows) + 1;
			if(col>column) {
				currentSectionIndex = i-1;
				break;
			}
			itemCount+=sectionCount;
		}
		int positionStart = itemCount + currentSectionIndex + (column - col)* mRows;
		if(!mAdapter.isEnabled(positionStart)) {
			//This column is separator, it has only one row.
			positionRange[0] = positionStart;
			positionRange[1] = positionStart;
			return positionRange;
		}
		final int lastPositionOfCurrentSection = mAdapter.getSectionCount(currentSectionIndex) + itemCount + currentSectionIndex;
		int positionEnd = positionStart;
		while(positionEnd < positionStart + mRows && positionEnd >= lastPositionOfCurrentSection) {
			++positionEnd;
		}
		positionRange[0] = positionStart;
		positionRange[1] = positionEnd;
		return positionRange;
	}
	
	private int amountToScroll(int direction, int nextSelectedPosition) {
		final int listRight = getWidth() + mListPadding.right;
		final int listLeft = mListPadding.left;
		
		if(direction==FOCUS_RIGHT) {
			int indexToMakeVisible = getChildCount() - 1;
			if(nextSelectedPosition!=INVALID_POSITION) {
				indexToMakeVisible = nextSelectedPosition - mFirstPosition;
			}
			
			final int positionToMakeVisible = indexToMakeVisible + mFirstPosition;
			final View viewToMakeVisible = getChildAt(indexToMakeVisible);
			
			int goalRight = listRight;
			if(positionToMakeVisible < mAdapter.getCount() -1) {
				goalRight -= getArrowScrollPreviewLength();
			}
			
			if(viewToMakeVisible.getRight() < goalRight) {
				return 0;
			}
			
			if(nextSelectedPosition!=INVALID_POSITION && goalRight - viewToMakeVisible.getLeft() >  getMaxScrollAmount()) {
				// item already has enough of it visible, changing selection is good enough
				return 0;
			}
			
			int amountToScroll = viewToMakeVisible.getRight() - goalRight;
			if(mFirstPosition+getChildCount()==mAdapter.getCount()) {
				// Make sure we will not scroll over the last item.
				final int max = getChildAt(mFirstPosition+getChildCount()-1).getRight() - goalRight;
				amountToScroll = Math.min(max, amountToScroll);
			}
			return Math.min(amountToScroll, getMaxScrollAmount());
		} else {
			int indexToMakeVisible = 0;
			if(nextSelectedPosition!=INVALID_POSITION) {
				indexToMakeVisible = nextSelectedPosition - mFirstPosition;
			}
			
			final int positionToMakeVisible = indexToMakeVisible + mFirstPosition;
			final View viewToMakeVisible = getChildAt(indexToMakeVisible);
			
			int goalLeft = listLeft;
			if(positionToMakeVisible > 0) {
				goalLeft += getArrowScrollPreviewLength();
			}
			
			if(viewToMakeVisible.getLeft() > goalLeft) {
				return 0;
			}
			
			if(nextSelectedPosition!=INVALID_POSITION && viewToMakeVisible.getRight() - goalLeft > getMaxScrollAmount()) {
				// item already has enough of it visible, changing selection is good enough
				return 0;
			}
			
			int amountToScroll = goalLeft - viewToMakeVisible.getLeft();
			
			if(mFirstPosition == 0) {
				// Make sure we will not scroll over the last item.
				final int max = getChildAt(0).getLeft() - goalLeft;
				amountToScroll = Math.min(max, amountToScroll);
			}
			return Math.min(amountToScroll, getMaxScrollAmount());
		}
	}

	private int getArrowScrollPreviewLength() {
		return Math.max(MIN_SCROLL_PREVIEW_PIXELS, getHorizontalFadingEdgeLength());
	}
	
	private int getMaxScrollAmount() {
		return (int) (MAX_SCROLL_FACTOR * (getRight() - getLeft()));
	}
	
	private void resetList() {
		mSelectedPosition = INVALID_POSITION;
		mNextSelectedPosition = INVALID_POSITION;
		mFirstPosition = 0;
		
	}
	
	public void setRowHeight(int height) {
		mRowHeight = height;
	}
	
	public int getRowHeight() {
		return mRowHeight;
	}
	
	protected boolean shouldShowSelector() {
		return hasFocus();
	}
	
	
	static class LayoutParams extends ViewGroup.LayoutParams {

		public LayoutParams(Context arg0, AttributeSet arg1) {
			super(arg0, arg1);
			// TODO Auto-generated constructor stub
		}

		public LayoutParams(int arg0, int arg1) {
			super(arg0, arg1);
			// TODO Auto-generated constructor stub
		}

		public LayoutParams(android.view.ViewGroup.LayoutParams arg0) {
			super(arg0);
			// TODO Auto-generated constructor stub
		}
		
	}
	
	class RecycleBin {
		
		private int firstActivePosition;
		
		private ArrayList<View> activeViews;
		
		private ArrayList<View> mScrapViews;
		
		private void fillActiveViews(int childCount, int firstPosition) {
			
		}
		
		private View getActiveView(int position) {
			return null;
		}
		public void addScrapView(View scrap) {
			
		}
		
		public View getScrapView(int position) {
			return null;
		}
		
		public void clear() {
			
		}
	}
	
	class AdapterDataSetObserver extends DataSetObserver {

		@Override
		public void onChanged() {
			// TODO Auto-generated method stub
			super.onChanged();
		}

		@Override
		public void onInvalidated() {
			// TODO Auto-generated method stub
			super.onInvalidated();
		}
		
	}

}
