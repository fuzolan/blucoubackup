/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.blucou.backup;

import com.blucou.backup.DomainClasses.*;
import com.j256.ormlite.dao.CloseableIterator;
import com.j256.ormlite.stmt.DeleteBuilder;
import com.j256.ormlite.stmt.Where;
import org.apache.commons.io.FileUtils;
import org.bouncycastle.openpgp.PGPException;

import java.io.Console;
import java.io.FileOutputStream;
import java.io.IOException;
import java.math.BigInteger;
import java.net.URI;
import java.net.URISyntaxException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.SecureRandom;
import java.sql.SQLException;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author Fuzolan
 */
public class StorageService {

    private static byte[] image = null;

    private static Storage lastStorage = null;


    public static byte[] getImage() throws Exception {

        if (StorageService.image == null) {
            StorageService.image = new byte[3705];
            int count = StorageService.class.getClassLoader().getResourceAsStream("tb.jpg").read(StorageService.image, 0, 3705);
            if (count != 3705) throw new Exception("Wrong Pic Data");
        }
        return StorageService.image;
    }

    public static void setImage(byte[] image) {

        StorageService.image = image;
    }

    public static Storage initStorage(String path) throws SQLException {
        //Storage identifier erstellen (Ordner mit einem bestimmten Wert)


        java.io.File backupDir = new java.io.File(path);


        if (backupDir.isDirectory()) {
            String[] subNodes = backupDir.list();
            for (String filename : subNodes) {
                //Suche nach bestehenden identfiern
                //checkFS(new java.io.File(backupDir,filename));
                if (filename.contains("blucoubackupidf")) {
                    //Möglicherweise Ist dies ein IDentifier einer anderen Datenbank und hat nichts mit dieser zutun
                    Storage foundStorage = DatabaseService.getInstance().getStorageDaoImpl().queryBuilder().where().eq("identifier", filename).queryForFirst();

                    if (foundStorage != null) {
                        foundStorage.setPath(backupDir.getAbsolutePath());
                        DatabaseService.getInstance().getStorageDaoImpl().update(foundStorage);
                        return foundStorage;
                    }
                }
            }
        } else {
            //überlieferter Pfad ist kein Verzeichnis...raus hier
            return null;
        }

        //muss initialisiert werden
        String idf = "blucoubackupidf" + new BigInteger(70, new SecureRandom()).toString(32);
        java.io.File idfDir = new java.io.File(backupDir.getAbsolutePath() + java.io.File.separator + idf);
        idfDir.mkdir();

        Storage storage = new Storage();
        storage.setPath(backupDir.getAbsolutePath());
        storage.setIdentifier(idf);
        storage.setType("Filesystem");
        DatabaseService.getInstance().getStorageDaoImpl().create(storage);

        return storage;
    }

    public static boolean checkStorage(Storage storage, long bytesNeeded) {

        java.io.File storageProbe = new java.io.File(storage.getPath() + java.io.File.separator + storage.getIdentifier());
        return storageProbe.canWrite() && storageProbe.getUsableSpace() > bytesNeeded && (storage.getQuota() > storage.getUsedSpace() || storage.getQuota() == 0);
    }

    public static Storage getPossibleStorage(long bytes) throws Exception {
        //Suche nach einem brauchbaren Storage
        if (StorageService.lastStorage != null && StorageService.checkStorage(StorageService.lastStorage, bytes)) {
            return StorageService.lastStorage;
        }

        List<Storage> storage = DatabaseService.getInstance().getStorageDaoImpl().queryForAll();

        for (Iterator<Storage> it = storage.iterator(); it.hasNext(); ) {
            Storage aStorage = it.next();

            if (StorageService.checkStorage(aStorage, bytes)) {
                StorageService.lastStorage = aStorage;
                return aStorage;
            }

        }
        System.err.println("No Storage Available");
        throw new Exception("No Storage Available");
    }

