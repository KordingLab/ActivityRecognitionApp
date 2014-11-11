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
import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesClient;
import com.google.android.gms.location.ActivityRecognitionClient;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;

import edu.northwestern.sohrob.activityrecognition.activityrecognition.FeatureExtractor.Feature;
import edu.northwestern.sohrob.activityrecognition.activityrecognition.trees.LeafNode;


public class ActivityRecognition extends Activity implements GooglePlayServicesClient.ConnectionCallbacks,GooglePlayServicesClient.OnConnectionFailedListener,SensorEventListener {

    private ActivityRecognitionClient ARClient;
    private PendingIntent pIntent;

    private ToneGenerator tg;

    TextView sText;

    RandomForest RF;


    private BroadcastReceiver receiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String v = intent.getStringExtra("Activity") + "\n" + intent.getExtras().getInt("Confidence") + "%";
            TextView gText = (TextView) findViewById(R.id.gText);
            gText.setText(v);
            Log.i("Test", intent.getStringExtra("Activity") + "-" + intent.getExtras().getInt("Confidence"));
            if (intent.getStringExtra("Activity").equals("walking"))
                tg.startTone(ToneGenerator.TONE_CDMA_ALERT_CALL_GUARD, 200);
        }

    };

    private SensorManager sensors;
    private Sensor accelerometer, gyroscope;

    private Clip _accelerometerClip = null;
    private Clip _gyroscopeClip = null;
    //private Clip _barometerClip = null;

    private boolean _hasAccelerometer = false;
    private boolean _hasGyroscope = false;
    //private boolean _hasBarometer = false;

    private boolean _extractFeatures = false;
    private boolean _runRandomForest = false;

    private FeatureExtractor _accelerometerExtractor = null;
    private FeatureExtractor _gyroscopeExtractor = null;
    private FeatureExtractor _barometerExtractor = null;

    private final HashMap<String, Double> _features = new HashMap<String, Double>();

    private long last_timestamp_acc = 0;
    private long last_timestamp_gyr = 0;
    //private long last_timestamp_bar = 0;

    private static final long WINDOW_SIZE = (long) 4e9; // sensor timestamps are in nanoseconds
    private static final long WINDOW_SHIFT = (long) 3e9; // sensor timestamps are in nanoseconds

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
                    Feature.ACCX_FFT1, Feature.ACCX_FFT2, Feature.ACCX_FFT3, Feature.ACCX_FFT4, Feature.ACCX_FFT5, Feature.ACCX_FFT6, Feature.ACCX_FFT7, Feature.ACCX_FFT8, Feature.ACCX_FFT9, Feature.ACCX_FFT10,
                    Feature.ACCY_FFT1, Feature.ACCY_FFT2, Feature.ACCY_FFT3, Feature.ACCY_FFT4, Feature.ACCY_FFT5, Feature.ACCY_FFT6, Feature.ACCY_FFT7, Feature.ACCY_FFT8, Feature.ACCY_FFT9, Feature.ACCY_FFT10,
                    Feature.ACCZ_FFT1, Feature.ACCZ_FFT2, Feature.ACCZ_FFT3, Feature.ACCZ_FFT4, Feature.ACCZ_FFT5, Feature.ACCZ_FFT6, Feature.ACCZ_FFT7, Feature.ACCZ_FFT8, Feature.ACCZ_FFT9, Feature.ACCZ_FFT10,
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
                    Feature.GYRX_FFT1, Feature.GYRX_FFT2, Feature.GYRX_FFT3, Feature.GYRX_FFT4, Feature.GYRX_FFT5, Feature.GYRX_FFT6, Feature.GYRX_FFT7, Feature.GYRX_FFT8, Feature.GYRX_FFT9, Feature.GYRX_FFT10,
                    Feature.GYRY_FFT1, Feature.GYRY_FFT2, Feature.GYRY_FFT3, Feature.GYRY_FFT4, Feature.GYRY_FFT5, Feature.GYRY_FFT6, Feature.GYRY_FFT7, Feature.GYRY_FFT8, Feature.GYRY_FFT9, Feature.GYRY_FFT10,
                    Feature.GYRZ_FFT1, Feature.GYRZ_FFT2, Feature.GYRZ_FFT3, Feature.GYRZ_FFT4, Feature.GYRZ_FFT5, Feature.GYRZ_FFT6, Feature.GYRZ_FFT7, Feature.GYRZ_FFT8, Feature.GYRZ_FFT9, Feature.GYRZ_FFT10,
                    Feature.GYRX_HIST1, Feature.GYRX_HIST2, Feature.GYRX_HIST3, Feature.GYRX_HIST4, Feature.GYRX_HIST5, Feature.GYRX_HIST6,
                    Feature.GYRY_HIST1, Feature.GYRY_HIST2, Feature.GYRY_HIST3, Feature.GYRY_HIST4, Feature.GYRY_HIST5, Feature.GYRY_HIST6,
                    Feature.GYRZ_HIST1, Feature.GYRZ_HIST2, Feature.GYRZ_HIST3, Feature.GYRZ_HIST4, Feature.GYRZ_HIST5, Feature.GYRZ_HIST6
            };

    private HashMap<String, String> _featureMap = new HashMap<String, String>();

    private Thread _featureThread = null;

    private Thread _randomForestThread = null;

    private Runnable _featureRunnable = new Runnable() {
        public void run() {
            while (_extractFeatures) {
                long now = System.currentTimeMillis();

                boolean generateTone = false;

                // checking if the clip has moved since last time -- accelerometer
                if (_accelerometerClip.getTimestamps().size() > 0) {
                    if (_accelerometerClip.getLastTimestamp() == last_timestamp_acc) {
                        Log.e("PR", "P20FeaturesProbe: Clip hasn't moved since last feature extraction!");
                        generateTone = true;
                    } else {
                        last_timestamp_acc = _accelerometerClip.getLastTimestamp();
                        Log.e("PR", "P20FeaturesProbe: n_samp (acc) = " + _accelerometerClip.getTimestamps().size());
                    }
                }

                // checking if the clip has moved since last time -- gyroscope
                if (_gyroscopeClip.getTimestamps().size() > 0) {
                    if (_gyroscopeClip.getLastTimestamp() == last_timestamp_gyr) {
                        Log.e("PR", "P20FeaturesProbe: Clip hasn't moved since last feature extraction!");
                        generateTone = true;
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
                            generateTone = true;
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
                            generateTone = true;
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

                //transmit feature values and time stamps
                //transmitAnalysis();

                try {
                    if (generateTone) {
                        // creating a tone generator
                        // the second argument is the volume (0-100)
                        ToneGenerator toneGenerator = new ToneGenerator(AudioManager.STREAM_ALARM, 50);

                        toneGenerator.startTone(ToneGenerator.TONE_CDMA_ALERT_CALL_GUARD, 200);
                    }

                    //measuring the processing time
                    long deltaT = System.currentTimeMillis() - now;

                    synchronized (_features) {
                        _features.put(Feature.PROCESSING_TIME.toString(), (double) deltaT);
                    }

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

    private Runnable _randomForestRunnable = new Runnable() {
        public void run() {
            while (_runRandomForest) {

                final HashMap<String, Object> _prediction;

                synchronized (_features) {
                    HashMap<String, Double> _featureValuesConverted = convertFeatureLabels(_features);
                    _prediction = RF.evaluateModel(_featureValuesConverted);
                    //Log.i("info", "CLASS: " + _prediction.get(LeafNode.PREDICTION));
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
                                sCls = "Walking";
                                break;
                            case 2:
                                sCls = "Sitting";
                                break;
                        }
                        String v = "" + sCls + "\n" + _prediction.get(LeafNode.ACCURACY);
                        sText = (TextView) findViewById(R.id.sText);
                        sText.setText(v);
                    }
                });

                transmitAnalysis(""+_prediction.get(LeafNode.PREDICTION), ""+_prediction.get(LeafNode.ACCURACY));

                try {
                    Thread.sleep((long)(WINDOW_SHIFT/1e6));
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

            }
        }
    };


    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_activity_recognition);

        tg = new ToneGenerator(AudioManager.STREAM_ALARM, 50);

        // registering the sensors (only accelerometer and gyroscope at the moment)
        sensors = (SensorManager) getSystemService(SENSOR_SERVICE);

        accelerometer = sensors.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        gyroscope = sensors.getDefaultSensor(Sensor.TYPE_GYROSCOPE);

        sensors.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_GAME, null);
        sensors.registerListener(this, gyroscope, SensorManager.SENSOR_DELAY_GAME, null);

        // initializing clips
        this._accelerometerClip = new Clip(3, WINDOW_SIZE, Clip.ACCELEROMETER);
        this._gyroscopeClip = new Clip(3, WINDOW_SIZE, Clip.GYROSCOPE);
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

            StringBuffer sb = new StringBuffer();

            String inputLine;

            while ((inputLine = in.readLine()) != null)
                sb.append(inputLine);

            in.close();

            String contents = sb.toString();

            if (contents != null)
            {
                JSONObject json = null;
                RF = new RandomForest();

                try {

                    json = new JSONObject(contents);

                    Log.i("info","now generating the forest model...");
                    RF.generateModel(json.get("model"));

                    if (json.has("map")) {
                        setFeatureMap(json.getJSONObject("map"));
                        Log.i("info","Features map set!");
                    }

                } catch (JSONException e) {
                    e.printStackTrace();
                }

            }


        } catch (IOException e) {
            e.printStackTrace();
        }

        Log.e("Test", "OnCreate Finished!");

    }

    @Override
    protected void onStart() {
        Log.e("Test", "running onStart...");
        super.onStart();

        //checking if Google Play Service is installed - skip it for now since we are sure that it is installed on the phone
/*
        int resp =GooglePlayServicesUtil.isGooglePlayServicesAvailable(this);
        if(resp == ConnectionResult.SUCCESS){
            arclient = new ActivityRecognitionClient(this, this, this);
            arclient.connect();
        }
        else{
            Toast.makeText(this, "Please install Google Play Service.", Toast.LENGTH_SHORT).show();
        }
*/

        ARClient = new ActivityRecognitionClient(this, this, this);
        ARClient.connect();

        IntentFilter filter = new IntentFilter();
        filter.addAction("edu.northwestern.sohrob.myactivityrecognition.ACTIVITY_RECOGNITION_DATA");
        registerReceiver(receiver, filter);

        // staring the feature extraction thread
        this._extractFeatures = true;
        if (this._featureThread == null || this._featureThread.isAlive() == false)
        {
            // A dead thread cannot be restarted. A new thread has to be created.
            this._featureThread = new Thread(this._featureRunnable);
            this._featureThread.start();
        }

        // staring the random forest thread
        this._runRandomForest = true;
        if (this._randomForestThread == null || this._randomForestThread.isAlive() == false)
        {
            // A dead thread cannot be restarted. A new thread has to be created.
            this._randomForestThread = new Thread(this._randomForestRunnable);
            this._randomForestThread.start();
        }


    }

    @Override
    protected void onStop() {
        Log.e("Test", "running onStop...");
        super.onStop();
        if (ARClient != null) {
            ARClient.removeActivityUpdates(pIntent);
            ARClient.disconnect();
        }
        unregisterReceiver(receiver);
    }

    @Override
    protected void onDestroy() {
        Log.e("Test", "running onDestroy...");
        super.onDestroy();
        if (ARClient != null) {
            ARClient.removeActivityUpdates(pIntent);
            ARClient.disconnect();
        }
        unregisterReceiver(receiver);

        sensors.unregisterListener(this, accelerometer);
        sensors.unregisterListener(this, gyroscope);

        this._extractFeatures = false;

    }

    @Override
    protected void onPause() {
        Log.e("Test", "running onPause...");
        super.onPause();
    }

    @Override
    public void onConnectionFailed(ConnectionResult arg0) {
        Toast.makeText(this, "Connection Failed", Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onConnected(Bundle arg0) {
        Log.e("Test", "running onConnected...");
        Intent intent = new Intent(this, ActivityRecognitionService.class);
        pIntent = PendingIntent.getService(this, 0, intent, PendingIntent.FLAG_CANCEL_CURRENT);
        ARClient.requestActivityUpdates(0, pIntent);
    }

    @Override
    public void onDisconnected() {
        Log.e("Test", "running onDisconnected...");
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        Sensor sensor = event.sensor;

        final double[] values = new double[event.values.length];

        for (int i = 0; i < event.values.length; i++)
            values[i] = (double) event.values[i]; // values.length = 3: X, Y, Z

        final long timestamp = event.timestamp;

        try {
            switch (sensor.getType()) {
                case Sensor.TYPE_ACCELEROMETER:
                    if (this._hasAccelerometer) {
                        synchronized (this._accelerometerClip) {
                            this._accelerometerClip.appendValues(values, timestamp);
                        }
                    }

                    break;
                case Sensor.TYPE_GYROSCOPE:
                    if (this._hasGyroscope) {
                        synchronized (this._gyroscopeClip) {
                            this._gyroscopeClip.appendValues(values, timestamp);
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

        Bundle bundle = new Bundle();
        bundle.putString("APP", "ACTIVITY RECOGNITION");
        bundle.putLong("TIMESTAMP", System.currentTimeMillis() / 1000);
        bundle.putString("CLASS", cls);
        bundle.putString("CONFIDENCE", confidence);

        Log.e("INF", "Output Transmitted.");

    }

    private void setFeatureMap(JSONObject mapJson)
    {
        Iterator<String> keys = mapJson.keys();

        while (keys.hasNext())
        {
            try
            {
                String original = keys.next();
                String replacement = mapJson.get(original).toString();

                this._featureMap.put(original, replacement);
            }
            catch (JSONException e)
            {
                e.printStackTrace();
            }
        }
    }

    private HashMap<String, Double> convertFeatureLabels(HashMap<String, Double> snapshot) {

        HashMap<String, Double> snapshot_out = new HashMap<String, Double>();

        for (String key : _featureMap.keySet())
        {
            String newKey = this._featureMap.get(key);

            if (snapshot.get(key) == null)
            {
                // Do nothing.
            }
            else
            {
                snapshot_out.put(newKey, snapshot.get(key));
            }
        }

        return snapshot_out;

    }

}