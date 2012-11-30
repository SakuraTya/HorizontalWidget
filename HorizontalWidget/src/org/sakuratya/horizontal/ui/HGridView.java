package org.sakuratya.horizontal.ui;

import java.util.ArrayList;
import java.util.Arrays;

import org.sakuratya.horizontal.R;
import org.sakuratya.horizontal.adapter.HGridAdapter;

import android.content.Context;
import android.content.res.TypedArray;
import android.database.DataSetObserver;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.util.FloatMath;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.accessibility.AccessibilityEvent;
import android.widget.Adapter;
import android.widget.AdapterView;
import android.widget.ListView;

/**
 * These is a horizontal scroll GridView with arrow key action support and a dataset of sections.
 * You need to implement an adapter base on {@link HGridAdapter}. if you have a dataset which is separated by sections. 
 * The {@link HGridAdapter#getSectionIndex(int)} should return its section index(based on 0).
 * @author bob
 *
 */
@SuppressWarnings("rawtypes")
public class HGridView extends AdapterView<HGridAdapter> {
	
	private final static String TAG = "HGridView";
    
    private static final int LAYOUT_SET_SELECTION = 0;
    private static final int LAYOUT_NORMAL = 1;
    private static final int LAYOUT_MOVE_SELECTION = 2;
    private static final int LAYOUT_SPECIFIC = 3;
    /**
     * This is a special layout mode. which just jump to special section without selection.
     */
    private static final int LAYOUT_JUMP = 4;
    
    private static final int LAYOUT_SYNC = 5;
    
    private static final int SYNC_SELECTED_POSITION = 0;
    private static final int SYNC_FIRST_POSITION = 1;
    
    private static final int INVALID_POSITION = -1;
	
    private HGridAdapter mAdapter;
    
	private int mOldSelectedPosition;
	private int mSelectedPosition;
	private int mNextSelectedPosition;
	
	private int mResurrectToPosition;
	
    protected int mSyncPosition;
    protected int mSpecificLeft;
    
    protected int mSyncMode;
    
    
    private boolean mNeedSync = false;
	/**
	 * Set rows per column.
	 */
	private int mRows;
	/**
	 * The position in adapter which is the first child of current list.
	 */
	private int mFirstPosition;
	
	private int mMaxColumn;
	
	private Rect mListPadding = new Rect();
	
	private int mSelectionLeftPadding;
	private int mSelectionTopPadding;
	private int mSelectionRightPadding;
	private int mSelectionBottomPadding;
	
	private boolean isInLayout = false;
	
	private final RecycleBin mRecycler = new RecycleBin();
	
    /**
     * The horizontal space between each item.
     */
    private int mHorizontalSpacing;
    /**
     * The vertical space between each item.
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
    
    private Rect mSelectorRect = new Rect();
    /**
     * The drawable used to draw the selector
     */
    protected Drawable mSelector;
    
    protected int mMeasuredHeight;
    
    private int[] mSectionFirstColumns;
    private int[] mSectionLastColumns;
    
    private Rect[] mSectionLabelRect;
    
    private int mLabelDrawableResId;
    private int mLabelBackgroundDrawableResId;

	private Rect mTempRect;
	
	private OnScrollListener mOnScrollListener;
	
	/**
	 * Store the label and label background drawable's offset related to parent(label draw rectangle).
	 */
	private Rect mLabelDrawableOffset = new Rect();
	private Rect mLabelBackgroundDrawableOffset = new Rect();
	/**
	 * Store the label text paint
	 */
	private Paint mLabelTextPaint;
	
	private Rect mLabelTextMargin = new Rect();
	
	private int mMinSingleTextHeight = 0;
	private int mMinSingleTextWidth = 0;
    
	private SelectionNotifier mSelectionNotifier;
	
	public int mScrollState = OnScrollListener.SCROLL_STATE_IDLE;
	
	public HGridView(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
		initView(context, attrs);
	}

	public HGridView(Context context, AttributeSet attrs) {
		super(context, attrs);
		initView(context, attrs);
	}

	public HGridView(Context context) {
		super(context);
		initView();
	}
	
	private void initView() {
		setWillNotDraw(false);
//		setFocusable(true);
		setClickable(true);
        setFocusableInTouchMode(true);
		setHorizontalFadingEdgeEnabled(true);
	}
	
	private void initView(Context context, AttributeSet attrs) {
		initView();
		TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.HGridView);
		int hSpacing = a.getDimensionPixelOffset(R.styleable.HGridView_horizontalSpacing, 0);
		setHorizontalSpacing(hSpacing);
		int vSpacing = a.getDimensionPixelOffset(R.styleable.HGridView_verticalSpacing, 0);
		setVerticalSpacing(vSpacing);
		int labelDrawableResId = a.getResourceId(R.styleable.HGridView_labelDrawable, 0);
		setLabelDrawableResId(labelDrawableResId);
		int labelBackgroundDrawableResId = a.getResourceId(R.styleable.HGridView_labelBackgroundDrawable, 0);
		setLabelBackgroundDrawableResId(labelBackgroundDrawableResId);
		int rowHeight = a.getDimensionPixelOffset(R.styleable.HGridView_rowHeight, -1);
		if(rowHeight > 0) {
			setRowHeight(rowHeight);
		}
		int numRows = a.getInt(R.styleable.HGridView_numRows, 1);
		setRows(numRows);
		Drawable d = a.getDrawable(R.styleable.HGridView_selectorDrawable);
		if(d!=null) {
			setSelector(d);
		}
		
		int labelLeftOffset = a.getDimensionPixelOffset(R.styleable.HGridView_labelLeftOffset, 0);
		int labelTopOffset = a.getDimensionPixelOffset(R.styleable.HGridView_labelTopOffset, 0);
		int labelWidth = a.getDimensionPixelOffset(R.styleable.HGridView_labelWidth, 0);
		int labelHeight = a.getDimensionPixelOffset(R.styleable.HGridView_labelHeight, 0);
		if(labelLeftOffset>0 || labelTopOffset > 0 || labelWidth > 0 || labelHeight > 0) {
			setLabelDrawableOffset(labelLeftOffset, labelTopOffset, labelLeftOffset + labelWidth, labelTopOffset +labelHeight);
		}
		
		int labelBackgroundLeftOffset = a.getDimensionPixelOffset(R.styleable.HGridView_labelBackgroundLeftOffset, 0);
		int labelBackgroundTopOffset = a.getDimensionPixelOffset(R.styleable.HGridView_labelBackgroundTopOffset, 0);
		int labelBackgroundWidth = a.getDimensionPixelOffset(R.styleable.HGridView_labelBackgroundWidth, 0);
		int labelBackgroundheight = a.getDimensionPixelOffset(R.styleable.HGridView_labelBackgroundHeight, 0);
		if(labelBackgroundLeftOffset > 0 || labelBackgroundTopOffset > 0 || labelBackgroundWidth > 0 || labelBackgroundheight > 0 ) {
			setLabelBackgroundDrawableOffset(labelBackgroundLeftOffset, labelBackgroundTopOffset, labelBackgroundLeftOffset + labelBackgroundWidth, labelBackgroundTopOffset + labelBackgroundheight);
		}
		
		int labelTextSize = a.getDimensionPixelSize(R.styleable.HGridView_labelTextSize, 20);
		int labelTextColor = a.getColor(R.styleable.HGridView_labelTextColor, 0xffffffff);
		mLabelTextPaint = new Paint();
		mLabelTextPaint.setTextSize(labelTextSize);
		mLabelTextPaint.setColor(labelTextColor);
		
