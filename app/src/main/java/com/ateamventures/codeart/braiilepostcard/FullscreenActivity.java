package com.ateamventures.codeart.braiilepostcard;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.graphics.Typeface;
import android.os.AsyncTask;
import android.os.Environment;
import android.os.IBinder;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.os.Handler;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import com.dd.CircularProgressButton;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.lang.ref.WeakReference;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Set;

/**
 * An example full-screen activity that shows and hides the system UI (i.e.
 * status bar and navigation/system bar) with user interaction.
 */
public class FullscreenActivity extends AppCompatActivity {

    public static final String braillRequestUrl = "http://192.168.1.37:8080/api/json";
    private View mDecorView;
    private EditText mPlainText;
    private EditText mBraille;
    private CircularProgressButton mSendButton;
    private CircularProgressButton mDownButton;
    static final String TAG = "MW";
    private CircularProgressButton mPrintButton;
    private String mGcodeUrl;


    public interface BrailleRequestCallBack {
        void convertComplete();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
//        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
//                WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setContentView(R.layout.activity_fullscreen);

        mDecorView = getWindow().getDecorView();
        mDecorView.setSystemUiVisibility(
                        View.SYSTEM_UI_FLAG_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);

        mBraille = (EditText) findViewById(R.id.Braille);
        mPlainText = (EditText) findViewById(R.id.PlainText);
        mSendButton = (CircularProgressButton) findViewById(R.id.sendButton);
        mSendButton.setIndeterminateProgressMode(true);

        mDownButton = (CircularProgressButton) findViewById(R.id.downloadButton);
        mDownButton.setIndeterminateProgressMode(true);

        mPrintButton = (CircularProgressButton) findViewById(R.id.printButton);
        mPrintButton.setIndeterminateProgressMode(true);

        Typeface type = Typeface.createFromAsset(getAssets(),"fonts/Sheets_Braille.ttf");
        mBraille.setTypeface(type);

        mPlainText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                mBraille.setText(mPlainText.getText());
            }

