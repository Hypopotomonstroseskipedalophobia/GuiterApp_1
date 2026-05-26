package com.example.guiterapp_1.data;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;

@Dao
public interface UserDao {
    @Insert
    void register(User user);

    @Query("SELECT * FROM users WHERE user_email = :email AND user_password = :password LIMIT 1")
    User login(String email, String password);

    @Query("SELECT * FROM users WHERE userId = :userId LIMIT 1")
    User getUserById(int userId);

    @Query("SELECT COUNT(*) FROM users")
    int getUserCount();
}
