package com.zhan_dui.download;

import java.util.concurrent.atomic.AtomicInteger;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;

/**
 * 
 * @author Darkness
 * @date 2016年11月1日 下午6:46:59
 * @version V1.0
 */
@SuppressWarnings("restriction")
@XmlRootElement(name = "MissionMonitor")
@XmlAccessorType(XmlAccessType.NONE)
public class MissionMonitor {

	public final DownloadMission hostMission;

	@XmlElement(name = "DownloadedSize")
	@XmlJavaTypeAdapter(AtomicIntegerAdapter.class)
	private AtomicInteger downloadedSize = new AtomicInteger();

	public MissionMonitor() {
		hostMission = null;
	}

	public MissionMonitor(DownloadMission monitorBelongsTo) {
		hostMission = monitorBelongsTo;
	}

	public void down(int size) {
		downloadedSize.addAndGet(size);
		if (downloadedSize.intValue() == hostMission.getFileSize()) {
			hostMission.setDownloadStatus(DownloadMission.FINISHED);
		}
	}

	public int getDownloadedSize() {
		return downloadedSize.get();
	}

}