		int labelTextMarginLeft = a.getDimensionPixelOffset(R.styleable.HGridView_labelTextMarginLeft, 0);
		int labelTextMarginTop = a.getDimensionPixelOffset(R.styleable.HGridView_labelTextMarginTop, 0);
		int labelTextMarginRight = a.getDimensionPixelOffset(R.styleable.HGridView_labelTextMarginRight, 0);
		int labelTextMarginBottom = a.getDimensionPixelOffset(R.styleable.HGridView_labelTextMarginBottom, 0);
		if(labelTextMarginLeft > 0 || labelTextMarginTop > 0 || labelTextMarginRight > 0 || labelTextMarginBottom > 0) {
			setLabelTextMargin(labelTextMarginLeft, labelTextMarginTop, labelTextMarginRight, labelTextMarginBottom);
		}
		a.recycle();
		
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
			mMaxColumn = getColumn(mAdapter.getCount() - 1);
			
			if(mAdapter.hasSection()) {
				final int totalSectionNum = mAdapter.getTotalSectionNum();
				mSectionFirstColumns = new int[totalSectionNum];
				mSectionLastColumns = new int[totalSectionNum -1];
				mSectionLabelRect = new Rect[totalSectionNum];
				int columnCount = 0;
				for(int i=0; i<totalSectionNum; i++) {
					mSectionLabelRect[i] = new Rect();
					mSectionFirstColumns[i] = columnCount;
					final int sectionCount = mAdapter.getSectionCount(i);
					columnCount += (int)FloatMath.ceil((float)sectionCount / (float)mRows);
					if(columnCount - 1 < mMaxColumn) {
						mSectionLastColumns[i] = columnCount - 1;
					}
				}
				measureMinSingleTextDimension();
			}
			
