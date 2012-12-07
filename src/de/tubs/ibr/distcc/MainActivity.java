package de.tubs.ibr.distcc;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.res.AssetManager;
import android.os.Bundle;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ScrollView;
import android.widget.TextView;

import java.io.*;
import java.util.concurrent.ExecutorService;

public class MainActivity extends Activity {

	private static final String TAG = MainActivity.class.getName();
	/**
	 * Called when the activity is first created.
	 */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);

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
		try {
			//String[] assets = { "busybox", "deploy_debian_bootstrap.sh", "build-cross-cc.sh" };
			String[] assets = { "busybox", "deploy_debian_bootstrap.sh"}; //only this busybox has correct WGET
			File mydir = getDir("bin", Context.MODE_PRIVATE); //Creating an internal dir;
			for(String asset: assets) {
				Log.i(TAG, "TRYING TO WRITE " + asset);
				InputStream inputStream = getResources().getAssets().open(asset);

				File fileWithinMyDir = new File(mydir, asset); //Getting a file within the dir.
				FileOutputStream fileOutputStream = new FileOutputStream(fileWithinMyDir); //Use the stream as
				//FileOutputStream fileOutputStream = new FileOutputStream(new File("/storage/sdcard0", "busybox"));
				byte[] buffer = new byte[1024];
				int read;
				while((read = inputStream.read(buffer)) != -1){
					fileOutputStream.write(buffer, 0, read);
				}
				fileOutputStream.close();
				inputStream.close();
			}

			final Button button = (Button) findViewById(R.id.button_debootstrap);
			button.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View view) {
					button.setActivated(false);
					button.setClickable(false);
					doDebootstrap();
				}
			});

		} catch(IOException e) {
			Log.i(TAG, e.getMessage());
			e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.

		}




	}

	private void doDebootstrap() {
		//execute sh deploy_debian_bootstrap.sh in new thread
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
					osw.write("cd /data/data/de.tubs.ibr.distcc/app_bin/\n");
					osw.write("chmod 0777 busybox\n");
					//osw.write("/data/data/berserker.android.apps.sshdroid/home/bin/wget https://raw.github.com/debidroidcc/debidroidcc/master/deploy_debian_bootstrap.sh -O tmpsh --no-check-certificate\n");
					osw.write("/data/data/berserker.android.apps.sshdroid/home/bin/sed 1d deploy_debian_bootstrap.sh > dstrap.sh\n"); //wget prepends three dots ... for some odd reason
					osw.write("sh dstrap.sh\n");
					osw.flush();
					osw.close();
				} catch(IOException ex) {
					Log.e(TAG, "Failed executing deploy_debian_debootstrap.sh " + ex.getMessage());
				}	finally {
					if(osw != null) {
						try {
							osw.close();
						}
						catch(IOException e) {

						}
					}
				}

				final InputStream stdout = proc.getInputStream();
				//monitor stdout and update textview
				new Thread(new Runnable() {
					@Override
					public void run() {
						Log.i(TAG, "THREAD STARTED ");
						try {
							int BUFF_LEN = 4096;
							byte[] buffer = new byte[BUFF_LEN];
							int read;
							while(true){
								read = stdout.read(buffer);
								if(read > 0) {
									final String str = new String(buffer, 0, read);
									runOnUiThread(new Runnable() {
										@Override
										public void run() {
											appendToTextView(str);
										}
									});
								}
							}
						} catch(IOException e) {
						}
					}
				}).start();

				final InputStream stderr = proc.getErrorStream();
				//monitor stdout and update textview
				new Thread(new Runnable() {
					@Override
					public void run() {
						Log.i(TAG, "THREAD STARTED ");
						try {
							int BUFF_LEN = 4096;
							byte[] buffer = new byte[BUFF_LEN];
							int read;
							while(true){
								read = stderr.read(buffer);
								if(read > 0) {
									final String str = new String(buffer, 0, read);
									runOnUiThread(new Runnable() {
										@Override
										public void run() {
											appendToTextView(str);
										}
									});
								}
							}
						} catch(IOException e) {
						}
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
		final ScrollView scrollView = (ScrollView) findViewById(R.id.scroll);

		scrollView.fullScroll(ScrollView.FOCUS_DOWN);
		scrollView.scrollBy(0, textView.getLineHeight());
	}
}
