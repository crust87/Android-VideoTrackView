/*
 * Android-VideoTrackView
 * https://github.com/crust87/Android-VideoTrackView
 *
 * Mabi
 * crust87@gmail.com
 * last modify 2015-12-14
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.crust87.videotrackview;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.graphics.RectF;
import android.media.MediaMetadataRetriever;
import android.os.AsyncTask;
import android.os.Handler;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.VelocityTracker;
import android.view.ViewConfiguration;
import android.widget.ListView;

import java.util.ArrayList;


public class VideoTrackView extends SurfaceView implements SurfaceHolder.Callback {

	/*
	Components
	 */
    private Context mContext;
	private Handler mHandler;
	private ArrayList<Bitmap> mThumbnailList;
	private Paint mBackgroundPaint;
	private Rect mBackgroundRect;
	private Track mTrack;
	private MediaMetadataRetriever mMediaMetadataRetriever;
	private OnTrackListener mOnTrackListener;
	private VelocityTracker mVelocityTracker;
	private VideoTrackOverlay mVideoTrackOverlay;

	/*
	Attributes
	 */
	private String mPath;
	private boolean isViewCreated;
    private float mScreenDuration;
	private int mThumbnailPerScreen;
	private float mThumbnailDuration;
	private int mTrackPadding;
	private int mWidth;						// view width
	private int mHeight;					// view height
	private int mVideoDuration;				// video duration
	private int mVideoDurationWidth;		// video duration in pixel
	private float mMillisecondsPerWidth;	// milliseconds per width
	private int thumbWidth;					// one second of width in pixel

	/*
	Working Variables
	 */
	private float pastX;					// past position x of touch event
	private float mVelocity;					// velocity of touch event;
	private AsyncTask<Void, Void, Void> mThumbnailTask;
	private boolean isLoading;

	/*
	Constructor
	 */
	public VideoTrackView(Context context) {
		super(context);
        mContext = context;
		mHandler = new Handler();

        initAttributes();
        initTrackView();
	}

    public VideoTrackView(Context context, AttributeSet attrs) {
        super(context, attrs);
        mContext = context;
		mHandler = new Handler();

        initAttributes(context, attrs, 0);
        initTrackView();
    }

    public VideoTrackView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        mContext = context;
		mHandler = new Handler();

        initAttributes(context, attrs, defStyleAttr);
        initTrackView();
    }

    private void initAttributes() {
        mScreenDuration = 30000f;
        mThumbnailPerScreen = 6;
        mThumbnailDuration = mScreenDuration / mThumbnailPerScreen;
		mTrackPadding = mContext.getResources().getDimensionPixelOffset(R.dimen.default_track_padding);
    }

    private void initAttributes(Context context, AttributeSet attrs, int defStyleAttr) {
        TypedArray typedArray = context.obtainStyledAttributes(attrs, R.styleable.VideoTrackView, defStyleAttr, 0);

        mScreenDuration = typedArray.getFloat(R.styleable.VideoTrackView_screen_duration, 30000f);
        mThumbnailPerScreen = typedArray.getInteger(R.styleable.VideoTrackView_thumbnail_per_screen, 6);
        mThumbnailDuration = mScreenDuration / mThumbnailPerScreen;

		int lDefaultTrackPadding = mContext.getResources().getDimensionPixelOffset(R.dimen.default_track_padding);
		mTrackPadding = typedArray.getDimensionPixelOffset(R.styleable.VideoTrackView_track_padding, lDefaultTrackPadding);

		typedArray.recycle();
		isLoading = false;
    }

    protected void initTrackView() {
		getHolder().addCallback(this);

        mThumbnailList = new ArrayList<>();
        mBackgroundPaint = new Paint();
        mBackgroundPaint.setColor(Color.parseColor("#222222"));
        setWillNotDraw(false);

		isViewCreated = false;
    }

	@Override
	public boolean onTouchEvent(MotionEvent event) {
		if (mVideoTrackOverlay != null) {
			if (mVideoTrackOverlay.onTrackTouchEvent(mTrack, event)) {
				invalidate();

				return true;
			}
		}

		switch (event.getAction()) {
			case MotionEvent.ACTION_DOWN:
				pastX = event.getX();

				if (mVelocityTracker == null) {
					mVelocityTracker = VelocityTracker.obtain();
				} else {
					mVelocityTracker.clear();
				}

				mAutoScrollRunnable.cancel();
				initOrResetVelocityTracker();
				mVelocityTracker.addMovement(event);
			case MotionEvent.ACTION_MOVE:
				updateTrackPosition(mTrack, (int) (event.getX() - pastX));
				pastX = event.getX();

				initVelocityTrackerIfNotExists();
				mVelocityTracker.addMovement(event);
				mVelocityTracker.computeCurrentVelocity(500);
				mVelocity = -mVelocityTracker.getXVelocity();
				break;
			case MotionEvent.ACTION_CANCEL:
			case MotionEvent.ACTION_UP:
//				mAutoScrollRunnable.start();
				recycleVelocityTracker();
				break;
		}

		if (mVelocityTracker != null) {
			mVelocityTracker.addMovement(event);
		}

		invalidate();

		return true;
	}

	class AutoScrollRunnable implements Runnable {
		private static final int DELAY = 50;

		public void start() {
			pastX = mVelocity;

			mHandler.postDelayed(this, DELAY);
		}

		public void cancel() {
			mVelocity = 0;
			mHandler.removeCallbacks(mAutoScrollRunnable);
		}

		@Override
		public void run() {
			if (updateTrackPosition(mTrack, (int) (mVelocity - pastX))) {
				mVelocity = 0;
			} else {
				pastX = mVelocity;

				mVelocity = mVelocity * 0.9f;
			}

			invalidate();

			if (mVelocity > 100 || mVelocity < -100) {
				mHandler.postDelayed(this, DELAY);
			}
		}
	}

	private AutoScrollRunnable mAutoScrollRunnable = new AutoScrollRunnable();

	private void initOrResetVelocityTracker() {
		if (mVelocityTracker == null) {
			mVelocityTracker = VelocityTracker.obtain();
		} else {
			mVelocityTracker.clear();
		}
	}

	private void initVelocityTrackerIfNotExists() {
		if (mVelocityTracker == null) {
			mVelocityTracker = VelocityTracker.obtain();
		}
	}

	private void recycleVelocityTracker() {
		if (mVelocityTracker != null) {
			mVelocityTracker.recycle();
			mVelocityTracker = null;
		}
	}

	// update track position
	// int x: it's actually delta x
	private boolean  updateTrackPosition(VideoTrackView.Track track, float x) {
		boolean isEnd = false;

		// check next position in boundary
		if(track.left + x > 0) {
			x = -track.left;

			isEnd = true;
		}

		if((track.right - mWidth) + x < 0) {
			x = 0 - (track.right - mWidth);

			isEnd = true;
		}

		track.left += x;
		track.right += x;

		return isEnd;
	}

	public boolean setVideo(String path) {
		mPath = path;

		if(path == null || mThumbnailList == null) {
			return false;
		}

		for(Bitmap b: mThumbnailList) {
			b.recycle();
		}

		if(!isViewCreated) {
			return false;
		}

		mThumbnailList.clear();
		mMediaMetadataRetriever = new  MediaMetadataRetriever();
		mMediaMetadataRetriever.setDataSource(path);

		String duration = mMediaMetadataRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION);

		try {
			mVideoDuration = Integer.parseInt(duration);
		} catch(NumberFormatException e) {
			mVideoDuration = 0;
		}

		if(mVideoDuration == 0) {
			return false;
		}

		mVideoDurationWidth = (int) (mVideoDuration * mMillisecondsPerWidth);
		mTrack.right = mVideoDurationWidth;

		isLoading = true;

		mThumbnailTask = new AsyncTask<Void, Void, Void>() {

			@Override
			protected void onPreExecute() {
				super.onPreExecute();

				if(mOnTrackListener != null) {
					mOnTrackListener.onLoadStart();
				}
			}

			@Override
			protected Void doInBackground(Void... params) {
				// create thumbnail for draw track
				long thumbDurationInMicro = (int) (mThumbnailDuration * 1000);
				for(long i = 0; i < mVideoDuration * 1000; i += thumbDurationInMicro) {
					if(isCancelled()) {
						return null;
					}

					Bitmap thumbnail = mMediaMetadataRetriever.getFrameAtTime(i);
					if(thumbnail != null) {
						mThumbnailList.add(scaleCenterCrop(thumbnail, thumbWidth, mHeight - (mTrackPadding * 2)));
						thumbnail.recycle();
						publishProgress();
					}
				}

				return null;
			}

			@Override
			protected void onProgressUpdate(Void... values) {
				invalidate();
			}

			@Override
			protected void onPostExecute(Void aVoid) {
				finishTask();

				if(mOnTrackListener != null) {
					mOnTrackListener.onLoadFinish();
				}
			}

			@Override
			protected void onCancelled() {
				super.onCancelled();

				finishTask();

				if(mOnTrackListener != null) {
					mOnTrackListener.onLoadCancel();
				}
			}

			private void finishTask() {
				if(mMediaMetadataRetriever != null) {
					mMediaMetadataRetriever.release();
					mMediaMetadataRetriever = null;
				}

				mThumbnailTask = null;

				isLoading = false;
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
		};

		mThumbnailTask.execute();

        invalidate();

		return true;
	}

	@Override
	public void surfaceCreated(SurfaceHolder holder) {

	}

	@Override
	public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
		mWidth = width;
		mHeight = height;

		initTrackSettings();
	}

	@Override
	public void surfaceDestroyed(SurfaceHolder holder) {
		mPath = null;
		isViewCreated = false;
	}

	@Override
	protected void onDraw(Canvas canvas) {
		super.onDraw(canvas);

		canvas.drawRect(mBackgroundRect, mBackgroundPaint);
		mTrack.draw(canvas);

		if (mVideoTrackOverlay != null) {
			mVideoTrackOverlay.drawOverlay(canvas);
		}
	}

	public void cancelLoadTask() {
		mThumbnailTask.cancel(true);
	}

	private void initTrackSettings() {
		mMillisecondsPerWidth = mWidth / mScreenDuration;
		mVideoDurationWidth = (int) (mVideoDuration * mMillisecondsPerWidth);
		thumbWidth = (int) (mMillisecondsPerWidth * mThumbnailDuration);

		mBackgroundRect = new Rect(0, 0, mWidth, mHeight);
		mTrack = new Track(0, mTrackPadding, mVideoDurationWidth, mHeight - mTrackPadding);

		if (mVideoTrackOverlay != null) {
			mVideoTrackOverlay.onSurfaceChanged(mWidth, mHeight);
		}

		isViewCreated = true;

		if (mVideoTrackOverlay != null) {
			mVideoTrackOverlay.onSetVideo(mMillisecondsPerWidth);
		}

		if(mPath != null) {
			setVideo(mPath);
		}
	}

    public void setScreenDuration(float screenDuration) {
        mScreenDuration = screenDuration;
		mThumbnailDuration = mScreenDuration / mThumbnailPerScreen;

		initTrackSettings();
    }

	public float getScreenDuration() {
		return mScreenDuration;
	}

    public void setThumbnailPerScreen(int thumbnailPerScreen) {
        mThumbnailPerScreen = thumbnailPerScreen;
		mThumbnailDuration = mScreenDuration / mThumbnailPerScreen;

		initTrackSettings();
    }

	public int getThumbnailPerScreen() {
		return mThumbnailPerScreen;
	}

	public void setTrackPadding(int trackPadding) {
		mTrackPadding = trackPadding;

		initTrackSettings();
	}

	public int getTrackPadding() {
		return mTrackPadding;
	}

	public boolean isLoading() {
		return isLoading;
	}

	public void setVideoTrackOverlay(VideoTrackOverlay videoTrackOverlay) {
		 mVideoTrackOverlay = videoTrackOverlay;
	}

	// Track class
	public class Track {
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

		private Track(int left, int top, int right, int bottom) {
			mTrackPaint = new Paint();
			mTrackPaint.setColor(Color.parseColor("#000000"));
			mTrackPaint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.SRC_ATOP));
			mThumbnailPaint = new Paint();

			mBitmapPaint = new Paint();

			lineBoundLeft = -thumbWidth;
			lineBoundRight = mWidth;

			this.left = left;
			this.top = top;
			this.right = right;
			this.bottom = bottom;

			mBound = new Rect(0, top, mWidth, bottom);

			mBitmap = Bitmap.createBitmap(mWidth, mHeight, Bitmap.Config.ARGB_8888);
			mCanvas = new Canvas(mBitmap);
		}

		private void draw(Canvas canvas) {
			mCanvas.drawRect(0, 0, mWidth, mHeight, mBackgroundPaint);
			mCanvas.drawRect(left, top, right, bottom, mTrackPaint);
            drawThumbnail();
			mCanvas.drawRect(right, 0, mWidth, mHeight, mBackgroundPaint);
			canvas.drawBitmap(mBitmap, null, mBound, mBitmapPaint);
		}

		private void drawThumbnail() {
            int i = 0;
			while(i < mThumbnailList.size()) {
				int x = (left + i * thumbWidth);

				if(x < lineBoundLeft) {
					int next = x / -thumbWidth < 1 ? 1 : x / -thumbWidth;
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

	public void setOnTrackListener(OnTrackListener onTrackListener) {
		mOnTrackListener = onTrackListener;
	}

	public interface OnTrackListener {
		void onLoadStart();
		void onLoadFinish();
		void onLoadCancel();
	}
}
