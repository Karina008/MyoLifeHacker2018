package blueberrycheese.myolifehacker.CameraView;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothManager;
import android.content.SharedPreferences;
import android.graphics.drawable.Drawable;
import android.os.Environment;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
//import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.BottomSheetBehavior;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.airbnb.lottie.LottieAnimationView;
import com.otaliastudios.cameraview.CameraListener;
import com.otaliastudios.cameraview.CameraLogger;
import com.otaliastudios.cameraview.CameraOptions;
import com.otaliastudios.cameraview.CameraView;
import com.otaliastudios.cameraview.Flash;
import com.otaliastudios.cameraview.Grid;
import com.otaliastudios.cameraview.SessionType;
import com.otaliastudios.cameraview.Size;
import com.otaliastudios.cameraview.VideoQuality;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.HashMap;

import blueberrycheese.myolifehacker.FontConfig;
import blueberrycheese.myolifehacker.MyoApp;
import blueberrycheese.myolifehacker.R;
import blueberrycheese.myolifehacker.Toasty;
import blueberrycheese.myolifehacker.events.ServiceEvent;
import blueberrycheese.myolifehacker.myo_manage.GestureSaveMethod;
import blueberrycheese.myolifehacker.myo_manage.GestureSaveModel;
import blueberrycheese.myolifehacker.myo_manage.MyoGattCallback;
import blueberrycheese.myolifehacker.myo_manage.GestureDetectModelManager;
import blueberrycheese.myolifehacker.myo_manage.IGestureDetectModel;
import blueberrycheese.myolifehacker.myo_manage.MyoCommandList;
import blueberrycheese.myolifehacker.myo_manage.MyoService;
import blueberrycheese.myolifehacker.myo_manage.NopModel;

public class CameraActivity extends AppCompatActivity implements View.OnClickListener, ControlView.Callback {

//    @Override
//    protected void onCreate(Bundle savedInstanceState) {
//        super.onCreate(savedInstanceState);
//        setContentView(R.layout.activity_camera);
//    }

    private CameraView camera;
    private ViewGroup controlPanel;

    private boolean mCapturingPicture;
    private boolean mCapturingVideo;

    private MyoApp myoApp = null;
    // To show stuff in the callback
    private Size mCaptureNativeSize;
    private long mCaptureTime;
    private LottieAnimationView animationView_camera_lock;
    private LottieAnimationView animationView_camera_unlock;
    boolean videoRecording = false;
    private Drawable icon_1,icon_2,icon_3,icon_4,icon_5,icon_6;

    private static final int VIBRATION_A = 1;
    private static final int VIBRATION_B = 2;
    private static final int VIBRATION_C = 3;

    private static final int ADDITIONAL_DELAY = 5000;
    private boolean first=true;
    private boolean myoConnection;
    private static final int CURRENT_ACTIVITY = 0;
    private Toast toast;
    private static final String TAG = "CameraActivity";
//    private Handler mHandler;
    private TextView gestureText;
    String[] gestureString = {"WiFi On, Off", "Sound Mode Chnage ", "Volume Up", "Volume Down", "Brightness Up", "Brightness Down"};

