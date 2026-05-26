package com.example.guiterapp_1.data;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "users")
public class User {
    @PrimaryKey(autoGenerate = true)
    public int userId;

    @ColumnInfo(name = "username")
    public String username;

    @ColumnInfo(name = "user_email")
    public String email;

    @ColumnInfo(name = "user_password")
    public String password; // Encrypted

    public User(String username, String email, String password) {
        this.username = username;
        this.email = email;
        this.password = password;
    }
}
