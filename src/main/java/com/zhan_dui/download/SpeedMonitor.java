package com.zhan_dui.download;

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

	@XmlElement(name = "LastSecondSize")
	private int lastSecondSize = 0;
	@XmlElement(name = "CurrentSecondSize")
	private int currentSecondSize = 0;
	@XmlElement(name = "Speed")
	private int speed;
	@XmlElement(name = "MaxSpeed")
	private int maxSpeed;
	@XmlElement(name = "AverageSpeed")
	private int averageSpeed;
	@XmlElement(name = "TimePassed")
	private int counter;

	private DownloadMission mHostMission;

	@SuppressWarnings("unused")
	private SpeedMonitor() {
		// never use , for annotation
	}

	public int getMaxSpeed() {
		return maxSpeed;
	}

	public SpeedMonitor(DownloadMission missionBelongTo) {
		mHostMission = missionBelongTo;
	}

	@Override
	public void run() {
		this.counter++;
		this.currentSecondSize = this.mHostMission.getDownloadedSize();
		this.speed = this.currentSecondSize - this.lastSecondSize;
		this.lastSecondSize = this.currentSecondSize;
		if (this.speed > this.maxSpeed) {
			this.maxSpeed = this.speed;
		}

		this.averageSpeed = this.currentSecondSize / this.counter;
	}

	public int getDownloadedTime() {
		return this.counter;
	}

	public int getSpeed() {
		return this.speed;
	}

	public int getAverageSpeed() {
		return this.averageSpeed;
	}
}
