package com.zhan_dui.download;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.net.URL;
import java.net.URLConnection;
import java.text.MessageFormat;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

@SuppressWarnings("restriction")
@XmlRootElement(name = "Downloading")
@XmlAccessorType(XmlAccessType.NONE)
public class DownloadRunnable implements Runnable {

	private static final int BUFFER_SIZE = 1024;

	private static int counter = 0;
	public final int MISSION_ID;
	public final int ID = counter++;
	
	private String fileUrl;
	private String saveDirectory;
	private String saveFileName;
	
	@XmlElement(name = "StartPosition")
	private int startPosition;
	@XmlElement(name = "EndPosition")
	private int endPosition;
	@XmlElement(name = "CurrentPosition")
	private int currentPosition;

	private MissionMonitor downloadMonitor;

	@SuppressWarnings("unused")
	private DownloadRunnable() {
		// just use for annotation
		// -1 is meanningless
		MISSION_ID = -1;
	}

	public DownloadRunnable(MissionMonitor monitor, String fileUrl, String saveDirectory, String saveFileName, 
			int startPosition, int endPosition) {
		super();
		this.fileUrl = fileUrl;
		this.saveDirectory = saveDirectory;
		this.saveFileName = saveFileName;
		this.startPosition = startPosition;
		this.endPosition = endPosition;
		this.downloadMonitor = monitor;
		this.currentPosition = this.startPosition;
		MISSION_ID = monitor.hostMission.missionId;
	}

	public DownloadRunnable(MissionMonitor monitor, String fileUrl, String saveDirectory, String saveFileName, 
			int startPosition, int currentPosition, int endPosition) {
		this(monitor, fileUrl, saveDirectory, saveFileName, startPosition, endPosition);
		this.currentPosition = currentPosition;
	}

	@Override
	public void run() {
		File targetFile;
		synchronized (this) {
			File dir = new File(saveDirectory + File.pathSeparator);
			if (!dir.exists()) {
				dir.mkdirs();
			}
			targetFile = new File(saveDirectory + File.separator + saveFileName);
			if (!targetFile.exists()) {
				try {
					targetFile.createNewFile();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}

		String downloadInfo = MessageFormat.format("Download Task ID:{0} has been started! Range From {1} To {2}", Thread.currentThread().getId(), currentPosition, endPosition);
		System.out.println(downloadInfo);
		
		BufferedInputStream bufferedInputStream = null;
		RandomAccessFile randomAccessFile = null;
		byte[] buf = new byte[BUFFER_SIZE];
		URLConnection urlConnection = null;
		try {
			URL url = new URL(fileUrl);
			urlConnection = url.openConnection();
			urlConnection.setRequestProperty("Range", "bytes=" + currentPosition + "-" + endPosition);
			randomAccessFile = new RandomAccessFile(targetFile, "rw");
			randomAccessFile.seek(currentPosition);
			bufferedInputStream = new BufferedInputStream(urlConnection.getInputStream());
			while (currentPosition < endPosition) {
				if (Thread.currentThread().isInterrupted()) {
					String interruptedInfo = MessageFormat.format("Download TaskID:{0} was interrupted, Start:{1} Current:{2} End:{3}", 
							Thread.currentThread().getId(), startPosition, currentPosition, endPosition);
					System.out.println(interruptedInfo);
					break;
				}
				int len = bufferedInputStream.read(buf, 0, BUFFER_SIZE);
				if (len == -1) {
					break;
				} else {
					randomAccessFile.write(buf, 0, len);
					currentPosition += len;
					downloadMonitor.down(len);
				}
			}
			bufferedInputStream.close();
			randomAccessFile.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public DownloadRunnable split() {
		int end = endPosition;
		int remaining = endPosition - currentPosition;
		int remainingCenter = remaining / 2;
		System.out.print(MessageFormat.format("CurrentPosition:{0} EndPosition:{1}, Rmaining:{2} ", 
				currentPosition, endPosition, remaining));
		int minSize = 1 * 1024 * 1024;// 1M
		if (remainingCenter > minSize) {
			int centerPosition = remainingCenter + currentPosition;
			System.out.print(" Center position:" + centerPosition);
			endPosition = centerPosition;

			DownloadRunnable newSplitedRunnable = new DownloadRunnable(downloadMonitor, fileUrl, saveDirectory, saveFileName, centerPosition + 1, end);
			downloadMonitor.hostMission.addPartedMission(newSplitedRunnable);
			return newSplitedRunnable;
		} else {
			System.out.println(toString() + " can not be splited ,less than 1M");
			return null;
		}
	}
	
	public boolean isFinished() {
		return currentPosition >= endPosition;
	}

	public int getCurrentPosition() {
		return currentPosition;
	}

	public int getEndPosition() {
		return endPosition;
	}

	public int getStartPosition() {
		return startPosition;
	}

}