			int position = lookForSelectablePosition(0, true);
			setSelectedPositionInt(position);
			setNextSelectedPositionInt(position);
			checkSelectionChanged();
		} else {
			checkSelectionChanged();
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
		for(int i=position; i<adapter.getCount(); i++) {
			if(adapter.isEnabled(i)) {
				return i;
			}
		}
		return INVALID_POSITION;
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
		setNextSelectedPositionInt(position);
		mLayoutMode = LAYOUT_SET_SELECTION;
		layoutChildren();
	}
	
	public void jumpToSection(int sectionIndex) {
		int itemCount = 0;
		for(int i=0; i<sectionIndex; i++) {
			itemCount += mAdapter.getSectionCount(i);
		}
		int sectionFirstPosition = itemCount;
		setNextSelectedPositionInt(sectionFirstPosition);
		mLayoutMode = LAYOUT_JUMP;
		layoutChildren();
	}
	
	private void setSelectedPositionInt(int position) {
		mSelectedPosition = position;
	}
	
	private void setNextSelectedPositionInt(int position) {
		mNextSelectedPosition = position;
	}
	
	private void adjustViewLeftAndRight() {
		final int fadingEdgeLength = getHorizontalFadingEdgeLength();
		View child = getChildAt(0);
		int delta = child.getLeft() - mListPadding.left - fadingEdgeLength;
		if(mFirstPosition!=0) {
			// It's ok to have some spacing before the first item if it is the
			// part of the horizontalSpacing.
			delta -= mHorizontalSpacing;
		}
		if(delta < 0) {
			// we want adjust too right not too left.
			delta = 0;
		}
		offsetChildrenLeftAndRight(-delta);
	}
	
	private void pinToRight(int childrenRight) {
		final int count = getChildCount();
		if(mFirstPosition + count == mAdapter.getCount() - 1) {
			final int right = getChildAt(count - 1).getRight();
			final int offset = childrenRight - right;
			offsetChildrenLeftAndRight(offset);
		}
	}
	
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
		adjustViewLeftAndRight();
		after = fillRight(motionCol + 1, referenceView.getRight() + horizontalSpacing);
		
		if(temp != null) {
			return temp;
		} else if( before != null) {
			return before;
		} else {
			return after;
		}
	}
	
	private View fillFromSelection(int selectedLeft, int childrenLeft, int childrenRight) {
		final int fadingEdgeLength = getHorizontalFadingEdgeLength();
		final int selectedPosition = mSelectedPosition;
		final int horizontalSpacing = mHorizontalSpacing;
		int column = getColumn(selectedPosition);
		
		View sel;
		View referenceView;
		
		int leftSelectionPixel = getLeftSelectionPixel(childrenLeft, fadingEdgeLength, column);
		int rightSelectionPixel = getRightSelectionPixel(childrenRight, fadingEdgeLength, column);
		sel = makeColumn(column, selectedLeft, true);
		// Possibly changed again in fillUp if we add rows above this one.
		mFirstPosition = getPositionRangeByColumn(column)[0];
		referenceView = mReferenceView;
		
		adjustForLeftFadingEdge(referenceView, leftSelectionPixel, rightSelectionPixel);
		adjustForRightFadingEdge(referenceView, leftSelectionPixel, rightSelectionPixel);
		
		int columnLeft = column - 1;
		if(columnLeft >= 0) {
			fillLeft(columnLeft, referenceView.getLeft() - horizontalSpacing);
		}
		int columnRight = column + 1;
		if(columnRight <= mMaxColumn) {
			fillRight(columnRight, referenceView.getRight() + horizontalSpacing);
		}
		
		return sel;
	}
	
	private View fillSelection(int childrenLeft, int childrenRight) {
		final int selectedPosition = reconcileSelectedPosition();
		final int fadingEdgeLength = getHorizontalFadingEdgeLength();
		final int horizontalSpacing = mHorizontalSpacing;
		
		int col = getColumn(selectedPosition);
		
		int leftSelectionPixel = getLeftSelectionPixel(childrenLeft, fadingEdgeLength, col);
		
		final View sel = makeColumn(col, leftSelectionPixel, true);
		
		mFirstPosition = getPositionRangeByColumn(col)[0];
		
		final View referenceView = mReferenceView;
		
		int columnRight = col + 1;
		if(columnRight <= mMaxColumn) {
			fillRight(columnRight, referenceView.getRight() + horizontalSpacing);
		}
		pinToRight(childrenRight);
		int columnLeft = col - 1;
		if(columnLeft >= 0) {
			fillLeft(columnLeft, referenceView.getLeft() - horizontalSpacing);
		}
		adjustViewLeftAndRight();
		
		return sel;
	}
	
	private View fillFromLeft(int nextLeft) {
		mFirstPosition = Math.min(mFirstPosition, mSelectedPosition);
		mFirstPosition = Math.min(mFirstPosition, mAdapter.getCount() - 1);
		if(mFirstPosition < 0) {
			mFirstPosition = 0;
		}
		if(mFirstPosition==0) {
			nextLeft += getHorizontalFadingEdgeLength();
		}
		int col = getColumn(mFirstPosition);
		return fillRight(col, nextLeft);
	}
	
	private View fillLeft(int col, int nextRight) {
		View selectedView = null;
		final int end = mListPadding.left;
		
		while(nextRight > end && col >=0) {
			
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
		
		while(nextLeft < end && col <= mMaxColumn) {
			
			View temp = makeColumn(col, nextLeft, true);
			if(temp != null) {
				selectedView = temp;
			}
			nextLeft = mReferenceView.getRight() + mHorizontalSpacing;
			
			col++;
		}
		return selectedView;
	}
	
	private void adjustForLeftFadingEdge(View childInSelectedRow, int leftSelectionPixel, int rightSelectionPixel) {
		// Some of newly selected view extends the left edge of the list.
		if(childInSelectedRow.getLeft() < leftSelectionPixel) {
			// Find the space required to bring the selected view fully into the view
			int spaceLeft = leftSelectionPixel - childInSelectedRow.getLeft();
			// Find the available right space of the selected view we can scroll right.
			int spaceRight = rightSelectionPixel - childInSelectedRow.getRight();
			
			int offset = Math.min(spaceLeft, spaceRight);
			// Now offset the selected item to get it into view.
			offsetChildrenLeftAndRight(offset);
		}
	}
	
	private void adjustForRightFadingEdge(View childInSelectedRow, int leftSelectionPixel, int rightSelectionPixel) {
		// Some of newly selected view extends the right edge of the list.
		if(childInSelectedRow.getRight() > rightSelectionPixel) {
			// Find the available left space of the selected view we can scroll left.
			int spaceLeft = childInSelectedRow.getRight() - leftSelectionPixel;
			int spaceRight = childInSelectedRow.getRight() - rightSelectionPixel;
			
			int offset = Math.min(spaceLeft, spaceRight);
			//Now offset the selected item to get it into view.
			offsetChildrenLeftAndRight(-offset);
		}
	}
	
	private View moveSelection(int delta, int childrenLeft, int childrenRight) {
		final int fadingEdgeLength = getHorizontalFadingEdgeLength();
		final int nextSelectedPosition = mNextSelectedPosition;
		final int horizontalSpacing = mHorizontalSpacing;
		View selectedView;
		View referenceView;
		
		int selectedPosition = nextSelectedPosition - delta;
		
		int oldCol = getColumn(selectedPosition);
		int col = getColumn(nextSelectedPosition);
		
		mFirstPosition = getPositionRangeByColumn(col)[0];
		int colDelta = col - oldCol;
		final int leftSelectionPixel = getLeftSelectionPixel(childrenLeft, fadingEdgeLength, col);
		final int rightSelectionPixel = getRightSelectionPixel(childrenRight, fadingEdgeLength, col);
		if(colDelta > 0) {
			/*
			 * Case 1: Scrolling Right
			 */
			final int oldRight = mReferenceViewInSelectedColumn == null ? 0: mReferenceViewInSelectedColumn.getRight();
			
			selectedView = makeColumn(col, oldRight + horizontalSpacing, true);
			referenceView = mReferenceView;
			adjustForRightFadingEdge(referenceView, leftSelectionPixel, rightSelectionPixel);
		} else if(colDelta < 0) {
			/*
			 * Case 2: Scrolling Left
			 */
			final int oldLeft = mReferenceViewInSelectedColumn == null ? 0: mReferenceViewInSelectedColumn.getLeft();
			
			selectedView = makeColumn(col, oldLeft - horizontalSpacing, false);
			referenceView = mReferenceView;
			adjustForLeftFadingEdge(referenceView, leftSelectionPixel, rightSelectionPixel);
		} else {
			/*
			 * Case 3: Keep selection where it was
			 */
			final int oldLeft = mReferenceViewInSelectedColumn == null ? 0: mReferenceViewInSelectedColumn.getLeft();
			selectedView = makeColumn(col, oldLeft, true);
			referenceView = mReferenceView;
			
		}
		
		int columnLeft = col - 1;
		if(columnLeft >= 0) {
			fillLeft(columnLeft, referenceView.getLeft() - horizontalSpacing);
		}
		int columnRight = col + 1;
		if(columnRight <= mMaxColumn) {
			fillRight(columnRight, referenceView.getRight() + horizontalSpacing);
		}
		return selectedView;
	}
	
	/**
	 * Calculate the left-most pixel that we can draw our selection. if we have a separator to left of selection,
	 * We need to keep the separator not in the fadingEdge.
	 * @param childrenLeft the left pixel were children can draw.
	 * @param fadingEdgeLength length of the fading edge pixel, if present.
	 * @param col the selection column.
	 * @return the left-most pixel that we can draw our selection.
	 */
	private int getLeftSelectionPixel(int childrenLeft, int fadingEdgeLength ,int col) {
		int leftSelectionPixel = childrenLeft;
		leftSelectionPixel += fadingEdgeLength;
		return leftSelectionPixel;
	}
	
	/**
	 * Calculate the right-most pixel that we can draw our selection.
	 * @param childrenLeft the right pixel were children can draw.
	 * @param fadingEdgeLength length of the fading edge pixel, if present.
	 * @param col the selection column.
	 * @return the right-most pixel that we can draw our selection.
	 */
	private int getRightSelectionPixel(int childrenRight, int fadingEdgeLength, int col) {
		int rightSelectionPixel = childrenRight;
		if(col<mMaxColumn) {
			rightSelectionPixel -= fadingEdgeLength;
		}
		return rightSelectionPixel;
	}

	private View makeColumn(int column, int x, boolean flow) {
		final int selectedPosition = mSelectedPosition;
		final int verticalSpacing = mVerticalSpace;
		int[] positionRange = getPositionRangeByColumn(column);
		View selectedView = null;
		View child = null;
		int nextTop = mListPadding.top + verticalSpacing;
		for(int pos = positionRange[0]; pos <= positionRange[1];pos++) {
			boolean selected = pos == selectedPosition;
			
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
		boolean updateChildSelected = isSelected != child.isSelected();
		boolean needToMeasure = !recycled || updateChildSelected || child.isLayoutRequested();
		
		HGridView.LayoutParams p = (HGridView.LayoutParams) child.getLayoutParams();
		if(p == null) {
			p = new HGridView.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.MATCH_PARENT);
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
				dispatchFinishTemporaryDetach(child);
			} else {
				mRecycler.addScrapView(scrapView);
			}
		} else {
			child = mAdapter.getView(position, null, this);
		}
		return child;
	}
	
	@Override
	protected void onLayout(boolean changed, int left, int top, int right,
			int bottom) {
		super.onLayout(changed, left, top, right, bottom);
		isInLayout = true;
		if (changed) {
            int childCount = getChildCount();
            for (int i = 0; i < childCount; i++) {
                getChildAt(i).forceLayout();
            }
            mRecycler.markChildrenDirty();
        }
		layoutChildren();
		isInLayout = false;
	}
	
	protected void layoutChildren() {
		
		
		
		try {
			invalidate();
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
			View sel = null;
			View oldSel = null;
			View oldFirst = null;
			View newSel = null;
			boolean needToScroll = false;
			int delta = 0;
			switch(mLayoutMode) {
			case LAYOUT_JUMP:
				index = mNextSelectedPosition - mFirstPosition;
				if(index >= 0 && index < childCount) {
					newSel = getChildAt(index);
				}
				int nextSectionIndex = mAdapter.getSectionIndex(mNextSelectedPosition);
				int lastIndexOfNextSection = index + mAdapter.getSectionCount(nextSectionIndex);
				if(lastIndexOfNextSection > 0 && lastIndexOfNextSection < childCount) {
					View lastViewOfNextSection = getChildAt(lastIndexOfNextSection);
					if(newSel!=null && lastViewOfNextSection!=null) {
						needToScroll = true;
					}
				}
				break;
			case LAYOUT_SET_SELECTION:
				index = mNextSelectedPosition - mFirstPosition;
				if(index >=0 && index < childCount) {
					newSel = getChildAt(index);
				}
				break;
			case LAYOUT_MOVE_SELECTION:
				delta = mNextSelectedPosition - mSelectedPosition;
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
				handleDataChanged();
			}
			
			if(mReferenceViewInSelectedColumn == null) {
				mReferenceViewInSelectedColumn = oldSel;
			}
			setSelectedPositionInt(mNextSelectedPosition);
			
			resetLabel();
			
			if(mDataChanged) {
				for(int i=0; i<childCount;i++) {
					recycleBin.addScrapView(getChildAt(i));
				}
			} else {
				recycleBin.fillActiveViews(childCount, firstPosition);
			}
			
			detachAllViewsFromParent();
			
			switch(mLayoutMode) {
			case LAYOUT_SYNC:
				sel = fillSpecific(mSyncPosition, mSpecificLeft);
				break;
			case LAYOUT_JUMP:
				if(needToScroll) {
					fillFromSelection(newSel.getLeft(), childrenLeft, childrenRight);
				} else {
					fillSelection(childrenLeft, childrenRight);
				}
				break;
			case LAYOUT_SET_SELECTION:
				if(newSel!=null) {
					sel = fillFromSelection(newSel.getLeft(), childrenLeft, childrenRight);
				} else {
					sel = fillSelection(childrenLeft, childrenRight);
				}
				break;
			case LAYOUT_MOVE_SELECTION:
//				int delta = newSel.getLeft() - mReferenceViewInSelectedColumn.getLeft();
				sel = moveSelection(delta, childrenLeft, childrenRight);
				break;
			default:
				if(childCount == 0) {
//					setSelectedPositionInt(mAdapter == null ? INVALID_POSITION : 0 );
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
			
			recycleBin.scrapActiveViews();
			
			if(sel!=null) {
				positionSelector(sel);
			} else {
				mSelectorRect.setEmpty();
			}
			mLayoutMode = LAYOUT_NORMAL;
			mDataChanged = false;
			if(mAdapter.getCount() > 0) {
				checkSelectionChanged();
			}
			invokeOnScrollListener();
		} finally {
			
		}
	}
	
	private void resetLabel() {
		if(mAdapter!=null && mAdapter.hasSection()) {
			for(Rect rect: mSectionLabelRect) {
				rect.setEmpty();
			}
		}
	}
	
	protected void positionSelector(View sel) {
		final Rect selectorRect = mSelectorRect;
		selectorRect.set(sel.getLeft(), sel.getTop(), sel.getRight(), sel.getBottom());
		positionSelector(selectorRect.left, selectorRect.top, selectorRect.right, selectorRect.bottom);
		refreshDrawableState();
	}
	
	private void positionSelector(int l, int t, int r, int b) {
        mSelectorRect.set(l - mSelectionLeftPadding, t - mSelectionTopPadding, r
                + mSelectionRightPadding, b + mSelectionBottomPadding);
    }

	@Override
	protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
		final Rect listPadding = mListPadding;
		listPadding.left = mSelectionLeftPadding + getPaddingLeft();
		listPadding.top = mSelectionTopPadding + getPaddingTop();
		listPadding.right = mSelectionRightPadding + getPaddingRight();
		listPadding.bottom = mSelectionBottomPadding + getPaddingBottom();
		
		int widthMode = MeasureSpec.getMode(widthMeasureSpec);
        int heightMode = MeasureSpec.getMode(heightMeasureSpec);
        int widthSize = MeasureSpec.getSize(widthMeasureSpec);
        int heightSize = MeasureSpec.getSize(heightMeasureSpec);
        
        if(heightMode == MeasureSpec.UNSPECIFIED) {
        	if(mRowHeight > 0) {
        		heightSize = mRowHeight + mListPadding.top + mListPadding.bottom;
        	} else {
        		heightSize = mListPadding.top + mListPadding.bottom;
        	}
        }
        
        int childWidth = 0;
        final int itemCount = mAdapter == null ? 0 : mAdapter.getCount();
        if(itemCount > 0) {
        	final View child = obtainView(0, mIsScrap);
        	HGridView.LayoutParams p = (HGridView.LayoutParams) child.getLayoutParams();
        	if(p==null) {
        		p = new HGridView.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.MATCH_PARENT);
        		child.setLayoutParams(p);
        	}
        	int childWidthSpec = getChildMeasureSpec(MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED), 0, p.height);
        	int childHeightSpec = getChildMeasureSpec(MeasureSpec.makeMeasureSpec(mRowHeight, MeasureSpec.EXACTLY), 0, p.width);
        	child.measure(childWidthSpec, childHeightSpec);
        	childWidth = child.getMeasuredWidth();
        	mRecycler.addScrapView(child);
        }
    	if (widthMode == MeasureSpec.UNSPECIFIED) {
            widthSize = mListPadding.left + mListPadding.right + childWidth +
                    getHorizontalFadingEdgeLength() * 2;
        }
    	
    	if(widthMode == MeasureSpec.AT_MOST) {
    		int ourSize =  mListPadding.left + mListPadding.right;
    		for(int i=0; i<= mMaxColumn; i++) {
    			ourSize += childWidth + mHorizontalSpacing;
    			if(ourSize > widthSize) {
    				ourSize = widthSize;
    				break;
    			}
    		}
    		widthSize = ourSize;
    	}
    	setMeasuredDimension(widthSize, heightSize);
    	mMeasuredHeight = heightSize;
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
		if(mDataChanged) {
			layoutChildren();
		}
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
			case KeyEvent.KEYCODE_DPAD_UP:
				handled = arrowScroll(FOCUS_UP);
				if(!handled) {
					handled = seekForOtherView(FOCUS_UP);
				}
				break;
			case KeyEvent.KEYCODE_DPAD_DOWN:
				handled = arrowScroll(FOCUS_DOWN);
				if(!handled) {
					handled = seekForOtherView(FOCUS_DOWN);
				}
				break;
			case KeyEvent.KEYCODE_DPAD_CENTER:
			case KeyEvent.KEYCODE_ENTER:
				if(getChildCount()>0 && repeatCount == 0) {
					keyPressed();
				}
				break;
			}
			
		}
		if((keyCode == KeyEvent.KEYCODE_DPAD_LEFT || keyCode == KeyEvent.KEYCODE_DPAD_RIGHT) && action == KeyEvent.ACTION_DOWN) {
			checkScrollState(OnScrollListener.SCROLL_STATE_FOCUS_MOVING);
		}
		if((keyCode == KeyEvent.KEYCODE_DPAD_LEFT || keyCode == KeyEvent.KEYCODE_DPAD_RIGHT) && action == KeyEvent.ACTION_UP) {
			checkScrollState(OnScrollListener.SCROLL_STATE_IDLE);
		}
		
		if(action==KeyEvent.ACTION_UP) {
			switch(keyCode) {
			case KeyEvent.KEYCODE_DPAD_CENTER:
			case KeyEvent.KEYCODE_ENTER:
				if(!isEnabled()) {
					return true;
				}
				if(isClickable() && isPressed() && mSelectedPosition>=0 && mAdapter!=null && mSelectedPosition < mAdapter.getCount()) {
					final View v = getChildAt(mSelectedPosition - mFirstPosition);
					if(v!=null) {
						performItemClick(v, mSelectedPosition, 0);
						v.setPressed(false);
					}
					setPressed(false);
					return true;
				}
				break;
			}
		}

		if(handled) {
			return true;
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
//		Log.d(TAG, "mSelectedPosition="+mSelectedPosition+" nextSelectedPosition=" + nextSelectedPosition);
		if(nextSelectedPosition==INVALID_POSITION) {
			return false;
		}

		if(nextSelectedPosition!=INVALID_POSITION) {
			setNextSelectedPositionInt(nextSelectedPosition);
		}

		if(nextSelectedPosition!=INVALID_POSITION) {
			mLayoutMode = LAYOUT_MOVE_SELECTION;
			layoutChildren();
		}
		
		return true;
	}
	
	private int lookForSelectablePositionOnScreen(int direction) {
		final int firstPosition = mFirstPosition;
		final int selectedPosition = mSelectedPosition;
		if(direction== FOCUS_LEFT) {
			final int lastVisiblePosition = mFirstPosition + getChildCount() - 1;
			final int currentSelectedColumn = selectedPosition==INVALID_POSITION ? getColumn(lastVisiblePosition): getColumn(selectedPosition);
			if(currentSelectedColumn<=0) {
				return INVALID_POSITION;
			}
			final int currentSelectedRow =selectedPosition==INVALID_POSITION ? 0 : getRow(selectedPosition);
			int[] positionRange = getPositionRangeByColumn(currentSelectedColumn - 1);
			// If next column has less rows than currentSelectedRow, just return the last row of next column. 
			return currentSelectedRow > positionRange[1] - positionRange[0] ? positionRange[1] : positionRange[0] + currentSelectedRow;
			
		} else if(direction==FOCUS_RIGHT) {
			if(selectedPosition >= mAdapter.getCount() - 1) {
				return INVALID_POSITION;
			}
			final int currentSelectedColumn = selectedPosition==INVALID_POSITION ? getColumn(firstPosition): getColumn(selectedPosition);
			if(currentSelectedColumn >= mMaxColumn) {
				return INVALID_POSITION;
			}
			final int currentSelectedRow =selectedPosition==INVALID_POSITION ? 0 : getRow(selectedPosition);
			int[] positionRange = getPositionRangeByColumn(currentSelectedColumn + 1);
			// If next column has less rows than currentSelectedRow, just return the last row of next column. 
			return currentSelectedRow > positionRange[1] - positionRange[0] ? positionRange[1] : positionRange[0] + currentSelectedRow;
		} else if(direction==FOCUS_UP) {
			if(selectedPosition<= firstPosition) {
				return INVALID_POSITION;
			}
			final int currentSelectedColumn = getColumn(selectedPosition);
			final int nextSelectedColumn = getColumn(selectedPosition - 1);
			if(nextSelectedColumn != currentSelectedColumn) {
				return INVALID_POSITION;
			} else {
				return selectedPosition -1;
			}
		} else {
			if(selectedPosition >= mAdapter.getCount() - 1) {
				return INVALID_POSITION;
			}
			final int currentSelectedColumn = getColumn(selectedPosition);
			final int nextSelectedColumn = getColumn(selectedPosition + 1);
			if(nextSelectedColumn != currentSelectedColumn) {
				return INVALID_POSITION;
			} else {
				return selectedPosition + 1;
			}
		}
	}
	
	/**
	 * Get zero based column according to given position.
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
				column += (position - itemCount) / mRows;
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
			int row = 0;
			if(mAdapter.hasSection()) {
				final int sectionIndex = mAdapter.getSectionIndex(position);
				int prevSectionItemCount = 0;
				for(int i=0; i<sectionIndex;i++) {
					prevSectionItemCount += mAdapter.getSectionCount(i);
				}
				int currentSectionCount = position - prevSectionItemCount;
				row = currentSectionCount % mRows;
			} else {
				row = position % mRows;
			}
			return row;
		}
		return INVALID_POSITION;
	}
	
	private int[] getPositionRangeByColumn(int column) {
		int[] positionRange = new int[2];
		
		final int totalSectionCount = mAdapter.getTotalSectionNum();
		int currentSectionIndex = 0;
		int itemCount = 0;
		int col = 0;
		for(int i=0; i<totalSectionCount; i++) {
			final int sectionCount = mAdapter.getSectionCount(i);
			final int sectionColumnCount =  (int)FloatMath.ceil((float)sectionCount / (float) mRows);
			if(col + sectionColumnCount > column) {
				currentSectionIndex = i;
				break;
			}
			col += sectionColumnCount;
			itemCount+=sectionCount;
		}
		int positionStart = itemCount + (column - col)* mRows;
		final int lastPositionOfCurrentSection = mAdapter.getSectionCount(currentSectionIndex) + itemCount - 1;
		int positionEnd = positionStart;
		while(positionEnd < positionStart + mRows && positionEnd <= lastPositionOfCurrentSection) {
			positionRange[1] = positionEnd;
			++positionEnd;
		}
		positionRange[0] = positionStart;
		
		return positionRange;
	}
	
	/**
     * @return A position to select. First we try mSelectedPosition. If that has been clobbered by
     * entering touch mode, we then try mResurrectToPosition. Values are pinned to the range
     * of items available in the adapter
     */
    int reconcileSelectedPosition() {
        int position = mSelectedPosition;
        if (position < 0) {
            position = mResurrectToPosition;
        }
        position = Math.max(0, position);
        position = Math.min(position, mAdapter.getCount() - 1);
        return position;
    }
    

