/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.blucou.backup;

import com.blucou.backup.DomainClasses.*;
import com.blucou.backup.DomainClasses.File;
import com.j256.ormlite.dao.CloseableIterator;
import com.j256.ormlite.stmt.QueryBuilder;
import com.j256.ormlite.stmt.SelectArg;
import com.j256.ormlite.stmt.Where;
import org.apache.commons.io.filefilter.FileFilterUtils;
import org.apache.commons.io.filefilter.IOFileFilter;

import java.io.*;
import java.math.BigInteger;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.DirectoryStream;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.zip.CRC32;
import java.util.zip.Checksum;

/**
 * @author Fuzolan
 */
public class FileService {

    Backupobj backupobj;

    List<FileBackupobj> cache = null;

    long i = 0;


    public FileService(Backupobj backupobj) {
        this.backupobj = backupobj;
    }

    public static boolean validateFile(long fileid, java.io.File node) throws FileNotFoundException, NoSuchAlgorithmException, SQLException, IOException {

        InputStream fis = new FileInputStream(node.getAbsolutePath());
        Checksum crc32_block = new CRC32();

        QueryBuilder q = DatabaseService.getInstance().getFileBlockDaoImpl().queryBuilder();
        q.where().eq("file_id", fileid);
        q.orderBy("blockNr", true);
        CloseableIterator<FileBlock> it = q.iterator();

        while (it.hasNext()) {
            FileBlock fb = it.next();
            DatabaseService.getInstance().getFileDaoImpl().refresh(fb.getBlock());
            byte[] buffer = new byte[(int) fb.getBlock().getFileSize()];
            fis.read(buffer, 0, (int) fb.getBlock().getFileSize());
            crc32_block.update(buffer, 0, (int) fb.getBlock().getFileSize());
            long crc = crc32_block.getValue();
            if (crc != Long.parseLong(fb.getBlock().getCrc32())) {
                System.out.println("Error_Block:" + fb.getBlockNr() + ": CRC32:" + fb.getBlock().getCrc32() + " ---READ:" + crc);
                return false;
            }
            crc32_block.reset();
            System.out.print(fb.getBlockNr() + "\r");
        }
        return true;
    }

    public static File getFileByPath(Backupobj backupobj, URI absolutePath) throws SQLException {

        FileBackupobj returnfile = DatabaseService.getInstance().getFileBackupobjDaoImpl().queryBuilder().where().eq("backupobj_id", backupobj.getId()).and().eq("absolutePath", absolutePath.toString()).queryForFirst();
        DatabaseService.getInstance().getFileDaoImpl().refresh(returnfile.getFile());
        return returnfile.getFile();
    }

    public static ArrayList<String> createChecksum(java.io.File file) {

        try {
            long timeStart = System.currentTimeMillis();
            InputStream fis = new FileInputStream(file.getAbsolutePath());
            ArrayList<String> hashes = new ArrayList<String>();

            byte[] buffer = new byte[4096];
            MessageDigest sha512 = MessageDigest.getInstance("SHA-512");
            MessageDigest md5 = MessageDigest.getInstance("md5");
            MessageDigest sha1 = MessageDigest.getInstance("sha1");
            int numRead;

            do {
                numRead = fis.read(buffer);
                if (numRead > 0) {
                    sha512.update(buffer, 0, numRead);
                    md5.update(buffer, 0, numRead);
                    sha1.update(buffer, 0, numRead);

                    //Wenn Blockgenerierung Aktiv ist, dann
                    //java.io.File.createTempFile(file., filename)
                }
            } while (numRead != -1);

            fis.close();
            hashes.add(new BigInteger(1, md5.digest()).toString(16));
            hashes.add(new BigInteger(1, sha512.digest()).toString(16));
            hashes.add(new BigInteger(1, sha1.digest()).toString(16));
            Logger.getLogger(BlucouBackup.class.getName()).log(Level.FINE, "Hashgeneration completed in " + (System.currentTimeMillis() - timeStart) + "ms");
            return hashes;
        } catch (Exception e) {
            Logger.getLogger(BlucouBackup.class.getName()).log(Level.SEVERE, null, e);
            return null;
        }

    }

