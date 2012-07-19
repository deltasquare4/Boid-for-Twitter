package com.teamboid.twitter.views;

import java.util.ArrayList;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.StringBuilder;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.os.SystemClock;
import android.os.Environment;
import android.util.AttributeSet;
import android.util.Log;
import android.view.GestureDetector;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.HeaderViewListAdapter;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.AbsListView;

import com.teamboid.twitter.R;

/**
 * ListView subclass that mediates drag and drop resorting of items. <br/>
 * <br/>
 * https://github.com/afollestad/drag-sort-listview
 * 
 * @author heycosmo
 * 
 */
public class DragSortListView extends ListView {

	public final static int NO_DRAG = 0;
	public final static int SRC_EXP = 1;
	public final static int SRC_ABOVE = 2;
	public final static int SRC_BELOW = 3;
	private int mDragState = NO_DRAG;
	private ImageView mFloatView;
	private int mFloatBGColor;
	private WindowManager mWindowManager;
	private WindowManager.LayoutParams mWindowParams;

	private int mExpDragPos;
	private int mSrcDragPos;
	private int mDragPointX;
	private int mDragPointY;
	private int mXOffset;
	private int mYOffset;
	private DragListener mDragListener;
	private DropListener mDropListener;
	private RemoveListener mRemoveListener;
	private int mUpScrollStartY;
	private int mDownScrollStartY;
	private float mDownScrollStartYF;
	private float mUpScrollStartYF;
	private GestureDetector mGestureDetector;
	private Rect mTempRect = new Rect();
	private int[] mTempLoc = new int[2];
	private Bitmap mDragBitmap;
	private int mItemHeightCollapsed = 1;
	private int mExpandedChildHeight;
	private int mFloatViewHeight;
	private int mFloatViewHeightHalf;
	private Drawable mTrashcan;

	private View[] mSampleViewTypes = new View[1];

	private ArrayList<Integer> mHeaderHeights = new ArrayList<Integer>();
	private ArrayList<Integer> mFooterHeights = new ArrayList<Integer>();

	private DragScroller mDragScroller;
	private float mDragUpScrollStartFrac = 1.0f / 3.0f;
	private float mDragDownScrollStartFrac = 1.0f / 3.0f;
	private float mDragUpScrollHeight;
	private float mDragDownScrollHeight;

	private float mMaxScrollSpeed = 0.3f;

	private boolean mTrackDragScroll = false;

	private DragScrollProfile mScrollProfile = new DragScrollProfile() {
		@Override
		public float getSpeed(float w, long t) {
			return mMaxScrollSpeed * w;
		}
	};

	private int mLastY;
	private int mDownY;

	private AdapterWrapper mAdapterWrapper;

	public DragSortListView(Context context, AttributeSet attrs) {
		super(context, attrs);
		if (attrs != null) {
			TypedArray a = getContext().obtainStyledAttributes(attrs, R.styleable.DragSortListView, 0, 0);
			mItemHeightCollapsed = a.getDimensionPixelSize(R.styleable.DragSortListView_collapsed_height, mItemHeightCollapsed);
			mTrackDragScroll = a.getBoolean(R.styleable.DragSortListView_track_drag_scroll, false);
			mFloatBGColor = a.getColor(R.styleable.DragSortListView_float_background_color, 0x00000000);
			float frac = a.getFloat(R.styleable.DragSortListView_drag_scroll_start, mDragUpScrollStartFrac);
			setDragScrollStart(frac);
			mMaxScrollSpeed = a.getFloat(R.styleable.DragSortListView_max_drag_scroll_speed, mMaxScrollSpeed);
			a.recycle();
		}
		mDragScroller = new DragScroller();
		setOnScrollListener(mDragScroller);
	}
	
	public void setMaxScrollSpeed(float max) { mMaxScrollSpeed = max; }

	@Override
	public void setAdapter(ListAdapter adapter) {
		mAdapterWrapper = new AdapterWrapper(null, null, adapter);
		super.setAdapter(mAdapterWrapper);
	}

	public ListAdapter getInputAdapter() {
		if (mAdapterWrapper == null) return null;
		else return mAdapterWrapper.getAdapter();
	}

