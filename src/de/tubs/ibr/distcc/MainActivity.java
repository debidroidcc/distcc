package de.tubs.ibr.distcc;

import android.app.*;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.AssetManager;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ScrollView;
import android.widget.TextView;

import java.io.*;
import java.util.concurrent.ExecutorService;

public class MainActivity extends Activity implements SharedPreferences.OnSharedPreferenceChangeListener {

	private static final String TAG = MainActivity.class.getName();
	private static final String MOUNT_POINT = "/data";
	private static final String PATH_SCRIPTS = "/root/debidroidcc-master";
	public static final int NOTIFICATION_ID_ONPAUSE = 0;
	private boolean isRunning = false;

	private String debianDir;
	private MenuItem toggleItem;


	private static final Object runLock = new Object();


	/**
	 * Called when the activity is first created.
	 */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);

		final SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
		debianDir = MOUNT_POINT + sharedPreferences.getString(PreferencesActivity.KEY_TARGET, getString(R.string.pref_targetDebian_d));
		Log.i(TAG, "debianDir="+debianDir);


		/*
		final ScrollView scrollView = (ScrollView) findViewById(R.id.scroll);
		scrollView.postDelayed(new Runnable() {
			@Override
			public void run() {
				scrollView.fullScroll(ScrollView.FOCUS_DOWN);
			}
		}, 500);
		*/
		//move all the assets to certain directory
		//maybe we just do this for the first time ;)
		try {
			String[] assets = { "busybox", "deploy_debian_bootstrap.sh", "deploy-equipped-debian.sh"}; //only this busybox has correct WGET
			File mydir = getDir("bin", Context.MODE_PRIVATE); //Creating an internal dir;
			for(String asset: assets) {
				Log.i(TAG, "TRYING TO WRITE " + asset);
				InputStream inputStream = getResources().getAssets().open(asset);

				File fileWithinMyDir = new File(mydir, asset); //Getting a file within the dir.
				FileOutputStream fileOutputStream = new FileOutputStream(fileWithinMyDir); //Use the stream as
				byte[] buffer = new byte[1024];
				int read;
				while((read = inputStream.read(buffer)) != -1){
					fileOutputStream.write(buffer, 0, read);
				}
				fileOutputStream.close();
				inputStream.close();
			}
		} catch(IOException e) {
			Log.i(TAG, e.getMessage());
			e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.

		}




	}

	private void doStartDaemon() {

		new Thread(new Runnable() {
			@Override
			public void run() {

				Runtime runtime = Runtime.getRuntime();
				Process proc = null;
				OutputStreamWriter osw = null;
				try {
					proc = runtime.exec("su");
					osw = new OutputStreamWriter(proc.getOutputStream());
					osw.write("export debian_dir="+debianDir+"\n");
					String cmd = "sh " + debianDir + PATH_SCRIPTS + "/startup-outside-chroot.sh\n";
					Log.i(TAG, cmd);
					osw.write(cmd);
					osw.flush();
					osw.close();
				} catch(IOException ex) {
					Log.e(TAG, "Failed executing deploy_debian_debootstrap.sh " + ex.getMessage());
				}	finally {
					if(osw != null) {
						try {
							osw.close();
						} catch(IOException e) {
						}
						synchronized(runLock) {
							runOnUiThread(new Runnable() {
								@Override
								public void run() {
									toggleItem.setIcon(R.drawable.owl_icon);
								}
							});

							showNotification();
							isRunning = true;
						}
					}
				}


				final BufferedReader stdout = new BufferedReader(new InputStreamReader(proc.getInputStream()));
				//monitor stdout and update textview
				new Thread(new Runnable() {
					@Override
					public void run() {
						Log.i(TAG, "START Monitoring STDOUT");
						try {
							String line;
							while((line = stdout.readLine()) != null){
									final String str = line+"\n";
									runOnUiThread(new Runnable() {
										@Override
										public void run() {
											appendToTextView(str);
										}
									});
							}
						} catch(IOException e) {
						}
						Log.i(TAG, "STOP Monitoring STDOUT");
					}
				}).start();

				try {
					proc.waitFor();
				} catch(InterruptedException e) {

				}
			}


		}).start();
	}

	private void doStopDaemon() {
		new Thread(new Runnable() {
			@Override
			public void run() {

				Runtime runtime = Runtime.getRuntime();
				Process proc = null;
				OutputStreamWriter osw = null;
				try {
					proc = runtime.exec("su");
					osw = new OutputStreamWriter(proc.getOutputStream());
					osw.write("export debian_dir="+debianDir+"\n");
					String cmd = "sh "+debianDir+PATH_SCRIPTS+"/shutdown-outside-chroot.sh\n";
					Log.i(TAG, cmd);
					osw.write(cmd);
					osw.flush();
					osw.close();
				} catch(IOException ex) {
					Log.e(TAG, "Failed executing shutdown-outside-chroot.sh " + ex.getMessage());
				}	finally {
					if(osw != null) {
						try {
							osw.close();
						} catch(IOException e) {
						}
						synchronized(runLock) {

							runOnUiThread(new Runnable() {
								@Override
								public void run() {
									toggleItem.setIcon(R.drawable.owl_negative);
								}
							});
							cancelNotification();
							isRunning = false;
						}
					}
				}


				final BufferedReader stdout = new BufferedReader(new InputStreamReader(proc.getInputStream()));
				//monitor stdout and update textview
				new Thread(new Runnable() {
					@Override
					public void run() {
						Log.i(TAG, "START Monitoring STDOUT");
						try {
							String line;
							while((line = stdout.readLine()) != null){
								final String str = line+"\n";
								runOnUiThread(new Runnable() {
									@Override
									public void run() {
										appendToTextView(str);
									}
								});
							}
						} catch(IOException e) {
						}
						Log.i(TAG, "STOP Monitoring STDOUT");
					}
				}).start();

				try {
					proc.waitFor();
				} catch(InterruptedException e) {

				}
			}
		}).start();
	}

	private void doRemove() {
		String[] cmd = {"rm -fr "+debianDir+"\n", "echo 'Debian removed...'\n"};
		shellExecute(cmd, true, true);
	}

	private void doDebootstrap() {

		String[] cmd = {
			"export debian_dir="+debianDir+"\n",
			"cd /data/data/de.tubs.ibr.distcc/app_bin/\n",
			"chmod 0777 busybox\n",
			"/data/data/berserker.android.apps.sshdroid/home/bin/sed 1d deploy-equipped-debian.sh > dstrap.sh\n",
			"sh dstrap.sh\n"
		};
		shellExecute(cmd, true, true);
	}

	private void shellExecute(final String[] cmd, final boolean monitorSTDOUT, final boolean monitorSTDERR) {
		new Thread(new Runnable() {
			@Override
			public void run() {
				Runtime runtime = Runtime.getRuntime();
				Process proc = null;
				OutputStreamWriter osw = null;
				try {
					proc = runtime.exec("su");
					osw = new OutputStreamWriter(proc.getOutputStream());
					//osw.write("chmod +x /data/data/de.tubs.ibr.distcc/app_bin/busybox");
					for(String s: cmd) {
						Log.i(TAG, s);
						osw.write(s);
					}
					osw.flush();
					osw.close();
				} catch(IOException ex) {
					Log.e(TAG, "Failed executing " + ex.getMessage());
				}	finally {
					if(osw != null) {
						try {
							osw.close();
						} catch(IOException e) {
						}
					}
				}



					final BufferedReader stdout = new BufferedReader(new InputStreamReader(proc.getInputStream()));
					//monitor stdout and update textview
					new Thread(new Runnable() {
						@Override
						public void run() {
							Log.i(TAG, "START Monitoring STDOUT");
							try {
								String line;
								while((line = stdout.readLine()) != null){
										final String str = line+"\n";
										runOnUiThread(new Runnable() {
											@Override
											public void run() {
												if(monitorSTDOUT) {
													appendToTextView(str);
												}
											}
										});
								}
							} catch(IOException e) {
							}
							Log.i(TAG, "STOP Monitoring STDOUT");
						}
					}).start();


					final BufferedReader stderr = new BufferedReader(new InputStreamReader(proc.getErrorStream()));
					//monitor stdout and update textview
					new Thread(new Runnable() {
						@Override
						public void run() {
							Log.i(TAG, "START Monitoring STDERR");
							try {
								String line;
								while((line = stderr.readLine()) != null){
										final String str = line+"\n";
										runOnUiThread(new Runnable() {
											@Override
											public void run() {
												if(monitorSTDERR) {
													appendToTextView(str);
												}
											}
										});
								}
							} catch(IOException e) {
							}
							Log.i(TAG, "STOP Monitoring STDERR");
						}
					}).start();



				try {
					proc.waitFor();
				} catch(InterruptedException e) {

				}

			}
		}).start();
	}

	private void appendToTextView(String str) {
		final TextView textView = (TextView) findViewById(R.id.text);
		textView.append(str);
		/*
		final ScrollView scrollView = (ScrollView) findViewById(R.id.scroll);

		scrollView.fullScroll(ScrollView.FOCUS_DOWN);
		scrollView.scrollBy(0, textView.getLineHeight());
		*/
	}

	private void showNotification() {
		final Intent intent = new Intent(this, MainActivity.class);
		intent.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT | Intent.FLAG_ACTIVITY_SINGLE_TOP);
		final PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);

		final NotificationManager mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
		Notification.Builder notificationBuilder = new Notification.Builder(this);
		notificationBuilder.setSmallIcon(R.drawable.owl_icon);
		notificationBuilder.setOngoing(true);
		notificationBuilder.setContentIntent(pendingIntent);
		notificationBuilder.setContentText(getString(R.string.app_desc));
		//		notificationBuilder.setSubText("SBTEXT HERE");
		notificationBuilder.setContentTitle(getString(R.string.app_name));
		notificationBuilder.setTicker(getString(R.string.app_name));
		notificationBuilder.setWhen(System.currentTimeMillis());
		Notification notification = notificationBuilder.build();
		mNotificationManager.notify(NOTIFICATION_ID_ONPAUSE, notification);
	}

	private void cancelNotification() {
		final NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
		notificationManager.cancel(NOTIFICATION_ID_ONPAUSE);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		final MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.main_activity, menu);

		toggleItem = menu.findItem(R.id.menu_toggle);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem menuItem) {
		switch(menuItem.getItemId()) {
			case R.id.menu_exit:
				finish();
				break;

			case R.id.menu_options:
				final Intent vMenu = new Intent(this, PreferencesActivity.class);
				startActivity(vMenu);
				break;

			case R.id.menu_install:
				new AlertDialog.Builder(this)
					.setMessage("Are you sure you want to continue?")
					.setCancelable(false)
					.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
						@Override
						public void onClick(DialogInterface dialogInterface, int i) {
							doDebootstrap();
						}
					}).setNegativeButton("No", null).show();
				break;

			case R.id.menu_remove:
				new AlertDialog.Builder(this)
						  .setMessage("Are you sure you want to continue?")
						  .setCancelable(false)
						  .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
							  @Override
							  public void onClick(DialogInterface dialogInterface, int i) {
								  doStopDaemon();
								  doRemove();
							  }
						  }).setNegativeButton("No", null).show();
				break;

			case R.id.menu_forcestop:
				doStopDaemon();
				break;

			case R.id.menu_toggle:
				if(isRunning) {
					doStopDaemon();
				} else {
					doStartDaemon();
				}
				break;

		}
		return true;
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		doStopDaemon();
	}

	@Override
	public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String s) {
		debianDir = MOUNT_POINT + sharedPreferences.getString(PreferencesActivity.KEY_TARGET, getString(R.string.pref_targetDebian_d));
		Log.i(TAG, "debianDir="+debianDir);
	}
}
