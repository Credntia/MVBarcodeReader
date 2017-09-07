package devliving.online.mvbarcodereader;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.hardware.Camera;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.GestureDetector;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.gms.vision.MultiProcessor;
import com.google.android.gms.vision.barcode.Barcode;
import com.google.android.gms.vision.barcode.BarcodeDetector;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import online.devliving.mobilevisionpipeline.GraphicOverlay;
import online.devliving.mobilevisionpipeline.camera.CameraSource;
import online.devliving.mobilevisionpipeline.camera.CameraSourcePreview;

/**
 * Created by Mehedi on 10/2/16.
 */
public class BarcodeCaptureFragment extends Fragment implements View.OnTouchListener {
    // intent request code to handle updating play services if needed.
    private static final int RC_HANDLE_GMS = 9001;

    // permission request codes need to be < 256
    private static final int RC_HANDLE_CAMERA_PERM = 2;

    int mFormats = Barcode.ALL_FORMATS;
    MVBarcodeScanner.ScanningMode mMode = MVBarcodeScanner.ScanningMode.SINGLE_AUTO;
    CameraSourcePreview.PreviewScaleType mPreviewScaleType = CameraSourcePreview.PreviewScaleType.FILL;

    FrameLayout topLayout;
    ImageButton flashToggle;

    boolean mFlashOn = false;

    final Object mLock = new Object();

    private CameraSource mCameraSource;
    private CameraSourcePreview mPreview;
    private GraphicOverlay<BarcodeGraphic> mGraphicOverlay;
    private BarcodeScanningListener mListener;

    // helper objects for detecting taps and pinches.
    private ScaleGestureDetector scaleGestureDetector;
    private GestureDetector gestureDetector;

    public static BarcodeCaptureFragment instantiate(MVBarcodeScanner.ScanningMode mode,
                                                     @MVBarcodeScanner.BarCodeFormat int... formats) {
        BarcodeCaptureFragment fragment = new BarcodeCaptureFragment();
        Bundle args = new Bundle();
        if (mode != null) args.putSerializable(MVBarcodeScanner.SCANNING_MODE, mode);

        if (formats != null && formats.length > 0) {
            int barcodeFormats = formats[0];

            if (formats.length > 1) {
                for (int i = 1; i < formats.length; i++) {
                    barcodeFormats = barcodeFormats | formats[i];
                }
            }

            args.putInt(MVBarcodeScanner.BARCODE_FORMATS, barcodeFormats);
        }
        fragment.setArguments(args);
        return fragment;
    }

    public static BarcodeCaptureFragment instantiate(MVBarcodeScanner.ScanningMode mode, CameraSourcePreview.PreviewScaleType scaleType,
                                                     @MVBarcodeScanner.BarCodeFormat int... formats) {
        BarcodeCaptureFragment fragment = instantiate(mode, formats);
        fragment.getArguments().putSerializable(MVBarcodeScanner.PREVIEW_SCALE_TYPE, scaleType);
        return fragment;
    }

