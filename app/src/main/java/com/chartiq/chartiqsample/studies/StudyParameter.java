package com.chartiq.chartiqsample.studies;

import java.io.Serializable;
import java.util.HashMap;

public class StudyParameter implements Serializable {
    String color;
    String heading;
    String name;
    String type;
    Object value;
    Object defaultInput;
    Object defaultOutput;
    HashMap<String, Object> options;
}
