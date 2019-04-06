/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.blucou.backup;

import com.blucou.backup.DomainClasses.FileBackupobj;
import com.blucou.backup.DomainClasses.Storage;
import com.j256.ormlite.stmt.UpdateBuilder;
import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.io.FileUtils;
import org.bouncycastle.crypto.InvalidCipherTextException;
import org.bouncycastle.openpgp.PGPException;
import org.junit.*;
import org.junit.rules.TemporaryFolder;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URISyntaxException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.spec.InvalidKeySpecException;
import java.sql.SQLException;
import java.util.Iterator;
import java.util.logging.Level;
import java.util.logging.Logger;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * @author Fuzolan
 */
public class BlucouBackupTest {

    BackupObjService backupObjService;

    DatabaseService databaseService;

    File pathToTestData;

    StorageService storageService;

    Storage storage;

    CryptoService cryptoService;

    CryptoKeyService cryptoKeyService;

    int blocksize;

    public BlucouBackupTest() {


    }

    @BeforeClass
    public static void setUpClass() throws Exception {

    }

    @AfterClass
    public static void tearDownClass() throws Exception {

    }

    @Before
    public void setUp() throws SQLException, IOException, NoSuchAlgorithmException, ConfigurationException, InterruptedException, URISyntaxException {

        ConfigurationService cfg = ConfigurationService.getInstance();
        cfg.init();


        pathToTestData = new java.io.File(BlucouBackupTest.class.getProtectionDomain().getCodeSource().getLocation().toURI().getPath() +  java.io.File.separator + "testdata");
        pathToTestData.mkdir();

        java.io.File node = new File(this.pathToTestData, "destination");
        node.mkdir();
        deleteAndCreateFiles(node);
        node = new File(this.pathToTestData, "restore");
        deleteAndCreateFiles(node);
        node = new File(this.pathToTestData, "source");
        deleteAndCreateFiles(node);
        node = new File(node, "folder");
        node.mkdir();
        blocksize = ConfigurationService.getInstance().getConfig().getInt("blockSize");
        blocksize = ConfigurationService.getInstance().getConfig().getInt("blockSize");
        ConfigurationService.getInstance().getConfig().setProperty("blockSize", 128);
        databaseService = DatabaseService.getInstance();
        databaseService.closeDatabase();
        //databaseService.initDatabase("mysql", "192.168.0.100", "blucoubackup_dev", "root", "1234", true, true);
        databaseService.initDatabase("sqlite", "testdatabase.db", "", "", "", true, true);

        backupObjService = new BackupObjService();
        backupObjService.init("xyztestmaschine");


        storageService = new StorageService();
        storage = storageService.initStorage(new File(pathToTestData, "destination").getAbsolutePath());

        cryptoService = CryptoService.getInstance();

        String pass = "test123";

        try {
            CryptoKeyService cryptoKeyService = CryptoKeyService.getInstance();
            cryptoKeyService.init(pass);
        } catch (Exception ex) {
            Logger.getLogger(BlucouBackupTest.class.getName()).log(Level.SEVERE, null, ex);
        }

    }

    private void deleteAndCreateFiles(File node) throws InterruptedException {

        int i = 0;
        while (node.exists()) {

            try {
                FileUtils.deleteDirectory(node);
            } catch (IOException e) {
                e.printStackTrace();
                Thread.sleep(1000);
            }

        }

        node.mkdir();
    }

    @After
    public void tearDown() throws IOException {

        ConfigurationService.getInstance().getConfig().setProperty("blockSize", blocksize);

    }

    @Test
    public void cryptoTest() throws NoSuchAlgorithmException, InvalidKeySpecException, IOException, InvalidCipherTextException, InvalidKeyException, UnsupportedEncodingException, IllegalBlockSizeException, BadPaddingException, NoSuchPaddingException, NoSuchProviderException, PGPException, Exception {
        File fileToCrypt = new File(this.pathToTestData, "source" + File.separator + "cryptdata.txt");
        File fileAfterCrypt = new File(this.pathToTestData, "source" + File.separator + "cryptdata.crypt");
        File fileAfterEnCrypt = new File(this.pathToTestData, "source" + File.separator + "encryptdata.txt");
        for (int i = 0; i < 100; i++) {
            FileUtils.writeStringToFile(fileToCrypt, "Das sind ein paar Testdaten\n", true);
        }
        for (int i = 0; i < 100; i++) {
            FileUtils.writeStringToFile(fileToCrypt, "Andere TESTDATEN" + i + "\n", true);
        }

        cryptoService.encrypt(fileToCrypt, fileAfterCrypt);
        cryptoService.decrypt(fileAfterCrypt, fileAfterEnCrypt, false);
        assertTrue(FileUtils.contentEquals(fileToCrypt, fileAfterEnCrypt));
    }


