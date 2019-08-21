package org.song.qsrpc.send.cb;

import org.song.qsrpc.RPCException;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * 回调的future实现
 */
public class CallFuture<T> implements Future<T>, Callback<T> {

	private final int MAX_WAIT = 1 * 60 * 1000;

	/**
	 * 内部回调用的栅栏
	 */
	private final CountDownLatch latch = new CountDownLatch(1);

	/**
	 * 调用返回结果
	 */
	private T result = null;

	/**
	 * 调用错误信息
	 */
	private Throwable error = null;

	/**
	 * Creates a new instance of CallFuture.
	 */
	private CallFuture() {

	}

	/**
	 * 静态创建方法
	 *
	 * @return
	 */
	public static <E> CallFuture<E> newInstance() {
		return new CallFuture<E>();
	}

	/**
	 * Sets the RPC response, and unblocks all threads waiting on {@link #get()} or
	 * {@link #get(long, TimeUnit)}.
	 *
	 * @param result the RPC result to set.
	 */
	@Override
	public void handleResult(T result) {
		this.result = result;
		latch.countDown();
	}

	/**
	 * Sets an error thrown during RPC execution, and unblocks all threads waiting
	 * on {@link #get()} or {@link #get(long, TimeUnit)}.
	 *
	 * @param error the RPC error to set.
	 */
	@Override
	public void handleError(Throwable error) {
		this.error = error;
		latch.countDown();
	}

	/**
	 * Gets the value of the RPC result without blocking. Using {@link #get()} or
	 * {@link #get(long, TimeUnit)} is usually preferred because these methods block
	 * until the result is available or an error occurs.
	 *
	 * @return the value of the response, or null if no result was returned or the
	 *         RPC has not yet completed.
	 */
	public T getResult() {
		return result;
	}

	/**
	 * Gets the error that was thrown during RPC execution. Does not block. Either
	 * {@link #get()} or {@link #get(long, TimeUnit)} should be called first because
	 * these methods block until the RPC has completed.
	 *
	 * @return the RPC error that was thrown, or null if no error has occurred or if
	 *         the RPC has not yet completed.
	 */
	public Throwable getError() {
		return error;
	}

	/**
	 * @see Future#get()
	 */
	@Override
	public T get() throws InterruptedException, RPCException {
		return get(MAX_WAIT, TimeUnit.MILLISECONDS);
	}

	/**
	 * @throws InterruptedException
	 * @see Future#get(long, TimeUnit)
	 */
	@Override
	public T get(long timeout, TimeUnit unit) throws RPCException, InterruptedException {

		if (latch.await(timeout, unit)) {
			if (error != null) {
				throw new RPCException(error);
			}
			return result;
		} else {
			throw new RPCException("CallFuture get time out");
		}
	}

	/**
	 * Waits for the CallFuture to complete without returning the result.
	 *
	 * @throws InterruptedException if interrupted.
	 */
	public void await() throws InterruptedException {
		latch.await();
	}

	/**
	 * Waits for the CallFuture to complete without returning the result.
	 *
	 * @param timeout the maximum time to wait.
	 * @param unit    the time unit of the timeout argument.
	 * @throws InterruptedException if interrupted.
	 * @throws TimeoutException     if the wait timed out.
	 */
	public void await(long timeout, TimeUnit unit) throws InterruptedException, TimeoutException {
		if (!latch.await(timeout, unit)) {
			throw new TimeoutException();
		}
	}

	/**
	 * @see Future#cancel(boolean)
	 */
	@Override
	public boolean cancel(boolean mayInterruptIfRunning) {
		return false;
	}

	/**
	 * @see Future#isCancelled()
	 */
	@Override
	public boolean isCancelled() {
		return false;
	}

	/**
	 * @see Future#isDone()
	 */
	@Override
	public boolean isDone() {
		return latch.getCount() <= 0;
	}

}