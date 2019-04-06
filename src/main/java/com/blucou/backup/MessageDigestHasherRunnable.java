package com.blucou.backup;

import java.security.MessageDigest;

public class MessageDigestHasherRunnable implements Runnable {
    MessageDigest md;

    byte[] buffer;

    int numBytes;

    public MessageDigestHasherRunnable(MessageDigest md, byte[] buffer, int numBytes) {

        this.md = md;
        this.buffer = buffer;
        this.numBytes = numBytes;
    }

    public void run() {

        this.md.update(buffer, 0, numBytes);
    }
}
