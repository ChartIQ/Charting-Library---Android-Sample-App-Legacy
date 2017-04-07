package com.chartiq.chartiqsample;

import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.widget.EditText;
import android.widget.Switch;
import android.widget.TextView;

import com.chartiq.sdk.ChartIQ;

public class ChartOptions extends AppCompatActivity {

    Toolbar toolbar;
    TextView chartStyle;
    Switch logScale;
    private int secretCounter = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().requestFeature(Window.FEATURE_ACTION_BAR);
        setContentView(R.layout.activity_chart_options);
        toolbar = (Toolbar) findViewById(R.id.toolbar);
        chartStyle = (TextView) findViewById(R.id.chart_style);
        logScale = (Switch) findViewById(R.id.switch_log);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowHomeEnabled(true);

        if (getIntent().getStringExtra("chartStyle") != null) {
            chartStyle.setText(getIntent().getStringExtra("chartStyle"));
        } else {
            chartStyle.setText("Candle");
        }
        logScale.setChecked(getIntent().getBooleanExtra("logScale", false));
    }


    public void applyChanges(View view) {
        Intent result = new Intent();
        result.putExtra("chartStyle", chartStyle.getText().toString());
        result.putExtra("logScale", logScale.isChecked());
        setResult(RESULT_OK, result);
        finish();
    }

    public void resetToDefaults(View view) {
        chartStyle.setText("Candle");
        logScale.setChecked(false);
    }

    public void startChartStyleChooseActivity(View view) {
        Intent chartStyleChooseActivity = new Intent(this, ChartStyleChooseActivity.class);
        chartStyleChooseActivity.putExtra("chartStyle", chartStyle.getText().toString());
        startActivityForResult(chartStyleChooseActivity, 0);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                onBackPressed();
        }
        return true;
    }

    @Override
    public void onBackPressed() {
        setResult(RESULT_CANCELED);
        finish();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (0 == requestCode) {
            if (RESULT_OK == resultCode) {
                chartStyle.setText(data.getStringExtra("chartStyle"));
            }
        } else {
            super.onActivityResult(requestCode, resultCode, data);
        }
    }

    public void openSecretDialog(View view) {
        secretCounter++;
        if (secretCounter == 3) {
            secretCounter = 0;
            final EditText username = new EditText(this);
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle("Set User")
                    .setCancelable(true)
                    .setView(username)
                    .setPositiveButton("Set User", new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {
                            ChartIQ.setUser(username.getText().toString());
                        }
                    });
            AlertDialog alert = builder.create();
            alert.show();
        }
    }
}
