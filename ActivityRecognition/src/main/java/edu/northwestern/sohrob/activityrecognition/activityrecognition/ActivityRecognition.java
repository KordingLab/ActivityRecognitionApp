package edu.northwestern.sohrob.activityrecognition.activityrecognition;

import android.app.Activity;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.AssetManager;
import android.graphics.Color;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.ToneGenerator;
import android.os.Bundle;
import android.os.SystemClock;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.RadioButton;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesClient;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.location.ActivityRecognitionClient;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

import au.com.bytecode.opencsv.CSVWriter;
import edu.northwestern.sohrob.activityrecognition.activityrecognition.FeatureExtractor.Feature;
import edu.northwestern.sohrob.activityrecognition.activityrecognition.trees.LeafNode;


public class ActivityRecognition extends Activity implements GooglePlayServicesClient.ConnectionCallbacks,GooglePlayServicesClient.OnConnectionFailedListener,SensorEventListener {

    private static enum TrialType {WALK, SIT}

    TrialType _trialType = TrialType.WALK;

    private Context context;

    private ActivityRecognitionClient ARClient;
    private PendingIntent pIntent;

    private static final String NEW_DATA = "edu.northwestern.sohrob.activityrecognition.activityrecognition.NEW_DATA";
    private static final String DATA = "DATA";

    private ToneGenerator tg;

    private boolean _trial_started = false;

    private static final long WINDOW_SIZE = (long) 4e9; // sensor timestamps are in nanoseconds
    private static final long WINDOW_SHIFT = (long) 1e9; // sensor timestamps are in nanoseconds

    private final int f_interp = 50;    // (Hz) sampling frequency of interpolation prior to feature extraction

    private long trial_length_millis = 300000;
    private long n_success = (trial_length_millis/((long)(WINDOW_SHIFT/1e6)))/2;

    TextView sText;

    RandomForest RF;

    private int _RFClass;
    private List<Integer> _RFClass_list = new ArrayList<Integer>();
    private String _GoogleClass;
    private double _RFConfidence;
    private int _GoogleConfidence;

    private double _uptime_unix_sec;

    private long _t_trial_start;

