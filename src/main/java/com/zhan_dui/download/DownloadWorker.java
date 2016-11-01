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
public class DownloadWorker implements Runnable {

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
	private DownloadWorker() {
		// just use for annotation
		// -1 is meanningless
		MISSION_ID = -1;
	}

	public DownloadWorker(MissionMonitor monitor, String fileUrl, String saveDirectory, String saveFileName, 
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

	public DownloadWorker(MissionMonitor monitor, String fileUrl, String saveDirectory, String saveFileName, 
			int startPosition, int currentPosition, int endPosition) {
		this(monitor, fileUrl, saveDirectory, saveFileName, startPosition, endPosition);
		this.currentPosition = currentPosition;
	}
	
	private File initTargetFile() {
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
		return targetFile;
	}

	@Override
	public void run() {
		File targetFile =  initTargetFile();
		
		String downloadInfo = MessageFormat.format("Download Task ID:{0} has been started! Range From {1} To {2}", Thread.currentThread().getId(), currentPosition, endPosition);
		System.out.println(downloadInfo);
		
		RandomAccessFile randomAccessFile = null;
		byte[] buf = new byte[BUFFER_SIZE];
		
		try {
			RemoteHttpFile remoteHttpFile = new RemoteHttpFile(fileUrl, currentPosition, endPosition);
			
			randomAccessFile = new RandomAccessFile(targetFile, "rw");
			randomAccessFile.seek(currentPosition);
			
			while (currentPosition < endPosition) {
				if (Thread.currentThread().isInterrupted()) {
					String interruptedInfo = MessageFormat.format("Download TaskID:{0} was interrupted, Start:{1} Current:{2} End:{3}", 
							Thread.currentThread().getId(), startPosition, currentPosition, endPosition);
					System.out.println(interruptedInfo);
					break;
				}
				int len = remoteHttpFile.read(buf);
				if (len == -1) {
					break;
				} else {
					randomAccessFile.write(buf, 0, len);
					currentPosition += len;
					downloadMonitor.down(len);
				}
			}
			remoteHttpFile.close();
			randomAccessFile.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public DownloadWorker split() {
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

			DownloadWorker newSplitedRunnable = new DownloadWorker(downloadMonitor, fileUrl, saveDirectory, saveFileName, centerPosition + 1, end);
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
