package com.example.firebaseauthapp;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.util.Patterns;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

/**
 * Handles user sign-in using Firebase Authentication.
 * Users can log in with email and password or navigate
 * to the registration screen to create an account.
 */
public class SignInActivity extends Activity {

    private EditText etEmail;
    private EditText etPassword;
    private Button btnSignIn;
    private TextView tvSignUp;

    private FirebaseAuth mAuth;

    /**
     * Initializes the sign-in screen, Firebase Authentication,
     * and button click listeners.
     *
     * @param savedInstanceState Saved activity state.
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_sign_in);

        // Initialize Firebase Authentication
        mAuth = FirebaseAuth.getInstance();

        // Connect Java variables to XML elements
        etEmail = findViewById(R.id.etEmail);
        etPassword = findViewById(R.id.etPassword);
        btnSignIn = findViewById(R.id.btnSignIn);
        tvSignUp = findViewById(R.id.tvSignUp);

        // Sign In button
        btnSignIn.setOnClickListener(v -> signInUser());

        // Navigate to Sign Up screen
        tvSignUp.setOnClickListener(v -> {
            Intent intent = new Intent(
                    SignInActivity.this,
                    SignUpActivity.class
            );

            startActivity(intent);
        });
    }

    /**
     * Validates the user's email and password before
     * attempting to authenticate with Firebase.
     */
    private void signInUser() {

        String email = etEmail.getText()
                .toString().trim();

        String password = etPassword.getText()
                .toString();

        // Check for empty fields
        if (TextUtils.isEmpty(email)) {
            etEmail.setError("Email is required");
            etEmail.requestFocus();
            return;
        }

        if (TextUtils.isEmpty(password)) {
            etPassword.setError("Password is required");
            etPassword.requestFocus();
            return;
        }

        // Validate email format
        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            etEmail.setError("Enter a valid email address");
            etEmail.requestFocus();
            return;
        }

        // Prevent repeated login requests
        btnSignIn.setEnabled(false);

        // Authenticate user with Firebase
        mAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {

                    btnSignIn.setEnabled(true);

                    if (task.isSuccessful()) {

                        FirebaseUser user = mAuth.getCurrentUser();

                        if (user != null) {

                            Toast.makeText(
                                    SignInActivity.this,
                                    "Login successful!",
                                    Toast.LENGTH_SHORT
                            ).show();

                            navigateToProfile();

                        } else {
                            Toast.makeText(
                                    SignInActivity.this,
                                    "Unable to retrieve user",
                                    Toast.LENGTH_LONG
                            ).show();
                        }

                    } else {

                        Exception exception = task.getException();

                        Log.e(
                                "SignInActivity",
                                "Firebase sign-in failed",
                                exception
                        );

                        String errorMessage =
                                exception != null
                                        ? exception.getMessage()
                                        : "Unknown error";

                        Toast.makeText(
                                SignInActivity.this,
                                "Login failed: " + errorMessage,
                                Toast.LENGTH_LONG
                        ).show();
                    }
                });
    }

    /**
     * Navigates to the authenticated user's profile
     * and clears the previous activity history.
     */
    private void navigateToProfile() {

        Intent intent = new Intent(
                SignInActivity.this,
                ProfileActivity.class
        );

        intent.setFlags(
                Intent.FLAG_ACTIVITY_NEW_TASK |
                        Intent.FLAG_ACTIVITY_CLEAR_TASK
        );

        startActivity(intent);
        finish();
    }
}