    /**
     * Scannt das Verzeichnis und fügt sie bei Bedarf der Datenbank hinzu
     *
     * @param node
     */
    public void checkFS(java.io.File node, IOFileFilter fileFilter, IOFileFilter dirFilter) {

        try {
            final int checkDirectoryIntervalInDays = 1000 * 60 * 60 * 24 * Integer.valueOf(ConfigurationService.getInstance().readcfg("checkDirectoryIntervalInDays"));
            Path dir = FileSystems.getDefault().getPath(node.getAbsolutePath());
            DirectoryStream<Path> stream = Files.newDirectoryStream(dir);
            for (Path path : stream) {
                System.out.print(i++ + " Files processed\r");
                org.apache.log4j.Logger.getLogger(this.getClass().getName()).debug(path.toFile().getAbsolutePath());
                if (!Files.isSymbolicLink(path)) //Ausschluss von symbolischen Links
                {
                    boolean isReadable = Files.isReadable(path);
                    java.io.File fsItem = path.toFile();
                    if (Files.isDirectory(path) && isReadable) {
                        FileBackupobj fileBackupobj = DatabaseService.getInstance().getFileBackupobjDaoImpl().queryBuilder().where().
                                eq("backupobj_id", this.backupobj.getId()).and().
                                eq("absolutePath", fsItem.toURI().toString()).queryForFirst();
                        java.io.File[] filteredDir = FileFilterUtils.filter(dirFilter, fsItem);
                        if (filteredDir.length <= 0 || (fileBackupobj != null && checkDirectoryIntervalInDays != 0 && fileBackupobj.getLastchecked().getTime() + checkDirectoryIntervalInDays > (new Date()).getTime())) {
                            org.apache.log4j.Logger.getLogger(this.getClass().getName()).info("skipped: " + fsItem.getName());
                            continue;
                        }

                        checkFS(fsItem, fileFilter, dirFilter);
                        createNewFileBackupobjForDirectory(fsItem);
                    } else {
                        //Node ist eine Datei und muss überprüft werden: Bereits in Datenbank? Wurde geändert?
                        java.io.File[] filteredFile = FileFilterUtils.filter(fileFilter, fsItem);
                        if (filteredFile.length <= 0) continue;

                        //überprüfe ob Datei gelesen werden kann
                        if (isReadable) {
                            checkFileBackupobjEntry(fsItem);
                        } else {
                            org.apache.log4j.Logger.getLogger(this.getClass().getName()).warn("Can't read file: " + fsItem.getAbsolutePath());
                        }
                    }
                }
            }
            stream.close();
        } catch (Exception e) {
            org.apache.log4j.Logger.getLogger(this.getClass().getName()).fatal(e);
        }
    }

    public void markDeletedFiles(Backupobj backupobj, String path) throws SQLException, URISyntaxException {

        System.out.println("Mark deleted Files");
        List<FileBackupobj> filebackupobjs;


        Where<FileBackupobj, Long> queryStatement = DatabaseService.getInstance().getFileBackupobjDaoImpl().queryBuilder().orderBy("absolutePath", false).orderBy("lastbackup", false).where();
        queryStatement.eq("backupobj_id", backupobj.getId());
        queryStatement.and().like("absolutePath", path + "%");
        queryStatement.and().ne("deleted", true);


        long i = 0;

        CloseableIterator<FileBackupobj> it = queryStatement.iterator();
        try {
            while (it.hasNext()) {
                FileBackupobj fileBackupobj = it.next();

                java.io.File file = new java.io.File(new URI(fileBackupobj.getAbsolutePath()));
                if (!file.exists()) {
                    System.out.println("Mark as deleted: " + file.getAbsolutePath());//deleted
                    fileBackupobj.setDeleted(true);
                    DatabaseService.getInstance().getFileBackupobjDaoImpl().update(fileBackupobj);
                } else {
                }

            }
        } finally {
            // close it at the end to close underlying SQL statement
            it.close();
        }

    }

