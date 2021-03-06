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
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;
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
import cc.wulian.smarthomev5.utils.ScreenSwitchUtils;

public class EyecatVideoCallActivity extends Activity {
	private SurfaceView surfaceView;
	private String callId;
	private int currVolume;
	private int current;
	private boolean isMuteFlag;
	private boolean hasVideo;
	private String uid;
	private boolean isPlaying = false;
	private boolean isExit = false;
	private AudioManager audioManager;
	private FrameLayout operationFrameLayout;
	private FrameLayout btnCapture, btnMute, btnHangupCall;
	private ImageView btnSoundSwitch;
	private Handler handler = new Handler(Looper.getMainLooper());
	private ImageView iv_mute,levelone,leveltwo,levelthree,levelfour,levelfive;
	private TextView battery_status_title;
	private int screenWidthDip;
	private int screenHeightDip;
	private String bid;
	private ScreenSwitchUtils instance;
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		Log.i("eyecat:","onCreate");
		instance = ScreenSwitchUtils.init(this.getApplicationContext());
		getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON | WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED);
		audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
		current = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC);
		audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, current, 0);
		
		currVolume = current;
		hasVideo = getIntent().getBooleanExtra(Method.ATTR_CALL_HASVIDEO, false);
		bid = getIntent().getStringExtra("bid");
		initUI();

	}
	public void onBackPressed() {
		hangUpCall();
	}

	@Override
	protected void onStart() {
		super.onStart();
		Log.i("eyecat:","onStart");
		instance.start(this);
	}
	@Override
	protected void onStop() {
		super.onStop();
		Log.i("eyecat:","onStop");
		instance.stop();
	}
	@Override
	public void onConfigurationChanged(Configuration newConfig) {
		super.onConfigurationChanged(newConfig);
		handler.post(new Runnable() {
			@Override
			public void run() {
				initUI();
				setVideoSize();
			}
		});
		handler.postDelayed(new Runnable() {
			@Override
			public void run() {
				startUpCall(uid);
				setAudioMute(); //设置是否静音
			}
		},2000);
	}
	@Override
	protected void onResume() {
		super.onResume();
		Log.i("eyecat:","onResume");
		EyecatManager.getInstance().addPacketListener(videoCallListener);
		EyecatManager.getInstance().addPacketListener(videoPlayingListener);
		EyecatManager.getInstance().addPacketListener(batteryStatusListener);
		EyecatManager.getInstance().addPacketListener(deviceDetailListener);
		EyecatManager.getInstance().login();
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
		Log.i("eyecat:","onPause");
		isExit = true;
		EyecatManager.getInstance().removePacketListener(videoCallListener);
		EyecatManager.getInstance().removePacketListener(videoPlayingListener);
		EyecatManager.getInstance().removePacketListener(batteryStatusListener);
		EyecatManager.getInstance().removePacketListener(deviceDetailListener);
		handler.removeCallbacks(runnable);
		finish();
	}
	protected void onDestroy() {
		super.onDestroy();
		Log.i("eyecat:","onDestroy");
		audioManager.setStreamMute(AudioManager.STREAM_MUSIC, false);
		audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, current, 0);
		closeSpeaker();
	}
	private void initUI() {
		// 清空
		if(surfaceView != null){
			surfaceView.setVisibility(View.GONE);
			surfaceView = null;
		}
		if (instance.isPortrait()) {
			Log.i("eyecat:","竖屏,uid:"+uid);
			setContentView(R.layout.eyecat_activity_videomain);
		}else{
			Log.i("eyecat:","横屏，uid:"+uid);
			setContentView(R.layout.eyecat_activity_videomain_land);
		}
		surfaceView = (SurfaceView) findViewById(R.id.surface_view);
		btnCapture = (FrameLayout) findViewById(R.id.btn_capture);
		btnCapture.setOnClickListener(new MyOnClickListener());

		btnMute = (FrameLayout) findViewById(R.id.btn_mute);
		btnMute.setOnClickListener(new MyOnClickListener());

		btnHangupCall = (FrameLayout) findViewById(R.id.btn_hangupCall);
		btnHangupCall.setOnClickListener(new MyOnClickListener());

		btnSoundSwitch = (ImageView) findViewById(R.id.btn_soundSwitch);
		btnSoundSwitch.setOnTouchListener(new MyOnTouchListener());
		operationFrameLayout = (FrameLayout) findViewById(R.id.operations_framelayout);
		iv_mute = (ImageView) findViewById(R.id.iv_mute);
		battery_status_title = (TextView) findViewById(R.id.battery_status_title);
		levelone = (ImageView) findViewById(R.id.level_one);
		leveltwo = (ImageView) findViewById(R.id.level_two);
		levelthree = (ImageView) findViewById(R.id.level_three);
		levelfour = (ImageView) findViewById(R.id.level_four);
		levelfive = (ImageView) findViewById(R.id.level_five);
		surfaceView.getHolder().addCallback(new SurfaceHolder.Callback() {

			public void surfaceChanged(SurfaceHolder holder, int arg1,
									   int arg2, int arg3) {
			}

			public void surfaceCreated(SurfaceHolder holder) {
				Log.i("eyecat:","surfaceCreated");

			}

			public void surfaceDestroyed(SurfaceHolder holder) {
				Log.i("eyecat:","surfaceDestroyed");
				hangUpCallNotCloseActivity();
			}
		});
		if (instance.isPortrait()) {
			operationFrameLayout.setVisibility(View.VISIBLE);
		}else{
			operationFrameLayout.setVisibility(View.GONE);
			surfaceView.setOnTouchListener(new OnTouchListener() {
				@Override
				public boolean onTouch(View v, MotionEvent event) {
					if (event.getAction() == MotionEvent.ACTION_DOWN) {
						showOPerations(5000);
						return true;
					}
					return false;
				}
			});
		}
		EyecatManager.getInstance().getICVSSUserInstance().equesGetDeviceInfo(bid);
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
//					Toast.makeText(EyecatVideoCallActivity.this,"开始打开回话:"+uid+",hasVideo:"+hasVideo,Toast.LENGTH_SHORT).show();
					boolean bo = audioManager.isWiredHeadsetOn();
					if(!bo){
						openSpeaker();
					}
					setVideoSize();
					startUpCall(uid);
					setAudioMute(); //设置是否静音

				}
			});
		}
	}
	private void callSpeakerSetting(boolean f) {
		if (f) {
			btnSoundSwitch.setImageResource(R.drawable.icon_talkback_green);
			if (callId != null) {
				EyecatManager.getInstance().getICVSSUserInstance().equesAudioRecordEnable(true, callId);
				EyecatManager.getInstance().getICVSSUserInstance().equesAudioPlayEnable(false, callId);
			}
			closeSpeaker();
		} else {
			btnSoundSwitch.setImageResource(R.drawable.icon_talkback);
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
		if (instance.isPortrait()) {
			screenHeightDip = screenHeightDip/2;
			Log.i("eyecat:",screenWidthDip+"*"+screenHeightDip +";sur:"+surfaceView.getMeasuredWidth()+"*"+surfaceView.getMeasuredHeight());
			surfaceView.getHolder().setFixedSize(screenWidthDip, screenHeightDip);
		}else{
			Log.i("eyecat:",screenWidthDip+"*"+screenHeightDip+";sur:"+surfaceView.getMeasuredWidth()+"*"+surfaceView.getMeasuredHeight());
			surfaceView.getHolder().setFixedSize(screenWidthDip,screenHeightDip);
		}
	}
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
	private Runnable runnableExecuteDelay = new Runnable() {
		@Override
		public void run() {
			if(!instance.isPortrait()){
				operationFrameLayout.setVisibility(View.GONE);
			}
		}
	};
	private void showOPerations(long time){
		if(!instance.isPortrait()) {
			if(operationFrameLayout.getVisibility() == View.VISIBLE){
				operationFrameLayout.setVisibility(View.GONE);
			}else {
				operationFrameLayout.setVisibility(View.VISIBLE);
				handler.removeCallbacks(runnableExecuteDelay);
				handler.postDelayed(runnableExecuteDelay, time);
			}
		}
	}
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
	private EyecatManager.PacketListener deviceDetailListener = new EyecatManager.PacketListener() {
		@Override
		public String getMenthod() {
			return Method.METHOD_DEVICEINFO_RESULT;
		}

		@Override
		public void processPacket(final JSONObject object) {
			final int status = object.optInt("battery_status");
			final int level = object.optInt("battery_level");
			showBattery(status,level);
		}
	};
	private EyecatManager.PacketListener batteryStatusListener = new EyecatManager.PacketListener() {
		@Override
		public String getMenthod() {
			return Method.METHOD_BATTERY_STATUS;
		}

		@Override
		public void processPacket(JSONObject object) {
			final int status = object.optInt(Method.ATTR_STATUS);
			final int level = object.optInt(Method.ATTR_LEVEL);
			showBattery(status,level);

		}


	};
	private void showBattery(final int  status,final int level) {

		runOnUiThread(new Runnable() {
			@Override
			public void run() {
				if(status == 2){
					battery_status_title.setText("充电中");
				}else {
					battery_status_title.setText(level+"%");
				}
				if(level>=80){
					levelone.setVisibility(View.VISIBLE);
					leveltwo.setVisibility(View.VISIBLE);
					levelthree.setVisibility(View.VISIBLE);
					levelfour.setVisibility(View.VISIBLE);
					levelfive.setVisibility(View.VISIBLE);
					levelone.setImageResource(R.drawable.barry_green);
				}else if(level<80&&level>=60){
					levelone.setVisibility(View.VISIBLE);
					leveltwo.setVisibility(View.VISIBLE);
					levelthree.setVisibility(View.VISIBLE);
					levelfour.setVisibility(View.VISIBLE);
					levelfive.setVisibility(View.INVISIBLE);
					levelone.setImageResource(R.drawable.barry_green);
				}else if(level<60&&level>=40){
					levelone.setVisibility(View.VISIBLE);
					leveltwo.setVisibility(View.VISIBLE);
					levelthree.setVisibility(View.VISIBLE);
					levelfour.setVisibility(View.INVISIBLE);
					levelfive.setVisibility(View.INVISIBLE);
					levelone.setImageResource(R.drawable.barry_green);
				}else if(level<40&&level>=20){
					levelone.setVisibility(View.VISIBLE);
					leveltwo.setVisibility(View.VISIBLE);
					levelthree.setVisibility(View.INVISIBLE);
					levelfour.setVisibility(View.INVISIBLE);
					levelfive.setVisibility(View.INVISIBLE);
					levelone.setImageResource(R.drawable.barry_green);
				}else if(level<20){
					levelone.setVisibility(View.VISIBLE);
					leveltwo.setVisibility(View.INVISIBLE);
					levelthree.setVisibility(View.INVISIBLE);
					levelfour.setVisibility(View.INVISIBLE);
					levelfive.setVisibility(View.INVISIBLE);
					levelone.setImageResource(R.drawable.barry_red);
				}
			}
		});
	}
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
			iv_mute.setImageResource(R.drawable.eyecat_silence);

			
		}else{
			audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, current, 0);
			callSpeakerSetting(false);
			iv_mute.setImageResource(R.drawable.icon_mute_on);

		}
	}
	private void startUpCall(String uid){
		if(isPlaying){
			return ;
		}
		if(hasVideo){ //是否显示视频
			callId = EyecatManager.getInstance().getICVSSUserInstance().equesOpenCall(uid, surfaceView.getHolder().getSurface()); //视频 + 语音通话
		}else{
			callId = EyecatManager.getInstance().getICVSSUserInstance().equesOpenCall(uid, surfaceView, null); //语音通话
		}
	}
	private void hangUpCallNotCloseActivity(){
		if (callId != null) {
			EyecatManager.getInstance().getICVSSUserInstance().equesCloseCall(callId);
			isPlaying = false;
			callId = null;
		}
	}
	private void hangUpCall(){
		if (callId != null) {
			EyecatManager.getInstance().getICVSSUserInstance().equesCloseCall(callId);
			callId = null;
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
