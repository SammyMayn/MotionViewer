package com.sjm.motionmonitor;

import android.Manifest;
import android.animation.ObjectAnimator;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;

import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.database.Cursor;
import android.media.AudioManager;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.os.Build;
import android.os.Bundle;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.os.Environment;
import android.util.Log;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.animation.TranslateAnimation;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import com.google.android.gms.ads.AdView;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;//SJM

import com.google.android.gms.ads.MobileAds;
import com.google.android.gms.ads.initialization.InitializationStatus;
import com.google.android.gms.ads.initialization.OnInitializationCompleteListener;
import static android.view.animation.Animation.ZORDER_TOP;


public class MainActivity extends AppCompatActivity  implements View.OnClickListener {
    private double m_tolerance = 10.5; //1.3;// default for now
    private String m_Orientation = "LANDSCAPE";
    private String m_eMail = "";
    private String m_eMailPW = "";
    private String m_ringtone = "-1";

    private int m_eMailCount = 0;
    private int m_eMailNum = 999;
    private boolean m_sendMail = true;
    private boolean looper = true;
    private boolean m_allwaysNotify = false;
    private int m_MillisecFramePeriod = 20;
    private int IMAGE_ARRAY_SIZE = 15;
    private static int ANIMATION_DURATION = 1000;
    private static int ANIMATION_BUTTON_DURATION = 3500;
    private boolean m_deleted = false;
    private boolean m_bOnce = true;
    private boolean m_bPauseBeforeEmail = true;
    private boolean m_bAlarmRang = false;
    private boolean m_bInitialWait = true;
    private boolean m_bOKToGo = false;
    private boolean m_bContinueButtonClicked = false;
    private static int EMAIL_PAUSE_DURATION = 30000;
    Ringtone ringtone;
    Button m_btnStopAlarm;
    Button m_btnReset;
    Button m_btnContinue;
    private Bitmap previousImage = null;
    private AdView mAdView;
    //static {
    //    OpenCVLoader.initDebug();
    //}

    @SuppressLint("SourceLockedOrientationActivity")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //this.requestWindowFeature(Window.FEATURE_NO_TITLE);
        //Remove notification bar
        //this.getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);

//MobileAds.initialize(this, new OnInitializationCompleteListener() {
//    @Override
//    public void onInitializationComplete(InitializationStatus initializationStatus) {
//    }
//});

        setTitle("PRO Motion Detector Viewer");
        setContentView(R.layout.activity_main);
//mAdView = findViewById(R.id.adView);
//AdRequest adRequest = new AdRequest.Builder().build();
//mAdView.loadAd(adRequest);


