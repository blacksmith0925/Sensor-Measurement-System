package cn.blesolution.resistance;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.Set;
import java.util.UUID;

import lecho.lib.hellocharts.listener.LineChartOnValueSelectListener;
import lecho.lib.hellocharts.model.Axis;
import lecho.lib.hellocharts.model.Line;
import lecho.lib.hellocharts.model.LineChartData;
import lecho.lib.hellocharts.model.PointValue;
import lecho.lib.hellocharts.model.ValueShape;
import lecho.lib.hellocharts.model.Viewport;
import lecho.lib.hellocharts.util.ChartUtils;
import lecho.lib.hellocharts.view.LineChartView;
//import net.flyget.bluetoothhelper.MainActivity.ConnectThread;
//import net.flyget.bluetoothhelper.MainActivity.ConnectedThread;
import android.app.Activity;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Paint;
import android.net.Uri;
import android.nfc.NfcAdapter;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import cn.blesolution.resistance.R;

import com.aofei.nfc.TagUtil;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.bluetooth.BluetoothAdapter;



public class BleSamplingActivity extends Activity {
	private Button mBtnStart;
	private EditText mEdtTime;
	private Button mBtnOpenFile;
	private TagUtil mTagUtil = null;
	private Intent mKeepIntent = null;
	private final boolean isCheckSUM = false;
	public static final float Rm = 47.7f;  // 47.7 k娆у
	private NfcAdapter mNfcAdapter;
	private PendingIntent mPendingIntent;
	private LineChartView chart;
	private LineChartData data;
	private int numberOfPoints = 0;
	private float mMinRm=1024;
	private float mMaxRm =0;
	
	private final List<Float> randomNumbersTab = new ArrayList<Float>();
	
	private final boolean hasAxes = true;
	private final boolean hasAxesNames = true;
	private final boolean hasLines = true;
	private final boolean hasPoints = true;
	private final ValueShape shape = ValueShape.CIRCLE;
	private final boolean isFilled = false;
	private final boolean hasLabels = false;
	private final boolean isCubic = true;
	private final boolean hasLabelForSelected = false;
	private boolean pointsHaveDifferentColor;
	private Line mLine;
	private List<PointValue> mValues = new ArrayList<PointValue>();
	private List<Line> mLines;
	private int mLoopCounter = 0;
	private TextView mBtnExport;
	private TextView mBtnImport;
	private TextView mBtnOpenDir;
	private TextView contentTv;
	private File mParentDirectory = null;
	
	
	/**远端蓝牙设备名称*/
	private String remoteName = null;
	/**远端蓝牙设备地址*/
	private String remoteAddress = null;
	
	/**远端蓝牙设备*/
	public BluetoothDevice remoteDevice = null;
	private BluetoothAdapter bluetoothAdapter = null;
	
//	private ClientThread clientThread = null;
	
	private static final int BlUETOOTH_REQUEST_ENABLE = 1;
	
	private BluetoothSocket socket = null;
	
	private List<BluetoothDevice>bondedDevices = new ArrayList<BluetoothDevice>();
	
	public static final String BLUETOOTH_UUID = "00001101-0000-1000-8000-00805F9B34FB";
	private static final int REQUEST_ENABLE_BT = 0;
	private ConnectThread mConnectThread;
	public ConnectedThread mConnectedThread;
	
	private static final String TAG = "BleSamplingActivity";
	private List<Integer> mBuffer;
	private static final int MSG_NEW_DATA = 3;
	private boolean isPause = false;
	
