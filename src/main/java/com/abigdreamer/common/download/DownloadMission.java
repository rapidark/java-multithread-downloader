package com.abigdreamer.common.download;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.Timer;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlRootElement;

@SuppressWarnings("restriction")
@XmlRootElement(namespace = "com.abigdreamer.common.download")
@XmlAccessorType(XmlAccessType.NONE)
public class DownloadMission {

	public static final int READY = 1;
	public static final int DOWNLOADING = 2;
	public static final int PAUSED = 3;
	public static final int FINISHED = 4;

	public static int DEFAULT_THREAD_COUNT = 4;
	private static int MISSION_ID_COUNTER = 0;
	
	protected int missionId = MISSION_ID_COUNTER++;
	
	@XmlElement(name = "url")
	protected String url;
	@XmlElement(name = "folder")
	protected String folder;
	@XmlElement(name = "file-name")
	protected String fileName;
	@XmlElementWrapper(name = "download-workers")
	@XmlElement(name = "download-worker")
	private ArrayList<DownloadWorker> downloadWorkers = new ArrayList<>();

	private ArrayList<RecoveryWorkerInfo> recoveryWorkerInfos = new ArrayList<>();

	@XmlElement(name = "mission-status")
	private int missionStatus = READY;

	private String progressDir;
	private String progressFileName;
	
	@XmlElement(name = "file-size")
	private int fileSize;
	private int threadCount = DEFAULT_THREAD_COUNT;

	@XmlElement(name = "mission-monitor")
	protected MissionMonitor monitor = new MissionMonitor(this);
	@XmlElement(name = "speed-monitor")
	protected SpeedMonitor speedMonitor = new SpeedMonitor(this);

	protected StoreMonitor storeMonitor = new StoreMonitor(this);
	protected Timer speedTimer = new Timer();
	protected Timer storeTimer = new Timer();

	protected DownloadScheduler scheduler;

	@SuppressWarnings("unused")
	private DownloadMission() {
		// just for annotation
	}

	public DownloadMission(String url, String saveDirectory, String saveName) throws IOException {
		this.url = url;

		setTargetFile(saveDirectory, saveName);

		setProgessFile(this.folder, this.fileName);
	}

	public boolean setTargetFile(String saveDir, String saveName) throws IOException {
		if (saveDir.lastIndexOf(File.separator) == saveDir.length() - 1) {
			saveDir = saveDir.substring(0, saveDir.length() - 1);
		}
		this.folder = saveDir;
		File dirFile = new File(saveDir);
		if (!dirFile.exists()) {
			if (!dirFile.mkdirs()) {
				throw new RuntimeException("Error to create directory");
			}
		}

		File file = new File(dirFile.getPath() + File.separator + saveName);
		if (!file.exists()) {
			file.createNewFile();
		}
		this.fileName = saveName;
		return true;
	}

	public int getMissionId() {
		return missionId;
	}

	public String getUrl() {
		return this.url;
	}

	public void setUrl(String Url) {
		this.url = Url;
	}

	public String getSaveDirectory() {
		return this.folder;
	}

	public void setSaveDirectory(String saveDirectory) {
		this.folder = saveDirectory;
	}

	public String getSaveName() {
		return this.fileName;
	}

	public void setSaveName(String saveName) {
		this.fileName = saveName;
	}

	public void setMissionThreadCount(int threadCount) {
		this.threadCount = threadCount;
	}

	public int getMissionThreadCount() {
		return this.threadCount;
	}

	public void setDefaultThreadCount(int default_thread_count) {
		if (default_thread_count > 0) {
			DEFAULT_THREAD_COUNT = default_thread_count;
		}
	}

	public int getDefaultThreadCount() {
		return DEFAULT_THREAD_COUNT;
	}

