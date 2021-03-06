package cc.wulian.app.model.device.impls.controlable.dock;

import java.util.ArrayList;
import java.util.Map;

import android.content.Context;
import android.view.LayoutInflater;
import cc.wulian.app.model.device.R;
import cc.wulian.app.model.device.category.Category;
import cc.wulian.app.model.device.category.DeviceClassify;
import cc.wulian.app.model.device.impls.controlable.AbstractSwitchDevice;
import cc.wulian.app.model.device.interfaces.EditDeviceInfoView;
import cc.wulian.app.model.device.interfaces.EditDeviceInfoView.DeviceCategoryEntity;
import cc.wulian.app.model.device.utils.DeviceUtil;
import cc.wulian.ihome.wan.util.ConstUtil;

/**
 * 0:关,1:开
 */
@DeviceClassify(devTypes = {ConstUtil.DEV_TYPE_FROM_GW_DOCK}, category = Category.C_CONTROL)
public class WL_16_Dock extends AbstractSwitchDevice
{
	private static final String DATA_CTRL_STATE_OPEN_1 = "1";
	private static final String DATA_CTRL_STATE_CLOSE_0 = "0";
	private Map<String,Map<Integer,Integer>> categoryIcons = DeviceUtil.getDockCategoryDrawables();
	private int SMALL_OPEN_D = R.drawable.device_dock_open;
	private int SMALL_CLOSE_D = R.drawable.device_dock_close;

	private int BIG_OPEN_D = R.drawable.device_dock_open_big;
	private int BIG_CLOSE_D = R.drawable.device_dock_close_big;

	public WL_16_Dock( Context context, String type )
	{
		super(context, type);
	}

	@Override
	public int getOpenSmallIcon() {
		return SMALL_OPEN_D;
	}

	@Override
	public int getCloseSmallIcon() {
		return SMALL_CLOSE_D;
	}

	@Override
	public int getOpenBigPic() {
		return BIG_OPEN_D;
	}

	@Override
	public int getCloseBigPic() {
		return BIG_CLOSE_D;
	}

	@Override
	public String getOpenSendCmd() {
		return DATA_CTRL_STATE_OPEN_1;
	}

	@Override
	public String getCloseSendCmd() {
		return DATA_CTRL_STATE_CLOSE_0;
	}
	@Override
	public void setResourceByCategory() {
		Map<Integer, Integer> dockMap = categoryIcons.get(getDeviceCategory());
		if(dockMap != null && dockMap.size()>=4){
			SMALL_OPEN_D = dockMap.get(0);
			SMALL_CLOSE_D = dockMap.get(1);
	
			BIG_OPEN_D = dockMap.get(2);
			BIG_CLOSE_D = dockMap.get(3);
		}
	}

	@Override
	public EditDeviceInfoView onCreateEditDeviceInfoView(LayoutInflater inflater) {
		EditDeviceInfoView view  = super.onCreateEditDeviceInfoView(inflater);
		ArrayList<DeviceCategoryEntity> entities= new ArrayList<EditDeviceInfoView.DeviceCategoryEntity>();
		for(String key : categoryIcons.keySet()){
			DeviceCategoryEntity entity = new DeviceCategoryEntity();
			entity.setCategory(key);
			entity.setResources(categoryIcons.get(key));
			entities.add(entity);
		}
		view.setDeviceIcons(entities);
		return view;
	}

}