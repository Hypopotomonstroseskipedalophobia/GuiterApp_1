package com.example.guiterapp_1.data;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "exercises")
public class Exercise {
    @PrimaryKey(autoGenerate = true)
    public int id;
    @ColumnInfo(name = "instrument_id")
    public int instrument_id;
    @ColumnInfo(name = "title")
    public String title;
    @ColumnInfo(name = "type")
    public String type;
    @ColumnInfo(name = "description")
    public String description;
    @ColumnInfo(name = "current_bpm")
    public int current_bpm;
    @ColumnInfo(name = "target_bpm")
    public int target_bpm;
}
