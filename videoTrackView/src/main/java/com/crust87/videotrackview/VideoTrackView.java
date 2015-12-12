package com.crust87.videotrackview;

import java.util.ArrayList;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.graphics.RectF;
import android.media.MediaMetadataRetriever;
import android.util.TypedValue;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;


public class VideoTrackView extends SurfaceView implements SurfaceHolder.Callback {
	private static enum ACTION_TYPE {anchor, normal, idle}	// touch event action type

	private float SCREEN_DURATION = 15000f;
	private float MAX_DURATION = 15000f;	// maximum of duration
	private float MIN_DURATION = 4000f;		// minimum of duration

	// Components for SurfaceView
	private SurfaceHolder mHolder;

	// Components
	private ArrayList<Bitmap> mThumbnailList;
	private Paint mBackgroundPaint;
	private Rect mBackgroundRect;
	private Paint mDisablePaint;
	private Rect mDisableRect;
	private Track mTrack;
	private Anchor mAnchor;					// duration anchor

	// Attributes
	private int mWidth;						// view width
	private int mHeight;					// view height
	private int mVideoDuration;				// video duration
	private int mVideoDurationWidth;		// video duration in pixel
	private float secPerWidth;				// seconds per width
	private int minWidth;					// minimum duration in pixel
	private int maxWidth;					// maximum duration in pixel
	private int secWidth;					// one second of width in pixel

	// Layout Attribyte;
	private int dp1;
	private int dp2;
	private int dp4;
	private int dp12;
	private int dp18;
	private int dp80;

	// Working Variable;
	private int currentPosition;			// current start position
	private int currentDuration;			// current duration position
	private float pastX;					// past position x of touch event
	private ACTION_TYPE actionType;			// current touche event type

	// event listener
	private OnUpdatePositionListener mOnUpdatePositionListener;

	public VideoTrackView(Context context) {
		super(context);

		mHolder = getHolder();
		mHolder.addCallback(this);

		dp1 = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 1, context.getResources().getDisplayMetrics());
		dp2 = dp1 * 2;
		dp4 = dp1 * 4;
		dp12 = dp1 * 12;
		dp18 = dp1 * 18;
		dp80 = dp1 * 80;

