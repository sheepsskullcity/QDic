package org.qtproject.qdic;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.UUID;

import android.annotation.TargetApi;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.support.v4.app.NotificationCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.widget.Toast;
import android.os.Binder;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;

public class QDicService extends Service {
	
	private final int ONGOING_NOTIFICATION_ID = 1;
	private final int NO_ERRORS = 0;
	private final int DIC_ERROR = -1;
	private final int BOOK_ERROR = -2;
	private final int CLEAR_ERROR = -3;
	private final String dicExceptionMsg = "dicExceptionMsg";
	private final String bookExceptionMsg = "bookExceptionMsg";
	
	private long startTime;
	private long endTime;
	private long totalTime;
	private QDicService service = this;
	private MyBinder mBinder = new MyBinder();
	private boolean cancel;
	private boolean isRunning = false;
	private Intent mintent;
	private int progressStatus = 0;
	private Thread t;
	private Handler handler = new Handler();
	private int numOfDics;
	private int currentDic;
	private Notification notification;

	
	static {
	    try {
	        System.loadLibrary("gnustl_shared");
	        System.loadLibrary("Qt5Core");
	    } 
	    catch( UnsatisfiedLinkError e ) {
	        System.err.println("Native code library failed to load.\n" + e);
	    }
	    System.loadLibrary("QDic");
	}

	public native int setEncoding(String denc, String benc);
    public native int initDic(String d);
    public native int initRexDic(String d);
    public native int startTask(String b, String e, String l,boolean fb2, boolean makel, boolean appendl);
    public native int clearStatus();
    public native int cancel();
    public native int getRules();
    public native int getTotal();
    
    public class MyBinder extends Binder {
    	public QDicService getService() {
    		return QDicService.this;
    	}
    }
    
    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    @Override
    public void onCreate() {
    	super.onCreate();
    	mintent = new Intent(MainActivity.CUSTOM_INTENT_FILTER);
  	  	Intent notificationIntent = new Intent(this, MainActivity.class);
  	  	notificationIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
  	  		.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
  	  	PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, notificationIntent, PendingIntent.FLAG_UPDATE_CURRENT);
  	  
  	  	Bitmap bm = BitmapFactory.decodeResource(getResources(), R.drawable.ic_launcher);
  	  
  	  	NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(this)
  	  		.setSmallIcon(R.drawable.ic_notif)
  	  		.setLargeIcon(bm)
  	  		.setContentInfo(getText(R.string.ticker_text))
  	  		.setContentTitle(getText(R.string.notification_title))
  	  		.setContentText(getText(R.string.notification_message))
  	  		.setContentIntent(pendingIntent);
  	  
