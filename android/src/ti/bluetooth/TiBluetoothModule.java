/**
 * This file was auto-generated by the Titanium Module SDK helper for Android
 * Appcelerator Titanium Mobile
 * Copyright (c) 2009-2010 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Apache Public License
 * Please see the LICENSE included with this distribution for details.
 *
 */
package ti.bluetooth;

import java.util.ArrayList;
import java.util.List;

import org.appcelerator.kroll.KrollDict;
import org.appcelerator.kroll.KrollFunction;
import org.appcelerator.kroll.KrollModule;
import org.appcelerator.kroll.annotations.Kroll;
import org.appcelerator.kroll.common.Log;
import org.appcelerator.titanium.TiApplication;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.bluetooth.BluetoothSocket;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanFilter;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.content.Context;
import android.content.IntentFilter;
import android.os.ParcelUuid;

@Kroll.module(name = "TiBluetooth", id = "ti.bluetooth")
public class TiBluetoothModule extends KrollModule {

	public interface ConnectionCallback {
		void onConnectionStateChange(BluetoothDevice device, int newState);
	}

	public static final String LCAT = "BLE";
	private BluetoothManager btManager;
	private BluetoothAdapter btAdapter;
	private TiApplication appContext;
	private Activity activity;
	private KrollFunction onFound;
	private KrollFunction onConnections;
	private BluetoothLeScanner btScanner;

	private int currentState = 0;
	private boolean isScanning = false;

	@Kroll.constant
	public static final int MANAGER_STATE_UNKNOWN = 0;
	@Kroll.constant
	public static final int MANAGER_STATE_UNSUPPORTED = 1;
	@Kroll.constant
	public static final int MANAGER_STATE_UNAUTHORIZED = 2;
	@Kroll.constant
	public static final int MANAGER_STATE_POWERED_OFF = 10;
	@Kroll.constant
	public static final int MANAGER_STATE_POWERED_ON = 12;
	@Kroll.constant
	public static final int MANAGER_STATE_RESETTING = 5;

	@Kroll.constant
	public static final int SCAN_MODE_BALANCED = ScanSettings.SCAN_MODE_BALANCED;
	@Kroll.constant
	public static final int SCAN_MODE_LOW_LATENCY = ScanSettings.SCAN_MODE_LOW_LATENCY;
	@Kroll.constant
	public static final int SCAN_MODE_LOW_POWER = ScanSettings.SCAN_MODE_LOW_POWER;
	@Kroll.constant
	public static final int SCAN_MODE_OPPORTUNISTIC = ScanSettings.SCAN_MODE_OPPORTUNISTIC;

	@Kroll.constant
	public static final int PROFILE_ADP = BluetoothProfile.A2DP;

	@Kroll.constant
	public static final int PROFILE_HEADSET = BluetoothProfile.HEADSET;
	@Kroll.constant
	public static final int PROFILE_HEALTH = BluetoothProfile.HEALTH;

	@Kroll.constant
	public static final int DEVICE_BOND_BONDED = BluetoothDevice.BOND_BONDED;
	@Kroll.constant
	public static final int DEVICE_BOND_BONDING = BluetoothDevice.BOND_BONDING;
	@Kroll.constant
	public static final int DEVICE_BOND_NONE = BluetoothDevice.BOND_NONE;

	@Kroll.constant
	public static final int TYPE_L2CAP = BluetoothSocket.TYPE_L2CAP;
	@Kroll.constant
	public static final int TYPE_RFCOMM = BluetoothSocket.TYPE_RFCOMM;
	@Kroll.constant
	public static final int TYPE_SCO = BluetoothSocket.TYPE_SCO;

	public final int DEFAULT_SCAN_MODE = SCAN_MODE_LOW_POWER;
	private int currentScanmode = DEFAULT_SCAN_MODE;

