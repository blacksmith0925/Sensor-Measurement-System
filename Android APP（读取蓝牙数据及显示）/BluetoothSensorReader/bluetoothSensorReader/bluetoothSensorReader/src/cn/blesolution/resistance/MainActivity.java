package cn.blesolution.resistance;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Toast;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothAdapter;


public class MainActivity extends Activity {
    public static final String BLUETOOTH_UUID = "00001101-0000-1000-8000-00805F9B34FB";

    public static final String TAG_REMOTE_ADDRESS="remote_address";
    public static final String TAG_REMOTE_NAME="remote_name";

    private BluetoothAdapter bluetoothAdapter = null;

    private static final int BlUETOOTH_REQUEST_ENABLE = 1;

    private EditText nameEt=null;
    private Button changeNameBtn=null;
    private Button findBtn=null;
    private Button serviceBtn=null;
    private ListView bondedDeviceLv=null;
    private ListView newDeviceLv=null;
    /**进度框*/
    private ProgressDialog pDialog=null;
    /**已配对蓝牙设备集合*/
    private List<BluetoothDevice>bondedDevices = new ArrayList<BluetoothDevice>();
    /**新的蓝牙设备集合*/
    private List<BluetoothDevice>newDevices = new ArrayList<BluetoothDevice>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //创建ProgressDialog进度框，并定义其属性
        pDialog=new ProgressDialog(this);
        pDialog.setCancelable(false);
        pDialog.setMessage("正在扫描蓝牙设备");

        nameEt=(EditText)findViewById(R.id.act_main_btname_et);
        changeNameBtn=(Button)findViewById(R.id.act_main_change_name_btn);
        findBtn = (Button)findViewById(R.id.act_main_find_btn);
        bondedDeviceLv = (ListView)findViewById(R.id.act_main_device_bonded_list);
        newDeviceLv = (ListView)findViewById(R.id.act_main_device_new_list);
        bondedDeviceLv.setOnItemClickListener(onListItemClickListener);
        newDeviceLv.setOnItemClickListener(onListItemClickListener);

        changeNameBtn.setOnClickListener(onBtnClickListener);
        findBtn.setOnClickListener(onBtnClickListener);


        bluetoothAdapter=BluetoothAdapter.getDefaultAdapter();
        // If the adapter is null, then Bluetooth is not supported
        if (bluetoothAdapter == null) {
            Toast.makeText(this, "Bluetooth is not available",
                    Toast.LENGTH_LONG).show();
            finish();
            return;
        }
        //检测蓝牙设备是否已开启
        if(bluetoothAdapter.isEnabled()) {
            initBluetooth();
        }else {
            openBluetooth();
        }
    }

    private OnClickListener onBtnClickListener = new OnClickListener() {
        @Override
        public void onClick(View v) {
            if(v==changeNameBtn) {
                //修改本地蓝牙名称
                String name = nameEt.getText().toString();
                boolean rst = bluetoothAdapter.setName(name);
                Toast.makeText(MainActivity.this, rst?"名称已修改":"修改失败", Toast.LENGTH_SHORT).show();
            }else if(v==findBtn) {
                findBluetooth();
            }
        }

    };

    /**
     * 开启蓝牙
     * */
    protected void openBluetooth() {
        //向用户发出请求，开启蓝牙设备
        Intent intent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
        startActivityForResult(intent,BlUETOOTH_REQUEST_ENABLE);
    }
    @Override
    protected void onActivityResult(int requestCode,int resultCode,Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if(requestCode==BlUETOOTH_REQUEST_ENABLE) {
            if(resultCode==RESULT_OK) {
                Toast.makeText(this, "蓝牙开启成功", Toast.LENGTH_SHORT).show();
                initBluetooth();
            }else {
                Toast.makeText(this, "蓝牙开启失败", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void initBluetooth() {
        //获取本机蓝牙设备名称，显示到TextView
        String btName = bluetoothAdapter.getName();
        nameEt.setText(btName);
        //显示历史配对的蓝牙设备
        showBondedBluetooth();
    }

    /**
     * 查找远端蓝牙设备
     * */
    protected void findBluetooth() {
        pDialog.show();
        registReceiver();
        //开始查找蓝牙设备
        bluetoothAdapter.startDiscovery();
    }

    /**
     * 注册监听
     * */
    private void registReceiver() {
        IntentFilter fileter = new IntentFilter();
        fileter.addAction(BluetoothDevice.ACTION_FOUND);
        fileter.addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
        registerReceiver(bluetoothReceiver,fileter);
    }
    private final BroadcastReceiver bluetoothReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context,Intent intent) {
            String action = intent.getAction();
            if(BluetoothDevice.ACTION_FOUND.equals(action)) {
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                if(device.getBondState()!=BluetoothDevice.BOND_BONDED) {
                    //去除重复添加的设备
                    if(!newDevices.contains(device)) {
                        newDevices.add(device);
                    }
                }
            }else if(BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(action)) {
                pDialog.dismiss();
                unregisterReceiver(bluetoothReceiver);
                //扫描完毕，显示查找到的蓝牙设备
                showFoundDevices();
            }
        }
    };

    /**
     * 显示已经找到的蓝牙设备
     * */
    protected void showFoundDevices() {
        BluetoothListAdapter bondedAdapter = new BluetoothListAdapter(this,newDevices);
        newDeviceLv.setAdapter(bondedAdapter);
    }

    /**
     * 显示已配对蓝牙设备
     * */
    protected void showBondedBluetooth() {
        Set<BluetoothDevice>deviceSet = bluetoothAdapter.getBondedDevices();
        if(deviceSet.size()==0) {
            Toast.makeText(this, "没有已配对设备", Toast.LENGTH_SHORT).show();
            return;
        }
        bondedDevices = new ArrayList<BluetoothDevice>(deviceSet);
        BluetoothListAdapter bondedAdapter = new BluetoothListAdapter(this,bondedDevices);
        bondedDeviceLv.setAdapter(bondedAdapter);
    }

    private OnItemClickListener onListItemClickListener = new OnItemClickListener() {
        @Override
        public void onItemClick(AdapterView<?>parent,View view, int position,long id) {
            BluetoothDevice device = null;
            //判断点击的ListView，获取相应的设备对象
            if(parent==bondedDeviceLv) {
                device = bondedDevices.get(position);
            }else if(parent == newDeviceLv) {
                device = newDevices.get(position);
            }
            //打开客户端通信页面
            Intent intent = new Intent(getApplicationContext(),BleSamplingActivity.class);
            intent.putExtra(TAG_REMOTE_NAME, device.getName());
            intent.putExtra(TAG_REMOTE_ADDRESS, device.getAddress());
            startActivity(intent);
        }
    };

}

