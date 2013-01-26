package de.tubs.ibr.distcc;

import android.content.*;
import android.os.BatteryManager;
import android.preference.PreferenceManager;
import android.util.Log;

import java.io.*;
import java.lang.Override;import java.lang.String;
import java.net.Socket;
import java.net.UnknownHostException;

/**
 * Created with IntelliJ IDEA.
 * User: goforthanddie
 * Date: 25.01.13
 * Time: 16:10
 * To change this template use File | Settings | File Templates.
 */
public class PowerConnectionReceiver extends BroadcastReceiver {
	private final static String TAG = PowerConnectionReceiver.class.getName();


	private String masterIP;
	private int masterPort;

	private DebiDroidCC app;
	@Override
	public void onReceive(Context context, Intent intent) {

		app = (DebiDroidCC) context.getApplicationContext();
		Log.i(TAG, "APPTEST " + app.isRunning);
		int status = intent.getIntExtra(BatteryManager.EXTRA_STATUS, -1);
		boolean isCharging = (status == BatteryManager.BATTERY_STATUS_CHARGING || status == BatteryManager.BATTERY_STATUS_FULL);

		int chargePlug = intent.getIntExtra(BatteryManager.EXTRA_PLUGGED, -1);
		Log.i(TAG, "chargePlug="+chargePlug+" status="+status);
		boolean usbCharge = (chargePlug == BatteryManager.BATTERY_PLUGGED_USB);
		boolean acCharge = (chargePlug == BatteryManager.BATTERY_PLUGGED_AC);

		Log.i(TAG, "usbCharge=" + usbCharge + " acCharge=" + acCharge);

		app.checkCharging();
	}


}
