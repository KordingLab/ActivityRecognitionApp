package edu.northwestern.sohrob.activityrecognition.activityrecognition;

import android.app.Activity;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.AssetManager;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.media.AudioManager;
import android.media.ToneGenerator;
import android.os.Bundle;
import android.os.SystemClock;
import android.util.Log;
import android.view.View;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesClient;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.location.ActivityRecognitionClient;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import au.com.bytecode.opencsv.CSVWriter;
import edu.northwestern.sohrob.activityrecognition.activityrecognition.FeatureExtractor.Feature;
import edu.northwestern.sohrob.activityrecognition.activityrecognition.trees.LeafNode;


public class ActivityRecognition extends Activity implements GooglePlayServicesClient.ConnectionCallbacks,GooglePlayServicesClient.OnConnectionFailedListener,SensorEventListener {

    private Context context;

    private ActivityRecognitionClient ARClient;
    private PendingIntent pIntent;

    private static final String NEW_DATA = "edu.northwestern.sohrob.activityrecognition.activityrecognition.NEW_DATA";
    private static final String DATA = "DATA";

    private ToneGenerator tg;

    TextView sText;

    RandomForest RF;

    private String[] _feature_labels;

    private double _uptime_unix_sec;


    private BroadcastReceiver receiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String v = intent.getStringExtra("Activity") + " (" + intent.getExtras().getInt("Confidence") + "%)";
            TextView gText = (TextView) findViewById(R.id.gText);
            gText.setText(v);
            Log.i("Test", intent.getStringExtra("Activity") + "-" + intent.getExtras().getInt("Confidence"));
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

    private boolean _writeFeatures = false;
    private boolean _writeSensorValues = false;

    private FeatureExtractor _accelerometerExtractor = null;
    private FeatureExtractor _gyroscopeExtractor = null;
    //private FeatureExtractor _barometerExtractor = null;

    private final HashMap<String, Double> _features = new HashMap<String, Double>();

    private final List<String[]> _features_towrite = new ArrayList<String[]>();
    private final List<String[]> _accelerometer_towrite = new ArrayList<String[]>();
    private final List<String[]> _gyroscope_towrite = new ArrayList<String[]>();

    private long last_timestamp_acc = 0;
    private long last_timestamp_gyr = 0;
    //private long last_timestamp_bar = 0;

    private static final long WINDOW_SIZE = (long) 4e9; // sensor timestamps are in nanoseconds
    private static final long WINDOW_SHIFT = (long) 1e9; // sensor timestamps are in nanoseconds

