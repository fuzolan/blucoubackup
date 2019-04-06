package com.blucou.backup;

import java.util.zip.Checksum;

public class ChecksumHasherRunnable implements Runnable {
    Checksum checksum;

    byte[] buffer;

    int numBytes;

    public ChecksumHasherRunnable(Checksum checksum, byte[] buffer, int numBytes) {

        this.checksum = checksum;
        this.buffer = buffer;
        this.numBytes = numBytes;
    }

    public void run() {
        this.checksum.update(buffer, 0, numBytes);
    }

}
