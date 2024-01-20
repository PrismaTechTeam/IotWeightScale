package com.tbossgroup.scale;

import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Toast;

public class ActivitySettings extends AppCompatActivity {

    private ImageButton button2;
    private EditText apiAddressInput;
    private DatabaseHelper db;
    private EditText editTextApiKey;
    private ApiKeyDAO apiKeyDAO;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);


        apiAddressInput = findViewById(R.id.editText);
        Button setApiButton = findViewById(R.id.button);


        editTextApiKey = findViewById(R.id.editText);
        Button btnSave = findViewById(R.id.button);
        apiKeyDAO = new ApiKeyDAO(this);

        btnSave.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String apiKey = editTextApiKey.getText().toString();
                apiKeyDAO.insertApiKey(apiKey);
                Toast.makeText(ActivitySettings.this, "API Key saved", Toast.LENGTH_SHORT).show();
            }
        });



        String CurrectApi = getApiAddress();

        if(CurrectApi != null){
            editTextApiKey.setText(CurrectApi);
        }


        // Optional: Load the saved API key when the activity starts
        String savedApiKey = apiKeyDAO.getApiKey();
        if (savedApiKey != null) {
            editTextApiKey.setText(savedApiKey);
        }


        //Access MainActivity Using Back Button
        ImageButton button2 = findViewById(R.id.backButton);
        button2.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent i = new Intent(ActivitySettings.this, MainActivity.class);
                startActivity(i);
            }

        });

        setApiButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String apiAddress = apiAddressInput.getText().toString();
                saveApiAddress(apiAddress);

            }
        });


    }


    private void saveApiAddress(String apiAddress) {
        SharedPreferences sharedPreferences = getSharedPreferences("AppSettings", MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString("API_ADDRESS", apiAddress);
        editor.apply();
        Toast.makeText(this, "API Address saved", Toast.LENGTH_SHORT).show();
    }

    private String getApiAddress() {
        SharedPreferences sharedPreferences = getSharedPreferences("AppSettings", MODE_PRIVATE);
        return sharedPreferences.getString("API_ADDRESS", ""); // Replace with a default API address if necessary
    }

    public class ApiKeyDAO {
        private DatabaseHelper dbHelper;

        public ApiKeyDAO(Context context) {
            dbHelper = new DatabaseHelper(context);
        }

        public void insertApiKey(String apiKey) {
            SQLiteDatabase db = dbHelper.getWritableDatabase();
            ContentValues values = new ContentValues();
            values.put("apikey", apiKey);
            db.insert("ApiKeys", null, values);
            db.close();
        }

        public String getApiKey() {
            SQLiteDatabase db = dbHelper.getReadableDatabase();
            Cursor cursor = db.query("ApiKeys", new String[]{"apikey"},
                    null, null, null, null, null);
            String apiKey = null;
            if (cursor != null && cursor.moveToFirst()) {
                apiKey = cursor.getString(cursor.getColumnIndex("apikey"));
                cursor.close();
            }
            db.close();
            return apiKey;
        }

    }
}