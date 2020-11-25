package org.song.qsrpc.statistics;

import org.song.qsrpc.Message;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by QSong
 * Contact github.com/tohodog
 * Date 2020/11/25
 * 服务统计
 */
public class StatisticsManager {

    private static volatile StatisticsManager instance;

    public static StatisticsManager getInstance() {
        if (instance == null) {
            synchronized (StatisticsManager.class) {
                if (instance == null) {
                    instance = new StatisticsManager();
                }
            }
        }
        return instance;
    }

    private Map<String, StatisticsInfo> map = new HashMap<>();


    public void start(String ipport, Message message) {

    }

    public void success(String ipport, Message message) {

    }

    public void fail(String ipport, Throwable t) {

    }

    public static class StatisticsInfo {

    }
}
