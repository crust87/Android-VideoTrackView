package com.crust87.videotrackview;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.SurfaceHolder;

/**
 * Created by mabi on 2015. 12. 15..
 */
public class AnchorVideoTrackView extends VideoTrackView {
    private enum ACTION_TYPE {anchor, normal, idle}	// touch event action type

    private Paint mDisablePaint;
    private Rect mDisableRect;

    private Anchor mAnchor;
    private OnUpdateAnchorListener mOnUpdateAnchorListener;

    private int mDefaultAnchorPosition;
    private int mAnchorWidth;
    private int mAnchorRound;
    private int mAnchorArea;

    private int currentDuration;			// current duration position
    private ACTION_TYPE actionType;			// current touche event type

    public AnchorVideoTrackView(Context context) {
        super(context);

        mAnchor = new Anchor();
        mDefaultAnchorPosition = getResources().getDimensionPixelOffset(R.dimen.default_anchor_position);
        mAnchorWidth = getResources().getDimensionPixelOffset(R.dimen.anchor_width);
        mAnchorRound = getResources().getDimensionPixelOffset(R.dimen.anchor_round);
        mAnchorArea = getResources().getDimensionPixelOffset(R.dimen.anchor_area);

        mDisablePaint = new Paint(Color.parseColor("#000000"));
        mDisablePaint.setAlpha(128);

    }

    public AnchorVideoTrackView(Context context, AttributeSet attrs) {
        super(context, attrs);

        mAnchor = new Anchor();
        mDefaultAnchorPosition = getResources().getDimensionPixelOffset(R.dimen.default_anchor_position);
        mAnchorWidth = getResources().getDimensionPixelOffset(R.dimen.anchor_width);
        mAnchorRound = getResources().getDimensionPixelOffset(R.dimen.anchor_round);
        mAnchorArea = getResources().getDimensionPixelOffset(R.dimen.anchor_area);

        mDisablePaint = new Paint(Color.parseColor("#000000"));
        mDisablePaint.setAlpha(192);
    }

    public AnchorVideoTrackView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);

        mAnchor = new Anchor();
        mDefaultAnchorPosition = getResources().getDimensionPixelOffset(R.dimen.default_anchor_position);
        mAnchorWidth = getResources().getDimensionPixelOffset(R.dimen.anchor_width);
        mAnchorRound = getResources().getDimensionPixelOffset(R.dimen.anchor_round);
        mAnchorArea = getResources().getDimensionPixelOffset(R.dimen.anchor_area);

        mDisablePaint = new Paint(Color.parseColor("#000000"));
        mDisablePaint.setAlpha(192);
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
        super.surfaceChanged(holder, format, width, height);

        mDisableRect = new Rect((int) mAnchor.position, 0, mWidth, mHeight);
    }

    @Override
    public boolean setVideo(String path) {
        super.setVideo(path);

        currentPosition = 0;
        currentDuration = (int) (mDefaultAnchorPosition / millisecondsPerWidth);

        mAnchor.position = mDefaultAnchorPosition;
        mDisableRect.left = (int) mDefaultAnchorPosition;

        invalidate();
        return true;
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {

        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                if(mOnUpdateAnchorListener != null) {
                    mOnUpdateAnchorListener.onUpdatePositionStart();
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
                if(mOnUpdateAnchorListener != null) {
                    mOnUpdateAnchorListener.onUpdatePositionEnd(currentPosition, currentDuration);
                }
                // action type to idle
                actionType = ACTION_TYPE.idle;
        }

        return true;
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

        currentPosition = (int) -(mTrack.left / millisecondsPerWidth);
        if(x < 0) {
            int nextDuration = mVideoDuration - currentPosition;
            currentDuration = nextDuration > currentDuration ? currentDuration : nextDuration;
            mAnchor.position = (int) (currentDuration * millisecondsPerWidth);
            mDisableRect.left = (int) mAnchor.position;
        }

        if(mOnUpdateAnchorListener != null) {
            mOnUpdateAnchorListener.onUpdatePosition(currentPosition, currentDuration);
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
        }

        mAnchor.position += x;
        mDisableRect.left = (int) mAnchor.position;

        currentDuration = (int) (mAnchor.position / millisecondsPerWidth);
        if(mOnUpdateAnchorListener != null) {
            mOnUpdateAnchorListener.onUpdatePosition(currentPosition, currentDuration);
        }

        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        canvas.drawRect(mDisableRect, mDisablePaint);
        mAnchor.draw(canvas);
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
            if(x > (position - mAnchorArea) && x < (position + mAnchorWidth + mAnchorArea)) {
                return true;
            } else {
                return false;
            }
        }

        public void draw(Canvas canvas) {
            canvas.drawRoundRect(new RectF(position, 0, position + mAnchorWidth, mHeight), mAnchorRound, mAnchorRound, mAnchorPaint);
        }
    }

    public void setOnUpdateAnchorListener(OnUpdateAnchorListener onUpdateAnchorListener) {
        mOnUpdateAnchorListener = onUpdateAnchorListener;
    }

    // video time position change listener
    public interface OnUpdateAnchorListener {
        void onUpdatePositionStart();
        void onUpdatePosition(int seek, int duration);
        void onUpdatePositionEnd(int seek, int duration);
    }
}
