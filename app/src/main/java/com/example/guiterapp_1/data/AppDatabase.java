package com.example.guiterapp_1.data;

import android.content.Context;
import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;

@Database(entities = {Lesson.class, User.class, Exercise.class, Instrument.class}, version = 8)
public abstract class AppDatabase extends RoomDatabase {
    private static AppDatabase instance;
    public abstract LessonDao lessonDao();
    public abstract UserDao userDao();
    public abstract ExerciseDao exerciseDao();
    public abstract InstrumentDao instrumentDao();


    public static synchronized AppDatabase getInstance(Context context) {
        if (instance == null) {
            instance = Room.databaseBuilder(context.getApplicationContext(),
                    AppDatabase.class, "GuitarAppDB")
                    .fallbackToDestructiveMigration()
                    .build();
        }
        return instance;
    }
}
