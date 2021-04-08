package com.zzc.espcolor;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.annotation.SuppressLint;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.TextView;

import java.util.ArrayList;

import static com.zzc.espcolor.ColorPickView.toBrowserHexValue;

public class MainActivity extends AppCompatActivity implements BLESPPUtils.OnBluetoothAction {

    private BLESPPUtils mBLESPPUtils = null;
    // 保存搜索到的设备，避免重复
    private final ArrayList<BluetoothDevice> mDevicesList = new ArrayList<>();
    // 对话框控制
    private DeviceDialogCtrl mDeviceDialogCtrl;

    private Button btn_blingbling;
    private Button btn_rainbow;
    private Button btn_runningWater;
    private Button btn_backAndForth;
    private Button btn_three;
    private Button btn_single;

    private ColorPickView color_picker_view;

    private TextView colorText;

    private int RGB_r,RGB_g,RGB_b;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        hideStatus();
        init();
        initPermissions();
        // 初始化
        mBLESPPUtils = new BLESPPUtils(this, this);
        // 启用日志输出
        mBLESPPUtils.enableBluetooth();
        // 设置接收停止标志位字符串
        mBLESPPUtils.setStopString("\r\n");
        // 用户没有开启蓝牙的话打开蓝牙
        if (!mBLESPPUtils.isBluetoothEnable()) mBLESPPUtils.enableBluetooth();
        // 启动工具类
        mBLESPPUtils.onCreate();
        mDeviceDialogCtrl = new DeviceDialogCtrl(this);
        mDeviceDialogCtrl.show();
    }

    // 初始化控件
    @SuppressLint("SetTextI18n")
    private void init() {
        btn_blingbling = findViewById(R.id.btn_blingbling);
        btn_rainbow = findViewById(R.id.btn_rainbow);
        btn_runningWater = findViewById(R.id.btn_runningWater);
        btn_backAndForth = findViewById(R.id.btn_backAndForth);
        btn_three = findViewById(R.id.btn_three);
        btn_single = findViewById(R.id.btn_single);
        color_picker_view = findViewById(R.id.color_picker_view);
        colorText = findViewById(R.id.colorText);
        Button btn_change = findViewById(R.id.btn_change);

        color_picker_view.setOnColorChangedListener((a, r, g, b) -> {
            RGB_r = r;
            RGB_g = g;
            RGB_b = b;
            colorText.setText("R:"+ r + "   G:" + g + "   B:" + b +"   " + color_picker_view.getColorStr());
        });

        btn_blingbling.setOnClickListener(v -> {
            onSendBytes("A".getBytes());
            changeButton(1);
        });

        btn_rainbow.setOnClickListener(v -> {
            onSendBytes("B".getBytes());
            changeButton(2);
        });

        btn_runningWater.setOnClickListener(v -> {
            onSendBytes("C".getBytes());
            changeButton(3);
        });

        btn_backAndForth.setOnClickListener(v -> {
            onSendBytes("D".getBytes());
            changeButton(4);
        });

        btn_three.setOnClickListener(v -> {
            onSendBytes("E".getBytes());
            changeButton(5);
        });

        btn_single.setOnClickListener(v -> {
            onSendBytes("F".getBytes());
            changeButton(6);
        });

        btn_change.setOnClickListener(v -> onSendBytes(("G;" + RGB_r + ";" + RGB_g + ";" + RGB_b).getBytes()));

    }

    // 时间栏透明
    private void hideStatus() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            Window window = getWindow();
            window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS
                    | WindowManager.LayoutParams.FLAG_TRANSLUCENT_NAVIGATION);
            window.getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                    | View.SYSTEM_UI_FLAG_LAYOUT_STABLE);
            window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
            window.setStatusBarColor(Color.TRANSPARENT);
        }
    }

    // 更改界面
    private void changeButton(int index) {
        btn_rainbow.setBackgroundResource(R.color.btn_yc_color);
        btn_runningWater.setBackgroundResource(R.color.btn_yc_color);
        btn_backAndForth.setBackgroundResource(R.color.btn_yc_color);
        btn_three.setBackgroundResource(R.color.btn_yc_color);
        btn_single.setBackgroundResource(R.color.btn_yc_color);
        btn_blingbling.setBackgroundResource(R.color.btn_yc_color);
        switch (index) {
            case 2: btn_rainbow.setBackgroundResource(R.color.btn_color); break;
            case 3: btn_runningWater.setBackgroundResource(R.color.btn_color); break;
            case 4: btn_backAndForth.setBackgroundResource(R.color.btn_color); break;
            case 5: btn_three.setBackgroundResource(R.color.btn_color); break;
            case 6: btn_single.setBackgroundResource(R.color.btn_color); break;
            default: btn_blingbling.setBackgroundResource(R.color.btn_color); break;
        }
    }

    /**
     * 申请运行时权限，不授予会搜索不到设备
     */
    private void initPermissions() {
        if (ContextCompat.checkSelfPermission(this, "android.permission-group.LOCATION") != 0) {
            ActivityCompat.requestPermissions(
                    this,
                    new String[]{
                            "android.permission.ACCESS_FINE_LOCATION",
                            "android.permission.ACCESS_COARSE_LOCATION",
                            "android.permission.ACCESS_WIFI_STATE"},
                    1
            );
        }
    }

    /**
     * 当发现新设备
     *
     * @param device 设备
     */
    @Override
    public void onFoundDevice(BluetoothDevice device) {
        Log.d("BLE", "发现设备 " + device.getName() + device.getAddress());
        // 判断是不是重复的
        for (int i = 0; i < mDevicesList.size(); i++) {
            if (mDevicesList.get(i).getAddress().equals(device.getAddress())) return;
        }
        // 添加，下次有就不显示了
        mDevicesList.add(device);
        // 添加条目到 UI 并设置点击事件
        mDeviceDialogCtrl.addDevice(device, v -> {
            BluetoothDevice clickDevice = (BluetoothDevice) v.getTag();
            postShowToast("开始连接:" + clickDevice.getName());
            mBLESPPUtils.connect(clickDevice);
        });
    }

    /**
     * 当连接成功
     *
     * @param device 设备
     */
    @Override
    public void onConnectSuccess(final BluetoothDevice device) {
        postShowToast("连接成功", () -> mDeviceDialogCtrl.dismiss());
    }

    /**
     * 当连接失败
     *
     * @param msg 失败信息
     */
    @Override
    public void onConnectFailed(final String msg) {
        postShowToast("连接失败:" + msg, () -> Log.e("zzc","连接失败:"+msg));
    }

    /**
     * 当接收到 byte 数组
     *
     * @param bytes 内容
     */
    @SuppressLint("SetTextI18n")
    @Override
    public void onReceiveBytes(final byte[] bytes) {
        postShowToast(new String(bytes), () -> {
            String[] stp = new String(bytes).trim().split(";");
            int number = (stp[0].charAt(0) - 'A') +1;
            changeButton(number);
            int r = Integer.parseInt(stp[1]);
            int g = Integer.parseInt(stp[2]);
            int b = Integer.parseInt(stp[3]);
            colorText.setText("R:"+ r + "   G:" + g + "   B:" + b +"   " + "#" + toBrowserHexValue(r) + toBrowserHexValue(g) + toBrowserHexValue(b));
        });
    }

    /**
     * 当调用接口发送 byte 数组
     *
     * @param bytes 内容7
     */
    @Override
    public void onSendBytes(final byte[] bytes) {
        postShowToast(new String(bytes), () -> mBLESPPUtils.send(bytes));
    }

    /**
     * 当结束搜索设备
     */
    @Override
    public void onFinishFoundDevice() { }

    /**
     * 设备选择对话框控制
     */
    private class DeviceDialogCtrl {
        private final LinearLayout mDialogRootView;
        private final ProgressBar mProgressBar;
        private final AlertDialog mConnectDeviceDialog;

        DeviceDialogCtrl(Context context) {
            // 搜索进度条
            mProgressBar = new ProgressBar(context, null, android.R.attr.progressBarStyleHorizontal);
            mProgressBar.setLayoutParams(
                    new LinearLayout.LayoutParams(
                            LinearLayout.LayoutParams.MATCH_PARENT,
                            50
                    )
            );

            // 根布局
            mDialogRootView = new LinearLayout(context);
            mDialogRootView.setOrientation(LinearLayout.VERTICAL);
            mDialogRootView.addView(mProgressBar);
            mDialogRootView.setMinimumHeight(700);

            // 容器布局
            ScrollView scrollView = new ScrollView(context);
            scrollView.addView(mDialogRootView,
                    new FrameLayout.LayoutParams(
                            FrameLayout.LayoutParams.MATCH_PARENT,
                            700
                    )
            );

            // 构建对话框
            mConnectDeviceDialog = new AlertDialog
                    .Builder(context)
                    .setNegativeButton("刷新", null)
                    .setPositiveButton("退出", null)
                    .create();
            mConnectDeviceDialog.setTitle("选择连接的蓝牙设备");
            mConnectDeviceDialog.setView(scrollView);
            mConnectDeviceDialog.setCancelable(false);
        }

        /**
         * 显示并开始搜索设备
         */
        void show() {
            mBLESPPUtils.startDiscovery();
            mConnectDeviceDialog.show();
            mConnectDeviceDialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnLongClickListener(v -> {
                mConnectDeviceDialog.dismiss();
                return false;
            });
            mConnectDeviceDialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
                mConnectDeviceDialog.dismiss();
                finish();
            });
            mConnectDeviceDialog.getButton(AlertDialog.BUTTON_NEGATIVE).setOnClickListener(v -> {
                mDialogRootView.removeAllViews();
                mDialogRootView.addView(mProgressBar);
                mDevicesList.clear();
                mBLESPPUtils.startDiscovery();
            });
        }

        /**
         * 取消对话框
         */
        void dismiss() {
            mConnectDeviceDialog.dismiss();
        }

        /**
         * 添加一个设备到列表
         * @param device 设备
         * @param onClickListener 点击回调
         */
        @SuppressLint("SetTextI18n")
        private void addDevice(final BluetoothDevice device, final View.OnClickListener onClickListener) {
            runOnUiThread(() -> {
                TextView devTag = new TextView(MainActivity.this);
                devTag.setClickable(true);
                devTag.setPadding(20,20,20,20);
                devTag.setBackgroundResource(R.drawable.rect_round_button_ripple);
                devTag.setText(device.getName() + "\nMAC:" + device.getAddress());
                devTag.setTextColor(Color.WHITE);
                devTag.setOnClickListener(onClickListener);
                devTag.setTag(device);
                devTag.setLayoutParams(
                        new LinearLayout.LayoutParams(
                                LinearLayout.LayoutParams.MATCH_PARENT,
                                LinearLayout.LayoutParams.WRAP_CONTENT
                        )
                );
                ((LinearLayout.LayoutParams) devTag.getLayoutParams()).setMargins(
                        20, 20, 20, 20);
                mDialogRootView.addView(devTag);
            });
        }
    }

    /**
     * 在主线程弹出 Toast
     *
     * @param msg 信息
     */
    private void postShowToast(final String msg) {
        postShowToast(msg, null);
    }

    /**
     * 在主线程弹出 Toast
     *
     * @param msg 信息
     * @param doSthAfterPost 在弹出后做点什么
     */
    private void postShowToast(final String msg, final DoSthAfterPost doSthAfterPost) {
        runOnUiThread(() -> {
            if (doSthAfterPost != null) doSthAfterPost.doIt();
        });
    }

    private interface DoSthAfterPost {
        void doIt();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mBLESPPUtils.onDestroy();
    }
}