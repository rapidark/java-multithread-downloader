package com.abigdreamer.common.download;

/**
 *  
 * @author Darkness
 * @date 2016年11月1日 下午6:48:26
 * @version V1.0
 */
public class RecoveryWorkerInfo {

	private int startPosition;
	private int endPosition;
	private int currentPosition;

	public RecoveryWorkerInfo(int start, int current, int end) {
		if (end > start && current > start) {
			startPosition = start;
			endPosition = end;
			currentPosition = current;
		} else {
			throw new RuntimeException("position logical error");
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
		return currentPosition >= endPosition;
	}
}