  	  	if (android.os.Build.VERSION.SDK_INT >= 21)
  	  		mBuilder.setCategory(Notification.CATEGORY_SERVICE);
  	  	notification = mBuilder.build();
  	}
    
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
    	return START_NOT_STICKY;
    }
    
    @Override
    public void onDestroy() {
    	super.onDestroy();
  	}
    
    @Override
    public IBinder onBind(Intent intent) {
    	return mBinder;
    }
    
    @Override
    public boolean onUnbind(Intent intent) {
    	return super.onUnbind(intent);
    }
    
    public void stop() {
    	cancel = true;
    	cancel();
    	t.interrupt();
    }
    
    public boolean isRunning() {
    	return isRunning;
    }
    
    private void sendMessage(int x) {
    	if (x >= 0) {
    		progressStatus = x;
    	} else {
    		progressStatus = 0;
    	}
    	mintent.putExtra(MainActivity.MESSAGE, x);
    	LocalBroadcastManager.getInstance(this).sendBroadcast(mintent);
    }
    
    private void updatePBar(int progress) {
    	sendMessage(progress);
    }
    
    public int getProgress() {
    	return progressStatus;
    }
    
    public void runTask(Intent intent) {
    	startForeground(ONGOING_NOTIFICATION_ID, notification);
    	startTime = System.currentTimeMillis();
    	cancel = false;
    	isRunning = true;
    	currentDic = 0;
    	final String dic_path = intent.getStringExtra(MainActivity.INPUT_DIC_FILE);
    	final String book_path = intent.getStringExtra(MainActivity.INPUT_BOOK_FILE);
    	final String list_name = intent.getStringExtra(MainActivity.OUTPUT_LIST_NAME);
    	final String output_bname = intent.getStringExtra(MainActivity.OUTPUT_BOOK_NAME);
    	final boolean gen_list = intent.getBooleanExtra(MainActivity.GENERATE_LIST, false);
    	
    	Runnable nativeRunnable = new Runnable() {
    		
    		private int size = 0;
    		private int rules = 0;
    		private int total = 0;
    		private int e_code = NO_ERRORS;
    		private boolean glist = gen_list;
    		private boolean fb2 = false;
    		private String book = book_path;
    		private String output = output_bname;
    		private String listName = list_name;
    		
    		Runnable messageFinished = new Runnable() {
    			@Override
    			public void run() {
    				finished(rules, total, e_code);
    			}
			};
			
    		Runnable messageUpdate = new Runnable() {
    			@Override
    			public void run() {
    				update(size);
    			}
			};
			
			@Override
			public void run() {
				try {
					boolean errors = false;
					DicList dlist = new DicList(dic_path);
					size = dlist.getSize();
					handler.post(messageUpdate);
					if (dlist.getSize() > 0) {
						String tempDic;
						String tempBook;
						String b = book;
						String parent = new File(book).getParent() + File.separator;
						String listPath = parent + listName;
						if (new File(book).getName().matches("^.+\\.[Ff][Bb]2$")) {
							fb2 = true;
						}
						for (int i = 0; (tempDic = dlist.getNextDic()) != null &&
										!errors &&
										!Thread.currentThread().isInterrupted(); i++) {
							if (i == 0 && new File(listPath).exists()) {
								new File(listPath).delete();
							}
							tempBook = parent + UUID.randomUUID().toString() + ".temp";
							int x = task(tempDic, b, tempBook, listPath, dlist.isRexType(tempDic), dlist.getSize() > 1 ? true : false);
							if (x != NO_ERRORS) {
								errors = true;
								e_code = x;
							} 
							currentDic++;
							if (i > 0) {
								new File(b).delete();
							}
							b = tempBook;
						}
						if (!Thread.currentThread().isInterrupted() && !errors) {
							File bookf = new File(b);
							bookf.renameTo(new File(parent + output));
						} else {
							if (new File(b).exists()) {new File(b).delete();}
							if (new File(listPath).exists()) {new File(listPath).delete();}
						}
					}
				} catch (IOException e) {
					e.printStackTrace();
				} catch (IllegalArgumentException e) {
					String msg = e.getMessage();
					if (msg != null) {
						Log.i("Qt", "ini " + msg + " error");
						e_code = DIC_ERROR;
					}
				}
		        handler.post(messageFinished);
			}
			
			public int task(String s1, String s2, String s3, String s4, boolean isRex, boolean append_list) throws IOException {
		    	int e;
		    	int errorCode = NO_ERRORS;
		        CharsetDetector cd = new CharsetDetector();
		        Charset	charset1, charset2;
		        try {
		        	charset1 = cd.getCharset(new File(s1));
		        } catch (IOException er) {
		        	e_code = DIC_ERROR;
		        	throw new IOException(er);
		        }
		        try {
		        	charset2 = cd.getCharset(new File(s2));
		        } catch (IOException er) {
		        	e_code = BOOK_ERROR;
		        	throw new IOException(er);
		        }
		    	setEncoding(charset1.name(), charset2.name());
		    	if (!Thread.currentThread().isInterrupted()) {
		    		if(!isRex)
		    			e = initDic(s1);
		    		else
		    			e = initRexDic(s1);
			    	if (e < 0) {
			    		Log.i("Qt", "dic initialization error");
			    		errorCode = DIC_ERROR;
			    	}
		        }
		    	if (!Thread.currentThread().isInterrupted()) {
		    		e = startTask(s2, s3, s4, fb2, glist, append_list);
			    	if (e < 0) {
			    		Log.i("Qt", "book replace error");
			    		errorCode = BOOK_ERROR;
			    	}
		    	}
		        e = clearStatus();
		    	if (e < 0) {
		    		Log.i("Qt", "clear error");
		    		errorCode = CLEAR_ERROR;
		    	}
		        rules += getRules();
		        total += getTotal();
		        return errorCode;
			}
    	};
    	t = new Thread(nativeRunnable);
    	t.start();
  	}
    
    public void updateProgress(float x) {
    	float total = (float) numOfDics;
    	float curr = (float) currentDic;
    	int y;
    	y = (int) ( ( (x * (1 / total) ) + (curr / total) ) * 100.f);
    	if (y > progressStatus) {
    		progressStatus = y;
    		updatePBar(progressStatus);
    	}
    }
    
    private void finished(int rules, int total, int error_code) {
    	endTime = System.currentTimeMillis();
    	totalTime = endTime - startTime;
    	stopForeground(true);
    	String fmsg = getText(R.string.final_toast_msg).toString();
    	if (!cancel) {
    		if (error_code == NO_ERRORS) {
    			finalNotif(rules, total);
    			Toast.makeText(this, fmsg, Toast.LENGTH_LONG).show();
    			Toast.makeText(this, "Time: " + Utils.getTime(totalTime), Toast.LENGTH_LONG).show();
    		} else {
    			showError(error_code);
    		}
    	} else {
    		fmsg = getText(R.string.final_toast_cancelmsg).toString();
    		Toast.makeText(this, fmsg, Toast.LENGTH_LONG).show();
    	}
    	isRunning = false;
    	int msg = -1;
    	updatePBar(msg);
    }
    
    public void update(int x) {
    	numOfDics = x;
    }
    
    private void showError(int er) {
    	String errorMsg = (String) getText(R.string.default_error);;
    	switch(er) {
    	case DIC_ERROR:
    		errorMsg = (String) getText(R.string.dic_error);
    		break;
    	case BOOK_ERROR:
    		errorMsg = (String) getText(R.string.book_error);
    		break;
    	case CLEAR_ERROR:
    		errorMsg = (String) getText(R.string.clear_error);
    		break;
    	}
    	Toast.makeText(this, errorMsg, Toast.LENGTH_LONG).show();
    	showErrNotif(errorMsg);
      }
    
    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    private void showErrNotif(String msg) {
  	  Intent notificationIntent = new Intent(this, MainActivity.class);
  	  notificationIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
  	  	.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
  	  
  	  PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, notificationIntent, 0);
  	  
  	  Bitmap bm = BitmapFactory.decodeResource(getResources(), R.drawable.ic_launcher);
  	  
  	  NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(this)
  	    .setSmallIcon(R.drawable.ic_notif)
  	    .setLargeIcon(bm)
  	    .setContentTitle(getText(R.string.error_notification_title))
  	    .setContentText(msg)
  	    .setDefaults(Notification.DEFAULT_LIGHTS)
  	    .setContentIntent(pendingIntent);
  	  
  	  if (android.os.Build.VERSION.SDK_INT >= 21)
  		  mBuilder.setCategory(Notification.CATEGORY_ERROR);
  	  
  	  mBuilder.setAutoCancel(true);
  	  
  	  NotificationManager mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
  	  mNotificationManager.notify(ONGOING_NOTIFICATION_ID, mBuilder.build());
    }
    
    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    private void finalNotif(int a, int b) {
  	  Intent notificationIntent = new Intent(this, MainActivity.class);
  	  notificationIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
  	  	.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
  	  PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, notificationIntent, 0);
  	  
  	  Bitmap bm = BitmapFactory.decodeResource(getResources(), R.drawable.ic_launcher);
  	  
  	  NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(this)
  	    .setSmallIcon(R.drawable.ic_notif)
  	    .setLargeIcon(bm)
  	    .setContentTitle(getText(R.string.final_notification_title))
  	    .setContentText(" " + a + " " + getText(R.string.final_notification_rulestext).toString()
  	    				+ " " + b + " " + getText(R.string.final_notification_timestext).toString())
  	    .setDefaults(Notification.DEFAULT_LIGHTS)
  	    .setContentIntent(pendingIntent);
  	  
  	  if (android.os.Build.VERSION.SDK_INT >= 21)
  		  mBuilder.setCategory(Notification.CATEGORY_STATUS);
  	  
  	  mBuilder.setAutoCancel(true);
  	  
  	  NotificationManager mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
  	  mNotificationManager.notify(ONGOING_NOTIFICATION_ID, mBuilder.build());
    }
}

