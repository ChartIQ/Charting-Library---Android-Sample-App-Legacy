package com.chartiq.chartiqsample.studies;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.PopupWindow;
import android.widget.Switch;
import android.widget.TextView;

import com.chartiq.chartiq.model.Study;
import com.chartiq.chartiqsample.ColorAdapter;
import com.chartiq.chartiqsample.R;
import com.google.gson.Gson;

import java.util.HashMap;
import java.util.Map;

public class StudyOptionsActivity extends AppCompatActivity {

    private static final int RESULT_STUDY_REMOVED = 4;

    TextView studyTitle;
    Toolbar toolbar;
    Study study;
    HashMap<String, Object> defaultInputs = new HashMap<>();
    HashMap<String, Object> defaultOutputs = new HashMap<>();
    LinearLayout optionsLayout;
    private PopupWindow colorPalette;
    private RecyclerView colorRecycler;
    private TextView currentColorView;
    private String colorOptionName;
    private StudyParameter[] inputs;
    private StudyParameter[] outputs;
    private TextView selectView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_study_options);
        toolbar = (Toolbar) findViewById(R.id.toolbar);
        toolbar.setTitleTextColor(Color.WHITE);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowHomeEnabled(true);

        studyTitle = (TextView) findViewById(R.id.study_title);
        optionsLayout = (LinearLayout) findViewById(R.id.options);

        if (getIntent().hasExtra("study")) {
            study = (Study) getIntent().getSerializableExtra("study");
            studyTitle.setText(study.name);
            if (study.inputs != null) {
                defaultInputs = new HashMap<>(study.inputs);
            }
            if (study.outputs != null) {
                defaultOutputs = new HashMap<>(study.outputs);
            }
        }

        if (getIntent().hasExtra("inputs")) {
            inputs = new Gson().fromJson(getIntent().getStringExtra("inputs"), StudyParameter[].class);
            if (study.inputs != null) {
                bindStudyOptions(inputs, study.inputs);
            }
        }

        if (getIntent().hasExtra("outputs")) {
            outputs = new Gson().fromJson(getIntent().getStringExtra("outputs"), StudyParameter[].class);
            if (study.outputs != null) {
                bindStudyOptions(outputs, study.outputs);
            }
        }

        colorPalette = new PopupWindow(this);
        colorPalette.setWidth(WindowManager.LayoutParams.WRAP_CONTENT);
        colorPalette.setHeight(WindowManager.LayoutParams.WRAP_CONTENT);
        colorPalette.setContentView(getLayoutInflater().inflate(R.layout.color_palette, null));
        colorRecycler = (RecyclerView) colorPalette.getContentView().findViewById(R.id.recycler);
        colorRecycler.setLayoutManager(new GridLayoutManager(this, 5));
        colorRecycler.setAdapter(new ColorAdapter(this, R.array.colors, new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                setColor(v);
            }
        }));
    }

    private void showColorPalette(View view) {
        if (colorPalette.isShowing() && currentColorView == view) {
            currentColorView = null;
            colorPalette.dismiss();
        } else {
            colorPalette.dismiss();
            currentColorView = (TextView) view;
            int[] coord = {0, 0};
            view.getLocationOnScreen(coord);
            colorPalette.showAtLocation(view, Gravity.NO_GRAVITY, 0, coord[1]);
//            colorPalette.showAtLocation(view, Gravity.CENTER, 0, 0);
        }
    }

    private void setColor(View view) {
        colorPalette.dismiss();
        currentColorView.setBackgroundColor(Color.parseColor(String.valueOf(view.getTag())));
        if (study.outputs != null) {
            study.outputs.put(colorOptionName, String.valueOf(view.getTag()));
        }
    }

    private void bindStudyOptions(StudyParameter[] array, final Map<String, Object> studyParams) {
        for (final StudyParameter parameter : array) {
            if (parameter.color != null) {
                if (studyParams.containsKey(parameter.name)) {
                    parameter.color = String.valueOf(studyParams.get(parameter.name));
                }
                bindColor(parameter);
            } else if (parameter.type != null) {
                switch (parameter.type) {
                    case "select":
                        if (studyParams.containsKey(parameter.name) && !"field".equals(studyParams.get(parameter.name))) {
                            parameter.value = studyParams.get(parameter.name);
                        }
                        bindSelect(parameter);
                        break;
                    case "number":
                        if (studyParams.containsKey(parameter.name)) {
                            parameter.value = studyParams.get(parameter.name);
                        }
                        bindNumber(studyParams, parameter);
                        break;
                    case "text":
                        if (studyParams.containsKey(parameter.name)) {
                            parameter.value = studyParams.get(parameter.name);
                        }
                        bindString(studyParams, parameter);
                        break;
                    case "checkbox":
                        if (studyParams.containsKey(parameter.name)) {
                            parameter.value = studyParams.get(parameter.name);
                        }
                        bindBoolean(studyParams, parameter);
                        break;
                }
            }
        }
    }

    private void bindSelect(final StudyParameter parameter) {
        View v = getLayoutInflater().inflate(R.layout.select_study_option, null);
        optionsLayout.addView(v);
        TextView optionName = (TextView) v.findViewById(R.id.option_name);
        optionName.setText(parameter.heading);
        final TextView textView = (TextView) v.findViewById(R.id.value);
        selectView = textView;
        textView.setText(String.valueOf(parameter.value));
        textView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(StudyOptionsActivity.this, StudySelectOptionActivity.class);
                intent.putExtra("parameter", parameter);
                startActivityForResult(intent, 0);
            }
        });
    }

    private void bindBoolean(final Map<String, Object> studyParams, final StudyParameter parameter) {
        View v = getLayoutInflater().inflate(R.layout.boolean_study_option, null);
        optionsLayout.addView(v);
        TextView optionName = (TextView) v.findViewById(R.id.option_name);
        optionName.setText(parameter.heading);
        final Switch switchView = (Switch) v.findViewById(R.id.value);
        switchView.setChecked(Boolean.valueOf(String.valueOf(parameter.value)));
        switchView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                studyParams.put(parameter.name, switchView.isChecked());
            }
        });
    }

    private void bindString(final Map<String, Object> studyParams, final StudyParameter parameter) {
        View v = getLayoutInflater().inflate(R.layout.edittext_study_option, null);
        optionsLayout.addView(v);
        TextView optionName = (TextView) v.findViewById(R.id.option_name);
        optionName.setText(parameter.heading);
        final EditText editText = (EditText) v.findViewById(R.id.value);
        editText.setText(String.valueOf(parameter.value));
        editText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                studyParams.put(parameter.name, editText.getText().toString());
            }

            @Override
            public void afterTextChanged(Editable s) {

            }
        });
    }

    private void bindColor(final StudyParameter parameter) {
        View v = getLayoutInflater().inflate(R.layout.color_study_option, null);
        optionsLayout.addView(v);
        TextView optionName = (TextView) v.findViewById(R.id.option_name);
        optionName.setText(parameter.heading);
        final TextView color = (TextView) v.findViewById(R.id.value);
        color.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showColorPalette(v);
                colorOptionName = parameter.heading;
            }
        });
        if ("auto".equals(parameter.color)) {
            color.setBackgroundColor(Color.BLACK);
        } else {
            color.setBackgroundColor(Color.parseColor(parameter.color));
        }
    }

    private void bindNumber(final Map<String, Object> studyParams, final StudyParameter parameter) {
        View v = getLayoutInflater().inflate(R.layout.number_study_option, null);
        optionsLayout.addView(v);
        TextView optionName = (TextView) v.findViewById(R.id.option_name);
        optionName.setText(parameter.heading);
        final EditText editText = (EditText) v.findViewById(R.id.value);
        editText.setText(String.valueOf(parameter.value));
        editText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                studyParams.put(parameter.name, editText.getText().toString());
            }

            @Override
            public void afterTextChanged(Editable s) {

            }
        });
    }

    public void applyChanges(View view) {
        Intent result = new Intent();
        result.putExtra("study", study);
        setResult(RESULT_OK, result);
        finish();
    }

    public void resetToDefaults(View view) {
        study.inputs = new HashMap<>(defaultInputs);
        study.outputs = new HashMap<>(defaultOutputs);
        optionsLayout.removeAllViews();
        if (inputs != null) {
            bindStudyOptions(inputs, study.inputs);
        }
        if (outputs != null) {
            bindStudyOptions(outputs, study.outputs);
        }
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

    public void removeStudy(View view) {
        Intent result = new Intent();
        result.putExtra("study", study);
        setResult(RESULT_STUDY_REMOVED, result);
        finish();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == 0 && resultCode == RESULT_OK) {
            if (data.hasExtra("chosenValue") && data.hasExtra("parameter")) {
                StudyParameter parameter = (StudyParameter) data.getSerializableExtra("parameter");
                String value = data.getStringExtra("chosenValue");
                if (parameter.defaultInput != null) {
                    study.inputs.put(parameter.name, value);
                } else {
                    study.outputs.put(parameter.name, value);
                }
                selectView.setText(value);
            }
        }
    }
}
