package com.xiaofu_yan.smartbikedemo;

import java.util.ArrayList;
import java.util.List;

import android.app.ListActivity;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.TextView;

import com.xiaofu_yan.blux.smart_bike.SmartBike;
import com.xiaofu_yan.blux.smart_bike.SmartBikeManager;
import com.xiaofu_yan.blux.smart_bike.SmartBikeServerConnection;

public class ScanActivity extends ListActivity {

	private class BlueGuardList extends BaseAdapter {
		@Override
		public int getCount() {
			return mDevices.size();
		}

		@Override
		public Object getItem(int i) {
			return mDevices.get(i);
		}

		@Override
		public long getItemId(int i) {
			return i;
		}

		@Override
		public View getView(int i, View view, ViewGroup viewGroup) {
			// General ListView optimization code.
			LayoutInflater inflater = ScanActivity.this.getLayoutInflater();
			View row = inflater.inflate(R.layout.listitem_device, null);//viewGroup);
			updateListItem(row, getDevice(i));
			return row;
		}

		// Public members.
		void addDevice(DeviceDB.Record r) {
			for(int i=0; i<mDevices.size(); i++) {
				//ListItem t = mDevices.get(i);
				//if(t.rec.identifier.equals(r.identifier)) return;
				DeviceDB.Record t = mDeviceList.getDevice(i);
				if(t.identifier.equals(r.identifier)) return;
			}
			
			ListItem item = new ListItem(r);
			mDevices.add(item);
			notifyDataSetChanged();
		}
		
		DeviceDB.Record getDevice(int index) {
			return mDevices.get(index).rec;
		}

		void reset() {
			mDevices.clear();
			DeviceDB.Record rec = DeviceDB.load(ScanActivity.this);
			if(rec != null)
				mDeviceList.addDevice(rec);
			notifyDataSetChanged();
		}

		// Private members.
		private class ListItem {
			DeviceDB.Record rec;
			ListItem(DeviceDB.Record r) {
				rec = r;
			}
		}
		private List<ListItem> mDevices = new ArrayList<ListItem>();

		private void updateListItem(View row, DeviceDB.Record rec) {
			TextView txt;
			txt = (TextView)row.findViewById(R.id.device_name);
			txt.setText(rec.name);
			txt = (TextView)row.findViewById(R.id.device_address);
			txt.setText(rec.identifier);
		}
	}

	// Private members.
	private SmartBikeServerConnection mConnection;
	private SmartBikeManager mBlueGuardManager;
	private BlueGuardList mDeviceList;
	

	// activity overrides.
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		mConnection = new SmartBikeServerConnection();
		mConnection.delegate = new ServerConnectionDelegate();
		
		mDeviceList = new BlueGuardList();
		mDeviceList.reset();
	}

	@Override
	protected void onStart() {
		setListAdapter(mDeviceList);
		super.onStart();
	}

	@Override
	protected void onStop() {
		super.onStop();
	}

	@Override
	protected void onResume() {
		mConnection.connect(this);
		super.onResume();
	}

	@Override
	protected void onPause() {
		super.onPause();

		mDeviceList.reset();
		mConnection.disconnect();
	}

	@Override
	protected void onListItemClick(ListView l, View v, int position, long id) {
		DeviceDB.Record rec = mDeviceList.getDevice(position);
		Intent intent = new Intent(this, BlueGuardActivity.class);
		intent.putExtra("identifier", rec.identifier);
		if(rec.name != null)
			intent.putExtra("name", rec.name);
		if(rec.key != null && rec.key.length() > 0)
			intent.putExtra("key", rec.key);
		startActivity(intent);
	}

	// options menu override.
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.ble, menu);

		MenuItem mItem = menu.findItem(R.id.start_stop_scan);
		if (mBlueGuardManager != null) {
			mItem.setEnabled(true);
			if (mBlueGuardManager.isScanning())
				mItem.setTitle("Stop scan");
			else
				mItem.setTitle("Start scan");
		} else {
			mItem.setEnabled(false);
		}
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case R.id.start_stop_scan:
			if (mBlueGuardManager.isScanning()) {
				mBlueGuardManager.stopScan();
			} else {
				mBlueGuardManager.scanSmartBike();
				mDeviceList.reset();
			}
			invalidateOptionsMenu();
			break;
		}
		return super.onContextItemSelected(item);
	}

	// BlueGuardServerConnection.Delegate
	private class ServerConnectionDelegate extends SmartBikeServerConnection.Delegate {
		@Override
		public void smartBikeServerConnected(SmartBikeManager smartBikeManager) {
			mBlueGuardManager = smartBikeManager;
			mBlueGuardManager.delegate = new SmartBikeManagerDelegate();
			mBlueGuardManager.setContext(getBaseContext());
			invalidateOptionsMenu();
		}

		@Override
		public void smartBikeServerDisconnected() {
		}
	}

	// SmartGuardManager.Delegate
	private class SmartBikeManagerDelegate extends SmartBikeManager.Delegate {
		@Override
		public void smartBikeManagerFoundSmartBike(String identifier, String name, int nMode) {
			DeviceDB.Record rec = new DeviceDB.Record(name, identifier, null);
			mDeviceList.addDevice(rec);
		}

		@Override
		public void smartBikeManagerGotSmartBike(SmartBike smartBike) {
		}
	}

}
