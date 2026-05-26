package com.example.guiterapp_1.data;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "instruments")
public class Instrument {

    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "instrument_id")
    public int instrumentId;

    @ColumnInfo(name = "name")
    public String name;

    public Instrument(String name) {
        this.name = name;
    }
}
