package edu.northwestern.sohrob.activityrecognition.activityrecognition;

import android.app.IntentService;
import android.content.Intent;

import com.google.android.gms.location.ActivityRecognitionResult;
import com.google.android.gms.location.DetectedActivity;

import java.util.List;

public class ActivityRecognitionService extends IntentService  {

    //private String TAG = this.getClass().getSimpleName();
    public ActivityRecognitionService() {
        super("My Activity Recognition Service");
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        if (ActivityRecognitionResult.hasResult(intent)){
            ActivityRecognitionResult result = ActivityRecognitionResult.extractResult(intent);
            Intent i = new Intent("edu.northwestern.sohrob.myactivityrecognition.ACTIVITY_RECOGNITION_DATA");
            i.putExtra("Activity", getType(result.getProbableActivities()));
            i.putExtra("Confidence", result.getMostProbableActivity().getConfidence());
            i.putExtra("Timestamp", result.getTime());
            sendBroadcast(i);
        }
    }

    private String getType(List<DetectedActivity> activities){

        if (activities.get(0).getType()==DetectedActivity.UNKNOWN)
            return "unknown";
        else if (activities.get(0).getType()==DetectedActivity.IN_VEHICLE)
            return "in vehicle";
        else if (activities.get(0).getType()==DetectedActivity.ON_BICYCLE)
            return "on bicycle";
        else if (activities.get(0).getType()==DetectedActivity.STILL)
            return "still";
        else if (activities.get(0).getType()==DetectedActivity.TILTING)
            return "tilting";
        else if (activities.get(0).getType()==DetectedActivity.ON_FOOT) {
            switch (activities.get(1).getType()) {
                case DetectedActivity.WALKING:
                    return "walking";
                case DetectedActivity.RUNNING:
                    return "running";
                default:
                    return "on foot";
            }
        }

        return "";

    }

}