    /*
     * public static boolean backupToStorage(Storage storage, File file,
     * java.io.File fileNode) throws SQLException, IOException{ return
     * backupToStorage(storage, file, fileNode, false); }
     */
    public static boolean backupToStorage(Storage storage, File file, byte[] buffer, int numBytes) throws Exception {
        //Nachschauen ob datei schon gesichert wurde?
            /*
         * if(DatabaseService.getInstance().getFileStorageDaoImpl().queryBuilder().where().eq("file_id",file.getId()).countOf()
         * > 0){ //Mit dieser Abfrage ist es derzeit nicht möglich Dateien auf
         * mehrfach auf Storages zu verteilen return false; //Datei wurde schon
         * gesichert };
         */

        FileStorage fileStorage = new FileStorage();
        fileStorage.setFilePath(getRightFolder(storage, file));
        fileStorage.setFile(file);
        fileStorage.setStorage(storage);


        //Bild speichern
        //java.io.File pic = new java.io.File("tb.jpg");
        //ava.io.File pic = StorageService.getImage();
        //ImageIO.write(image, "jpg", destination);
        java.io.File destination;
        if (ConfigurationService.getInstance().getConfig().getBoolean("saveAsPicture")) {
            destination = new java.io.File(storage.getBackupFolderPath().getAbsolutePath() + fileStorage.getFilePath() + java.io.File.separator + file.getId() + ".jpg");
            FileUtils.writeByteArrayToFile(destination, StorageService.getImage());
        } else {
            destination = new java.io.File(storage.getBackupFolderPath().getAbsolutePath() + fileStorage.getFilePath() + java.io.File.separator + file.getId() + ".dat");
        }

        //copyFile(pic, destination);

        CryptoService crypto = CryptoService.getInstance();
        try {

            crypto.encrypt(buffer, destination, numBytes);
            //System.out.print("+");
            if (org.apache.log4j.Logger.getLogger(StorageService.class.getName()).isTraceEnabled()) {
                org.apache.log4j.Logger.getLogger(StorageService.class.getName()).trace("compressed:" + destination.length() + "-uncompressed:" + numBytes);
            }
            //FileUtils.moveFile(fileNode, destination);
            //fileNode.delete();
        } catch (Exception ex) {
            org.apache.log4j.Logger.getLogger(StorageService.class.getClass().getName()).error(ex);
            System.exit(1);
        }

        crypto = null;
        try {
            fileStorage.setFileSize(destination.length());
            storage.setUsedSpace(storage.getUsedSpace() + fileStorage.getFileSize());
            DatabaseService.getInstance().getFileStorageDaoImpl().create(fileStorage);
            DatabaseService.getInstance().getStorageDaoImpl().update(storage);
        } catch (Exception ex) {
            org.apache.log4j.Logger.getLogger(StorageService.class.getClass().getName()).error(ex);
            //System.exit(1);
        }

        fileStorage = null;
        destination = null;
        return true;
    }


    private static void restoreFile(File file, java.io.File destination) throws SQLException {

        System.out.print("*");
        //DatabaseService.getInstance().getFileDaoImpl().refresh(file);
        //Eintrag holen und nachschauen ob verbundene Datei ein file_block ist
        Iterator<FileBlock> itFileBlock = DatabaseService.getInstance().getFileBlockDaoImpl().queryBuilder().orderBy("blockNr", true).where().eq("file_id", file.getId()).iterator();
        if (itFileBlock.hasNext()) {
            Logger.getLogger(StorageService.class.getName()).log(Level.FINE, null, "File-Block Restore");
            //Es sind Fileblöcke vorhanden, welche aneinandergereiht werden müssen

            while (itFileBlock.hasNext()) {
                FileBlock fileBlock = itFileBlock.next();

                //Falls man mit den Blockgrößen herumspielt...könnte es passieren, dass hinter einer block_id eine virtuelle Datei steckt die ebenfalls über Fileblocks aufgebaut ist. Deswegen die Rekursion
                restoreFile(fileBlock.getBlock(), destination);

            }//endwhile

        } else {
            Logger.getLogger(StorageService.class.getName()).log(Level.FINE, null, "Single File Restore");
            //Die Datei befindet sich komplett in einem File
            FileStorage fileStorage = DatabaseService.getInstance().getFileStorageDaoImpl().queryBuilder().where().eq("file_id", file.getId()).queryForFirst();
            if (fileStorage == null) {
                System.err.println("File has no storage data...the file is garbage!!!!");
                System.exit(1);
                return;
            }

            String suffix;
            //Datei vom Restore
            if (ConfigurationService.getInstance().getConfig().getBoolean("saveAsPicture")) {
                suffix = ".jpg";
            } else {
                suffix = ".dat";
            }
            java.io.File backupedFile = new java.io.File(fileStorage.getStorage().getBackupFolderPath().getAbsolutePath() + fileStorage.getFilePath() + java.io.File.separator + file.getId() + suffix);
            try {
                CryptoService crypto = CryptoService.getInstance();
                crypto.decrypt(backupedFile, destination, true);
            } catch (IOException ex) {
                Logger.getLogger(StorageService.class.getName()).log(Level.SEVERE, null, ex);
            } catch (NoSuchProviderException ex) {
                Logger.getLogger(StorageService.class.getName()).log(Level.SEVERE, null, ex);
            } catch (PGPException ex) {
                Logger.getLogger(StorageService.class.getName()).log(Level.SEVERE, null, ex);
            } catch (Exception ex) {
                Logger.getLogger(StorageService.class.getName()).log(Level.SEVERE, null, ex);
            }

        }//endif
    }

