package com.amazonaws.kinesisvideo.demoapp.fragment;

import android.graphics.Matrix;
import android.graphics.RectF;
import android.graphics.SurfaceTexture;
import android.os.Bundle;
import androidx.fragment.app.Fragment;
import android.util.Log;
import android.util.Size;
import android.view.LayoutInflater;
import android.view.Surface;
import android.view.TextureView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.Toast;

import com.amazonaws.kinesisvideo.common.exception.KinesisVideoException;
import com.amazonaws.kinesisvideo.demoapp.KinesisVideoDemoApp;
import com.amazonaws.kinesisvideo.demoapp.R;
import com.amazonaws.kinesisvideo.demoapp.activity.SimpleNavActivity;
import com.amazonaws.kinesisvideo.client.KinesisVideoClient;
import com.amazonaws.mobileconnectors.kinesisvideo.client.KinesisVideoAndroidClientFactory;
import com.amazonaws.mobileconnectors.kinesisvideo.mediasource.android.AndroidCameraMediaSource;
import com.amazonaws.mobileconnectors.kinesisvideo.mediasource.android.AndroidCameraMediaSourceConfiguration;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import static com.amazonaws.mobileconnectors.kinesisvideo.util.CameraUtils.getSupportedResolutions;

public class StreamingFragment extends Fragment implements TextureView.SurfaceTextureListener {
    public static final String KEY_MEDIA_SOURCE_CONFIGURATION = "mediaSourceConfiguration";
    public static final String KEY_STREAM_NAME = "streamName";
    public static final String KEY_ROTATION = "rotation";

    private static final String TAG = StreamingFragment.class.getSimpleName();

    private Button mStartStreamingButton;
    private KinesisVideoClient mKinesisVideoClient;
    private String mStreamName;
    private AndroidCameraMediaSourceConfiguration mConfiguration;
    private AndroidCameraMediaSource mCameraMediaSource;
    private TextureView mTextureView;
    private int mWidth;
    private int mHeight;
    private float mRotation;
    private Size mPreviewSize;

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
        mRotation = getArguments().getFloat(KEY_ROTATION);

        final View view = inflater.inflate(R.layout.fragment_streaming, container, false);
        mTextureView = (TextureView) view.findViewById(R.id.texture);
        mTextureView.setSurfaceTextureListener(this);
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

    private void updateSurfaceTextureDims(int width, int height) {
        mPreviewSize = getPreferredPreviewSize(getSupportedResolutions(getActivity(), mConfiguration.getCameraId()), width, height);

        Log.d(TAG, "rotation is " + mRotation);

        if (mPreviewSize == null || mTextureView == null) {
            return;
        }

        final Matrix transformMatrix = new Matrix();
        final RectF textureViewRect = new RectF(0, 0, width, height);
        final RectF outputPreviewRect = new RectF(0, 0, mPreviewSize.getHeight(), mPreviewSize.getWidth());

        final float centerX = textureViewRect.centerX();
        final float centerY = textureViewRect.centerY();
        textureViewRect.offset(centerX - outputPreviewRect.centerX(), centerY - outputPreviewRect.centerY());
        transformMatrix.setRectToRect(textureViewRect, outputPreviewRect, Matrix.ScaleToFit.FILL);
        final float scale = Math.min((float) width / mPreviewSize.getWidth(), (float) height / mPreviewSize.getHeight());
        transformMatrix.postScale(scale, scale, centerX, centerY);
        transformMatrix.postRotate(mRotation - 90f);
        mTextureView.setTransform(transformMatrix);
    }

    private Size getPreferredPreviewSize(List<Size> sizes, int width, int height) {
        List<Size> possibleSizes = new ArrayList<>();
        for (Size size : sizes) {
            if (width > height) {
                if (size.getWidth() > width && size.getHeight() > height) {
                    possibleSizes.add(size);
                }
            } else {
                if (size.getWidth() > height && size.getWidth() > width) {
                    possibleSizes.add(size);
                }
            }
        }
        if (possibleSizes.size() > 0) {
            return Collections.min(possibleSizes, new Comparator<Size>() {
                @Override
                public int compare(Size s1, Size s2) {
                    return Long.signum(s1.getWidth() * s1.getHeight() - s2.getWidth() * s2.getHeight());
                }
            });
        }
        return sizes.get(0);
    }

    ////
    // TextureView.SurfaceTextureListener methods
    ////

    @Override
    public void onSurfaceTextureAvailable(SurfaceTexture surfaceTexture, int i, int i1) {
        surfaceTexture.setDefaultBufferSize(1280, 720);
        updateSurfaceTextureDims(i1, i);
        createClientAndStartStreaming(surfaceTexture);
    }

    @Override
    public void onSurfaceTextureSizeChanged(SurfaceTexture surfaceTexture, int i, int i1) {
        updateSurfaceTextureDims(i, i1);
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
