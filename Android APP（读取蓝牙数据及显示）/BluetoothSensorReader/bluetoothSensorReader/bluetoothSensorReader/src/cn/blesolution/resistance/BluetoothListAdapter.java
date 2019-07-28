package cn.blesolution.resistance;

import java.util.List;

import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

public class BluetoothListAdapter extends BaseAdapter{
	private Context context=null;
	private List<BluetoothDevice>devices = null;
	
	public BluetoothListAdapter(Context context,List<BluetoothDevice>devices) {
		this.context = context;
		this.devices = devices;
	}
	
	@Override
	public int getCount() {
		return devices.size();
	}
	
	@Override
	public Object getItem(int position) {
		return devices.get(position);
	}
	
	@Override
	public long getItemId(int position) {
		return 0;
	}
	
	@Override
	public View getView(int position,View convertView,ViewGroup parent) {
		BluetoothDevice device = devices.get(position);
		if(convertView == null) {
			convertView = LayoutInflater.from(context).inflate(R.layout.item_device_list,null);
		}
		TextView nameTv = (TextView)convertView.findViewById(R.id.item_device_name_tv);
		TextView macTv = (TextView)convertView.findViewById(R.id.item_device_mac_tv);
		TextView stateTv = (TextView)convertView.findViewById(R.id.item_device_state_tv);
		nameTv.setText(device.getName());
		macTv.setText(device.getAddress());
		stateTv.setText(convertState(device.getBondState()));
		return convertView;
	}
	
	/**
	 * ������ת�����ı�
	 * */
	private String convertState(int bondState) {
		switch(bondState) {
		case BluetoothDevice.BOND_BONDED:
			return "�����";
		case BluetoothDevice.BOND_BONDING:
			return "�����";
		case BluetoothDevice.BOND_NONE:
			return "δ���";
		}
		return "δ֪״̬";
	}
}
