package com.example.myapplication;

import android.os.StrictMode;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class Settings {

    private static Settings settings;

    public static float DEFAULT_ZOOM = 7f;

    public static int userid = -1;

    private Settings() {
        StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
        StrictMode.setThreadPolicy(policy);
    }

    public static Settings getInstance() {
        if (settings == null) {
            settings = new Settings();
        }
        return settings;
    }

    public Connection getConnection() throws ClassNotFoundException, SQLException {
        String DB_URL = "jdbc:jtds:sqlserver://3.87.197.166:1433/MapsGame;user=MapsGame;password=password";
        Class.forName("net.sourceforge.jtds.jdbc.Driver");
        Connection con = DriverManager.getConnection(DB_URL);
        return con;
    }
}