    /**
     * Test of backupfunktionality
     */
    @Test
    public void backupTest() throws IOException, SQLException, NoSuchAlgorithmException, InvalidKeySpecException, InvalidCipherTextException, Exception {

        System.out.println("checkFS");
        //Dateien erzeugen
        File fileToBackup = new File(this.pathToTestData, "source" + File.separator + "testdata.txt");
        FileUtils.writeStringToFile(fileToBackup, "Das sind ein paar Testdaten");

        java.io.File node = new File(pathToTestData, "source");
        backupObjService.backup(node);
        assertEquals(databaseService.getFileDaoImpl().countOf(), 1);
        assertEquals(databaseService.getFileStorageDaoImpl().countOf(), 1);
        assertEquals(2, databaseService.getFileBackupobjDaoImpl().countOf());


        //pr?fen ob Datei am Backupziel vorhanden ist
        //pr?fen ob kopierte Datei ignoriert wird
        //pr?fen ob neue Datei erkannt wird aufgenommen wird und richtig abgespeichert wird
        FileUtils.copyFile(fileToBackup, new File(node, "testdata2.txt"));
        backupObjService.backup(node);
        assertEquals(databaseService.getFileDaoImpl().countOf(), 1);
        assertEquals(databaseService.getFileStorageDaoImpl().countOf(), 1);
        assertEquals(3, databaseService.getFileBackupobjDaoImpl().countOf());
        com.blucou.backup.DomainClasses.File file = FileService.getFileByPath(this.backupObjService.getBackupobj(), fileToBackup.toURI());
        File backupedFile = new File(storageService.getAbsolutePathOfFile(storage, file));

        //Bestehende Datei ver?ndern und ?berpr?fen ob Obsolete Eintrag erstellt wurde
        FileUtils.writeStringToFile(fileToBackup, "Das sind ein paar TestdatenXY");
        backupObjService.backup(node);
        assertEquals(databaseService.getFileDaoImpl().countOf(), 2);
        assertEquals(databaseService.getFileStorageDaoImpl().countOf(), 2);
        assertEquals(4, databaseService.getFileBackupobjDaoImpl().countOf());
        try {
            Thread.sleep(2000);
        } catch (InterruptedException ex) {
            Logger.getLogger(BlucouBackupTest.class.getName()).log(Level.SEVERE, null, ex);
        }
        fileToBackup.delete();
        backupObjService.backup(node);
    }


    @Test
    public void cleanupTest() throws Exception {

        restoreTest();

        assertEquals(0, storageService.validateBackupFiles());

        //ung?ltiger fileeintrag
        com.blucou.backup.DomainClasses.File file = new com.blucou.backup.DomainClasses.File();
        databaseService.getFileDaoImpl().create(file);
        long rowcount = databaseService.getFileDaoImpl().countOf();
        storageService.cleanup();
        assertEquals(rowcount - 1, databaseService.getFileDaoImpl().countOf());

        //filebackupobj mit file_id = null (abbruch beim splitten)
        long id = databaseService.getFileBackupobjDaoImpl().iterator().first().getId();
        UpdateBuilder<FileBackupobj, Long> updateBuilder = databaseService.getFileBackupobjDaoImpl().updateBuilder();
        updateBuilder.updateColumnValue("file_id", null);
        updateBuilder.where().idEq(id);
        updateBuilder.update();
        rowcount = databaseService.getFileBackupobjDaoImpl().countOf();
        storageService.cleanup();
        assertEquals(rowcount, databaseService.getFileBackupobjDaoImpl().countOf());

        java.io.File node = new File(this.pathToTestData, "destination");
        Iterator<File> it = FileUtils.iterateFiles(node, null, true);
        while (it.hasNext()) {
            java.io.File afile = it.next();
            FileUtils.forceDelete(afile);
            System.out.println(afile.getAbsoluteFile());
        }
        assertEquals(7, storageService.validateBackupFiles());


    }

