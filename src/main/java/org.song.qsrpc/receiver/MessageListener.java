package org.song.qsrpc.receiver;

public interface MessageListener {
    byte[] onMessage(final byte[] message);
}
