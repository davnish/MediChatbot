package com.example.beast.weka;

import java.util.Arrays;

public class WekaData {
    public int label;
    public String[] wekaData;

    public WekaData(int label, String[] wekaData) {
        this.label = label;
        this.wekaData = wekaData;
    }

    @Override
    public String toString() {
        return "WekaData{" +
                "label=" + label +
                ", wekaData=" + Arrays.toString(wekaData) +
                '}';
    }
}

