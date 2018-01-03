package org.qtproject.qdic;

import java.io.File;

import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Toast;

public class MainActivity extends AppCompatActivity {
	
	public static final String MESSAGE = "MESSAGE";
	public static final String CUSTOM_INTENT_FILTER = "org.qtproject.qdic.ResultForMainActivity";
	public static final String INPUT_DIC_FILE = "INPUT_DIC_FILE";
	public static final String INPUT_BOOK_FILE = "INPUT_BOOK_FILE";
	public static final String OUTPUT_BOOK_NAME = "OUTPUT_BOOK_NAME";
	public static final String OUTPUT_LIST_NAME = "OUTPUT_LIST_NAME";
	public static final String GENERATE_LIST = "GENERATE_LIST";
	public static final int REQUEST_CODE_FILES = 1;
	public static final int DARK = 1;
	public static final String DIC_FILE_PATH = "DIC_FILE_PATH";
	public static final String BOOK_FILE_PATH = "BOOK_FILE_PATH";
	
	private EditText dicPathEdit;
	private EditText bookPathEdit;
	private Button stButton;
	private Button filesButton;
	private boolean bound = false;
	private Intent serviceIntent;
	private ServiceConnection sConn;
	private QDicService myService;
	private IntentFilter cif;
	private ProgressBar mProgress;
	private int mProgressStatus = 0;
	private int mtheme;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
		mtheme = Integer.parseInt(prefs.getString("pref_theme", "1"));
		if (mtheme == DARK)
			setTheme(R.style.AppBaseTheme);
		else
			setTheme(R.style.AppBaseLightTheme);
		super.onCreate(savedInstanceState);
		serviceIntent = new Intent(this, QDicService.class);
		cif = new IntentFilter(CUSTOM_INTENT_FILTER);
		setContentView(R.layout.activity_main);
		Toolbar myToolbar = (Toolbar) findViewById(R.id.ma_toolbar);
		setSupportActionBar(myToolbar);
		mProgress = (ProgressBar) findViewById(R.id.progress_bar);
		dicPathEdit = (EditText) findViewById(R.id.dic_path);
		bookPathEdit = (EditText) findViewById(R.id.book_path);
		stButton = (Button) findViewById(R.id.start_btn);
		filesButton = (Button) findViewById(R.id.file_button);
		startService(serviceIntent);
		sConn = new ServiceConnection() {
        	@Override
        	public void onServiceConnected(ComponentName className,
            		IBinder service) {
        		myService = ((QDicService.MyBinder) service).getService();
            	bound = true;
        		if (myService.isRunning()) {
        			setDisabledViews();
        			mProgressStatus = myService.getProgress();	
        		} else {
        			setEnabledViews();
        			mProgressStatus = 0;
        		}
        		mProgress.setProgress(mProgressStatus);
            }
        	@Override
            public void onServiceDisconnected(ComponentName name) {
            	bound = false;
            }
        };
	}
	
	private BroadcastReceiver mMessageReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			int x = intent.getExtras().getInt(MESSAGE);
			if (x == -1) {
				setEnabledViews();
				mProgressStatus = 0;
				mProgress.setProgress(mProgressStatus);
			} else {
				mProgressStatus = x;
				mProgress.setProgress(mProgressStatus);
			}
		}
	};
	
	@Override
	protected void onStart() {
		super.onStart();
		bindService(serviceIntent, sConn, 0);
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
		if (mtheme != Integer.parseInt(prefs.getString("pref_theme", "1"))) {
			finish();
			startActivity(getIntent());
		}
	}
	
	@Override
	protected void onRestart() {
		super.onRestart();
	}
	
	@Override
	protected void onPause() {
		super.onPause();
		LocalBroadcastManager.getInstance(this).unregisterReceiver(mMessageReceiver);
	}
	
	@Override
	protected void onResume() {
		super.onResume();
		LocalBroadcastManager.getInstance(this).registerReceiver(mMessageReceiver, cif);
	}
	
	@Override
	protected void onStop() {
		super.onStop();
		//Log.d(LOG_TAG, "onStop");
        if (bound) {
        	unbindService(sConn);
        	bound = false;
        }
	}
	
	@Override
	protected void onDestroy() {
		super.onDestroy();
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// Handle action bar item clicks here. The action bar will
		// automatically handle clicks on the Home/Up button, so long
		// as you specify a parent activity in AndroidManifest.xml.
		int id = item.getItemId();
		if (id == R.id.action_settings) {
			startActivity(new Intent(this, SettingsActivity.class));
			return true;
		}
		return super.onOptionsItemSelected(item);
	}
	
	public void onclickStart(View v) {
		if (bound && myService.isRunning()) {
			myService.stop();
			return;
		}
		if (dicPathEdit.getText().toString().isEmpty()
				|| bookPathEdit.getText().toString().isEmpty()) {
			return;
		}
		final EditText input = new EditText(this);
		final String name = new File(bookPathEdit.getText().toString()).getName();
		input.setHint(R.string.output_filename_hint);
		input.setText("NEW_" + name);
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setTitle(R.string.output_filename);
		builder.setView(input);
		builder.setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
			@Override
		    public void onClick(DialogInterface dialog, int item) {
				String value = input.getText().toString().trim();
				if (value != null && !value.isEmpty() && !value.equals(name)) {
					if (Utils.cantainsIllegalChars(value)) {
						Toast.makeText(MainActivity.this, getText(R.string.out_name_chars).toString() + " * \\ / \" : ? | < >", Toast.LENGTH_LONG).show();
					} else {
						startTask(value);
					}
				}
		    }
		});
		builder.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
			@Override
		    public void onClick(DialogInterface dialog, int item) {
				;
		    }
		});
		AlertDialog alert = builder.create();
		alert.show();
	}
	
	private void startTask(String str) {
		Intent taskIntent = new Intent(this, QDicService.class);
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
		String d = dicPathEdit.getText().toString();
		String b = bookPathEdit.getText().toString();
		boolean gel_list = prefs.getBoolean("pref_gen_list", false);
		String outl = prefs.getString("pref_listTemplate", "list.txt");
		String outb = str;
		taskIntent.putExtra(INPUT_DIC_FILE, d);
		taskIntent.putExtra(INPUT_BOOK_FILE, b);
		taskIntent.putExtra(OUTPUT_LIST_NAME, outl);
		taskIntent.putExtra(OUTPUT_BOOK_NAME, outb);
		taskIntent.putExtra(GENERATE_LIST, gel_list);
		if (bound && !myService.isRunning()) {
			mProgress.setProgress(0);
			setDisabledViews();
			myService.runTask(taskIntent);
		}
	}
	
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		//Log.d(LOG_TAG, "onResult");
		if (resultCode == RESULT_OK) {
			switch (requestCode) {
			case REQUEST_CODE_FILES:
				String dic_path = data.getStringExtra(DIC_FILE_PATH);
				String book_path = data.getStringExtra(BOOK_FILE_PATH);
				if (!dic_path.isEmpty())
					dicPathEdit.setText(dic_path);
				if (!book_path.isEmpty())
					bookPathEdit.setText(book_path);
				break;
			}
		} else {
			Toast.makeText(this, "Wrong result from FilesActivity", Toast.LENGTH_SHORT).show();
		}
	}
	
	public void onclickFiles(View v) {
		Intent filesIntent = new Intent(this, FilesActivity.class);
		startActivityForResult(filesIntent, REQUEST_CODE_FILES);;
	}
	
	private void setDisabledViews() {
		stButton.setText(R.string.stopBtn);
		filesButton.setVisibility(View.INVISIBLE);
		mProgress.setVisibility(View.VISIBLE);
		findViewById(R.id.dic_path).setEnabled(false);
		findViewById(R.id.book_path).setEnabled(false);
	}
	
	private void setEnabledViews() {
		stButton.setText(R.string.startBtn);
		mProgress.setVisibility(View.INVISIBLE);
		filesButton.setVisibility(View.VISIBLE);
		findViewById(R.id.dic_path).setEnabled(true);
		findViewById(R.id.book_path).setEnabled(true);
	}
}
