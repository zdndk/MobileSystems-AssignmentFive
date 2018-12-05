package dk.zdn.sdu.actiloc;

import android.Manifest;
import android.app.Activity;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;

import com.google.android.gms.location.ActivityRecognition;
import com.google.android.gms.location.ActivityRecognitionClient;
import com.google.android.gms.location.DetectedActivity;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Calendar;

public class MainActivity extends AppCompatActivity implements SharedPreferences.OnSharedPreferenceChangeListener {

    private Context _context;
    public static final String DETECTED_ACTIVITY = ".DETECTED_ACTIVITY";
    public static final String MOST_PROPABLE_ACTIVITY = ".MOST_PROPABLE_ACTIVITY";

    private ActivityRecognitionClient _activityRecognitionClient;
    private ActivitiesAdapter _activitiesAdapter;
    private File _activitySavingFile;

    //GUI controls
    private ListView _activitiesListview;
    private Button _activityMonitoringButton;
    private TextView _errorTextView;

    private static final int REQUEST_EXTERNAL_STORAGE = 1;
    private static String[] PERMISSIONS_STORAGE = {
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        verifyStoragePermissions(this);
        _activitySavingFile = GetFileForSavingActivityData("Activities.log");
        _context = this;
        setContentView(R.layout.activity_main);
        SetupViews();
        SetupDetectedActivities();
    }

    public File GetFileForSavingActivityData(String fileName) {
        File file = new File(Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_DOCUMENTS), fileName);
        return file;
    }

    public static void verifyStoragePermissions(Activity activity) {
        // Check if we have write permission
        int permission = ActivityCompat.checkSelfPermission(activity, Manifest.permission.WRITE_EXTERNAL_STORAGE);

        if (permission != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(
                    activity,
                    PERMISSIONS_STORAGE,
                    REQUEST_EXTERNAL_STORAGE
            );
        }
    }

    private void SetupDetectedActivities() {
        ArrayList<DetectedActivity> detectedActivities = ActivityMonitorIntentService.detectedActivitiesFromJson(
                PreferenceManager.getDefaultSharedPreferences(this).getString(
                        DETECTED_ACTIVITY, ""));

        _activitiesAdapter = new ActivitiesAdapter(this, detectedActivities);
        _activitiesListview.setAdapter(_activitiesAdapter);
        _activityRecognitionClient = ActivityRecognition.getClient(this);
    }

       protected void updateDetectedActivitiesList() {
        ArrayList<DetectedActivity> detectedActivities = ActivityMonitorIntentService.detectedActivitiesFromJson(
                PreferenceManager.getDefaultSharedPreferences(_context)
                        .getString(DETECTED_ACTIVITY, ""));


        String mostProbableActivityString = PreferenceManager.getDefaultSharedPreferences(_context).getString(MOST_PROPABLE_ACTIVITY, "");
        try(FileWriter fw = new FileWriter(_activitySavingFile, true))
        {
            fw.write(Calendar.getInstance().getTime() + "|" + mostProbableActivityString + "\n");
            fw.flush();
        } catch (IOException ioe) {
            _errorTextView.setText(ioe.getMessage());
        }
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
        _errorTextView = findViewById(R.id.errorTextView);
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
