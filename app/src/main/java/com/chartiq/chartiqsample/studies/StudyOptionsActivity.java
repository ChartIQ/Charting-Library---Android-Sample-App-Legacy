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

import com.chartiq.sdk.model.Study;
import com.chartiq.chartiqsample.ColorAdapter;
import com.chartiq.chartiqsample.R;
import com.google.gson.Gson;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

public class StudyOptionsActivity extends AppCompatActivity {

    private static final int RESULT_STUDY_REMOVED = 4;

    TextView studyTitle;
    Toolbar toolbar;
    Study study;
    private HashMap<String, Object> defaultInputs = new HashMap<>();
    private HashMap<String, Object> defaultOutputs = new HashMap<>();
    private HashMap<String, Object> defaultParameters = new HashMap<>();
    private HashMap<String, String> studyParameterColors = new HashMap<>();
    private HashMap<String, String> studyParameterValues = new HashMap<>();
    private HashMap<String, Object> studyFields = new HashMap<>();
    private HashMap<String, String> movingAverageAbbr = new HashMap<>();
    LinearLayout optionsLayout;
    private PopupWindow colorPalette;
    private RecyclerView colorRecycler;
    private TextView currentColorView;
    private String colorOptionName;
    private StudyParameter[] inputs;
    private StudyParameter[] outputs;
    private StudyParameter[] parameters;
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

        // map values for the study parameters
        studyParameterColors.put("OverBought", "studyOverBoughtColor");
        studyParameterColors.put("OverSold", "studyOverSoldColor");
        studyParameterValues.put("OverBought", "studyOverBoughtValue");
        studyParameterValues.put("OverSold", "studyOverSoldValue");
        studyParameterValues.put("Show Zones", "studyOverZones");
        studyFields.put("Close", "Close");
        studyFields.put("Open", "Open");
        studyFields.put("High", "High");
        studyFields.put("Low", "Low");
        studyFields.put("Adj_Close", "Adj_Close");
        studyFields.put("hl/2", "hl/2");
        studyFields.put("hlc/3", "hlc/3");
        studyFields.put("hlcc/4", "hlcc/4");
        studyFields.put("ohlc/4", "ohlc/4");
        movingAverageAbbr.put("ma", "simple");
        movingAverageAbbr.put("ema", "exponential");
        movingAverageAbbr.put("tsma", "time series");
        movingAverageAbbr.put("tma", "triangular");
        movingAverageAbbr.put("vma", "variable");
        movingAverageAbbr.put("vdma", "VIDYA");
        movingAverageAbbr.put("wma", "weighted");
        movingAverageAbbr.put("smma", "welles wilder");

        if (getIntent().hasExtra("study")) {
            study = (Study) getIntent().getSerializableExtra("study");
            studyTitle.setText(study.name);
            if (study.inputs != null) {
                defaultInputs = new HashMap<>(study.inputs);
            }
            if (study.outputs != null) {
                defaultOutputs = new HashMap<>(study.outputs);
            }
            if (study.parameters != null) {
                defaultParameters = new HashMap<>(study.parameters);
            }
        }

        if (getIntent().hasExtra("inputs")) {
            try {
                inputs = new Gson().fromJson(getIntent().getStringExtra("inputs"), StudyParameter[].class);
            } catch(Exception exception){
                exception.printStackTrace();

            }
            if (study.inputs != null) {
                bindStudyOptions(inputs, study.inputs);
            }
        }

        if (getIntent().hasExtra("outputs")) {
            try {
                outputs = new Gson().fromJson(getIntent().getStringExtra("outputs"), StudyParameter[].class);
            } catch(Exception exception){
                exception.printStackTrace();

            }
            if (study.outputs != null || outputs != null) {
                bindStudyOptions(outputs, study.outputs);
            }
        }

