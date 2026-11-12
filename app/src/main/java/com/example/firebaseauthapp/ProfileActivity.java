package com.example.firebaseauthapp;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.util.Base64;
import android.util.Log;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

public class ProfileActivity extends Activity {

    private ImageView ivProfilePicture;
    private TextView tvFullName, tvEmail;
    private Button btnSignOut;

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

    /**
     * Initializes the profile screen and loads
     * the authenticated user's information.
     *
     * @param savedInstanceState Saved activity state.
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        ivProfilePicture = findViewById(R.id.ivProfilePicture);
        tvFullName = findViewById(R.id.tvFullName);
        tvEmail = findViewById(R.id.tvEmail);
        btnSignOut = findViewById(R.id.btnSignOut);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        FirebaseUser user = mAuth.getCurrentUser();

        if (user == null) {
            navigateToSignIn();
            return;
        }

        loadUserProfile(user.getUid());

        btnSignOut.setOnClickListener(v -> signOut());
    }

    /**
     * Retrieves the user's profile information
     * from Cloud Firestore.
     *
     * @param uid Firebase Authentication user ID.
     */
    private void loadUserProfile(String uid) {

        db.collection("users")
                .document(uid)
                .get()
                .addOnSuccessListener(document -> {

                    if (!document.exists()) {
                        Toast.makeText(this,
                                "Profile not found",
                                Toast.LENGTH_SHORT).show();
                        return;
                    }

                    String firstName =
                            document.getString("firstName");

                    String lastName =
                            document.getString("lastName");

                    String email =
                            document.getString("email");

                    String encodedImage =
                            document.getString("profileImage");

                    String fullName =
                            (firstName == null ? "" : firstName)
                                    + " "
                                    + (lastName == null ? "" : lastName);

                    tvFullName.setText(fullName.trim());
                    tvEmail.setText(email == null ? "" : email);

                    if (encodedImage != null &&
                            !encodedImage.isEmpty()) {

                        displayProfileImage(encodedImage);
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e("ProfileActivity",
                            "Failed to load profile", e);

                    Toast.makeText(this,
                            "Failed to load profile",
                            Toast.LENGTH_LONG).show();
                });
    }

    /**
     * Decodes a Base64 JPEG image and displays it
     * in the profile ImageView.
     *
     * @param encodedImage Base64-encoded profile image.
     */
    private void displayProfileImage(String encodedImage) {

        try {
            byte[] imageBytes = Base64.decode(
                    encodedImage,
                    Base64.DEFAULT
            );

            Bitmap bitmap = BitmapFactory.decodeByteArray(
                    imageBytes,
                    0,
                    imageBytes.length
            );

            if (bitmap != null) {
                ivProfilePicture.setImageBitmap(bitmap);
            }

        } catch (IllegalArgumentException e) {
            Log.e("ProfileActivity",
                    "Invalid profile image", e);
        }
    }

    /**
     * Signs the current user out of Firebase
     * and returns to the Sign In screen.
     */
    private void signOut() {
        mAuth.signOut();
        navigateToSignIn();
    }

    /**
     * Navigates to Sign In and clears previous
     * activities from the back stack.
     */
    private void navigateToSignIn() {

        Intent intent = new Intent(
                ProfileActivity.this,
                SignInActivity.class
        );

        intent.setFlags(
                Intent.FLAG_ACTIVITY_NEW_TASK |
                        Intent.FLAG_ACTIVITY_CLEAR_TASK
        );

        startActivity(intent);
        finish();
    }
}
