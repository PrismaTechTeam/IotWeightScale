package com.tbossgroup.scale;

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
    private Device selectedDevice;

    public static class Device {
        private String deviceName;
        private int workCentreId;

        public Device(String deviceName, int workCentreId) {
            this.deviceName = deviceName;
            this.workCentreId = workCentreId;
        }

        public String getDeviceName() {
            return deviceName;
        }

        public int getWorkCentreId() {
            return workCentreId;
        }

        @Override
        public String toString() {
            return deviceName; // For displaying in the spinner
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_choice);

        machineSpinner = findViewById(R.id.machine_spinner);
        selectMachineButton = findViewById(R.id.selectMachine);

        selectMachineButton.setEnabled(false);

        machineSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                selectedDevice = (Device) parent.getItemAtPosition(position);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                selectedDevice = null;
            }
        });

        new FetchDevicesTask().execute();

        selectMachineButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (selectedDevice != null) {
                    saveMachineSelection(selectedDevice);
                    Toast.makeText(ActivityChoice.this, "Machine Selected: " + selectedDevice.getDeviceName(), Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    private class FetchDevicesTask extends AsyncTask<Void, Void, List<Device>> {
        private boolean errorOccurred = false;

        @Override
        protected List<Device> doInBackground(Void... voids) {
            List<Device> devices = new ArrayList<>();
            try {
                URL url = new URL("http://192.168.1.137:4554/api/iotDevices/list");
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("GET");
                connection.setRequestProperty("Accept", "application/json");

                int responseCode = connection.getResponseCode();
                if (responseCode == HttpURLConnection.HTTP_OK) {
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
                        String deviceName = jsonObject.getString("deviceName");
                        int workCentreId = jsonObject.getInt("workCentreId");
                        devices.add(new Device(deviceName, workCentreId));
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
        protected void onPostExecute(List<Device> devices) {
            super.onPostExecute(devices);
            if (errorOccurred) {
                Toast.makeText(ActivityChoice.this, "Failed to fetch devices", Toast.LENGTH_LONG).show();
            } else {
                ArrayAdapter<Device> adapter = new ArrayAdapter<>(ActivityChoice.this, android.R.layout.simple_spinner_item, devices);
                adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                machineSpinner.setAdapter(adapter);
                selectMachineButton.setEnabled(true);
            }
        }
    }

    private void saveMachineSelection(Device device) {
        SharedPreferences sharedPreferences = getSharedPreferences("AppPrefs", MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString("DefaultMachine", device.getDeviceName());
        editor.putInt("DefaultWorkCentreId", device.getWorkCentreId());
        editor.apply();
    }

}
