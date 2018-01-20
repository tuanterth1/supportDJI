package com.dji.uilibrarydemo;

import android.Manifest;
import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import dji.common.error.DJIError;
import dji.common.error.DJISDKError;
import dji.common.util.CommonCallbacks;
import dji.sdk.base.BaseComponent;
import dji.sdk.base.BaseProduct;
import dji.sdk.products.Aircraft;
import dji.sdk.sdkmanager.DJISDKManager;

public class MainActivity extends AppCompatActivity {
    private TextView tvSerial;
    private static BaseProduct mProduct;
    final Activity activity = this;

    private void checkPermission() {
        PermissionUtil.checkPermissions(this);
    }

    public static synchronized BaseProduct getProductInstance() {
        if (null == mProduct) {
            mProduct = DJISDKManager.getInstance().getProduct();
        }
        return mProduct;
    }

    static BaseProduct getProduct() {
        return DJISDKManager.getInstance().getProduct();
    }

    public DJISDKManager.SDKManagerCallback mDJISDKManagerCallback = new DJISDKManager.SDKManagerCallback() {

        //Listens to the SDK registration result
        @Override
        public void onRegister(DJIError error) {
            if (error == DJISDKError.REGISTRATION_SUCCESS) {
                DJISDKManager.getInstance().startConnectionToProduct();
                Handler handler = new Handler(Looper.getMainLooper());
                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(getApplicationContext(), "Register Success", Toast.LENGTH_LONG).show();
                    }
                });
            } else {
                Handler handler = new Handler(Looper.getMainLooper());
                handler.post(new Runnable() {

                    @Override
                    public void run() {
                        Toast.makeText(getApplicationContext(), "Register Failed, check network is available", Toast.LENGTH_LONG).show();
                    }
                });

            }
            Log.e("TAG", error.toString());
        }

        //Listens to the connected product changing, including two parts, component changing or product connection changing.
        @Override
        public void onProductChange(BaseProduct oldProduct, BaseProduct newProduct) {

            mProduct = newProduct;
            if (mProduct != null) {
                mProduct.setBaseProductListener(mDJIBaseProductListener);
            }

            notifyStatusChange();
        }
    };

    private BaseProduct.BaseProductListener mDJIBaseProductListener = new BaseProduct.BaseProductListener() {

        @Override
        public void onComponentChange(BaseProduct.ComponentKey key, BaseComponent oldComponent, BaseComponent newComponent) {

            if (newComponent != null) {
                newComponent.setComponentListener(mDJIComponentListener);
            }
            notifyStatusChange();
        }

        @Override
        public void onConnectivityChange(boolean isConnected) {

            notifyStatusChange();
        }

    };

    private BaseComponent.ComponentListener mDJIComponentListener = new BaseComponent.ComponentListener() {

        @Override
        public void onConnectivityChange(boolean isConnected) {
            notifyStatusChange();
        }

    };

    private void notifyStatusChange() {
        mHandler.removeCallbacks(updateRunnable);
        mHandler.postDelayed(updateRunnable, 500);
    }

    private Runnable updateRunnable = new Runnable() {

        @Override
        public void run() {
            Intent intent = new Intent(FLAG_CONNECTION_CHANGE);
            sendBroadcast(intent);
        }
    };
    private Handler mHandler;
    public static final String FLAG_CONNECTION_CHANGE = "uilibrary_demo_connection_change";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        checkPermission();
        if (PermissionUtil.checkPhoneStatePermission(this)) {
            registerDjiSDK();
        }