    /**
     * Test of restorefunktionality
     */
    //@Test
    // wird implizit �ber cleanup mitgetestet
    public void restoreTest() throws IOException, SQLException, NoSuchAlgorithmException, InvalidKeySpecException, InvalidCipherTextException, Exception {

        System.out.println("checkFS");
        //Dateien erzeugen
        File fileToBackup = new File(this.pathToTestData, "source" + File.separator + "testdata.txt");
        File fileToBackup_split = new File(this.pathToTestData, "source" + File.separator + "testdata_split.txt");
        FileUtils.writeStringToFile(fileToBackup, "Das sind ein paar Testdaten");

        java.io.File node = new File(pathToTestData, "source");
        //FileService instance = new FileService(this.backupObjService.getBackupobj());
        backupObjService.backup(node);


        //instance.checkFS(node);
        assertEquals(1, databaseService.getFileDaoImpl().countOf());
        assertEquals(1, databaseService.getFileStorageDaoImpl().countOf());
        assertEquals(2, databaseService.getFileBackupobjDaoImpl().countOf());


        //pr?fen ob Datei am Backupziel vorhanden ist
        //pr?fen ob kopierte Datei ignoriert wird
        //pr?fen ob neue Datei erkannt wird aufgenommen wird und richtig abgespeichert wird
        FileUtils.copyFile(fileToBackup, new File(node, "testdata2äöüß'.txt"));
        //FileUtils.copyFile(fileToBackup_split, new File(node,"testdata_split.txt"));
        FileUtils.writeStringToFile(fileToBackup, "Das sind ein paar Testdaten2123123");

        backupObjService.backup(node);


        assertEquals(databaseService.getFileDaoImpl().countOf(), 2);
        assertEquals(databaseService.getFileStorageDaoImpl().countOf(), 2);
        assertEquals(4, databaseService.getFileBackupobjDaoImpl().countOf());

        //assertTrue( FileUtils.contentEquals(fileToBackup, backupedFile));

        //Bestehende Datei ver?ndern und ?berpr?fen ob Obsolete Eintrag erstellt wurde
        FileUtils.writeStringToFile(fileToBackup, "Das sind ein paar TestdatenXY");
        for (int i = 0; i < 10000; i++) {
            FileUtils.writeStringToFile(fileToBackup_split, "Das sind ein paar Testdaten\n", true);
        }
        for (int i = 0; i < 10000; i++) {
            FileUtils.writeStringToFile(fileToBackup_split, "Andere TESTDATEN" + i + "\n", true);
        }

        com.blucou.backup.DomainClasses.File file = FileService.getFileByPath(this.backupObjService.getBackupobj(), fileToBackup.toURI());
        File backupedFile = new File(storageService.getAbsolutePathOfFile(storage, file));

        backupObjService.backup(node);
        assertEquals(databaseService.getFileDaoImpl().countOf(), 8);
        assertEquals(databaseService.getFileStorageDaoImpl().countOf(), 7);
        assertEquals(databaseService.getFileBlockDaoImpl().countOf(), 4);
        assertEquals(6, databaseService.getFileBackupobjDaoImpl().countOf());

        long fileToBackup_splitChecksum = FileUtils.checksumCRC32(fileToBackup_split);
        long fileToBackupChecksum = FileUtils.checksumCRC32(fileToBackup);

        fileToBackup.delete();
        backupObjService.backup(node, true); //gel?schte markieren

        assertEquals(databaseService.getFileBackupobjDaoImpl().queryBuilder().where().eq("deleted", true).countOf(), 3);
        assertEquals(databaseService.getFileBackupobjDaoImpl().queryBuilder().where().eq("obsolete", true).countOf(), 2);


        //Eigentlicher RESTORE
        //Thread.sleep(120 * 1000);
        java.io.File restore = new File(pathToTestData, "restore");
        assertEquals(storageService.restoreFromStorage(backupObjService.getBackupobj(), restore, fileToBackup.toURI().toString(), false, 0), 0);
        assertEquals(storageService.restoreFromStorage(backupObjService.getBackupobj(), restore, fileToBackup.toURI().toString(), true, 0), 1);
        assertEquals(2, StorageService.restoreFromStorage(backupObjService.getBackupobj(), restore, "", true, 0));

        //pr?fen ob maxobsolete passt
        assertEquals(StorageService.restoreFromStorage(backupObjService.getBackupobj(), restore, "", true, 1), 1);
        assertEquals(StorageService.restoreFromStorage(backupObjService.getBackupobj(), restore, "", true, 2), 1);


        //content zerst?ckelt vergleichen
        assertEquals(fileToBackup_splitChecksum, FileUtils.checksumCRC32(new java.io.File(restore, fileToBackup_split.getAbsolutePath().replaceAll("[:*\"\\?<>|]", "_"))));
        //content einfach vergleichen
        assertEquals(fileToBackupChecksum, FileUtils.checksumCRC32(new java.io.File(restore, fileToBackup.getAbsolutePath().replaceAll("[:*\"\\?<>|]", "_"))));


        //todo preserve timestamps

    }

    @Test
    public void printTest() {

        int z = 3;
        for (int i = 0; i <= 3; i++) {
            BlucouBackupCLIOptions.printProgress(3, i, 0);
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }
}