    /**
     * überprüft ob ein Eintrag besteht und fügt in bei Bedarf hinzu
     */
    public FileBackupobj checkFileBackupobjEntry(java.io.File node) throws SQLException, NoSuchAlgorithmException, UnsupportedEncodingException, IOException {

        FileBackupobj fileBackupobj = null;

        SelectArg selectArgAbsolutePath = new SelectArg(node.toURI().toString());

        buildupFileBackupobjCache(node);

        fileBackupobj = lookupFileBackupobjCache(fileBackupobj, selectArgAbsolutePath);

        fileBackupobj = getFileBackupobjFromDatabaseIfNeeded(fileBackupobj, selectArgAbsolutePath);

        if (fileBackupobj == null) {
            fileBackupobj = createNewFileBackupobj(node);

        } else {
            //Eintrag besteht...

            DatabaseService.getInstance().getFileDaoImpl().refresh(fileBackupobj.getFile());

            //Überprüfen ob sich Datei geändert hat
            if (fileBackupobj.getUpdated() != node.lastModified() || fileBackupobj.getFile().getFileSize() != node.length()) {
                //Datei hat sich geändert...

                //erst auf obsolete setzen wenn file auch wirklich unterschiedlich
                fileBackupobj = updateFileBackupobj(node, fileBackupobj);
            } else {
                //Datei nicht geändert
                //Timestamp aktuallisieren
                fileBackupobj.setLastchecked(new java.util.Date());
            }
        }
        return fileBackupobj;
    }

    private FileBackupobj updateFileBackupobj(java.io.File node, FileBackupobj fileBackupobj) throws IOException, NoSuchAlgorithmException, SQLException {

        File file = createMultipleFileEntries(node);
        if (fileBackupobj.getFile().getId() != file.getId()) {
            //Alte Zuordnung auf obsolete stellen
            fileBackupobj.setObsolete(true);
            DatabaseService.getInstance().getFileBackupobjDaoImpl().update(fileBackupobj);

            //Neuen Eintrag erstellen
            fileBackupobj = new FileBackupobj();
            fileBackupobj.setCreated(new java.util.Date());
            fileBackupobj.setDeleted(false);
            fileBackupobj.setFile(file);
            fileBackupobj.setBackupobjId(this.backupobj.getId());
            fileBackupobj.setAbsolutePath(node.toURI().toString());
            fileBackupobj.setGroup("-");
            fileBackupobj.setName(node.getName());
            fileBackupobj.setOwner("-");
            fileBackupobj.setRights(777);
            fileBackupobj.setLastchecked(new java.util.Date());
            fileBackupobj.setLastbackup(new java.util.Date());
            fileBackupobj.setUpdated(node.lastModified());
            fileBackupobj.setModifiedCounter(0);
            if (fileBackupobj.getFile() != null) {
                DatabaseService.getInstance().getFileBackupobjDaoImpl().create(fileBackupobj);
            } else {
                org.apache.log4j.Logger.getLogger(this.getClass().getName()).warn("Something goes wrong with " + node.getAbsolutePath());
                return null;
            }
        } else {
            //nur timer wurde geändert...und wird entsprechend angepasst
            fileBackupobj.setUpdated(node.lastModified());
            DatabaseService.getInstance().getFileBackupobjDaoImpl().update(fileBackupobj);
        }
        return fileBackupobj;
    }

    private FileBackupobj createNewFileBackupobj(java.io.File node) throws IOException, NoSuchAlgorithmException, SQLException {

        FileBackupobj fileBackupobj;//nicht vorhanden ...muss erstellt werden

        //File muss ebenfalls erstellt werden

        //Datei in Datenbank schreiben
        fileBackupobj = new FileBackupobj();
        fileBackupobj.setCreated(new java.util.Date());
        fileBackupobj.setDeleted(false);
        fileBackupobj.setFile(createMultipleFileEntries(node));
        fileBackupobj.setBackupobjId(this.backupobj.getId());
        fileBackupobj.setAbsolutePath(node.toURI().toString());
        fileBackupobj.setGroup("-");
        fileBackupobj.setName(node.getName());
        fileBackupobj.setOwner("-");
        fileBackupobj.setRights(777);
        fileBackupobj.setLastchecked(new java.util.Date());
        fileBackupobj.setLastbackup(new java.util.Date());
        fileBackupobj.setUpdated(node.lastModified());
        fileBackupobj.setModifiedCounter(0);
        if (fileBackupobj.getFile() != null) {
            DatabaseService.getInstance().getFileBackupobjDaoImpl().create(fileBackupobj);
        } else {
            org.apache.log4j.Logger.getLogger(this.getClass().getName()).warn("Something goes wrong with " + node.getAbsolutePath());
            return null;
        }
        return fileBackupobj;
    }

