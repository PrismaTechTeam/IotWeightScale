package com.tbossgroup.scale;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.Spinner;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

public class ActivityChoice extends AppCompatActivity {
    private DatabaseHelper db;
    private Spinner machineSpinner;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_choice);

        machineSpinner = findViewById(R.id.machine_spinner);
        new FetchDevicesTask().execute();

        Button selectMachineButton = findViewById(R.id.selectMachine);
        selectMachineButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Handle the selection of the machine here
                String selectedMachine = machineSpinner.getSelectedItem().toString();
                saveMachineSelection(selectedMachine);

                // You can start another activity or perform other actions here
            }
        });
    }

    private class FetchDevicesTask extends AsyncTask<Void, Void, List<String>> {

        @Override
        protected List<String> doInBackground(Void... voids) {
            List<String> devices = new ArrayList<>();
            try {
                URL url = new URL("http://192.168.1.92:7025/api/iotDevices");
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("GET");
                connection.setRequestProperty("accept", "text/plain");

                BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                StringBuilder response = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    response.append(line);
                }
                reader.close();

                JSONArray jsonArray = new JSONArray(response.toString());
                for (int i = 0; i < jsonArray.length(); i++) {
                    JSONObject jsonObject = jsonArray.getJSONObject(i);
                    String name = jsonObject.getString("DeviceName"); // Assuming the field is named "DeviceName"
                    devices.add(name);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            return devices;
        }

        @Override
        protected void onPostExecute(List<String> devices) {
            super.onPostExecute(devices);
            ArrayAdapter<String> adapter = new ArrayAdapter<>(ActivityChoice.this, android.R.layout.simple_spinner_item, devices);
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            machineSpinner.setAdapter(adapter);
        }
    }

    private void saveMachineSelection(String machine) {
        SharedPreferences sharedPreferences = getSharedPreferences("AppPrefs", MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString("DefaultMachine", machine);
        editor.apply();
    }



}