	private final int HEX = 0;
	private final int DEC = 1;
	private final int ASCII = 2;
	private int mCodeType = ASCII;
	private String single_data;
	
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_ble_sampling);
		
		initBt();
		try {
//			TimeUnit.MILLISECONDS.sleep(40);
//			TimeUnit.SECONDS.sleep(10);
			setupListener();
			initData();
			initChart();
//			TimeUnit.SECONDS.sleep(10);
			processIntent(getIntent());
		}catch( Exception e) {
			Tools.showToast(this, e.getMessage());
		}
	}

	private void initChart() {
		chart = (LineChartView) findViewById(R.id.chart);
		chart.setOnValueTouchListener(new ValueTouchListener(this));
		chart.setViewportCalculationEnabled(false);
		chart.setVisibility(8);
	}
	
	private void resetViewport(long maxmillseconds) {
		final Viewport v = new Viewport(chart.getMaximumViewport());
		v.bottom = mMinRm;
		v.top = mMaxRm;
		v.left = 0;
		v.right = maxmillseconds * 1.2f;
		chart.setMaximumViewport(v);
		chart.setCurrentViewport(v);
	}
	
	
	/**创建界面按钮的监控*/
	private void setupListener() {
		contentTv = (TextView)findViewById(R.id.ble_chat_content_tv);
		mBtnStart = (Button) findViewById(R.id.btnStart);
		mBtnStart.setOnClickListener(new JEscapeDoubleClickListener() {
			@Override
			public void onNoDoubleClick(View v) {
				doStart();
			}
		});
		
		mEdtTime = (EditText) findViewById(R.id.edtimer);

		mBtnOpenFile=(Button)findViewById(R.id.button_OpenFile);
		mBtnOpenFile.setOnClickListener(new JEscapeDoubleClickListener() {
			@Override
			public void onNoDoubleClick(View v) {
				ImportLocalData();}});
		
		mBtnExport = (TextView) findViewById(R.id.btnExport);
		mBtnExport.getPaint().setFlags(Paint.UNDERLINE_TEXT_FLAG );
		mBtnExport.setOnClickListener(new JEscapeDoubleClickListener() {
			@Override
			public void onNoDoubleClick(View v) {
				export2Local();
			}
		});

	}

	private void export2Local() {
		Tools.shortToast(this, "正在导出..");
		new AsyncTask<Void, Void, String>() {
			@Override
			protected String doInBackground(Void... arg0) {
				if (randomNumbersTab.size() == 0) {
					return "无数据可导出";
				}
				int i = 0;
				StringBuffer filecontent = new StringBuffer();
				filecontent.append("传感器数据\n");
				filecontent.append("-------------\n");
				for (Float r : randomNumbersTab) {
					filecontent.append(String.format("%d: %.3f\n", (i+1), r));
					i++;
				}
				return Tools.savaFileToSD(BleSamplingActivity.this, filecontent.toString());
			}
			
			@Override
			protected void onPostExecute(String result) {
				if (result != null && result.endsWith(".txt")) {
					Tools.showToast(BleSamplingActivity.this,result);
					mParentDirectory = new File(result);
				} else {
					Tools.showToast(BleSamplingActivity.this, result);
				}
			};
		}.execute();
	}
	
	private void initData() {
		
	}
	
	/**
	 * 初始化蓝牙
	 * */
	protected void initBt() {
		bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
		
		Intent intent = getIntent();
		remoteAddress = intent.getStringExtra(MainActivity.TAG_REMOTE_ADDRESS);
		remoteName = intent.getStringExtra(MainActivity.TAG_REMOTE_NAME);
		
		remoteDevice = bluetoothAdapter.getRemoteDevice(remoteAddress);
		// Attempt to connect to the device
		connect(remoteDevice);
		Tools.showToast(this, "蓝牙连接成功："+remoteDevice.getName());
		
		//大概意思是说就算本程序退出了，依然保持蓝牙连接HC05模块
		mPendingIntent = PendingIntent.getActivity(this, 0, new Intent(this,
				getClass()), 0);
	}
	public void connect(BluetoothDevice device) {
		// Start the thread to connect with the given device
		mConnectThread = new ConnectThread(device);
		mConnectThread.start();
	}
	
	/**
	 * This thread runs while attempting to make an outgoing connection with a
	 * device. It runs straight through; the connection either succeeds or
	 * fails.
	 */
	private class ConnectThread extends Thread {
		private final BluetoothSocket mmSocket;
		private final BluetoothDevice mmDevice;
	
		public ConnectThread(BluetoothDevice device) {
			mmDevice = device;
			BluetoothSocket tmp = null;

			// Get a BluetoothSocket for a connection with the
			// given BluetoothDevice
			try {
				tmp = device.createRfcommSocketToServiceRecord(UUID
						.fromString(BLUETOOTH_UUID));
			} catch (IOException e) {
				Log.e(TAG, "create() failed", e);
			}
			mmSocket = tmp;
		}

		public void run() {
			Log.i(TAG, "BEGIN mConnectThread");
			setName("ConnectThread");

			// Always cancel discovery because it will slow down a connection
			bluetoothAdapter.cancelDiscovery();

			// Make a connection to the BluetoothSocket
			try {
				// This is a blocking call and will only return on a
				// successful connection or an exception
				mmSocket.connect();
			} catch (IOException e) {

				Log.e(TAG, "unable to connect() socket", e);
				// Close the socket
				try {
					mmSocket.close();
				} catch (IOException e2) {
					Log.e(TAG,"unable to close() socket during connection failure",e2);
				}
				return;
			}

			mConnectThread = null;

			// Start the connected thread
			// Start the thread to manage the connection and perform
			// transmissions
			mConnectedThread = new ConnectedThread(mmSocket);
			mConnectedThread.start();

		}
		public void cancel() {
			try {
				mmSocket.close();
			} catch (IOException e) {
				Log.e(TAG, "close() of connect socket failed", e);
			}
		}
	}
	
	/**
	 * This thread runs during a connection with a remote device. It handles all
	 * incoming and outgoing transmissions.
	 */
	private class ConnectedThread extends Thread {
		private final BluetoothSocket mmSocket;
		private final InputStream mmInStream;
		private final OutputStream mmOutStream;
		long millStart = System.currentTimeMillis();
		
		public ConnectedThread(BluetoothSocket socket) {
			Log.d(TAG, "create ConnectedThread");
			mmSocket = socket;
			InputStream tmpIn = null;
			OutputStream tmpOut = null;

			// Get the BluetoothSocket input and output streams
			try {
				tmpIn = socket.getInputStream();
				tmpOut = socket.getOutputStream();
			} catch (IOException e) {
				Log.e(TAG, "temp sockets not created", e);
			}

			mmInStream = tmpIn;
			mmOutStream = tmpOut;
		}
		public void run() {
			Log.i(TAG, "BEGIN mConnectedThread");
			byte[] buffer = new byte[256];
			int bytes;

			// Keep listening to the InputStream while connected
			while (true) {
				try {
					// Read from the InputStream
					bytes = mmInStream.read(buffer);
					synchronized (mBuffer) {
						for (int i = 0; i < bytes; i++) {
							mBuffer.add(buffer[i] & 0xFF);
						}
					}
					mHandler.sendEmptyMessage(MSG_NEW_DATA);
				} catch (IOException e) {
					Log.e(TAG, "disconnected", e);
					break;
				}
			}
		}

		/**
		 * Write to the connected OutStream.
		 * 
		 * @param buffer
		 *            The bytes to write
		 */
		public void write(byte[] buffer) {
			try {
				mmOutStream.write(buffer);
			} catch (IOException e) {
				Log.e(TAG, "Exception during write", e);
			}
		}

		public void cancel() {
			try {
				mmSocket.close();
			} catch (IOException e) {
				Log.e(TAG, "close() of connect socket failed", e);
			}
		}
	}
	
	private Handler mHandler = new Handler() {
		@Override
		public void handleMessage(Message msg) {
			// TODO Auto-generated method stub
			switch (msg.what) {
			case MSG_NEW_DATA:
				if (isPause) {
					break;
				} else {
					StringBuffer buf = new StringBuffer();
					synchronized (mBuffer) {
						if (mCodeType == ASCII) {
							for (int i : mBuffer) {
								buf.append((char) i);
								single_data = buf.toString();
								int  sb_length = buf.length();
								buf.delete(0,sb_length); 
							}
						} else if (mCodeType == HEX) {
							for (int i : mBuffer) {
								buf.append(Integer.toHexString(i));
								buf.append(' ');
							}
						} else {
							for (int i : mBuffer) {
								buf.append(i);
								buf.append(' ');
							}
						}
					}
					
				}

				break;

			default:
				break;
			}
		}
	};
	
	/**
	 * 初始化NFC
	 * */
