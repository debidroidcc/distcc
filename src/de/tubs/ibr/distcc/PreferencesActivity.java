package de.tubs.ibr.distcc;

import android.app.Activity;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import android.util.Log;
import org.apache.http.message.BasicNameValuePair;
import org.json.JSONException;

/**
 * Created with IntelliJ IDEA.
 * User: goforthanddie
 * Date: 11.10.12
 * Time: 11:17
 * To change this template use File | Settings | File Templates.
 */
public class PreferencesActivity extends Activity {


	public static final String KEY_TARGET = "pref_targetDebian";
	public static final String KEY_MASTERIP = "pref_masterIP";

	@Override
	protected void onCreate(Bundle savedInstanceState) {

		super.onCreate(savedInstanceState);

		// Display the fragment as the main content.
		getFragmentManager().beginTransaction().replace(android.R.id.content, new PrefsFragment()).commit();
	}

	private class PrefsFragment extends PreferenceFragment implements SharedPreferences.OnSharedPreferenceChangeListener {

		private final String TAG = PrefsFragment.class.getName();

		@Override
		public void onCreate(Bundle savedInstanceState) {
			super.onCreate(savedInstanceState);

			PreferenceManager.getDefaultSharedPreferences(getApplicationContext()).registerOnSharedPreferenceChangeListener(this);
			// Load the preferences from an XML resource
			addPreferencesFromResource(R.xml.preferences);
			setSummary();
		}


		private void setSummary() {
			/*
			final SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
			Preference p = findPreference(KEY_SERVERIP);
			if(p != null) {
				if(isAdded())
					p.setSummary(sharedPreferences.getString(KEY_SERVERIP, getString(R.string.pref_server_ip_d)));
			}
			p = findPreference(KEY_SERVERPORT);
			if(p != null) {
				if(isAdded())
					p.setSummary(sharedPreferences.getString(KEY_SERVERPORT, getString(R.string.pref_server_port_d)));
			}
			*/
		}

		@Override
		public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String s) {
			setSummary();
		}

		@Override
		public void onStop() {
			super.onStop();

		}
	}
}
