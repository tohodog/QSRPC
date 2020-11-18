
package org.song.qsrpc;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author song
 * @Email vipqinsong@gmail.com
 * @date 2018年8月6日
 * <p>
 * 服务器日记
 */
public class Log {
    public static Logger LOGGER = LoggerFactory.getLogger("QSRPC");

    public static void e(Object msg) {
        LOGGER.error(String.valueOf(msg));
    }

    public static void e(String msg, Throwable t) {
        if (t == null)
            e(msg);
        else
            LOGGER.error(msg, t);
    }

    public static void e(String msg, String t) {
        if (t == null)
            e(msg);
        else
            LOGGER.error(msg + " -> " + t);
    }

    public static void i(Object msg) {
        LOGGER.info(String.valueOf(msg));
    }

    public static void w(String msg, Throwable t) {
        if (t == null)
            w(msg);
        else
            LOGGER.warn(msg, t);
    }

    public static void w(String msg) {
        LOGGER.warn(msg);
    }

    public static void d(String msg, Throwable t) {
        if (t == null)
            d(msg);
        else
            LOGGER.debug(msg, t);
    }

    public static void d(String msg) {
        LOGGER.debug(msg);
    }

}
