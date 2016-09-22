/*
 * Android-VideoTrackView
 * https://github.com/crust87/Android-VideoTrackView
 *
 * Mabi
 * crust87@gmail.com
 * last modify 2015-12-15
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
import android.graphics.Canvas;
import android.view.MotionEvent;

public abstract class VideoTrackOverlay {

    // Application Context
    protected Context mContext;

    // Attributes
    protected int mWidth;
    protected int mHeight;
    protected float mMillisecondsPerWidth;

    public VideoTrackOverlay(Context context) {
        mContext = context;
    }

    public void onSurfaceChanged(int width, int height) {
        mWidth = width;
        mHeight = height;
    }

    public void onSetVideo(float millisecondsPerWidth) {
        mMillisecondsPerWidth = millisecondsPerWidth;
    }

    public abstract boolean onTrackTouchEvent(VideoTrackView.Track track, MotionEvent event);

    public void drawOverlay(Canvas canvas) {
    }
}