    public void setListener(BarcodeScanningListener listener) {
        mListener = listener;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (getArguments() != null && !getArguments().isEmpty()) {
            if (getArguments().containsKey(MVBarcodeScanner.SCANNING_MODE))
                mMode = (MVBarcodeScanner.ScanningMode) getArguments().getSerializable(MVBarcodeScanner.SCANNING_MODE);

            if (getArguments().containsKey(MVBarcodeScanner.BARCODE_FORMATS))
                mFormats = getArguments().getInt(MVBarcodeScanner.BARCODE_FORMATS);

            if (getArguments().containsKey(MVBarcodeScanner.PREVIEW_SCALE_TYPE))
                mPreviewScaleType = (CameraSourcePreview.PreviewScaleType) getArguments().getSerializable(MVBarcodeScanner.PREVIEW_SCALE_TYPE);
        }
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View content = inflater.inflate(R.layout.barcode_capture, container, false);
        mPreview = content.findViewById(R.id.preview);
        mGraphicOverlay = content.findViewById(R.id.graphicOverlay);
        topLayout = content.findViewById(R.id.topLayout);
        flashToggle = content.findViewById(R.id.flash_torch);

        flashToggle.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.d("SCANNER-FRAGMENT", "Got tap on Flash");
                if (mCameraSource.setFlashMode(mFlashOn ? Camera.Parameters.FLASH_MODE_OFF : Camera.Parameters.FLASH_MODE_TORCH)) {
                    mFlashOn = !mFlashOn;
                    flashToggle.setImageResource(mFlashOn ? R.drawable.ic_torch_on : R.drawable.ic_torch);
                }
            }
        });
        mPreview.setScaletype(mPreviewScaleType);
        gestureDetector = new GestureDetector(getActivity(), new CaptureGestureListener());
        scaleGestureDetector = new ScaleGestureDetector(getActivity(), new ScaleListener());

        content.setOnTouchListener(this);
        return content;
    }

    @Override
    public void onResume() {
        super.onResume();

        if (mCameraSource == null) {
            int rc = ActivityCompat.checkSelfPermission(getActivity(), Manifest.permission.CAMERA);
            if (rc == PackageManager.PERMISSION_GRANTED) {
                initiateCamera();
            } else {
                requestCameraPermission();
            }
        } else startCameraSource();
    }

    /**
     * Make sure you have camera permissions before calling this
     */
    protected void initiateCamera(){
        createCameraSource();
        startCameraSource();
    }

    /**
     * Stops the camera.
     */
    protected void stopCamera(){
        if (mPreview != null) {
            mPreview.stop();
        }
    }

    /**
     * Releases the resources associated with the camera source, the associated detectors, and the
     * rest of the processing pipeline.
     */
    protected void releaseCamera(){
        if (mPreview != null) {
            mPreview.release();
        }
    }


    @Override
    public void onPause() {
        super.onPause();
        stopCamera();
    }


    @Override
    public void onDestroy() {
        super.onDestroy();
        releaseCamera();
    }

    /**
     * Callback for the result from requesting permissions. This method
     * is invoked for every call on {@link #requestPermissions(String[], int)}.
     * <p>
     * <strong>Note:</strong> It is possible that the permissions request interaction
     * with the user is interrupted. In this case you will receive empty permissions
     * and results arrays which should be treated as a cancellation.
     * </p>
     *
     * @param requestCode  The request code passed in {@link #requestPermissions(String[], int)}.
     * @param permissions  The requested permissions. Never null.
     * @param grantResults The grant results for the corresponding permissions
     *                     which is either {@link PackageManager#PERMISSION_GRANTED}
     *                     or {@link PackageManager#PERMISSION_DENIED}. Never null.
     * @see #requestPermissions(String[], int)
     */
    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        if (requestCode != RC_HANDLE_CAMERA_PERM) {
            Log.d("BARCODE-SCANNER", "Got unexpected permission result: " + requestCode);
            super.onRequestPermissionsResult(requestCode, permissions, grantResults);
            return;
        }

        if (grantResults.length != 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            Log.d("BARCODE-SCANNER", "Camera permission granted - initialize the camera source");
            // we have permission, so create the camerasource
            initiateCamera();

            return;
        }

        Log.e("BARCODE-SCANNER", "Permission not granted: results len = " + grantResults.length +
                " Result code = " + (grantResults.length > 0 ? grantResults[0] : "(empty)"));

        if (mListener != null) mListener.onBarcodeScanningFailed("Camera permission denied");
        else {
            DialogInterface.OnClickListener listener = new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int id) {
                    getActivity().finish();
                }
            };

            AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
            builder.setTitle("Camera Permission Denied")
                    .setMessage(R.string.no_camera_permission)
                    .setPositiveButton(R.string.ok, listener)
                    .show();
        }
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);

        if (context instanceof BarcodeScanningListener)
            mListener = (BarcodeScanningListener) context;
    }

    /**
     * Handles the requesting of the camera permission.  This includes
     * showing a "Snackbar" message of why the permission is needed then
     * sending the request.
     */
    private void requestCameraPermission() {
        Log.w("BARCODE-SCANNER", "Camera permission is not granted. Requesting permission");

        final String[] permissions = new String[]{Manifest.permission.CAMERA};

        if (!shouldShowRequestPermissionRationale(Manifest.permission.CAMERA)) {
            requestPermissions(permissions, RC_HANDLE_CAMERA_PERM);
            return;
        }

        Snackbar.make(mGraphicOverlay, R.string.permission_camera_rationale, Snackbar.LENGTH_INDEFINITE)
                .setAction(R.string.ok, new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        requestPermissions(permissions, RC_HANDLE_CAMERA_PERM);
                    }
                })
                .show();


        /*
        new AlertDialog.Builder(getContext())
                .setTitle(R.string.perm_required)
                .setMessage(R.string.permission_camera_rationale)
                .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        //dialogInterface.dismiss();
                        requestPermissions(permissions, RC_HANDLE_CAMERA_PERM);
                    }
                })
                .setCancelable(false)
                .show();*/
    }

    /**
     * Creates and starts the camera.  Note that this uses a higher resolution in comparison
     * to other detection examples to enable the barcode detector to detect small barcodes
     * at long distances.
     * <p/>
     * Suppressing InlinedApi since there is a check that the minimum version is met before using
     * the constant.
     */
    @SuppressLint("InlinedApi")
    private void createCameraSource() {
        Context context = getActivity().getApplicationContext();

        // A barcode detector is created to track barcodes.  An associated multi-processor instance
        // is set to receive the barcode detection results, track the barcodes, and maintain
        // graphics for each barcode on screen.  The factory is used by the multi-processor to
        // create a separate tracker instance for each barcode.
        BarcodeDetector barcodeDetector = new BarcodeDetector.Builder(context)
                .setBarcodeFormats(mFormats)
                .build();

        BarcodeTrackerFactory barcodeFactory = new BarcodeTrackerFactory(mGraphicOverlay, new BarcodeGraphicTracker.BarcodeDetectionListener() {
            @Override
            public void onNewBarcodeDetected(int id, Barcode barcode) {
                if (barcode != null) onBarcodeDetected(barcode);
                else if (mGraphicOverlay.getFirstGraphic() != null && mGraphicOverlay.getFirstGraphic().getBarcode() != null) {
                    onBarcodeDetected(mGraphicOverlay.getFirstGraphic().getBarcode());
                }
            }
        });

        barcodeDetector.setProcessor(
                new MultiProcessor.Builder<>(barcodeFactory).build());

        if (!barcodeDetector.isOperational()) {
            // Note: The first time that an app using the barcode or face API is installed on a
            // device, GMS will download a native libraries to the device in order to do detection.
            // Usually this completes before the app is run for the first time.  But if that
            // download has not yet completed, then the above call will not detect any barcodes
            // and/or faces.
            //
            // isOperational() can be used to check if the required native libraries are currently
            // available.  The detectors will automatically become operational once the library
            // downloads complete on device.
            Log.w("BARCODE-SCANNER", "Detector dependencies are not yet available.");

            // Check for low storage.  If there is low storage, the native library will not be
            // downloaded, so detection will not become operational.
            IntentFilter lowstorageFilter = new IntentFilter(Intent.ACTION_DEVICE_STORAGE_LOW);
            boolean hasLowStorage = getActivity().registerReceiver(null, lowstorageFilter) != null;

            if (hasLowStorage) {
                if (mListener == null)
                    Toast.makeText(getActivity(), R.string.low_storage_error, Toast.LENGTH_LONG).show();
                else
                    mListener.onBarcodeScanningFailed("Barcode detector dependencies cannot be downloaded due to low storage");
                Log.w("BARCODE-SCANNER", getString(R.string.low_storage_error));
            }
        }

        //boolean isPortrait = mPreview.isPortraitMode();

        // Creates and starts the camera.  Note that this uses a higher resolution in comparison
        // to other detection examples to enable the barcode detector to detect small barcodes
        // at long distances.
        CameraSource.Builder builder = new CameraSource.Builder(getActivity().getApplicationContext(), barcodeDetector)
                .setFacing(CameraSource.CAMERA_FACING_BACK)
                .setRequestedFps(15.0f);

        // make sure that auto focus is an available option
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH) {
            builder = builder.setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE);
        }

        mCameraSource = builder.build();
        Log.d("SCANNER-FRAGMENT", "created camera source");
    }

    /**
     * Starts or restarts the camera source, if it exists.  If the camera source doesn't exist yet
     * (e.g., because onResume was called before the camera source was created), this will be called
     * again when the camera source is created.
     */
    private void startCameraSource() throws SecurityException {
        // check that the device has play services available.
        int code = GoogleApiAvailability.getInstance().isGooglePlayServicesAvailable(
                getActivity().getApplicationContext());
        if (code != ConnectionResult.SUCCESS) {
            Dialog dlg =
                    GoogleApiAvailability.getInstance().getErrorDialog(getActivity(), code, RC_HANDLE_GMS);
            dlg.show();
        }

        if (mCameraSource != null) {
            try {
                mPreview.start(mCameraSource, mGraphicOverlay);
            } catch (IOException e) {
                Log.e("BARCODE-SCANNER", "Unable to start camera source.", e);
                mCameraSource.release();
                mCameraSource = null;
                if (mListener != null)
                    mListener.onBarcodeScanningFailed("could not create camera source");
            }
        }
    }

    /**
     * Called when a touch event is dispatched to a view. This allows listeners to
     * get a chance to respond before the target view.
     *
     * @param v     The view the touch event has been dispatched to.
     * @param event The MotionEvent object containing full information about
     *              the event.
     * @return True if the listener has consumed the event, false otherwise.
     */
    @Override
    public boolean onTouch(View v, MotionEvent event) {
        boolean b = scaleGestureDetector.onTouchEvent(event);

        boolean c = gestureDetector.onTouchEvent(event);

        return b || c || v.onTouchEvent(event);
    }

    /**
     * onTap is called to capture the oldest barcode currently detected and
     * return it to the caller.
     *
     * @param rawX - the raw position of the tap
     * @param rawY - the raw position of the tap.
     * @return true if the activity is ending.
     */
    protected boolean onTap(float rawX, float rawY) {
        Barcode barcode = null;

        if (mMode == MVBarcodeScanner.ScanningMode.SINGLE_AUTO) {
            BarcodeGraphic graphic = mGraphicOverlay.getFirstGraphic();

            if (graphic != null) {
                barcode = graphic.getBarcode();
                if (barcode != null && mListener != null) {
                    mListener.onBarcodeScanned(barcode);
                }
            }
        } else if (mMode == MVBarcodeScanner.ScanningMode.SINGLE_MANUAL) {
            Set<BarcodeGraphic> graphicSet = mGraphicOverlay.getAllGraphics();
            if (graphicSet != null && !graphicSet.isEmpty()) {
                for (BarcodeGraphic graphic : graphicSet) {
                    if (graphic != null && graphic.isPointInsideBarcode(rawX, rawY)) {
                        barcode = graphic.getBarcode();
                        break;
                    }
                }

                if (mListener != null && barcode != null) {
                    mListener.onBarcodeScanned(barcode);
                }
            }
        } else {
            Set<BarcodeGraphic> graphicSet = mGraphicOverlay.getAllGraphics();
            if (graphicSet != null && !graphicSet.isEmpty()) {
                List<Barcode> barcodes = new ArrayList<>();

                for (BarcodeGraphic graphic : graphicSet) {
                    if (graphic != null) {
                        barcode = graphic.getBarcode();
                        if (barcode != null) barcodes.add(barcode);
                    }
                }

                if (mListener != null && !barcodes.isEmpty()) {
                    mListener.onBarcodesScanned(barcodes);
                }
            }
        }

        return barcode != null;
    }

    boolean isListenerBusy = false;
    protected void onBarcodeDetected(final Barcode barcode){
        Log.d("BARCODE-SCANNER", "NEW BARCODE DETECTED");
        if (mMode == MVBarcodeScanner.ScanningMode.SINGLE_AUTO && mListener != null) {
            synchronized (mLock){
                if(!isListenerBusy) {
                    isListenerBusy = true;
                    new Handler(Looper.getMainLooper()).post(new Runnable() {
                        @Override
                        public void run() {
                            if (mCameraSource != null) mCameraSource.stop();
                            mListener.onBarcodeScanned(barcode);
                            isListenerBusy = false;
                        }
                    });
                }
            }
        }
    }

    private class CaptureGestureListener extends GestureDetector.SimpleOnGestureListener {

        @Override
        public boolean onSingleTapConfirmed(MotionEvent e) {

            return onTap(e.getRawX(), e.getRawY()) || super.onSingleTapConfirmed(e);
        }
    }

    private class ScaleListener implements ScaleGestureDetector.OnScaleGestureListener {

        /**
         * Responds to scaling events for a gesture in progress.
         * Reported by pointer motion.
         *
         * @param detector The detector reporting the event - use this to
         *                 retrieve extended info about event state.
         * @return Whether or not the detector should consider this event
         * as handled. If an event was not handled, the detector
         * will continue to accumulate movement until an event is
         * handled. This can be useful if an application, for example,
         * only wants to update scaling factors if the change is
         * greater than 0.01.
         */
        @Override
        public boolean onScale(ScaleGestureDetector detector) {
            return false;
        }

        /**
         * Responds to the beginning of a scaling gesture. Reported by
         * new pointers going down.
         *
         * @param detector The detector reporting the event - use this to
         *                 retrieve extended info about event state.
         * @return Whether or not the detector should continue recognizing
         * this gesture. For example, if a gesture is beginning
         * with a focal point outside of a region where it makes
         * sense, onScaleBegin() may return false to ignore the
         * rest of the gesture.
         */
        @Override
        public boolean onScaleBegin(ScaleGestureDetector detector) {
            return true;
        }

        /**
         * Responds to the end of a scale gesture. Reported by existing
         * pointers going up.
         * <p/>
         * Once a scale has ended, {@link ScaleGestureDetector#getFocusX()}
         * and {@link ScaleGestureDetector#getFocusY()} will return focal point
         * of the pointers remaining on the screen.
         *
         * @param detector The detector reporting the event - use this to
         *                 retrieve extended info about event state.
         */
        @Override
        public void onScaleEnd(ScaleGestureDetector detector) {
            mCameraSource.doZoom(detector.getScaleFactor());
        }
    }

    public interface BarcodeScanningListener {
        void onBarcodeScanned(Barcode barcode);

        void onBarcodesScanned(List<Barcode> barcodes);

        void onBarcodeScanningFailed(String reason);
    }
}
