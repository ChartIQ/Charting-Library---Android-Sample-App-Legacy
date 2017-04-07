package com.chartiq.chartiqsample;

import android.content.DialogInterface;
import android.content.Intent;
import android.database.DataSetObserver;
import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.content.res.ResourcesCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.ListPopupWindow;
import android.support.v7.widget.RecyclerView;
import android.text.Editable;
import android.text.InputFilter;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.view.inputmethod.EditorInfo;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListAdapter;
import android.widget.PopupWindow;
import android.widget.TextView;

import com.chartiq.sdk.ChartIQ;
import com.chartiq.sdk.Promise;
import com.chartiq.sdk.User;
import com.chartiq.sdk.model.OHLCChart;
import com.chartiq.sdk.model.Study;
import com.chartiq.chartiqsample.studies.StudiesActivity;
import com.google.gson.Gson;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;

import static com.chartiq.chartiqsample.studies.StudiesActivity.ACTIVE_STUDIES;
import static com.chartiq.chartiqsample.studies.StudiesActivity.STUDIES_LIST;

public class MainActivity extends AppCompatActivity {

    private static final int DRAW_REQUEST_CODE = 1;
    private static final int STUDIES_REQUEST_CODE = 2;
    private static final int CHART_OPTIONS_REQUEST_CODE = 3;
    private static final int REFRESH_INTERVAL = 1;
    private static final String defaultSymbol = "AAPL";
    public static final String chartUrl = "http://192.168.1.31:8080/3.0.0/default/template-native-sdk.html";
    ChartIQ chartIQ;

    //top toolbar
    EditText symbolInput;
    TextView clear;
    Button interval;
    ImageView crosshair;

    //drawing toolbar
    LinearLayout drawingToolbar;
    TextView drawingToolName;
    LinearLayout fill;
    LinearLayout line;
    ImageView lineType;
    PopupWindow fillColorPalette;
    PopupWindow lineColorPalette;
    PopupWindow lineTypePalette;
    RecyclerView fillColorRecycler;
    RecyclerView lineColorRecycler;

    //variables
    String chartStyle;
    boolean logScale;
    String drawingTool;
    ArrayList<Study> activeStudies = new ArrayList<>();
    OHLCChart[] data;
    boolean chartLoaded = false;

