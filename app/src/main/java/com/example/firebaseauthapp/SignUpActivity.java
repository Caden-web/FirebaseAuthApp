package com.example.firebaseauthapp;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Base64;
import android.util.Patterns;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

public class SignUpActivity extends androidx.activity.ComponentActivity {

    private EditText etFirstName, etLastName, etEmail;
    private EditText etPassword, etConfirmPassword;
    private ImageView ivProfile;
    private Button btnSignUp, btnSelectImage;
    private TextView tvSignIn;

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private String encodedImage = "";

    private ActivityResultLauncher<String> imagePicker;

    /**
     * Initializes registration fields, Firebase services,
     * and user interaction listeners.
     *
     * @param savedInstanceState Saved activity state.
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        imagePicker = registerForActivityResult(
                new ActivityResultContracts.GetContent(),
                this::processSelectedImage
        );

        setContentView(R.layout.activity_sign_up);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        etFirstName = findViewById(R.id.etFirstName);
        etLastName = findViewById(R.id.etLastName);
        etEmail = findViewById(R.id.etEmail);
        etPassword = findViewById(R.id.etPassword);
        etConfirmPassword = findViewById(R.id.etConfirmPassword);

        ivProfile = findViewById(R.id.ivProfile);
        btnSignUp = findViewById(R.id.btnSignUp);
        btnSelectImage = findViewById(R.id.btnSelectImage);
        tvSignIn = findViewById(R.id.tvSignIn);

        btnSelectImage.setOnClickListener(v ->
                imagePicker.launch("image/*"));

        btnSignUp.setOnClickListener(v -> registerUser());

        tvSignIn.setOnClickListener(v -> {
            startActivity(new Intent(
                    SignUpActivity.this,
                    SignInActivity.class
            ));
            finish();
        });
    }

    /**
     * Processes a selected profile image, resizes it,
     * and converts it into a Base64 JPEG string.
     *
     * @param uri URI of the selected image.
     */
    private void processSelectedImage(Uri uri) {
        if (uri == null) {
            return;
        }

        try (InputStream stream =
                     getContentResolver().openInputStream(uri)) {

            Bitmap original = BitmapFactory.decodeStream(stream);

            if (original == null) {
                throw new Exception("Invalid image");
            }

            int width = original.getWidth();
            int height = original.getHeight();

            float scale = Math.min(
                    1f,
                    Math.min(300f / width, 300f / height)
            );

            Bitmap resized = Bitmap.createScaledBitmap(
                    original,
                    Math.max(1, Math.round(width * scale)),
                    Math.max(1, Math.round(height * scale)),
                    true
            );

            ByteArrayOutputStream output =
                    new ByteArrayOutputStream();

            resized.compress(
                    Bitmap.CompressFormat.JPEG,
                    60,
                    output
            );

            byte[] imageBytes = output.toByteArray();

            if (imageBytes.length > 500_000) {
                throw new Exception("Image is too large");
            }

            encodedImage = Base64.encodeToString(
                    imageBytes,
                    Base64.NO_WRAP
            );

            ivProfile.setImageBitmap(resized);

        } catch (Exception e) {
            encodedImage = "";

            Toast.makeText(
                    this,
                    "Could not process selected image",
                    Toast.LENGTH_SHORT
            ).show();
        }
    }

    /**
     * Validates registration information and creates
     * a new account using Firebase Authentication.
     */
    private void registerUser() {

        String firstName = etFirstName.getText()
                .toString().trim();
        String lastName = etLastName.getText()
                .toString().trim();
        String email = etEmail.getText()
                .toString().trim();
        String password = etPassword.getText()
                .toString();
        String confirmPassword = etConfirmPassword.getText()
                .toString();

        if (TextUtils.isEmpty(firstName) ||
                TextUtils.isEmpty(lastName) ||
                TextUtils.isEmpty(email) ||
                TextUtils.isEmpty(password) ||
                TextUtils.isEmpty(confirmPassword)) {

            Toast.makeText(
                    this,
                    "Please fill in all fields",
                    Toast.LENGTH_SHORT
            ).show();
            return;
        }

        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            Toast.makeText(this,
                    "Please enter a valid email",
                    Toast.LENGTH_SHORT).show();
            return;
        }

        if (password.length() < 6) {
            Toast.makeText(this,
                    "Password must be at least 6 characters",
                    Toast.LENGTH_SHORT).show();
            return;
        }

        if (!password.equals(confirmPassword)) {
            Toast.makeText(this,
                    "Passwords do not match",
                    Toast.LENGTH_SHORT).show();
            return;
        }

        if (encodedImage.isEmpty()) {
            Toast.makeText(this,
                    "Please select a profile photo",
                    Toast.LENGTH_SHORT).show();
            return;
        }

        btnSignUp.setEnabled(false);

        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {

                    if (task.isSuccessful()) {
                        FirebaseUser user = mAuth.getCurrentUser();

                        if (user != null) {
                            saveUserProfile(
                                    user.getUid(),
                                    firstName,
                                    lastName,
                                    email
                            );
                        } else {
                            btnSignUp.setEnabled(true);
                            Toast.makeText(this,
                                    "Could not retrieve new user",
                                    Toast.LENGTH_LONG).show();
                        }

                    } else {
                        btnSignUp.setEnabled(true);

                        Toast.makeText(this,
                                "Registration failed: " +
                                        (task.getException() != null
                                                ? task.getException().getMessage()
                                                : "Unknown error"),
                                Toast.LENGTH_LONG).show();
                    }
                });
    }

    /**
     * Saves a newly registered user's profile information
     * and compressed profile photo in Cloud Firestore.
     *
     * @param uid Firebase Authentication user ID.
     * @param firstName User's first name.
     * @param lastName User's last name.
     * @param email User's email address.
     */
    private void saveUserProfile(
            String uid,
            String firstName,
            String lastName,
            String email) {

        Map<String, Object> userData = new HashMap<>();

        userData.put("firstName", firstName);
        userData.put("lastName", lastName);
        userData.put("email", email);
        userData.put("profileImage", encodedImage);

        db.collection("users")
                .document(uid)
                .set(userData)
                .addOnSuccessListener(unused -> {

                    btnSignUp.setEnabled(true);

                    Toast.makeText(this,
                            "Account created successfully!",
                            Toast.LENGTH_LONG).show();

                    mAuth.signOut();

                    Intent intent = new Intent(
                            SignUpActivity.this,
                            SignInActivity.class
                    );

                    intent.addFlags(
                            Intent.FLAG_ACTIVITY_CLEAR_TOP |
                                    Intent.FLAG_ACTIVITY_SINGLE_TOP
                    );

                    startActivity(intent);
                    finish();
                })
                .addOnFailureListener(e -> {
                    btnSignUp.setEnabled(true);

                    Toast.makeText(this,
                            "Account created, but profile save failed: "
                                    + e.getMessage(),
                            Toast.LENGTH_LONG).show();
                });
    }
}
