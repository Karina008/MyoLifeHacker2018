package blueberrycheese.myolifehacker.CameraView;

import android.content.SharedPreferences;
import android.graphics.drawable.Drawable;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

import android.app.Activity;
import android.graphics.Bitmap;
import android.support.annotation.Nullable;
import android.util.Log;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.otaliastudios.cameraview.AspectRatio;
import com.otaliastudios.cameraview.CameraUtils;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.io.File;
import java.io.FileOutputStream;
import java.lang.ref.WeakReference;

import blueberrycheese.myolifehacker.R;
import blueberrycheese.myolifehacker.Toasty;
import blueberrycheese.myolifehacker.events.ServiceEvent;


public class PicturePreviewActivity extends AppCompatActivity {
    private static final String TAG = "PicturePreviewActivity";
    private static final int ADDITIONAL_DELAY = 0;
    private static final int VIBRATION_A = 1;
    private static final int VIBRATION_B = 2;
    private static final int VIBRATION_C = 3;
    private Drawable icon_1,icon_2,icon_3,icon_4,icon_5,icon_6;

    private int gestureNum = -1;
    int[] smoothcount = new int[6];
    private static final int CURRENT_ACTIVITY = 2;
    private int lock_vibrate_state;
    private int recog_vibrate_state;
    private int conn_vibrate_state;
    private SharedPreferences sharedPreferences;  //sharePreference호출 후 적용
    private static WeakReference<byte[]> image;

    public static void setImage(@Nullable byte[] im) {
        image = im != null ? new WeakReference<>(im) : null;
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        //화면 꺼짐/잠금 상태에서 가능하도록
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED
                | WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD
                | WindowManager.LayoutParams.FLAG_FULLSCREEN
                | WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED
//                | WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
                | WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
                | WindowManager.LayoutParams.FLAG_ALLOW_LOCK_WHILE_SCREEN_ON);


