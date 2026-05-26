package com.example.guiterapp_1.data;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import java.util.List;

@Dao
public interface InstrumentDao {
    @Insert
    void insert(Instrument instrument);

    @Update
    void update(Instrument instrument);

    @Delete
    void delete(Instrument instrument);

    @Query("SELECT * FROM instruments")
    List<Instrument> getAllInstruments();

    @Query("SELECT * FROM instruments WHERE instrument_id = :id")
    Instrument getInstrumentById(int id);
}
