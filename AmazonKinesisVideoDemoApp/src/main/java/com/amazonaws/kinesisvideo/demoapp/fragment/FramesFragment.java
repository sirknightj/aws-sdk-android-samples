package com.amazonaws.kinesisvideo.demoapp.fragment;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.SurfaceTexture;
import android.os.Bundle;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.Fragment;
import androidx.core.content.ContextCompat;
import android.util.Log;
import android.util.Size;
import android.view.LayoutInflater;
import android.view.Surface;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.amazonaws.kinesisvideo.client.KinesisVideoClient;
import com.amazonaws.kinesisvideo.common.exception.KinesisVideoException;
import com.amazonaws.kinesisvideo.demoapp.KinesisVideoDemoApp;
import com.amazonaws.kinesisvideo.demoapp.R;
import com.amazonaws.kinesisvideo.demoapp.activity.SimpleNavActivity;
import com.amazonaws.kinesisvideo.internal.client.mediasource.MediaSource;
import com.amazonaws.mobileconnectors.kinesisvideo.client.KinesisVideoAndroidClientFactory;
import com.amazonaws.mobileconnectors.kinesisvideo.mediasource.file.ImageFileMediaSource;
import com.amazonaws.mobileconnectors.kinesisvideo.mediasource.file.ImageFileMediaSourceConfiguration;

public class FramesFragment extends Fragment {
    private static final String TAG = StreamConfigurationFragment.class.getSimpleName();

    private Button mStartStreamingButton;
    private EditText mStreamName;
    private KinesisVideoClient mKinesisVideoClient;
    private MediaSource mMediaSource;
    private static boolean streamingNow;

    private SimpleNavActivity navActivity;

    public static FramesFragment newInstance(SimpleNavActivity navActivity) {
        FramesFragment s = new FramesFragment();
        s.navActivity = navActivity;
        return s;
    }

    @Override
    public View onCreateView(final LayoutInflater inflater,
                             final ViewGroup container,
                             final Bundle savedInstanceState) {

        getActivity().setTitle(getActivity().getString(R.string.title_fragment_stream));

        final View view = inflater.inflate(R.layout.fragment_frames, container, false);

        return view;
    }

    @Override
    public void onViewCreated(final View view, Bundle savedInstanceState) {
        mStartStreamingButton = (Button) view.findViewById(R.id.start_streaming_frames);
        mStreamName = (EditText) view.findViewById(R.id.stream_name);
        streamingNow = false;

        mStartStreamingButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (streamingNow) {
                    stopStreaming();
                } else {
                    createClientAndStartStreaming();
                }
                streamingNow = !streamingNow;
            }
        });
    }

    private void createClientAndStartStreaming() {
        try {
            mKinesisVideoClient = KinesisVideoAndroidClientFactory.createKinesisVideoClient(
                    getContext(),
                    KinesisVideoDemoApp.KINESIS_VIDEO_REGION,
                    KinesisVideoDemoApp.getCredentialsProvider());

            mMediaSource = createImageFileMediaSource(getContext(), mStreamName.getText().toString());

            mKinesisVideoClient.registerMediaSource(mMediaSource);

            mMediaSource.start();
            Toast.makeText(getActivity(), "Started Streaming", Toast.LENGTH_SHORT).show();
        } catch (final KinesisVideoException e) {
            Log.e(TAG, "unable to start streaming");
            throw new RuntimeException("unable to start streaming", e);
        }
        mStartStreamingButton.setText("Stop Streaming");
    }

    public void stopStreaming() {
        try {
            if (mMediaSource != null)
                mMediaSource.stop();
            if (mKinesisVideoClient != null)
                mKinesisVideoClient.stopAllMediaSources();
            KinesisVideoAndroidClientFactory.freeKinesisVideoClient();
        } catch (final KinesisVideoException e) {
            Log.e(TAG, "failed to release kinesis video client", e);
        }
        Toast.makeText(getActivity(), "Stopped Streaming", Toast.LENGTH_SHORT).show();
        mStartStreamingButton.setText("Start Streaming");
    }

    @Override
    public void onPause() {
        stopStreaming();
        super.onPause();
    }

    private static final int FPS_25 = 25;
    private static final String IMAGE_FILENAME_FORMAT = "frame-%03d.h264";
    private static final int START_FILE_INDEX = 1;
    private static final int END_FILE_INDEX = 375;
    private static final String FRAME_DIR = "sample_frames"; // relative file path from assets folder

    private static MediaSource createImageFileMediaSource(Context context, String streamName) {
        final ImageFileMediaSourceConfiguration configuration =
                new ImageFileMediaSourceConfiguration.Builder()
                        .fps(FPS_25)
                        .dir(FRAME_DIR)
                        .assetManager(context.getAssets())
                        .filenameFormat(IMAGE_FILENAME_FORMAT)
                        .startFileIndex(START_FILE_INDEX)
                        .endFileIndex(END_FILE_INDEX)
                        .build();
        final ImageFileMediaSource mediaSource = new ImageFileMediaSource(streamName);
        mediaSource.configure(configuration);

        return mediaSource;
    }
}