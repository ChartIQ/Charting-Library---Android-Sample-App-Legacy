package com.chartiq.chartiqsample.studies;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import com.chartiq.chartiqsample.R;

import java.util.HashMap;

public class StudySelectOptionActivity extends AppCompatActivity {

    ListView listView;
    Toolbar toolbar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().requestFeature(Window.FEATURE_ACTION_BAR);
        setContentView(R.layout.activity_chart_style_choose);
        toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowHomeEnabled(true);
        listView = (ListView) findViewById(R.id.listview);

        final ArrayAdapter<CharSequence> adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1);
        if (getIntent().hasExtra("parameter")) {
            StudyParameter parameter = (StudyParameter) getIntent().getSerializableExtra("parameter");
            if (parameter.options != null) {
                for (HashMap.Entry<String, Object> entry : parameter.options.entrySet()) {
                    adapter.add(String.valueOf(entry.getValue()));
                }
            }
        }

        listView.setAdapter(adapter);
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Intent result = new Intent();
                result.putExtra("chosenValue", String.valueOf(adapter.getItem(position)));
                result.putExtra("parameter", getIntent().getSerializableExtra("parameter"));
                setResult(RESULT_OK, result);
                finish();
            }
        });
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
}
