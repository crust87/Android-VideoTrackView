package com.crust87.videotrackviewsample;

import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.media.MediaPlayer;
import android.net.Uri;
import android.provider.MediaStore;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.FrameLayout;

import com.crust87.videotrackview.VideoTrackView;
import com.mabi87.videocropview.VideoCropView;

public class MainActivity extends AppCompatActivity {

    // Layout Components
    private VideoCropView mVideoCropView;
    private FrameLayout mLayoutTrackView;
    private VideoTrackView mVideoTrackView;

    // Attributes
    private String originalPath;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        loadGUI();
        bindEvent();
    }

    private void loadGUI() {
        setContentView(R.layout.activity_main);

        mVideoCropView = (VideoCropView) findViewById(R.id.videoCropView);
        mLayoutTrackView = (FrameLayout) findViewById(R.id.layoutTrackView);
        mVideoTrackView = new VideoTrackView(getApplication());
        mLayoutTrackView.addView(mVideoTrackView);
    }

    private void bindEvent() {
        mVideoCropView.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {

            @Override
            public void onPrepared(MediaPlayer mp) {
                mVideoCropView.start();
            }
        });

        mVideoTrackView.setOnUpdatePositionListener(new VideoTrackView.OnUpdatePositionListener() {
            @Override
            public void onUpdatePositionStart() {
                mVideoCropView.pause();
            }

            @Override
            public void onUpdatePosition(int seek, int duration) {
            }

            @Override
            public void onUpdatePositionEnd(int seek, int duration) {
                mVideoCropView.seekTo(seek);
                mVideoCropView.start();
            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == 1000 && resultCode == RESULT_OK) {
            Uri selectedVideoUri = data.getData();

            originalPath = getRealPathFromURI(selectedVideoUri);

            mVideoCropView.setVideoURI(selectedVideoUri);
            mVideoCropView.seekTo(1);

            mVideoTrackView.setVideo(originalPath);
            mVideoTrackView.invalidate();
        }
    }

    public void onButtonLoadClick(View v) {
        Intent lIntent = new Intent(Intent.ACTION_PICK);
        lIntent.setType("video/*");
        lIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        startActivityForResult(lIntent, 1000);
    }

    public String getRealPathFromURI(Uri contentUri) {
        Cursor cursor = null;
        try {
            String[] proj = { MediaStore.Images.Media.DATA };
            cursor = getApplicationContext().getContentResolver().query(contentUri, proj, null, null, null);
            int column_index = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
            cursor.moveToFirst();
            return cursor.getString(column_index);
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
    }
}
