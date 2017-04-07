package com.chartiq.chartiqsample.studies;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.KeyEvent;
import android.view.View;
import android.view.Window;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.TextView;

import com.chartiq.sdk.ChartIQ;
import com.chartiq.sdk.Promise;
import com.chartiq.sdk.model.Study;
import com.chartiq.chartiqsample.HideKeyboardOnTouchListener;
import com.chartiq.chartiqsample.MainActivity;
import com.chartiq.chartiqsample.R;
import com.chartiq.chartiqsample.Util;
import com.chartiq.chartiqsample.ui.DividerDecoration;
import com.chartiq.chartiqsample.ui.StickyHeaderDecoration;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Set;

public class StudiesActivity extends AppCompatActivity {
    public static final String STUDIES_LIST = "studiesList";
    public static final String ACTIVE_STUDIES = "activeStudies";
    private static final int RESULT_STUDY_REMOVED = 4;
    private Toolbar toolbar;
    private RecyclerView studiesList;
    private ArrayList<Study> studies;
    private ArrayList<Study> activeStudies = new ArrayList<>();
    private Set<Study> lastSelection;
    private TextView studiesCount;
    private Toolbar selectiontoolbar;
    private View closeSelection;
    private StudiesAdapter studiesAdapter;
    private EditText studySearch;
    private View addButton;
    private View removeButton;
    private TextView clearStudySearchInput;
    private ChartIQ chartIQ;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().requestFeature(Window.FEATURE_ACTION_BAR);
        setContentView(R.layout.activity_studies);
        studiesCount = (TextView) findViewById(R.id.studies_count);
        selectiontoolbar = (Toolbar) findViewById(R.id.selection_toolbar);
        toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        studiesList = (RecyclerView) findViewById(R.id.studies_list);
        chartIQ = (ChartIQ) findViewById(R.id.chart);
        chartIQ.disableAnalytics = true;
        chartIQ.getSettings().setJavaScriptEnabled(true);
        chartIQ.getSettings().setDomStorageEnabled(true);
        chartIQ.addJavascriptInterface(chartIQ, "promises");
        chartIQ.loadUrl(MainActivity.chartUrl);

        studiesList.setOnTouchListener(new HideKeyboardOnTouchListener());

        if (getIntent().hasExtra(STUDIES_LIST)) {
            studies = (ArrayList<Study>) getIntent().getExtras().getSerializable(STUDIES_LIST);
            Collections.sort(studies, new Comparator<Study>() {
                @Override
                public int compare(Study o1, Study o2) {
                    if (o1.name.isEmpty()) {
                        if (o2.name.isEmpty()) {
                            return o1.shortName.compareToIgnoreCase(o2.shortName);
                        }
                        return o1.shortName.compareToIgnoreCase(o2.name);
                    } else if (o2.name.isEmpty()) {
                        return o1.name.compareToIgnoreCase(o2.shortName);
                    } else {
                        return o1.name.compareToIgnoreCase(o2.name);
                    }
                }
            });
        }
        if (getIntent().hasExtra(ACTIVE_STUDIES)) {
            activeStudies = (ArrayList<Study>) getIntent().getExtras().getSerializable(ACTIVE_STUDIES);
            if (activeStudies != null) {
                for (Study s : activeStudies) {
                    chartIQ.addStudy(s);
                }
            }
            Collections.sort(activeStudies, new Comparator<Study>() {
                @Override
                public int compare(Study o1, Study o2) {
                    if (o1.name.isEmpty()) {
                        if (o2.name.isEmpty()) {
                            return o1.shortName.compareToIgnoreCase(o2.shortName);
                        }
                        return o1.shortName.compareToIgnoreCase(o2.name);
                    } else if (o2.name.isEmpty()) {
                        return o1.name.compareToIgnoreCase(o2.shortName);
                    } else {
                        return o1.name.compareToIgnoreCase(o2.name);
                    }
                }
            });
        }
        configureStudiesList(studiesList, activeStudies, studies);