	private class AdapterWrapper extends HeaderViewListAdapter {
		private ListAdapter mAdapter;
		public AdapterWrapper(ArrayList<FixedViewInfo> headerViewInfos, ArrayList<FixedViewInfo> footerViewInfos, ListAdapter adapter) {
			super(headerViewInfos, footerViewInfos, adapter);
			mAdapter = adapter;
		}
		public ListAdapter getAdapter() { return mAdapter; }
		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			RelativeLayout v;
			View child;
			if (convertView != null) {
				v = (RelativeLayout) convertView;
				View oldChild = v.getChildAt(0);
				child = mAdapter.getView(position, oldChild, v);
				if (child != oldChild) {
					v.removeViewAt(0);
					v.addView(child);
					v.setTag(child.findViewById(R.id.drag));
				}
			} else {
				AbsListView.LayoutParams params = new AbsListView.LayoutParams(
						ViewGroup.LayoutParams.MATCH_PARENT,
						ViewGroup.LayoutParams.WRAP_CONTENT);
				v = new RelativeLayout(getContext());
				v.setLayoutParams(params);
				child = mAdapter.getView(position, null, v);
				v.addView(child);
				v.setTag(child.findViewById(R.id.drag));
			}
			ViewGroup.LayoutParams lp = v.getLayoutParams();
			final int numHeaders = getHeaderViewsCount();
			final int srcAdapter = mSrcDragPos - numHeaders;
			final int expAdapter = mExpDragPos - numHeaders;
			boolean itemIsNormal = position != srcAdapter && position != expAdapter;
			boolean listHasExpPos = mDragState == SRC_ABOVE || mDragState == SRC_BELOW;
			boolean itemNeedsWC = itemIsNormal || !listHasExpPos;
			int oldHeight = lp.height;
			if (itemNeedsWC && lp.height != ViewGroup.LayoutParams.WRAP_CONTENT) {
				lp.height = ViewGroup.LayoutParams.WRAP_CONTENT;
			} else if (listHasExpPos) {
				if (position == srcAdapter && lp.height != mItemHeightCollapsed) {
					lp.height = mItemHeightCollapsed;
				} else if (position == expAdapter) {
					int spec = MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED);
					child.measure(spec, spec);
					mExpandedChildHeight = child.getMeasuredHeight();
					int height = mExpandedChildHeight + mFloatViewHeight;
					if (lp.height != height) lp.height = height;
					if (mDragState == SRC_ABOVE) {
						v.setGravity(Gravity.TOP);
					} else v.setGravity(Gravity.BOTTOM);
				}
			}
			if (lp.height != oldHeight) v.setLayoutParams(lp);
			int oldVis = v.getVisibility();
			int vis = oldVis;
			if (position == srcAdapter && mDragState != NO_DRAG && vis == View.VISIBLE) {
				vis = View.INVISIBLE;
			} else if (vis == View.INVISIBLE) vis = View.VISIBLE;
			if (vis != oldVis) v.setVisibility(vis);
			return v;
		}
	}

	private int getItemHeight(int position) {
		final int first = getFirstVisiblePosition();
		final int last = getLastVisiblePosition();
		if (position >= first && position <= last) {
			return getChildAt(position - first).getHeight();
		} else {
			final ListAdapter adapter = getAdapter();
			int type = adapter.getItemViewType(position);
			final int typeCount = adapter.getViewTypeCount();
			if (typeCount != mSampleViewTypes.length) mSampleViewTypes = new View[typeCount];
			View v;
			if (type >= 0) {
				if (mSampleViewTypes[type] == null) {
					v = adapter.getView(position, null, this);
					mSampleViewTypes[type] = v;
				} else v = adapter.getView(position, mSampleViewTypes[type], this);
			} else v = adapter.getView(position, null, this);
			ViewGroup.LayoutParams lp = v.getLayoutParams();
			final int height = lp == null ? 0 : lp.height;
			if (height > 0) return height;
			else {
				int spec = MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED);
				v.measure(spec, spec);
				return v.getMeasuredHeight();
			}
		}
	}

	private int getVisualItemHeight(int position) {
		final int divHeight = getDividerHeight();
		if (position == mExpDragPos) return mFloatViewHeight + divHeight;
		int height;
		switch (mDragState) {
		case SRC_ABOVE:
			if (position >= mSrcDragPos && position < mExpDragPos) {
				height = getItemHeight(position + 1);
				if (position == mSrcDragPos) height += mItemHeightCollapsed + divHeight;
				if (position == mExpDragPos - 1) height -= mFloatViewHeight;
				return height + divHeight;
			}
			break;
		case SRC_BELOW:
			if (position <= mSrcDragPos && position > mExpDragPos) {
				height = getItemHeight(position - 1);
				if (position == mSrcDragPos) height += mItemHeightCollapsed + divHeight;
				if (position == mExpDragPos + 1) height -= mFloatViewHeight;
				return height + divHeight;
			}
			break;
		default:
		}
		return getItemHeight(position) + getDividerHeight();
	}

	private int getDragEdge(int vposition, int vtop) {
		if (vposition == 0) return vtop;
		if (vposition <= mExpDragPos) {
			return vtop + (mFloatViewHeight - getVisualItemHeight(vposition - 1)) / 2;
		} else return vtop + (getVisualItemHeight(vposition) - mFloatViewHeight) / 2;
	}

	private int getFloatPosition(int y, int position, int top) {
		final int floatViewMidY = Math.max(
				mFloatViewHeightHalf + getPaddingTop(),
				Math.min(getHeight() - getPaddingBottom()
						- mFloatViewHeightHalf, y - mDragPointY
						+ mFloatViewHeightHalf));
		int visItemTop;
		int visItemPos;
		final int divHeight = getDividerHeight();
		switch (mDragState) {
		case SRC_ABOVE:
			visItemTop = top;
			if (position == mSrcDragPos + 1) visItemTop -= mItemHeightCollapsed + divHeight;
			if (position > mSrcDragPos && position <= mExpDragPos) {
				visItemPos = position - 1;
			} else visItemPos = position;
			break;
		case SRC_BELOW:
			visItemTop = top;
			if (position == mSrcDragPos) visItemTop += mItemHeightCollapsed + divHeight;
			if (position < mSrcDragPos && position >= mExpDragPos) {
				visItemPos = position + 1;
			} else visItemPos = position;
			break;
		default:
			visItemTop = top;
			visItemPos = position;
		}
		int edge = getDragEdge(visItemPos, visItemTop);
		if (floatViewMidY < edge) {
			while (visItemPos >= 0) {
				visItemPos--;
				if (visItemPos <= 0) {
					visItemPos = 0;
					break;
				}
				visItemTop -= getVisualItemHeight(visItemPos);
				edge = getDragEdge(visItemPos, visItemTop);
				if (floatViewMidY >= edge) break;
			}
		} else {
			final int count = getCount();
			while (visItemPos < count) {
				if (visItemPos == count - 1) break;
				visItemTop += getVisualItemHeight(visItemPos);
				edge = getDragEdge(visItemPos + 1, visItemTop);
				if (floatViewMidY < edge) break;
				visItemPos++;
			}
		}
		final int numHeaders = getHeaderViewsCount();
		final int numFooters = getFooterViewsCount();
		if (visItemPos < numHeaders) {
			return numHeaders;
		} else if (visItemPos >= getCount() - numFooters) {
			return getCount() - numFooters - 1;
		}
		return visItemPos;
	}

	@Override
	public boolean onInterceptTouchEvent(MotionEvent ev) {
		if (mDragListener != null || mDropListener != null) {
			switch (ev.getAction()) {
			case MotionEvent.ACTION_DOWN:
				int x = (int) ev.getX();
				int y = (int) ev.getY();
				mLastY = y;
				mDownY = y;
				int itemnum = pointToPosition(x, y);
				final int numHeaders = getHeaderViewsCount();
				final int numFooters = getFooterViewsCount();
				if (itemnum == AdapterView.INVALID_POSITION || itemnum < numHeaders || itemnum >= getCount() - numFooters) {
					break;
				}
				ViewGroup item = (ViewGroup) getChildAt(itemnum - getFirstVisiblePosition());
				mDragPointX = x - item.getLeft();
				mDragPointY = y - item.getTop();
				final int rawX = (int) ev.getRawX();
				final int rawY = (int) ev.getRawY();
				mXOffset = rawX - x;
				mYOffset = rawY - y;
				View dragBox = (View) item.getTag();
				boolean dragHit = false;
				if (dragBox != null) {
					dragBox.getLocationOnScreen(mTempLoc);
					dragHit = rawX > mTempLoc[0] && rawY > mTempLoc[1]
							&& rawX < mTempLoc[0] + dragBox.getWidth()
							&& rawY < mTempLoc[1] + dragBox.getHeight();
				}
				if (dragHit) {
					item.setDrawingCacheEnabled(true);
					Bitmap bitmap = Bitmap.createBitmap(item.getDrawingCache());
					item.setDrawingCacheEnabled(false);
					mFloatViewHeight = item.getHeight();
					mFloatViewHeightHalf = mFloatViewHeight / 2;
					mExpDragPos = itemnum;
					mSrcDragPos = itemnum;
					startDragging(bitmap, x, y);
					MotionEvent ev2 = MotionEvent.obtain(ev);
					ev2.setAction(MotionEvent.ACTION_CANCEL);
					super.onInterceptTouchEvent(ev2);
					return true;
				}
				removeFloatView();
				break;
			}
		}
		return super.onInterceptTouchEvent(ev);
	}

	public void setDragScrollStart(float heightFraction) {
		setDragScrollStarts(heightFraction, heightFraction);
	}

	public void setDragScrollStarts(float upperFrac, float lowerFrac) {
		if (lowerFrac > 0.5f) {
			mDragDownScrollStartFrac = 0.5f;
		} else mDragDownScrollStartFrac = lowerFrac;
		if (upperFrac > 0.5f) {
			mDragUpScrollStartFrac = 0.5f;
		} else mDragUpScrollStartFrac = upperFrac;
		if (getHeight() != 0) updateScrollStarts();
	}

	private void updateScrollStarts() {
		final int padTop = getPaddingTop();
		final int listHeight = getHeight() - padTop - getPaddingBottom();
		float heightF = (float) listHeight;
		mUpScrollStartYF = padTop + mDragUpScrollStartFrac * heightF;
		mDownScrollStartYF = padTop + (1.0f - mDragDownScrollStartFrac) * heightF;
		mUpScrollStartY = (int) mUpScrollStartYF;
		mDownScrollStartY = (int) mDownScrollStartYF;
		mDragUpScrollHeight = mUpScrollStartYF - padTop;
		mDragDownScrollHeight = padTop + listHeight - mDownScrollStartYF;
	}

	@Override
	protected void onSizeChanged(int w, int h, int oldw, int oldh) {
		super.onSizeChanged(w, h, oldw, oldh);
		updateScrollStarts();
	}

	private int getViewHeight(View v) {
		ViewGroup.LayoutParams lp = v.getLayoutParams();
		final int height = lp == null ? 0 : lp.height;
		if (height > 0) return height;
		else {
			int spec = MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED);
			v.measure(spec, spec);
			return v.getMeasuredHeight();
		}
	}

	@Override
	public void addHeaderView(View v, Object data, boolean isSelectable) {
		super.addHeaderView(v, data, isSelectable);
		mHeaderHeights.add(getViewHeight(v));
	}

	@Override
	public void addFooterView(View v, Object data, boolean isSelectable) {
		super.addFooterView(v, data, isSelectable);
		mFooterHeights.add(getViewHeight(v));
	}

	private void dropFloatView(boolean removeSrcItem) {
		mDragScroller.stopScrolling(true);
		if (removeSrcItem) {
			if (mRemoveListener != null) {
				mRemoveListener.remove(mSrcDragPos - getHeaderViewsCount());
			}
		} else {
			if (mDropListener != null && mExpDragPos >= 0 && mExpDragPos < getCount()) {
				final int numHeaders = getHeaderViewsCount();
				mDropListener.drop(mSrcDragPos - numHeaders, mExpDragPos - numHeaders);
			}
			int top = getChildAt(0).getTop();
			int firstPos = getFirstVisiblePosition();
			View expView = getChildAt(mExpDragPos - firstPos);
			if (expView != null) {
				ViewGroup.LayoutParams lp = expView.getLayoutParams();
				lp.height = ViewGroup.LayoutParams.WRAP_CONTENT;
				expView.requestLayout();
			}
			if (mSrcDragPos < firstPos) {
				setSelectionFromTop(firstPos - 1, top - getPaddingTop());
			} else if (mSrcDragPos <= getLastVisiblePosition()) {
				View srcView = getChildAt(mSrcDragPos - firstPos);
				ViewGroup.LayoutParams lp = srcView.getLayoutParams();
				lp.height = ViewGroup.LayoutParams.WRAP_CONTENT;
				srcView.requestLayout();
				srcView.setVisibility(View.VISIBLE);
			}
		}
		removeFloatView();
		mDragState = NO_DRAG;
	}

	private void updateListState() {
		if (mFloatView == null) {
			mDragState = NO_DRAG;
			return;
		} else if (mExpDragPos == mSrcDragPos) {
			mDragState = SRC_EXP;
		} else if (mSrcDragPos < mExpDragPos) {
			mDragState = SRC_ABOVE;
		} else mDragState = SRC_BELOW;
	}

	private void expandItem(int position) {
		final int first = getFirstVisiblePosition();
		RelativeLayout v = (RelativeLayout) getChildAt(position - first);
		if (v != null && mFloatView != null) {
			ViewGroup.LayoutParams lp = v.getLayoutParams();
			int oldHeight = lp.height;
			if (lp.height == mItemHeightCollapsed && position == mSrcDragPos) {
				lp.height = ViewGroup.LayoutParams.WRAP_CONTENT;
			} else if (lp.height == ViewGroup.LayoutParams.WRAP_CONTENT && position != mExpDragPos) {
				lp.height = v.getHeight() + mFloatViewHeight;
				if (position > mSrcDragPos) v.setGravity(Gravity.TOP);
				else v.setGravity(Gravity.BOTTOM);
			}
			if (lp.height != oldHeight) v.requestLayout();
		}
	}

	private void collapseItem(int position) {
		View v = getChildAt(position - getFirstVisiblePosition());
		if (v != null) {
			ViewGroup.LayoutParams lp = v.getLayoutParams();
			int oldHeight = lp.height;
			if (position == mSrcDragPos) {
				lp.height = mItemHeightCollapsed;
			} else if (position == mExpDragPos) {
				lp.height = ViewGroup.LayoutParams.WRAP_CONTENT;
			}
			if (lp.height != oldHeight) v.requestLayout();
		}
	}

	private boolean shuffleItems(int floatPos) {
		if (floatPos != mExpDragPos) {
			collapseItem(mExpDragPos);
			expandItem(floatPos);
			if (mDragListener != null) {
				final int numHeaders = getHeaderViewsCount();
				mDragListener.drag(mExpDragPos - numHeaders, floatPos - numHeaders);
			}
			mExpDragPos = floatPos;
			updateListState();
			return true;
		} else return false;
	}

	@Override
	protected void layoutChildren() {

		// we need to control calls to layoutChildren while
		// dragging to prevent things happening out of order
		if (mFloatView == null) {
			super.layoutChildren();
		}

	}

	@Override
	public boolean onTouchEvent(MotionEvent ev) {
		if (mGestureDetector != null) mGestureDetector.onTouchEvent(ev);
		if ((mDragListener != null || mDropListener != null)
				&& mFloatView != null) {
			int action = ev.getAction();
			switch (action & MotionEvent.ACTION_MASK) {
			case MotionEvent.ACTION_UP:
			case MotionEvent.ACTION_CANCEL:
				Rect r = mTempRect;
				mFloatView.getDrawingRect(r);
				dropFloatView(false);
				break;
			case MotionEvent.ACTION_MOVE:
				int x = (int) ev.getX();
				int y = (int) ev.getY();
				if (mLastY == mDownY) {
					final View item = getChildAt(mSrcDragPos - getFirstVisiblePosition());
					if (item != null) item.setVisibility(INVISIBLE);
				}
				dragView(x, y);
				if (!mDragScroller.isScrolling()) {
					final int first = getFirstVisiblePosition();
					final View startView = getChildAt(mExpDragPos - first);
					int startPos;
					int startTop;
					if (startView == null) {
						startPos = first + getChildCount() / 2;
						startTop = getChildAt(startPos - first).getTop();
						Log.d("mobeta", "startView was null");
					} else {
						startPos = mExpDragPos;
						startTop = startView.getTop();
					}
					boolean shuffled = shuffleItems(getFloatPosition(y, startPos, startTop));
					if (shuffled) super.layoutChildren();
				}
				if (y > mDownScrollStartY) {
					if (mLastY <= mDownScrollStartY) {
						if (mDragScroller.isScrolling()) {
							mDragScroller.stopScrolling(true);
						}
						mLastY = y;
						mDragScroller.startScrolling(DragScroller.DOWN);
					}
				} else if (y < mUpScrollStartY) {
					if (mLastY >= mUpScrollStartY) {
						if (mDragScroller.isScrolling()) {
							mDragScroller.stopScrolling(true);
						}
						mLastY = y;
						mDragScroller.startScrolling(DragScroller.UP);
					}
				} else if (mLastY > mDownScrollStartY || mLastY < mUpScrollStartY) {
					mDragScroller.stopScrolling(true);
				}
				mLastY = y;
				break;
			}
			return true;
		}
		return super.onTouchEvent(ev);
	}

	private void startDragging(Bitmap bm, int x, int y) {
		mWindowParams = new WindowManager.LayoutParams();
		mWindowParams.gravity = Gravity.TOP | Gravity.LEFT;
		mWindowParams.x = x - mDragPointX + mXOffset;
		mWindowParams.y = y - mDragPointY + mYOffset;
		mWindowParams.height = WindowManager.LayoutParams.WRAP_CONTENT;
		mWindowParams.width = WindowManager.LayoutParams.WRAP_CONTENT;
		mWindowParams.flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
				| WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE
				| WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
				| WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN
				| WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS;
		mWindowParams.format = PixelFormat.TRANSLUCENT;
		mWindowParams.windowAnimations = 0;
		Context context = getContext();
		ImageView v = new ImageView(context);
		v.setBackgroundColor(mFloatBGColor);
		v.setPadding(0, 0, 0, 0);
		v.setImageBitmap(bm);
		mDragBitmap = bm;
		mWindowManager = (WindowManager)context.getSystemService(Context.WINDOW_SERVICE);
		mWindowManager.addView(v, mWindowParams);
		mFloatView = v;
		mDragState = SRC_EXP;
	}

	private void dragView(int x, int y) {
		mWindowParams.x = 0;
		final int numHeaders = getHeaderViewsCount();
		final int numFooters = getFooterViewsCount();
		final int firstPos = getFirstVisiblePosition();
		final int lastPos = getLastVisiblePosition();
		int limit = getPaddingTop();
		if (firstPos < numHeaders) limit = getChildAt(numHeaders - firstPos - 1).getBottom();
		int footerLimit = getHeight() - getPaddingBottom();
		if (lastPos >= getCount() - numFooters) {
			footerLimit = getChildAt(getCount() - numFooters - firstPos).getTop();
		}
		if (y - mDragPointY < limit) {
			mWindowParams.y = mYOffset + limit;
		} else if (y - mDragPointY + mFloatViewHeight > footerLimit) {
			mWindowParams.y = mYOffset + footerLimit - mFloatViewHeight;
		} else mWindowParams.y = y - mDragPointY + mYOffset;
		mWindowManager.updateViewLayout(mFloatView, mWindowParams);
		if (mTrashcan != null) {
			int width = mFloatView.getWidth();
			if (y > getHeight() * 3 / 4) {
				mTrashcan.setLevel(2);
			} else if (width > 0 && x > width / 4) {
				mTrashcan.setLevel(1);
			} else mTrashcan.setLevel(0);
		}
	}

	private void removeFloatView() {
		if (mFloatView != null) {
			mFloatView.setVisibility(GONE);
			WindowManager wm = (WindowManager) getContext().getSystemService(Context.WINDOW_SERVICE);
			wm.removeView(mFloatView);
			mFloatView.setImageDrawable(null);
			mFloatView = null;
		}
		if (mDragBitmap != null) {
			mDragBitmap.recycle();
			mDragBitmap = null;
		}
		if (mTrashcan != null) mTrashcan.setLevel(0);
	}

	public void setTrashcan(Drawable trash) { mTrashcan = trash; }

	public void setDragListener(DragListener l) { mDragListener = l; }

	public void setDropListener(DropListener l) { mDropListener = l; }

	public interface DragListener {
		void drag(int from, int to);
	}

	public interface DropListener {
		void drop(int from, int to);
	}

	public interface RemoveListener {
		void remove(int which);
	}

	public void setDragScrollProfile(DragScrollProfile ssp) {
		if (ssp != null) mScrollProfile = ssp;
	}

	public interface DragScrollProfile {
		float getSpeed(float w, long t);
	}

	private class DragScroller implements Runnable, AbsListView.OnScrollListener {

		private boolean mAbort;
		private long mPrevTime;
		private int dy;
		private float dt;
		private long tStart;
		private int scrollDir;
		public final static int UP = 0;
		public final static int DOWN = 1;
		private float mScrollSpeed;
		private boolean mScrolling = false;
		private int mLastHeader;
		private int mFirstFooter;
		private StateTracker mStateTracker;

		public boolean isScrolling() {
			return mScrolling;
		}

		public DragScroller() {
			if (mTrackDragScroll) mStateTracker = new StateTracker();
		}

		public void startScrolling(int dir) {
			if (!mScrolling) {
				if (mTrackDragScroll) {
					mStateTracker.startTracking();
				}
				mAbort = false;
				mScrolling = true;
				tStart = SystemClock.uptimeMillis();
				mPrevTime = tStart;
				mLastHeader = getHeaderViewsCount() - 1;
				mFirstFooter = getCount() - getFooterViewsCount();
				scrollDir = dir;
				post(this);
			}
		}

		public void stopScrolling(boolean now) {
			if (now) {
				DragSortListView.this.removeCallbacks(this);
				mScrolling = false;
			} else mAbort = true;
			if (mTrackDragScroll) mStateTracker.stopTracking();
		}

		@Override
		public void run() {
			if (mAbort) {
				mScrolling = false;
				return;
			}
			if (mTrackDragScroll) mStateTracker.appendState();
			if (scrollDir == UP) {
				mScrollSpeed = mScrollProfile.getSpeed((mUpScrollStartYF - mLastY) / mDragUpScrollHeight, mPrevTime);
			} else mScrollSpeed = -mScrollProfile.getSpeed((mLastY - mDownScrollStartYF) / mDragDownScrollHeight, mPrevTime);
			dt = SystemClock.uptimeMillis() - mPrevTime;
			dy = (int) Math.round(mScrollSpeed * dt);
			if (dy == 0) {
				mPrevTime += dt;
				post(this);
				return;
			}
			final int first = getFirstVisiblePosition();
			final int last = getLastVisiblePosition();
			final int count = getCount();
			final int padTop = getPaddingTop();
			final int listHeight = getHeight() - padTop - getPaddingBottom();
			int movePosition;
			if (dy > 0) {
				if (first == 0 && getChildAt(0).getTop() == padTop) {
					mScrolling = false;
					return;
				}
				movePosition = first;
				dy = Math.min(listHeight, dy);
			} else {
				if (last == count - 1 && getChildAt(last - first).getBottom() <= listHeight + padTop) {
					mScrolling = false;
					return;
				}
				movePosition = last;
				dy = Math.max(-listHeight, dy);
			}

			final int oldTop = getChildAt(movePosition - first).getTop();
			int newTop = oldTop + dy;
			int newFloatPos = getFloatPosition(mLastY, movePosition, newTop);
			if (newFloatPos != mExpDragPos) {
				if (newFloatPos == movePosition && scrollDir == DragScroller.DOWN) {
					newTop -= mFloatViewHeight + getDividerHeight();
				}
				if (newFloatPos < movePosition && newFloatPos >= mSrcDragPos
						&& scrollDir == DragScroller.UP) {
					newTop += mFloatViewHeight + getDividerHeight();
				}
			}
			shuffleItems(newFloatPos);
			setSelectionFromTop(movePosition, newTop - getPaddingTop());
			DragSortListView.super.layoutChildren();
			mPrevTime += dt;
			post(this);
		}
		
		@Override
		public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount) {
			if (mScrolling && visibleItemCount != 0) {
				if (firstVisibleItem <= mLastHeader) {
					int dragViewTop = mLastY - mDragPointY;
					int lastHeaderBottom = getChildAt(mLastHeader - firstVisibleItem).getBottom();
					if (dragViewTop < lastHeaderBottom) {
						mWindowParams.y = mYOffset + lastHeaderBottom;
						mWindowManager.updateViewLayout(mFloatView, mWindowParams);
					}
				} else if (firstVisibleItem + visibleItemCount > mFirstFooter) {
					int dragViewBottom = mLastY - mDragPointY + mFloatViewHeight;
					int firstFooterTop = getChildAt(mFirstFooter - firstVisibleItem).getTop();
					if (dragViewBottom > firstFooterTop) {
						mWindowParams.y = mYOffset + firstFooterTop - mFloatViewHeight;
						mWindowManager.updateViewLayout(mFloatView, mWindowParams);
					}
				}
			}
		}
		@Override
		public void onScrollStateChanged(AbsListView view, int scrollState) { }
	}

	private class StateTracker {
		
		StringBuilder mBuilder = new StringBuilder();
		File mFile;
		private int mNumInBuffer = 0;
		private int mNumFlushes = 0;
		private boolean mTracking = false;
		
		public StateTracker() {
			File root = Environment.getExternalStorageDirectory();
			mFile = new File(root, "dslv_state.txt");
			if (!mFile.exists()) {
				try { mFile.createNewFile(); }
				catch (IOException e) { }
			}
		}

		public void startTracking() {
			mBuilder.append("<DSLVStates>\n");
			mNumFlushes = 0;
			mTracking = true;
		}

		public void appendState() {
			if (!mTracking) return;
			mBuilder.append("<DSLVState>\n");
			final int children = getChildCount();
			final int first = getFirstVisiblePosition();
			mBuilder.append("  <Positions>");
			for (int i = 0; i < children; ++i) {
				mBuilder.append(first + i).append(",");
			}
			mBuilder.append("</Positions>\n");
			mBuilder.append("  <Tops>");
			for (int i = 0; i < children; ++i) {
				mBuilder.append(getChildAt(i).getTop()).append(",");
			}
			mBuilder.append("</Tops>\n");
			mBuilder.append("  <Bottoms>");
			for (int i = 0; i < children; ++i) {
				mBuilder.append(getChildAt(i).getBottom()).append(",");
			}
			mBuilder.append("</Bottoms>\n");
			mBuilder.append("  <ExpPos>").append(mExpDragPos).append("</ExpPos>\n");
			mBuilder.append("  <SrcPos>").append(mSrcDragPos).append("</SrcPos>\n");
			mBuilder.append("  <DragState>").append(mDragState).append("</DragState>\n");
			mBuilder.append("  <SrcHeight>").append(mFloatViewHeight + getDividerHeight()).append("</SrcHeight>\n");
			mBuilder.append("  <ViewHeight>").append(getHeight()).append("</ViewHeight>\n");
			mBuilder.append("  <LastY>").append(mLastY).append("</LastY>\n");
			mBuilder.append("</DSLVState>\n");
			mNumInBuffer++;
			if (mNumInBuffer > 1000) {
				flush();
				mNumInBuffer = 0;
			}
		}

		public void flush() {
			if (!mTracking) return;
			try {
				boolean append = true;
				if (mNumFlushes == 0) append = false;
				FileWriter writer = new FileWriter(mFile, append);
				writer.write(mBuilder.toString());
				mBuilder.delete(0, mBuilder.length());
				writer.flush();
				writer.close();
				mNumFlushes++;
			} catch (IOException e) { }
		}

		public void stopTracking() {
			if (mTracking) {
				mBuilder.append("</DSLVStates>\n");
				flush();
				mTracking = false;
			}
		}
	}
}