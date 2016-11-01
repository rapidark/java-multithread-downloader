package com.zhan_dui.download;

/**
 *  
 * @author Darkness
 * @date 2016年11月1日 下午6:48:26
 * @version V1.0
 */
public class RecoveryRunnableInfo {

	private int startPosition;
	private int endPosition;
	private int currentPosition;
	private boolean isFinished = false;

	public RecoveryRunnableInfo(int start, int current, int end) {
		if (end > start && current > start) {
			startPosition = start;
			endPosition = end;
			currentPosition = current;
		} else {
			throw new RuntimeException("position logical error");
		}
		if (currentPosition >= endPosition) {
			isFinished = true;
		}
	}

	public int getStartPosition() {
		return startPosition;
	}

	public int getEndPosition() {
		return endPosition;
	}

	public int getCurrentPosition() {
		return currentPosition;
	}

	public boolean isFinished() {
		return isFinished;
	}
}