    private FileBackupobj createNewFileBackupobjForDirectory(java.io.File node) throws IOException, NoSuchAlgorithmException, SQLException {

        FileBackupobj fileBackupobj;//nicht vorhanden ...muss erstellt werden

        //File muss ebenfalls erstellt werden
        fileBackupobj = DatabaseService.getInstance().getFileBackupobjDaoImpl().queryBuilder().where().
                eq("backupobj_id", this.backupobj.getId()).and().
                eq("absolutePath", node.toURI().toString()).queryForFirst();
        //Datei in Datenbank schreiben
        if (fileBackupobj == null) {
            fileBackupobj = new FileBackupobj();
            fileBackupobj.setCreated(new java.util.Date());
            fileBackupobj.setDeleted(false);
            fileBackupobj.setFile(null);
            fileBackupobj.setBackupobjId(this.backupobj.getId());
            fileBackupobj.setAbsolutePath(node.toURI().toString());
            fileBackupobj.setGroup("-");
            fileBackupobj.setName("");
            fileBackupobj.setOwner("-");
            fileBackupobj.setRights(777);
            fileBackupobj.setLastchecked(new java.util.Date());
            fileBackupobj.setLastbackup(new java.util.Date());
            fileBackupobj.setUpdated(node.lastModified());
            fileBackupobj.setModifiedCounter(0);

            DatabaseService.getInstance().getFileBackupobjDaoImpl().create(fileBackupobj);
        } else {
            fileBackupobj.setLastchecked(new java.util.Date());
            fileBackupobj.setLastbackup(new java.util.Date());
            fileBackupobj.setUpdated(node.lastModified());
            DatabaseService.getInstance().getFileBackupobjDaoImpl().update(fileBackupobj);

        }

        return fileBackupobj;
    }

    private FileBackupobj getFileBackupobjFromDatabaseIfNeeded(FileBackupobj fileBackupobj, SelectArg selectArgAbsolutePath) throws SQLException {

        if (fileBackupobj == null) {
            //war wohl nicht auffindbar
            cache = null;
            fileBackupobj = DatabaseService.getInstance().getFileBackupobjDaoImpl().queryBuilder().where().eq("absolutePath", selectArgAbsolutePath).and().eq("backupobj_id", this.backupobj.getId()).and().eq("obsolete", false).and().eq("deleted", false).queryForFirst();
        }
        return fileBackupobj;
    }

    private void buildupFileBackupobjCache(java.io.File node) throws SQLException {

        if (this.cache == null) {
            SelectArg selectArgAbsolutePathCache = new SelectArg(node.getParentFile().toURI().toString() + "%");
            cache = DatabaseService.getInstance().getFileBackupobjDaoImpl().queryBuilder().limit(100).where().like("absolutePath", selectArgAbsolutePathCache).and().eq("backupobj_id", this.backupobj.getId()).and().eq("obsolete", false).and().eq("deleted", false).query();

        }
    }

    private FileBackupobj lookupFileBackupobjCache(FileBackupobj fileBackupobj, SelectArg selectArgAbsolutePath) {

        //in Cache nachsehen
        for (FileBackupobj f : cache) {
            if (f.getAbsolutePath().equals(selectArgAbsolutePath.toString())) {
                fileBackupobj = f;
                break;
            }
        }
        return fileBackupobj;
    }

    public boolean isReadyForBackup(java.io.File node) {


        return true;
    }

