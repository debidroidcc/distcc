package de.tubs.ibr.distcc;


import android.app.Application;
import android.content.SharedPreferences;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.util.Log;
import android.widget.Toast;

/**
 * Created with IntelliJ IDEA.
 * User: goforthanddie
 * Date: 21.09.12
 * Time: 12:14
 * To change this template use File | Settings | File Templates.
 */
public class DebiDroidCC extends Application {



	private static final String TAG = DebiDroidCC.class.getName();

	private Handler handler;
	public SharedPreferences sharedPreferences;
	public boolean isRunning = false;


	@Override
	public void onCreate() {
		super.onCreate();

		this.handler = new Handler(getMainLooper());
		this.sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());

	}

}