    private int gestureNum = -1;
    private Flash currentCameraFlash = Flash.OFF;
    private Grid currentGrid = Grid.OFF;
    int[] smoothcount = new int[6];
    private int lock_vibrate_state;
    private int recog_vibrate_state;
    private int conn_vibrate_state;
    private SharedPreferences sharedPreferences;  //sharePreference호출 후 적용

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        //화면 꺼짐/잠금 상태에서 가능하도록
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED
                | WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD
                | WindowManager.LayoutParams.FLAG_FULLSCREEN
                | WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED
//                | WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
                | WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
                | WindowManager.LayoutParams.FLAG_ALLOW_LOCK_WHILE_SCREEN_ON);

        getWindow().setFlags(WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED,
                WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED);
        setContentView(R.layout.activity_camera);
        this.getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);  //윈도우 가장위에 배터리,wifi뜨는 부분 제거
        CameraLogger.setLogLevel(CameraLogger.LEVEL_VERBOSE);
        setTitle("Camera");
        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getBaseContext());  //sharePreference호출 후 적용
        setting_vibrate();

        camera = findViewById(R.id.camera);
        camera.addCameraListener(new CameraListener() {
            public void onCameraOpened(CameraOptions options) { onOpened(); }
            public void onPictureTaken(byte[] jpeg) { onPicture(jpeg); }

            @Override
            public void onVideoTaken(File video) {
                super.onVideoTaken(video);
                onVideo(video);
            }
        });

        icon_1 = getResources().getDrawable(R.drawable.gesture_1_w);
        icon_2 = getResources().getDrawable(R.drawable.gesture_2_w);
        icon_3 = getResources().getDrawable(R.drawable.gesture_3_w);
        icon_4 = getResources().getDrawable(R.drawable.gesture_4_w);
        icon_5 = getResources().getDrawable(R.drawable.gesture_5_w);
        icon_6 = getResources().getDrawable(R.drawable.gesture_6_w);

        FontConfig.setGlobalFont(this,getWindow().getDecorView());
        findViewById(R.id.edit).setOnClickListener(this);
        findViewById(R.id.capturePhoto).setOnClickListener(this);
        findViewById(R.id.captureVideo).setOnClickListener(this);
        findViewById(R.id.toggleCamera).setOnClickListener(this);
        animationView_camera_lock = (LottieAnimationView) findViewById(R.id.lottie_camera_lock);
        animationView_camera_unlock = (LottieAnimationView) findViewById(R.id.lottie_camera_unlock);
       animationView_camera_lock.setVisibility(View.INVISIBLE);
        animationView_camera_unlock.setVisibility(View.INVISIBLE);
        controlPanel = findViewById(R.id.controls);
        ViewGroup group = (ViewGroup) controlPanel.getChildAt(0);
        Control[] controls = Control.values();
        for (Control control : controls) {
            ControlView view = new ControlView(this, control, this);
            group.addView(view, ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT);
        }

        controlPanel.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                BottomSheetBehavior b = BottomSheetBehavior.from(controlPanel);
                b.setState(BottomSheetBehavior.STATE_HIDDEN);
            }
        });

        camera.setVideoQuality(VideoQuality.MAX_1080P);

    }

    private void message(String content, boolean important) {
        int length = important ? Toast.LENGTH_LONG : Toast.LENGTH_SHORT;
        Toast.makeText(this, content, length).show();
    }

    private void onOpened() {
        ViewGroup group = (ViewGroup) controlPanel.getChildAt(0);
        for (int i = 0; i < group.getChildCount(); i++) {
            ControlView view = (ControlView) group.getChildAt(i);
            view.onCameraOpened(camera);
        }
    }

    private void onPicture(byte[] jpeg) {
        mCapturingPicture = false;
        long callbackTime = System.currentTimeMillis();
        if (mCapturingVideo) {
//            message("Captured while taking video. Size="+mCaptureNativeSize, false);
            return;
        }

        // This can happen if picture was taken with a gesture.
        if (mCaptureTime == 0) mCaptureTime = callbackTime - 300;
        if (mCaptureNativeSize == null) mCaptureNativeSize = camera.getPictureSize();

        PicturePreviewActivity.setImage(jpeg);
        Intent intent = new Intent(CameraActivity.this, PicturePreviewActivity.class);
        intent.putExtra("delay", callbackTime - mCaptureTime);
        intent.putExtra("nativeWidth", mCaptureNativeSize.getWidth());
        intent.putExtra("nativeHeight", mCaptureNativeSize.getHeight());
        startActivity(intent);

        mCaptureTime = 0;
        mCaptureNativeSize = null;
    }

    private void onVideo(File video) {
        mCapturingVideo = false;
        Log.d("VideoUri","Video Uri"+Uri.fromFile(video));

        Intent intent = new Intent(CameraActivity.this, VideoPreviewActivity.class);
        intent.putExtra("video", Uri.fromFile(video));
        startActivity(intent);
        saveVideo(Uri.fromFile(video));
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.edit: edit(); break;
            case R.id.capturePhoto: capturePhoto(); break;
            case R.id.captureVideo:
                startVideoRecording();
//                if(videoRecording == false){
//                    videoRecording = true;
//                    captureVideo();
//                } else if(videoRecording == true){
//                    videoRecording = false;
//                    camera.stopCapturingVideo();
//                }
                break;
            case R.id.toggleCamera: toggleCamera(); break;
        }
    }

    @Override
    public void onBackPressed() {
        BottomSheetBehavior b = BottomSheetBehavior.from(controlPanel);
        if (b.getState() != BottomSheetBehavior.STATE_HIDDEN) {
            b.setState(BottomSheetBehavior.STATE_HIDDEN);
            return;
        }
        super.onBackPressed();
    }

    private void edit() {
        BottomSheetBehavior b = BottomSheetBehavior.from(controlPanel);
        b.setState(BottomSheetBehavior.STATE_COLLAPSED);
    }

    private void capturePhoto() {
        if (mCapturingPicture || mCapturingVideo) return;
        mCapturingPicture = true;
        mCaptureTime = System.currentTimeMillis();
        mCaptureNativeSize = camera.getPictureSize();
        message("Capturing picture...", false);
        camera.capturePicture();
    }

    private void captureVideo() {
        if (camera.getSessionType() != SessionType.VIDEO) {
            message("Can't record video while session type is 'picture'.", false);
            return;
        }
        if (mCapturingPicture || mCapturingVideo) return;
        mCapturingVideo = true;
        message("Recording...", true);
//        camera.startCapturingVideo(null, 8000);
        camera.startCapturingVideo(null);
    }

    private void toggleCamera() {
        if (mCapturingPicture) return;
        switch (camera.toggleFacing()) {
            case BACK:
                message("Switched to back camera!", false);
                break;

            case FRONT:
                message("Switched to front camera!", false);
                break;
        }
    }

    @Override
    public boolean onValueChanged(Control control, Object value, String name) {
        if (!camera.isHardwareAccelerated() && (control == Control.WIDTH || control == Control.HEIGHT)) {
            if ((Integer) value > 0) {
                message("This device does not support hardware acceleration. " +
                        "In this case you can not change width or height. " +
                        "The view will act as WRAP_CONTENT by default.", true);
                return false;
            }
        }
        control.applyValue(camera, value);
        BottomSheetBehavior b = BottomSheetBehavior.from(controlPanel);
        b.setState(BottomSheetBehavior.STATE_HIDDEN);
        message("Changed " + control.getName() + " to " + name, false);
        return true;
    }

    //region Boilerplate

    @Override
    protected void onResume() {
        super.onResume();
        if(!EventBus.getDefault().isRegistered(this)) {
            EventBus.getDefault().register(this);
        }
        //Post event to notify that user's watching the activity.
        EventBus.getDefault().postSticky(new ServiceEvent.currentActivity_Event(CURRENT_ACTIVITY));
        camera.start();
        camera.setSessionType(SessionType.PICTURE);

    }

    @Override
    protected void onPause() {
        //Post event to notify that user's leaving the activity.
        EventBus.getDefault().postSticky(new ServiceEvent.currentActivity_Event(-1));
        super.onPause();
        camera.stop();
//        emgOff();
//        detectOn = false;
    }

    @Override
    protected void onDestroy() {
        camera.destroy();
        super.onDestroy();
//        emgOff();
//        detectOn = false;
    }


    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        boolean valid = true;
        for (int grantResult : grantResults) {
            valid = valid && grantResult == PackageManager.PERMISSION_GRANTED;
        }
        if (valid && !camera.isStarted()) {
            camera.start();
        }
    }

    //endregion


    @Override
    public void onStop(){
        //Post event to notify that user's leaving the activity.
//        EventBus.getDefault().postSticky(new ServiceEvent.currentActivity_Event(-1));
        EventBus.getDefault().unregister(this);
        super.onStop();
//        this.closeBLEGatt();

    }


    public void saveVideo(Uri videoUri){
        String root = Environment.getExternalStorageDirectory().toString();
        String video_name = "" + System.currentTimeMillis();

        File myDir = new File(root+"/MHL_Camera");
        myDir.mkdirs();
        String fname = "MHL_Video_" + video_name+ ".mp4";
        File file = new File(myDir, fname);
        if (file.exists()){
            file.delete();
        }
//        Log.i("LOAD", root + fname);
//        try {
//            FileOutputStream out = new FileOutputStream(file);
//            finalBitmap.compress(Bitmap.CompressFormat.JPEG, 90, out);
//            out.flush();
//            out.close();
//        } catch (Exception e) {
//            e.printStackTrace();
//        }

        try{
            FileOutputStream newVideo = new FileOutputStream(file);
            FileInputStream tempVideo = new FileInputStream(videoUri.getPath());

            // Transfer bytes from in to out
            byte[] buf = new byte[1024];
            int len;
            while ((len = tempVideo.read(buf)) > 0) {
                newVideo.write(buf, 0, len);
            }
            newVideo.flush();
            newVideo.close();
            tempVideo.close();

        } catch(Exception e){
            e.printStackTrace();
        }
    }

    // 마요 잠기면 애니메이션 재생
    @Subscribe
    public void getMyoDevice(ServiceEvent.myoLock_Event event) {
        myoConnection = event.lock;
        if(myoConnection) {
            //  animationView_main.cancelAnimation();
            //  animationView_main.clearAnimation();
            //  animationView_main.setAnimation("lock.json");
            animationView_camera_lock.playAnimation();
            animationView_camera_lock.loop(true);
            animationView_camera_lock.setVisibility(View.VISIBLE);
            animationView_camera_unlock.setVisibility(View.INVISIBLE);
        }
        else {
            //  animationView_main.cancelAnimation();
            // animationView_main.clearAnimation();
            //animationView_main_unlock.setAnimation("material_wave_loading.json");
            animationView_camera_unlock.playAnimation();
            animationView_camera_unlock.loop(true);
            animationView_camera_unlock.setVisibility(View.VISIBLE);
            animationView_camera_lock.setVisibility(View.INVISIBLE);
        }
    }

    // 마요 연결되어 있으면 애니메이션 재생
    @Subscribe(sticky = true)
    public void getMyoDevice(ServiceEvent.myoConnected_Event event) {
        myoConnection = event.connection;
        myoApp = (MyoApp) getApplication().getApplicationContext();
        if(myoConnection) {
            if(!myoApp.isUnlocked()) {
                animationView_camera_lock.playAnimation();
                animationView_camera_lock.loop(true);
                animationView_camera_lock.setVisibility(View.VISIBLE);
                first=false;
            }else {
                animationView_camera_unlock.playAnimation();
                animationView_camera_unlock.loop(true);
                animationView_camera_unlock.setVisibility(View.VISIBLE);
                first=false;
            }
        }
        else {
            animationView_camera_lock.cancelAnimation();
            animationView_camera_unlock.cancelAnimation();
            animationView_camera_lock.setVisibility(View.INVISIBLE);
            animationView_camera_unlock.setVisibility(View.INVISIBLE);
        }
    }
