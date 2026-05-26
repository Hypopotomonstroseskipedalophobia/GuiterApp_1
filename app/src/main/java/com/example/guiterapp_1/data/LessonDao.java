package com.example.guiterapp_1.data;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Transaction;
import androidx.room.Update;

import java.util.List;

@Dao
public interface LessonDao {
    @Insert
    long insert(Lesson lesson);

    @Update
    void update(Lesson lesson);

    @Delete
    void delete(Lesson lesson);

    @Query("SELECT * FROM learning_sessions WHERE lesson_length >= 10 OR notes = 'Manual Entry' ORDER BY id DESC")
    List<Lesson> getAllLessons();

    @Transaction
    @Query("SELECT * FROM learning_sessions WHERE lesson_length >= 10 OR notes = 'Manual Entry' ORDER BY id DESC")
    List<LessonWithExercise> getAllLessonsWithExercise();

    @Query("SELECT * FROM learning_sessions WHERE id = :id")
    Lesson getLessonById(int id);

    @Query("SELECT * FROM learning_sessions WHERE lesson_date = :date LIMIT 1")
    Lesson getLessonByDate(String date);

    @Transaction
    @Query("SELECT * FROM learning_sessions WHERE lesson_date = :date AND (lesson_length >= 10 OR notes = 'Manual Entry') ORDER BY start_time ASC")
    List<LessonWithExercise> getLessonsWithExerciseByDate(String date);

    @Query("SELECT * FROM learning_sessions WHERE lesson_date = :date AND (lesson_length >= 10 OR notes = 'Manual Entry') ORDER BY start_time ASC")
    List<Lesson> getLessonsByDate(String date);

    @Query("SELECT * FROM learning_sessions WHERE lesson_date LIKE :monthPattern AND (lesson_length >= 10 OR notes = 'Manual Entry')")
    List<Lesson> getLessonsByMonth(String monthPattern);

    @Query("SELECT * FROM learning_sessions WHERE lesson_date >= :startDate AND (lesson_length >= 10 OR notes = 'Manual Entry') ORDER BY lesson_date ASC")
    List<Lesson> getLessonsAfterDate(String startDate);
}