    /**
     * @param node
     * @return Haupt-File welches die Verlinkung zu anderen Blocks enthält
     * Sofern file eine block-id hat ist, wurde sie gesplittet
     */
    private File createMultipleFileEntries(java.io.File node) throws IOException, NoSuchAlgorithmException, SQLException {

        System.out.println(node.getAbsolutePath() + "\r");
        File rootFile = new File();
        try {

            final ExecutorService blockWriterThreads = Executors.newFixedThreadPool(4);
            final boolean onlyMainThread = ConfigurationService.getInstance().readcfg("dbtype").equals("sqlite");
            //Überfile erstellen
            rootFile.setFileSize(node.length());
            rootFile.setMd5("");
            rootFile.setSha512("");

            //Die Datei wird eingelesen und es werden verschiedene Hashes erzeugt.
            //GesamtHashes und Blockhashes
            //Blocks werden temporär gespeichert und dann zum Ziel übertragen
            long timeStart = System.currentTimeMillis();
            InputStream fis = new FileInputStream(node.getAbsolutePath());

            MessageDigest sha512 = MessageDigest.getInstance("SHA-512");
            MessageDigest md5 = MessageDigest.getInstance("md5");
            Checksum crc32 = new CRC32();

            MessageDigest sha512_block = MessageDigest.getInstance("SHA-512");
            MessageDigest md5_block = MessageDigest.getInstance("md5");
            Checksum crc32_block = new CRC32();


            int numRead;
            int boLength = 0;
            ByteArrayOutputStream bo = null;

            long block_num = 1;
            long rounds_completed = 0;
            long treshold_in_kb = ConfigurationService.getInstance().getConfig().getLong("blockSize");
            long rounds_for_block;
            boolean blockGeneration = node.length() > (1024 * treshold_in_kb);
            long blocks = (long) Math.ceil((float) node.length() / (float) (1024 * treshold_in_kb));
            long oldBlock = 0;
            long newBlock = 0;
            byte[] buffer;
            ArrayList<Long> fileblocks = new ArrayList<Long>();

            buffer = new byte[32768];
            rounds_for_block = treshold_in_kb / 32;//


            java.io.File blockFile = null;
            Storage storage_For_Block = StorageService.getPossibleStorage(treshold_in_kb * 1024);

            bo = new ByteArrayOutputStream((int) treshold_in_kb * 1024);//
            do {
                numRead = fis.read(buffer);
                if (numRead > 0) {

                    boLength += numRead;
                    bo.write(buffer, 0, numRead);

                    sha512.update(buffer, 0, numRead);
                    md5.update(buffer, 0, numRead);
                    crc32.update(buffer, 0, numRead);

                    if (blockGeneration) {
                        sha512_block.update(buffer, 0, numRead);
                        md5_block.update(buffer, 0, numRead);
                        crc32_block.update(buffer, 0, numRead);


                    }

                    rounds_completed++;
                }

                if (blockGeneration && numRead != -1 && (rounds_completed % rounds_for_block == 0) || (blockGeneration && numRead < 0 && boLength > 0)) {//Blockende erreicht || Ende der Datei erreicht ---block_num > 1, weil eventuell ein normaler FileEintrag auch ausreicht
                    //Überprüfen ob Block überhaupt gespeichert werden muss
                    String md5_block_string = new BigInteger(1, md5_block.digest()).toString(16);
                    String sha512_block_string = new BigInteger(1, sha512_block.digest()).toString(16);
                    String crc32_block_string = String.valueOf(crc32_block.getValue());

                    File file = DatabaseService.getInstance().getFileDaoImpl().queryBuilder().where().eq("md5", md5_block_string).and().eq("crc32", crc32_block_string).and().eq("sha512", sha512_block_string).and().eq("filesize", boLength).queryForFirst();
                    if (file != null) {
                        //Datei besteht bereits und muss nicht neu abgelegt werden

                        oldBlock++;
                    } else {
                        //Date besteht noch nicht und muss angelegt werden

                        file = new File();
                        file.setMd5(md5_block_string);
                        file.setCrc32(crc32_block_string);
                        file.setSha512(sha512_block_string);
                        file.setFileSize(boLength);
                        DatabaseService.getInstance().getFileDaoImpl().create(file);

                        //tmp kann kopiert werden (Ist File-Eintrag bereits vorhanden, kann trotzdem vorkommen, dass Datei nicht gesichert wurde)
                        if (!StorageService.checkStorage(storage_For_Block, boLength)) {
                            storage_For_Block = StorageService.getPossibleStorage(boLength);
                        }

                        if (onlyMainThread) {
                            (new BlockWriterRunnable(storage_For_Block, file, bo.toByteArray(), boLength)).run();
                        } else {
                            blockWriterThreads.execute(new BlockWriterRunnable(storage_For_Block, file, bo.toByteArray(), boLength));
                        }
                        newBlock++;
                    }

                    //Erzeugen

                    //Erzeugung wurde ausgelagert...mögliches Speicherproblem bei sehr kleiner Blocksize und großen Dateien
                    fileblocks.add(file.getId());

                    //Für neuen Block alles vorbereiten
                    sha512_block.reset();
                    md5_block.reset();
                    crc32_block.reset();
                    block_num++;
                    boLength = 0;
                    bo.reset();
                    BlucouBackupCLIOptions.printProgress(blocks, oldBlock, newBlock);
                }


            } while (numRead != -1); //nach dem jetzigen Konzept dürfte das Ding niemals zweimal durchlaufen

            if (!onlyMainThread) {
                blockWriterThreads.shutdown();
                blockWriterThreads.awaitTermination(Long.MAX_VALUE, TimeUnit.HOURS);
            }

            fis.close();
            fis = null;

            String md5_string = new BigInteger(1, md5.digest()).toString(16);
            String sha512_string = new BigInteger(1, sha512.digest()).toString(16);
            String crc32_string = String.valueOf(crc32.getValue());

            File probeRootFile = DatabaseService.getInstance().getFileDaoImpl().queryBuilder().where().eq("md5", md5_string).and().eq("crc32", crc32_string).and().eq("sha512", sha512_string).and().eq("filesize", node.length()).queryForFirst();
            if (probeRootFile == null) {
                //Root File besteht noch nicht und darf gespeichert werden
                rootFile.setMd5(md5_string);
                rootFile.setSha512(sha512_string);
                rootFile.setCrc32(crc32_string);
                DatabaseService.getInstance().getFileDaoImpl().create(rootFile);

                //In SChleife die Blocks erzeugen
                Iterator it = fileblocks.iterator();
                long i = 0;
                while (it.hasNext()) {
                    i++;
                    FileBlock fileBlock = new FileBlock();
                    File aFile = new File();
                    aFile.setId((Long) it.next());

                    fileBlock.setFile(rootFile);
                    fileBlock.setBlock(aFile);
                    fileBlock.setBlockNr(i);
                    DatabaseService.getInstance().getFileBlockDaoImpl().create(fileBlock);

                }


                //Versuch einzelne Datei zu sichern...wird sich zeigen ob Sicherung bereits besteht
                StorageService storageService;
                try {
                    if (block_num <= 1 && node.length() >= 0) {
                        StorageService.backupToStorage(StorageService.getPossibleStorage(node.length()), rootFile, bo.toByteArray(), boLength);
                        newBlock++;
                    }

                } catch (Exception e) {
                    Logger.getLogger(BlucouBackup.class.getName()).log(Level.SEVERE, null, e);
                    System.exit(1);
                }


            } else {
                //Root File besteht bereits...dies muss gelöscht werden und den Fileblock Eintrag ebenso


                //DatabaseService.getInstance().getFileDaoImpl().deleteById(rootFile.getId().toString());
                rootFile = probeRootFile; //Damit Backup funktionieren kann -> und file_id richtig in file_backupobj gesetzt wird
                oldBlock++;
            }
            BlucouBackupCLIOptions.printProgress(blocks, oldBlock, newBlock);

            if (bo != null) {
                bo.close();
                bo = null;
            }
            System.out.print("\n");
            org.apache.log4j.Logger.getLogger(this.getClass().getName()).debug("Block / Hashgeneration & Backup completed in " + (System.currentTimeMillis() - timeStart) + "ms");

            return rootFile;
        } catch (Exception e) {
            org.apache.log4j.Logger.getLogger(this.getClass().getName()).fatal(e);
            return null;
        }


    }

}
