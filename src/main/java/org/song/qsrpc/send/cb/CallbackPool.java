package org.song.qsrpc.send.cb;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import org.song.qsrpc.Message;
import org.song.qsrpc.RPCException;

/**
 * 客户端回调池，用于保存调用发送请求出去的上下文，用于nio异步通信收到服务端响应后回调成功或者失败
 */
public class CallbackPool {

    /**
     * Map默认键数量
     */
    private static final int INITIAL_CAPACITY = 128 * 4 / 3;

    /**
     * Map的扩容装载因子
     */
    private static final float LOAD_FACTOR = 0.75f;

    /**
     * Map的并发度，也就是segament数量，读不锁写锁，
     */
    private static final int CONCURRENCY_LEVEL = Math.max(Runtime.getRuntime().availableProcessors() * 2, 8);

    /**
     * 保存键为调用的唯一标示requestId</tt>
     */
    private static ConcurrentHashMap<Object, Callback<?>> CALLBACK_MAP = new ConcurrentHashMap<>(
            INITIAL_CAPACITY, LOAD_FACTOR, CONCURRENCY_LEVEL);

    private static ConcurrentHashMap<Object, ScheduledFuture<?>> TIMEOUT_MAP = new ConcurrentHashMap<>(INITIAL_CAPACITY,
            LOAD_FACTOR, CONCURRENCY_LEVEL);

    /**
     * 放入回调上下文
     *
     * @param requestId requestId
     * @param callback  客户端句柄callback
     * @param timeout   客户端调用超时
     */
    public static void put(final Integer requestId, Callback<?> callback, final int timeout) {
        CALLBACK_MAP.putIfAbsent(requestId, callback);
        if (timeout > 0) {
            TIMEOUT_MAP.putIfAbsent(requestId, SCHEDULED_EXECUTOR_SERVICE.schedule(new Runnable() {
                @Override
                public void run() {
                    TIMEOUT_MAP.remove(requestId);
                    @SuppressWarnings("unchecked")
                    Callback<Message> cb = (Callback<Message>) CALLBACK_MAP.remove(requestId);
                    if (cb != null) {
                        cb.handleError(new RPCException("CallbackPool time out: " + timeout + "ms, id:" + requestId));
                    }
                }
            }, timeout, TimeUnit.MILLISECONDS));
        }
    }

    public static void put(Integer requestId, Callback<?> callback) {
        put(requestId, callback, 0);
    }

    /**
     * 移除回调上下文
     *
     * @param requestId
     */
    public static Callback<?> remove(Integer requestId) {
        ScheduledFuture<?> scheduledFuture = TIMEOUT_MAP.remove(requestId);
        if (scheduledFuture != null) {
            scheduledFuture.cancel(false);
        }
        return CALLBACK_MAP.remove(requestId);
    }

    private static final ScheduledExecutorService SCHEDULED_EXECUTOR_SERVICE = Executors.newScheduledThreadPool(1);//处理超时回调,一个线程

//	private static ReentrantReadWriteLock readWriteLock = new ReentrantReadWriteLock();
//
//	/**
//	 * 获取保存回调上下文的Map
//	 *
//	 * @return
//	 */
//	public static ConcurrentHashMap<Object, CallbackContext> getCurrentMap() {
//		try {
//			readWriteLock.readLock().lock();
//			return CALLBACK_MAP;
//		} finally {
//			readWriteLock.readLock().unlock();
//		}
//	}
//
//	public static void changeCurrentMap() {
//		try {
//			readWriteLock.writeLock().lock();
//
//		} finally {
//			readWriteLock.writeLock().unlock();
//		}
//	}
}
