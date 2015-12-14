package com.crust87.videotrackviewsample;

import android.content.Intent;
import android.database.Cursor;
import android.media.MediaPlayer;
import android.net.Uri;
import android.provider.MediaStore;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;

import com.crust87.videocropview.VideoCropView;
import com.crust87.videotrackview.VideoTrackView;

public class MainActivity extends AppCompatActivity {

    // Layout Components
    private VideoCropView mVideoCropView;
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
        mVideoTrackView = (VideoTrackView) findViewById(R.id.videoTrackView);
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

            }

            @Override
            public void onUpdatePosition(int seek) {

            }

            @Override
            public void onUpdatePositionEnd(int seek) {

            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == 1000 && resultCode == RESULT_OK) {
            Uri selectedVideoUri = data.getData();

            originalPath = getRealPathFromURI(selectedVideoUri);
            mVideoTrackView.setVideo(originalPath);

            mVideoCropView.setVideoURI(selectedVideoUri);
            mVideoCropView.seekTo(1);
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
