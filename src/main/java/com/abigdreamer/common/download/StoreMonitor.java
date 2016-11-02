package com.abigdreamer.common.download;

import java.util.TimerTask;

/**
 *  
 * @author Darkness
 * @date 2016年11月1日 下午6:50:47
 * @version V1.0
 */
public class StoreMonitor extends TimerTask {
	
	DownloadMission mission;
	
	public StoreMonitor(DownloadMission mission) {
		this.mission = mission;
	}
	
	@Override
	public void run() {
		mission.storeProgress();
	}
}
