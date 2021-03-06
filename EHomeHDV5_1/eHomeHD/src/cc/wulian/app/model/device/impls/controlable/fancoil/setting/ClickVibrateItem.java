package cc.wulian.app.model.device.impls.controlable.fancoil.setting;

import android.content.Context;
import android.graphics.Color;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.LinearLayout;
import cc.wulian.app.model.device.impls.controlable.fancoil.FanCoilUtil;
import cc.wulian.ihome.wan.util.StringUtil;
import cc.wulian.smarthomev5.R;
import cc.wulian.smarthomev5.fragment.setting.AbstractSettingItem;
import cc.wulian.smarthomev5.tools.AccountManager;
import cc.wulian.smarthomev5.tools.SendMessage;

public class ClickVibrateItem extends AbstractSettingItem{
	
	private String mGwId;
	private String mDevId;
	private String mEp;
	private String mEpType;
	private boolean isVibrateOpen = true;
	
	public ClickVibrateItem(Context context) {
		super(context, R.drawable.icon_gateway_id, context.getResources().getString(R.string.AP_vibrate));
	}

	@Override
	public void initSystemState() {
		super.initSystemState();
		setVibrate();
	}
	
	public void setVibrateData(String gwId,String devId,String ep,String epType){
		mGwId = gwId;
		mDevId = devId;
		mEp = ep;
		mEpType = epType;
	}
	
	//设置 震动开关状态
	public void setVibrateType(String vibrateData){
		if(StringUtil.equals(vibrateData, FanCoilUtil.STATE_ON)){
			isVibrateOpen = true;
		}else if(StringUtil.equals(vibrateData, FanCoilUtil.STATE_OFF)){
			isVibrateOpen = false;
		}
		setVibrateImage(isVibrateOpen);
	}

	private void setVibrateImage(boolean isOpen){
		if(isOpen){
			infoImageView.setBackgroundResource(FanCoilUtil.DRAWABLE_BUTTON_ON);
		}else{
			infoImageView.setBackgroundResource(FanCoilUtil.DRAWABLE_BUTTON_OFF);
		}
	}

	public void setVibrate() {
		nameTextView.setTextColor(Color.parseColor("#3e3e3e"));
		iconImageView.setVisibility(View.GONE);
		LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
		params.setMargins(0, 0, 0, 0);
		nameTextView.setLayoutParams(params);
		infoImageView.setVisibility(View.VISIBLE);
		infoImageView.setLayoutParams(params);
		infoImageView.setImageDrawable(null);
		setVibrateImage(isVibrateOpen);
		
		infoImageView.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View view) {
				if(isVibrateOpen){
					SendMessage.sendControlDevMsg(mGwId, mDevId, mEp, mEpType, FanCoilUtil.VIBRATE_CMD_OFF);
				}else{
					SendMessage.sendControlDevMsg(mGwId, mDevId, mEp, mEpType, FanCoilUtil.VIBRATE_CMD_ON);
				}
				
			}
		});;
	}

	@Override
	public void doSomethingAboutSystem() {
		
	}
}
