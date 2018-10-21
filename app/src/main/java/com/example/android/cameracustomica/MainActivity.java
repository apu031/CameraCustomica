package com.example.android.cameracustomica;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.SurfaceTexture;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.os.Build;
import android.os.Handler;
import android.os.HandlerThread;
import android.support.annotation.NonNull;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Size;
import android.util.SparseIntArray;
import android.view.Surface;
import android.view.TextureView;
import android.view.View;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private static final int REQUEST_CAMERA_PERMISSION_RESULT = 0;
    private TextureView mTextureView;
    // To make TextureView available setup a surface listener
    private TextureView.SurfaceTextureListener mSurfaceTextureListener =
            new TextureView.SurfaceTextureListener() {
                @Override
                public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
                    //Toast.makeText(getApplicationContext(), "TextureView is available", Toast.LENGTH_SHORT).show();
                    // Width 1440 and Height = 2112
                    // When TextureView is available setup the camera with respective width and height
                    setupCamera(width, height);

                    // Connect to Camera
                    connectCamera();
                }

                @Override
                public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {

                }

                @Override
                public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
                    return false;
                }

                @Override
                public void onSurfaceTextureUpdated(SurfaceTexture surface) {

                }
            };
    // Creating a background thread to remove any time consuming (i.e. loading) task from UI thread
    // to not affect the UI behavior
    // for this we need two variables: one for the thread itself and one for the handler of the thread
    private HandlerThread mBackgroundHandlerThread;
    private Handler mBackgroundHandler;

    // Creating a CameraDevice member variable
    private CameraDevice mCameraDevice;

    private static class CompareSizeByArea implements Comparator<Size> {

        @Override
        public int compare(Size lhs, Size rhs) {
            return Long.signum((long) lhs.getWidth() * lhs.getHeight() /
                    (long) rhs.getWidth() * rhs.getHeight());
        }
    }

    // A listener for the CameraDevice
    private CameraDevice.StateCallback mCameraDeviceStateCallback =
            new CameraDevice.StateCallback() {
                /**
                 * Method called when a camera device has finished opening
                 * and is ready to use
                 * @param camera is a CameraDevice
                 */
                @Override
                public void onOpened(CameraDevice camera) {
                    // Assigning the camera to our CameraDevice object
                    mCameraDevice = camera;
                    Toast.makeText(getApplicationContext(), "Camera Connection Successful!", Toast.LENGTH_SHORT).show();
                }

                /**
                 * Method is called when a camera device is no longer available to use
                 * @param camera is a CameraDevice
                 */
                @Override
                public void onDisconnected(CameraDevice camera) {
                    // Cleaning up CameraDevice resources to free memory
                    camera.close();
                    mCameraDevice = null;
                }

                /**
                 * Method is called when a camera device has encountered a serious error
                 * @param camera is a CameraDevice
                 * @param error the integer value of the type of error
                 */
                @Override
                public void onError(CameraDevice camera, int error) {
                    // Freeing up CameraDevice resources to free memory
                    camera.close();
                    mCameraDevice = null;
                }
            };

    // String object to reference CameraID
    private String mCameraId;

    private Size mPreviewSize;

    // Use SparseArray of integer to store orientation types
    private static SparseIntArray ORIENTATION = new SparseIntArray();
    static {
        ORIENTATION.append(Surface.ROTATION_0, 0);
        ORIENTATION.append(Surface.ROTATION_90, 90);
        ORIENTATION.append(Surface.ROTATION_180, 180);
        ORIENTATION.append(Surface.ROTATION_270, 270);
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mTextureView = (TextureView) findViewById(R.id.textureView);
    }

    @Override
    protected void onResume(){

        super.onResume();

        startBackgroundThread();

        // Checking if TextureView is available
        if (mTextureView.isAvailable()){

            // When TextureView is available setup the camera with respective width and height
            setupCamera(mTextureView.getWidth(), mTextureView.getHeight());

            // Connect to camera
            connectCamera();

        } else {

            // If not available, inflate it
            mTextureView.setSurfaceTextureListener(mSurfaceTextureListener);

        }
    }

    @Override
    protected void onPause() {

        closeCamera();

        stopBackgroundThread();

        super.onPause();

    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        // Check the request code
        if (requestCode == REQUEST_CAMERA_PERMISSION_RESULT) {
            // If the request was granted or not
            if (grantResults[0] != PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(getApplicationContext(), "App won't run without camera permission", Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        // View to show decorated layout on view pager
        View decorView = getWindow().getDecorView();

        if (hasFocus){
            // If focused DecorView will run full screen
            // View.SYSTEM_UI_FLAG_LAYOUT_STABLE --> Stable the transition
            // View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY --> Means swiping up and
            //      down will reveal system UI settings
            // View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN --> Remove artifacts
            //      when switching in and out of that mode
            // View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION --> Remove artifacts
            //      when switching in and out of that mode
            // View.SYSTEM_UI_FLAG_FULLSCREEN --> View goes to normal fullscreen mode so that
            //      its content can take over the screen while still allowing the user to interact
            // View.SYSTEM_UI_FLAG_HIDE_NAVIGATION -->  View has requested that system navigation
            //      can temporarily be hidden
            decorView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_STABLE
            | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
            | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
            | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
            | View.SYSTEM_UI_FLAG_FULLSCREEN
            | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION);
        }
    }

    /**
     * This method setup the specific camera by getting the CameraId
     * @param width - width of the TextureView layout
     * @param height - height of the TextureView layout
     */

    private void setupCamera (int width, int height){

        // CameraManager object handles CAMERA_SERVICE of the system
        CameraManager cameraManager = (CameraManager) getSystemService(Context.CAMERA_SERVICE);

        // CameraManager object gets the list of cameraId for the device
        // Need to have exception handler because we are making a call to CameraDevice itself
        try{
            for (String cameraId : cameraManager.getCameraIdList()){

                // For each id, we get the characteristic of that camera
                CameraCharacteristics cameraCharacteristics = cameraManager.getCameraCharacteristics(cameraId);

                if (cameraCharacteristics.get(cameraCharacteristics.LENS_FACING) == cameraCharacteristics.LENS_FACING_FRONT){
                    // If we get front facing camera, we continue the for loop without storing the cameraId
                    // continue;
                } else {
                    // Map for all the different resolution represented by the preview, by the camera, by the video...
                    StreamConfigurationMap map = cameraCharacteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);

                    // Getting the device orientation
                    int deviceOrientation = getWindowManager().getDefaultDisplay().getRotation();
                    // Getting the total orientation
                    int totalRotation = sensorToDeviceRotaion(cameraCharacteristics, deviceOrientation);
                    int rotatedWidth = width;
                    int rotatedHeight = height;

                    if (totalRotation == 90 || totalRotation == 270){
                        rotatedWidth = height;
                        rotatedHeight = width;
                    }

                    // Setting up the preview display size
                    mPreviewSize = chooseOptimalSize(map.getOutputSizes(SurfaceTexture.class), rotatedWidth, rotatedHeight);
                    // Getting the cameraId
                    mCameraId = cameraId;
                }
            }
        } catch (CameraAccessException e){
            e.printStackTrace();
        }
    }

    /**
     * Method for connecting to camera
     */
    private void connectCamera (){
        // Instance of the CameraManager object to connect to camera
        CameraManager cameraManager = (CameraManager) getSystemService(Context.CAMERA_SERVICE);

        try {
            // Check if SDK version is at least 23
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M){
                // Check if the permission has been granted in AndroidManifest.xml
                if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) ==
                        PackageManager.PERMISSION_GRANTED) {
                    cameraManager.openCamera(mCameraId, mCameraDeviceStateCallback, mBackgroundHandler);
                } else {
                    // if the permission was denied before, system prompt permission request
                    if (shouldShowRequestPermissionRationale(Manifest.permission.CAMERA)){
                        Toast.makeText(this, "Video app requires access to camera", Toast.LENGTH_SHORT).show();
                    }
                    // If it is the 1st time
                    requestPermissions (new String[] {Manifest.permission.CAMERA}, REQUEST_CAMERA_PERMISSION_RESULT);
                }
            } else {
                // For SDK older than 23, just open the camera
                cameraManager.openCamera(mCameraId, mCameraDeviceStateCallback, mBackgroundHandler);
            }
        } catch (CameraAccessException e){
            e.printStackTrace();
        }
    }

    /**
     * This method cleanup the CameraDevice resources
     */
    private void closeCamera(){
        if (mCameraDevice != null){
            // If CameraDevice is occupied, clean up the CameraDevice
            mCameraDevice.close();
            mCameraDevice = null;
        }
    }

    /**
     * This method starts a background thread, so that time consuming task does not affect the UI
     */
    private void startBackgroundThread(){
        mBackgroundHandlerThread = new HandlerThread("CustomizedCamera");
        mBackgroundHandlerThread.start();
        // Once the thread started, we can setup a handler pointing to that thread
        mBackgroundHandler = new Handler(mBackgroundHandlerThread.getLooper());
    }

    /**
     * This method stops the background thread safely by not letting it affected by other applications
     */
    private void stopBackgroundThread(){
        // Block on the tread, and stop everything else form interupting it
        // and let it clean up without being affected by other application
        mBackgroundHandlerThread.quitSafely();

        try {
            mBackgroundHandlerThread.join();
            mBackgroundHandlerThread = null;
            mBackgroundHandler = null;
        } catch(InterruptedException e){
            e.printStackTrace();
        }

    }

    /**
     *
     * @param cameraCharacteristics --> object to figure out camera attributes
     * @param deviceOrientaion --> The orientation status of the device
     * @return --> the angle of mod of total camera orientation and device orientation
     */
    private static int sensorToDeviceRotaion(CameraCharacteristics cameraCharacteristics, int deviceOrientaion){

        // Getting the orientation of the camera
        int sensorOrientaion = cameraCharacteristics.get(CameraCharacteristics.SENSOR_ORIENTATION);

        // Getting the oritentaion of the device
        deviceOrientaion = ORIENTATION.get(deviceOrientaion);

        // Returns the degree of total orientation
        return (sensorOrientaion + deviceOrientaion + 360) % 360;
    }

    private static Size chooseOptimalSize (Size[] choices, int width, int height) {

        // Is the resolution field sensor big enough for our display
        List<Size> bigEnough = new ArrayList<Size>();

        // Iterating over all the preview resolutions
        for (Size option : choices){
            // Check if 1. Aspect ratio matches the TextureView
            //          2. Value from the preview sensor is big enough for width
            //          3. and height wise for our requested TextureView
            if (option.getHeight() == option.getWidth() * height / width &&
                    option.getWidth() >= width && option.getHeight() >= height) {

                bigEnough.add(option);
            }
        }

        if (bigEnough.size() > 0) {
            // Returns the minimum size from the list
            return Collections.min(bigEnough, new CompareSizeByArea());
        } else {
            return choices[0];
        }
    }
}