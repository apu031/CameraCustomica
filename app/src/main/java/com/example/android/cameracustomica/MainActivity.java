package com.example.android.cameracustomica;

import android.content.Context;
import android.graphics.SurfaceTexture;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.os.Handler;
import android.os.HandlerThread;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.TextureView;
import android.view.View;
import android.widget.Toast;

public class MainActivity extends AppCompatActivity {

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

    private void setupCamera(int width, int height){

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
                    mCameraId = cameraId;
                }
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
}
