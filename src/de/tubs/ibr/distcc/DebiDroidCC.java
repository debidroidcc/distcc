package de.tubs.ibr.distcc;


import android.app.Application;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.BatteryManager;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.util.Log;
import android.widget.Toast;

import java.io.*;
import java.net.Socket;
import java.net.UnknownHostException;

/**
 * Created with IntelliJ IDEA.
 * User: goforthanddie
 * Date: 21.09.12
 * Time: 12:14
 * To change this template use File | Settings | File Templates.
 */
public class DebiDroidCC extends Application {



	private static final String TAG = DebiDroidCC.class.getName();
	protected static final int STATUS_PLUGGED = 0;
	protected static final int STATUS_UNPLUGGED = 1;

	private Handler handler;
	public SharedPreferences sharedPreferences;
	public boolean isRunning = false;


	@Override
	public void onCreate() {
		super.onCreate();

		this.handler = new Handler(getMainLooper());
		this.sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());

	}


	protected void sendStatus(final int status) {
		final SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
		final String ip = sharedPreferences.getString(PreferencesActivity.KEY_MASTER_IP, getString(R.string.pref_masterIP_d));
		final int port = Integer.parseInt(sharedPreferences.getString(PreferencesActivity.KEY_MASTER_PORT, getString(R.string.pref_masterPort_d)));
		Log.i(TAG, "sendStatus, status="+status);
		new Thread(new Runnable() {

			@Override
			public void run() {
				try {
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

	protected void checkCharging() {
		// THIS IS DIRTY BUT INTENT SEEMS EMPTY :/
		IntentFilter ifilter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
		Intent batteryStatus = registerReceiver(null, ifilter);
		// Are we charging / charged?
		int statusa = batteryStatus.getIntExtra(BatteryManager.EXTRA_STATUS, -1);
		boolean isCharginga = (statusa == BatteryManager.BATTERY_STATUS_CHARGING || statusa == BatteryManager.BATTERY_STATUS_FULL);

		int chargePluga = batteryStatus.getIntExtra(BatteryManager.EXTRA_PLUGGED, -1);
		boolean usbChargea = chargePluga == BatteryManager.BATTERY_PLUGGED_USB;
		boolean acChargea = chargePluga == BatteryManager.BATTERY_PLUGGED_AC;
		Log.i(TAG, "usbChargea=" + usbChargea + " acChargea=" + acChargea);

		if(acChargea && isRunning) {
			sendStatus(STATUS_PLUGGED);
		} else {
			sendStatus(STATUS_UNPLUGGED);
		}
	}
}
