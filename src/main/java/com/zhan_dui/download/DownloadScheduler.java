package com.zhan_dui.download;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Future;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class DownloadScheduler {

	private ThreadPoolExecutor executor;
	
	private ConcurrentHashMap<Future<?>, DownloadWorker> runnableMonitorMap = new ConcurrentHashMap<>();
	private ConcurrentHashMap<Integer, ConcurrentLinkedQueue<Future<?>>> missionMonitors = new ConcurrentHashMap<>();

	public DownloadScheduler(int corePoolSize, int maximumPoolSize,
			long keepAliveTime, TimeUnit unit,
			BlockingQueue<Runnable> workQueue, RejectedExecutionHandler handler) {
		executor = new ThreadPoolExecutor(corePoolSize, maximumPoolSize, keepAliveTime, unit, workQueue, handler);
	}

	public DownloadScheduler(int corePoolSize, int maximumPoolSize,
			long keepAliveTime, TimeUnit unit, BlockingQueue<Runnable> workQueue) {
		executor = new ThreadPoolExecutor(corePoolSize, maximumPoolSize, keepAliveTime, unit, workQueue){
			@Override
			protected void afterExecute(Runnable r, Throwable t) {
				super.afterExecute(r, t);
				onWorkerFinish(r, t);
			}
		};
	}

	public DownloadScheduler(int corePoolSize, int maximumPoolSize,
			long keepAliveTime, TimeUnit unit,
			BlockingQueue<Runnable> workQueue, ThreadFactory threadFactory) {
		executor = new ThreadPoolExecutor(corePoolSize, maximumPoolSize, keepAliveTime, unit, workQueue, threadFactory){
			@Override
			protected void afterExecute(Runnable r, Throwable t) {
				super.afterExecute(r, t);
				onWorkerFinish(r, t);
			}
		};
	}

	public DownloadScheduler(int corePoolSize, int maximumPoolSize,
			long keepAliveTime, TimeUnit unit,
			BlockingQueue<Runnable> workQueue, ThreadFactory threadFactory,
			RejectedExecutionHandler handler) {
		executor = new ThreadPoolExecutor(corePoolSize, maximumPoolSize, keepAliveTime, unit, workQueue, threadFactory, handler) {
			@Override
			protected void afterExecute(Runnable r, Throwable t) {
				super.afterExecute(r, t);
				onWorkerFinish(r, t);
			}
		};
	}

	protected void onWorkerFinish(Runnable r, Throwable t) {
		if (t == null) {
			System.out.println(Thread.currentThread().getId() + " has been succeesfully finished!");
		} else {
			System.out.println(Thread.currentThread().getId() + " errroed! Retry");
		}
		for (Future<?> future : runnableMonitorMap.keySet()) {
			if (!future.isDone()) {
				DownloadWorker runnable = runnableMonitorMap.get(future);
				DownloadWorker newRunnable = runnable.split();
				if (newRunnable != null) {
					push(newRunnable);
					break;
				}
			}
		}
	}

	public Future<?> push(DownloadWorker worker) {
		Future<?> future = executor.submit(worker);

		if (missionMonitors.containsKey(worker.MISSION_ID)) {
			missionMonitors.get(worker.MISSION_ID).add(future);
		} else {
			ConcurrentLinkedQueue<Future<?>> queue = new ConcurrentLinkedQueue<>();
			queue.add(future);
			missionMonitors.put(worker.MISSION_ID, queue);
		}

		runnableMonitorMap.put(future, worker);

		return future;
	}

	public boolean isFinished(int missionId) {
		ConcurrentLinkedQueue<Future<?>> futures = missionMonitors.get(missionId);
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
		ConcurrentLinkedQueue<Future<?>> futures = missionMonitors.get(missionId);
		for (Future<?> future : futures) {
			future.cancel(true);
		}
	}

	public void cancel(int missionId) {
		ConcurrentLinkedQueue<Future<?>> futures = missionMonitors.remove(missionId);
		for (Future<?> future : futures) {
			runnableMonitorMap.remove(future);
			future.cancel(true);
		}
	}

	public int getActiveCount() {
		return executor.getActiveCount();
	}

	public void setCorePoolSize(int maxCount) {
		executor.setCorePoolSize(maxCount);
	}

	public boolean isShutdown() {
		return executor.isShutdown();
	}

	public void shutdown() {
		executor.shutdown();
	}

	public void shutdownNow() {
		executor.shutdownNow();
	}
}
