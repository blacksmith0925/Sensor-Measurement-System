package cn.blesolution.resistance;

import java.io.File;
import java.io.FileOutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.UUID;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Environment;
import android.util.Log;
import android.widget.Toast;

import com.aofei.nfc.TagUtil;

public final class Tools {
	// 
	public static boolean write32H(TagUtil mTagUtil, Intent intent, boolean isCheckSUM) {
		boolean writeSuccess = false;
		try {
			if (intent != null && mTagUtil != null) {
				
				byte addr = 0x32;
				byte[] content = {(byte) 0xAA, (byte) 0xAA, (byte) 0xAA, (byte) 0xAA };
				writeSuccess = mTagUtil.writeTag(intent, addr, content, isCheckSUM);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return writeSuccess;
	}
	
	public static byte[] read24H(TagUtil mTagUtil, Intent intent, boolean isCheckSUM) {
		try {
			if (intent != null && mTagUtil != null) {
				byte addr = 0x24;
				return mTagUtil.readOnePage(intent, addr, isCheckSUM);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}
	
//	public static final List<String> hexCache = new ArrayList<String>();
//	public static final List<Integer> bitsCache = new ArrayList<Integer>();
	
	public static Integer trim15bits(byte[] data_read) {
		if (data_read != null && data_read.length >= 2) {
			byte high = data_read[1];
			byte low = data_read[0];
			int data = (high & 0x7f);
			int data2 = data << 8;
			int data3 = (data2) | (low & 0xff);
			// Print for testing
//			bitsCache.add(data3);
//			hexCache.add("0x" + BytesHexStrTranslate.bytesToHex(data_read));
			
			return data3;
		}
//		bitsCache.add(0);
//		hexCache.add("0x00");
		return 0;
	}
	
	public static float readRValue(TagUtil mTagUtil, Intent intent, boolean isCheckSUM) {
		byte[] dataRead = read24H(mTagUtil, intent, isCheckSUM);
		int data = Tools.trim15bits(dataRead);
		float f = 0.0f;
		if (data != 0) {
			f = (((32767.0f / data) - 1) * BleSamplingActivity.Rm);
		}
		return f;
	}
	
	public static void readNoAuthPages(TagUtil mTagUtil, Intent intent, boolean isCheckSUM) {
		if (mTagUtil.lockPageAll(intent, isCheckSUM)) {
			try {
				byte[] bytes = mTagUtil.readAllPages(intent, isCheckSUM);
				for (byte b : bytes) {
					Log.i("Main", "read byte: " + b);
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		} else {
			Log.i("Main", "lock page failed!");
		}
	}
	
	public static void showToast(Context context, String message) {
		Toast.makeText(context, message, Toast.LENGTH_LONG).show();
	}
	
	public static void shortToast(Context context, String message) {
		Toast.makeText(context, message, Toast.LENGTH_SHORT).show();
	}
	
	public static String savaFileToSD(Context context, String filecontent) {
		try {
			SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyyMMddHHmmssS", Locale.getDefault());
			String filename = simpleDateFormat.format(new Date()) + ".txt";
		    if(Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)) {
		    	String dirName = Environment.getExternalStorageDirectory().getCanonicalPath() + File.separator + "PSS";
		    	filename = dirName + File.separator + filename;
		        File file = new File(dirName);
		        if (!file.exists()) {
					file.mkdirs();
				}
		        
		    	FileOutputStream output = new FileOutputStream(filename);
		        output.write(filecontent.getBytes());
		        output.close();
		        return filename;
		    } else {
		    	return "SD鍗�?�笉瀛樺湪鎴栬�呬笉鍙鍐�?";
		    }
		} catch (Exception e) {
			return "瀵煎嚭澶辫触锛�"+ e.getMessage();
		}
	}
	
	public static void openDirectory(Context context, File parentFlie) {
		Intent intent = new Intent(Intent.ACTION_VIEW);
		intent.setDataAndType(Uri.fromFile(parentFlie), "text/plain");
		intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
		context.startActivity(intent);
	}
}
