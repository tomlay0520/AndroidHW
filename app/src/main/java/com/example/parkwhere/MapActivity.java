package com.example.parkwhere;

import static androidx.constraintlayout.helper.widget.MotionEffect.TAG;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.baidu.location.BDAbstractLocationListener;
import com.baidu.location.BDLocation;
import com.baidu.location.LocationClient;
import com.baidu.location.LocationClientOption;
import com.baidu.mapapi.SDKInitializer;
import com.baidu.mapapi.map.BaiduMap;
import com.baidu.mapapi.map.BitmapDescriptor;
import com.baidu.mapapi.map.BitmapDescriptorFactory;
import com.baidu.mapapi.map.MapStatusUpdate;
import com.baidu.mapapi.map.MapStatusUpdateFactory;
import com.baidu.mapapi.map.MapView;
import com.baidu.mapapi.map.Marker;
import com.baidu.mapapi.map.MarkerOptions;
import com.baidu.mapapi.map.MyLocationData;
import com.baidu.mapapi.model.LatLng;
import com.baidu.mapapi.search.core.PoiInfo;
import com.baidu.mapapi.search.poi.OnGetPoiSearchResultListener;
import com.baidu.mapapi.search.poi.PoiCitySearchOption;
import com.baidu.mapapi.search.poi.PoiDetailResult;
import com.baidu.mapapi.search.poi.PoiDetailSearchResult;
import com.baidu.mapapi.search.poi.PoiIndoorResult;
import com.baidu.mapapi.search.poi.PoiResult;
import com.baidu.mapapi.search.poi.PoiSearch;
import com.baidu.mapapi.search.poi.PoiAddrInfo;

import java.util.List;

public class MapActivity extends AppCompatActivity {
    private PoiAddrInfo pInfo;

    private MapView mapView;
    private BaiduMap baiduMap;
    private LocationClient locationClient;
    private PoiSearch poiSearch;
    private boolean isFirstLoc = true;
    private String destination;
    private boolean useCurrentLocation;

    private static final int LOCATION_PERMISSION_REQUEST_CODE = 100;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // 确保百度地图SDK已初始化
        try {
            SDKInitializer.initialize(getApplicationContext());
        } catch (Exception e) {
            Log.e(TAG, "百度地图SDK初始化异常: " + e.getMessage());
        }

        setContentView(R.layout.activity_map);

        // 获取传递的参数
        destination = getIntent().getStringExtra("destination");
        useCurrentLocation = getIntent().getBooleanExtra("useCurrentLocation", false);

        // 初始化地图
        mapView = findViewById(R.id.bmapView);
        baiduMap = mapView.getMap();
        baiduMap.setMyLocationEnabled(true);

        // 检查并请求权限
        if (checkPermissions()) {
            initLocation();
        }