	private ArrayList<DownloadWorker> splitDownload(int threadCount) {
		ArrayList<DownloadWorker> runnables = new ArrayList<>();
		try {
			int size = getContentLength(this.url);
			this.fileSize = size;
			int sublen = size / threadCount;
			for (int i = 0; i < threadCount; i++) {
				int startPos = sublen * i;
				int endPos = (i == threadCount - 1) ? size : (sublen * (i + 1) - 1);
				DownloadWorker worker = new DownloadWorker(this.monitor, this.url, this.folder, this.fileName, startPos, endPos);
				runnables.add(worker);
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		return runnables;
	}

	private void resumeMission() throws IOException {
		try {
			File progressFile = new File(FileUtils.getSafeDirPath(progressDir) + File.separator + progressFileName);
			if (!progressFile.exists()) {
				throw new IOException("Progress File does not exsist");
			}
			
			if(progressFile.length() == 0) {
				return;
			}

			JAXBContext context = JAXBContext.newInstance(DownloadMission.class);
			Unmarshaller unmarshaller = context.createUnmarshaller();
			DownloadMission mission = (DownloadMission) unmarshaller.unmarshal(progressFile);
			File targetSaveFile = new File(FileUtils.getSafeDirPath(mission.folder + File.separator + mission.fileName));
			if (!targetSaveFile.exists()) {
				throw new IOException("Try to continue download file , but target file does not exist");
			}
			ArrayList<RecoveryWorkerInfo> recoveryWorkerInfos = getDownloadProgress();
			recoveryWorkerInfos.clear();
			for (DownloadWorker worker : mission.downloadWorkers) {
				recoveryWorkerInfos.add(new RecoveryWorkerInfo(worker.getStartPosition(), worker.getCurrentPosition(), worker.getEndPosition()));
			}
			this.speedMonitor = new SpeedMonitor(this);
			this.storeMonitor = new StoreMonitor(this);
			System.out.println("Resume finished");
			this.downloadWorkers.clear();
		} catch (JAXBException e) {
			e.printStackTrace();
		}
	}

	public void startMission(DownloadScheduler scheduler) {
		setDownloadStatus(DOWNLOADING);
		try {
			resumeMission();
		} catch (IOException e) {
			e.printStackTrace();
		}
		this.scheduler = scheduler;
		if (!this.recoveryWorkerInfos.isEmpty()) {
			for (RecoveryWorkerInfo workerInfo : this.recoveryWorkerInfos) {
				if (!workerInfo.isFinished()) {
					DownloadWorker worker = new DownloadWorker(this.monitor, this.url, this.folder, this.fileName, 
							workerInfo.getStartPosition(), workerInfo.getCurrentPosition(), workerInfo.getEndPosition());
					this.downloadWorkers.add(worker);
					scheduler.push(worker);
				}
			}
		} else {
			for (DownloadWorker runnable : splitDownload(this.threadCount)) {
				this.downloadWorkers.add(runnable);
				scheduler.push(runnable);
			}
		}
		this.speedTimer.scheduleAtFixedRate(this.speedMonitor, 0, 1000);// 1s
		this.storeTimer.scheduleAtFixedRate(this.storeMonitor, 0, 5000);// 5s
	}

	public boolean isFinished() {
		return missionStatus == FINISHED;
	}

	public void addPartedMission(DownloadWorker runnable) {
		this.downloadWorkers.add(runnable);
	}

	private int getContentLength(String fileUrl) throws IOException {
		URL url = new URL(fileUrl);
		URLConnection connection = url.openConnection();
		return connection.getContentLength();
	}

	private boolean setProgessFile(String dir, String filename) throws IOException {
		if (dir.lastIndexOf(File.separator) == dir.length() - 1) {
			dir = dir.substring(0, dir.length() - 1);
		}
		File dirFile = new File(dir);
		if (!dirFile.exists()) {
			if (!dirFile.mkdirs()) {
				throw new RuntimeException("Error to create directory");
			}
		}
		this.progressDir = dirFile.getPath();
		File file = new File(dirFile.getPath() + File.separator + filename + ".tmp");
		if (file.exists() == false) {
			file.createNewFile();
		}
		this.progressFileName = file.getName();
		return true;
	}

	public File getProgressFile() {
		return new File(this.progressDir + File.separator + this.progressFileName);
	}

	public File getDownloadFile() {
		return new File(this.folder + File.separator + this.fileName);
	}

	public String getProgressDir() {
		return this.progressDir;
	}

	public String getProgressFileName() {
		return this.progressFileName;
	}

	public int getDownloadedSize() {
		return this.monitor.getDownloadedSize();
	}

	public String getReadableSize() {
		return DownloadUtils.getReadableSize(getDownloadedSize());
	}

	public int getSpeed() {
		return this.speedMonitor.getSpeed();
	}

	public String getReadableSpeed() {
		return DownloadUtils.getReadableSpeed(getSpeed());
	}

	public int getMaxSpeed() {
		return this.speedMonitor.getMaxSpeed();
	}

	public String getReadableMaxSpeed() {
		return DownloadUtils.getReadableSpeed(getMaxSpeed());
	}

	public int getAverageSpeed() {
		return this.speedMonitor.getAverageSpeed();
	}

	public String getReadableAverageSpeed() {
		return DownloadUtils.getReadableSpeed(this.speedMonitor.getAverageSpeed());
	}

	public int getTimePassed() {
		return this.speedMonitor.getDownloadedTime();
	}

	public int getActiveTheadCount() {
		return this.scheduler.getActiveCount();
	}

	public int getFileSize() {
		return this.fileSize;
	}

	public void pause() {
		setDownloadStatus(PAUSED);
		storeProgress();
		this.scheduler.pause(this.missionId);
	}

	void setDownloadStatus(int status) {
		if (status == FINISHED) {
			this.speedTimer.cancel();
		}
		this.missionStatus = status;
	}

	public void storeProgress() {
		try {
			JAXBContext context = JAXBContext.newInstance(DownloadMission.class);
			Marshaller m = context.createMarshaller();
			m.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);
			m.marshal(this, getProgressFile());
		} catch (JAXBException e) {
			e.printStackTrace();
		}
	}

	public static DownloadMission recoverMissionFromProgressFile(String progressDirectory, String progressFileName)
			throws IOException {
		try {
			File progressFile = new File(FileUtils.getSafeDirPath(progressDirectory) + File.separator + progressFileName);
			if (progressFile.exists() == false) {
				throw new IOException("Progress File does not exsist");
			}

			JAXBContext context = JAXBContext.newInstance(DownloadMission.class);
			Unmarshaller unmarshaller = context.createUnmarshaller();
			DownloadMission mission = (DownloadMission) unmarshaller.unmarshal(progressFile);
			File targetSaveFile = new File(FileUtils.getSafeDirPath(mission.folder + File.separator + mission.fileName));
			if (!targetSaveFile.exists()) {
				throw new IOException("Try to continue download file , but target file does not exist");
			}
			mission.setProgessFile(progressDirectory, progressFileName);
			mission.missionId = MISSION_ID_COUNTER++;
			ArrayList<RecoveryWorkerInfo> recoveryRunnableInfos = mission.getDownloadProgress();
			for (DownloadWorker runnable : mission.downloadWorkers) {
				recoveryRunnableInfos.add(new RecoveryWorkerInfo(runnable.getStartPosition(), runnable.getCurrentPosition(), runnable.getEndPosition()));
			}
			mission.downloadWorkers.clear();
			return mission;
		} catch (JAXBException e) {
			e.printStackTrace();
			return null;
		}
	}

	private void deleteProgressFile() {
		getProgressFile().delete();
	}

	public ArrayList<RecoveryWorkerInfo> getDownloadProgress() {
		return this.recoveryWorkerInfos;
	}

	public void cancel() {
		deleteProgressFile();
		this.speedTimer.cancel();
		this.downloadWorkers.clear();
		this.scheduler.cancel(missionId);
	}
}
