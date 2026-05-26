package com.example.guiterapp_1;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.guiterapp_1.data.AppDatabase;
import com.example.guiterapp_1.data.User;
import com.example.guiterapp_1.databinding.ActivityLoginBinding;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.concurrent.Executors;

public class LoginActivity extends AppCompatActivity {

    private ActivityLoginBinding binding;
    private boolean isLoginMode = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityLoginBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        binding.loginButton.setOnClickListener(v -> handleAuth());

        binding.switchModeText.setOnClickListener(v -> {
            isLoginMode = !isLoginMode;
            binding.usernameEditText.setVisibility(isLoginMode ? View.GONE : View.VISIBLE);
            binding.loginButton.setText(isLoginMode ? "Login" : "Register");
            binding.switchModeText.setText(isLoginMode ? 
                "Don't have an account? Register" : "Already have an account? Login");
        });
    }

    private void handleAuth() {
        String email = binding.emailEditText.getText().toString();
        String password = binding.passwordEditText.getText().toString();
        
        if (email.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show();
            return;
        }

        String hashedPassword = hashPassword(password);

        Executors.newSingleThreadExecutor().execute(() -> {
            AppDatabase db = AppDatabase.getInstance(this);
            if (isLoginMode) {
                User user = db.userDao().login(email, hashedPassword);
                if (user != null) {
                    saveLoginState(user.userId);
                    navigateToMain();
                } else {
                    runOnUiThread(() -> Toast.makeText(this, "Invalid credentials", Toast.LENGTH_SHORT).show());
                }
            } else {
                String username = binding.usernameEditText.getText().toString();
                if (username.isEmpty()) {
                    runOnUiThread(() -> Toast.makeText(this, "Enter username", Toast.LENGTH_SHORT).show());
                    return;
                }
                User newUser = new User(username, email, hashedPassword);
                db.userDao().register(newUser);
                runOnUiThread(() -> {
                    Toast.makeText(this, "Registered! Please login", Toast.LENGTH_SHORT).show();
                    binding.switchModeText.performClick();
                });
            }
        });
    }

    private String hashPassword(String password) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(password.getBytes(StandardCharsets.UTF_8));
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (Exception e) {
            return password; // Fallback
        }
    }

    private void saveLoginState(int userId) {
        SharedPreferences pref = getSharedPreferences("GuitarApp", MODE_PRIVATE);
        pref.edit().putInt("current_user_id", userId).apply();
    }

    private void navigateToMain() {
        startActivity(new Intent(this, MainActivity.class));
        finish();
    }
}
