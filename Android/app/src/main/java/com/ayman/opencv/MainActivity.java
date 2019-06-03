package com.ayman.opencv;

import android.Manifest;
import android.app.Activity;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.SurfaceView;
import android.view.View;
import android.view.WindowManager;
import android.widget.ScrollView;
import android.widget.SeekBar;
import android.widget.TextView;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.CameraBridgeViewBase.CvCameraViewFrame;
import org.opencv.android.CameraBridgeViewBase.CvCameraViewListener2;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.MatOfPoint2f;
import org.opencv.core.Point;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends Activity implements CvCameraViewListener2, View.OnTouchListener {
    private static final String TAG = "OCVSample::Activity";
    private static final int MY_PERMISSIONS_REQUEST_CAMERA = 0;

    private CameraBridgeViewBase mOpenCvCameraView;
    private boolean mIsJavaCamera = true;
    private MenuItem mItemSwitchCamera = null;
    private boolean valuesVisible = false;
    private SeekBar hmin, smin, vmin, hmax, smax, vmax;
    private TextView hminValue, sminValue, vminValue, hmaxValue, smaxValue, vmaxValue;
    private SharedPreferences defaultSharedPreferences;
    private ScrollView mSettings;
    private float ASPECT_RATIO;
    private static final Scalar lowerb = new Scalar(80, 40, 10);
    private static final Scalar upperb = new Scalar(100, 230, 255);
    private static final Scalar circleColor = new Scalar(255, 255, 255);
    private static final Scalar contourColor = new Scalar(240, 240, 60);
    private static final Scalar maxContourColor = new Scalar(0, 255, 255);
    private static final int width = 400;
    private ArduinoSerial serial;
    private long currentTime;
    private Mat outputFrame;
    private Mat structuringElement;
    private Point center = new Point();
    private float radius[] = new float[1];
    private static final double ratio = 0.20344699428;
    private static final double near = 317;//368.64639;
    private boolean detected;
    private boolean exit;

    private BaseLoaderCallback mLoaderCallback = new BaseLoaderCallback(this) {
        @Override
        public void onManagerConnected(int status) {
            switch (status) {
                case LoaderCallbackInterface.SUCCESS: {
                    Log.i(TAG, "OpenCV loaded successfully");
                    mOpenCvCameraView.enableView();
                }
                break;
                default: {
                    super.onManagerConnected(status);
                }
                break;
            }
        }
    };

    public MainActivity() {
        Log.i(TAG, "Instantiated new " + this.getClass());
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        Log.i(TAG, "called onCreate");
        super.onCreate(savedInstanceState);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        setContentView(R.layout.activity_main);

        mOpenCvCameraView = (CameraBridgeViewBase) findViewById(R.id.camera_view);
        mOpenCvCameraView.setVisibility(SurfaceView.VISIBLE);
        mOpenCvCameraView.setCvCameraViewListener(this);
        mOpenCvCameraView.setOnTouchListener(this);

        mSettings = findViewById(R.id.settings);
        mSettings.setVisibility(View.GONE);

        hmin = findViewById(R.id.h_min);
        smin = findViewById(R.id.s_min);
        vmin = findViewById(R.id.v_min);
        hmax = findViewById(R.id.h_max);
        smax = findViewById(R.id.s_max);
        vmax = findViewById(R.id.v_max);
        hminValue = findViewById(R.id.h_min_value);
        sminValue = findViewById(R.id.s_min_value);
        vminValue = findViewById(R.id.v_min_value);
        hmaxValue = findViewById(R.id.h_max_value);
        smaxValue = findViewById(R.id.s_max_value);
        vmaxValue = findViewById(R.id.v_max_value);
        defaultSharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        SeekBar.OnSeekBarChangeListener listener = new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                updateSeekBarValues();
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        };
        hmin.setOnSeekBarChangeListener(listener);
        smin.setOnSeekBarChangeListener(listener);
        vmin.setOnSeekBarChangeListener(listener);
        hmax.setOnSeekBarChangeListener(listener);
        smax.setOnSeekBarChangeListener(listener);
        vmax.setOnSeekBarChangeListener(listener);

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, MY_PERMISSIONS_REQUEST_CAMERA);
        }

        serial = new ArduinoSerial(this);
    }

    private void updateSeekBarValues() {
        int hminValue = hmin.getProgress();
        int sminValue = smin.getProgress();
        int vminValue = vmin.getProgress();
        int hmaxValue = hmax.getProgress();
        int smaxValue = smax.getProgress();
        int vmaxValue = vmax.getProgress();
        defaultSharedPreferences.edit().putInt("hmin", hminValue).putInt("smin", sminValue).putInt("vmin", vminValue)
                .putInt("hmax", hmaxValue).putInt("smax", smaxValue).putInt("vmax", vmaxValue).apply();
        updateSeekBarValues(hminValue, sminValue, vminValue, hmaxValue, smaxValue, vmaxValue, false);
    }

    private void updateSeekBarValues(int hminValueInt, int sminValueInt, int vminValueInt, int hmaxValueInt, int smaxValueInt, int vmaxValueInt, boolean resume) {
        lowerb.set(new double[]{hminValueInt, sminValueInt, vminValueInt});
        upperb.set(new double[]{hmaxValueInt, smaxValueInt, vmaxValueInt});
        hminValue.setText(String.valueOf(hminValueInt));
        sminValue.setText(String.valueOf(sminValueInt));
        vminValue.setText(String.valueOf(vminValueInt));
        hmaxValue.setText(String.valueOf(hmaxValueInt));
        smaxValue.setText(String.valueOf(smaxValueInt));
        vmaxValue.setText(String.valueOf(vmaxValueInt));
        if (resume) {
            hmin.setProgress(hminValueInt);
            smin.setProgress(sminValueInt);
            vmin.setProgress(vminValueInt);
            hmax.setProgress(hmaxValueInt);
            smax.setProgress(smaxValueInt);
            vmax.setProgress(vmaxValueInt);

        }
    }

    @Override
    public void onPause() {
        super.onPause();
        if (mOpenCvCameraView != null)
            mOpenCvCameraView.disableView();
    }

    @Override
    public void onResume() {
        super.onResume();
        if (!OpenCVLoader.initDebug()) {
            Log.d(TAG, "Internal OpenCV library not found. Using OpenCV Manager for initialization");
            OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_3_0_0, this, mLoaderCallback);
        } else {
            Log.d(TAG, "OpenCV library found inside package. Using it!");
            mLoaderCallback.onManagerConnected(LoaderCallbackInterface.SUCCESS);
        }

        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(this);
        updateSeekBarValues(
                sp.getInt("hmin", 0),
                sp.getInt("smin", 0),
                sp.getInt("vmin", 0),
                sp.getInt("hmax", 0),
                sp.getInt("smax", 0),
                sp.getInt("vmax", 0), true);
    }

    public void onDestroy() {
        super.onDestroy();
        if (mOpenCvCameraView != null)
            mOpenCvCameraView.disableView();
        exit = true;
        serial.destroy();
    }

    public void onCameraViewStarted(int width, int height) {
        outputFrame = new Mat();
        structuringElement = Imgproc.getStructuringElement(Imgproc.MORPH_RECT, new Size(2, 2));
        Log.d("ScreenWidth", width + "");
        Log.d("ScreenHeight", height + "");
        ASPECT_RATIO = (float) width / height;
    }

    public void onCameraViewStopped() {
    }

    public Mat onCameraFrame(CvCameraViewFrame inputFrame) {
        Mat frame = inputFrame.rgba();
        frame.copyTo(outputFrame);
        Imgproc.resize(frame, outputFrame, new Size(width, width / ASPECT_RATIO));
        Imgproc.GaussianBlur(outputFrame, outputFrame, new Size(11, 11), 0);
        Imgproc.cvtColor(outputFrame, outputFrame, Imgproc.COLOR_BGR2HSV);
        Core.inRange(outputFrame, lowerb, upperb, outputFrame);
        Imgproc.erode(outputFrame, outputFrame, structuringElement);
        Imgproc.dilate(outputFrame, outputFrame, structuringElement);
        List<MatOfPoint> contours = new ArrayList<>();
        Imgproc.findContours(outputFrame, contours, new Mat(), Imgproc.RETR_EXTERNAL, Imgproc.CHAIN_APPROX_SIMPLE);
        if (contours.size() > 0) {
            int maxI = 0;
            double maxArea = Imgproc.contourArea(contours.get(0));
            for (int i = 1; i < contours.size(); i++) {
                double currentArea = Imgproc.contourArea(contours.get(i));
                if (currentArea > maxArea) {
                    maxArea = currentArea;
                    maxI = i;
                }
            }
            MatOfPoint contour = contours.get(maxI);
            Imgproc.minEnclosingCircle(new MatOfPoint2f(contours.get(maxI).toArray()), center, radius);
//            Imgproc.circle(frame, center, (int) radius[0], circleColor, 10);
            Core.multiply(contour, new Scalar(1280.0f / width, 1280.0f / width), contour);
            Imgproc.drawContours(frame, contours, maxI, maxContourColor, 10);
            Log.d("TAG", radius[0] * 2 + " " + center.x + " " + center.y);
            detected = true;
            if (valuesVisible) {
                for (int i = 0; i < contours.size(); i++) {
                    if (i == maxI)
                        continue;
                    contour = contours.get(i);
                    Core.multiply(contour, new Scalar(1280.0f / width, 1280.0f / width), contour);
                    Imgproc.drawContours(frame, contours, i, contourColor, 10);
                }
            }
        } else {
            detected = false;
        }
        return frame;
    }

    public void begin(View view) {
        serial.begin();
        exit = false;
        new AsyncTask<Void, Void, Void>() {

            @Override
            protected Void doInBackground(Void... voids) {
                while (!exit) {
                    try {
                        Thread.sleep(100);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    if (detected) {
                        serial.send((int) Math.round(center.x), 0);
                        serial.send((int) Math.round(center.y), 1);
                        serial.send((int) Math.round(2 * radius[0]), 2);
                    }
                }

                return null;
            }

            @Override
            protected void onPostExecute(Void aVoid) {
                super.onPostExecute(aVoid);
                serial.stop();
            }
        }.execute();
    }

    public void stop(View view) {
        exit = true;
    }

    @Override
    public boolean onTouch(View v, MotionEvent event) {
        if (!valuesVisible) {
            mSettings.setVisibility(View.VISIBLE);
            valuesVisible = true;
        }
        return false;
    }

    public void hide(View view) {
        mSettings.setVisibility(View.GONE);
        valuesVisible = false;
    }
}