//	private int getArrowScrollPreviewLength() {
//		return Math.max(MIN_SCROLL_PREVIEW_PIXELS, getHorizontalFadingEdgeLength());
//	}
	
//	private int getMaxScrollAmount() {
//		return (int) (MAX_SCROLL_FACTOR * (getRight() - getLeft()));
//	}
	
	public void offsetChildrenLeftAndRight(int offset) {
		final int count = getChildCount();
		for(int i=0; i<count; i++) {
			final View v = getChildAt(i);
			v.offsetLeftAndRight(offset);
		}
	}
	
	protected void resetList() {
		removeAllViewsInLayout();
		mDataChanged = false;
		mNeedSync = false;
		mOldSelectedPosition = INVALID_POSITION;
		setSelectedPositionInt(INVALID_POSITION);
		setNextSelectedPositionInt(INVALID_POSITION);
		mFirstPosition = 0;
		mSelectorRect.setEmpty();
		invalidate();
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
	
	@Override
    public boolean verifyDrawable(Drawable dr) {
        return mSelector == dr || super.verifyDrawable(dr);
    }
	
	@Override
	protected void dispatchDraw(Canvas canvas) {
		if(mAdapter!=null && mAdapter.hasSection() && getChildCount() > 0) {
			drawSectionLabels(canvas);
		}
		drawSelector(canvas);
		super.dispatchDraw(canvas);
	}
	
	/**
	 * Draw section labels.
	 * @param canvas
	 */
	private void drawSectionLabels(Canvas canvas) {
		final int firstPosition = mFirstPosition;
		final int firstVisibleColumn = getColumn(mFirstPosition);
		final int lastVisibleColumn = getColumn(mFirstPosition + getChildCount() - 1);
		final int leftBound = mListPadding.left;
		final int rightBound = getRight() - mListPadding.right;
		// look up all visible column on screen. check if it is in the mSectionFirstColumns
		// or mSectionLastColumns.
		for(int i=firstVisibleColumn; i<=lastVisibleColumn; i++) {
			final int sectionFirstColumnIndex = Arrays.binarySearch(mSectionFirstColumns, i);
			
			if(sectionFirstColumnIndex>=0) {
				Rect rect = mSectionLabelRect[sectionFirstColumnIndex];
				// we don't want set a Rect twice
				if(rect.isEmpty()) {
					final int columnStart = getPositionRangeByColumn(i)[0];
					final View referenceView = getChildAt(columnStart - firstPosition);
					// only if the left edge of referenceView is on screen, we will draw
					// its label before it.
					if(referenceView.getLeft() > leftBound) {
						rect.top = mListPadding.top + mVerticalSpace;
						rect.right = referenceView.getLeft();
						rect.left = rect.right - mHorizontalSpacing;
						rect.bottom = getBottom() - mListPadding.bottom;
					}
				}
			}
			
			final int sectionLastColumnIndex = Arrays.binarySearch(mSectionLastColumns, i);
			
			if(sectionLastColumnIndex >=0 ) {
				Rect rect = mSectionLabelRect[sectionLastColumnIndex + 1];
				if(rect.isEmpty()) {
					final int columnStart = getPositionRangeByColumn(i)[0];
					final View referenceView = getChildAt(columnStart - firstPosition);
					if(referenceView.getRight() < rightBound) {
						rect.top = mListPadding.top + mVerticalSpace;
						rect.left = referenceView.getRight();
						rect.right = rect.left + mHorizontalSpacing;
						rect.bottom = getBottom() - mListPadding.bottom;
					}
				}
			}
		}
		
		if(mLabelTextPaint==null) {
			mLabelTextPaint = new Paint();
		}
		
		for(int i=0; i< mSectionLabelRect.length; i++) {
			final Rect rect = mSectionLabelRect[i];
			if(!rect.isEmpty()) {
				String labelText = mAdapter.getLabelText(i);
				Drawable[] drawables = obtainLabelDrawable();
				int textLeft = rect.left;
				int textTop = rect.top;
				if(!mLabelTextMargin.isEmpty()) {
					textLeft += mLabelTextMargin.left;
					textTop += mLabelTextMargin.top;
					
				}
				if(drawables!=null && drawables.length>1) {
					
					Drawable backgroundDrawable = drawables[0];
					if(!mLabelBackgroundDrawableOffset.isEmpty()) {
						Rect backgroundRect = new Rect();
						backgroundRect.top = rect.top + mLabelBackgroundDrawableOffset.top;
						backgroundRect.left = rect.left + mLabelBackgroundDrawableOffset.left;
						backgroundRect.bottom = mLabelBackgroundDrawableOffset.bottom > 0 ? rect.top + mLabelBackgroundDrawableOffset.bottom : rect.bottom;
						backgroundRect.right = mLabelBackgroundDrawableOffset.right > 0 ? rect.left + mLabelBackgroundDrawableOffset.right : rect.right;
						backgroundDrawable.setBounds(backgroundRect);
					} else {
						backgroundDrawable.setBounds(rect);
					}
					
					backgroundDrawable.draw(canvas);
					
					Drawable labelDrawable = drawables[1];
					Rect labelRect;
					if(!mLabelDrawableOffset.isEmpty()) {
						labelRect = new Rect();
						labelRect.top = rect.top + mLabelDrawableOffset.top;
						labelRect.left = rect.left + mLabelDrawableOffset.left;
						labelRect.bottom = mLabelDrawableOffset.bottom > 0 ? rect.top + mLabelDrawableOffset.bottom : rect.bottom;
						labelRect.right = mLabelDrawableOffset.right > 0 ? rect.left + mLabelDrawableOffset.right : rect.right;
					} else {
						labelRect = rect;
					}
					textLeft = textLeft - rect.left + labelRect.left;
					textTop = textTop -rect.top + labelRect.top;
					// Ensure that label is large enough to hold the text.
					if(!mLabelTextMargin.isEmpty()) {
						int textHorizontalSpace = mLabelTextMargin.left + mMinSingleTextWidth + mLabelTextMargin.right;
						int textVerticalSpace = mLabelTextMargin.top + (mMinSingleTextHeight + mMinSingleTextHeight / 4) * labelText.length() + mLabelTextMargin.bottom;
						labelRect.right = labelRect.right >= labelRect.left + textHorizontalSpace ? labelRect.right : labelRect.left +textHorizontalSpace;
						labelRect.bottom = labelRect.bottom >= labelRect.top + textVerticalSpace ? labelRect.bottom : labelRect.top + textVerticalSpace;
					}
					labelDrawable.setBounds(labelRect);
					labelDrawable.draw(canvas);
				}
				
				for(int j=0; j<labelText.length(); j++) {
					canvas.drawText(labelText, j, j+1, textLeft, textTop, mLabelTextPaint);
					textTop += mMinSingleTextHeight + mMinSingleTextHeight /5;
				}
			}
		}
		
		// Test purpose
//		Log.d(TAG, "mScrapDrawables size = " + mRecycler.mScrapDrawables.size());
	}
	
	private Drawable[] obtainLabelDrawable() {
		Drawable[] drawables =  new Drawable[2];
		// label background drawable
		drawables[0] = getResources().getDrawable(mLabelBackgroundDrawableResId);
		// label drawable
		drawables[1] = getResources().getDrawable(mLabelDrawableResId);
		return drawables;
	}
	
	private void drawSelector(Canvas canvas) {
		if(shouldShowSelector() && mSelector!=null && !mSelectorRect.isEmpty()) {
			final Drawable selector = mSelector;
			selector.setBounds(mSelectorRect);
			selector.draw(canvas);
		}
	}
	
	@Override
    public void requestLayout() {
        if (!isInLayout) {
            super.requestLayout();
        }
    }
	
	private boolean isCandidateSelection(int childIndex, int direction) {
		
		int position = childIndex + mFirstPosition;
		int column = getColumn(position);
		int[] positionRange = getPositionRangeByColumn(column);
		switch(direction) {
		case FOCUS_UP:
			// coming from bottom, only valid if end of a column
			return childIndex == positionRange[1];
		case FOCUS_DOWN:
			// coming from top, only valid if start of a column.
			return childIndex == positionRange[0];
		case FOCUS_LEFT:
			// coming from right, need to be the last column.
			return positionRange[1] == getChildCount() - 1;
		case FOCUS_RIGHT:
			// coming from left, only vali if in the most left column.
			return positionRange[0] == 0;
		default:
            throw new IllegalArgumentException("direction must be one of "
                    + "{FOCUS_UP, FOCUS_DOWN, FOCUS_LEFT, FOCUS_RIGHT}.");
		}
	}
	
	@Override
	protected void onFocusChanged(boolean gainFocus, int direction,
			Rect previouslyFocusedRect) {
		super.onFocusChanged(gainFocus, direction, previouslyFocusedRect);
		int closestChildIndex = -1;
		if(gainFocus && previouslyFocusedRect != null) {
//			previouslyFocusedRect.offset(mScrollX, mScrollY);
			// figure out which item should be selected based on previously
            // focused rect
			Rect otherRect = mTempRect;
            int minDistance = Integer.MAX_VALUE;
            final int childCount = getChildCount();
            for(int i = 0; i < childCount; i++) {
            	if(!isCandidateSelection(i, direction)) {
            		continue;
            	}
            	final View other = getChildAt(i);
            	other.getDrawingRect(otherRect);
            	offsetDescendantRectToMyCoords(other, otherRect);
            	int distance = getDistance(previouslyFocusedRect, otherRect, direction);
            	if(distance < minDistance) {
            		minDistance = distance;
            		closestChildIndex = i;
            	}
            }
		}
		if(closestChildIndex >= 0) {
			setSelection(closestChildIndex + mFirstPosition);
		} else {
			requestLayout();
		}
	}
    /**
     * What is the distance between the source and destination rectangles given the direction of
     * focus navigation between them? The direction basically helps figure out more quickly what is
     * self evident by the relationship between the rects...
     *
     * @param source the source rectangle
     * @param dest the destination rectangle
     * @param direction the direction
     * @return the distance between the rectangles
     */
    public static int getDistance(Rect source, Rect dest, int direction) {
        int sX, sY; // source x, y
        int dX, dY; // dest x, y
        switch (direction) {
        case View.FOCUS_RIGHT:
            sX = source.right;
            sY = source.top + source.height() / 2;
            dX = dest.left;
            dY = dest.top + dest.height() / 2;
            break;
        case View.FOCUS_DOWN:
            sX = source.left + source.width() / 2;
            sY = source.bottom;
            dX = dest.left + dest.width() / 2;
            dY = dest.top;
            break;
        case View.FOCUS_LEFT:
            sX = source.left;
            sY = source.top + source.height() / 2;
            dX = dest.right;
            dY = dest.top + dest.height() / 2;
            break;
        case View.FOCUS_UP:
            sX = source.left + source.width() / 2;
            sY = source.top;
            dX = dest.left + dest.width() / 2;
            dY = dest.bottom;
            break;
        default:
            throw new IllegalArgumentException("direction must be one of "
                    + "{FOCUS_UP, FOCUS_DOWN, FOCUS_LEFT, FOCUS_RIGHT}.");
        }
        int deltaX = dX - sX;
        int deltaY = dY - sY;
        return deltaY * deltaY + deltaX * deltaX;
    }

	/**
     * Set a Drawable that should be used to highlight the currently selected item.
     *
     * @param resID A Drawable resource to use as the selection highlight.
     *
     * @attr ref android.R.styleable#AbsListView_listSelector
     */
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
        sel.setState(getDrawableState());
	}

	@Override
    protected void drawableStateChanged() {
        super.drawableStateChanged();
        if (mSelector != null) {
            mSelector.setState(getDrawableState());
        }
    }
	
	
	@Override
	protected int[] onCreateDrawableState(int extraSpace) {
		return super.onCreateDrawableState(extraSpace);
	}

	void hideSelector() {
        if (mSelectedPosition != INVALID_POSITION) {
            if (mLayoutMode != LAYOUT_SPECIFIC) {
                mResurrectToPosition = mSelectedPosition;
            }
            if (mNextSelectedPosition >= 0 && mNextSelectedPosition != mSelectedPosition) {
                mResurrectToPosition = mNextSelectedPosition;
            }
            setSelectedPositionInt(INVALID_POSITION);
            setNextSelectedPositionInt(INVALID_POSITION);
//            mSelectedTop = 0;
            mSelectorRect.setEmpty();
        }
    }
	
	public void setHorizontalSpacing(int spacing) {
		mHorizontalSpacing = spacing;
	}
	
	public void setVerticalSpacing(int spacing) {
		mVerticalSpace = spacing;
	}
	
	public void setRows(int rows) {
		mRows = rows;
	}
	
	public int getRows() {
		return mRows;
	}
	
	public void setLabelDrawableResId(int resId) {
		mLabelDrawableResId = resId;
	}
	
	public void setLabelBackgroundDrawableResId(int resId) {
		mLabelBackgroundDrawableResId = resId;
	}
	
	public void setLabelDrawableOffset(int left, int top, int right, int bottom) {
		mLabelDrawableOffset.set(left, top, right, bottom);
	}
	
	public void setLabelBackgroundDrawableOffset(int left, int top, int right, int bottom) {
		mLabelBackgroundDrawableOffset.set(left, top, right, bottom);
	}
	
	public void setLabelTextPaint(Paint paint) {
		
		measureMinSingleTextDimension();
		
		mLabelTextPaint = paint;
	}
	
	public void setLabelTextMargin(int left, int top, int right, int bottom) {
		mLabelTextMargin.set(left, top, right, bottom);
	}
	
	public void setOnScrollListener(OnScrollListener listener) {
		mOnScrollListener = listener;
	}
	
	private void measureMinSingleTextDimension() {
		
		if(mAdapter!=null && mAdapter.hasSection() && mLabelTextPaint!=null) {
			long startTime = System.currentTimeMillis();
			Rect measureRect = new Rect();
			for(int i=0; i<mAdapter.getTotalSectionNum(); i++) {
				String text = mAdapter.getLabelText(i);
				if(!TextUtils.isEmpty(text)) {
					for(int j=0; j<text.length(); j++) {
						mLabelTextPaint.getTextBounds(text, j, j+1, measureRect);
						mMinSingleTextHeight = Math.max(mMinSingleTextHeight, measureRect.bottom - measureRect.top);
						mMinSingleTextWidth = Math.max(mMinSingleTextWidth, measureRect.right - measureRect.left);
					}
				}
			}
			long timeCost = System.currentTimeMillis() - startTime;
			Log.d(TAG, "time cost :" + timeCost);
			Log.d(TAG, "min dimension is " +"w:"+ mMinSingleTextWidth + " h:" +mMinSingleTextHeight);
		}
	}
	
    public void dispatchStartTemporaryDetach(View child) {
    	child.onStartTemporaryDetach();
    	if(child instanceof ViewGroup) {
    		final int count = ((ViewGroup) child).getChildCount();
	        for (int i = 0; i < count; i++) {
	        	((ViewGroup) child).getChildAt(i).onStartTemporaryDetach();
	        }
    	}
        
    }
    
    public void dispatchFinishTemporaryDetach(View child) {
    	child.onFinishTemporaryDetach();
    	if(child instanceof ViewGroup) {
    		final int count = ((ViewGroup) child).getChildCount();
	        for (int i = 0; i < count; i++) {
	        	((ViewGroup) child).getChildAt(i).onFinishTemporaryDetach();
	        }
    	}
    }
	
	
	public static class LayoutParams extends ViewGroup.LayoutParams {
		
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
		
		private int mFirstActivePosition;
		
		private View[] mActiveViews = new View[0];
		
		private ArrayList<View> mScrapViews = new ArrayList<View>();
		
//		public ArrayList<Drawable[]> mScrapDrawables = new ArrayList<Drawable[]>();
		
		public void markChildrenDirty() {
			final ArrayList<View> scrap = mScrapViews;
            final int scrapCount = scrap.size();
            for (int i = 0; i < scrapCount; i++) {
                scrap.get(i).forceLayout();
            }
		}
		
		public void fillActiveViews(int childCount, int firstPosition) {
			if(mActiveViews.length < childCount) {
				mActiveViews = new View[childCount];
			}
			mFirstActivePosition = firstPosition;
			
			final View[] activeViews = mActiveViews;
			for(int i=0; i<childCount; i++) {
				View child = getChildAt(i);
				activeViews[i] = child;
			}
		}
		
		/**
         * Move all views remaining in mActiveViews to mScrapViews.
         */
		public View getActiveView(int position) {
			int index = position - mFirstActivePosition;
			final View[] activeViews = mActiveViews;
			if(index > 0 && index < activeViews.length) {
				final View match = activeViews[index];
				activeViews[index] = null;
				return match;
			}
			return null;
		}
		
		public void scrapActiveViews() {
			final View[] activeViews = mActiveViews;
			ArrayList<View> scrapViews = mScrapViews;
			final int count = activeViews.length;
			for(int i=count-1;i>=0;i--) {
				final View victim = activeViews[i];
				if(victim!=null) {
					activeViews[i] = null;
//					detachViewFromParent(victim);
					dispatchStartTemporaryDetach(victim);
					scrapViews.add(victim);
				}
			}
			pruneScrapViews();
		}
		
		/**
         * Makes sure that the size of mScrapViews does not exceed the size of mActiveViews.
         * (This can happen if an adapter does not recycle its views).
         */
		private void pruneScrapViews() {
			final int maxViews = mActiveViews.length;
			final ArrayList<View> scrapViews = mScrapViews;
			int size = scrapViews.size();
			final int extras = size - maxViews;
			size--;
			for(int i=0; i< extras; i++) {
				removeDetachedView(mScrapViews.remove(size--), false);
			}
		}
		
		public void addScrapView(View scrap) {
			HGridView.LayoutParams p = (HGridView.LayoutParams)scrap.getLayoutParams();
			if(p==null) {
				return;
			}
//			detachViewFromParent(scrap);
			dispatchStartTemporaryDetach(scrap);
			mScrapViews.add(scrap);
			
		}
		
		public View getScrapView(int position) {
			ArrayList<View> scrapViews = mScrapViews;
			int size = scrapViews.size();
			if(size>0) {
				return scrapViews.remove(size -1);
			} else {
				return null;
			}
		}
		
		public void clear() {
			final ArrayList<View> scrapView = mScrapViews;
			final int scrapCount = scrapView.size();
			for(int i=0; i<scrapCount; i++) {
				removeViewInLayout(scrapView.remove(scrapCount - 1 - i));
			}
//			mScrapDrawables.clear();
		}
		
//		public void addScrapDrawables(Drawable[] scrap) {
//			mScrapDrawables.add(scrap);
//		}
//		
//		public Drawable[] getScrapDrawables() {
//			ArrayList<Drawable[]> scrapDrawables = mScrapDrawables;
//			int size = scrapDrawables.size();
//			if(size > 0) {
//				return mScrapDrawables.remove(size - 1);
//			} else {
//				return null;
//			}
//		}
	}
	
	protected void invokeOnScrollListener() {
		if(mOnScrollListener!=null) {
			mOnScrollListener.onScroll(this, mFirstPosition, getChildCount(), mAdapter.getCount());
		}
	}
	
	/**
     * Interface definition for a callback to be invoked when the list or grid
     * has been scrolled.
     */
	public interface OnScrollListener {
		
		/**
         * The view is not scrolling. Note navigating the list using the trackball counts as
         * being in the idle state since these transitions are not animated.
         */
        public static int SCROLL_STATE_IDLE = 0;

        /**
         * The user is scrolling using touch, and their finger is still on the screen
         */
        public static int SCROLL_STATE_TOUCH_SCROLL = 1;

        /**
         * The user had previously been scrolling using touch and had performed a fling. The
         * animation is now coasting to a stop
         */
        public static int SCROLL_STATE_FLING = 2;

        /**
         * Callback method to be invoked while the grid view is being scrolled. If the
         * view is being scrolled, this method will be called before the next frame of the scroll is
         * rendered. In particular, it will be called before any calls to
         * {@link Adapter#getView(int, View, ViewGroup)}.
         *
         * @param view The view whose scroll state is being reported
         *
         * @param scrollState The current scroll state. One of {@link #SCROLL_STATE_IDLE},
         * {@link #SCROLL_STATE_TOUCH_SCROLL} or {@link #SCROLL_STATE_IDLE}.
         */
        public static int SCROLL_STATE_FOCUS_MOVING = 4;
        public void onScrollStateChanged(HGridView view, int scrollState);

        /**
         * Callback method to be invoked when the list or grid has been scrolled. This will be
         * called after the scroll has completed
         * @param view The view whose scroll state is being reported
         * @param firstVisibleItem the index of the first visible cell (ignore if
         *        visibleItemCount == 0)
         * @param visibleItemCount the number of visible cells
         * @param totalItemCount the number of items in the list adaptor
         */
        public void onScroll(HGridView view, int firstVisibleItem, int visibleItemCount,
                int totalItemCount);
	}
	
	class AdapterDataSetObserver extends DataSetObserver {

		@Override
		public void onChanged() {
			mDataChanged = true;
			rememberSyncState();
			mMaxColumn = getColumn(mAdapter.getCount() - 1);
			
			if(mAdapter.hasSection()) {
				final int totalSectionNum = mAdapter.getTotalSectionNum();
				mSectionFirstColumns = new int[totalSectionNum];
				mSectionLastColumns = new int[totalSectionNum -1];
				resetLabel();
				mSectionLabelRect = new Rect[totalSectionNum];
				int columnCount = 0;
				for(int i=0; i<totalSectionNum; i++) {
					mSectionLabelRect[i] = new Rect();
					mSectionFirstColumns[i] = columnCount;
					final int sectionCount = mAdapter.getSectionCount(i);
					columnCount += (int)FloatMath.ceil((float)sectionCount / (float)mRows);
					if(columnCount - 1 < mMaxColumn) {
						mSectionLastColumns[i] = columnCount - 1;
					}
				}
			}
			requestLayout();
		}

		@Override
		public void onInvalidated() {
			// TODO Auto-generated method stub
			super.onInvalidated();
		}
		
	}
	
	protected void rememberSyncState() {
		if(getChildCount() > 0) {
			mNeedSync = true;
			if(mSelectedPosition >= 0) {
				View v = getChildAt(mSelectedPosition - mFirstPosition);
				if(v != null) {
					mSpecificLeft = v.getLeft();
				}
				mSyncPosition = mNextSelectedPosition;
				mSyncMode = SYNC_SELECTED_POSITION;
			} else {
				View v = getChildAt(0);
				if(v != null) {
					mSpecificLeft = v.getLeft();
				}
				mSyncPosition = mFirstPosition;
				mSyncMode = SYNC_SELECTED_POSITION;
			}
		}
	}
	
	protected void handleDataChanged() {
		if(mNeedSync) {
			switch(mSyncMode) {
			case SYNC_SELECTED_POSITION:
				mLayoutMode = LAYOUT_SYNC;
				mSyncPosition = lookForSelectablePosition(mSyncPosition, true);
				setNextSelectedPositionInt(mSyncPosition);
				break;
			case SYNC_FIRST_POSITION:
				mLayoutMode = LAYOUT_SYNC;
				mSyncPosition = Math.min(Math.max(mSyncPosition, 0), mAdapter.getCount() - 1);
				break;
			}
		}
	}
	
	protected void keyPressed() {
		// We will support longClick in the future.
		if(!isEnabled() || !isClickable()) {
			return;
		}
		if(mSelector!=null && isFocused() && mSelectorRect != null && !mSelectorRect.isEmpty()) {
			final View v = getChildAt(mSelectedPosition - mFirstPosition);
			if(v!=null) {
				if(v.hasFocusable()) return;
				v.setPressed(true);
			}
			setPressed(true);
		}
	}
	
	private class SelectionNotifier implements Runnable {

		@Override
		public void run() {
			if(mDataChanged) {
				// Data has changed between when this SelectionNotifier
                // was posted and now. We need to wait until the AdapterView
                // has been synched to the new data.
				if(mAdapter!=null) {
					post(this);
				}
			} else {
				fireOnSelected();
			}
		}
		
	}
	
	protected void selectionChanged() {
		OnItemSelectedListener itemSelectedListener = getOnItemSelectedListener();
        if (itemSelectedListener != null) {
            if (isInLayout) {
                // If we are in a layout traversal, defer notification
                // by posting. This ensures that the view tree is
                // in a consistent state and is able to accomodate
                // new layout or invalidate requests.
                if (mSelectionNotifier == null) {
                    mSelectionNotifier = new SelectionNotifier();
                }
                post(mSelectionNotifier);
            } else {
                fireOnSelected();
            }
        }

        // we fire selection events here not in View
        if (mSelectedPosition != ListView.INVALID_POSITION && isShown() && !isInTouchMode()) {
            sendAccessibilityEvent(AccessibilityEvent.TYPE_VIEW_SELECTED);
        }
    }
	
	private void fireOnSelected() {
		OnItemSelectedListener itemSelectedListener = getOnItemSelectedListener();
        if (itemSelectedListener == null)
            return;

        int selection = mNextSelectedPosition;
        if (selection >= 0) {
            View v = getSelectedView();
            itemSelectedListener.onItemSelected(this, v, selection,
                    getAdapter().getItemId(selection));
        } else {
        	itemSelectedListener.onNothingSelected(this);
        }
    }
	
	protected void checkSelectionChanged() {
        if ((mSelectedPosition != mOldSelectedPosition)) {
            selectionChanged();
            mOldSelectedPosition = mSelectedPosition;
        }
    }
	
	private void checkScrollState(int newState) {
		if(mScrollState != newState && mAdapter != null) {
			if(mOnScrollListener!=null) {
				mOnScrollListener.onScrollStateChanged(this, newState);
			}
			mScrollState = newState;
		}
	}
}
