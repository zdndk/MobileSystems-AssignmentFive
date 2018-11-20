package dk.zdn.sdu.actiloc;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;

public class MainActivity extends AppCompatActivity {

    private TextView _activityTextBox;
    private Button _activityMonitoringButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        SetupViews();
    }

    private void SetupViews() {
        _activityMonitoringButton = findViewById(R.id.activityMonitoringButton);
        _activityTextBox = findViewById(R.id.activityTextBox);
    }
}