        closeSelection = findViewById(R.id.close_selection);
        closeSelection.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                clearSelection();
            }
        });

        clearStudySearchInput = (TextView) findViewById(R.id.clear);
        clearStudySearchInput.setVisibility(View.INVISIBLE);

        studySearch = (EditText) findViewById(R.id.study_search_field);
        studySearch.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView view, int actionId, KeyEvent event) {
                if (actionId == EditorInfo.IME_ACTION_SEND || actionId == EditorInfo.IME_ACTION_NEXT || actionId == EditorInfo.IME_ACTION_DONE) {
                    setFilteredStudies();
                    Util.hideKeyboard(studySearch);
                    return true;
                }
                return false;
            }
        });
        studySearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (!s.toString().isEmpty()) {
                    clearStudySearchInput.setVisibility(View.VISIBLE);
                } else {
                    clearStudySearchInput.setVisibility(View.INVISIBLE);
                }
                setFilteredStudies();
            }

            @Override
            public void afterTextChanged(Editable s) {

            }
        });

        addButton = findViewById(R.id.add_button);
        addButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                for (Study s : lastSelection) {
                    chartIQ.addStudy(s);
                }
                activeStudies.addAll(lastSelection);
                studiesAdapter.setActiveStudiesList(activeStudies);
                studiesAdapter.getAvailableStudies().removeAll(lastSelection);
                clearSelection();
            }
        });
        removeButton = findViewById(R.id.remove_button);
        removeButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                activeStudies.removeAll(lastSelection);
                studiesAdapter.getAvailableStudies().addAll(lastSelection);
                clearSelection();
            }
        });
    }

    private void setFilteredStudies() {
        if (studies != null) {
            String text = studySearch.getText().toString();
            ArrayList<Study> filteredStudies = new ArrayList<Study>();
            for (Study study : studies) {
                if (!activeStudies.contains(study) && (study.name.toLowerCase().contains(text) || study.shortName.toLowerCase().contains(text))) {
                    filteredStudies.add(study);
                }
            }
            studiesAdapter.setAvailableStudies(filteredStudies);
            studiesAdapter.notifyDataSetChanged();
        }
    }

    public void clearStudySearch(View view) {
        studySearch.setText("");
        setFilteredStudies();
    }

    private void clearSelection() {
        if (lastSelection != null) {
            lastSelection.clear();
        }
        studiesAdapter.clearSelection();
        selectiontoolbar.setVisibility(View.GONE);
    }

    private void configureStudiesList(final RecyclerView studiesList, List<Study> activeStudiesList, List<Study> availableStudies) {
        studiesAdapter = new StudiesAdapter(this, activeStudiesList, availableStudies, new View.OnClickListener() {
            @Override
            public void onClick(final View v) {
                final Study clickedStudy = studiesAdapter.getItemByPosition((Integer) v.getTag());
                chartIQ.getActiveStudies().than(new Promise.Callback<Study[]>() {
                    @Override
                    public void call(Study[] array) {
                        if (array != null && array.length > 0) {
                            ArrayList<Study> active = new ArrayList<>(Arrays.asList(array));
                            for (final Study s : active) {
                                if (s.shortName.equals(clickedStudy.shortName) || s.type.equals(clickedStudy.shortName) || s.type.equals(clickedStudy.type)) {
                                    chartIQ.getStudyOutputParameters(s.shortName).than(new Promise.Callback<String>() {
                                        @Override
                                        public void call(final String outputs) {

                                            chartIQ.getStudyInputParameters(s.shortName).than(new Promise.Callback<String>() {
                                                @Override
                                                public void call(String inputs) {

                                                    Intent studyOptionsIntent = new Intent(StudiesActivity.this, StudyOptionsActivity.class);
                                                    studyOptionsIntent.putExtra("study", clickedStudy);
                                                    studyOptionsIntent.putExtra("outputs", outputs);
                                                    studyOptionsIntent.putExtra("inputs", inputs);
                                                    startActivityForResult(studyOptionsIntent, 0);
                                                }
                                            });
                                        }
                                    });
                                }
                            }
                        }
                    }
                });
            }
        });
        StickyHeaderDecoration decoration = new StickyHeaderDecoration(studiesAdapter);
        final DividerDecoration divider = new DividerDecoration.Builder(this)
                .setHeight(R.dimen.default_divider_height)
                .setColorResource(R.color.default_header_color)
                .build();

        studiesList.setAdapter(studiesAdapter);
        studiesList.addItemDecoration(divider, 0);
        studiesList.addItemDecoration(decoration, 1);
        studiesAdapter.setSelectionChangeListener(new StudiesAdapter.OnSelectionChangeListener() {
            @Override
            public void onSelectionchange(Set<Study> selectedStudies, boolean isActive) {
                lastSelection = selectedStudies;
                if (selectedStudies.size() > 0) {
                    studiesCount.setText(String.valueOf(selectedStudies.size()));
                    selectiontoolbar.setVisibility(View.VISIBLE);
                    removeButton.setVisibility(isActive ? View.VISIBLE : View.GONE);
                    addButton.setVisibility(isActive ? View.GONE : View.VISIBLE);
                } else {
                    selectiontoolbar.setVisibility(View.GONE);
                }
                InputMethodManager imm = (InputMethodManager) StudiesActivity.this.getSystemService(Activity.INPUT_METHOD_SERVICE);
                imm.hideSoftInputFromWindow(studiesList.getWindowToken(), 0);
            }
        });
    }

    @Override
    public void setSupportActionBar(Toolbar toolbar) {
        super.setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayShowTitleEnabled(false);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowHomeEnabled(true);
        toolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onBackPressed();
            }
        });
    }

    @Override
    public void onBackPressed() {
        setResult(RESULT_OK, new Intent().putExtra(ACTIVE_STUDIES, activeStudies));
        finish();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (0 == requestCode) {
            if (RESULT_OK == resultCode) {
                Study updatedStudy = (Study) data.getSerializableExtra("study");
                for (Study s : activeStudies) {
                    if (updatedStudy.name.equals(s.name)) {
                        activeStudies.set(activeStudies.indexOf(s), updatedStudy);
//                        chartIQ.removeStudy(s);
//                        chartIQ.addStudy(updatedStudy);
                    }
                }
            } else if (RESULT_STUDY_REMOVED == resultCode) {
                Study removedStudy = (Study) data.getSerializableExtra("study");
                activeStudies.remove(removedStudy);
                studiesAdapter.setActiveStudiesList(activeStudies);
                studiesAdapter.getAvailableStudies().add(removedStudy);
                clearSelection();
            }
        } else {
            super.onActivityResult(requestCode, resultCode, data);
        }
    }

}