        Button btnStopAlarm = findViewById(R.id.btnStopAlarm);
        Button btnConnect = findViewById(R.id.btnConnect);
        Button btnReset = findViewById(R.id.btnReset);
        Button btnContinue = findViewById(R.id.btnContinue);
        //Button btnTestSend = findViewById(R.id.btnTestSend);
        TextView editTextAddress = (TextView) findViewById(R.id.txtStatus);
        editTextAddress.setBackgroundColor(Color.BLACK);
        editTextAddress.setTextColor(Color.YELLOW);
        //editTextAddress.setText("Status");
        TextView editTolerance = (TextView) findViewById(R.id.txtTolerance);
        editTolerance.setBackgroundColor(Color.BLACK);
        editTolerance.setTextColor(Color.YELLOW);
        //editTolerance.setText(".");
        btnConnect.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View view) {
                view.setKeepScreenOn(true);
                connect(view);
            }
        });


        btnStopAlarm.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                ringtone.stop();
                m_btnStopAlarm.setVisibility(View.INVISIBLE);

                //Show Reset and Continue buttons
                if (!m_allwaysNotify) {
                    m_btnReset = findViewById(R.id.btnReset);
                    m_btnReset.setVisibility(View.VISIBLE);
                    m_btnContinue = findViewById(R.id.btnContinue);
                    m_btnContinue.setVisibility(View.VISIBLE);
                }
                //final Animation animBounce1 = AnimationUtils.loadAnimation(getApplicationContext(),  R.anim.bounce);
                //m_btnReset.startAnimation(animBounce1);

            }
        });
        btnReset.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View view) {
                //Show Reset and continue button
                ResetAndContinue();

            }
        });
        btnContinue.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View view) {
                m_btnContinue.setVisibility(View.INVISIBLE);
                m_btnReset = findViewById(R.id.btnReset);
                m_btnReset.setVisibility(View.INVISIBLE);
                //SJM this flag prevents further files being written to disk
                m_bContinueButtonClicked = true;
                //test end
            }
        });

        int rot = 0;  //Force PORTRAIT mode

       if (rot==0) {
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        }


        Context context=getApplicationContext();

        //write incoming file to device storage
        File myDir = null;
        String root = Environment.getExternalStorageDirectory().toString();
        myDir = new File(root + "/motion_images");
        myDir.mkdirs();

        btnConnect.setEnabled(false);
        handlePermissions();

        //AudioManager mobilemode = (AudioManager)Context.getSystemService(Context.AUDIO_SERVICE);
        //mobilemode.setStreamVolume(AudioManager.STREAM_RING,mobilemode.getStreamMaxVolume(AudioManager.STREAM_RING),0);
        //AudioManager mgr = (AudioManager)context.getSystemService(Context.AUDIO_SERVICE);
        //mgr.setStreamVolume(AudioManager.STREAM_RING,mgr.getStreamMaxVolume(AudioManager.STREAM_RING),0);
        //mgr.setStreamMute(AudioManager.STREAM_SYSTEM, true);
    }
    public void handlePermissions() {
        askPermission();
    }
    public void askPermission()
    {
        int PERMISSION_ALL = 1;
        String[] PERMISSIONS = {Manifest.permission.WRITE_EXTERNAL_STORAGE}; // List of permissions required

        for (String permission : PERMISSIONS) {
            if (ActivityCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(PERMISSIONS, PERMISSION_ALL);
                return;
            }
        }
        Button btnConnect = findViewById(R.id.btnConnect);
        btnConnect.setEnabled(true);
    }
    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions,
                                           int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        final Button btnConnect = findViewById(R.id.btnConnect);
        try {
            switch (requestCode) {
                case 1: {
                    if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED && grantResults[1] == PackageManager.PERMISSION_GRANTED) {

                        btnConnect.setEnabled(true);
                    } else {
                        Toast.makeText(this, "Until you grant the permission, we cannot proceed further. Restart the app and try again.", Toast.LENGTH_SHORT).show();
                        btnConnect.setEnabled(false);
                    }
                    return;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            btnConnect.setEnabled(true);
        }
    }
    private View.OnClickListener mMoveButtonClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {

            ObjectAnimator animation = ObjectAnimator.ofFloat(v, "translationX", 100f);
            animation.setDuration(ANIMATION_BUTTON_DURATION);
            animation.start();
        }

    };
    private void ResetAndContinue() {
        m_btnReset = findViewById(R.id.btnReset);
        m_btnReset.setVisibility(View.INVISIBLE);
        m_btnContinue = findViewById(R.id.btnContinue);
        m_btnContinue.setVisibility(View.INVISIBLE);

        m_bOKToGo = false;
        m_bPauseBeforeEmail = true;
        m_bInitialWait = true;
        m_bOnce = true;
        m_bAlarmRang = false;

        if (m_bInitialWait) {
            Thread tWait = new srvThreadWait();
            tWait.start();
            m_bInitialWait = false;
        }

        m_allwaysNotify = false;
        m_eMailCount = 0;
        //m_btnReset.setVisibility(View.INVISIBLE);
    }
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);