        setContentView(R.layout.activity_picture_preview);
        this.getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);  //윈도우 가장위에 배터리,wifi뜨는 부분 제거
        setTitle("Picture Preview");
        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getBaseContext());  //sharePreference호출 후 적용
        setting_vibrate();
        final ImageView imageView = findViewById(R.id.image);
        final MessageView nativeCaptureResolution = findViewById(R.id.nativeCaptureResolution);
        // final MessageView actualResolution = findViewById(R.id.actualResolution);
        // final MessageView approxUncompressedSize = findViewById(R.id.approxUncompressedSize);
        final MessageView captureLatency = findViewById(R.id.captureLatency);

        final long delay = getIntent().getLongExtra("delay", 0);
        final int nativeWidth = getIntent().getIntExtra("nativeWidth", 0);
        final int nativeHeight = getIntent().getIntExtra("nativeHeight", 0);
        byte[] b = image == null ? null : image.get();
        if (b == null) {
            finish();
            return;
        }

        icon_1 = getResources().getDrawable(R.drawable.gesture_1_w);
        icon_2 = getResources().getDrawable(R.drawable.gesture_2_w);
        icon_3 = getResources().getDrawable(R.drawable.gesture_3_w);
        icon_4 = getResources().getDrawable(R.drawable.gesture_4_w);
        icon_5 = getResources().getDrawable(R.drawable.gesture_5_w);
        icon_6 = getResources().getDrawable(R.drawable.gesture_6_w);


        CameraUtils.decodeBitmap(b, 1000, 1000, new CameraUtils.BitmapCallback() {
            @Override
            public void onBitmapReady(Bitmap bitmap) {
                imageView.setImageBitmap(bitmap);
                saveImage(bitmap, "" + System.currentTimeMillis());

                // approxUncompressedSize.setTitle("Approx. uncompressed size");
                // approxUncompressedSize.setMessage(getApproximateFileMegabytes(bitmap) + "MB");

                captureLatency.setTitle("Approx. capture latency");
                captureLatency.setMessage(delay + " milliseconds");

                // ncr and ar might be different when cropOutput is true.
                AspectRatio nativeRatio = AspectRatio.of(nativeWidth, nativeHeight);
                nativeCaptureResolution.setTitle("Native capture resolution");
                nativeCaptureResolution.setMessage(nativeWidth + "x" + nativeHeight + " (" + nativeRatio + ")");

                // AspectRatio finalRatio = AspectRatio.of(bitmap.getWidth(), bitmap.getHeight());
                // actualResolution.setTitle("Actual resolution");
                // actualResolution.setMessage(bitmap.getWidth() + "x" + bitmap.getHeight() + " (" + finalRatio + ")");
            }
        });

        //Restart lock Timer so user can use gesture continuously
        EventBus.getDefault().post(new ServiceEvent.restartLockTimerEvent(ADDITIONAL_DELAY));

    }

    private static float getApproximateFileMegabytes(Bitmap bitmap) {
        return (bitmap.getRowBytes() * bitmap.getHeight()) / 1024 / 1024;
    }

    private void saveImage(Bitmap finalBitmap, String image_name) {
        String root = Environment.getExternalStorageDirectory().toString();
//        String time = "" + System.currentTimeMillis();
        File myDir = new File(root+"/MHL_Camera");
        myDir.mkdirs();
        String fname = "MHL_Image_" + image_name+ ".jpg";
        File file = new File(myDir, fname);
        if (file.exists()){
            file.delete();
        }
        Log.i("LOAD", root + fname);
        try {
            FileOutputStream out = new FileOutputStream(file);
            finalBitmap.compress(Bitmap.CompressFormat.JPEG, 90, out);
            out.flush();
            out.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onGestureEvent(ServiceEvent.GestureEvent event) {
        gestureNum = event.gestureNumber;
        Log.d(TAG,"Gesture num : "+event.gestureNumber);

        switch(gestureNum){
            case 0 :
                if(smoothcount[gestureNum]>1) {
                    //Send Vibration Event
                    EventBus.getDefault().post(new ServiceEvent.VibrateEvent(VIBRATION_A));
                    //Restart lock Timer so user can use gesture continuously
                    EventBus.getDefault().post(new ServiceEvent.restartLockTimerEvent(ADDITIONAL_DELAY));
                    finish();
                    Toasty.normal(getBaseContext(),"Capture Photo", Toast.LENGTH_SHORT, icon_1).show();
                    smoothcount[gestureNum]=-1;
                    resetSmoothCount();
                }
                smoothcount[gestureNum]++;

                break;
            default :
                break;

        }
    }

    public void resetSmoothCount(){
        for(int i=0;i<smoothcount.length;i++){
            smoothcount[i]=0;
        }
    }

    @Override
    protected void onResume(){
        super.onResume();
        if(!EventBus.getDefault().isRegistered(this)) {
            EventBus.getDefault().register(this);
        }
        //Post event to notify that user's watching the activity.
        EventBus.getDefault().postSticky(new ServiceEvent.currentActivity_Event(CURRENT_ACTIVITY));
    }

    @Override
    public void onStop(){
//        //Post event to notify that user's leaving the activity.
//        EventBus.getDefault().postSticky(new ServiceEvent.currentActivity_Event(-1));
        EventBus.getDefault().unregister(this);
        super.onStop();

    }

    @Override
    public void onPause(){
        //Post event to notify that user's leaving the activity.
        EventBus.getDefault().postSticky(new ServiceEvent.currentActivity_Event(-1));
        super.onPause();
    }

    public void setting_vibrate(){
        String lock_vp = sharedPreferences.getString("lock_vibrate_power","강하게");
        String recog_vp = sharedPreferences.getString("recog_vibrate_power","강하게");
        String conn_vp = sharedPreferences.getString("conn_vibrate_power","강하게");
        int lock_vpp,recog_vpp,conn_vpp;

        boolean iv = sharedPreferences.getBoolean("vibrate",true);
        if(iv){
            if(lock_vp.equals("강하게"))
                lock_vpp=3;
            else if(lock_vp.equals("보통"))
                lock_vpp=2;
            else if(lock_vp.equals("약하게"))
                lock_vpp=1;
            else
                lock_vpp=3;

            if(recog_vp.equals("강하게"))
                recog_vpp=3;
            else if(recog_vp.equals("보통"))
                recog_vpp=2;
            else if(recog_vp.equals("약하게"))
                recog_vpp=1;
            else
                recog_vpp=3;

            if(conn_vp.equals("강하게"))
                conn_vpp=3;
            else if(conn_vp.equals("보통"))
                conn_vpp=2;
            else if(conn_vp.equals("약하게"))
                conn_vpp=1;
            else
                conn_vpp=3;
        }else{
            lock_vpp=0;
            recog_vpp=0;
            conn_vpp=0;
        }
        lock_vibrate_state = lock_vpp;
        recog_vibrate_state = recog_vpp;
        conn_vibrate_state = conn_vpp;
    }
}