    /**
     * Schreibt die gesicherten Daten zurück
     *
     * @param backupobj     //legt fest welches System zurückgespielt wird
     * @param destination   //Wenn leer, dann wird die Datei erzeugt und zurückgegeben
     * @param pathToRestore
     * @return
     */
    public static long restoreFromStorage(Backupobj backupobj, java.io.File destination, String pathToRestore, boolean restoreDeleted, int maxVersionsOfObsolete) throws SQLException, URISyntaxException {

        long restoredFiles = 0;
        int obsoleteVersion = 0;
        String lastpath = "";

        System.out.println("##RESTORE## Machine:" + backupobj.getName() + " Destination:" + destination.getAbsolutePath() + "  restoreDeleted:" + restoreDeleted + " maxVersionsOfObsolete:" + maxVersionsOfObsolete);


        //Datensätze aus file_backupobj holen die a)dem Backupobj entsprechen und b)den zurücksichernden path enthält
        CloseableIterator<FileBackupobj> filebackupobjs;
        Where<FileBackupobj, Long> queryStatement = DatabaseService.getInstance().getFileBackupobjDaoImpl().queryBuilder().orderBy("absolutePath", false).orderBy("lastbackup", false).where();
        queryStatement.eq("backupobj_id", backupobj.getId());

        if (pathToRestore != "") {
            queryStatement.and().like("absolutePath", pathToRestore + "%");
        }
        if (!restoreDeleted) {
            queryStatement.and().eq("deleted", false);
        }
        if (maxVersionsOfObsolete == 0) {
            queryStatement.and().eq("obsolete", false);
        }

        filebackupobjs = queryStatement.iterator();

        for (; filebackupobjs.hasNext(); ) {
            FileBackupobj fileBackupobj = filebackupobjs.next();
            if (!lastpath.equals(fileBackupobj.getAbsolutePath())) {
                obsoleteVersion = 0; //Counter zurücksetzen
            }
            lastpath = fileBackupobj.getAbsolutePath();

            java.io.File file = new java.io.File(new URI(fileBackupobj.getAbsolutePath()));
            String restorePath = file.getAbsolutePath().replaceAll("[:*\"\\?<>|]", "_");
            java.io.File restoreFileDestination = new java.io.File(destination, restorePath); //Ziel bestimmen

            if (fileBackupobj.getName().equals("") && fileBackupobj.getFile() == null) {
                file.mkdirs();
                continue;
            }

            if (fileBackupobj.isObsolete()) {
                if (obsoleteVersion >= maxVersionsOfObsolete) {
                    System.out.println("Maximum of restored obsolete versions reached. Skip to next file.");
                    continue;
                } //Obsolete Datei wurde schon mit ausreichend Versionen zurückgesetzt
                obsoleteVersion++; //hochsetzen des counters
                restoreFileDestination = new java.io.File(destination, restorePath + "-BlucouBackup" + "-" + fileBackupobj.getId() + "-" + String.valueOf(fileBackupobj.getUpdated())); //Ziel bestimmen
            } //Eine obsolete DAtei wird zurückgespielt

            if (restoreFileDestination.exists()) {
                System.out.println(restoreFileDestination.getName() + " already exists. Move to Next File");
                continue;
            } //Datei ist schon vorhanden
            System.out.println(fileBackupobj.getName());
            restoreFile(fileBackupobj.getFile(), restoreFileDestination);
            restoreFileDestination.setLastModified(fileBackupobj.getUpdated());
            restoredFiles++;

        }//endfor
        filebackupobjs.close();

        // }
        //sortiere nach pfad

        //(nachschauen ob Einträge vielleicht alt od. gelöscht sind)

        //Eintrag holen und nachschauen ob verbundene Datei ein file_block ist

        //fileblock ja: für die Datei alle nötigen Fileblocks ermitteln
        //              file storage ermitteln evtl. anfragen und an Zielort schreiben

        //fileblock nein: file storage für file ermitteln evtl. anfragen und an Zielort schreiben

        System.out.println("##RESTORE## COMPLETE -> Number of files restored: " + restoredFiles);
        return restoredFiles;
    }