//        if (newConfig.orientation == Configuration.ORIENTATION_PORTRAIT) {
//            Log.d("Entered to change as Portrait ","PORT");
//            setContentView(R.layout.activity_main);
//        } else if (newConfig.orientation == Configuration.ORIENTATION_LANDSCAPE) {
//            Log.d("Entered to change as LandScape ","LAND");
//            //setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
//            //setContentView(R.layout.activity_main);
//        }

    }
    //private void stopCallingService() {
    //    Log.d("[DEBUG]", "stopCallingService");
    //    //Intent i1 = new  Intent("SERVICEMSGTOCLIENT");
    //    //i1.putExtra("serviceMessage","Test from Service");
    //    broadcastIntent();
    //}
    //private void broadcastStatus() {
        //Log.i("NLService", "Broadcasting status added("+nAdded+")/removed("+nRemoved+")");
        //Intent i1 = new  Intent("SERVICEMSG");
        //i1.putExtra("serviceMessage","Test from Monitor");
        //LocalBroadcastManager.getInstance(this).sendBroadcast(i1);
        //sendBroadcast(i1); //broadcast back to calling activity (not used but could be)
    //}
    public void broadcastIntent(View view){
        Log.i("MONITOR_BROADCAST: ", "Broadcasting ....");
        //Intent intent = new Intent();
        //intent.setAction("com.tutorialspoint.CUSTOM_INTENT");
        //intent.putExtra("data","Notice me senpai!");
        //sendBroadcast(intent);
        final Intent i= new Intent();
        i.putExtra("data", "Some data");
        i.setAction("com.sjm.motionmonitor");
        i.addFlags(Intent.FLAG_INCLUDE_STOPPED_PACKAGES);
        getApplicationContext().sendBroadcast(i);
    }
    private void connect (View view) {
        try {

            System.out.println(System.getProperty("file.encoding"));
            Log.d("[DEBUG]", "SERVER CONNECTING");
            Button btnCon = (Button) findViewById(R.id.btnConnect);
            btnCon.setEnabled(false);
            btnCon.setText("Listening on 8080/8081");

            //Startup threads

            Thread t1 = new srvThreadImage();
            t1.start();
            Thread t2 = new srvThreadData();
            t2.start();

            TextView txtStatus = (TextView) findViewById(R.id.txtStatus);
            txtStatus.setText("Waiting for Camera app to connect...");


        } catch (Exception e) {
            e.printStackTrace();
            //break;
        }
    }

    @Override
    public void onClick(View view) {
        int i = 0;
        i = 0;
    }

    public class srvThreadImage extends Thread {
        @SuppressWarnings("deprecation")
        public void run() {
            try {
                //Boolean bOnline = testTCPConnection();
                //if (bOnline)
                //    System.out.println("Server: Running in thread srvThreadImage.ONLINE");
                //else
                //    System.out.println("Server: Running in thread srvThreadImage.NOT ONLINE");

                double diff = 0.0;
                int iSnapCnt = 0;
                int iDiffRange = 0;
                int iDiffRangeLast = 0;
                List<Double> list = null;
                List<String> listFiles = new ArrayList<String>();
                String fullFName = "";
                File myDir = null;
                int index = 0;

                TextView txtStatus = (TextView) findViewById(R.id.txtStatus);
                txtStatus.setText("Waiting for Camera app to connect...");
                //System.out.println("Server: Running in thread srvThreadImage.");
                //Log.d("[DEBUG]", "SERVER Running in thread srvThreadImage");

                Boolean bError = false;
                Boolean looper = true;
                int counter = 0;
                list = new ArrayList<>(Arrays.asList(0.0));
                while (looper) {
                    //Log.d("[DEBUG]", "SERVER LOOPER srvThreadImage counter : " + counter);
                    counter+=1;
                    DatagramSocket ds = null;
                    try {
                        ds = new DatagramSocket(8080);
                    } catch (SocketException e) {
                        e.printStackTrace();
                    }
                    byte[] receive = new byte[65535];

                    DatagramPacket DpReceive = null;
                    DpReceive = new DatagramPacket(receive, receive.length);

                    try {
                        //Log.d("[DEBUG]", "SERVER srvThreadImage waiting for client");
                        ds.receive(DpReceive);

                        byte[] buff = DpReceive.getData();
                        if (ds != null) {
                            ds.close();
                        }

                        Log.d("[DEBUG]", "SERVER Running in thread srvThreadImage");

                        ByteArrayInputStream bais = new ByteArrayInputStream(buff);
                        final Bitmap img = BitmapFactory.decodeStream(bais);
                        Log.d("[DEBUG]", "img: " + img);

                        runThreadSetMainImage(img, false,0);

                        // Pause for a bit (in a new thread) before starting monitoring
                        if (m_bInitialWait) {
                            Thread tWait = new srvThreadWait();
                            tWait.start();
                            m_bInitialWait = false;
                        }

                        receive = new byte[65535]; //clear buffer
                        if (previousImage!=null) {
                            diff = compareImgMethod3(img,previousImage);
                            if (diff > m_tolerance) {
                                iDiffRangeLast = iDiffRange;
                                iDiffRange++;
                                //show thumbnails for each motion change
                                runThreadSetMainImage(img, true,iSnapCnt);
                                iSnapCnt++;

                                if (iSnapCnt > (IMAGE_ARRAY_SIZE  )) //reset thumbnail image count
                                    iSnapCnt = 0;

                                list.add(diff);
                               // if (iDiffRange > iDiffRangeLast) {
                               //     System.out.println("Server: iDiffRange SPIKE:" + iDiffRange + " : " + iDiffRangeLast + " : "  + diff);
                               // }

//                                if (!m_deleted) {
//                                    //Clean up (delete created file(s)
//                                    String root = Environment.getExternalStorageDirectory().toString();
//                                    myDir = new File(root + "/motion_images");
//                                    if (myDir.isDirectory()) {
//                                        for (File file : myDir.listFiles())
//                                            if (!file.isDirectory())
//                                                file.delete();
//                                    }
//                                    m_deleted = true;
//                                }
                                if (!m_deleted) {
                                    String root = Environment.getExternalStorageDirectory().toString();
                                    File file = new File(root + "/motion_images");
                                    String[] files;
                                    files = file.list();
                                    for (int i = 0; i < files.length; i++) {
                                        File myFile = new File(file, files[i]);
                                        myFile.delete();
                                    }
                                    //SJM testing 21/2021
                                    //listFiles.clear();
                                    //SJM end test

                                    m_deleted = true;
                                }
//                                if (m_sendMail && m_bOKToGo) {
//                                    //write incoming file to device storage (if continue button has not been hit)
//                                    String root = Environment.getExternalStorageDirectory().toString();
//                                    myDir = new File(root + "/motion_images");
//                                    myDir.mkdirs();
//                                    Date date = new Date();
//                                    //This method returns the time in millis
//                                    long timeMilli = date.getTime();
//                                    String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
//                                    String fname = "Snap_" + timeMilli + ".jpg";
//                                    fullFName = root + "/motion_images/" + fname;
//                                    File file = new File(myDir, fname);
//                                    if (file.exists()) file.delete();
//                                    try {
//
//                                        FileOutputStream out = new FileOutputStream(file);
//                                        out.flush();
//                                        out.close();
//
//                                        img.compress(Bitmap.CompressFormat.JPEG, 100, out);
//
//                                        //delete the file if continue button clicked otherwise files just keep getting written
//                                        if (m_bContinueButtonClicked) {
//                                            file.delete();
//                                        }
//                                    } catch (Exception e) {
//                                        e.printStackTrace();
//                                    }
//                                    listFiles.add(fullFName);
//                                }
//                                else {
//                                    listFiles.clear();
//                                }
                                if (m_sendMail && m_bOKToGo) {
                                    //write incoming file to device storage
                                    String root = Environment.getExternalStorageDirectory().toString();
                                    myDir = new File(root + "/motion_images");
                                    myDir.mkdirs();
                                    Date date = new Date();
                                    //This method returns the time in millis
                                    long timeMilli = date.getTime();
                                    String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
                                    String fname = "Snap_" + timeMilli + ".jpg";
                                    fullFName = root + "/motion_images/" + fname;
                                    File file = new File(myDir, fname);
                                    if (file.exists()) file.delete();
                                    try {
                                        FileOutputStream out = new FileOutputStream(file);
                                        img.compress(Bitmap.CompressFormat.JPEG, 100, out);
                                        out.flush();
                                        out.close();
                                        //delete the file if continue button clicked otherwise files just keep getting written
                                        if (m_bContinueButtonClicked) {
                                            file.delete();
                                        }
                                    } catch (Exception e) {
                                        e.printStackTrace();
                                    }
                                    listFiles.add(fullFName);
                                }
                                else {
                                    listFiles.clear();
                                }
//
////                                // Pause for a bit before starting monitoring
//                                if (m_bInitialWait) {
//                                    Thread tWait = new srvThreadWait();
//                                    tWait.start();
//                                    m_bInitialWait = false;
//                                }

//                                        Thread.sleep(1000);
//                                    }
//                                    m_bPauseBeforeEmail = false;
//                                }
                                // Setup email
                                //if (m_bOnce && m_bOKToGo ) {
                                if (m_bOnce && m_bOKToGo && m_sendMail ) {
                                    Log.d("[DEBUG]", "LOOPER IMAGETHREAD " + m_eMailNum);
                                    m_eMailCount += 1;
                                    if (m_eMailCount == m_eMailNum) { //send email once limit is reached.

                                        // Play the selected ringtone until cancelled by user
                                        if ( !m_bAlarmRang) {
                                            m_bAlarmRang = true;
                                            Thread tAlarm = new srvThreadAlarm();
                                            tAlarm.start();
                                        }

                                        Log.d("[DEBUG]", "MSENDMAIL//EMAILNUM/EMAILCNT BEFORE SEND : " + m_eMail + " : " + m_sendMail + ":" +  m_eMailNum + ":" + m_eMailCount + ":" + listFiles.size());
                                        MailSender sm = new MailSender(m_eMail, "Motion Detector Images", "Here are your captured images..", m_eMailPW, listFiles,m_eMailNum) {
                                        };
                                        sm.execute();
                                        Log.d("[DEBUG]", "MSENDMAIL//EMAILNUM/EMAILCNT after SEND : " + m_sendMail + ":" +  m_eMailNum + ":" + m_eMailCount + ":" + listFiles.size());
                                        //TextView TextAddress =  findViewById(R.id.txtStatus);
                                        //TextAddress.setText("eMail has been sent.");
                                        //listFiles.clear();

                                        if (m_allwaysNotify) {
                                            Log.d("[DEBUG]", "RESET AND CONTINUE " );
                                            ResetAndContinue();
                                        }
                                    }
                                }
                            }
                            else {
                                iDiffRange = 0;
                                Collections.sort(list);
                                if (list.size() > 0) {
                                    double dHighest = list.get((list.size() - 1));
                                }
                                list.clear();
                            }
                            DecimalFormat df2 = new DecimalFormat("#.##");
                            String msg = "[Frame: " + counter + "] " + "[Scan:" + df2.format(diff) + "]";
                            if (!m_bPauseBeforeEmail) {
                                runThreadSetText(msg, diff, true);
                                System.out.println("Server: iDiffRange:" + iDiffRange + " : " + diff);
                            }
                        }
                        previousImage = img;

                    } catch (IOException ex) {
                        Log.d("[DEBUG]", "ERROR(0) : " + ex);
                        System.out.println("Server: Error:" + ex);
                        //Logger.getLogger(main.class.getName()).log(Level.SEVERE, null, ex);
                    }

                }
            } catch (Exception ex) {
                Log.d("[DEBUG]", "ERROR(1) : " + ex);
                System.out.println("Server: Error:" + ex);

            }
        }
    }
    public class srvThreadData extends Thread  {
        @SuppressWarnings("deprecation")
        public void run(){
            try {
                DatagramSocket ds = new DatagramSocket(8081);
                byte[] receive = new byte[2048];
                DatagramPacket DpReceive = null;

                System.out.println("Server: Running in thread srvThreadData.");

                while (looper){
                    DpReceive = new DatagramPacket(receive, receive.length);
                    ds.receive(DpReceive);
                    byte[] buff = DpReceive.getData();

                    String sInputData = new String(buff);
                    receive = new byte[2048]; //clear buffer

                    try {

                        String[] arrOfData = sInputData.split("~", 10); //pull out data from string

                        m_tolerance = Float.parseFloat(arrOfData[0]);
                        m_Orientation = arrOfData[1].toString();
                        m_eMail = arrOfData[2].toString();
                        m_eMailPW = arrOfData[3].toString();
                        String sNum = arrOfData[4].toString();
                        m_eMailNum =  Integer.parseInt(sNum);
                        m_sendMail = Boolean.parseBoolean(arrOfData[5]);
                        m_ringtone = arrOfData[6].toString();
                        m_allwaysNotify = Boolean.parseBoolean(arrOfData[7]);

                        runThreadSetText("Tolerance : " ,0,false );
                    } catch (Exception Exception) {
                        System.out.println("Server: IOError(0) : " + Exception.getStackTrace());

                    }

                }
            } catch (IOException ex) {
                System.out.println("Server: Error:" + ex );
                Logger.getLogger(MainActivity.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }

//    Mat setBitmapToMat(Bitmap bitmap) { //OpenCV
//        Mat tmpMat = null;
//        Mat mat = new Mat (bitmap.getWidth(), bitmap.getHeight(), CvType.CV_8UC1);
//        Bitmap bmp32 = bitmap.copy(Bitmap.Config.ARGB_8888, true);
//        Utils.bitmapToMat(bmp32, mat);
//
//        return mat;
//
//    }
//    Bitmap setOutputMatToBitmap(Mat mat) {
//        Bitmap scale = Bitmap.createBitmap(mat.width(), mat.height(), Bitmap.Config.ARGB_8888);
//        return scale;
//    }
    public class srvThreadWait extends Thread {

        public void run() {
            int idx = 0;
            for (int i=0; i< (EMAIL_PAUSE_DURATION / 1000); i++) {
                runThreadSetText("Monitoring starts in " + ((EMAIL_PAUSE_DURATION / 1000) - i) + " seconds", 0, true);
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            m_bOKToGo = true;
            m_bPauseBeforeEmail = false;
            m_deleted = false; //Delete the next set of temp files from device

        }
    }
    public class srvThreadAlarm extends Thread {

        @RequiresApi(api = Build.VERSION_CODES.P)
        public void run() {
            if (m_ringtone.equals("-1"))  //no ringtone selected
                return;

            RingtoneManager ringtoneManager = new RingtoneManager(MainActivity.this);
            ringtoneManager.setType(RingtoneManager.TYPE_NOTIFICATION);
            Cursor c = ringtoneManager.getCursor();

            ringtone = ringtoneManager.getRingtone(Integer.parseInt(m_ringtone));
            //String tone = c.getString(2);
            //AudioManager mgr = (AudioManager)getApplicationContext().getSystemService(Context.AUDIO_SERVICE);
            //mgr.setStreamVolume(AudioManager.STREAM_RING,mgr.getStreamMaxVolume(AudioManager.STREAM_RING),0);

                if (ringtone != null) {
                    Log.d("[DEBUG]", "RINGTONE CLIENT: " + ringtone);

                ringtone.setLooping(true);
                ringtone.play();

                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            m_btnStopAlarm = findViewById(R.id.btnStopAlarm);
                            m_btnStopAlarm.setVisibility(View.VISIBLE);
                            final Animation animBounce = AnimationUtils.loadAnimation(getApplicationContext(),  R.anim.bounce);
                            m_btnStopAlarm.startAnimation(animBounce);

                        } catch (Exception e) {
                            e.printStackTrace();
                        }                    }
                });

            }
        }
    }

