package org.song.qsrpc;

/**
 * 客户端通用的异常
 */
public class RPCException extends RuntimeException {

    /**
     * serialVersionUID
     */
    private static final long serialVersionUID = 5196421433506179782L;

    /**
     * Creates a new instance of PbrpcException.
     */
    public RPCException() {
        super();
    }

    /**
     * Creates a new instance of PbrpcException.
     *
     * @param arg0
     * @param arg1
     */
    public RPCException(String arg0, Throwable arg1) {
        super(arg0, arg1);
    }

    /**
     * Creates a new instance of PbrpcException.
     *
     * @param arg0
     */
    public RPCException(String arg0) {
        super(arg0);
    }

    /**
     * Creates a new instance of PbrpcException.
     *
     * @param arg0
     */
    public RPCException(Throwable arg0) {
        super(arg0);
    }

}
