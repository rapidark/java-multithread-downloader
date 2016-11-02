package com.abigdreamer.common.download;

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
	
	private ConcurrentHashMap<Future<?>, DownloadWorker> workerFuturesMap = new ConcurrentHashMap<>();
	private ConcurrentHashMap<Integer, ConcurrentLinkedQueue<Future<?>>> missionWorkerFutures = new ConcurrentHashMap<>();

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
		for (Future<?> workerFuture : workerFuturesMap.keySet()) {
			if (!workerFuture.isDone()) {
				DownloadWorker worker = workerFuturesMap.get(workerFuture);
				DownloadWorker splitWorker = worker.split();
				if (splitWorker != null) {
					push(splitWorker);
					break;
				}
			}
		}
	}

	public Future<?> push(DownloadWorker worker) {
		Future<?> workerFuture = executor.submit(worker);

		if (missionWorkerFutures.containsKey(worker.MISSION_ID)) {
			missionWorkerFutures.get(worker.MISSION_ID).add(workerFuture);
		} else {
			ConcurrentLinkedQueue<Future<?>> queue = new ConcurrentLinkedQueue<>();
			queue.add(workerFuture);
			missionWorkerFutures.put(worker.MISSION_ID, queue);
		}

		workerFuturesMap.put(workerFuture, worker);

		return workerFuture;
	}

	public boolean isFinished(int missionId) {
		ConcurrentLinkedQueue<Future<?>> futures = missionWorkerFutures.get(missionId);
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
		ConcurrentLinkedQueue<Future<?>> futures = missionWorkerFutures.get(missionId);
		for (Future<?> workerFuture : futures) {
			workerFuture.cancel(true);
		}
	}

	public void cancel(int missionId) {
		ConcurrentLinkedQueue<Future<?>> futures = missionWorkerFutures.remove(missionId);
		for (Future<?> future : futures) {
			workerFuturesMap.remove(future);
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
