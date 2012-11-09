package org.sakuratya.horizontal.ui;

import javax.crypto.spec.PSource;

import org.sakuratya.horizontal.adapter.HGridAdapter;

import android.content.Context;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.util.FloatMath;
import android.view.KeyEvent;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListAdapter;
import android.widget.Scroller;

/**
 * These is a horizontal scroll GridView with arrow key action support and a dataset of sections.
 * You need to implement an adapter base on {@link HGridAdapter}. if you have a dataset which is separated by sections. 
 * The {@link HGridAdapter#getSectionIndex(int)} should return its section index(based on 0).
 * @author bob
 *
 */
public class HGridView extends AdapterView<HGridAdapter> {
	
	
	private HGridAdapter mAdapter;
	
	private static final int INVALID_POSITION = -1;
	private int mSelectedPosition;
	private int mNextSelectedPosition;
	
	/**
	 * Set rows per column.
	 */
	private int mRows;
	private int mFirstPosition;
	private Scroller mScroller;
	private int mScrollAmount = 0;
	private int mCurrentX;
	
	private int mNextX;
	
	private Rect mListPadding;
	
	private int mSelectorLeftPadding;
	private int mSelectorTopPadding;
	private int mSelectorRightPadding;
	private int mSelectorBottomPadding;
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
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void setAdapter(HGridAdapter adapter) {
		
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
	
	
	private void setNextSelectionInt(int position) {
		mNextSelectedPosition = position;
	}
	
	@Override
	protected void onLayout(boolean changed, int left, int top, int right,
			int bottom) {
		// TODO Auto-generated method stub
		super.onLayout(changed, left, top, right, bottom);
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
		
		View selectedView = getSelectedView();
		int nextSelectedPosition = lookForSelectablePositionOnScreen(direction);
		int amountToScroll = amountToScroll(direction, nextSelectedPosition);
		
		if(nextSelectedPosition!=INVALID_POSITION) {
			setNextSelectionInt(nextSelectedPosition);
		}
		if(amountToScroll>0) {
			doScroll(direction==FOCUS_RIGHT?amountToScroll:-amountToScroll);
		}
		return false;
	}
	
	private void doScroll(int delta) {
		if(delta!=0) {
			scrollBy(delta, 0);
		}
	}
	
	
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
	
	private void makeRow() {
		
	}

}