    private final Item[] items = new Item[]{
            new Item("header", "Intervals", -1),
            new Item("divider", null, -1),
            new Item("item", "1 minute", R.id.m1),
            new Item("item", "3 minute", R.id.m3),
            new Item("item", "5 minute", R.id.m5),
            new Item("item", "10 minute", R.id.m10),
            new Item("item", "30 minute", R.id.m30),
            new Item("divider", null, -1),
            new Item("item", "1 hour", R.id.h1),
            new Item("item", "4 hour", R.id.h4),
            new Item("divider", null, -1),
            new Item("item", "1 day", R.id.d1, true),
            new Item("item", "2 day", R.id.d2),
            new Item("item", "3 day", R.id.d3),
            new Item("item", "5 day", R.id.d5),
            new Item("item", "10 day", R.id.d10),
            new Item("item", "20 day", R.id.d10),
            new Item("divider", null, -1),
            new Item("item", "1 week", R.id.w1),
            new Item("divider", null, -1),
            new Item("item", "1 month", R.id.month1)
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().requestFeature(Window.FEATURE_ACTION_BAR);
        setContentView(R.layout.activity_main);
        doMappings();

        chartIQ.setRefreshInterval(REFRESH_INTERVAL);
        symbolInput.setText(defaultSymbol);

        chartIQ.setDataSource(new ChartIQ.DataSource() {
            @Override
            public void pullInitialData(Map<String, Object> params, ChartIQ.DataSourceCallback callback) {
                loadChartData(params, callback);
            }

            @Override
            public void pullUpdateData(Map<String, Object> params, ChartIQ.DataSourceCallback callback) {
                loadChartData(params, callback);
            }

            @Override
            public void pullPaginationData(Map<String, Object> params, ChartIQ.DataSourceCallback callback) {
                loadChartData(params, callback);
            }
        });

        chartIQ.start("JGPHS0Pk6St63QUBdHk5uVZDM11T2Z1d1/BFz9E8HyI=", chartUrl, new ChartIQ.CallbackStart() {
            @Override
            public void onStart() {
                ChartIQ.setUser("android@chartiq.com", new ChartIQ.SetUserCallback() {
                    @Override
                    public void onSetUser(User user) {
                        chartIQ.setDataMethod(ChartIQ.DataMethod.PULL, defaultSymbol);
                        chartIQ.setSymbol(defaultSymbol);
                    }
                });
            }
        });

        chartIQ.setShowDebugInfo("debug".equals(BuildConfig.BUILD_TYPE));
        chartIQ.setOnTouchListener(new HideKeyboardOnTouchListener());

        drawingToolbar.setVisibility(View.GONE);
        interval.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showPopupMenu();
            }
        });
        interval.setOnTouchListener(new HideKeyboardOnTouchListener());
        clear.setVisibility(View.INVISIBLE);
        clear.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                symbolInput.setText("");
            }
        });
        symbolInput.setFilters(new InputFilter[]{new InputFilter.AllCaps()});
        symbolInput.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if (actionId == EditorInfo.IME_ACTION_NEXT
                        || actionId == EditorInfo.IME_ACTION_SEND
                        || actionId == EditorInfo.IME_ACTION_GO
                        || actionId == EditorInfo.IME_ACTION_DONE) {
                    if (!v.getText().toString().isEmpty()) {
//                        loadChartData(v.getText().toString());
                        chartIQ.setSymbol(v.getText().toString());
                    }
                    Util.hideKeyboard(symbolInput);
                    v.clearFocus();
                    return true;
                }
                return false;
            }
        });
        symbolInput.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (!s.toString().isEmpty()) {
                    clear.setVisibility(View.VISIBLE);
                } else {
                    clear.setVisibility(View.INVISIBLE);
                }
            }

            @Override
            public void afterTextChanged(Editable s) {

            }
        });

        crosshair.setTag("off");
        crosshair.setOnTouchListener(new HideKeyboardOnTouchListener());
        crosshair.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if ("off".equals(String.valueOf(v.getTag()))) {
                    chartIQ.enableCrosshairs();
                    v.setTag("on");
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                        ((ImageView) v).setImageDrawable(getResources().getDrawable(R.drawable.target_on, null));
                    } else {
                        ((ImageView) v).setImageDrawable(ResourcesCompat.getDrawable(getResources(), R.drawable.target_on, null));
                    }
                } else {
                    chartIQ.disableCrosshairs();
                    v.setTag("off");
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                        ((ImageView) v).setImageDrawable(getResources().getDrawable(R.drawable.target_off, null));
                    } else {
                        ((ImageView) v).setImageDrawable(ResourcesCompat.getDrawable(getResources(), R.drawable.target_off, null));
                    }
                }
            }
        });

        fill.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showFillColorPalette();
            }
        });
        line.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showLineColorPalette();
            }
        });
        lineType.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showLineTypePalette();
            }
        });
        fillColorPalette = new PopupWindow(this);
        fillColorPalette.setWidth(WindowManager.LayoutParams.WRAP_CONTENT);
        fillColorPalette.setHeight(WindowManager.LayoutParams.WRAP_CONTENT);
        fillColorPalette.setContentView(getLayoutInflater().inflate(R.layout.color_palette, null));

        lineColorPalette = new PopupWindow(this);
        lineColorPalette.setWidth(WindowManager.LayoutParams.WRAP_CONTENT);
        lineColorPalette.setHeight(WindowManager.LayoutParams.WRAP_CONTENT);
        lineColorPalette.setContentView(getLayoutInflater().inflate(R.layout.color_palette, null));

        lineTypePalette = new PopupWindow(this);
        lineTypePalette.setWidth(WindowManager.LayoutParams.WRAP_CONTENT);
        lineTypePalette.setHeight(WindowManager.LayoutParams.WRAP_CONTENT);
        lineTypePalette.setContentView(getLayoutInflater().inflate(R.layout.line_type_palette, null));

        fillColorRecycler = (RecyclerView) fillColorPalette.getContentView().findViewById(R.id.recycler);
        fillColorRecycler.setLayoutManager(new GridLayoutManager(this, 5));
        fillColorRecycler.setAdapter(new ColorAdapter(this, R.array.colors, new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                changeFillColor(v);
            }
        }));

        lineColorRecycler = (RecyclerView) lineColorPalette.getContentView().findViewById(R.id.recycler);
        lineColorRecycler.setLayoutManager(new GridLayoutManager(this, 5));
        lineColorRecycler.setAdapter(new ColorAdapter(this, R.array.colors, new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                changeLineColor(v);
            }
        }));
    }

    private void showFillColorPalette() {
        if (fillColorPalette.isShowing()) {
            fillColorPalette.dismiss();
        } else {
            lineColorPalette.dismiss();
            lineTypePalette.dismiss();
            fillColorPalette.showAtLocation(fill, Gravity.NO_GRAVITY, (int) fill.getX() - 90, 580);
        }
    }

    private void showLineColorPalette() {
        if (lineColorPalette.isShowing()) {
            lineColorPalette.dismiss();
        } else {
            fillColorPalette.dismiss();
            lineTypePalette.dismiss();
            lineColorPalette.showAtLocation(line, Gravity.NO_GRAVITY, (int) line.getX() - 90, 580);
        }
    }

    private void showLineTypePalette() {
        if (lineTypePalette.isShowing()) {
            lineTypePalette.dismiss();
        } else {
            fillColorPalette.dismiss();
            lineColorPalette.dismiss();
            lineTypePalette.showAtLocation(lineType, Gravity.NO_GRAVITY, (int) lineType.getX(), 580);
        }
    }

    private void doMappings() {
        chartIQ = (ChartIQ) findViewById(R.id.webview);
        drawingToolbar = (LinearLayout) findViewById(R.id.drawing_toolbar);
        drawingToolName = (TextView) findViewById(R.id.drawing_tool_name);
        fill = (LinearLayout) findViewById(R.id.fill);
        line = (LinearLayout) findViewById(R.id.line);
        lineType = (ImageView) findViewById(R.id.line_type);
        interval = (Button) findViewById(R.id.interval);
        clear = (TextView) findViewById(R.id.clear);
        symbolInput = (EditText) findViewById(R.id.symbol);
        crosshair = (ImageView) findViewById(R.id.crosshair);
    }

    private void loadChartData(Map<String, Object> params, final ChartIQ.DataSourceCallback callback) {
        if(!params.containsKey("start") || params.get("start") == null || "".equals(params.get("start"))) {
            params.put("start", "2016-12-16T16:00:00.000Z");
        }

        if(params.containsKey("end") || "".equals(params.get("start"))) {
            TimeZone tz = TimeZone.getTimeZone("UTC");
            DateFormat df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm'Z'");
            df.setTimeZone(tz);
            String endDate = df.format(new Date());
            params.put("end", endDate);
        }

        boolean isMinute = params.containsKey("interval") && TextUtils.isDigitsOnly(String.valueOf(params.get("interval")));
        params.put("interval", isMinute ? "minute" : params.get("interval"));
        params.put("period", isMinute ? interval : params.get("period"));

        StringBuilder builder = new StringBuilder();
        builder.append("http://simulator.chartiq.com/datafeed?");
        builder.append("identifier=" + params.get("symbol"));
        builder.append("&startdate=" + params.get("start"));
        if (params.containsKey("end")) {
            builder.append("&enddate=" + params.get("end"));
        }
        builder.append("&interval=" + params.get("interval"));
        builder.append("&period=" + params.get("period"));
        builder.append("&seed=1001");

        final String url = builder.toString();
        final String symbol = String.valueOf(params.get("symbol"));
        new AsyncTask<Void, Void, String>() {
            @Override
            protected String doInBackground(Void... params) {
                String body = "";
                try {
                    URL connectionUrl = new URL(url);
                    HttpURLConnection connection = (HttpURLConnection) connectionUrl.openConnection();
                    connection.setRequestMethod("GET");
                    connection.setRequestProperty("Content-Type", "application/json");
                    connection.connect();
                    int code = connection.getResponseCode();

                    InputStream is;
                    StringBuilder builder;
                    if (code >= 200 && code < 400) {
                        builder = new StringBuilder();
                        is = connection.getInputStream();
                    } else {
                        is = connection.getErrorStream();
                        builder = new StringBuilder("Error("+code+"): ");
                    }
                    if (is != null) {
                        BufferedReader reader = new BufferedReader(new InputStreamReader(is));
                        String line;
                        while ((line = reader.readLine()) != null) {
                            builder.append(line);
                        }
                        body = builder.toString();
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
                return body;
            }

            @Override
            protected void onPostExecute(String body) {
                if(body.startsWith("Error")){
                    AlertDialog invalidSymbolAlert = new AlertDialog.Builder(MainActivity.this)
                            .setTitle(body)
                            .setCancelable(true)
                            .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int id) {
                                    dialog.dismiss();
                                }
                            }).create();
                    invalidSymbolAlert.show();
                } else if ("invalid symbol".equals(body)) {
                    AlertDialog invalidSymbolAlert = new AlertDialog.Builder(MainActivity.this)
                            .setTitle("Invalid symbol")
                            .setCancelable(true)
                            .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int id) {
                                    dialog.dismiss();
                                }
                            }).create();
                    invalidSymbolAlert.show();
                } else {
                    data = new Gson().fromJson(body, OHLCChart[].class);
                    callback.execute(data);
                }

                chartLoaded = true;

            }
        }.execute((Void[]) null);
    }

    private void showPopupMenu() {
        final ListPopupWindow popupWindow = new ListPopupWindow(this);
        popupWindow.setAnchorView(interval);
        popupWindow.setAdapter(new IntervalsAdapter(Arrays.asList(items)));
        popupWindow.setWidth(ListPopupWindow.MATCH_PARENT);
        popupWindow.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Item item = items[position];
                for (Item interval : items) {
                    interval.selected = false;
                }
                item.selected = true;
                setInterval(item);
                popupWindow.dismiss();
            }
        });
        popupWindow.show();
    }

    public void setInterval(Item item) {
        switch (item.id) {
            case R.id.m1:
                chartIQ.setPeriodicity(1, "1", "minute");
                break;
            case R.id.m3:
                chartIQ.setPeriodicity(1, "3", "minute");
                break;
            case R.id.m5:
                chartIQ.setPeriodicity(1, "5", "minute");
                break;
            case R.id.m10:
                chartIQ.setPeriodicity(1, "10", "minute");
                break;
            case R.id.m15:
                chartIQ.setPeriodicity(1, "15", "minute");
                break;
            case R.id.m30:
                chartIQ.setPeriodicity(1, "30", "minute");
                break;
            case R.id.h1:
                chartIQ.setPeriodicity(1, "60", "minute");
                break;
            case R.id.h4:
                chartIQ.setPeriodicity(1, "240", "minute");
                break;
            case R.id.d1:
                chartIQ.setPeriodicity(1, "day", "minute");
                break;
            case R.id.d2:
                chartIQ.setPeriodicity(2, "day", "minute");
                break;
            case R.id.d3:
                chartIQ.setPeriodicity(3, "day", "minute");
                break;
            case R.id.d5:
                chartIQ.setPeriodicity(5, "day", "minute");
                break;
            case R.id.d10:
                chartIQ.setPeriodicity(10, "day", "minute");
                break;
            case R.id.d20:
                chartIQ.setPeriodicity(20, "day", "minute");
                break;
            case R.id.w1:
                chartIQ.setPeriodicity(1, "week", "minute");
                break;
            case R.id.month1:
                chartIQ.setPeriodicity(1, "month", "minute");
                break;
        }
        interval.setText(item.title);
    }

    public void onDrawMenuClick(View view) {
        if (drawingToolbar.getVisibility() == View.VISIBLE) {
            fillColorPalette.dismiss();
            lineColorPalette.dismiss();
            lineTypePalette.dismiss();
            drawingToolbar.setVisibility(View.GONE);
            chartIQ.disableDrawing();
        } else {
            startDrawActivity(null);
        }
    }

    public void startDrawActivity(View view) {
        Intent drawIntent = new Intent(this, DrawActivity.class);
        startActivityForResult(drawIntent, DRAW_REQUEST_CODE);
    }

    public void startStudiesActivity(View view) {
        if (chartLoaded) {
            chartIQ.getStudyList().than(new Promise.Callback<Study[]>() {
                @Override
                public void call(final Study[] studies) {
                    chartIQ.getActiveStudies().than(new Promise.Callback<Study[]>() {
                        @Override
                        public void call(Study[] array) {
                            Intent studiesIntent = new Intent(MainActivity.this, StudiesActivity.class);
                            ArrayList<Study> allStudies = new ArrayList<>(Arrays.asList(studies));
                            activeStudies = new ArrayList<>(Arrays.asList(array));
                            studiesIntent.putExtra(STUDIES_LIST, allStudies);
                            studiesIntent.putExtra(ACTIVE_STUDIES, activeStudies);
                            startActivityForResult(studiesIntent, STUDIES_REQUEST_CODE);
                        }
                    });
                }
            });
        }
    }

    public void startChartOptionsActivity(View view) {
        Intent chartOptionsIntent = new Intent(this, ChartOptions.class);
        chartOptionsIntent.putExtra("chartStyle", chartStyle);
        chartOptionsIntent.putExtra("logScale", logScale);
        startActivityForResult(chartOptionsIntent, CHART_OPTIONS_REQUEST_CODE);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (DRAW_REQUEST_CODE == requestCode) {
            if (RESULT_OK == resultCode) {
                if (data.getStringExtra("drawingTool") != null) {
                    drawingTool = data.getStringExtra("drawingTool");
                    activateDrawingTool(drawingTool);
                } else if (data.getBooleanExtra("clearAllDrawings", false)) {
                    fillColorPalette.dismiss();
                    lineColorPalette.dismiss();
                    lineTypePalette.dismiss();
                    drawingToolbar.setVisibility(View.GONE);
                    chartIQ.clearDrawing();
                }
            }
        } else if (CHART_OPTIONS_REQUEST_CODE == requestCode) {
            if (RESULT_OK == resultCode) {
                chartStyle = data.getStringExtra("chartStyle");
                switch (chartStyle) {
                    case "Heikin Ashi":
                        chartIQ.setAggregationType("heikinashi");
                        chartIQ.setChartType("candle");
                        break;
                    case "Kagi":
                        chartIQ.setAggregationType("kagi");
                        chartIQ.setChartType("candle");
                        break;
                    case "Renko":
                        chartIQ.setAggregationType("renko");
                        chartIQ.setChartType("candle");
                        break;
                    case "Range Bars":
                        chartIQ.setAggregationType("rangebars");
                        chartIQ.setChartType("candle");
                        break;
                    case "Point & Figure":
                        chartIQ.setAggregationType("pandf");
                        chartIQ.setChartType("candle");
                        break;
                    default:
                        chartIQ.setChartType(chartStyle.toLowerCase().replace(" ", "_"));
                        chartIQ.setAggregationType(null);
                }
                logScale = data.getBooleanExtra("logScale", false);
                chartIQ.setChartScale(logScale ? "log" : "linear");
            }
        } else if (STUDIES_REQUEST_CODE == requestCode) {
            if (RESULT_OK == resultCode) {
                for (Study activeStudy : activeStudies) {
                    chartIQ.removeStudy(activeStudy.shortName);
                }
                activeStudies = (ArrayList<Study>) data.getSerializableExtra(ACTIVE_STUDIES);
                for (Study activeStudy : activeStudies) {
                    chartIQ.addStudy(activeStudy);
                }
            }
        } else {
            super.onActivityResult(requestCode, resultCode, data);
        }
    }

    private void activateDrawingTool(String drawingTool) {
        drawingToolName.setText(drawingTool);
        drawingToolbar.setVisibility(View.VISIBLE);
        switch (drawingTool) {
            case "Channel":
                chartIQ.enableDrawing("channel");
                break;
            case "Doodle":
                chartIQ.enableDrawing("freeform");
                break;
            case "Ellipse":
                chartIQ.enableDrawing("ellipse");
                break;
            case "Fib-arc":
                chartIQ.enableDrawing("fibarc");
                break;
            case "Fib-fan":
                chartIQ.enableDrawing("fibfan");
                break;
            case "Fib-retrace":
                chartIQ.enableDrawing("fibonacci");
                break;
            case "Fib-timezone":
                chartIQ.enableDrawing("fibtimezone");
                break;
            case "Gartley":
                chartIQ.enableDrawing("gartley");
                break;
            case "Horizontal line":
                chartIQ.enableDrawing("horizontal");
                break;
            case "Line":
                chartIQ.enableDrawing("line");
                break;
            case "Pitch fork":
                chartIQ.enableDrawing("pitchfork");
                break;
            case "Ray":
                chartIQ.enableDrawing("ray");
                break;
            case "Rectangle":
                chartIQ.enableDrawing("rectangle");
                break;
            case "Segment":
                chartIQ.enableDrawing("segment");
                break;
            case "Vertical line":
                chartIQ.enableDrawing("vertical");
                break;
        }
    }

    public void changeFillColor(View view) {
        fillColorPalette.dismiss();
        fill.getChildAt(1).setBackgroundColor(Color.parseColor(String.valueOf(view.getTag())));
        chartIQ.setDrawingParameter("\"fillColor\"", "\"" + String.valueOf(view.getTag()) + "\"");
    }

    public void changeLineColor(View view) {
        lineColorPalette.dismiss();
        line.getChildAt(1).setBackgroundColor(Color.parseColor(String.valueOf(view.getTag())));
        chartIQ.setDrawingParameter("\"currentColor\"", "\"" + String.valueOf(view.getTag()) + "\"");
    }

    public void changeLineType(View view) {
        lineTypePalette.dismiss();
        lineType.setImageDrawable(((ImageView) view).getDrawable());
        String pattern = String.valueOf(view.getTag());
        chartIQ.setDrawingParameter("\"pattern\"", "\"" + pattern.substring(0, pattern.length() - 1) + "\"");
        chartIQ.setDrawingParameter("\"lineWidth\"", pattern.substring(pattern.length() - 1));
    }

    class Item {
        String type;
        String title;
        int id;
        boolean selected;

        public Item(String type, String title, int id) {
            this.type = type;
            this.title = title;
            this.id = id;
        }

        public Item(String type, String title, int id, boolean selected) {
            this(type, title, id);
            this.selected = selected;
        }
    }

    class IntervalsAdapter implements ListAdapter {
        private List<Item> items;

        public IntervalsAdapter(List<Item> items) {
            this.items = items;
        }

        @Override
        public boolean areAllItemsEnabled() {
            return false;
        }

        @Override
        public boolean isEnabled(int position) {
            return "item".equals(items.get(position).type);
        }

        @Override
        public void registerDataSetObserver(DataSetObserver observer) {

        }

        @Override
        public void unregisterDataSetObserver(DataSetObserver observer) {

        }

        @Override
        public int getCount() {
            return items.size();
        }

        @Override
        public Object getItem(int position) {
            return items.get(position);
        }

        @Override
        public long getItemId(int position) {
            return items.get(position).id;
        }

        @Override
        public boolean hasStableIds() {
            return true;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            Item item = items.get(position);
            View view;
            if("divider".equals(item.type)) {
                view = getLayoutInflater().inflate(R.layout.menu_divider, parent, false);
            } else if("item".equals(item.type)) {
                view = getLayoutInflater().inflate(R.layout.menu_item, parent, false);
                ((TextView) view).setText(items.get(position).title);
                if(item.selected){
                    ((TextView) view).setTextColor(getResources().getColor(R.color.blue));
                }
            } else {
                view = getLayoutInflater().inflate(R.layout.menu_header, parent, false);
                ((TextView) view).setText(items.get(position).title);
            }
            return view;
        }

        @Override
        public int getItemViewType(int position) {
            Item item = items.get(position);
            if( "item".equals(item.type))
                return 1;

            if( "divider".equals(item.type))
                return 2;

            return 0;
        }

        @Override
        public int getViewTypeCount() {
            return 3;
        }

        @Override
        public boolean isEmpty() {
            return items.isEmpty();
        }
    }
}
