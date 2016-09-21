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
import android.view.MotionEvent;

public class DefaultOverlay extends VideoTrackOverlay {

    // Working Variable;
    protected float pastX;					// past position x of touch event

    public DefaultOverlay(Context context) {
        super(context);
    }

    @Override
    public boolean onTrackTouchEvent(VideoTrackView.Track track, MotionEvent event) {
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                pastX = event.getX();
            case MotionEvent.ACTION_MOVE:
                updateTrackPosition(track, (int) (event.getX() - pastX));
                pastX = event.getX();
                break;
            case MotionEvent.ACTION_UP:
                break;
        }

        return true;
    }

    // update track position
    // int x: it's actually delta x
    private void updateTrackPosition(VideoTrackView.Track track, float x) {
        // check next position in boundary
        if(track.left + x > 0) {
            x = -track.left;
        }

        if(track.right + x < 0) {
            x = 0 - track.right;
        }

        track.left += x;
        track.right += x;
    }
}