//        // When the compile and target version is higher than 22, please request the
//        // following permissions at runtime to ensure the
//        // SDK work well.
//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
//            ActivityCompat.requestPermissions(this,
//                    new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.VIBRATE,
//                            Manifest.permission.INTERNET, Manifest.permission.ACCESS_WIFI_STATE,
//                            Manifest.permission.WAKE_LOCK, Manifest.permission.ACCESS_COARSE_LOCATION,
//                            Manifest.permission.ACCESS_NETWORK_STATE, Manifest.permission.ACCESS_FINE_LOCATION,
//                            Manifest.permission.CHANGE_WIFI_STATE, Manifest.permission.MOUNT_UNMOUNT_FILESYSTEMS,
//                            Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.SYSTEM_ALERT_WINDOW,
//                            Manifest.permission.READ_PHONE_STATE,
//                    }
//                    , 1);
//        }

        setContentView(R.layout.activity_main);
        tvSerial = (TextView) findViewById(R.id.tvSerial);
        tvSerial.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                getSerialDrone(tvSerial, activity);
            }
        });
        updateDroneConnection();
        registerReceiver(connectionUpdateReceiver, new IntentFilter(FLAG_CONNECTION_CHANGE));
    }

    public static void getSerialDrone(final TextView tvSerial, final Activity activity) {
        if (null != DemoApplication.getProductInstance()) {
            Aircraft aircraft = (Aircraft) DemoApplication.getProductInstance();
            aircraft.getFlightController().getSerialNumber(new CommonCallbacks.CompletionCallbackWith<String>() {
                @Override
                public void onSuccess(final String serialNumber) {
                    if (!TextUtils.isEmpty(serialNumber)) {
                        activity.runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                tvSerial.setText(serialNumber);
                            }
                        });
                    } else {
                        activity.runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                getSerialDrone(tvSerial, activity);
                            }
                        });
                    }
                }

                @Override
                public void onFailure(DJIError djiError) {
                    activity.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            getSerialDrone(tvSerial, activity);
                        }
                    });
                }
            });
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PermissionUtil.REQUEST_PERMISSION_PHONE_STATE) {
//            DJISDKManager.getInstance().registerApp(this, mDJISDKManagerCallback);
            registerDjiSDK();
        } else if (requestCode == PermissionUtil.REQUEST_ALL_DANGEROUS_PERMISSION) {
            for (int i = 0; i < permissions.length; i++) {
                String permission = permissions[i];
                int grantResult = grantResults[i];
                if (permission.equals(Manifest.permission.READ_PHONE_STATE)) {
                    if (grantResult == PackageManager.PERMISSION_GRANTED) {
//                        DJISDKManager.getInstance().registerApp(this, mDJISDKManagerCallback);
                        registerDjiSDK();
                    }
                    break;
                }
            }
        }
    }

    public void register() {
        mHandler = new Handler(Looper.getMainLooper());
        //This is used to start SDK services and initiate SDK.
        if (mProduct == null) {
            DJISDKManager.getInstance().registerApp(getApplicationContext(), mDJISDKManagerCallback);
//            DJISDKManager.getInstance().enableBridgeModeWithBridgeAppIP("10.10.20.57");
        }
    }

    private void registerDjiSDK() {
        try {
            register();
        } catch (Throwable e) {
            e.printStackTrace();
        }
    }


//    @Override
//    public void onAttach(Activity context) {
//        super.onAttach(context);
//        getActivity().registerReceiver(connectionUpdateReceiver, new IntentFilter(FLAG_CONNECTION_CHANGE));
//    }

    private BroadcastReceiver connectionUpdateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            try {
                updateDroneConnection();
            } catch (Throwable t) {
                t.printStackTrace();
            }
//            Toast.makeText(context, "Status connected:" + DJIUtil.isConnectedToProduct(), Toast.LENGTH_SHORT).show();
        }
    };

    private void updateDroneConnection() {
        try {
            tvSerial.setBackgroundColor(isConnectedToProduct() ? getResources().getColor(R.color.blue) : getResources().getColor(R.color.black));
            if (isConnectedToProduct()) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        getSerialDrone(tvSerial, activity);
                    }
                });
            } else
                tvSerial.setText("Chưa có kết nối đến Drone, Vui lòng kết nối đến drone");
        } catch (Throwable throwable) {
            throwable.printStackTrace();
        }
    }

    public static boolean isConnectedToProduct() {
        return null != getProduct() && getProduct().isConnected();
    }
}