    /**
     * Schreibt ins tmp Verzeichnis von Storage
     *
     * @param storage
     * @param fileNode //Wenn leer, dann wird die Datei erzeugt und
     *                 zurückgegeben
     * @param data
     * @return
     */
    public static java.io.File writeBlockToStorageTemp(Storage storage, java.io.File fileNode, byte[] data, int num) throws IOException {

        if (fileNode == null) {
            //Tempdatei erzeugen
            fileNode = java.io.File.createTempFile("blucoubackupblk", ".dat", StorageService.getStorageTempDirectory(storage));
        }
        //Encryption

        FileOutputStream fos = FileUtils.openOutputStream(fileNode, true);

        fos.write(data, 0, num);
        fos.close();

        return fileNode;
    }

    /**
     * Erstellt aus dem Hash der Datei ein Zielort
     *
     * @param storage
     * @param file
     * @return Gibt den Zielordner als relative Pfadangabe zurück
     */
    public static String getRightFolder(Storage storage, File file) {

        String firstFolder = file.getMd5().substring(0, 1);
        String secondFolder = file.getMd5().substring(0, 4);
        String relativePath = java.io.File.separator + firstFolder + java.io.File.separator + secondFolder;
        java.io.File backupFolder = new java.io.File(storage.getBackupFolderPath().getAbsolutePath() + relativePath);
        if (!backupFolder.isDirectory()) {
            backupFolder.mkdirs();
        }


        return relativePath;
    }

    /**
     * Erstellt aus dem Hash der Datei ein Zielort
     *
     * @param storage
     * @param file
     * @return Gibt den Zielordner als relative Pfadangabe zurück
     */
    public static java.io.File getRightFolderAsFile(Storage storage, File file) {

        String firstFolder = file.getMd5().substring(0, 1);
        String secondFolder = file.getMd5().substring(0, 4);
        String relativePath = java.io.File.separator + firstFolder + java.io.File.separator + secondFolder;
        java.io.File backupFolder = new java.io.File(storage.getBackupFolderPath().getAbsolutePath() + relativePath);
        if (!backupFolder.isDirectory()) {
            backupFolder.mkdirs();
        }


        return backupFolder;
    }

    public static void deleteStorage() throws SQLException {

        System.out.println("!!!!DELETE A STORAGE!!!!\nChoose a storage from list:");
        CloseableIterator<Storage> it = DatabaseService.getInstance().getStorageDaoImpl().iterator();
        while (it.hasNext()) {
            Storage s = it.next();
            System.out.println(s.getId() + ": " + s.getPath() + " " + s.getIdentifier() + " " + (s.getUsedSpace() / 1024 / 1024) + "MiB Used");
        }

        Console cons;
        String input;
        if (!((cons = System.console()) != null && (input = cons.readLine("[%s]", "Press a number and hit enter to delete (0 to exit)")) != null)) {
            throw new IllegalArgumentException("Something goes wrong with input");
        }
        it.close();

        DeleteBuilder<FileStorage, Long> db = DatabaseService.getInstance().getFileStorageDaoImpl().deleteBuilder();
        db.where().eq("storage_id", input);
        DatabaseService.getInstance().getStorageDaoImpl().deleteById(Long.parseLong(input));
        System.out.println("Storage deleted -> you should run a cleanup");
    }

