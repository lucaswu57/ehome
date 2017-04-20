package cc.wulian.smarthomev5.eyecat;

import android.app.Activity;
import android.content.Context;
import android.content.res.Configuration;
import android.media.AudioManager;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnTouchListener;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.LinearLayout.LayoutParams;
import android.widget.RelativeLayout;
import android.widget.Toast;

import com.eques.icvss.core.module.user.BuddyType;
import com.eques.icvss.utils.ELog;
import com.eques.icvss.utils.Method;

import org.apache.commons.lang3.StringUtils;
import org.json.JSONObject;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;

import cc.wulian.ihome.wan.util.StringUtil;
import cc.wulian.smarthomev5.R;

public class EyecatVideoCallActivity extends Activity {
	private SurfaceView surfaceView;
	private String callId;
	private int currVolume;
	private int devType = 0;
	private int current;
	private boolean isMuteFlag;
	private boolean hasVideo;
	private String uid;
	private boolean isPlaying = false;
	private boolean isExit = false;
	private AudioManager audioManager;
	private LinearLayout linear_padding;
	private FrameLayout btnCapture, btnMute, btnHangupCall;
	private Handler handler = new Handler(Looper.getMainLooper());

	private ImageView btnSoundSwitch,iv_mute;
	
	int width = 640;
	int height = 480;
	
