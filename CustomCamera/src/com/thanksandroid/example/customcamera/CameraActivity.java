package com.thanksandroid.example.customcamera;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.hardware.Camera;
import android.hardware.Camera.CameraInfo;
import android.hardware.Camera.PictureCallback;
import android.hardware.Camera.ShutterCallback;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.Toast;

public class CameraActivity extends Activity implements OnClickListener {

	private final String TAG = "Camera Activity";
	private int mCurrentCamera;

	private Camera mCamera;
	private CameraPreview mPreview;
	private FrameLayout mPreviewContainer;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_camera);

		((ImageButton) findViewById(R.id.button_capture))
				.setOnClickListener(this);
		((ImageButton) findViewById(R.id.button_switch_camera))
				.setOnClickListener(this);

		mPreviewContainer = (FrameLayout) findViewById(R.id.camera_preview);

		if (!checkCameraHardware(this)) {
			showToast("This device has no camera");
			return;
		}

		mCurrentCamera = CameraInfo.CAMERA_FACING_BACK;

		// Create an instance of Camera
		mCamera = getCameraInstance(mCurrentCamera);

		if (mCamera == null)
			return;

		initPreview();

	}

	private void switchCamera() {

		if (mCamera != null) {
			mCamera.stopPreview(); // stop preview
			mCamera.release(); // release previous camera
		}

		if (mCurrentCamera == CameraInfo.CAMERA_FACING_BACK) {
			mCurrentCamera = CameraInfo.CAMERA_FACING_FRONT;
		} else {
			mCurrentCamera = CameraInfo.CAMERA_FACING_BACK;
		}

		// Create an instance of Camera
		mCamera = getCameraInstance(mCurrentCamera);

		if (mCamera == null)
			return;

		initPreview();
		// mCamera.startPreview();
	}

	private void initPreview() {
		// Create Preview view and set it as the content of our activity.
		mPreview = new CameraPreview(this, mCamera);
		mPreviewContainer.removeAllViews();
		mPreviewContainer.addView(mPreview);
	}

	private void showToast(String text) {
		Toast.makeText(this, text, Toast.LENGTH_LONG).show();
	}

	/** Check if device has camera */
	private boolean checkCameraHardware(Context context) {
		if (context.getPackageManager().hasSystemFeature(
				PackageManager.FEATURE_CAMERA)) {
			// this device has a camera
			return true;
		} else {
			// no camera on this device
			return false;
		}
	}

	private Camera getCameraInstance(int type) {
		Camera c = null;
		int numberOfCameras = Camera.getNumberOfCameras();
		for (int i = 0; i < numberOfCameras; i++) {
			CameraInfo info = new CameraInfo();
			Camera.getCameraInfo(i, info);
			if (info.facing == type) {
				try {
					c = Camera.open(i); // attempt to get a Camera instance
				} catch (Exception e) {
					// Camera is not available
					showToast("Camera not available.");
				}
				break;
			}
		}
		return c;
	}

	private PictureCallback mPicture = new PictureCallback() {

		@Override
		public void onPictureTaken(byte[] data, Camera camera) {

			mPreview.initPreview();

			File pictureFile = Utilities.getOutputMediaFile();
			if (pictureFile == null) {
				Log.d(TAG,
						"Error creating media file, check storage permissions.");
				showToast("Error creating media file.");
				return;
			}

			try {
				FileOutputStream fos = new FileOutputStream(pictureFile);
				fos.write(data);
				fos.close();

				Intent mediaScanIntent = new Intent(
						Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
				Uri contentUri = Uri.fromFile(pictureFile);
				mediaScanIntent.setData(contentUri);
				CameraActivity.this.sendBroadcast(mediaScanIntent);

			} catch (FileNotFoundException e) {
				Log.d(TAG, "File not found: " + e.getMessage());
			} catch (IOException e) {
				Log.d(TAG, "Error accessing file: " + e.getMessage());
			}
		}
	};

	ShutterCallback mShutterCallback = new ShutterCallback() {
		public void onShutter() {
			// do stuff like playing shutter sound here
		}
	};

	private void captureImage() {
		mCamera.takePicture(mShutterCallback, null, mPicture);
	}

	@Override
	protected void onPause() {
		super.onPause();
		if (mCamera != null) {
			mCamera.release(); // release the camera for other applications
			mCamera = null;
		}
	}

	@Override
	public void onClick(View v) {
		int id = v.getId();
		if (id == R.id.button_switch_camera) {
			switchCamera();
		} else if (id == R.id.button_capture) {
			captureImage();
		}

	}

}
