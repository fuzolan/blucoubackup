/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.blucou.backup;

import com.blucou.backup.DomainClasses.Backupobj;
import org.apache.commons.io.filefilter.IOFileFilter;
import org.apache.commons.io.filefilter.NotFileFilter;
import org.apache.commons.io.filefilter.OrFileFilter;
import org.apache.commons.io.filefilter.WildcardFileFilter;

import java.io.IOException;
import java.math.BigInteger;
import java.net.URISyntaxException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * @author Fuzolan
 */
public class BackupObjService {
    private String identifier;
    private Backupobj backupobj;

    public void backup(java.io.File node) throws SQLException, URISyntaxException {
        this.backup(node, false);
    }

    public void backup(java.io.File node, boolean markDeleted) throws SQLException, URISyntaxException {
        if (backupobj == null) {
            System.err.println("INIT Backupobj!!!!");
        } else {
            java.util.Date lastRun = new java.util.Date();
            FileService fileService = new FileService(backupobj);


            //Filter

            //GenerateDirblacklist:
            Iterator dit = ConfigurationService.getInstance().getConfig().getList("dirBlacklist").iterator();
            List<IOFileFilter> dffList = new ArrayList<>();

            while (dit.hasNext()) {
                dffList.add(new WildcardFileFilter((String) dit.next()));
            }

            //GenerateFileblacklist:
            Iterator fit = ConfigurationService.getInstance().getConfig().getList("fileBlacklist").iterator();
            List<IOFileFilter> fffList = new ArrayList<>();

            while (fit.hasNext()) {
                fffList.add(new WildcardFileFilter((String) fit.next()));
            }


            fileService.checkFS(node, new NotFileFilter(new OrFileFilter(fffList)), new NotFileFilter(new OrFileFilter(dffList)));
            this.getBackupobj().setLastbackup(lastRun);
            DatabaseService.getInstance().getBackupobjDaoImpl().update(getBackupobj());
            if (markDeleted) {
                fileService.markDeletedFiles(getBackupobj(), node.toURI().toString());
            }

        }
    }

    public String initMachineIdentifier() throws NoSuchAlgorithmException, IOException {
        String identifier = "";
        ConfigurationService cfg = ConfigurationService.getInstance();
        if (((String) cfg.getConfig().getProperty("machine_identifier")) == null || ((String) cfg.getConfig().getProperty("machine_identifier")).isEmpty()) {
            //noch kein Machineidentifier gesetzt
            MessageDigest md = MessageDigest.getInstance("MD5");
            String stringToEncrypt = System.getProperty("os.version") + (new java.util.Date());
            md.update(stringToEncrypt.getBytes(), 0, stringToEncrypt.length());
            identifier = new BigInteger(1, md.digest()).toString(16);
            cfg.getConfig().setProperty("machine_identifier", identifier);
            System.out.println("Machine identifier was created and saved to config");

        } else {
            //idenftifier vorhanden
            identifier = cfg.getConfig().getString("machine_identifier");
        }

        System.out.println("Identifier is " + identifier);

        return identifier;

    }

    public Backupobj init() throws NoSuchAlgorithmException, SQLException, IOException {
        return init(this.initMachineIdentifier());

    }

    public Backupobj init(String identifier) throws SQLException, IOException, NoSuchAlgorithmException {
        this.setIdentifier(identifier);

        this.setBackupobj(DatabaseService.getInstance().getBackupobjDaoImpl().queryBuilder().where().eq("client_identifier", this.getIdentifier()).queryForFirst());
        if (getBackupobj() == null) {
            setBackupobj(new Backupobj());
            getBackupobj().setClientIdentifier(getIdentifier());
            getBackupobj().setLastbackup(new java.util.Date());
            getBackupobj().setName(System.getProperty("os.name") + " " + System.getProperty("os.arch") + " " + System.getProperty("os.version") + " " + System.getProperty("user.name"));
            getBackupobj().setRootFileBackupobjId(123l);
            DatabaseService.getInstance().getBackupobjDaoImpl().create(getBackupobj());
        } else {

        }
        return getBackupobj();
    }

    /**
     * @return the identifier
     */
    public String getIdentifier() {
        return identifier;
    }

    /**
     * @param identifier the identifier to set
     */
    public void setIdentifier(String identifier) {
        this.identifier = identifier;
    }

    /**
     * @return the backupobj
     */
    public Backupobj getBackupobj() {
        return backupobj;
    }

    /**
     * @param backupobj the backupobj to set
     */
    public void setBackupobj(Backupobj backupobj) {
        this.backupobj = backupobj;
    }

}
