package com.zhan_dui.download;

import java.io.IOException;
import java.util.Hashtable;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.TimeUnit;

public class DownloadEnginer {

	private static DownloadEnginer instance = new DownloadEnginer();

	private static DownloadThreadPool threadPool;

	public static final int DEFAULT_MISSION_THREAD_COUNT = 4;
	public static final int DEFAULT_CORE_POOL_SIZE = 10;

	public static final int DEFAULT_MAX_POOL_SIZE = Integer.MAX_VALUE;
	public static final int DEFAULT_KEEP_ALIVE_TIME = 0;

	private static int ID = 0;
	
	public static DownloadEnginer getInstance() {
		if (threadPool.isShutdown()) {
			threadPool = new DownloadThreadPool(DEFAULT_CORE_POOL_SIZE,
					DEFAULT_MAX_POOL_SIZE, DEFAULT_KEEP_ALIVE_TIME,
					TimeUnit.SECONDS, new LinkedBlockingDeque<Runnable>());
		}
		return instance;
	}
	
	private Hashtable<Integer, DownloadMission> missions = new Hashtable<>();

	private DownloadEnginer() {
		threadPool = new DownloadThreadPool(DEFAULT_CORE_POOL_SIZE,
				DEFAULT_MAX_POOL_SIZE, DEFAULT_KEEP_ALIVE_TIME,
				TimeUnit.SECONDS, new LinkedBlockingDeque<Runnable>());
	}

	public void setMaxThreadCount(int maxCount) {
		if (maxCount > 0) {
			threadPool.setCorePoolSize(maxCount);
		}
	}

	public DownloadMission addMission(String url, String saveDirectory, String saveName) throws IOException {
		DownloadMission downloadMission = new DownloadMission(url, saveDirectory, saveName);
		addMission(downloadMission);
		return downloadMission;
	}

	public void addMission(DownloadMission downloadTask) {
		missions.put(ID++, downloadTask);
	}

	public DownloadMission getMission(int missionID) {
		return missions.get(missionID);
	}

	public void start() {
		for (DownloadMission mission : missions.values()) {
			mission.startMission(threadPool);
		}
	}

	public boolean isAllMissionsFinished() {
		for (Integer missionId : missions.keySet()) {
			if (!isMissionFinished(missionId)) {
				return false;
			}
		}
		return true;
	}

	public boolean isMissionFinished(int missionId) {
		DownloadMission mission = missions.get(missionId);
		return mission.isFinished();
	}

	public void pauseAllMissions() {
		for (Integer missionID : missions.keySet()) {
			pauseMission(missionID);
		}
	}

	public void pauseMission(int missionId) {
		if (missions.contains(missionId)) {
			DownloadMission mission = missions.get(missionId);
			mission.pause();
		}
	}

	public void cancelAllMissions() {
		for (Integer missionId : missions.keySet()) {
			cancelMission(missionId);
		}
	}

	public void cancelMission(int missionId) {
		if (missions.contains(missionId)) {
			DownloadMission mission = missions.remove(missionId);
			mission.cancel();
		}
	}

	public void shutdownSafely() {
		for (Integer missionId : missions.keySet()) {
			missions.get(missionId).pause();
		}
		threadPool.shutdown();
	}

	public int getTotalDownloadedSize() {
		int size = 0;
		for (DownloadMission mission : missions.values()) {
			size += mission.getDownloadedSize();
		}
		return size;
	}

	public String getReadableDownloadSize() {
		return DownloadUtils.getReadableSize(getTotalDownloadedSize());
	}

	public int getTotalSpeed() {
		int speed = 0;
		for (DownloadMission mission : missions.values()) {
			speed += mission.getSpeed();
		}
		return speed;
	}

	public String getReadableTotalSpeed() {
		return DownloadUtils.getReadableSpeed(getTotalSpeed());
	}

	public void shutdDownloadRudely() {
		threadPool.shutdownNow();
	}
}
