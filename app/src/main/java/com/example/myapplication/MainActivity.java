package com.example.myapplication;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class MainActivity extends AppCompatActivity {

    private TextView txtUsername;
    private TextView txtPassword;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //Find and initialize layout
        txtUsername = (TextView)findViewById(R.id.txtUsername);
        txtPassword = (TextView)findViewById(R.id.txtPassword);

        Button btnLogin = (Button)findViewById(R.id.btnLogin);
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
                            Intent intent = new Intent(v.getContext(), com.example.myapplication.MapsActivity.class);
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

        Button btnRegister = (Button)findViewById(R.id.btnRegister);
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
    }

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
}
