package org.song.qsrpc.discover;

import java.util.List;

public interface Watcher<T> {

    void onNodeChange(List<T> serverList);

}