            @Override
            public void afterTextChanged(Editable editable) {

            }
        });

        mSendButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                BrailleRequestCallBack cb = new BrailleRequestCallBack() {
                    @Override
                    public void convertComplete() {
                        mSendButton.setProgress(100);
                    }
                };

                BrailleRequest brailleRequest = new BrailleRequest();
                brailleRequest.registerCallBack(cb);

                brailleRequest.sendRequsest(braillRequestUrl, mPlainText.toString());

                mGcodeUrl = brailleRequest.getGcodeUrl();

                if (mSendButton.getProgress() == 0) {
                    mSendButton.setProgress(50);
                } else if (mSendButton.getProgress() == -1) {
                    mSendButton.setProgress(0);
                } else {
                    mSendButton.setProgress(-1);
                }
            }
        });


        mDownButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Log.d(TAG, "onClick: " + mGcodeUrl);
                mDownButton.setProgress(0);
                if (mGcodeUrl==null)
                    mGcodeUrl="http://192.168.1.37:8080/braillePostCard.gcode";
                new GcodeDownload().execute(mGcodeUrl);
            }
        });

        mPrintButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                usbService.write("M105\n".getBytes());
                usbService.write("M105\n".getBytes());

                gcodeReader.startGCodeReadThread();
                mPrintButton.setProgress(100);
            }
        });

        mHandler = new MyHandler(this);
    }

    @Override
    protected void onResume() {
        super.onResume();
        setFilters();  // Start listening notifications from UsbService
        startService(UsbService.class, usbConnection, null); // Start UsbService(if it was not started before) and Bind it

    }

    @Override
    protected void onPause() {
        super.onPause();
        unregisterReceiver(mUsbReceiver);
        unbindService(usbConnection);
    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        // Trigger the initial hide() shortly after the activity has been
        // created, to briefly hint to the user that UI controls
        // are available.
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Intent intent = new Intent(
                getApplicationContext(),//현재제어권자
                UsbService.class); //

        stopService(intent);
    }

    //<-------------------------------------------------------------------------------------------

    /*
     * Notifications from UsbService will be received here.
     */
    private final BroadcastReceiver mUsbReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            switch (intent.getAction()) {
                case UsbService.ACTION_USB_PERMISSION_GRANTED: // USB PERMISSION GRANTED
                    Toast.makeText(context, "USB Ready", Toast.LENGTH_SHORT).show();
                    break;
                case UsbService.ACTION_USB_PERMISSION_NOT_GRANTED: // USB PERMISSION NOT GRANTED
                    Toast.makeText(context, "USB Permission not granted", Toast.LENGTH_SHORT).show();
                    break;
                case UsbService.ACTION_NO_USB: // NO USB CONNECTED
                    Toast.makeText(context, "No USB connected", Toast.LENGTH_SHORT).show();
                    break;
                case UsbService.ACTION_USB_DISCONNECTED: // USB DISCONNECTED
                    Toast.makeText(context, "USB disconnected", Toast.LENGTH_SHORT).show();
                    break;
                case UsbService.ACTION_USB_NOT_SUPPORTED: // USB NOT SUPPORTED
                    Toast.makeText(context, "USB device not supported", Toast.LENGTH_SHORT).show();
                    break;
            }
        }
    };

    private static UsbService usbService;
    private MyHandler mHandler;

    private static GCodeReader gcodeReader = new GCodeReader("Download/braillePostCard.gcode");

    private final ServiceConnection usbConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName arg0, IBinder arg1) {
            usbService = ((UsbService.UsbBinder) arg1).getService();
            usbService.setHandler(mHandler);
        }

        @Override
        public void onServiceDisconnected(ComponentName arg0) {
            usbService = null;
        }
    };

    /*
     * This handler will be passed to UsbService. Data received from serial port is displayed through this handler
     */
    private static class MyHandler extends Handler {

        private final WeakReference<FullscreenActivity> mActivity;
        private String temp = "";

        public MyHandler(FullscreenActivity activity) {
            mActivity = new WeakReference<>(activity);
        }

        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case UsbService.MESSAGE_FROM_SERIAL_PORT:
                    String data = (String) msg.obj;
                    Log.d(TAG, "handleMessage: " + data);
                    break;

                case UsbService.CTS_CHANGE:
                    Toast.makeText(mActivity.get(), "CTS_CHANGE",Toast.LENGTH_LONG).show();
                    break;

                case UsbService.DSR_CHANGE:
                    Toast.makeText(mActivity.get(), "DSR_CHANGE",Toast.LENGTH_LONG).show();
                    break;

                case UsbService.SYNC_READ:
                    String buffer = (String) msg.obj;

                    temp += buffer;

                    if (temp.charAt(temp.length()-1) != '\n') {
                        Log.d(TAG, "handleMessage: temp " + temp);
                        break;
                    }

                    if(temp.contains("ok")) {

                        try {
                            Thread.sleep(1);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }

                        gcodeReader.setOKfromPrinter(true);

                        String gcode = gcodeReader.getNextGcode();
                        if(gcode != null) {
                            Log.d(TAG, "Response: " + temp);
                            Log.d(TAG, gcode);

                            usbService.write(gcode.getBytes());
                        }

                        temp = "";
                    }
                    break;
            }
        }
    }

    private void startService(Class<?> service, ServiceConnection serviceConnection, Bundle extras) {
        if (!UsbService.SERVICE_CONNECTED) {
            Intent startService = new Intent(this, service);
            if (extras != null && !extras.isEmpty()) {
                Set<String> keys = extras.keySet();
                for (String key : keys) {
                    String extra = extras.getString(key);
                    startService.putExtra(key, extra);
                }
            }
            startService(startService);
        }
        Intent bindingIntent = new Intent(this, service);
        bindService(bindingIntent, serviceConnection, Context.BIND_AUTO_CREATE);
    }

    private void setFilters() {
        IntentFilter filter = new IntentFilter();
        filter.addAction(UsbService.ACTION_USB_PERMISSION_GRANTED);
        filter.addAction(UsbService.ACTION_NO_USB);
        filter.addAction(UsbService.ACTION_USB_DISCONNECTED);
        filter.addAction(UsbService.ACTION_USB_NOT_SUPPORTED);
        filter.addAction(UsbService.ACTION_USB_PERMISSION_NOT_GRANTED);
        registerReceiver(mUsbReceiver, filter);
    }

    private class GcodeDownload extends AsyncTask<String, Void, Void> {
        private String fileName;
        File sdcard = Environment.getExternalStorageDirectory();
        private final String SAVE_FOLDER = "/Download";

        @Override
        protected Void doInBackground(String... params)  {
            String savePath = Environment.getExternalStorageDirectory().toString() + SAVE_FOLDER;

            fileName = "braillePostCard.gcode";
            String fileUrl = params[0];

            Log.d(TAG, "doInBackground: " + fileUrl);

            String localPath = savePath + "/" + fileName;
            File oldFile = new File(localPath);
            if (oldFile.exists() == true) {
                oldFile.delete();
            }

            try {
                URL imgUrl = new URL(fileUrl);
                HttpURLConnection conn = (HttpURLConnection)imgUrl.openConnection();
                int len = conn.getContentLength();
                byte[] tmpByte = new byte[len];

                byte[] buffer = new byte[1024];

                Log.d(TAG, "doInBackground: len " + len );

                InputStream is = conn.getInputStream();

                File file = new File(localPath);
                FileOutputStream fos = new FileOutputStream(file);

                int read;
                for (;;) {
                    read = is.read(buffer);
                    Log.d(TAG, "doInBackground: read " + read );
                    if (read <= 0) {
                        break;
                    }
                    fos.write(buffer, 0, read); //file 생성
                }
                is.close();
                fos.close();
                conn.disconnect();

            } catch (Exception e) {
                e.printStackTrace();
            }
            return null;

        }


        @Override
        protected void onPostExecute(Void result) {

            super.onPostExecute(result);
            Log.d(TAG, "onPostExecute: filedownload done ");
            mDownButton.setProgress(100);

        }

    }

    //------------------------------------------------------------------------------------------->
}