    public static void setStorageQuota() throws SQLException {

        System.out.println("Set storage quato\nChoose a storage from list:");
        CloseableIterator<Storage> it = DatabaseService.getInstance().getStorageDaoImpl().iterator();
        while (it.hasNext()) {
            Storage s = it.next();
            System.out.println(s.getId() + ": " + s.getPath() + " " + s.getIdentifier() + " " + (s.getUsedSpace() / 1024 / 1024) + "MiB used " + s.getQuota() + "MiB quota");
        }

        Console cons;
        String input_storage;
        String input_quota;
        if (!((cons = System.console()) != null && (input_storage = cons.readLine("[%s]", "Press a number and hit enter to set a quota")) != null)) {
            throw new IllegalArgumentException("Something goes wrong with input");
        }

        if (!((cons = System.console()) != null && (input_quota = cons.readLine("[%s]", "Write quota in MiB and hit enter (0=no quota):")) != null)) {
            throw new IllegalArgumentException("Something goes wrong with input");
        }
        it.close();

        Storage s = DatabaseService.getInstance().getStorageDaoImpl().queryForId(Long.parseLong(input_storage));
        if (s != null) {
            s.setQuota(Long.parseLong(input_quota) * 1024 * 1024);
            DatabaseService.getInstance().getStorageDaoImpl().update(s);
        }
    }

    public static void UpdateUsedSpace() throws SQLException {

        long i = 0;
        CloseableIterator<Storage> it_storage = DatabaseService.getInstance().getStorageDaoImpl().closeableIterator();
        while (it_storage.hasNext()) {
            Storage storage = it_storage.next();
            if (storage.getBackupFolderPath().canRead()) {
                System.out.println("UpdateUsedSpace of storage " + storage.getPath() + ":");
                long usedSpace = 0;


                CloseableIterator<FileStorage> it = DatabaseService.getInstance().getFileStorageDaoImpl().queryBuilder().where().eq("storage_id", storage.getId()).iterator();
                while (it.hasNext()) {
                    FileStorage fs = it.next();
                    if (fs.getFileSize() <= 0) {
                        java.io.File fileInStorage = new java.io.File(storage.getPath() + java.io.File.separator + storage.getIdentifier() + java.io.File.separator + fs.getFilePath() + java.io.File.separator + fs.getFile().getId() + ".jpg");
                        if (!fileInStorage.exists()) {
                            //Fallback auf .dat
                            fileInStorage = new java.io.File(storage.getPath() + java.io.File.separator + storage.getIdentifier() + java.io.File.separator + fs.getFilePath() + java.io.File.separator + fs.getFile().getId() + ".dat");
                        }
                        fs.setFileSize(fileInStorage.length());
                        DatabaseService.getInstance().getFileStorageDaoImpl().update(fs);
                    }
                    usedSpace += fs.getFileSize();
                    System.out.print("\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b" + i++);
                }

                it.close();
                storage.setUsedSpace(usedSpace);
                DatabaseService.getInstance().getStorageDaoImpl().update(storage);
                System.err.println("\n->" + usedSpace / 1024 / 1024 + "MiB");
            }

        }
        it_storage.close();


    }

    /**
     * only used for test
     *
     * @param storage
     * @param file
     * @return
     */
    public static String getAbsolutePathOfFile(Storage storage, File file) {
        String suffix;
        if (ConfigurationService.getInstance().getConfig().getBoolean("saveAsPicture")) {
            suffix = ".jpg";
        } else {
            suffix = ".dat";
        }
        return storage.getPath() + java.io.File.separator + storage.getIdentifier() + java.io.File.separator + getRightFolder(storage, file) + java.io.File.separator + file.getId() + suffix;
    }

    public static java.io.File getStorageTempDirectory(Storage storage) {
        //TODO müsste am Storage erzeugt werden
        java.io.File tempdir = FileUtils.getTempDirectory();
        return tempdir;
    }