    private BroadcastReceiver receiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            //String v = "Google:\n" + intent.getStringExtra("Activity") + " (" + intent.getExtras().getInt("Confidence") + "%)";
            //TextView gText = (TextView) findViewById(R.id.gText);
            //gText.setText(v);
            _GoogleClass = intent.getStringExtra("Activity");
            _GoogleConfidence = intent.getExtras().getInt("Confidence");
            //Log.i("Test", intent.getStringExtra("Activity") + "-" + intent.getExtras().getInt("Confidence"));
            /*if (intent.getStringExtra("Activity").equals("walking"))
                tg.startTone(ToneGenerator.TONE_CDMA_ALERT_CALL_GUARD, 200);*/
        }

    };

    private File csv_dir;

    private SensorManager sensors;
    private Sensor accelerometer, gyroscope;

    private final Clip _accelerometerClip = new Clip(3, WINDOW_SIZE, Clip.ACCELEROMETER);
    private final Clip _gyroscopeClip  = new Clip(3, WINDOW_SIZE, Clip.GYROSCOPE);
    //private final Clip _barometerClip = new Clip(1, WINDOW_SIZE, Clip.BAROMETER);

    private boolean _hasAccelerometer = false;
    private boolean _hasGyroscope = false;
    //private boolean _hasBarometer = false;

    private boolean _extractFeatures = false;
    private boolean _runRandomForest = false;
    private boolean _runDisplay = false;

    private boolean _writeSensorValues = false;

    private FeatureExtractor _accelerometerExtractor = null;
    private FeatureExtractor _gyroscopeExtractor = null;
    //private FeatureExtractor _barometerExtractor = null;

    private final HashMap<String, Double> _features = new HashMap<String, Double>();

    private final List<String[]> _accelerometer_towrite = new ArrayList<String[]>();
    private final List<String[]> _gyroscope_towrite = new ArrayList<String[]>();

    private long last_timestamp_acc = 0;
    private long last_timestamp_gyr = 0;
    //private long last_timestamp_bar = 0;


    private FeatureExtractor.Feature[] _featureList =
            {
                    Feature.ACC_MEAN, Feature.ACCX_MEAN, Feature.ACCY_MEAN, Feature.ACCZ_MEAN,
                    Feature.ACC_MEAN_ABS, Feature.ACCX_MEAN_ABS, Feature.ACCY_MEAN_ABS, Feature.ACCZ_MEAN_ABS,
                    Feature.ACCX_STD, Feature.ACCY_STD, Feature.ACCZ_STD,
                    Feature.ACCX_SKEW, Feature.ACCY_SKEW, Feature.ACCZ_SKEW,
                    Feature.ACCX_KURT, Feature.ACCY_KURT, Feature.ACCZ_KURT,
                    Feature.ACCX_DIFF_MEAN, Feature.ACCY_DIFF_MEAN, Feature.ACCZ_DIFF_MEAN,
                    Feature.ACCX_DIFF_STD, Feature.ACCY_DIFF_STD, Feature.ACCZ_DIFF_STD,
                    Feature.ACCX_DIFF_SKEW, Feature.ACCY_DIFF_SKEW, Feature.ACCZ_DIFF_SKEW,
                    Feature.ACCX_DIFF_KURT, Feature.ACCY_DIFF_KURT, Feature.ACCZ_DIFF_KURT,
                    Feature.ACCX_MAX, Feature.ACCY_MAX, Feature.ACCZ_MAX,
                    Feature.ACCX_MIN, Feature.ACCY_MIN, Feature.ACCZ_MIN,
                    Feature.ACCX_MAX_ABS, Feature.ACCY_MAX_ABS, Feature.ACCZ_MAX_ABS,
                    Feature.ACCX_MIN_ABS, Feature.ACCY_MIN_ABS, Feature.ACCZ_MIN_ABS,
                    Feature.ACCX_RMS, Feature.ACCY_RMS, Feature.ACCZ_RMS,
                    Feature.ACC_CROSS_XY, Feature.ACC_CROSS_YZ, Feature.ACC_CROSS_ZX,
                    Feature.ACC_CROSS_XY_ABS, Feature.ACC_CROSS_YZ_ABS, Feature.ACC_CROSS_ZX_ABS,
                    Feature.ACC_CROSS_XY_NORM, Feature.ACC_CROSS_YZ_NORM, Feature.ACC_CROSS_ZX_NORM,
                    Feature.ACC_CROSS_XY_NORM_ABS, Feature.ACC_CROSS_YZ_NORM_ABS, Feature.ACC_CROSS_ZX_NORM_ABS,
                    Feature.ACCX_HIST1, Feature.ACCX_HIST2, Feature.ACCX_HIST3, Feature.ACCX_HIST4, Feature.ACCX_HIST5, Feature.ACCX_HIST6,
                    Feature.ACCY_HIST1, Feature.ACCY_HIST2, Feature.ACCY_HIST3, Feature.ACCY_HIST4, Feature.ACCY_HIST5, Feature.ACCY_HIST6,
                    Feature.ACCZ_HIST1, Feature.ACCZ_HIST2, Feature.ACCZ_HIST3, Feature.ACCZ_HIST4, Feature.ACCZ_HIST5, Feature.ACCZ_HIST6,
                    Feature.ACCX_FFT1, Feature.ACCX_FFT2, Feature.ACCX_FFT3, Feature.ACCX_FFT4, Feature.ACCX_FFT5, Feature.ACCX_FFT6, Feature.ACCX_FFT7, Feature.ACCX_FFT8, Feature.ACCX_FFT9, Feature.ACCX_FFT10, Feature.ACCX_FFT11, Feature.ACCX_FFT12, Feature.ACCX_FFT13, Feature.ACCX_FFT14, Feature.ACCX_FFT15, Feature.ACCX_FFT16, Feature.ACCX_FFT17, Feature.ACCX_FFT18, Feature.ACCX_FFT19,
                    Feature.ACCY_FFT1, Feature.ACCY_FFT2, Feature.ACCY_FFT3, Feature.ACCY_FFT4, Feature.ACCY_FFT5, Feature.ACCY_FFT6, Feature.ACCY_FFT7, Feature.ACCY_FFT8, Feature.ACCY_FFT9, Feature.ACCY_FFT10, Feature.ACCY_FFT11, Feature.ACCY_FFT12, Feature.ACCY_FFT13, Feature.ACCY_FFT14, Feature.ACCY_FFT15, Feature.ACCY_FFT16, Feature.ACCY_FFT17, Feature.ACCY_FFT18, Feature.ACCY_FFT19,
                    Feature.ACCZ_FFT1, Feature.ACCZ_FFT2, Feature.ACCZ_FFT3, Feature.ACCZ_FFT4, Feature.ACCZ_FFT5, Feature.ACCZ_FFT6, Feature.ACCZ_FFT7, Feature.ACCZ_FFT8, Feature.ACCZ_FFT9, Feature.ACCZ_FFT10, Feature.ACCZ_FFT11, Feature.ACCZ_FFT12, Feature.ACCZ_FFT13, Feature.ACCZ_FFT14, Feature.ACCZ_FFT15, Feature.ACCZ_FFT16, Feature.ACCZ_FFT17, Feature.ACCZ_FFT18, Feature.ACCZ_FFT19,
                    Feature.GYR_MEAN, Feature.GYRX_MEAN, Feature.GYRY_MEAN, Feature.GYRZ_MEAN,
                    Feature.GYR_MEAN_ABS, Feature.GYRX_MEAN_ABS, Feature.GYRY_MEAN_ABS, Feature.GYRZ_MEAN_ABS,
                    Feature.GYRX_STD, Feature.GYRY_STD, Feature.GYRZ_STD,
                    Feature.GYRX_SKEW, Feature.GYRY_SKEW, Feature.GYRZ_SKEW,
                    Feature.GYRX_KURT, Feature.GYRY_KURT, Feature.GYRZ_KURT,
                    Feature.GYRX_DIFF_MEAN, Feature.GYRY_DIFF_MEAN, Feature.GYRZ_DIFF_MEAN,
                    Feature.GYRX_DIFF_STD, Feature.GYRY_DIFF_STD, Feature.GYRZ_DIFF_STD,
                    Feature.GYRX_DIFF_SKEW, Feature.GYRY_DIFF_SKEW, Feature.GYRZ_DIFF_SKEW,
                    Feature.GYRX_DIFF_KURT, Feature.GYRY_DIFF_KURT, Feature.GYRZ_DIFF_KURT,
                    Feature.GYRX_MAX, Feature.GYRY_MAX, Feature.GYRZ_MAX, Feature.GYRX_MIN, Feature.GYRY_MIN, Feature.GYRZ_MIN,
                    Feature.GYRX_MAX_ABS, Feature.GYRY_MAX_ABS, Feature.GYRZ_MAX_ABS, Feature.GYRX_MIN_ABS, Feature.GYRY_MIN_ABS, Feature.GYRZ_MIN_ABS,
                    Feature.GYRX_RMS, Feature.GYRY_RMS, Feature.GYRZ_RMS,
                    Feature.GYR_CROSS_XY, Feature.GYR_CROSS_YZ, Feature.GYR_CROSS_ZX,
                    Feature.GYR_CROSS_XY_ABS, Feature.GYR_CROSS_YZ_ABS, Feature.GYR_CROSS_ZX_ABS,
                    Feature.GYR_CROSS_XY_NORM, Feature.GYR_CROSS_YZ_NORM, Feature.GYR_CROSS_ZX_NORM,
                    Feature.GYR_CROSS_XY_NORM_ABS, Feature.GYR_CROSS_YZ_NORM_ABS, Feature.GYR_CROSS_ZX_NORM_ABS,
                    Feature.GYRX_HIST1, Feature.GYRX_HIST2, Feature.GYRX_HIST3, Feature.GYRX_HIST4, Feature.GYRX_HIST5, Feature.GYRX_HIST6,
                    Feature.GYRY_HIST1, Feature.GYRY_HIST2, Feature.GYRY_HIST3, Feature.GYRY_HIST4, Feature.GYRY_HIST5, Feature.GYRY_HIST6,
                    Feature.GYRZ_HIST1, Feature.GYRZ_HIST2, Feature.GYRZ_HIST3, Feature.GYRZ_HIST4, Feature.GYRZ_HIST5, Feature.GYRZ_HIST6,
                    Feature.GYRX_FFT1, Feature.GYRX_FFT2, Feature.GYRX_FFT3, Feature.GYRX_FFT4, Feature.GYRX_FFT5, Feature.GYRX_FFT6, Feature.GYRX_FFT7, Feature.GYRX_FFT8, Feature.GYRX_FFT9, Feature.GYRX_FFT10, Feature.GYRX_FFT11, Feature.GYRX_FFT12, Feature.GYRX_FFT13, Feature.GYRX_FFT14, Feature.GYRX_FFT15, Feature.GYRX_FFT16, Feature.GYRX_FFT17, Feature.GYRX_FFT18, Feature.GYRX_FFT19,
                    Feature.GYRY_FFT1, Feature.GYRY_FFT2, Feature.GYRY_FFT3, Feature.GYRY_FFT4, Feature.GYRY_FFT5, Feature.GYRY_FFT6, Feature.GYRY_FFT7, Feature.GYRY_FFT8, Feature.GYRY_FFT9, Feature.GYRY_FFT10, Feature.GYRY_FFT11, Feature.GYRY_FFT12, Feature.GYRY_FFT13, Feature.GYRY_FFT14, Feature.GYRY_FFT15, Feature.GYRY_FFT16, Feature.GYRY_FFT17, Feature.GYRY_FFT18, Feature.GYRY_FFT19,
                    Feature.GYRZ_FFT1, Feature.GYRZ_FFT2, Feature.GYRZ_FFT3, Feature.GYRZ_FFT4, Feature.GYRZ_FFT5, Feature.GYRZ_FFT6, Feature.GYRZ_FFT7, Feature.GYRZ_FFT8, Feature.GYRZ_FFT9, Feature.GYRZ_FFT10, Feature.GYRZ_FFT11, Feature.GYRZ_FFT12, Feature.GYRZ_FFT13, Feature.GYRZ_FFT14, Feature.GYRZ_FFT15, Feature.GYRZ_FFT16, Feature.GYRZ_FFT17, Feature.GYRZ_FFT18, Feature.GYRZ_FFT19
            };

    private Thread _featureThread = null;
    private Thread _randomForestThread = null;
    private Thread _displayThread = null;

    private Runnable _featureRunnable = new Runnable() {
            public void run() {
                while (_extractFeatures) {

                    long now = System.currentTimeMillis();

                    // checking if the clip has moved since last time -- accelerometer
                    if (_accelerometerClip.getTimestamps().size() > 0) {
                        if (_accelerometerClip.getLastTimestamp() == last_timestamp_acc) {
                            Log.e("FTC", "Warning: Clip hasn't moved since last feature extraction!");
                        } else {
                            last_timestamp_acc = _accelerometerClip.getLastTimestamp();
                            Log.i("FTC", "n_samp (acc) = " + _accelerometerClip.getTimestamps().size());
                        }
                    }

                    // checking if the clip has moved since last time -- gyroscope
                    if (_gyroscopeClip.getTimestamps().size() > 0) {
                        if (_gyroscopeClip.getLastTimestamp() == last_timestamp_gyr) {
                            Log.e("FTC", "Warning: Clip hasn't moved since last feature extraction!");
                        } else {
                            last_timestamp_gyr = _gyroscopeClip.getLastTimestamp();
                            Log.i("FTC", "n_samp (gyr) = " + _gyroscopeClip.getTimestamps().size());
                        }
                    }


                    if (_hasAccelerometer) {
                        synchronized (_accelerometerClip) {
                            synchronized (_features) {
                                _features.putAll(_accelerometerExtractor.extractFeatures(_accelerometerClip, f_interp));

                            }

                            if (_accelerometerClip.getValues().size() < 100) {
                                Log.e("FTC", "Warning: Low number of acc samples!");
                            }
                        }
                    }

                    if (_hasGyroscope) {
                        synchronized (_gyroscopeClip) {
                            synchronized (_features) {
                                _features.putAll(_gyroscopeExtractor.extractFeatures(_gyroscopeClip, f_interp));
                            }

                            if (_gyroscopeClip.getValues().size() < 100) {
                                Log.e("FTC", "Warning: Low number of gyro samples!");
                            }

                        }
                    }

                /*if (_hasBarometer) {
                    synchronized (_barometerClip) {
                        synchronized (_features) {
                            _features.putAll(_barometerExtractor.extractFeatures(_barometerClip, f_interp));
                        }

                        if (_barometerClip.getValues().size() < 100) {
                            Log.e("FEATURE EXTRACTION THREAD", "Warning: Low number of baro samples!");
                        }
                    }
                }*/

                    try {

                        //measuring the processing time
                        long deltaT = System.currentTimeMillis() - now;

                        /*synchronized (_features) {
                            _features.put(Feature.PROCESSING_TIME.toString(), (double) deltaT);
                        }*/

                        //accounting for the processing time / also converting from ns to ms
                        long sleepTime = WINDOW_SHIFT / (long) 1e6 - deltaT;
                        if (sleepTime < 0) sleepTime = 0;

                        Thread.sleep(sleepTime);
                    } catch (Exception e) {
                        _extractFeatures = false;
                        e.printStackTrace();
                    }
                }
            }
        };

    private Runnable _randomForestRunnable = new Runnable() {
        public void run() {
            while (_runRandomForest) {

                long now = System.currentTimeMillis();

                final HashMap<String, Object> _prediction;

                synchronized (_features) {
                    //Log.e("inf", _features+"");
                    _prediction = RF.evaluateModel(_features);
                }

                if (_prediction.get(LeafNode.PREDICTION)!=null) {
                    _RFClass = Integer.parseInt("" + _prediction.get(LeafNode.PREDICTION));
                    _RFConfidence = (Double) _prediction.get(LeafNode.ACCURACY) * 100;
                }
                else {
                    _RFClass = 0;
                    _RFConfidence = 0.0;
                }
                _RFClass_list.add(_RFClass);

                transmitAnalysis(""+_prediction.get(LeafNode.PREDICTION), ""+_prediction.get(LeafNode.ACCURACY));

                long deltaT = System.currentTimeMillis() - now;

                long sleepTime = WINDOW_SHIFT / (long) 1e6 - deltaT;
                if (sleepTime < 0) sleepTime = 0;

                try {
                    Thread.sleep(sleepTime);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

            }
        }
    };

    private Runnable _displayRunnable = new Runnable() {
        public void run() {
            while (_runDisplay) {

                long now = System.currentTimeMillis();

                String sCls = "";
                switch (_RFClass) {
                    case 1:
                        sCls = "walking";
                        break;
                    case 2:
                        sCls = "still";
                        break;
                }
                final String v = "Google:\n"+_GoogleClass+ " ("+_GoogleConfidence+"%)\n\n"+"Random Forest:\n" + sCls + " (" + (int)(_RFConfidence) + "%)";
                sText = (TextView) findViewById(R.id.sText);

                if (_trial_started) {
                    long elapsed_time = System.currentTimeMillis() - _t_trial_start;
                    if (elapsed_time > trial_length_millis) {
                        onEndTrial(false);
                        break;
                    }
                    switch (_trialType) {
                        case WALK:
                            Log.i("inf","n_success: "+n_success);
                            Log.i("inf","frequency: "+Collections.frequency(_RFClass_list, 1));
                            if (Collections.frequency(_RFClass_list, 2) > n_success) {  // success
                                onEndTrial(true);
                            }
                            if (Collections.frequency(_RFClass_list, 1) > n_success) {  // failure
                                onEndTrial(false);
                            }
                            if (_RFClass==1)
                                tg.startTone(ToneGenerator.TONE_CDMA_ALERT_CALL_GUARD, 200);
                            break;
                        case SIT:
                            if (Collections.frequency(_RFClass_list, 1) > n_success) {  // success
                                onEndTrial(true);
                            }
                            if (Collections.frequency(_RFClass_list, 2) > n_success) {  // failure
                                onEndTrial(false);
                            }
                            if (_RFClass==2)
                                tg.startTone(ToneGenerator.TONE_CDMA_ALERT_CALL_GUARD, 200);
                            break;
                    }
                }

                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        sText.setText(v);
                    }
                });

                long deltaT = System.currentTimeMillis() - now;

                //Log.e("inf","Display Processing Time: "+deltaT);

                long sleepTime = WINDOW_SHIFT / (long) 1e6 - deltaT;
                if (sleepTime < 0) sleepTime = 0;

                try {
                    Thread.sleep(sleepTime);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

            }
        }
    };


    @Override
    protected void onCreate(Bundle savedInstanceState) {

        Log.e("Test", "Starting OnCreate...");

        this._uptime_unix_sec = ((double) System.currentTimeMillis() - (double) SystemClock.uptimeMillis())/1000.0;

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_activity_recognition);

        tg = new ToneGenerator(AudioManager.STREAM_ALARM, 25);

        // registering the sensors (only accelerometer and gyroscope at the moment)
        sensors = (SensorManager) getSystemService(SENSOR_SERVICE);

        accelerometer = sensors.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        gyroscope = sensors.getDefaultSensor(Sensor.TYPE_GYROSCOPE);

        sensors.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_GAME, null);
        sensors.registerListener(this, gyroscope, SensorManager.SENSOR_DELAY_GAME, null);

        // initializing clips
        //this._accelerometerClip = new Clip(3, WINDOW_SIZE, Clip.ACCELEROMETER);
        //this._gyroscopeClip = new Clip(3, WINDOW_SIZE, Clip.GYROSCOPE);
        //this._barometerClip = new Clip(1, WINDOW_SIZE, Clip.BAROMETER);

        //initializing feature arrays
        ArrayList<String> accelerometerFeatures = new ArrayList<String>();
        ArrayList<String> gyroscopeFeatures = new ArrayList<String>();
        //ArrayList<String> barometerFeatures = new ArrayList<String>();

        for (Feature f : this._featureList)
        {
            String featureName = f.toString().toLowerCase();

            if (featureName.startsWith("acc"))
            {
                this._hasAccelerometer = true;

                accelerometerFeatures.add(f.toString());
            }
            else if (featureName.startsWith("gyr"))
            {
                this._hasGyroscope = true;

                gyroscopeFeatures.add(f.toString());
            }
            /*else if (featureName.startsWith("bar"))
            {
                this._hasBarometer = true;
                barometerFeatures.add(f.toString());
            }*/
        }

        //initializing feature extractor objects
        this._accelerometerExtractor = new FeatureExtractor(WINDOW_SIZE, accelerometerFeatures, 3);
        this._gyroscopeExtractor = new FeatureExtractor(WINDOW_SIZE, gyroscopeFeatures, 3);
        //this._barometerExtractor = new FeatureExtractor(WINDOW_SIZE, barometerFeatures, 1);

        // reading the RF model

        AssetManager assets = getAssets();

        try {

            BufferedReader in = new BufferedReader(new InputStreamReader(assets.open("matlab-forest.json")));

            StringBuilder sb = new StringBuilder();

            String inputLine;

            while ((inputLine = in.readLine()) != null)
                sb.append(inputLine);

            in.close();

            String contents = sb.toString();

            if (contents != null)
            {
                JSONObject json;
                RF = new RandomForest();

                try {

                    json = new JSONObject(contents);

                    Log.i("info","now generating the forest model...");
                    RF.generateModel(json.get("model"));

                } catch (JSONException e) {
                    e.printStackTrace();
                }

            }


        } catch (IOException e) {
            e.printStackTrace();
        }

        // setting up the csv writer

        csv_dir = this.getBaseContext().getExternalFilesDir(null);

        if (csv_dir==null)
            Log.e("INFO","Cannot access the local CSV directory!" );

        if (csv_dir!=null && !csv_dir.exists()) {
            csv_dir.mkdirs();
            Log.i("INFO","CSV directory did not exist and was created." );
        }

        context = getApplicationContext();

        Log.e("Test", "OnCreate Finished.");

    }

    @Override
    protected void onStart() {

        Log.e("Test", "running onStart...");
        super.onStart();

        //checking if Google Play Service is installed
        int resp = GooglePlayServicesUtil.isGooglePlayServicesAvailable(this);
        if (resp == ConnectionResult.SUCCESS) {
            ARClient = new ActivityRecognitionClient(this, this, this);
            ARClient.connect();
        }
        else {
            Toast.makeText(this, "Please install Google Play Service.", Toast.LENGTH_SHORT).show();
        }

        // setting up the intent filter for fetching Google's AR
        IntentFilter filter = new IntentFilter();
        filter.addAction("edu.northwestern.sohrob.myactivityrecognition.ACTIVITY_RECOGNITION_DATA");
        registerReceiver(receiver, filter);


    }

    @Override
    public void onConnected(Bundle arg0) {
        Log.e("Test", "running onConnected...");
        Intent intent = new Intent(this, ActivityRecognitionService.class);
        pIntent = PendingIntent.getService(this, 0, intent, PendingIntent.FLAG_CANCEL_CURRENT);
        ARClient.requestActivityUpdates(0, pIntent);

        // staring the feature extraction thread
        this._extractFeatures = true;
        if (this._featureThread == null || !this._featureThread.isAlive())
        {
            // A dead thread cannot be restarted. A new thread has to be created.
            this._featureThread = new Thread(this._featureRunnable);
            this._featureThread.start();
        }

        // staring the random forest thread
        this._runRandomForest = true;
        if (this._randomForestThread == null || !this._randomForestThread.isAlive())
        {
            // A dead thread cannot be restarted. A new thread has to be created.
            this._randomForestThread = new Thread(this._randomForestRunnable);
            this._randomForestThread.start();
        }

        // staring the display thread
        this._runDisplay = true;
        if (this._displayThread == null || !this._displayThread.isAlive())
        {
            // A dead thread cannot be restarted. A new thread has to be created.
            this._displayThread = new Thread(this._displayRunnable);
            this._displayThread.start();
        }

    }

    @Override
    protected void onStop() {
        //Log.e("Test", "running onStop...");
        super.onStop();
        /*if (ARClient != null) {
            ARClient.removeActivityUpdates(pIntent);
            ARClient.disconnect();
        }
        this._runRandomForest = false;
        this._extractFeatures = false;
        unregisterReceiver(receiver);*/
    }

    @Override
    protected void onDestroy() {

        Log.e("Test", "running onDestroy...");

        super.onDestroy();

        this._runRandomForest = false;
        this._extractFeatures = false;

        if (ARClient != null) {
            ARClient.removeActivityUpdates(pIntent);
            ARClient.disconnect();
        }

        unregisterReceiver(receiver);

        sensors.unregisterListener(this, accelerometer);
        sensors.unregisterListener(this, gyroscope);

    }

    @Override
    protected void onPause() {
        //Log.e("Test", "running onPause...");
        super.onPause();
    }

    @Override
    public void onConnectionFailed(ConnectionResult arg0) {
        Toast.makeText(this, "Connection Failed", Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onDisconnected() {
        Log.e("Test", "running onDisconnected...");
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        Sensor sensor = event.sensor;

        int num_axes = event.values.length;

        final double[] values = new double[num_axes];

        for (int i = 0; i < num_axes; i++)
            values[i] = (double) event.values[i]; // values.length = 3: X, Y, Z

        final long timestamp = event.timestamp;

        try {
            switch (sensor.getType()) {
                case Sensor.TYPE_ACCELEROMETER:
                    if (this._hasAccelerometer) {
                        synchronized (this._accelerometerClip) {
                            this._accelerometerClip.appendValues(values, timestamp);
                        }
                        if (_writeSensorValues) {
                            String[] _accelerometer_towrite_row = new String[num_axes + 1];
                            _accelerometer_towrite_row[0] = String.valueOf((timestamp/1.0e9+this._uptime_unix_sec));
                            for (int i = 0; i < num_axes; i++)
                                _accelerometer_towrite_row[i + 1] = String.valueOf(values[i]);
                            synchronized (this._accelerometer_towrite) {
                                _accelerometer_towrite.add(_accelerometer_towrite_row);
                            }
                        }
                    }

                    break;
                case Sensor.TYPE_GYROSCOPE:
                    if (this._hasGyroscope) {
                        synchronized (this._gyroscopeClip) {
                            this._gyroscopeClip.appendValues(values, timestamp);
                        }
                        if (_writeSensorValues) {
                            String[] _gyroscope_towrite_row = new String[num_axes + 1];
                            _gyroscope_towrite_row[0] = String.valueOf((timestamp/1.0e9+this._uptime_unix_sec));
                            for (int i = 0; i < num_axes; i++)
                                _gyroscope_towrite_row[i + 1] = String.valueOf(values[i]);
                            synchronized (this._gyroscope_towrite) {
                                _gyroscope_towrite.add(_gyroscope_towrite_row);
                            }
                        }
                    }

                    break;
                /*case Sensor.TYPE_PRESSURE:
                    if (this._hasBarometer) {
                        synchronized (this._barometerClip) {
                            this._barometerClip.appendValues(values, timestamp);
                        }
                    }
                    break;*/
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    public void onAccuracyChanged(Sensor arg0, int arg1) {
        // TODO Auto-generated method stub
    }

    public void transmitAnalysis(String cls, String confidence) {

        Bundle data = new Bundle();
        data.putString("APP", "ACTIVITY RECOGNITION");
        data.putLong("TIMESTAMP", System.currentTimeMillis() / 1000);

        if (cls.equals("null"))
            data.putString("STATE", "Unknown");
        else {
            int cls_int = Integer.parseInt(cls);
            if (cls_int==1)
                data.putString("STATE", "Sitting");
            else if (cls_int==2)
                data.putString("STATE", "Walking");
        }
        data.putString("CONFIDENCE", confidence);

        Intent broadcast = new Intent(ActivityRecognition.NEW_DATA);
        broadcast.putExtra(ActivityRecognition.DATA, data);

        this.sendBroadcast(broadcast);

    }

    public void onPowerOff(View view) {

        if (ARClient != null) {
            ARClient.removeActivityUpdates(pIntent);
            ARClient.disconnect();
        }

        this._runRandomForest = false;
        this._extractFeatures = false;

        unregisterReceiver(receiver);

    }

    public void onStartTrial(View view) {

        final Button butStart = (Button) view;
        butStart.setEnabled(false);
        butStart.setTextColor(Color.parseColor("#000000"));

        final Button butStop = (Button) findViewById(R.id.bStop);
        final RadioButton radWalk = (RadioButton) findViewById(R.id.Walk);
        final RadioButton radSit = (RadioButton) findViewById(R.id.Sit);
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                //butTrialType.setEnabled(false);
                butStop.setEnabled(true);
                butStop.setTextColor(Color.parseColor("#FF0000"));
                radWalk.setEnabled(false);
                radSit.setEnabled(false);
            }
        }); // since this view is not created by the current thread, we need to explicitly run it on the UI thread

        _RFClass_list.clear();

        _writeSensorValues = true;
        _trial_started = true;

        if (context!=null) {
            Toast toast = Toast.makeText(context, "Start the Activity!", Toast.LENGTH_SHORT);
            toast.show();
        }

        _t_trial_start = System.currentTimeMillis();

    }

    private void onEndTrial(boolean success) {

        String result;
        if (success) {
            AudioManager audioManager = (AudioManager)getSystemService(Context.AUDIO_SERVICE);
            audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, 20, 0);
            MediaPlayer mp = MediaPlayer.create(this, R.raw.applause);
            mp.start();
            result = "success";
            try {
                Thread.sleep(3000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        } else {
            tg.startTone(ToneGenerator.TONE_CDMA_ALERT_CALL_GUARD, 4000);
            result = "failure";
        }

        _writeSensorValues = false;
        _trial_started = false;

        String filename_accelerometer = "acc.csv";
        String filename_gyroscope = "gyr.csv";
        String filename_result = "result.txt";

        File file_accelerometer, file_gyroscope, file_result;
        file_accelerometer = new File(csv_dir, filename_accelerometer);
        file_gyroscope = new File(csv_dir, filename_gyroscope);
        file_result = new File(csv_dir, filename_result);

        CSVWriter writer_accelerometer = null;
        CSVWriter writer_gyroscope = null;
        try {
            FileWriter writer_result = new FileWriter(file_result);
            writer_result.append(result);
            writer_result.flush();
            writer_result.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

        try {
            writer_accelerometer = new CSVWriter(new FileWriter(file_accelerometer), '\t');
            writer_gyroscope = new CSVWriter(new FileWriter(file_gyroscope), '\t');
        } catch (IOException e) {
            e.printStackTrace();
        }

        if (writer_accelerometer==null || writer_gyroscope==null) {
            Log.e("FTC","Error: Cannot write to files in directory " + csv_dir.toString());
        } else {

            synchronized (_accelerometer_towrite) {
                writer_accelerometer.writeAll(_accelerometer_towrite, false);
                _accelerometer_towrite.clear();
            }

            synchronized (_gyroscope_towrite) {
                writer_gyroscope.writeAll(_gyroscope_towrite, false);
                _gyroscope_towrite.clear();
            }

            try {
                writer_accelerometer.close();
                writer_gyroscope.close();
                Log.i("INFO","CSV files containing feature and sensor values written successfully." );
            } catch (IOException e) {
                e.printStackTrace();
            }

        }

        final Button butStart = (Button) findViewById(R.id.bStart);
        final Button butStop = (Button) findViewById(R.id.bStop);

        final RadioButton radWalk = (RadioButton) findViewById(R.id.Walk);
        final RadioButton radSit = (RadioButton) findViewById(R.id.Sit);
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                butStart.setEnabled(true);
                butStart.setTextColor(Color.parseColor("#00FF00"));
                butStop.setEnabled(false);
                butStop.setTextColor(Color.parseColor("#000000"));
                radWalk.setEnabled(true);
                radSit.setEnabled(true);
            }
         }); // since these views are not created by the current thread, we need to explicitly run them on the UI thread

    }

    public void onRadioButtonClicked(View view) {
        // Is the button now checked?
        boolean checked = ((RadioButton) view).isChecked();

        // Check which radio button was clicked
        switch(view.getId()) {
            case R.id.Walk:
                if (checked)
                    _trialType = TrialType.WALK;
                    break;
            case R.id.Sit:
                if (checked)
                    _trialType = TrialType.SIT;
                    break;
        }
    }

    public void onStopTrial(View view) {

        onEndTrial(false);

    }

}