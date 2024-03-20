package com.tbossgroup.scale;

import android.annotation.SuppressLint;
import android.app.ActivityManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.Color;
import android.media.MediaPlayer;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.RemoteException;
import android.support.annotation.RequiresApi;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.tbossgroup.tbscale.EventBatteryPower;
import com.tbossgroup.tbscale.TBScaleAidlInterface;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;

import static android.widget.Toast.LENGTH_LONG;

public class MainActivity extends AppCompatActivity {

    private static final String PACKAGE_NAME = "com.tbossgroup.tbscale.aidl";
    private static final String ACTION = "com.tbossgroup.tbscale.OTScaleService";
    private Button sendButton;
    private int clickCount = 0;
    private int totalClicks = 0;
    private final Handler hideHandler = new Handler();
    private final Handler minuteHandler = new Handler();
    private Runnable minuteRunnable;
    private DatabaseHelper db;




    private TBScaleAidlInterface mTBScaleAidlInterface;
    private ServiceConnection mServiceConnectionn = new ServiceConnection() {

        //绑定服务，回调onBind()方法
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            mTBScaleAidlInterface = TBScaleAidlInterface.Stub.asInterface(service);
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            mTBScaleAidlInterface = null;
        }
    };

    private static final int RESET_REFRESH_DELAY = 100;
    private static final int SERVICE_CONNECTION_DELAY = 1000;

    private static final int WAHT_REFRESH_UI = 1;
    private static final int WAHT_SERVICE_CONNECTION = 2;

    public interface ApiCallback {
        void onSuccess();
        void onFailure();
    }

    private Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            if (msg.what == WAHT_REFRESH_UI) {
                refreshWeightRecord();
            } else if (msg.what == WAHT_SERVICE_CONNECTION) {
                try {
                    if (mTBScaleAidlInterface == null
                            || TextUtils.isEmpty(mTBScaleAidlInterface.getNetWeightString())) {
                        unbindService(mServiceConnectionn);
                        bindService();
                        return;
                    }
                } catch (RemoteException e) {
                    e.printStackTrace();
                }
            }
        }
    };

    private TextView mTvNetWeight;
    private boolean isConfigAppInstall = false;

    private BatteryView mImgBattery;
    private TextView mTvBatteryPer;
    private Bundle savedInstanceState;

    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
    @SuppressLint("WrongViewCast")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        initView();
        bindService();
        initListener();


        //Access ActivitySettings Using Gear Button
        ImageButton button1 = findViewById(R.id.buttonSettings);
        button1.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent i = new Intent(MainActivity.this, ActivitySettings.class);
                startActivity(i);
            }


        });

        //Access ActivityChoice Using choice Button
        ImageButton button2 = findViewById(R.id.selectMachine);
        button2.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent i = new Intent(MainActivity.this, ActivityChoice.class);
                startActivity(i);
            }


        });


        if (checkPackInfo(PACKAGE_NAME)) {
            isConfigAppInstall = true;
            if (!isAppBackground(PACKAGE_NAME)) {
                Intent intent = getPackageManager().getLaunchIntentForPackage(PACKAGE_NAME);
                if (intent != null) {
                    //putExtra传递本包名，可以让sdk在开机零点获取完成后自动跳转会来
                    intent.putExtra("PackageName", this.getPackageName());
                    startActivity(intent);
                }
            }
        }


        //Send Weight to the Database
        final Button sendButton = findViewById(R.id.button_send_weight);
        sendButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                handleButtonClick(sendButton);

            }

            private void handleButtonClick(final Button sendButton) {
                Toast.makeText(MainActivity.this, "Sending weight to database...", Toast.LENGTH_SHORT).show();

                clickCount++;
                totalClicks++;

                if (clickCount == 1) {
                    sendButton.setVisibility(View.INVISIBLE);
                    clickCount = 0; // Reset the count for the next cycle
                    double weight;
                    weight = getWeightFromMachine();
                    String uom = "kg"; // Unit of measurement, e.g., kg, lbs
                    String date = getCurrentDate(); // Implement this method to get the current date in the required format

                    int iotDeviceId;
                    iotDeviceId = getIotDeviceId();

                    if (iotDeviceId != -1) {

                        sendWeightToDatabase(weight, uom, date, iotDeviceId, new ApiCallback() {
                            @Override
                            public void onSuccess() {
                                Toast.makeText(getApplicationContext(), "Successfully Added to Database", Toast.LENGTH_SHORT).show();

                                // Play sound
                                MediaPlayer mp = MediaPlayer.create(getApplicationContext(), R.raw.sound);
                                mp.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
                                    @Override
                                    public void onCompletion(MediaPlayer mp) {
                                        mp.release();
                                    }
                                });
                                mp.start();
                            }

                            @Override
                            public void onFailure() {
                                Toast.makeText(getApplicationContext(), "Failed to Add to Database", Toast.LENGTH_SHORT).show();
                            }
                        });
                    } else {
                        Toast.makeText(MainActivity.this, "No IoT Device ID found.", Toast.LENGTH_SHORT).show();
                        return;
                    }


                } else {
                    sendButton.setEnabled(false); // Disable the button after 20 total clicks
                    minuteHandler.removeCallbacks(minuteRunnable); // Stop resetting after 1 minute
                }


                // Make the button visible again after 3 seconds
                hideHandler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        sendButton.setVisibility(View.VISIBLE);
                    }
                }, 3000);
            }


        });

    }





    private String getApiAddress() {
        SharedPreferences sharedPreferences = getSharedPreferences("AppSettings", MODE_PRIVATE);
        return sharedPreferences.getString("API_ADDRESS", ""); // Replace with a default API address if necessary
    }




    @Override
    protected void onStart() {
        super.onStart();
        if (isConfigAppInstall) {
            bindService();
            mHandler.sendEmptyMessage(WAHT_REFRESH_UI);
        } else {
            Toast.makeText(this, "智能秤配置APK未安装", LENGTH_LONG).show();
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        unbindService(mServiceConnectionn);
        mHandler.removeCallbacksAndMessages(null);
    }

    private void initListener() {
        findViewById(R.id.btnTare).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                try {
                    mTBScaleAidlInterface.setTare();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
        findViewById(R.id.btnZero).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                try {
                    mTBScaleAidlInterface.setZero();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
        findViewById(R.id.btnSettings).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (checkPackInfo(PACKAGE_NAME)) {
                    openPackage(MainActivity.this, PACKAGE_NAME);
                } else {
                    Toast.makeText(MainActivity.this, "智能秤配置APK未安装", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    private void initView() {
        mTvNetWeight = findViewById(R.id.tvNetWeight);
        mImgBattery = findViewById(R.id.img_battery);
        mTvBatteryPer = findViewById(R.id.tv_battery_per);
    }

    private void bindService() {
        //绑定服务端的service
        Intent intent = new Intent();
        intent.setAction(ACTION);
        intent.setPackage(PACKAGE_NAME);
        //绑定的时候服务端自动创建
        bindService(intent, mServiceConnectionn, Context.BIND_AUTO_CREATE);

        mHandler.sendEmptyMessageDelayed(WAHT_SERVICE_CONNECTION, SERVICE_CONNECTION_DELAY);
    }

    private void refreshWeightRecord() {
        try {
            //获取净重
            mTvNetWeight.setText(mTBScaleAidlInterface.getNetWeight() + " KG");

            //获取电量
            EventBatteryPower batteryPower = mTBScaleAidlInterface.getBatteryPower();
            mTvBatteryPer.setText(batteryPower.getPower() + "%");
            mImgBattery.setPower(batteryPower.getPower());
            if (batteryPower.isLowPower()) {
                mTvBatteryPer.setTextColor(Color.RED);
                mImgBattery.setColor(Color.RED);
            } else {
                mTvBatteryPer.setTextColor(Color.WHITE);
                mImgBattery.setColor(Color.WHITE);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        mHandler.sendEmptyMessageDelayed(WAHT_REFRESH_UI, RESET_REFRESH_DELAY);
    }

    private boolean openPackage(Context context, String packageName) {
        Context pkgContext = getPackageContext(context, packageName);
        Intent intent = getAppOpenIntentByPackageName(context, packageName);
        if (pkgContext != null && intent != null) {
            pkgContext.startActivity(intent);
            return true;
        }
        return false;
    }

    private Context getPackageContext(Context context, String packageName) {
        Context pkgContext = null;
        if (context.getPackageName().equals(packageName)) {
            pkgContext = context;
        } else {
            try {
                pkgContext = context.createPackageContext(packageName,
                        Context.CONTEXT_IGNORE_SECURITY
                                | Context.CONTEXT_INCLUDE_CODE);
            } catch (PackageManager.NameNotFoundException e) {
                e.printStackTrace();
            }
        }
        return pkgContext;
    }

    private boolean checkPackInfo(String packname) {
        PackageInfo packageInfo = null;
        try {
            packageInfo = getPackageManager().getPackageInfo(packname, 0);
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }
        return packageInfo != null;
    }

    private Intent getAppOpenIntentByPackageName(Context context, String packageName) {
        String activityName = null;
        PackageManager pkgMag = context.getPackageManager();
        Intent intent = new Intent(Intent.ACTION_MAIN);
        intent.addCategory(Intent.CATEGORY_LAUNCHER);
        intent.setFlags(Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED | Intent.FLAG_ACTIVITY_NEW_TASK);

        List<ResolveInfo> list = pkgMag.queryIntentActivities(intent, PackageManager.GET_ACTIVITIES);
        for (int i = 0; i < list.size(); i++) {
            ResolveInfo info = list.get(i);
            if (info.activityInfo.packageName.equals(packageName)) {
                activityName = info.activityInfo.name;
                break;
            }
        }
        if (TextUtils.isEmpty(activityName)) {
            return null;
        }
        intent.setComponent(new ComponentName(packageName, activityName));
        return intent;
    }

    private boolean isAppBackground(String packageName) {
        ActivityManager activityManager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
        List<ActivityManager.RunningAppProcessInfo> appProcesses = activityManager
                .getRunningAppProcesses();
        for (ActivityManager.RunningAppProcessInfo appProcess : appProcesses) {
            if (appProcess.processName.equals("packageName")) {
                if (appProcess.importance != ActivityManager.RunningAppProcessInfo.IMPORTANCE_FOREGROUND) {
                    return true;
                } else {
                    return false;
                }
            }
        }
        return false;
    }



    private float getWeightFromMachine() {
        try {
            if (mTBScaleAidlInterface != null) {
                // Assuming 'getNetWeight' returns the current weight as an integer.
                // The actual method name or return type might differ based on your scale's API.
                return  mTBScaleAidlInterface.getNetWeight();
            } else {
                Log.e("WeightError", "Scale service is not connected");
            }
        } catch (RemoteException e) {
            Log.e("WeightError", "Error getting weight from scale", e);
        }
        return 0; // Example value
    }

//    private String getCurrentDate() {
//        // Implement to return the current date in the format "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'"
//        return "";
//    }

    private String getCurrentDate() {
        // Create a SimpleDateFormat instance with the desired format.
        @SuppressLint("SimpleDateFormat") SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");

        // Set the time zone to UTC if you want the 'Z' (Zulu Time) at the end of the date string.
        dateFormat.setTimeZone(TimeZone.getTimeZone("Asia/Kuala_Lumpur"));

        // Get the current date and time.
        Date now = new Date();

        // Return the formatted date string.
        return dateFormat.format(now);
    }

    private int getIotDeviceId() {
        SharedPreferences sharedPreferences = getSharedPreferences("AppPrefs", MODE_PRIVATE);
        return sharedPreferences.getInt("DefaultWorkCentreId", -1); // Return -1 if not found
    }










    //API connection
// Adjust the method signature to include the ApiCallback parameter
    private void sendWeightToDatabase(final double weight, final String uom, final String date, final int iotDeviceId, final ApiCallback callback) {
        new AsyncTask<Void, Void, Boolean>() {
            @SuppressLint("StaticFieldLeak")
            @Override
            protected Boolean doInBackground(Void... voids) {
                try {
                    String apiUrl = getApiAddress();
                    URL url = new URL(apiUrl + "/WeightReading");
                    HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                    conn.setRequestMethod("POST");
                    conn.setRequestProperty("Content-Type", "application/json; utf-8");
                    conn.setRequestProperty("Accept", "application/json");
                    conn.setDoOutput(true);

                    JSONObject jsonParam = new JSONObject();
                    jsonParam.put("weight", weight);
                    jsonParam.put("uom", uom);
                    jsonParam.put("date", date);
                    jsonParam.put("iotDeviceId", iotDeviceId);

                    try (OutputStream os = conn.getOutputStream()) {
                        byte[] input = jsonParam.toString().getBytes("utf-8");
                        os.write(input, 0, input.length);
                    }

                    // Check response code to determine success
                    int responseCode = conn.getResponseCode();
                    conn.disconnect();

                    // Consider 2xx status codes as success
                    return (responseCode >= 200 && responseCode < 300);

                } catch (Exception e) {
                    e.printStackTrace();
                    return false;
                }
            }

            @Override
            protected void onPostExecute(Boolean result) {
                if (result) {
                    callback.onSuccess();
                } else {
                    callback.onFailure();
                }
            }
        }.execute();
    }



}