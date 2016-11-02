package com.abigdreamer.common.download;

import java.util.TimerTask;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

/**
 * 
 * @author Darkness
 * @date 2016年11月1日 下午6:49:22
 * @version V1.0
 */
@SuppressWarnings("restriction")
@XmlRootElement
@XmlAccessorType(XmlAccessType.NONE)
class SpeedMonitor extends TimerTask {

	@XmlElement(name = "last-second-size")
	private int lastSecondSize = 0;
	@XmlElement(name = "current-second-size")
	private int currentSecondSize = 0;
	@XmlElement(name = "speed")
	private int speed;
	@XmlElement(name = "max-speed")
	private int maxSpeed;
	@XmlElement(name = "average-speed")
	private int averageSpeed;
	@XmlElement(name = "seconds")
	private int seconds;

	private DownloadMission mission;

	@SuppressWarnings("unused")
	private SpeedMonitor() {
		// never use , for annotation
	}

	public int getMaxSpeed() {
		return maxSpeed;
	}

	public SpeedMonitor(DownloadMission missionBelongTo) {
		this.mission = missionBelongTo;
	}

	@Override
	public void run() {
		this.seconds++;
		this.currentSecondSize = this.mission.getDownloadedSize();
		this.speed = this.currentSecondSize - this.lastSecondSize;
		this.lastSecondSize = this.currentSecondSize;
		if (this.speed > this.maxSpeed) {
			this.maxSpeed = this.speed;
		}

		this.averageSpeed = this.currentSecondSize / this.seconds;
	}

	public int getDownloadedTime() {
		return this.seconds;
	}

	public int getSpeed() {
		return this.speed;
	}

	public int getAverageSpeed() {
		return this.averageSpeed;
	}
}
