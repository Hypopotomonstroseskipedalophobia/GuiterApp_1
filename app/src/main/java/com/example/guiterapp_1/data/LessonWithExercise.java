package com.example.guiterapp_1.data;

import androidx.room.Embedded;
import androidx.room.Relation;

public class LessonWithExercise {
    @Embedded
    public Lesson lesson;

    @Relation(
            parentColumn = "exercise_id",
            entityColumn = "id"
    )
    public Exercise exercise;
}
