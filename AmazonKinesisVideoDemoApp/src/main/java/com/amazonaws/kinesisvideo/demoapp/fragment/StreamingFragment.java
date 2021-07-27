package com.amazonaws.kinesisvideo.demoapp.fragment;

import android.content.Context;
import android.graphics.Matrix;
import android.graphics.RectF;
import android.graphics.SurfaceTexture;
import android.os.Bundle;
import androidx.fragment.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Surface;
import android.view.TextureView;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.Toast;

import com.amazonaws.kinesisvideo.common.exception.KinesisVideoException;
import com.amazonaws.kinesisvideo.demoapp.KinesisVideoDemoApp;
import com.amazonaws.kinesisvideo.demoapp.R;
import com.amazonaws.kinesisvideo.demoapp.activity.SimpleNavActivity;
import com.amazonaws.kinesisvideo.client.KinesisVideoClient;
import com.amazonaws.mobileconnectors.kinesisvideo.client.KinesisVideoAndroidClientFactory;
import com.amazonaws.mobileconnectors.kinesisvideo.encoding.EncoderFrameSubmitter;
import com.amazonaws.mobileconnectors.kinesisvideo.mediasource.android.AndroidCameraMediaSource;
import com.amazonaws.mobileconnectors.kinesisvideo.mediasource.android.AndroidCameraMediaSourceConfiguration;

public class StreamingFragment extends Fragment implements TextureView.SurfaceTextureListener {
    public static final String KEY_MEDIA_SOURCE_CONFIGURATION = "mediaSourceConfiguration";
    public static final String KEY_STREAM_NAME = "streamName";
    public static final String KEY_IS_NEW_BEHAVIOR = "isNewBehavior";
    public static final String KEY_ROTATE_DISPLAY = "rotateDisplay";

    private static final String TAG = StreamingFragment.class.getSimpleName();

    private Button mStartStreamingButton;
    private Button takePhotoButton;
    private KinesisVideoClient mKinesisVideoClient;
    private String mStreamName;
    private AndroidCameraMediaSourceConfiguration mConfiguration;
    private AndroidCameraMediaSource mCameraMediaSource;
    private TextureView mTextureView;
    private boolean isNewBehavior;
    private boolean rotateDisplay;
    private Button mRotateRightButton;

    private SimpleNavActivity navActivity;

    public static StreamingFragment newInstance(SimpleNavActivity navActivity) {
        StreamingFragment s = new StreamingFragment();
        s.navActivity = navActivity;
        return s;
    }

    @Override
    public View onCreateView(final LayoutInflater inflater,
                             final ViewGroup container,
                             final Bundle savedInstanceState) {
        getArguments().setClassLoader(AndroidCameraMediaSourceConfiguration.class.getClassLoader());
        mStreamName = getArguments().getString(KEY_STREAM_NAME);
        mConfiguration = getArguments().getParcelable(KEY_MEDIA_SOURCE_CONFIGURATION);
        isNewBehavior = getArguments().getBoolean(KEY_IS_NEW_BEHAVIOR);
        rotateDisplay = getArguments().getBoolean(KEY_ROTATE_DISPLAY);

        final View view = inflater.inflate(R.layout.fragment_streaming, container, false);
        mTextureView = (TextureView) view.findViewById(R.id.texture);
        mTextureView.setSurfaceTextureListener(this);
        if (rotateDisplay) {
            mTextureView.setRotation(-90);
        }
        return view;
    }

    private void createClientAndStartStreaming(final SurfaceTexture previewTexture) {

        try {
            mKinesisVideoClient = KinesisVideoAndroidClientFactory.createKinesisVideoClient(
                    getActivity(),
                    KinesisVideoDemoApp.KINESIS_VIDEO_REGION,
                    KinesisVideoDemoApp.getCredentialsProvider());

            mCameraMediaSource = (AndroidCameraMediaSource) mKinesisVideoClient
                    .createMediaSource(mStreamName, mConfiguration);

            mCameraMediaSource.setPreviewSurfaces(new Surface(previewTexture));

            resumeStreaming();
        } catch (final KinesisVideoException e) {
            Log.e(TAG, "unable to start streaming");
            throw new RuntimeException("unable to start streaming", e);
        }
    }