//	protected void initNfc() {
//		mNfcAdapter = NfcAdapter.getDefaultAdapter(this);
//		if (mNfcAdapter == null) {
//			Tools.showToast(this, "该设备无NFC功能!");
////			finish();
////			return;
//		}
//		try {
//			if (!mNfcAdapter.isEnabled()) {
//				Tools.showToast(this, "该设备无NFC功能!");
////				finish();
////				return;
//			}
//		} catch (Exception e) {
////			finish();
////			return;
//		}
//
//		mPendingIntent = PendingIntent.getActivity(this, 0, new Intent(this,
//				getClass()), 0);
//	}

	@Override
	protected void onResume() {
		super.onResume();
		if (mNfcAdapter != null)
			mNfcAdapter.enableForegroundDispatch(this, mPendingIntent, null,null);
		processIntent(getIntent());
	}

	@SuppressWarnings("deprecation")
	@Override
	protected void onPause() {
		super.onPause();
//		BluetoothAdapter.disableForegroundNdefPush(this);
	}
	
	@Override
	protected void onNewIntent(Intent intent) {
		super.onNewIntent(intent);
		processIntent(intent);
	}
	
	private void processIntent(Intent intent) {
		if (intent != null) {
			String action = intent.getAction();
//			boolean tagDiscovered = bluetoothAdapter.ACTION_DISCOVERY_FINISHED
//					.equals(action);
//			boolean techDiscovered = bluetoothAdapter.STATE_CONNECTED
//					.equals(action);
			boolean tagDiscovered = true;
			boolean techDiscovered = true;      //随便加的
			if (tagDiscovered || techDiscovered) {
				this.mKeepIntent = intent;
				if (mTagUtil == null) {
					try {
						mTagUtil = TagUtil.selectTag(intent, isCheckSUM);
						String uidString = TagUtil.getUid();
//						Tools.showToast(MainActivity.this, "ID020=" + uidString);
						
//						int authAddress = mTagUtil.getAuthenticationAddr(intent, isCheckSUM);
//						if (authAddress > 48) {
//							Tools.showToast(MainActivity.this, uidString + "鎵�鏈塸age涓嶉渶瑕�?獙璇�?");
//						} else {
//							Tools.showToast(MainActivity.this, uidString + "闇�瑕侀獙璇乸age" + authAddress + "鍒�48");
//						}
					} catch (Exception e) {
						e.printStackTrace();
					}
				}
			}
		}
	}
	