        if (getIntent().hasExtra("parameters")) {
            try {
                parameters = new Gson().fromJson(getIntent().getStringExtra("parameters"), StudyParameter[].class);
            } catch(Exception exception){
                exception.printStackTrace();
            }
            if(study.parameters != null){
                bindStudyOptions(parameters, study.parameters);
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
        String parameterValue = studyParameterColors.get(colorOptionName);
        if(parameterValue != null) { // parameter entry, need to drill down to properly set the value
            changeStudyParameter(parameterValue, study, String.valueOf(view.getTag()));
        } else if (study.outputs != null) {
            study.outputs.put(colorOptionName, String.valueOf(view.getTag()));
        }
    }

    private void bindStudyOptions(StudyParameter[] array, final Map<String, Object> studyParams) {
        for (final StudyParameter parameter : array) {
            String heading = parameter.heading;
            String parameterColorValue = studyParameterColors.get(heading);
            // get the study parameter color, which has another fieldName from the heading in the parameter
            if(parameterColorValue != null) {
                HashMap<String, Object> oldParameters = (HashMap<String, Object>) study.parameters;
                for (Map.Entry<String,Object> entry : oldParameters.entrySet()) {
                    if (entry.getKey().equals(parameterColorValue)) {
                        parameter.color = String.valueOf(entry.getValue());
                        break;
                    }
                }
                bindColor(parameter);
            } else if (parameter.color != null || parameter.defaultOutput != null || parameter.defaultColor != null) {
                // get the color from the client-side Study object
                if (studyParams != null && studyParams.containsKey(parameter.name)) {
                    parameter.color = String.valueOf(studyParams.get(parameter.name));
                }
                // get the color from the study definition
                else if(parameter.color != null) {
                    parameter.color = String.valueOf(parameter.color);
                }
                // get the color from the study descriptor default value
                else if(parameter.defaultOutput != null) {
                    parameter.color = String.valueOf(parameter.defaultOutput);
                }
                bindColor(parameter);
            }

            if (parameter.type != null) {
                String parameterOldValue = studyParameterValues.get(heading);
                Object parameterNewValue = null;
                if (parameterOldValue != null) {
                    HashMap<String, Object> oldParameters = (HashMap<String, Object>) study.parameters;
                    for (Map.Entry<String, Object> entry : oldParameters.entrySet()) {
                        // ChartIQ parameter boolean follows "fieldNameEnabled" format, where the
                        // "Enabled" is dynamically added in the library study dialog. Which
                        // is not accessible on the mobile side.
                        if(entry.getValue() instanceof Boolean) {
                            parameterOldValue += "Enabled";
                        }
                        if (entry.getKey().equals(parameterOldValue)) {
                            parameterNewValue = entry.getValue();
                            break;
                        }
                    }
                }
                switch (parameter.type) {
                    case "select":
                        if(parameter.name.toLowerCase().equals("field")) {
                            parameter.options = studyFields;
                        }
                        if(parameterNewValue != null) {
                            parameter.value = parameterNewValue;
                        } else if (studyParams.containsKey(parameter.name)) {
                            parameter.value = studyParams.get(parameter.name);
                        }
                        bindSelect(parameter);
                        break;
                    case "number":
                        if(parameterNewValue != null) {
                            parameter.value = parameterNewValue;
                        } else if (studyParams.containsKey(parameter.name)) {
                            parameter.value = studyParams.get(parameter.name);
                        }
                        bindNumber(studyParams, parameter);
                        break;
                    case "text":
                        if(parameterNewValue != null) {
                            parameter.value = parameterNewValue;
                        } else {
                            if (studyParams.containsKey(parameter.name)) {
                                parameter.value = studyParams.get(parameter.name);
                            }
                        }
                        bindString(studyParams, parameter);
                        break;
                    case "checkbox":
                        if(parameterNewValue != null) {
                            parameter.value = parameterNewValue;
                        } else if (studyParams.containsKey(parameter.name)) {
                            parameter.value = studyParams.get(parameter.name) + "Enabled";
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
        String displayText = (String) parameter.value;

        // if a moving average value is abbreviated then use the default input
        if(parameter.heading.contains("Moving Average") || parameter.heading.equals("Type")) {
            String movingAverageValue = movingAverageAbbr.get(displayText);
            if(movingAverageValue != null) {
                displayText = (String) parameter.defaultInput;
            }
        }

        // default value for Field is actually field, which is normally handled in the Charting library but needs to be handled here
        if(parameter.name.toLowerCase().equals("field") && displayText.toLowerCase().equals("field")) {
            displayText = "Close";
        }

        // If there are display mappings be sure to get the correct display value
        if(parameter.options != null && parameter.options.size() > 0) {
            displayText = (String) parameter.options.get(displayText);
        }
        textView.setText(displayText);
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
                String parameterValue = studyParameterValues.get(parameter.heading);
                if(parameterValue != null) {
                    changeStudyParameter(parameterValue + "Enabled", study, switchView.isChecked());
                } else {
                    studyParams.put(parameter.name, switchView.isChecked());
                }
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
                String parameterValue = studyParameterValues.get(parameter.heading);
                if(parameterValue != null) {
                    changeStudyParameter(parameterValue, study, editText.getText().toString());
                } else {
                    studyParams.put(parameter.name, editText.getText().toString());
                }
            }

            @Override
            public void afterTextChanged(Editable s) {

            }
        });
    }

    /**
     * Helper method to ensure that the correct study parameter value is added to the client side study object
     * @param parameterName
     * @param study
     * @param value
     */
    private void changeStudyParameter(String parameterName, Study study, Object value) {
        Map<String, Object> currentParameters = (Map<String, Object>) study.parameters.get("init");
        if(currentParameters == null) {
            currentParameters = study.parameters;
        }
        for (Map.Entry<String,Object> entry : currentParameters.entrySet()) {
            if (entry.getKey().equals(parameterName)) {
                currentParameters.put(entry.getKey(), value);
                break;
            }
        }
        study.parameters = currentParameters; // replace with potential new values
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
        // check the parameter color field first then the default field
        // if both are null then the study is not fully defined
        if(parameter.color != null) {
            if ("auto".equals(parameter.color)) {
                color.setBackgroundColor(Color.BLACK);
            } else {
                if(parameter.color.contains("rgb")) {
                    color.setBackgroundColor(convertRGB(parameter.color));
                } else {
                    color.setBackgroundColor(Color.parseColor(parameter.color));
                }
            }
        } else if(parameter.defaultColor != null) {
            if ("auto".equals(parameter.defaultColor)) {
                color.setBackgroundColor(Color.BLACK);
            } else {
                if(parameter.defaultColor.contains("rgb")) {
                    color.setBackgroundColor(convertRGB(parameter.defaultColor));
                } else {
                    color.setBackgroundColor(Color.parseColor(parameter.defaultColor));
                }
            }
        }
    }

    /**
     * Helper method to convert an rgb color code
     * @param color
     * @return int Color code
     */
    private int convertRGB(String color) {
        String subString = color.substring(color.indexOf('(') + 1, color.indexOf(')'));
        String rgbColors[] = subString.split(",");
        int parsedColor = 0;
        int alphaValue = 255;

        // contains an alpha value
        if(rgbColors.length == 4) {
            double value = Double.parseDouble(rgbColors[3].trim());
            value = Math.floor(value >= 1.0 ? 255 : value * 256.0); // use 256 for floating point precision when value is less than 1.0
            alphaValue = (int) value;
        }
        parsedColor = Color.argb(alphaValue, Integer.parseInt(rgbColors[0].trim()),
                Integer.parseInt(rgbColors[1].trim()), Integer.parseInt(rgbColors[2].trim()));

        return parsedColor;
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
                String parameterValue = studyParameterValues.get(parameter.heading);
                if(parameterValue != null) {
                    changeStudyParameter(parameterValue, study, editText.getText().toString());
                } else {
                    studyParams.put(parameter.name, editText.getText().toString());
                }
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
        study.parameters = new HashMap<>(defaultParameters);
        optionsLayout.removeAllViews();
        if (inputs != null) {
            bindStudyOptions(inputs, study.inputs);
        }
        if (outputs != null) {
            bindStudyOptions(outputs, study.outputs);
        }
        if (parameters != null) {
            bindStudyOptions(parameters, study.parameters);
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
                    // it is a moving average, and options exist, get the mapped key value as that is the value needed by the study
                    if ((parameter.heading.contains("Moving Average") || parameter.heading.equals("Type")) && parameter.options != null) {
                        for (HashMap.Entry<String, Object> entry : parameter.options.entrySet()) {
                            if(entry.getValue().equals(value)){
                                String key = (String) entry.getKey();
                                study.inputs.put(parameter.name, key);
                                break;
                            }
                        }
                    } else {
                        study.inputs.put(parameter.name, value);
                    }
                } else {
                    study.outputs.put(parameter.name, value);
                }
                selectView.setText(value);
            }
        }
    }
}
