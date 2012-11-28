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


		//move all the assets to certain directory
		try {
			String[] assets = { "busybox", "deploy_debian_bootstrap.sh", "deb.sh" };

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
		final TextView textView = (TextView) findViewById(R.id.text);
		textView.setMovementMethod(new ScrollingMovementMethod());
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
					osw.write("sh /data/data/de.tubs.ibr.distcc/app_bin/deploy_debian_bootstrap.sh");
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
											textView.append(str);
											//this should to the auto scroll trick!
											if(textView.getLineCount()*textView.getLineHeight() > textView.getHeight()) {
												textView.scrollBy(0, textView.getLineHeight());
											}
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
}
