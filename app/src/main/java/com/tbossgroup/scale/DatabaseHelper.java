package com.tbossgroup.scale;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

public class DatabaseHelper extends SQLiteOpenHelper {
    private static final String DATABASE_NAME = "IOTDb.db";
    private static final int DATABASE_VERSION = 1;

    public DatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        String CREATE_TABLE = "CREATE TABLE ApiKeys (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "apikey TEXT)";
        db.execSQL(CREATE_TABLE);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // Upgrade logic goes here
    }

    public static class Device {
        private String name;
        private int id;

        public Device(int id, String name) {
            this.id = id;
            this.name = name;
        }

        public String getName() {
            return name;
        }

        public int getId() {
            return id;
        }

        @Override
        public String toString() {
            return name; // This will ensure the ArrayAdapter shows the device name
        }
    }




    public List<Device> getDeviceNames(){
        List<Device> devices = new ArrayList<>();
        Connection connection = null;
        Statement statement = null;
        ResultSet resultSet = null;

        try {
            // Replace with your database connection details
            String url = "jdbc:sqlserver://192.168.1.92;databaseName=MES_Developer_EM30_2;user=sa;password=rs6663;";

            connection = DriverManager.getConnection(url);
            statement = connection.createStatement();
            String sql = "SELECT DeviceName FROM IOTDevices"; // Simplified query
            resultSet = statement.executeQuery(sql);

            while (resultSet.next()) {
                int id = resultSet.getInt("Id");
                String name = resultSet.getString("DeviceName");
                devices.add(new Device(id, name));
            }
        } catch (Exception e) {
            e.printStackTrace(); // Handle exceptions appropriately
        } finally {
            try {
                if (resultSet != null) resultSet.close();
                if (statement != null) statement.close();
                if (connection != null) connection.close();
            } catch (Exception e) {
                e.printStackTrace(); // Handle exceptions appropriately
            }
        }

        return devices;
    }


}
