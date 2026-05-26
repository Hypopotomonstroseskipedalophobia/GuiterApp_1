package com.example.guiterapp_1.data;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.PrimaryKey;

@Entity(
        tableName = "learning_sessions",
        foreignKeys = @ForeignKey(
                entity = Instrument.class,
                parentColumns = "instrument_id",
                childColumns = "instrument_id",
                onDelete = ForeignKey.SET_NULL
        )
)
public class Lesson {

    @PrimaryKey(autoGenerate = true)
    public int id;

    @ColumnInfo(name = "user_id")
    public String userId;

    @ColumnInfo(name = "lesson_date")
    public String lessonDate;
    @ColumnInfo(name = "lesson_length")
    public long lessonLength;

    @ColumnInfo(name = "start_time")
    public String startTime;

    @ColumnInfo(name = "rating")
    public int rating;

    @ColumnInfo(name = "notes")
    public String notes;

    @ColumnInfo(name = "video_path")
    public String videoPath;

    @ColumnInfo(name = "audio_path")
    public String audioPath;

    @ColumnInfo(name = "exercise_id")
    public Integer exerciseId;

    @ColumnInfo(name = "instrument_id")
    public Integer instrumentId;

    public Lesson(String userId, String lessonDate, long lessonLength, String startTime, int rating, String notes) {
        this.userId = userId;
        this.lessonDate = lessonDate;
        this.lessonLength = lessonLength;
        this.startTime = startTime;
        this.rating = rating;
        this.notes = notes;
    }
}