    public long validateBackupFiles() throws NoSuchAlgorithmException, SQLException, IOException {

        BackupObjService bh = new BackupObjService();
        bh.init();
        long filesToDelete = 0;

        List<Storage> storage = DatabaseService.getInstance().getStorageDaoImpl().queryForAll();

        for (Iterator<Storage> it = storage.iterator(); it.hasNext(); ) {
            Storage aStorage = it.next();

            java.io.File storageProbe = new java.io.File(aStorage.getPath() + java.io.File.separator + aStorage.getIdentifier());
            if (storageProbe.canRead()) {
                //Storage ist angeschlossen
                CloseableIterator<FileStorage> itfs = DatabaseService.getInstance().getFileStorageDaoImpl().queryBuilder().where().eq("storage_id", aStorage.getId()).iterator();

                while (itfs.hasNext()) {
                    FileStorage aFileStorage = itfs.next();
                    File file = aFileStorage.getFile();
                    java.io.File fileToCheck = new java.io.File(aFileStorage.getStorage().getBackupFolderPath().getAbsolutePath() + aFileStorage.getFilePath() + java.io.File.separator + file.getId() + ".jpg");
                    if (!fileToCheck.exists()) {
                        //Fallback auf .dat
                        fileToCheck = new java.io.File(aFileStorage.getStorage().getBackupFolderPath().getAbsolutePath() + aFileStorage.getFilePath() + java.io.File.separator + file.getId() + ".dat");
                    }
                    if (fileToCheck.length() <= 0) {
                        org.apache.log4j.Logger.getLogger(StorageService.class.getName()).info("file is not longer present:" + file.getId());
                        DatabaseService.getInstance().getFileStorageDaoImpl().delete(aFileStorage);
                        filesToDelete++;
                    } else if (org.apache.log4j.Logger.getLogger(StorageService.class.getName()).isTraceEnabled()) {
                        org.apache.log4j.Logger.getLogger(StorageService.class.getName()).trace("file is present:" + file.getId());
                    }
                }
            }
        }
        return filesToDelete;
    }


