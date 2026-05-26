package com.example.guiterapp_1;

public class ChordPosition {
    private final String name;
    private final int[] frets;

    public ChordPosition(String name, int[] frets) {
        this.name = name;
        this.frets = frets;
    }

    public String getName() {
        return name;
    }

    public int[] getFrets() {
        return frets;
    }
}
