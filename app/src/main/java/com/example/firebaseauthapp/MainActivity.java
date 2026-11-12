package com.example.firebaseauthapp;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;

public class MainActivity extends Activity {

    /**
     * Initializes the welcome screen and
     * configures the Get Started button.
     *
     * @param savedInstanceState Saved activity state.
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Button btnGetStarted =
                findViewById(R.id.btnGetStarted);

        btnGetStarted.setOnClickListener(v -> {

            Intent intent = new Intent(
                    MainActivity.this,
                    SignInActivity.class
            );

            startActivity(intent);
        });
    }
}