    public void cleanup() throws SQLException, NoSuchAlgorithmException, IOException {

        BackupObjService bh = new BackupObjService();
        bh.init();
        {
            //filebackupobj   
            DeleteBuilder<FileBackupobj, Long> deleteBuilder = DatabaseService.getInstance().getFileBackupobjDaoImpl().deleteBuilder();
            Where where = deleteBuilder.where();

            Iterator it = ConfigurationService.getInstance().getConfig().getList("dirBlacklist").iterator();

            int i = 0;
            while (it.hasNext()) {
                String excludeDir = (String) it.next();
                if (!excludeDir.equals("")) {
                    i++;
                    where.like("absolutepath", excludeDir.replace("*", "%")).and().eq("backupobj_id", bh.getBackupobj().getId());
                }
            }

            Iterator itf = ConfigurationService.getInstance().getConfig().getList("fileBlacklist").iterator();

            while (itf.hasNext()) {
                String excludeFile = (String) itf.next();
                if (!excludeFile.equals("")) {
                    i++;
                    where.like("name", excludeFile.replace("*", "%")).and().eq("backupobj_id", bh.getBackupobj().getId());
                }
            }

            if (i > 0) {
                where.or(i);
                deleteBuilder.setWhere(where);

                System.out.println("<FileBackupobj> Cleanup entries from directory/file-blacklist: " + deleteBuilder.delete());
            }

        }

        {//storage entfernen
            long deleted = 0;
            CloseableIterator<FileStorage> it = DatabaseService.getInstance().getFileStorageDaoImpl().queryBuilder().where().notIn("storage_id", DatabaseService.getInstance().getStorageDaoImpl().queryBuilder().selectColumns("id")).iterator();
            while (it.hasNext()) {
                DeleteBuilder<FileStorage, Long> deleteBuilder = DatabaseService.getInstance().getFileStorageDaoImpl().deleteBuilder();
                deleteBuilder.where().eq("id", it.next().getId());
                deleted += deleteBuilder.delete();
            }
            System.out.println("<FileStorage> Cleanup lost StorageData: " + deleted);
            it.close();
        }

        //filebackupobj mit file=null entfernen
        {
            DeleteBuilder<FileBackupobj, Long> deleteBuilder = DatabaseService.getInstance().getFileBackupobjDaoImpl().deleteBuilder();
            deleteBuilder.where().isNull("file_id").and().ne("name", "");
            System.out.println("<FileBackupobj> Cleanup NULL entries: " + deleteBuilder.delete());
        }

        {
            //file einträge mit keinen hashes entfernen
            DeleteBuilder<File, Long> deleteBuilder = DatabaseService.getInstance().getFileDaoImpl().deleteBuilder();
            deleteBuilder.where().eq("md5", "");
            System.out.println("<File> Cleanup empty hashes: " + deleteBuilder.delete());

        }

        {//File löschen die nicht mehr in storage vorhanden ist UND keine Virtuelle  Datei ist
            DeleteBuilder<File, Long> deletebuilder = DatabaseService.getInstance().getFileDaoImpl().deleteBuilder();
            deletebuilder.where().
                    notIn("id", DatabaseService.getInstance().getFileStorageDaoImpl().queryBuilder().
                            selectColumns("file_id")).and().
                    notIn("id", DatabaseService.getInstance().getFileBlockDaoImpl().queryBuilder().
                            selectColumns("file_id"));
            System.out.println("<File> Not on any storage anymore: " + deletebuilder.delete());

        }


        {//file_backupobj
            // -Einträge entfernen die nach festgesetzter Zeitdauer schon sehr lange gelöscht waren - auf backupobj bezogen
            // -maxobsolete...veraltete Dateiversionen  - auf backupobj bezogen
            //option für globales reinigen ebenfalls anbieten  
            DeleteBuilder<FileBackupobj, Long> deletebuilder = DatabaseService.getInstance().getFileBackupobjDaoImpl().deleteBuilder();
            Date d = new Date();
            d.setTime(d.getTime() - (long) 1000 * 60 * 60 * 24 * ConfigurationService.getInstance().getConfig().getLong("keepDeletedInDays"));//vor 30 Tagen gelöscht
            deletebuilder.where().eq("deleted", true).and().lt("lastBackup", d).and().eq("backupobj_id", bh.getBackupobj().getId());
            System.out.println("<FileBackupobj> Cleanup deleted: " + deletebuilder.delete());
        }
        {
            DeleteBuilder<FileBackupobj, Long> deletebuilder = DatabaseService.getInstance().getFileBackupobjDaoImpl().deleteBuilder();
            Date d = new Date();
            d.setTime(d.getTime() - (long) 1000 * 60 * 60 * 24 * ConfigurationService.getInstance().getConfig().getLong("keepObsoleteInDays"));//vor 30 Tagen gelöscht
            deletebuilder.where().eq("obsolete", true).and().lt("lastBackup", d).and().eq("backupobj_id", bh.getBackupobj().getId());
            System.out.println("<FileBackupobj> Cleanup obsolete: " + deletebuilder.delete());

        }
        {//File_backupobj löschen welche keinen file eintrag mehr haben
            DeleteBuilder<FileBackupobj, Long> deletebuilder = DatabaseService.getInstance().getFileBackupobjDaoImpl().deleteBuilder();
            deletebuilder.where().notIn("file_id", DatabaseService.getInstance().getFileDaoImpl().queryBuilder().selectColumns("id"));
            System.out.println("<FileBackupobj> Fileentry is gone: " + deletebuilder.delete());


        }


        {//file_block: entferne blöcke von nicht mehr vorhandenen file-einträgen
            DeleteBuilder<FileBlock, Long> deleteBuilder = DatabaseService.getInstance().getFileBlockDaoImpl().deleteBuilder();
            deleteBuilder.where().notIn("file_id", DatabaseService.getInstance().getFileDaoImpl().queryBuilder().selectColumns("id"));
            System.out.println("<FileBlock> Cleanup lost virtual files: " + deleteBuilder.delete());
        }

        {
            long deleted = 0;
            CloseableIterator<FileBlock> it = DatabaseService.getInstance().getFileBlockDaoImpl().queryBuilder().where().notIn("block_id", DatabaseService.getInstance().getFileDaoImpl().queryBuilder().selectColumns("id")).iterator();
            while (it.hasNext()) {
                DeleteBuilder<FileBlock, Long> deleteBuilder = DatabaseService.getInstance().getFileBlockDaoImpl().deleteBuilder();
                deleteBuilder.where().eq("file_id", it.current().getFile().getId());
                deleted += deleteBuilder.delete();
            }
            System.out.println("<FileBlock> Cleanup lost blocks: " + deleted);
            it.close();
        }

        {
            //file_storage
            //entferne einträge zu denen es keine files mehr gibt

            CloseableIterator<FileStorage> it = DatabaseService.getInstance().getFileStorageDaoImpl().queryBuilder().where().notIn("file_id", DatabaseService.getInstance().getFileDaoImpl().queryBuilder().selectColumns("id")).iterator();
            long deleted = 0;
            while (it.hasNext()) {
                //einzeln überprüfen ob Daten auf Datenträger gelöscht werden kann
                FileStorage fs = it.current();
                Storage storage = fs.getStorage();
                if (fs.getStorage().getBackupFolderPath().exists()) {
                    //System.out.println("delete storagefile");
                    java.io.File file_to_delete = new java.io.File(storage.getBackupFolderPath() + fs.getFilePath() + java.io.File.separator + fs.getFile().getId() + ".jpg");
                    java.io.File file_to_delete2 = new java.io.File(storage.getBackupFolderPath() + fs.getFilePath() + java.io.File.separator + fs.getFile().getId() + ".dat");
                    boolean filedelete = file_to_delete.delete();
                    boolean filedelete2 = file_to_delete2.delete();
                    if (filedelete || filedelete2 || (!file_to_delete.exists() && !file_to_delete2.exists())) {
                        //storageeintrag entfernen
                        storage.setUsedSpace(storage.getUsedSpace() - fs.getFileSize());
                        if (DatabaseService.getInstance().getFileStorageDaoImpl().delete(fs) > 0) {
                            DatabaseService.getInstance().getStorageDaoImpl().update(storage);
                            deleted++;
                        }
                    } else {
                        System.out.println("Can't delete file (.dat also):" + file_to_delete.getAbsolutePath());
                    }
                } else {
                    System.out.println("Storage not connected:" + fs.getStorage().getBackupFolderPath());
                }


            }
            it.close();
            System.out.println("<FileStorage> Cleanup FileStorage with no valid Files:" + deleted);
        }


        {// Tabelle file:
            // -entferne Einträge die nicht mit file_backupobj verknüpft sind 
            //-Entferne File die keine Blöcke mehr haben, aber welche aufweisen sollten...//Files ohne file_storage Eintrag UND ohne blöcke
            CloseableIterator<File> it = DatabaseService.getInstance().getFileDaoImpl().queryBuilder().where().notIn("id", DatabaseService.getInstance().getFileBackupobjDaoImpl().queryBuilder().selectColumns("file_id")).and().notIn("id", DatabaseService.getInstance().getFileBlockDaoImpl().queryBuilder().selectColumns("file_id")).and().notIn("id", DatabaseService.getInstance().getFileBlockDaoImpl().queryBuilder().selectColumns("block_id")).iterator();
            //SELECT * FROM `file` WHERE ((`id` NOT IN (SELECT `file_id` FROM `file_backupobj` ) AND `id` NOT IN (SELECT `file_id` FROM `file_block` ) ) AND `id` NOT IN (SELECT `block_id` FROM `file_block` ) )
            //it= Files die mit nichts außer Storage verknüpft sind
            System.out.println("<File> <File_Storage> Cleanup in progress: ");
            long file_count = 0;
            long storage_count = 0;
            java.io.File file_to_delete = null;
            while (it.hasNext()) {
                File file = it.next();
                //storage-einträge nachschlagen und gegebenenfalls löschen
                CloseableIterator<FileStorage> it_fs = DatabaseService.getInstance().getFileStorageDaoImpl().queryBuilder().where().eq("file_id", file.getId()).iterator();
                boolean filedelete = true;
                while (it_fs.hasNext()) {
                    FileStorage fs = it_fs.next();
                    DatabaseService.getInstance().getStorageDaoImpl().refresh(fs.getStorage());
                    Storage storage = fs.getStorage();
                    file_to_delete = new java.io.File(storage.getBackupFolderPath() + fs.getFilePath() + java.io.File.separator + fs.getFile().getId() + ".jpg");
                    if (fs.getStorage().getBackupFolderPath().exists()) {
                        //System.out.println("delete storagefile");
                        filedelete = file_to_delete.delete();
                        if (filedelete || !file_to_delete.exists()) {
                            //storageeintrag entfernen
                            storage.setUsedSpace(storage.getUsedSpace() - fs.getFileSize());
                            if (DatabaseService.getInstance().getFileStorageDaoImpl().delete(fs) > 0) {
                                DatabaseService.getInstance().getStorageDaoImpl().update(storage);
                                storage_count++;
                            }

                        } else {


                        }
                    } else {
                        //storage ist nicht angeschlossen
                        filedelete = false;
                    }

                }
                if (filedelete) {
                    //fileeintrag entfernen
                    if (DatabaseService.getInstance().getFileDaoImpl().delete(file) > 0) {
                        file_count++;
                    }
                    System.out.print("\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b<File>|<File_Storage> deleted: " + file_count + "|" + storage_count + "");
                } else {
                    System.out.println("\nCan't delete:" + file_to_delete.getAbsolutePath());
                }
                it_fs.close();
            }
            it.close();
            System.out.print("\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b<File>|<File_Storage> deleted: " + file_count + "|" + storage_count + "\n");

        }

    }
}