    @Override
    public void onViewCreated(final View view, Bundle savedInstanceState) {
        mStartStreamingButton = (Button) view.findViewById(R.id.start_streaming);
        mStartStreamingButton.setOnClickListener(stopStreamingWhenClicked());

        takePhotoButton = (Button) view.findViewById(R.id.take_photo);
        takePhotoButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                EncoderFrameSubmitter.doneWriting = false;
                Toast.makeText(getContext(), "Photo taken!", Toast.LENGTH_LONG).show();
            }
        });

        mRotateRightButton = (Button) view.findViewById(R.id.rotate_right);
        mRotateRightButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                float newRotation = (mTextureView.getRotation() + 90) % 360;
                mTextureView.setRotation(newRotation);
                Toast.makeText(getContext(), "The rotation is now " + newRotation, Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public void onResume() {
        super.onResume();
        resumeStreaming();
    }

    @Override
    public void onPause() {
        super.onPause();
        pauseStreaming();
    }

    private View.OnClickListener stopStreamingWhenClicked() {
        return new View.OnClickListener() {
            @Override
            public void onClick(final View view) {
                pauseStreaming();
                navActivity.startConfigFragment();
            }
        };
    }

    private void resumeStreaming() {
        try {
            if (mCameraMediaSource == null) {
                return;
            }

            mCameraMediaSource.start();
            Toast.makeText(getActivity(), "resumed streaming", Toast.LENGTH_SHORT).show();
            mStartStreamingButton.setText(getActivity().getText(R.string.stop_streaming));
        } catch (final KinesisVideoException e) {
            Log.e(TAG, "unable to resume streaming", e);
            Toast.makeText(getActivity(), "failed to resume streaming", Toast.LENGTH_SHORT).show();
        }
    }

    private void pauseStreaming() {
        try {
            if (mCameraMediaSource == null) {
                return;
            }

            mCameraMediaSource.stop();
            Toast.makeText(getActivity(), "stopped streaming", Toast.LENGTH_SHORT).show();
            mStartStreamingButton.setText(getActivity().getText(R.string.start_streaming));
        } catch (final KinesisVideoException e) {
            Log.e(TAG, "unable to pause streaming", e);
            Toast.makeText(getActivity(), "failed to pause streaming", Toast.LENGTH_SHORT).show();
        }
    }

    ////
    // TextureView.SurfaceTextureListener methods
    ////

    private int mWidth;
    private int mHeight;

    private void updateWidthAndHeight(int width, int height) {
        mWidth = width;
        mHeight = height;
        if (isNewBehavior) {
            updateTransformMatrix();
        }
    }

    private void updateTransformMatrix() {
        if (mWidth != 0 && mHeight != 0 && mTextureView != null) {
            Log.d(TAG, "SETTING UP THE MATRIX!");
            Matrix matrix = new Matrix();
            int rotation = ((WindowManager) getActivity().getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay().getRotation();
            Log.d(TAG, "ROTATION IS: " + rotation);

            RectF textureRectF = new RectF(0, 0, mTextureView.getWidth(), mTextureView.getHeight());
            RectF previewRectF = new RectF(0, 0, mHeight, mWidth);
            float centerX = textureRectF.centerX();
            float centerY = textureRectF.centerY();
            previewRectF.offset(centerX - previewRectF.centerX(),
                    centerY - previewRectF.centerY());
            float scale = Math.max((float) mWidth / mTextureView.getWidth(), (float) mHeight / mTextureView.getHeight());
            matrix.setRectToRect(textureRectF, previewRectF, Matrix.ScaleToFit.FILL);
            matrix.postScale(scale, scale, centerX, centerY);
            Log.d(TAG, "scale: " + scale);

            matrix.postRotate(-90, centerX, centerY);
            mTextureView.setTransform(matrix);

            Log.d(TAG, "textureRectF: " + textureRectF.toShortString());
            Log.d(TAG, "previewRectF: " + previewRectF.toShortString());
        }
    }

    @Override
    public void onSurfaceTextureAvailable(SurfaceTexture surfaceTexture, int i, int i1) {
        surfaceTexture.setDefaultBufferSize(1280, 720);
        Log.d(TAG, "SurfaceTexture Width " + i);
        Log.d(TAG, "SurfaceTexture Height " + i1);
        updateWidthAndHeight(i, i1);
        createClientAndStartStreaming(surfaceTexture);
    }

    @Override
    public void onSurfaceTextureSizeChanged(SurfaceTexture surfaceTexture, int i, int i1) {
        updateWidthAndHeight(i, i1);
    }

    @Override
    public boolean onSurfaceTextureDestroyed(SurfaceTexture surfaceTexture) {
        try {
            if (mCameraMediaSource != null)
                mCameraMediaSource.stop();
            if (mKinesisVideoClient != null)
                mKinesisVideoClient.stopAllMediaSources();
            KinesisVideoAndroidClientFactory.freeKinesisVideoClient();
        } catch (final KinesisVideoException e) {
            Log.e(TAG, "failed to release kinesis video client", e);
        }
        return true;
    }

    @Override
    public void onSurfaceTextureUpdated(SurfaceTexture surfaceTexture) {

    }
}
