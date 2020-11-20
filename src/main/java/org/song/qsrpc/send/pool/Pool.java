package org.song.qsrpc.send.pool;

import org.apache.commons.pool.PoolableObjectFactory;
import org.apache.commons.pool.impl.GenericObjectPool;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.song.qsrpc.RPCException;

/**
 * 封装commons-pool的抽象对象池
 */
public abstract class Pool<T> {

	private static final Logger logger = LoggerFactory.getLogger(Pool.class);

	/**
	 * 对象池
	 */
	private final GenericObjectPool internalPool;

	/**
	 * Creates a new instance of Pool.
	 *
	 * @param poolConfig
	 * @param factory
	 */
	public Pool(final GenericObjectPool.Config poolConfig, PoolableObjectFactory factory) {
		this.internalPool = new GenericObjectPool(factory, poolConfig);
	}

	/**
	 * 从对象池获取一个可用对象
	 *
	 * @return
	 * @throws RPCException
	 */
	@SuppressWarnings("unchecked")
	public T getResource() {
		try {
			return (T) internalPool.borrowObject();
		} catch (Exception e) {
			logger.error("Could not get a resource from the pool", e);
			return null;
		}
	}

	/**
	 * 返回对象到池中
	 *
	 * @param resource
	 * @throws RPCException
	 */
	public void returnResource(final T resource) {
		try {
			internalPool.returnObject(resource);
		} catch (Exception e) {
			logger.error("Could not return the resource to the pool", e);
		}
	}

	/**
	 * 返回一个调用失败的对象到池中
	 *
	 * @param resource
	 * @throws RPCException
	 */
	public void invalidateResource(final T resource) {
		try {
			internalPool.invalidateObject(resource);
		} catch (Exception e) {
			logger.error("Could not invalidateObject the resource to the pool", e);
		}
	}

	/**
	 * 获取活跃的池中对象数量
	 *
	 * @return
	 */
	public int getNumActive() {
		if (this.internalPool == null || this.internalPool.isClosed()) {
			return -1;
		}

		return this.internalPool.getNumActive();
	}

	/**
	 * 获取暂时idle的对象数量
	 *
	 * @return
	 */
	public int getNumIdle() {
		if (this.internalPool == null || this.internalPool.isClosed()) {
			return -1;
		}
		return internalPool.getNumIdle();
	}

	/**
	 * 销毁对象池
	 *
	 * @throws RPCException
	 */
	public void destroy() throws RPCException {
		try {
			internalPool.close();
		} catch (Exception e) {
			logger.error("Could not destroy the pool", e);
		}
	}

	public boolean isClosed() {
		return internalPool.isClosed();
	}

}
