package dk.zdn.sdu.actiloc;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;

import com.google.android.gms.location.ActivityRecognition;
import com.google.android.gms.location.ActivityRecognitionClient;
import com.google.android.gms.location.DetectedActivity;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;

import java.util.ArrayList;

public class MainActivity extends AppCompatActivity implements SharedPreferences.OnSharedPreferenceChangeListener {

    private Context _context;
    public static final String DETECTED_ACTIVITY = ".DETECTED_ACTIVITY";

    private ActivityRecognitionClient _activityRecognitionClient;
    private ActivitiesAdapter _activitiesAdapter;

    //GUI controls
    private ListView _activitiesListview;
    private Button _activityMonitoringButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        _context = this;
        SetupViews();
        SetupDetectedActivities();

    }

    private void SetupDetectedActivities() {
        ArrayList<DetectedActivity> detectedActivities = ActivityMonitorIntentService.detectedActivitiesFromJson(
                PreferenceManager.getDefaultSharedPreferences(this).getString(
                        DETECTED_ACTIVITY, ""));

        _activitiesAdapter = new ActivitiesAdapter(this, detectedActivities);
        _activitiesListview.setAdapter(_activitiesAdapter);
        _activityRecognitionClient = ActivityRecognition.getClient(this);
    }

    //Process the list of activities//
    protected void updateDetectedActivitiesList() {
        ArrayList<DetectedActivity> detectedActivities = ActivityMonitorIntentService.detectedActivitiesFromJson(
                PreferenceManager.getDefaultSharedPreferences(_context)
                        .getString(DETECTED_ACTIVITY, ""));

        _activitiesAdapter.updateActivities(detectedActivities);
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String s) {
        if (s.equals(DETECTED_ACTIVITY)) {
            updateDetectedActivitiesList();
        }
    }


    private void SetupViews() {
        _activityMonitoringButton = findViewById(R.id.activityMonitoringButton);
        _activitiesListview = findViewById(R.id.activitiesListview);
    }


    @Override
    protected void onResume() {
        super.onResume();
        PreferenceManager.getDefaultSharedPreferences(this)
                .registerOnSharedPreferenceChangeListener(this);
        updateDetectedActivitiesList();
    }

    @Override
    protected void onPause() {
        PreferenceManager.getDefaultSharedPreferences(this)
                .unregisterOnSharedPreferenceChangeListener(this);
        super.onPause();
    }

    public void requestUpdatesHandler(View view) {
        Task<Void> task = _activityRecognitionClient.requestActivityUpdates(
                3000,
                getActivityDetectionPendingIntent());
        task.addOnSuccessListener(new OnSuccessListener<Void>() {
            @Override
            public void onSuccess(Void result) {
                updateDetectedActivitiesList();
            }
        });

    }

    private PendingIntent getActivityDetectionPendingIntent() {
        Intent intent = new Intent(this, ActivityMonitorIntentService.class);
        return PendingIntent.getService(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);

    }
}
