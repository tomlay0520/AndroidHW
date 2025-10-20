package com.example.parkwhere;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

public class MainActivity extends AppCompatActivity {

    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1;
    private static final String TAG = "MainActivity";

    private EditText destinationInput;
    private Button searchButton, currentLocationButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Log.d(TAG, "onCreate: 初始化主界面");

        initViews();

        checkLocationPermission();

        setupClickListeners();
    }

    private void initViews() {
        destinationInput = findViewById(R.id.destinationInput);
        searchButton = findViewById(R.id.searchButton);
        currentLocationButton = findViewById(R.id.currentLocationButton);

        // 设置输入框焦点变化监听
        destinationInput.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if (hasFocus) {
                    Log.d(TAG, "输入框获得焦点");
                }
            }
        });
    }

    private void setupClickListeners() {
        searchButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.d(TAG, "点击查询按钮");
                String destination = destinationInput.getText().toString().trim();
                if (destination.isEmpty()) {
                    Toast.makeText(MainActivity.this, "请输入目的地", Toast.LENGTH_SHORT).show();
                    destinationInput.requestFocus();
                    return;
                }

                // 跳转到地图页面
                Intent intent = new Intent(MainActivity.this, MapActivity.class);
                intent.putExtra("destination", destination);
                startActivity(intent);
            }
        });

        currentLocationButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.d(TAG, "点击使用当前位置按钮");
                // 使用当前位置作为目的地
                if (hasLocationPermission()) {
                    Log.d(TAG, "已有定位权限，跳转到地图页面");
                    // 获取当前位置并跳转
                    Intent intent = new Intent(MainActivity.this, MapActivity.class);
                    intent.putExtra("useCurrentLocation", true);
                    startActivity(intent);
                } else {
                    Log.d(TAG, "无定位权限，请求权限");
                    Toast.makeText(MainActivity.this, "需要定位权限才能使用此功能", Toast.LENGTH_SHORT).show();
                    requestLocationPermission();
                }
            }
        });
    }

    private boolean hasLocationPermission() {
        return ContextCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED;
    }

    private void checkLocationPermission() {
        if (!hasLocationPermission()) {
            Log.d(TAG, "检查定位权限：无权限");
            // 不立即请求权限，等待用户点击按钮时再请求
        } else {
            Log.d(TAG, "检查定位权限：已有权限");
        }
    }

    private void requestLocationPermission() {
        Log.d(TAG, "请求定位权限");
        if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                Manifest.permission.ACCESS_FINE_LOCATION)) {
            // 解释为什么需要权限
            Toast.makeText(this, "需要定位权限来获取您的位置信息", Toast.LENGTH_LONG).show();
        }

        ActivityCompat.requestPermissions(this,
                new String[]{
                        Manifest.permission.ACCESS_FINE_LOCATION,
                        Manifest.permission.ACCESS_COARSE_LOCATION
                },
                LOCATION_PERMISSION_REQUEST_CODE);
    }


//  定位权限函数
    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        Log.d(TAG, "onRequestPermissionsResult: 请求码=" + requestCode);

        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Log.d(TAG, "定位权限已授予");
                Toast.makeText(this, "定位权限已获取", Toast.LENGTH_SHORT).show();

                // 权限获取后，自动执行当前位置搜索
                Intent intent = new Intent(MainActivity.this, MapActivity.class);
                intent.putExtra("useCurrentLocation", true);
                startActivity(intent);
            } else {
                Log.d(TAG, "定位权限被拒绝");
                Toast.makeText(this, "定位权限被拒绝，部分功能将无法使用", Toast.LENGTH_LONG).show();
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.d(TAG, "onResume: 主界面恢复");
    }

    @Override
    protected void onPause() {
        super.onPause();
        Log.d(TAG, "onPause: 主界面暂停");
    }
}