package com.blucou.backup;

import com.blucou.backup.DomainClasses.File;
import com.blucou.backup.DomainClasses.Storage;

import java.util.logging.Level;
import java.util.logging.Logger;

public class BlockWriterRunnable implements Runnable {
    Storage storage;

    File file;

    byte[] buffer;

    int numBytes;

    public BlockWriterRunnable(Storage storage, File file, byte[] buffer, int numBytes) {

        this.storage = storage;
        this.file = file;
        this.buffer = buffer;
        this.numBytes = numBytes;
    }

    public void run() {

        try {
            StorageService.backupToStorage(storage, file, buffer, numBytes);
            this.buffer = null;
            System.gc();
        } catch (Exception ex) {
            Logger.getLogger(BlockWriterRunnable.class.getName()).log(Level.SEVERE, null, ex);
            System.exit(1);
        }
    }
}
