package com.zhan_dui.download;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Future;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class DownloadThreadPool extends ThreadPoolExecutor {

	private ConcurrentHashMap<Future<?>, DownloadRunnable> mRunnable_Monitor_HashMap = new ConcurrentHashMap<>();
	private ConcurrentHashMap<Integer, ConcurrentLinkedQueue<Future<?>>> mMissions_Monitor = new ConcurrentHashMap<>();

	public DownloadThreadPool(int corePoolSize, int maximumPoolSize,
			long keepAliveTime, TimeUnit unit,
			BlockingQueue<Runnable> workQueue, RejectedExecutionHandler handler) {
		super(corePoolSize, maximumPoolSize, keepAliveTime, unit, workQueue,
				handler);
	}

	public DownloadThreadPool(int corePoolSize, int maximumPoolSize,
			long keepAliveTime, TimeUnit unit, BlockingQueue<Runnable> workQueue) {
		super(corePoolSize, maximumPoolSize, keepAliveTime, unit, workQueue);
	}

	public DownloadThreadPool(int corePoolSize, int maximumPoolSize,
			long keepAliveTime, TimeUnit unit,
			BlockingQueue<Runnable> workQueue, ThreadFactory threadFactory) {
		super(corePoolSize, maximumPoolSize, keepAliveTime, unit, workQueue,
				threadFactory);
	}

	public DownloadThreadPool(int corePoolSize, int maximumPoolSize,
			long keepAliveTime, TimeUnit unit,
			BlockingQueue<Runnable> workQueue, ThreadFactory threadFactory,
			RejectedExecutionHandler handler) {
		super(corePoolSize, maximumPoolSize, keepAliveTime, unit, workQueue,
				threadFactory, handler);
	}

	@Override
	protected void afterExecute(Runnable r, Throwable t) {
		super.afterExecute(r, t);
		if (t == null) {
			System.out.println(Thread.currentThread().getId() + " has been succeesfully finished!");
		} else {
			System.out.println(Thread.currentThread().getId() + " errroed! Retry");
		}
		for (Future<?> future : mRunnable_Monitor_HashMap.keySet()) {
			if (!future.isDone()) {
				DownloadRunnable runnable = (DownloadRunnable) mRunnable_Monitor_HashMap.get(future);
				DownloadRunnable newRunnable = runnable.split();
				if (newRunnable != null) {
					submit(newRunnable);
					break;
				}
			}
		}
	}

	@Override
	public Future<?> submit(Runnable runnable) {
		if (!(runnable instanceof DownloadRunnable)) {
			throw new RuntimeException("runnable is not an instance of DownloadRunnable!");
		}
		
		Future<?> future = super.submit(runnable);
		
		DownloadRunnable task = (DownloadRunnable) runnable;

		if (mMissions_Monitor.containsKey(task.MISSION_ID)) {
			mMissions_Monitor.get(task.MISSION_ID).add(future);
		} else {
			ConcurrentLinkedQueue<Future<?>> queue = new ConcurrentLinkedQueue<>();
			queue.add(future);
			mMissions_Monitor.put(task.MISSION_ID, queue);
		}

		mRunnable_Monitor_HashMap.put(future, task);

		return future;
	}

	public boolean isFinished(int missionId) {
		ConcurrentLinkedQueue<Future<?>> futures = mMissions_Monitor.get(missionId);
		if (futures == null) {
			return true;
		}

		for (Future<?> future : futures) {
			if (!future.isDone()) {
				return false;
			}
		}
		return true;
	}

	public void pause(int missionId) {
		ConcurrentLinkedQueue<Future<?>> futures = mMissions_Monitor.get(missionId);
		for (Future<?> future : futures) {
			future.cancel(true);
		}
	}

	public void cancel(int missionId) {
		ConcurrentLinkedQueue<Future<?>> futures = mMissions_Monitor.remove(missionId);
		for (Future<?> future : futures) {
			mRunnable_Monitor_HashMap.remove(future);
			future.cancel(true);
		}
	}
}