	private int screenWidthDip;
	private int screenHeightDip;
	private String bid;
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON | WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED);
		setContentView(R.layout.eyecat_activity_videomain);

		audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
		current = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC);
		audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, current, 0);
		
		currVolume = current;
		hasVideo = getIntent().getBooleanExtra(Method.ATTR_CALL_HASVIDEO, false);
		bid = getIntent().getStringExtra("bid");
		initUI();
		EyecatManager.getInstance().login();
		EyecatManager.getInstance().addPacketListener(videoCallListener);
		EyecatManager.getInstance().addPacketListener(videoPlayingListener);
	}
	public void onBackPressed() {
		hangUpCall();
	}

	@Override
	protected void onResume() {
		super.onResume();
		new Thread(){
			@Override
			public void run() {
				showVideo();
			}
		}.start();
	}

	@Override
	protected void onPause() {
		super.onPause();
		isExit = true;
		EyecatManager.getInstance().removePacketListener(videoCallListener);
		EyecatManager.getInstance().removePacketListener(videoPlayingListener);
		handler.removeCallbacks(runnable);
	}
	protected void onDestroy() {
		super.onDestroy();
		audioManager.setStreamMute(AudioManager.STREAM_MUSIC, false);
		audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, current, 0);
		closeSpeaker();
	}
	private void initUI() {
		surfaceView = (SurfaceView) findViewById(R.id.surface_view);

		btnCapture = (FrameLayout) findViewById(R.id.btn_capture);
		btnCapture.setOnClickListener(new MyOnClickListener());

		btnMute = (FrameLayout) findViewById(R.id.btn_mute);
		btnMute.setOnClickListener(new MyOnClickListener());

		btnHangupCall = (FrameLayout) findViewById(R.id.btn_hangupCall);
		btnHangupCall.setOnClickListener(new MyOnClickListener());

		btnSoundSwitch = (ImageView) findViewById(R.id.btn_soundSwitch);
		btnSoundSwitch.setOnTouchListener(new MyOnTouchListener());

		linear_padding = (LinearLayout) findViewById(R.id.linear_padding);
		iv_mute = (ImageView) findViewById(R.id.iv_mute);
		RelativeLayout relative_videocall = (RelativeLayout) findViewById(R.id.relative_videocall);
		relative_videocall.setOnClickListener(new MyOnClickListener());

	}
	void showVideo(){
		int count = 0;
		EyecatManager.EyecatDevice device = null;
		runOnUiThread(new Runnable() {
			@Override
			public void run() {
				Toast.makeText(EyecatVideoCallActivity.this,"正在尝试连接。。",Toast.LENGTH_SHORT).show();
			}
		});
		while(count < 10) {
			device = EyecatManager.getInstance().getDevice(bid);
			if (device == null) {
				try {
					Thread.sleep(1000);
				}catch (Exception e){
					e.printStackTrace();
				}
				count++;
			}else{
				break;
			}
		}
		if(device == null){
			runOnUiThread(new Runnable() {
				@Override
				public void run() {
					Toast.makeText(EyecatVideoCallActivity.this,"登入失败,请重新尝试",Toast.LENGTH_LONG).show();
					finish();
				}
			});
		}else if(StringUtil.isNullOrEmpty(device.getUid())){
			runOnUiThread(new Runnable() {
				  @Override
				  public void run() {
					  Toast.makeText(EyecatVideoCallActivity.this,"设备不在线，请检查设备",Toast.LENGTH_LONG).show();
					  finish();
				  }
			});
		}else{
			uid = device.getUid();
			runOnUiThread(new Runnable() {
				@Override
				public void run() {
					Toast.makeText(EyecatVideoCallActivity.this,"开始打开回话:"+uid+",hasVideo:"+hasVideo,Toast.LENGTH_SHORT).show();
					boolean bo = audioManager.isWiredHeadsetOn();
					if(!bo){
						openSpeaker();
					}
					setVideoSize();

					LayoutParams layoutParams;
					if(devType == BuddyType.TYPE_CAMERA_C01){
						layoutParams = new LayoutParams(screenWidthDip, (screenWidthDip / 5));
					}else{
						layoutParams = new LayoutParams(screenWidthDip, (screenWidthDip / 7));
					}
					linear_padding.setLayoutParams(layoutParams);
					startUpCall(uid);
//					if(hasVideo){ //是否显示视频
//						callId = EyecatManager.getInstance().getICVSSUserInstance().equesOpenCall(uid, surfaceView.getHolder().getSurface()); //视频 + 语音通话
//					}else{
//						callId = EyecatManager.getInstance().getICVSSUserInstance().equesOpenCall(uid, surfaceView, null); //语音通话
//					}
					surfaceView.getHolder().addCallback(new SurfaceHolder.Callback() {

						public void surfaceChanged(SurfaceHolder holder, int arg1,
												   int arg2, int arg3) {
						}

						public void surfaceCreated(SurfaceHolder holder) {
							Log.i("eyecat:","surfaceCreated");

						}

						public void surfaceDestroyed(SurfaceHolder holder) {
							Log.i("eyecat:","surfaceDestroyed");
						}
					});

				}
			});
		}
	}

	private void callSpeakerSetting(boolean f) {
		if (f) {

			btnSoundSwitch.setBackgroundResource(R.color.action_bar_bg);

			if (callId != null) {
				EyecatManager.getInstance().getICVSSUserInstance().equesAudioRecordEnable(true, callId);
				EyecatManager.getInstance().getICVSSUserInstance().equesAudioPlayEnable(false, callId);
			}
			closeSpeaker();
		} else {

			btnSoundSwitch.setBackgroundResource(R.color.text_gray);

			if (callId != null) {
				EyecatManager.getInstance().getICVSSUserInstance().equesAudioPlayEnable(true, callId);
				EyecatManager.getInstance().getICVSSUserInstance().equesAudioRecordEnable(false, callId);
			}
			openSpeaker();
		}
	}
	
	private void setVideoSize(){
		DisplayMetrics dm = new DisplayMetrics();
		getWindowManager().getDefaultDisplay().getMetrics(dm);
		screenWidthDip = dm.widthPixels;
		screenHeightDip = dm.heightPixels;
		
		if(screenWidthDip == 1812){
			screenWidthDip = 1920;
		}
		setAudioMute(); //设置是否静音
		
		if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE) {
			surfaceView.getHolder().setFixedSize(screenWidthDip, screenHeightDip);
		} else {
			getVerticalPixel();
		}
	}

	private void getVerticalPixel() {
		int verticalHeight;
		
		if(devType == BuddyType.TYPE_CAMERA_C01){
			verticalHeight = (screenWidthDip * 9) / 16;
		
		}else{
			verticalHeight = (screenWidthDip * 3) / 4;
		}
		surfaceView.getHolder().setFixedSize(screenWidthDip, verticalHeight);
	}

	long waitTime = 5000;  
	long touchTime = 0;
	
	public String format(int i) {
		String s = i + "";
		if (s.length() == 1) {
			s = "0" + s;
		}
		return s;
	}
	private Runnable runnable = new Runnable() {
		@Override
		public void run() {
			if(!isPlaying) {
				if (callId != null) {
					EyecatManager.getInstance().getICVSSUserInstance().equesCloseCall(callId);
				}
			}
		}
	};
	private EyecatManager.PacketListener videoCallListener= new EyecatManager.PacketListener() {
		@Override
		public String getMenthod() {
			return Method.METHOD_CALL;
		}
		@Override
		public void processPacket(JSONObject object) {
			String state = object.optString("state");
			String sid = object.optString("sid");
			if("try".equals(state)){
			}else if("ok".equals(state)) {
				if (!isExit){
					handler.postDelayed(runnable, 3000);
			}
			}else if("close".equals(state)){
				if(!isExit) {
					handler.removeCallbacks(runnable);
				}
				if(!isPlaying && !isExit){
					runOnUiThread(new Runnable() {
						@Override
						public void run() {
							startUpCall(uid);
						}
					});
				}else{
					isPlaying = false;
				}
			}
		}
	};
	private EyecatManager.PacketListener videoPlayingListener= new EyecatManager.PacketListener() {
		@Override
		public String getMenthod() {
			return Method.METHOD_VIDEOPLAY_STATUS_PLAYING;
		}

		@Override
		public void processPacket(JSONObject object) {
			isPlaying = true;
			handler.removeCallbacks(runnable);
		}
	};
	private class MyOnTouchListener implements OnTouchListener {
		
		public boolean onTouch(View v, MotionEvent event) {
			switch (event.getAction()) {
			case MotionEvent.ACTION_DOWN:
				callSpeakerSetting(true);
				break;
				
			case MotionEvent.ACTION_UP:
				callSpeakerSetting(false);	
				break;
			}
			return true;
		}
	}
	
	class MyOnClickListener implements OnClickListener {
		
		public void onClick(View view) {
			
			switch (view.getId()) {
			case R.id.btn_hangupCall:
				hangUpCall();
				break;
				
			case R.id.btn_capture:
				String path =  Environment.getExternalStorageDirectory().getAbsolutePath()
						+ "/DingDong/";
				boolean isCreateOk = createDirectory(path);
				if(isCreateOk){
					SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
					String currentTime = format.format(new Date());
					path = StringUtils.join(path, currentTime,".jpg");
					if(devType == BuddyType.TYPE_CAMERA_C01){
						height = 360;
					}
					EyecatManager.getInstance().getICVSSUserInstance().equesSnapCapture(BuddyType.TYPE_WIFI_DOOR_R22, path);
					ELog.showToastLong(EyecatVideoCallActivity.this, "截图成功");
				}else{
					ELog.showToastLong(EyecatVideoCallActivity.this, "截图失败");
				}
				break;
				
			case R.id.btn_mute:
				if(callId != null){
					isMuteFlag = !isMuteFlag;
					
					setAudioMute();//设置静音
				}
				break;

			default:
				break;
			}
		}
	}
	
	
	  public boolean onKeyDown(int keyCode, KeyEvent event) {
	    switch (keyCode) {
	    case KeyEvent.KEYCODE_VOLUME_UP:
	    	audioManager.adjustStreamVolume(
	            AudioManager.STREAM_MUSIC ,
	            AudioManager.ADJUST_RAISE,
	            AudioManager.FLAG_PLAY_SOUND | AudioManager.FLAG_SHOW_UI);
	        return true;
	    case KeyEvent.KEYCODE_VOLUME_DOWN:
	    	audioManager.adjustStreamVolume(
	            AudioManager.STREAM_MUSIC ,
	            AudioManager.ADJUST_LOWER,
	            AudioManager.FLAG_PLAY_SOUND | AudioManager.FLAG_SHOW_UI);
	        return true;
	    default:
	        break;
	    }
	    return super.onKeyDown(keyCode, event);
	}

	public void openSpeaker() {
		try {
			audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
			currVolume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC);
			if (!audioManager.isSpeakerphoneOn()) {
				audioManager.setSpeakerphoneOn(true);
				audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, currVolume,
								0);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	public void closeSpeaker(){
		try {
			if (audioManager != null) {
				if (audioManager.isSpeakerphoneOn()) {
					currVolume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC);
					audioManager.setSpeakerphoneOn(false);
					audioManager.setStreamVolume(
							AudioManager.STREAM_MUSIC, currVolume,
							0);
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	private void setAudioMute(){
		audioManager.setStreamMute(AudioManager.STREAM_MUSIC, isMuteFlag);
		
		if(isMuteFlag){
			if(callId != null){
				EyecatManager.getInstance().getICVSSUserInstance().equesAudioPlayEnable(false, callId);
				EyecatManager.getInstance().getICVSSUserInstance().equesAudioRecordEnable(false, callId);
			}
			iv_mute.setImageResource(R.drawable.icon_suspend);

			
		}else{
			audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, current, 0);
			callSpeakerSetting(false);
			iv_mute.setImageResource(R.drawable.icon_mute_on);

		}
	}
	private void startUpCall(String uid){
		isPlaying = false;
		if(hasVideo){ //是否显示视频
			callId = EyecatManager.getInstance().getICVSSUserInstance().equesOpenCall(uid, surfaceView.getHolder().getSurface()); //视频 + 语音通话
		}else{
			callId = EyecatManager.getInstance().getICVSSUserInstance().equesOpenCall(uid, surfaceView, null); //语音通话
		}
	}
	private void hangUpCall(){
		if (callId != null) {
			EyecatManager.getInstance().getICVSSUserInstance().equesCloseCall(callId);
		}
		finish();
	}

	public boolean createDirectory(String filePath) {
		if (null == filePath) {
			return false;
		}
		File file = new File(filePath);
		if (file.exists()) {
			return true;
		}
		return file.mkdirs();

	}
	
	
}