//    static MatOfDMatch filterMatchesByDistance(MatOfDMatch matches){ //OpenCV
//        List<DMatch> matches_original = matches.toList();
//        List<DMatch> matches_filtered = new ArrayList<DMatch>();
//
//        int DIST_LIMIT = 30;
//        // Check all the matches distance and if it passes add to list of filtered matches
//        Log.d("DISTFILTER", "ORG SIZE:" + matches_original.size() + "");
//        for (int i = 0; i < matches_original.size(); i++) {
//            DMatch d = matches_original.get(i);
//            if (Math.abs(d.distance) <= DIST_LIMIT) {
//                matches_filtered.add(d);
//            }
//        }
//        Log.d("DISTFILTER", "FIL SIZE:" + matches_filtered.size() + "");
//
//        MatOfDMatch mat = new MatOfDMatch();
//        mat.fromList(matches_filtered);
//        return mat;
//    }
    private double compareImgMethod3(android.graphics.Bitmap image, android.graphics.Bitmap previousImage) {
        long difference = 0;
        int width1 = 0,height1 = 0;
        double avg_different_pixels = 0.00;

        width1 = image.getWidth();
        height1 = image.getHeight();
        for (int i = 0; i < width1; i++)
        {
            for (int j = 0; j < height1; j++)
            {
                int pixel = image.getPixel(i,j);
                int redValue = Color.red(pixel);
                int blueValue = Color.blue(pixel);
                int greenValue = Color.green(pixel);

                int pixel2 = previousImage.getPixel(i,j);
                int redValue2 = Color.red(pixel2);
                int blueValue2 = Color.blue(pixel2);
                int greenValue2 = Color.green(pixel2);
                difference += Math.abs(redValue - redValue2);
                difference += Math.abs(blueValue - blueValue2);
                difference += Math.abs(greenValue - greenValue2);
                double total_pixels = width1 * height1 * 3;
                avg_different_pixels = difference / total_pixels;

//                try {
//                    image.setPixel(i,j, Color.YELLOW);
//                } catch (Exception e) {
//                    Log.d("[DEBUG ERROR] ","[ERR] " + pixel + " : " + pixel2 + " : " + e);
//                    e.printStackTrace();
//                }

            }
        }
        //imgOutput.setImageBitmap(image);

        return avg_different_pixels;
    }

    private void runThreadSetMainImage(Bitmap imgIn,boolean isPreview, int diffCnt) {
        final Bitmap img = imgIn;
        final boolean Preview = isPreview;
        final int snapCnt = diffCnt;

        new Thread() {
            public void run() {

                try {
                    runOnUiThread(new Runnable() {

                        @Override
                        public void run() {
                            ImageView v = null;
                            ImageView v1 = null;
                            try {
                                Thread.sleep(m_MillisecFramePeriod);
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }
                            if (!Preview) {
                                v = (ImageView) findViewById(R.id.imgViewMain);
                                v.setImageBitmap(img);
                            }
                            else {
                                switch (snapCnt) {
                                    case 0:
                                        v =  findViewById(R.id.imageView0);
                                        v.setImageBitmap(img);
                                        moveImage(v);
                                        v1 =  findViewById(R.id.imageView14);
                                        v1.setImageResource(android.R.color.transparent);

                                        break;
                                    case 1:
                                        v =  findViewById(R.id.imageView1);
                                        v.setImageBitmap(img);
                                        moveImage(v);
                                        break;
                                    case 2:
                                        v =  findViewById(R.id.imageView2);
                                        v.setImageBitmap(img);
                                        moveImage(v);
                                        break;
                                    case 3:
                                        v =  findViewById(R.id.imageView3);
                                        v.setImageBitmap(img);
                                        moveImage(v);
                                        break;
                                    case 4:
                                        v =  findViewById(R.id.imageView4);
                                        v.setImageBitmap(img);
                                        moveImage(v);
                                        break;
                                    case 5:
                                        v =  findViewById(R.id.imageView5);
                                        v.setImageBitmap(img);
                                        moveImage(v);
                                        break;
                                    case 6:
                                        v =  findViewById(R.id.imageView6);
                                        v.setImageBitmap(img);
                                        moveImage(v);
                                        break;
                                    case 7:
                                        v =  findViewById(R.id.imageView7);
                                        v.setImageBitmap(img);
                                        moveImage(v);
                                        break;
                                    case 8:
                                        v =  findViewById(R.id.imageView8);
                                        v.setImageBitmap(img);
                                        moveImage(v);
                                        break;
                                    case 9:
                                        v =  findViewById(R.id.imageView9);
                                        v.setImageBitmap(img);
                                        moveImage(v);
                                        break;
                                    case 10:
                                        v =  findViewById(R.id.imageView10);
                                        v.setImageBitmap(img);
                                        moveImage(v);
                                        break;
                                    case 11:
                                        v =  findViewById(R.id.imageView11);
                                        v.setImageBitmap(img);
                                        moveImage(v);
                                        break;
                                    case 12:
                                        v =  findViewById(R.id.imageView12);
                                        v.setImageBitmap(img);
                                        moveImage(v);
                                        break;
                                    case 13:
                                        v =  findViewById(R.id.imageView13);
                                        v.setImageBitmap(img);
                                        moveImage(v);
                                        break;
                                    case 14:
                                        v =  findViewById(R.id.imageView14);
                                        v.setImageBitmap(img);
                                        moveImage(v);
//                                            break;
                                        //experimental
                                        v =  findViewById(R.id.imageView0);
                                        v.setImageResource(android.R.color.transparent);
                                        v =  findViewById(R.id.imageView1);
                                        v.setImageResource(android.R.color.transparent);
                                        v =  findViewById(R.id.imageView2);
                                        v.setImageResource(android.R.color.transparent);
                                        v =  findViewById(R.id.imageView3);
                                        v.setImageResource(android.R.color.transparent);
                                        v =  findViewById(R.id.imageView4);
                                        v.setImageResource(android.R.color.transparent);
                                        v =  findViewById(R.id.imageView5);
                                        v.setImageResource(android.R.color.transparent);
                                        v =  findViewById(R.id.imageView6);
                                        v.setImageResource(android.R.color.transparent);
                                        v =  findViewById(R.id.imageView7);
                                        v.setImageResource(android.R.color.transparent);
                                        v =  findViewById(R.id.imageView8);
                                        v.setImageResource(android.R.color.transparent);
                                        v =  findViewById(R.id.imageView9);
                                        v.setImageResource(android.R.color.transparent);
                                        v =  findViewById(R.id.imageView10);
                                        v.setImageResource(android.R.color.transparent);
                                        v =  findViewById(R.id.imageView11);
                                        v.setImageResource(android.R.color.transparent);
                                        v =  findViewById(R.id.imageView12);
                                        v.setImageResource(android.R.color.transparent);
                                        v =  findViewById(R.id.imageView13);
                                        v.setImageResource(android.R.color.transparent);
                                        //v =  findViewById(R.id.imageView14);
                                        //v.setImageResource(android.R.color.transparent);
                                        break;
                                }
//                                    if (snapCnt < IMAGE_ARRAY_SIZE ) {
//                                        assert v != null;
//                                        v.setImageBitmap(img);
//
//                                        if (snapCnt == 14) {
//                                        //    v.setImageResource(android.R.color.transparent);
//                                        }
//                                    }
                            }
                        }
                    });

                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

        }.start();

    }
    public void moveImage(ImageView b ) {
        //TranslateAnimation animation = new TranslateAnimation(0, 150, 100, -700);
        TranslateAnimation animation = new TranslateAnimation(0, 180, 0, -180);
        final ImageView bb = b;
        animation.setDuration(ANIMATION_DURATION);
        //animation.setDuration(1000);
        animation.setFillAfter(false);
        animation.setZAdjustment (ZORDER_TOP);
        bb.startAnimation(animation);
        animation.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {
                //Log.d("[DEBUG]","onAnimationStart");
            }

            @Override
            public void onAnimationEnd(Animation animation) {

            }

            @Override
            public void onAnimationRepeat(Animation animation) {

            }
        } );
    };

    private void runThreadSetText(String message,double diff,boolean fromImageThread) {
        final String msg = message;
        final double difference = diff;
        final boolean fromImageThrd = fromImageThread;
        new Thread() {
            public void run() {

                try {
                    runOnUiThread(new Runnable() {
                        @SuppressLint("ResourceAsColor")
                        @Override
                        public void run() {
                            TextView t = (TextView) findViewById(R.id.txtStatus);
                            TextView tol = (TextView) findViewById(R.id.txtTolerance);
                            DecimalFormat df2 = new DecimalFormat("##.##");
                            if (fromImageThrd ) {
                                if (difference > m_tolerance) {
                                    t.setTextColor(Color.parseColor("#ffff00")); //yellow

                                    //t.setText("Motion Detected - " + difference);
                                    t.setText("Motion Detected - " + df2.format(difference));
                                } else {
                                    t.setTextColor(Color.parseColor("#12c909")); //green
                                    t.setText(msg);
                                }
                            }
                            else {
                                //DecimalFormat df2 = new DecimalFormat("#.##");
                                tol.setText("Tolerance : " +  df2.format(m_tolerance));
                            }

                        }
                    });

                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

        }.start();
    }
  /*  public static Bitmap resize1(Bitmap img, int newW, int newH) {
        Bitmap otherImage = img;  // .. created somehow
        Graphics g = null;
        BufferedImage newImage = null;
        try {
            newImage = new BufferedImage(newW, newH, BufferedImage.TYPE_INT_RGB);
            g = newImage.createGraphics();
            g.drawImage(otherImage, 0, 0, newW, newH, null);
        } catch (Exception e) {
        }
        g.dispose();
        return newImage;
    };*/


}

