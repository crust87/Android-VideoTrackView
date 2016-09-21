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

package com.crust87.videotrackviewsample;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.provider.MediaStore;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import com.crust87.videotrackview.VideoTrackView;

public class MainActivity extends AppCompatActivity {

    /*
    Constants
     */
    private static final int ACTIVITY_REQUEST_VIDEO = 1000;
    private static final int PERMISSIONS_REQUEST_READ_EXTERNAL_STORAGE = 0;

    /*
    Activity
     */
    private Activity mActivity;

    /*
    Layout Components
     */
    private VideoTrackView mAnchorVideoTrackView;
    private EditText mEditScreenDuration;
    private EditText mEditThumbnailPerScreen;
    private EditText mEditTrackPadding;

    /*
    Attributes
     */
    private String originalPath;

    /*
    Lifecycel Method START
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mActivity = MainActivity.this;

        loadGUI();
        init();
    }
    /*
    Lifecycel Method END
     */

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == ACTIVITY_REQUEST_VIDEO && resultCode == RESULT_OK) {
            Uri selectedVideoUri = data.getData();

            originalPath = getRealPathFromURI(selectedVideoUri);
            mAnchorVideoTrackView.setVideo(originalPath);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        switch (requestCode) {
            case PERMISSIONS_REQUEST_READ_EXTERNAL_STORAGE: {
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    Intent lIntent = new Intent(Intent.ACTION_PICK);
                    lIntent.setType("video/*");
                    lIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                    startActivityForResult(lIntent, ACTIVITY_REQUEST_VIDEO);
                } else {
                    Toast.makeText(mActivity, "APPLICATION NEEDS PERMISSION!", Toast.LENGTH_LONG).show();
                }
            }
        }
    }

    private void loadGUI() {
        setContentView(R.layout.activity_main);

        mAnchorVideoTrackView = (VideoTrackView) findViewById(R.id.anchorVideoTrackView);
        mEditScreenDuration = (EditText) findViewById(R.id.editScreenDuration);
        mEditThumbnailPerScreen = (EditText) findViewById(R.id.editThumbnailPerScreen);
        mEditTrackPadding = (EditText) findViewById(R.id.editTrackPadding);

//        mAnchorVideoTrackView.setVideoTrackOverlay(new AnchorOverlay(getApplicationContext()));
    }

    private void init() {
        mEditScreenDuration.setText(String.valueOf(mAnchorVideoTrackView.getScreenDuration()));
        mEditThumbnailPerScreen.setText(String.valueOf(mAnchorVideoTrackView.getThumbnailPerScreen()));
        mEditTrackPadding.setText(String.valueOf(mAnchorVideoTrackView.getTrackPadding()));
    }

    public void onButtonLoadClick(View v) {
        if (ContextCompat.checkSelfPermission(mActivity, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(mActivity, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, PERMISSIONS_REQUEST_READ_EXTERNAL_STORAGE);
        } else {
            Intent lIntent = new Intent(Intent.ACTION_PICK);
            lIntent.setType("video/*");
            lIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            startActivityForResult(lIntent, ACTIVITY_REQUEST_VIDEO);
        }
    }

    private String getRealPathFromURI(Uri contentUri) {
        Cursor cursor = null;

        try {
            String[] proj = { MediaStore.Images.Media.DATA };
            cursor = mActivity.getContentResolver().query(contentUri, proj, null, null, null);

            if (cursor != null) {
                int columnIndex = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
                cursor.moveToFirst();
                return cursor.getString(columnIndex);
            } else {
                return null;
            }
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
    }

    public void onScreenDurationSetClicked(View view) {
        float newScreenDuration = Float.valueOf(mEditScreenDuration.getText().toString());

        if(!mAnchorVideoTrackView.isLoading()) {
            mAnchorVideoTrackView.cancelLoadTask();
        }

        mAnchorVideoTrackView.setScreenDuration(newScreenDuration);
    }

    public void onThumbnailPerScreenSetClicked(View view) {
        int newThumbnailPerScreen = Integer.valueOf(mEditThumbnailPerScreen.getText().toString());

        if(!mAnchorVideoTrackView.isLoading()) {
            mAnchorVideoTrackView.cancelLoadTask();
        }

        mAnchorVideoTrackView.setThumbnailPerScreen(newThumbnailPerScreen);
    }

    public void onTrackPaddingSetClicked(View view) {
        int newTrackPadding = Integer.valueOf(mEditTrackPadding.getText().toString());

        if(!mAnchorVideoTrackView.isLoading()) {
            mAnchorVideoTrackView.cancelLoadTask();
        }

        mAnchorVideoTrackView.setTrackPadding(newTrackPadding);
    }
}