/*
    @Subscribe(sticky = true)
    public void getMyoDevice(ServiceEvent.myoConnected_Event event) {
        myoConnection = event.connection;
        if(myoConnection) {
            animationView_camera.playAnimation();
            animationView_camera.loop(true);
            animationView_camera.setVisibility(View.VISIBLE);
        }
        else {
            animationView_camera.cancelAnimation();
            animationView_camera.setVisibility(View.INVISIBLE);
        }
    }
*/
    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onGestureEvent(ServiceEvent.GestureEvent event) {
        gestureNum = event.gestureNumber;
        Log.d(TAG,"CameraEvent Gesture num : "+event.gestureNumber);


        switch(gestureNum){
            case 0 :
                smoothcount[gestureNum]++;
                if(smoothcount[gestureNum]>2) {
                    //Send Vibration Event
                    EventBus.getDefault().post(new ServiceEvent.VibrateEvent(recog_vibrate_state));
                    //Restart lock Timer so user can use gesture continuously
                    EventBus.getDefault().post(new ServiceEvent.restartLockTimerEvent(ADDITIONAL_DELAY));

                    capturePhoto();
//                    smoothcount[gestureNum]=-1;
                    resetSmoothCount();
                    if (toast!=null)
                        toast.cancel();
                    toast =Toasty.normal(getBaseContext(),"Capture Photo", Toast.LENGTH_SHORT, icon_1);
                    toast.show();
                }


                break;

            case 1 :
                smoothcount[gestureNum]++;
                if(smoothcount[gestureNum]>2) {
                    //Send Vibration Event
                    EventBus.getDefault().post(new ServiceEvent.VibrateEvent(recog_vibrate_state));
                    //Restart lock Timer so user can use gesture continuously
                    EventBus.getDefault().post(new ServiceEvent.restartLockTimerEvent(ADDITIONAL_DELAY));

                    switch(currentCameraFlash){
                        case OFF:
                            camera.setFlash(Flash.ON);
                            currentCameraFlash = Flash.ON;
                            if (toast!=null)
                                toast.cancel();
                            toast =Toasty.normal(getBaseContext(),"Flash mode off", Toast.LENGTH_SHORT, icon_2);
                            toast.show();
                            break;
                        case ON:
                            camera.setFlash(Flash.AUTO);
                            currentCameraFlash = Flash.AUTO;
                            if (toast!=null)
                                toast.cancel();
                            toast =Toasty.normal(getBaseContext(),"Flash mode Auto", Toast.LENGTH_SHORT, icon_2);
                            toast.show();
                            break;
                        case AUTO:
                            camera.setFlash(Flash.TORCH);
                            currentCameraFlash = Flash.TORCH;
                            if (toast!=null)
                                toast.cancel();
                            toast =Toasty.normal(getBaseContext(),"Flash mode Touch", Toast.LENGTH_SHORT, icon_2);
                            toast.show();
                            break;
                        case TORCH:
                            camera.setFlash(Flash.OFF);
                            currentCameraFlash = Flash.OFF;
                            if (toast!=null)
                                toast.cancel();
                            toast =Toasty.normal(getBaseContext(),"Flash mode off", Toast.LENGTH_SHORT, icon_2);
                            toast.show();
                            break;
                        default:
                            break;
                    }

//                    smoothcount[gestureNum]=-1;
                    resetSmoothCount();
                }

                break;

            case 2 :
                smoothcount[gestureNum]++;
                if(smoothcount[gestureNum]>2) {
                    //Send Vibration Event
                    EventBus.getDefault().post(new ServiceEvent.VibrateEvent(recog_vibrate_state));
                    //Restart lock Timer so user can use gesture continuously
                    EventBus.getDefault().post(new ServiceEvent.restartLockTimerEvent(ADDITIONAL_DELAY));

                    toggleCamera();

                    if (toast!=null)
                        toast.cancel();
                    toast = Toasty.normal(getBaseContext(),"Camera Switch", Toast.LENGTH_SHORT, icon_3);
                    toast.show();
//                    switch(currentGrid){
//                        case OFF:
//                            camera.setGrid(Grid.DRAW_3X3);
//                            currentGrid = Grid.DRAW_3X3;
//                            Toasty.normal(getBaseContext(),"Grid mode  Draw", Toast.LENGTH_SHORT, icon_3).show();
//                            break;
//                        case DRAW_3X3:
//                            camera.setGrid(Grid.OFF);
//                            currentGrid = Grid.OFF;
//                            Toasty.normal(getBaseContext(),"Grid mode  off", Toast.LENGTH_SHORT, icon_3).show();
//                            break;
//                        default:
//                            break;
//                    }

//                    smoothcount[gestureNum]=-1;
                    resetSmoothCount();
                }


                break;

            case 3 :
                smoothcount[gestureNum]++;
                if(smoothcount[gestureNum]>1) {
                    //Send Vibration Event
                    EventBus.getDefault().post(new ServiceEvent.VibrateEvent(recog_vibrate_state));
                    //Restart lock Timer so user can use gesture continuously
                    EventBus.getDefault().post(new ServiceEvent.restartLockTimerEvent(ADDITIONAL_DELAY));

                    camera.setSessionType(SessionType.VIDEO);
                    if(videoRecording == false){
                        videoRecording = true;
                        captureVideo();
                        if (toast!=null)
                            toast.cancel();
                        toast = Toasty.normal(getBaseContext(),"Video record start", Toast.LENGTH_SHORT, icon_4);
                        toast.show();
                    } else if(videoRecording == true){
                        videoRecording = false;
                        camera.stopCapturingVideo();
                        if (toast!=null)
                            toast.cancel();
                        toast = Toasty.normal(getBaseContext(),"Video record stop", Toast.LENGTH_SHORT, icon_4);
                        toast.show();
                         }

//                    smoothcount[gestureNum]=-1;
                    resetSmoothCount();
                }
            case 5:
                smoothcount[gestureNum]++;
                if(smoothcount[gestureNum]>1) {
                    //Send Vibration Event
                    EventBus.getDefault().post(new ServiceEvent.VibrateEvent(recog_vibrate_state));
                    //Restart lock Timer so user can use gesture continuously
                    EventBus.getDefault().post(new ServiceEvent.restartLockTimerEvent(ADDITIONAL_DELAY));
                    if (toast!=null)
                        toast.cancel();
                    toast = Toasty.normal(getBaseContext(),"Go back", Toast.LENGTH_SHORT, icon_6);
                    toast.show();
                    finish();
                    resetSmoothCount();
//                    Toasty.normal(getBaseContext(),"Capture Photo", Toast.LENGTH_SHORT, icon_1).show();
                }

                break;

            default :
                break;

        }
    }

    public void startVideoRecording(){
        camera.setSessionType(SessionType.VIDEO);
        if(videoRecording == false){
            videoRecording = true;
            captureVideo();
            Toasty.normal(getBaseContext(),"Video record start", Toast.LENGTH_SHORT, icon_4).show();
        } else if(videoRecording == true){
            videoRecording = false;
            camera.stopCapturingVideo();
            Toasty.normal(getBaseContext(),"Video record stop", Toast.LENGTH_SHORT, icon_4).show();
        }
    }

    public void resetSmoothCount(){
        for(int i=0;i<smoothcount.length;i++){
            smoothcount[i]=0;
        }
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