	public TiBluetoothModule() {
		super();
		appContext = TiApplication.getInstance();
		activity = appContext.getCurrentActivity();
		IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
		filter.addAction(BluetoothDevice.ACTION_UUID);
		filter.addAction(BluetoothDevice.ACTION_FOUND);
		filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_STARTED);
		filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
		filter.addAction(BluetoothAdapter.ACTION_STATE_CHANGED);
		filter.addAction(BluetoothDevice.ACTION_BOND_STATE_CHANGED);
		appContext.registerReceiver(new BlutoothStateChangedBroadcastReceiver(
				TiBluetoothModule.this, btAdapter), filter);
	}

	@Kroll.onAppCreate
	public static void onAppCreate(TiApplication app) {
		Log.d(LCAT, "inside onAppCreate");
	}

	private final ScanCallback scanCallback = new ScanCallback() {
		@Override
		public void onScanResult(int callbackType, ScanResult result) {
			BluetoothDevice device = result.getDevice();
			if (device != null) {
				BluetoothDeviceProxy btDeviceProxy = new BluetoothDeviceProxy(
						device);
				if (device.getName() != null) {
					Log.d(LCAT,
							"Found: " + device.getName() + " "
									+ device.getAddress());
					ArrayList<String> ids = new ArrayList<String>();
					if (device.getUuids() != null) {
						for (ParcelUuid id : device.getUuids()) {
							ids.add(id.toString());
						}
					}
					KrollDict kd = new KrollDict();
					kd.put("device", btDeviceProxy);

					kd.put("name", device.getName());
					kd.put("address", device.getAddress());
					kd.put("bondState", device.getBondState());
					kd.put("ids", ids.toArray());
					fireEvent("didDiscoverPeripheral", kd);
					BluetoothGatt bluetoothGatt = device.connectGatt(
							appContext, false,
							new BluetoothGattCallbackHandler(
									TiBluetoothModule.this));
					btScanner.stopScan(scanCallback);
				}
			}
		}
	};

	private List<ScanFilter> scanFilters() {
		String[] ids = {};
		return scanFilters(ids);
	}

	private List<ScanFilter> scanFilters(String[] ids) {
		List<ScanFilter> list = new ArrayList<ScanFilter>(1);
		for (int i = 0; i < ids.length; i++) {
			ScanFilter filter = new ScanFilter.Builder().setServiceUuid(
					ParcelUuid.fromString(ids[i])).build();
			list.add(filter);
		}
		return list;
	}

	@Kroll.method
	public int getState() {
		return currentState;
	}

	@Kroll.method
	public boolean isScanning() {
		return isScanning;
	}

	@Kroll.method
	public void initialize() {
		btManager = (BluetoothManager) appContext
				.getSystemService(Context.BLUETOOTH_SERVICE);
		btAdapter = btManager.getAdapter();
		if (btAdapter != null) {
			setCurrentState(btAdapter.getState());
		} else {
			setCurrentState(MANAGER_STATE_UNSUPPORTED);
		}
	}

	@Kroll.method
	public void startScan(@Kroll.argument(optional = true) int _scanmode) {
		if (btAdapter != null) {
			ScanSettings scanSettings = new ScanSettings.Builder().setScanMode(
					_scanmode).build();
			btScanner = btAdapter.getBluetoothLeScanner();
			btScanner.startScan(scanFilters(), scanSettings, scanCallback);
			// btScanner.startScan(scanCallback);
			isScanning = true;
		}
	}

	@Kroll.method
	public void startScanWithServices(String[] ids) {
		if (btAdapter != null) {
			ScanSettings settings = new ScanSettings.Builder().setScanMode(
					ScanSettings.SCAN_MODE_BALANCED).build();
			btScanner = btAdapter.getBluetoothLeScanner();
			btScanner.startScan(scanFilters(ids), settings, scanCallback);
			isScanning = true;
		}
	}

	// @Override
	// public void eventListenerAdded(String eventName, int count, KrollProxy
	// proxy) {
	// super.eventListenerAdded(eventName, count, proxy);
	//
	// if (eventName.equals("didDiscoverPeripheral")) {
	//
	// }
	// }

	public void setCurrentState(int currentState) {
		this.currentState = currentState;
	}

	@Kroll.setProperty
	@Kroll.method
	public void setScanMode(int scanMode) {
		if (scanMode == SCAN_MODE_BALANCED || scanMode == SCAN_MODE_LOW_POWER
				|| scanMode == SCAN_MODE_LOW_LATENCY
				|| scanMode == SCAN_MODE_OPPORTUNISTIC) {
			currentScanmode = scanMode;
		} else {
			currentScanmode = SCAN_MODE_LOW_POWER;
		}

	}

	@Kroll.method
	public int getScanMode() {
		return currentScanmode;
	}

	@Kroll.method
	public void stopScan() {
		if (btAdapter != null) {
			btScanner.stopScan(scanCallback);
			isScanning = false;
		}
	}

	@Kroll.method
	public void flushPendingScanResults() {
		if (btAdapter != null) {
			btScanner.flushPendingScanResults(scanCallback);
			// isScanning = false;
		}
	}
}
