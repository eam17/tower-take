package com.example.myapplication;

import android.animation.Animator;
import android.content.Intent;
import android.os.CountDownTimer;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.ViewPropertyAnimator;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import static android.view.View.VISIBLE;

public class MainActivity extends AppCompatActivity {

    private android.support.design.widget.TextInputEditText txtUsername;
    private android.support.design.widget.TextInputEditText txtPassword;

    private ImageView iconImageView;
    private RelativeLayout rootView, afterAnimationView;
    private String TAG = "MainActivity";


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        //Full screen
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);

        setContentView(R.layout.activity_main);

        //Find and initialize layout
        txtUsername = findViewById(R.id.txtUsername);
        txtPassword = findViewById(R.id.txtPassword);

        iconImageView = findViewById(R.id.iconImageView);
        rootView = findViewById(R.id.rootView);
        afterAnimationView = findViewById(R.id.afterAnimationView);

        Button btnLogin = findViewById(R.id.btnLogin);
        btnLogin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //Immediately hash password
                String strUsername = txtUsername.getText().toString();
                String strPassword = hash(txtPassword.getText().toString());

                //Build sql prepared statement
                String strStatement = "select * from account where username = ? and password = ?";
                try {
                    PreparedStatement psSelect = Settings.getInstance().getConnection().prepareStatement(strStatement);
                    psSelect.setString(1, strUsername);
                    psSelect.setString(2, strPassword);
                    psSelect.execute();

                    //Get sql results
                    ResultSet rsSelect = psSelect.getResultSet();
                    while (rsSelect.next()) {

                        int id = rsSelect.getInt("id");
                        if (id != -1) {
                            //If result exists, login
                            Intent intent = new Intent(v.getContext(), com.example.myapplication.TowerTaker.class);
                            Settings.userid = id;
                            intent.putExtra("id", id);
                            startActivity(intent);

                        } else {
                            //User failed to login
                            Toast.makeText(getApplicationContext(), "Incorrect username or password.", Toast.LENGTH_LONG).show();
                        }
                    }
                    rsSelect.close();
                    psSelect.close();

                    Log.v(this.getClass().toString(), "Login complete.");
                } catch (ClassNotFoundException e) {
                    e.printStackTrace();
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }
        });

        Button btnRegister = findViewById(R.id.btnRegister);
        btnRegister.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //Determine if user proposed password meets requirements
                String strUsername = txtUsername.getText().toString();
                String strPassword = txtPassword.getText().toString();
                if (strPassword.length() < 4) {
                    Toast.makeText(getApplicationContext(), "Password requires at least 4 characters.", Toast.LENGTH_LONG).show();
                    return;
                }
                String strHashedPassword = hash(strPassword);

                //Build sql prepared statement
                String strStatement = "insert into account (username, password) values (?, ?)";
                try {
                    PreparedStatement psInsert = Settings.getInstance().getConnection().prepareStatement(strStatement);
                    psInsert.setString(1, strUsername);
                    psInsert.setString(2, strHashedPassword);
                    int result = psInsert.executeUpdate();
                    if (result != 0) {
                        Log.d("", "onClick: Error inserting account.");
                    }

                    psInsert.close();

                    //TODO: check result code to make sure it went through

                    Toast.makeText(getApplicationContext(), "Account successfully created.", Toast.LENGTH_LONG).show();
                    //Toast.makeText(getApplicationContext(), "Username already exists.", Toast.LENGTH_LONG).show();
                    //Toast.makeText(getApplicationContext(), "Password does not meet requirements.", Toast.LENGTH_LONG).show();
                } catch (ClassNotFoundException e) {
                    e.printStackTrace();
                } catch (SQLException e) {
                    e.printStackTrace();
                }

                Log.v(this.getClass().toString(), "Register complete.");
            }
        });

        //Time until main login screen shows up
        new CountDownTimer(1000, 1000) {

            @Override
            public void onTick(long millisUntilFinished) {


            }

            @Override
            public void onFinish() {
                startAnimation();
            }
        }.start();


    }//End OnCreate

    static String hash(String str) {
        try {
            //This is an implemented standard for the SHA-512 hashing
            MessageDigest md = MessageDigest.getInstance("SHA-512");
            byte[] digest = md.digest(str.getBytes(StandardCharsets.UTF_8));
            char[] hex = new char[digest.length * 2];
            for (int i = 0; i < digest.length; i++) {
                hex[2 * i] = "0123456789abcdef".charAt((digest[i] & 0xf0) >> 4);
                hex[2 * i + 1] = "0123456789abcdef".charAt(digest[i] & 0x0f);
            }
            return new String(hex);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }

    private void startAnimation() {
        ViewPropertyAnimator viewPropertyAnimator = iconImageView.animate();
        viewPropertyAnimator.x(50f);
        viewPropertyAnimator.y(100f);
        viewPropertyAnimator.setDuration(1000);
        viewPropertyAnimator.setListener(new Animator.AnimatorListener() {
            @Override
            public void onAnimationStart(Animator animation) {
            }

            @Override
            public void onAnimationEnd(Animator animation) {
                iconImageView.setImageResource(R.drawable.tower_gray);
                rootView.setBackgroundColor(ContextCompat.getColor(MainActivity.this, R.color.colorSplashText));
                afterAnimationView.setVisibility(VISIBLE);
            }

            @Override
            public void onAnimationCancel(Animator animation) {

            }

            @Override
            public void onAnimationRepeat(Animator animation) {

            }
        });
    }



}
