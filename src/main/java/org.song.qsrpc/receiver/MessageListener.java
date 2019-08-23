package org.song.qsrpc.receiver;

public interface MessageListener {
    byte[] onMessage(final Async async, final byte[] message);

    interface Async {
        void callBack(final byte[] message);
    }
}