		mThumbnailList = new ArrayList<Bitmap>();
		mBackgroundPaint = new Paint();
		mBackgroundPaint.setColor(Color.parseColor("#222222"));
		mDisablePaint = new Paint(Color.parseColor("#000000"));
		mDisablePaint.setAlpha(192);
		mAnchor = new Anchor();
		setWillNotDraw(false);
	}

	@Override
	public boolean onTouchEvent(MotionEvent event) {

		switch (event.getAction()) {
			case MotionEvent.ACTION_DOWN:
				if(mOnUpdatePositionListener != null) {
					mOnUpdatePositionListener.onUpdatePositionStart();
				}
				pastX = event.getX();

				// check event type
				if(mAnchor.contains(event.getX())) {
					actionType = ACTION_TYPE.anchor;
				} else {
					actionType = ACTION_TYPE.normal;
				}
			case MotionEvent.ACTION_MOVE:
				// do event process
				switch(actionType) {
					case anchor:
						updateAnchorPosition(event.getX() - pastX);
						pastX = event.getX();
						break;
					case normal:
						updateTrackPosition((int) (event.getX() - pastX));
						pastX = event.getX();
						break;
				}
				break;
			case MotionEvent.ACTION_UP:
				if(mOnUpdatePositionListener != null) {
					mOnUpdatePositionListener.onUpdatePositionEnd(currentPosition, currentDuration);
				}
				// action type to idle
				actionType = ACTION_TYPE.idle;
		}

		return true;
	}

	public boolean setVideo(String path) {
		mThumbnailList.clear();
		MediaMetadataRetriever retriever = new  MediaMetadataRetriever();
		retriever.setDataSource(path);

		String duration = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION);

		try {
			mVideoDuration = Integer.parseInt(duration);
		} catch(NumberFormatException e) {
			mVideoDuration = 0;
		}

		if(mVideoDuration == 0) {
			return false;
		}

		mVideoDurationWidth = (int) (mVideoDuration * secPerWidth);
		currentPosition = 0;
		currentDuration = (mVideoDuration - currentPosition) > 8000 ? 8000 : (mVideoDuration - currentPosition);

		mTrack.right = mVideoDurationWidth;
		mAnchor.position = (int) (currentDuration * secPerWidth);
		mDisableRect.left = (int) mAnchor.position;

		// create thumbnail for draw track
		for(int i = 0; i < mVideoDuration * 1000; i += 2000000) {
			Bitmap thumbnail = retriever.getFrameAtTime(i);
			if(thumbnail != null) {
				mThumbnailList.add(scaleCenterCrop(thumbnail, secWidth, dp80));
				thumbnail.recycle();
			}
		}

		retriever.release();
		invalidate();
		return true;
	}

	// return current duration
	public int getDuration() {
		return currentDuration;
	}

	// update track position
	// int x: it's actually delta x
	private void updateTrackPosition(float x) {
		// check next position in boundary
		if(mTrack.left + x > 0) {
			x = -mTrack.left;
		}

		if(mTrack.right + x < minWidth) {
			x = minWidth - mTrack.right;
		}

		mTrack.left += x;
		mTrack.right += x;

		currentPosition = (int) -(mTrack.left / secPerWidth);
		if(x < 0) {
			int nextDuration = mVideoDuration - currentPosition;
			currentDuration = nextDuration > currentDuration ? currentDuration : nextDuration;
			mAnchor.position = (int) (currentDuration * secPerWidth);
			mDisableRect.left = (int) mAnchor.position;
		}

		if(mOnUpdatePositionListener != null) {
			mOnUpdatePositionListener.onUpdatePosition(currentPosition, currentDuration);
		}

		invalidate();
	}

	private void updateAnchorPosition(float x) {
		// check next position in boundary
		if(mAnchor.position + x < minWidth) {
			x = minWidth - mAnchor.position;
		}

		if(mAnchor.position + x > mTrack.right) {
			x = mTrack.right - mAnchor.position;
		} else if(mAnchor.position + x > maxWidth) {
			x = maxWidth - mAnchor.position;
		}

		mAnchor.position += x;
		mDisableRect.left = (int) mAnchor.position;

		currentDuration = (int) (mAnchor.position / secPerWidth);
		if(mOnUpdatePositionListener != null) {
			mOnUpdatePositionListener.onUpdatePosition(currentPosition, currentDuration);
		}

		invalidate();
	}

	@Override
	public void surfaceCreated(SurfaceHolder holder) {

	}

	@Override
	public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
		mWidth = width;
		mHeight = height;

		secPerWidth = mWidth / SCREEN_DURATION;
		minWidth = (int) (MIN_DURATION * secPerWidth);
		maxWidth = (int) (MAX_DURATION * secPerWidth);
		secWidth = (int) (secPerWidth * 2000);

		mBackgroundRect = new Rect(0, 0, mWidth, mHeight);
		mTrack = new Track(0, dp4, 0, mHeight - dp4);
		mDisableRect = new Rect(0, 0, mWidth, mHeight);
	}

	@Override
	public void surfaceDestroyed(SurfaceHolder holder) {
	}

	@Override
	protected void onDraw(Canvas canvas) {
		super.onDraw(canvas);

		canvas.drawRect(mBackgroundRect, mBackgroundPaint);
		mTrack.draw(canvas);
		canvas.drawRect(mDisableRect, mDisablePaint);
		mAnchor.draw(canvas);
	}

	private Bitmap scaleCenterCrop(Bitmap source, int newWidth, int newHeight) {
		int sourceWidth = source.getWidth();
		int sourceHeight = source.getHeight();

		float xScale = (float) newWidth / sourceWidth;
		float yScale = (float) newHeight / sourceHeight;
		float scale = Math.max(xScale, yScale);

		float scaledWidth = scale * sourceWidth;
		float scaledHeight = scale * sourceHeight;

		float left = (newWidth - scaledWidth) / 2;
		float top = (newHeight - scaledHeight) / 2;

		RectF targetRect = new RectF(left, top, left + scaledWidth, top + scaledHeight);

		Bitmap dest = Bitmap.createBitmap(newWidth, newHeight, source.getConfig());
		Canvas canvas = new Canvas(dest);
		canvas.drawBitmap(source, null, targetRect, null);

		return dest;
	}

	public void setOnUpdatePositionListener(OnUpdatePositionListener pOnUpdatePositionListener) {
		mOnUpdatePositionListener = pOnUpdatePositionListener;
	}

	// video time position change listener
	public interface OnUpdatePositionListener {
		public abstract void onUpdatePositionStart();
		public abstract void onUpdatePosition(int seek, int duration);
		public abstract void onUpdatePositionEnd(int seek, int duration);
	}

	// Track class
	private class Track {
		private Paint mTrackPaint;
		private Paint mThumbnailPaint;
		private Paint mBitmapPaint;

		public int left;
		public int top;
		public int right;
		public int bottom;

		private int lineBoundLeft;
		private int lineBoundRight;

		private Bitmap mBitmap;
		private Canvas mCanvas;

		private Rect mBound;

		public Track(int left, int top, int right, int bottom) {
			mTrackPaint = new Paint();
			mTrackPaint.setColor(Color.parseColor("#000000"));
			mTrackPaint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.SRC_ATOP));
			mThumbnailPaint = new Paint();
			mThumbnailPaint.setColor(Color.parseColor("#ffffff"));
			mBitmapPaint = new Paint();

			lineBoundLeft = -secWidth;
			lineBoundRight = mWidth;

			this.left = left;
			this.top = top;
			this.right = right;
			this.bottom = bottom;

			mBound = new Rect(0, top, mWidth, bottom);

			mBitmap = Bitmap.createBitmap(mWidth, mHeight, Bitmap.Config.ARGB_8888);
			mCanvas = new Canvas(mBitmap);
		}

		public void draw(Canvas canvas) {
			mCanvas.drawRect(0, 0, mWidth, mHeight, mBackgroundPaint);
			mCanvas.drawRect(left, top, right, bottom, mTrackPaint);
			drawBitmap();
			mCanvas.drawRect(right, 0, mWidth, mHeight, mBackgroundPaint);
			canvas.drawBitmap(mBitmap, null, mBound, mBitmapPaint);
		}

		private void drawBitmap() {
			int i = 0;

			while(i < mThumbnailList.size()) {
				int x = (int) (left + i * secWidth);

				if(x < lineBoundLeft) {
					int next = x / -secWidth < 1 ? 1 : x / -secWidth;
					i += next;
					continue;
				}

				if(x > lineBoundRight) {
					break;
				}

				// draw thumbnail it's if visible
				mCanvas.drawBitmap(mThumbnailList.get(i), x, top, mThumbnailPaint);
				i++;
			}
		}

	}

	// Track anchor class
	private class Anchor {
		private Paint mAnchorPaint;
		public float position;

		public Anchor() {
			mAnchorPaint = new Paint();
			mAnchorPaint.setColor(Color.parseColor("#ffffff"));
		}

		public boolean contains(float x) {
			if(x > (position - dp12 - dp18) && x < (position + dp18)) {
				return true;
			} else {
				return false;
			}
		}

		public void draw(Canvas canvas) {
			canvas.drawRoundRect(new RectF(position - dp12, 0, position, mHeight), dp2, dp2, mAnchorPaint);
		}
	}

}