        // 初始化POI搜索
        poiSearch = PoiSearch.newInstance();
        poiSearch.setOnGetPoiSearchResultListener(poiListener);
    }

    // 检查权限
    private boolean checkPermissions() {
        String[] permissions = {
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
        };

        boolean allGranted = true;
        for (String permission : permissions) {
            if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
                allGranted = false;
                break;
            }
        }

        if (!allGranted) {
            ActivityCompat.requestPermissions(this, permissions, LOCATION_PERMISSION_REQUEST_CODE);
            return false;
        }
        return true;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                initLocation();
            } else {
                Toast.makeText(this, "需要定位权限才能使用地图功能", Toast.LENGTH_LONG).show();
                // 即使没有权限也初始化，但可能定位不准确
                initLocation();
            }
        }
    }

    private void initLocation() {
        try {
            locationClient = new LocationClient(getApplicationContext());

            LocationClientOption option = new LocationClientOption();
            option.setOpenGps(true);
            option.setCoorType("bd09ll");
            option.setScanSpan(3000);
            option.setIsNeedAddress(true);
            option.setIsNeedLocationDescribe(true);
            option.setLocationMode(LocationClientOption.LocationMode.Hight_Accuracy);
            option.disableCache(true);

            // 添加更多配置提高定位成功率
            option.setIsNeedLocationPoiList(true);
            option.setIgnoreKillProcess(false);
            option.SetIgnoreCacheException(false);

            locationClient.setLocOption(option);

            locationClient.registerLocationListener(new BDAbstractLocationListener() {
                @Override
                public void onReceiveLocation(BDLocation location) {
                    if (location == null) {
                        Log.e(TAG, "定位结果为空");
                        return;
                    }

                    Log.d(TAG, "定位类型: " + location.getLocType() + ", 描述: " + location.getLocTypeDescription());

                    if (location.getLocType() == BDLocation.TypeGpsLocation
                            || location.getLocType() == BDLocation.TypeNetWorkLocation
                            || location.getLocType() == BDLocation.TypeOffLineLocation
                            || location.getLocType() == 161) { // 161也是网络定位成功
                        handleLocationSuccess(location);
                    } else {
                        handleLocationFailure(location.getLocType());
                    }
                }
            });

            // 延迟启动定位
            new Handler().postDelayed(() -> {
                if (locationClient != null) {
                    locationClient.start();
                    Log.d(TAG, "定位服务已启动");
                }
            }, 1000);

        } catch (Exception e) {
            Log.e(TAG, "初始化定位失败: " + e.getMessage(), e);
            Toast.makeText(this, "定位服务初始化失败: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void handleLocationSuccess(BDLocation location) {
        Log.d(TAG, "定位成功: " + location.getAddrStr() +
                ", 纬度: " + location.getLatitude() +
                ", 经度: " + location.getLongitude() +
                ", 城市: " + location.getCity());

        // 更新地图位置
        MyLocationData locData = new MyLocationData.Builder()
                .accuracy(location.getRadius())
                .direction(location.getDirection())
                .latitude(location.getLatitude())
                .longitude(location.getLongitude())
                .build();
        baiduMap.setMyLocationData(locData);

        if (isFirstLoc) {
            isFirstLoc = false;
            LatLng ll = new LatLng(location.getLatitude(), location.getLongitude());
            MapStatusUpdate u = MapStatusUpdateFactory.newLatLngZoom(ll, 16f);
            baiduMap.animateMapStatus(u);

            // 定位成功后搜索停车场
            String searchCity = location.getCity();
            if (searchCity != null) {
                // 移除"市"字后缀
                searchCity = searchCity.replace("市", "");
            } else {
                searchCity = "北京"; // 备用城市
            }

            Log.d(TAG, "准备搜索，城市: " + searchCity + ", 目的地: " + destination);
            searchParking(searchCity, destination);
        }

        runOnUiThread(() ->
                Toast.makeText(this, "定位成功: " + location.getAddrStr(), Toast.LENGTH_SHORT).show()
        );
    }

    private void handleLocationFailure(int errorCode) {
        String errorMsg = getLocationErrorInfo(errorCode);
        Log.e(TAG, errorMsg);
        runOnUiThread(() ->
                Toast.makeText(this, errorMsg, Toast.LENGTH_LONG).show()
        );
    }

    private String getLocationErrorInfo(int locType) {
        return "";
    }

    private void searchParking(String city, String keyword) {
        if (city == null || city.isEmpty()) {
            city = "北京"; // 默认城市
        }

        if (keyword == null || keyword.isEmpty()) {
            keyword = "停车场"; // 如果目的地为空，只搜索停车场
        } else {
            keyword = keyword + "停车场";
        }

        Log.d(TAG, "开始搜索POI: 城市=" + city + ", 关键词=" + keyword);

        // 检查poiSearch是否初始化
        if (poiSearch == null) {
            Log.e(TAG, "POI搜索未初始化");
            poiSearch = PoiSearch.newInstance();
            poiSearch.setOnGetPoiSearchResultListener(poiListener);
        }

        // 执行搜索
        poiSearch.searchInCity(new PoiCitySearchOption()
                .city(city)
                .keyword(keyword)
                .pageNum(0)
                .pageCapacity(10));
    }

    OnGetPoiSearchResultListener poiListener = new OnGetPoiSearchResultListener() {
        @Override
        public void onGetPoiResult(PoiResult result) {
            if (result == null || result.error != PoiResult.ERRORNO.NO_ERROR) {
                Log.e(TAG, "POI搜索失败: " + (result != null ? result.error : "result is null"));
                runOnUiThread(() ->
                        Toast.makeText(MapActivity.this, "抱歉，未找到结果", Toast.LENGTH_LONG).show()
                );
                return;
            }

            // 处理搜索结果，在地图上标记停车场
            baiduMap.clear();
            List<PoiInfo> allPoi = result.getAllPoi();
            if (allPoi != null && !allPoi.isEmpty()) {
                addParkingMarkers(allPoi);
                Log.d(TAG, "找到 " + allPoi.size() + " 个停车场");

                runOnUiThread(() ->
                        Toast.makeText(MapActivity.this, "找到" + allPoi.size() + "个停车场",
                                Toast.LENGTH_SHORT).show()
                );
            } else {
                runOnUiThread(() ->
                        Toast.makeText(MapActivity.this, "未找到停车场", Toast.LENGTH_SHORT).show()
                );
            }
        }

        private void addParkingMarkers(List<PoiInfo> poiList) {
            for (PoiInfo poiInfo : poiList) {
                // 创建坐标点
                LatLng point = new LatLng(poiInfo.location.latitude, poiInfo.location.longitude);

                // 创建标记图标 - 使用正确的资源
                BitmapDescriptor bitmap = BitmapDescriptorFactory
                        .fromResource(R.drawable.button_secondary); // 确保使用正确的资源名

                // 创建标记选项
                MarkerOptions markerOptions = new MarkerOptions()
                        .position(point)
                        .icon(bitmap)
                        .title(poiInfo.name)
                        .zIndex(9);

                // 添加标记到地图
                Marker marker = (Marker) baiduMap.addOverlay(markerOptions);

                Log.d(TAG, "添加标记: " + poiInfo.name + " at " + point.latitude + "," + point.longitude);
            }
        }

        @Override
        public void onGetPoiDetailResult(PoiDetailResult result) {}

        @Override
        public void onGetPoiDetailResult(PoiDetailSearchResult result) {}

        @Override
        public void onGetPoiIndoorResult(PoiIndoorResult result) {}
    };

    @Override
    protected void onResume() {
        super.onResume();
        mapView.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
        mapView.onPause();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (locationClient != null && locationClient.isStarted()) {
            locationClient.stop();
        }
        mapView.onDestroy();
        if (poiSearch != null) {
            poiSearch.destroy();
        }
    }
}