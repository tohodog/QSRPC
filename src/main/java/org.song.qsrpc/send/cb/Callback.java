package org.song.qsrpc.send.cb;

/**
 * 客户端回调接口
 */
public interface Callback<T> {

    /**
     * 当接受到服务端返回的数据后的处理
     *
     * @param result
     */
    void handleResult(T result);

    /**
     * 发生异常时候的处理
     *
     * @param error
     */
    void handleError(Throwable error);


}