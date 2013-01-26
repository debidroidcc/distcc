package de.tubs.ibr.distcc;

import android.content.*;
import android.os.BatteryManager;
import android.preference.PreferenceManager;
import android.util.Log;

import java.io.*;
import java.lang.Override;import java.lang.String;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.*;

/**
 * Created with IntelliJ IDEA.
 * User: goforthanddie
 * Date: 25.01.13
 * Time: 16:10
 * To change this template use File | Settings | File Templates.
 */
public class PowerConnectionReceiver extends BroadcastReceiver {
	private final static String TAG = PowerConnectionReceiver.class.getName();
	private final int PORT = 8855;
	private final int STATUS_PLUGGED = 0;
	private final int STATUS_UNPLUGGED = 1;

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

		// THIS IS DIRTY BUT INTENT SEEMS EMPTY :/
		IntentFilter ifilter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
		Intent batteryStatus = context.registerReceiver(null, ifilter);
		// Are we charging / charged?
		int statusa = batteryStatus.getIntExtra(BatteryManager.EXTRA_STATUS, -1);
		boolean isCharginga = (statusa == BatteryManager.BATTERY_STATUS_CHARGING || statusa == BatteryManager.BATTERY_STATUS_FULL);

		int chargePluga = batteryStatus.getIntExtra(BatteryManager.EXTRA_PLUGGED, -1);
		boolean usbChargea = chargePluga == BatteryManager.BATTERY_PLUGGED_USB;
		boolean acChargea = chargePluga == BatteryManager.BATTERY_PLUGGED_AC;
		Log.i(TAG, "usbChargea=" + usbChargea + " acChargea=" + acChargea);

		final SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context.getApplicationContext());
		masterIP = sharedPreferences.getString(PreferencesActivity.KEY_MASTERIP, context.getString(R.string.pref_masterIP_d));
		Log.i(TAG, "masterIP " + masterIP);

		if(acChargea && app.isRunning) {
			sendStatus(STATUS_PLUGGED);
		} else {
			sendStatus(STATUS_UNPLUGGED);
		}
	}

	private void sendStatus(final int status) {
		new Thread(new Runnable() {

			@Override
			public void run() {
				try {

					final String ip = masterIP;
					final int port = PORT;
					final Socket s = new Socket(ip, port);

					//outgoing stream redirect to socket
					OutputStream out = s.getOutputStream();
					PrintWriter output = new PrintWriter(out);
					output.println(status);
					output.flush();
					output.close();
					out.flush();
					out.close();
					BufferedReader input = new BufferedReader(new InputStreamReader(s.getInputStream()));

					//read line(s)
					String st = input.readLine();

					//Close connection
					s.close();


				} catch(UnknownHostException e) {
					Log.i(TAG, e.getMessage());
					e.printStackTrace();
				} catch(IOException e) {
					Log.i(TAG, e.getMessage());
					e.printStackTrace();
				}
			}
		}).start();
	}
}