//	private String readBleValue(){
//		InputStream in =null;
////		float f=0;
//		String str = "0";
//		try {
//			socket = remoteDevice.createRfcommSocketToServiceRecord(UUID.fromString(BLUETOOTH_UUID));
//			socket.connect();
//			in = socket.getInputStream();
//			byte[] buffer = new byte[4];
//			int c = 0;
//			while((c = in.read(buffer)) > 0) {
//				str= new String(buffer,0,c,"gbk");
////				f = Float.parseFloat(str);
//			}
//		} catch (IOException e) {
//			e.printStackTrace();
//		}finally {
//			try {
//				if(in!=null) {
//					in.close();
//				}
//			}catch(IOException e){
//				e.printStackTrace();
//			}
//		}
//		return str;
//	}
	
	/**
	 * 连续采集的函数（线程执行的方式）
	 * */
//	private void continuousDetectingBLE1(final int maxMillseconds) {
//		clientThread = new ClientThread();
//		clientThread.start();
//	}
//	private class ClientThread extends Thread{
//		public void run() {
//			String str = readBleValue();
//			float f = Float.parseFloat(str);
//			Log.i("Main", "传感器数据：" + f);
////			Log.i("Main", "传感器�?�：" + f);
//			generateValues(f);
//			generateData(System.currentTimeMillis() - millseconds);
//		}
//	}
	
	/**
	 * 连续采集的函数（后台执行的方式）
	 * */
	private void continuousDetectingBLE2(final int maxMillseconds) {
//		Tools.showToast(this, "采集数据中");
		new AsyncTask<Void, Float, Void>() {
			@Override
			protected Void doInBackground(Void... arg0) {
				long millStart = System.currentTimeMillis();
				while (System.currentTimeMillis() - millStart <= maxMillseconds) {
//					boolean writeSuccess = Tools.write32H(mTagUtil, mKeepIntent, isCheckSUM);
					boolean writeSuccess = true;
					if (writeSuccess) {
						try {
							TimeUnit.MILLISECONDS.sleep(40);
//							float f = Tools.readRValue(mTagUtil, mKeepIntent, isCheckSUM); 		//数据来源
							
							float f = Float.parseFloat(single_data);
							Log.i(TAG, "传感器数据：" + f);
//							Log.i("Main", "传感器�?�：" + f);
							generateValues(f);
							generateData(System.currentTimeMillis() - millStart);
							publishProgress(f);
						} catch (Exception e) {
							e.printStackTrace();
							generateValues(0.0f);
							generateData(System.currentTimeMillis() - millStart);
							publishProgress(0.0f);
							return null;
						}
					} else {
						generateValues(0.0f);
						generateData(System.currentTimeMillis() - millStart);
						publishProgress(0.0f);
						return null;
					}
				}
				return null;
			}
			
			@Override
			protected void onPreExecute() {
				chart.setVisibility(0);
				mLoopCounter = 0;
//				mTipView.setText("姝ｅ湪杩愯..");
				setTitle(R.string.app_name);
				resetViewport(maxMillseconds);
			}
			
			@Override
			protected void onProgressUpdate(Float... values) {
				mLoopCounter++;
				if (values != null && values.length > 0 && values[0] > mMaxRm) {
					mMaxRm = values[0] +1;
					resetViewport(maxMillseconds);
				}
				if (values != null && values.length > 0 && values[0] <mMinRm) {
					mMinRm = values[0]-1;
					resetViewport(maxMillseconds);
				}
			}
			
			@Override
			protected void onPostExecute(Void result) {
				setTitle(String.format("%s  总计读取%d", getString(R.string.app_name), mLoopCounter));
				doStop();
			}
		}.execute();
		Tools.showToast(this, "采集完成");
	}
	
	private void generateValues(Float f) {
		randomNumbersTab.add(f);
		numberOfPoints = randomNumbersTab.size();
	}
	
	private void generateData(long timeSpanMill) {
		if (mLines == null) {
			mLines = new ArrayList<Line>();
		}
		
		if (mValues == null) {
			mValues = new ArrayList<PointValue>();
		}
		
		mValues.add(new PointValue(timeSpanMill, randomNumbersTab.get(numberOfPoints - 1)));
		
		if (mLine == null) {
			mLine = new Line(mValues);
			mLine.setColor(ChartUtils.COLORS[0]);
			mLine.setShape(shape);
			mLine.setCubic(isCubic);
			mLine.setFilled(isFilled);
			mLine.setHasLabels(hasLabels);
			mLine.setHasLabelsOnlyForSelected(hasLabelForSelected);
			mLine.setHasLines(hasLines);
			mLine.setHasPoints(hasPoints);
			// line.setHasGradientToTransparent(hasGradientToTransparent);
			if (pointsHaveDifferentColor) {
				mLine.setPointColor(ChartUtils.COLORS[(0 + 1)
						% ChartUtils.COLORS.length]);
			}
		}
		
		if (mLines.size() == 0) {
			mLines.add(mLine);
		}
		
		if (data == null) {
			data = new LineChartData(mLines);
			
			if (hasAxes) {
				Axis axisX = new Axis();
				Axis axisY = new Axis().setHasLines(true);
				if (hasAxesNames) {
					axisX.setName("时序");
					axisY.setName("电阻�?(KΩ)");
				}
				data.setAxisXBottom(axisX);
				data.setAxisYLeft(axisY);
			} else {
				data.setAxisXBottom(null);
				data.setAxisYLeft(null);
			}
			data.setBaseValue(Float.NEGATIVE_INFINITY);
		}
		
		chart.setLineChartData(data);
	}
	
	//打开本地数据
	private void ImportLocalData() {
		Tools.showToast(this, "正在打开本地数据");
		Intent intent=new Intent(Intent.ACTION_GET_CONTENT );
		intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
		intent.setType("*/*");
		intent.addCategory(Intent.CATEGORY_OPENABLE);
		startActivityForResult(intent,Activity.RESULT_OK);
		
	}
	
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
	    if (resultCode == Activity.RESULT_OK) {//是否选择，没选择就不会继�?
//	    	try {
	    	              Uri uri=data.getData();
	    	              Tools.showToast(this, "hello");
//	    	               String chooseFilePath=FileChooseUtil.getInstance(this).getChooseFileResultPath(uri);
//	    	              Tools.showToast(this,chooseFilePath);
	    }