    private final int f_interp = 50;    // (Hz) sampling frequency of interpolation prior to feature extraction

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
                    Feature.GYRZ_HIST1, Feature.GYRZ_HIST2, Feature.GYRZ_HIST3, Feature.GYRZ_HIST4, Feature.GYRZ_HIST5, Feature.GYRZ_HIST6
            };

    private Thread _featureThread = null;

    private Thread _randomForestThread = null;

    private Runnable _featureRunnable;

    {
        _featureRunnable = new Runnable() {
            public void run() {
                while (_extractFeatures) {

                    long now = System.currentTimeMillis();

                    boolean generateTone = false;

                    // checking if the clip has moved since last time -- accelerometer
                    if (_accelerometerClip.getTimestamps().size() > 0) {
                        if (_accelerometerClip.getLastTimestamp() == last_timestamp_acc) {
                            Log.e("PR", "P20FeaturesProbe: Clip hasn't moved since last feature extraction!");
                            //generateTone = true;
                        } else {
                            last_timestamp_acc = _accelerometerClip.getLastTimestamp();
                            Log.e("PR", "P20FeaturesProbe: n_samp (acc) = " + _accelerometerClip.getTimestamps().size());
                        }
                    }

                    // checking if the clip has moved since last time -- gyroscope
                    if (_gyroscopeClip.getTimestamps().size() > 0) {
                        if (_gyroscopeClip.getLastTimestamp() == last_timestamp_gyr) {
                            Log.e("PR", "P20FeaturesProbe: Clip hasn't moved since last feature extraction!");
                            //generateTone = true;
                        } else {
                            last_timestamp_gyr = _gyroscopeClip.getLastTimestamp();
                            Log.e("PR", "P20FeaturesProbe: n_samp (gyr) = " + _gyroscopeClip.getTimestamps().size());
                        }
                    }


                    if (_hasAccelerometer) {
                        synchronized (_accelerometerClip) {
                            synchronized (_features) {
                                _features.putAll(_accelerometerExtractor.extractFeatures(_accelerometerClip, f_interp));

                            }

                            if (_accelerometerClip.getValues().size() < 100) {
                                Log.e("FEATURE EXTRACTION THREAD", "Warning: Low number of acc samples!");
                                //generateTone = true;
                            }
                        }
                    }

                    if (_hasGyroscope) {
                        synchronized (_gyroscopeClip) {
                            synchronized (_features) {
                                _features.putAll(_gyroscopeExtractor.extractFeatures(_gyroscopeClip, f_interp));
                            }

                            if (_gyroscopeClip.getValues().size() < 100) {
                                Log.e("FEATURE EXTRACTION THREAD", "Warning: Low number of gyro samples!");
                                //generateTone = true;
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
                            generateTone = true;
                        }
                    }
                }*/

                    if (_writeFeatures) {
                        synchronized (_features) {
                            String[] _features_towrite_row = new String[_feature_labels.length];
                            for (int i=0; i<_feature_labels.length; i++)
                                _features_towrite_row[i] = _features.get(_feature_labels[i]).toString();
                            synchronized (_features_towrite) {
                                //_features_towrite.add(new String[_feature_labels.length]);
                                //_features_towrite.set(_features_towrite.size() - 1, _features_towrite_row);
                                _features_towrite.add(_features_towrite_row);
                            }
                        }
                    }

                    try {
                        if (generateTone) {
                            // creating a tone generator
                            // the second argument is the volume (0-100)
                            //ToneGenerator toneGenerator = new ToneGenerator(AudioManager.STREAM_ALARM, 50);
                            //toneGenerator.startTone(ToneGenerator.TONE_CDMA_ALERT_CALL_GUARD, 200);
                            tg.startTone(ToneGenerator.TONE_CDMA_ALERT_CALL_GUARD, 200);
                        }

                        //measuring the processing time
                        long deltaT = System.currentTimeMillis() - now;

                        /*synchronized (_features) {
                            _features.put(Feature.PROCESSING_TIME.toString(), (double) deltaT);
                        }*/

                        //accounting for the processing time / also converting from ns to ms

                        long sleepTime = WINDOW_SHIFT / (long) 1e6 - deltaT;

                        //in the rare case that processing time is greater than window shift interval

                        if (sleepTime < 0)
                            sleepTime = 0;

                        Thread.sleep(sleepTime);
                    } catch (Exception e) {
                        _extractFeatures = false;
                        e.printStackTrace();
                    }
                }
            }
        };
    }

    private Runnable _randomForestRunnable = new Runnable() {
        public void run() {
            while (_runRandomForest) {

                long now = System.currentTimeMillis();

                final HashMap<String, Object> _prediction;

                synchronized (_features) {
                    //Log.e("inf", _features+"");
                    _prediction = RF.evaluateModel(_features);
                }

                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        //String v = "n_samp_acc = " + _accelerometerClip.getTimestamps().size() + "\n" + "n_samp_gyr = " + _gyroscopeClip.getTimestamps().size();
                        int iCls;
                        if (_prediction.get(LeafNode.PREDICTION)!=null)
                            iCls = Integer.parseInt(""+_prediction.get(LeafNode.PREDICTION));
                        else
                            iCls = 0;
                        String sCls = "";
                        switch (iCls) {
                            case 1:
                                sCls = "sitting";
                                break;
                            case 2:
                                sCls = "walking";
                                tg.startTone(ToneGenerator.TONE_CDMA_ALERT_CALL_GUARD, 200);
                                break;
                        }
                        String v = sCls + " (" + (int)((Double)_prediction.get(LeafNode.ACCURACY)*100) + "%)";
                        sText = (TextView) findViewById(R.id.sText);
                        sText.setText(v);
                    }
                });

                transmitAnalysis(""+_prediction.get(LeafNode.PREDICTION), ""+_prediction.get(LeafNode.ACCURACY));

                long deltaT = System.currentTimeMillis() - now;

                long sleepTime = WINDOW_SHIFT / (long) 1e6 - deltaT;

                // in case that processing time is greater than window shift interval
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

                    JSONArray feature_labels_json = (JSONArray) json.get("feature_labels");
                    _feature_labels = new String[feature_labels_json.length()];
                    for (int i=0; i<feature_labels_json.length(); i++)
                        _feature_labels[i] = feature_labels_json.getString(i);


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

        // staring the feature extraction thread
        /*this._extractFeatures = true;
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
        }*/

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

        //Log.e("INF", "Output Transmitted.");

    }

    public void onStartRecording(View view) {

        if (!_writeFeatures) {  //start recording
            final ImageButton bRecord = (ImageButton) view;
            bRecord.setBackgroundColor(0xFF00FFFF);

            _writeFeatures = true;
            _writeSensorValues = true;

            if (context!=null) {
                Toast toast = Toast.makeText(context, "Recording started", Toast.LENGTH_SHORT);
                toast.show();
            }

        }
        else {  // stop recording
            final ImageButton bRecord = (ImageButton) view;
            bRecord.setBackgroundColor(0x0000FFFF);

            _writeFeatures = false;
            _writeSensorValues = false;

            String filename_features = "features.csv";
            String filename_accelerometer = "acc.csv";
            String filename_gyroscope = "gyr.csv";

            File file_features, file_accelerometer, file_gyroscope;
            file_features = new File(csv_dir, filename_features);
            file_accelerometer = new File(csv_dir, filename_accelerometer);
            file_gyroscope = new File(csv_dir, filename_gyroscope);

            CSVWriter writer_features = null;
            CSVWriter writer_accelerometer = null;
            CSVWriter writer_gyroscope = null;
            try {
                writer_features = new CSVWriter(new FileWriter(file_features), ' ');
                writer_accelerometer = new CSVWriter(new FileWriter(file_accelerometer), '\t');
                writer_gyroscope = new CSVWriter(new FileWriter(file_gyroscope), '\t');
            } catch (IOException e) {
                e.printStackTrace();
            }

            if (writer_features==null || writer_accelerometer==null || writer_gyroscope==null) {
                Log.e("INFO","Cannot write to files in directory " + csv_dir.toString());
            } else {
                synchronized (_features_towrite) {
                    writer_features.writeAll(_features_towrite, false);
                    _features_towrite.clear();
                }
                synchronized (_accelerometer_towrite) {
                    writer_accelerometer.writeAll(_accelerometer_towrite, false);
                    _accelerometer_towrite.clear();
                }
                synchronized (_gyroscope_towrite) {
                    writer_gyroscope.writeAll(_gyroscope_towrite, false);
                    _gyroscope_towrite.clear();
                }

                try {
                    writer_features.close();
                    writer_accelerometer.close();
                    writer_gyroscope.close();
                    Log.i("INFO","CSV files containing feature and sensor values written successfully." );
                } catch (IOException e) {
                    e.printStackTrace();
                }

            }

            if (context!=null) {
                Toast toast = Toast.makeText(context, "Recording Stopped\nFeatures written to local storage", Toast.LENGTH_SHORT);
                toast.show();
            }

        }


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

}