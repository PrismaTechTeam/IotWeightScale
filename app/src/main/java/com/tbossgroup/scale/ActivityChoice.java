package com.tbossgroup.scale;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

public class ActivityChoice extends AppCompatActivity {
    private Spinner machineSpinner;
    private Button selectMachineButton;
    private String selectedMachine; // This will hold the selected machine

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_choice);

        machineSpinner = findViewById(R.id.machine_spinner);
        selectMachineButton = findViewById(R.id.selectMachine);

        // Initially disable the button until we have the machines loaded
        selectMachineButton.setEnabled(false);

        // Set up the spinner selection listener
        machineSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                selectedMachine = parent.getItemAtPosition(position).toString();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                selectedMachine = null; // Clear selection
            }
        });

        // Start the async task to fetch devices
        new FetchDevicesTask().execute();

        selectMachineButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Check if a machine is selected
                if (selectedMachine != null) {
                    saveMachineSelection(selectedMachine);
                    // You can start another activity or perform other actions here
                }
            }
        });
    }

    private class FetchDevicesTask extends AsyncTask<Void, Void, List<String>> {
        private boolean errorOccurred = false;

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            // Show a loading message or spinner (not implemented here)
        }

        @Override
        protected List<String> doInBackground(Void... voids) {
            List<String> devices = new ArrayList<>();
            try {
                // The exact URL for your API
                URL url = new URL("http://192.168.1.137:4554/api/iotDevices/list");
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("GET");
                connection.setRequestProperty("accept", "text/plain");
                connection.setRequestProperty("Content-Type", "application/json; utf-8");
                connection.setRequestProperty("Accept", "application/json");
                connection.setDoOutput(true);


                int responseCode = connection.getResponseCode();
                if (responseCode == HttpURLConnection.HTTP_OK) {
                    BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                    StringBuilder response = new StringBuilder();
                    String line;
                    while ((line = reader.readLine()) != null) {
                        response.append(line);
                    }
                    reader.close();

                    // Assuming your API returns a JSON array of objects
                    JSONArray jsonArray = new JSONArray(response.toString());
                    for (int i = 0; i < jsonArray.length(); i++) {
                        JSONObject jsonObject = jsonArray.getJSONObject(i);
                        // Extract device name
                        devices.add(jsonObject.getString("DeviceName"));
                    }
                } else {
                    errorOccurred = true;
                }
            } catch (Exception e) {
                errorOccurred = true;
                e.printStackTrace();
            }
            return devices;
        }


        @Override
        protected void onPostExecute(List<String> devices) {
            super.onPostExecute(devices);
            if (errorOccurred) {
                Toast.makeText(ActivityChoice.this, "Failed to fetch devices", Toast.LENGTH_LONG).show();
                return;
            }
            // Set up the adapter with the fetched devices
            ArrayAdapter<String> adapter = new ArrayAdapter<>(ActivityChoice.this, android.R.layout.simple_spinner_item, devices);
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            machineSpinner.setAdapter(adapter);
            selectMachineButton.setEnabled(true); // Enable the button now that we have the list
        }
    }

    private void saveMachineSelection(String machine) {
        // Save the selected machine name to SharedPreferences
        SharedPreferences sharedPreferences = getSharedPreferences("AppPrefs", MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString("DefaultMachine", machine);
        editor.apply();
    }
}