//	    	}catch(Exception e) {
//	    		Tools.showToast(this, e.getMessage());
//	    	}
	   
	}
	
	private void doStart() {
//		hasStarted = true;
		if (mKeepIntent == null) {
			Tools.showToast(this, "数据读取失败");
			return;									
		}
		mBtnStart.setEnabled(false);
		mBtnStart.setText(R.string.stop);
		mBtnStart.setBackgroundResource(R.drawable.button_disabled_bg);
		mBtnStart.setCompoundDrawablesWithIntrinsicBounds(R.drawable.icon_stop,
				0, 0, 0);
		mEdtTime.setEnabled(false);
		// Clear all data
		randomNumbersTab.clear();
		mValues.clear();
//		Tools.bitsCache.clear();
//		Tools.hexCache.clear();
		
		numberOfPoints = 0;
		// Add testing points one by one.
		String secStr = mEdtTime.getText().toString();
		int millseconds = Integer.parseInt(secStr) * 1000;
		if(millseconds<10000) {
			millseconds=10000;
		}
		//开启连接之后的线程
//		mConnectThread = null;
//		// Start the connected thread
//		// Start the thread to manage the connection and perform
//		// transmissions
//		mConnectedThread = new ConnectedThread(mmSocket);
//		mConnectedThread.start();
		
		
		
//		contentTv.append(single_data);
		continuousDetectingBLE2(millseconds);
	}
	
	private void doStop() {
		mMinRm=1024;
		mMaxRm =0;
		mBtnStart.setEnabled(true);
		mBtnStart.setText(R.string.start);
		mBtnStart.setBackgroundResource(R.drawable.button_enable_green_bg);
		mBtnStart.setCompoundDrawablesWithIntrinsicBounds(
				R.drawable.icon_start, 0, 0, 0);
		mEdtTime.setEnabled(true);
	}

	private class ValueTouchListener implements LineChartOnValueSelectListener {
		private final Context mContext;

		public ValueTouchListener(Context context) {
			this.mContext = context;
		}

		@Override
		public void onValueSelected(int lineIndex, int pointIndex, PointValue value) {
			Tools.showToast(mContext, String.format("ID009: %.3f ID010", (pointIndex+1), value.getY()));
			/*
			String addrContent = "";
			if (Tools.hexCache.size() > pointIndex) {
				addrContent = Tools.hexCache.get(pointIndex);
			}
			Tools.showToast(mContext, String.format("绗�%d娆＄數闃诲��?: %.3f 鍗冩\n24H鍦板潃鍐呭锛�%s", 
					(pointIndex+1), value.getY(), addrContent));
			*/
		}

		@Override
		public void onValueDeselected() {

		}
	}
}
