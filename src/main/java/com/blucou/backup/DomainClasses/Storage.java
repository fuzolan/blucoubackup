/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.blucou.backup.DomainClasses;

import com.blucou.backup.DAOImpl.StorageDaoImpl;
import com.j256.ormlite.field.DataType;
import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;

import javax.xml.bind.annotation.XmlRootElement;
import java.io.Serializable;
import java.util.Date;

/**
 * @author Fuzolan
 *         Hier werden bestimmte Zugangsdaten für ftp, webdav etc. bereitgehalten. Auch IDentifier für lokale Medien werden gespeichert
 *         Per Aufruf, werden Backuppfade anhand von Nodes mitgeteilt...Wird in diesem Pfad ein Identifier erkannt, kann auf diesen gespeichert werden
 */

//@Table(name = "storage")
@DatabaseTable(tableName = "storage", daoClass = StorageDaoImpl.class)
@XmlRootElement

public class Storage implements Serializable {

    private static final long serialVersionUID = 1L;

    @DatabaseField(generatedId = true)
    private Long id;

    @DatabaseField(canBeNull = false)
    private String identifier;

    @DatabaseField
    private long quota;

    @DatabaseField
    private Date lastseen;

    @DatabaseField
    private Date lastbackup;

    @DatabaseField(canBeNull = false)
    private String type;

    @DatabaseField
    private String host;

    @DatabaseField
    private String user;

    @DatabaseField
    private String pass;

    @DatabaseField(dataType = DataType.LONG_STRING)
    private String path;

    @DatabaseField
    private long usedSpace;


    public Storage() {

    }

    public Storage(Long id) {

        this.id = id;
    }

    public Storage(Long id, String identifier, String path, long quota, Date lastseen, Date lastbackup, String type, String host, String user, String pass) {

        this.id = id;
        this.identifier = identifier;
        this.quota = quota;
        this.lastseen = lastseen;
        this.lastbackup = lastbackup;
        this.type = type;
        this.host = host;
        this.user = user;
        this.pass = pass;
        this.path = path;
    }

    public Long getId() {

        return id;
    }

    public void setId(Long id) {

        this.id = id;
    }

    public String getIdentifier() {

        return identifier;
    }

    public void setIdentifier(String identifier) {

        this.identifier = identifier;
    }

    public long getQuota() {

        return quota;
    }

    public void setQuota(long quota) {

        this.quota = quota;
    }

    public Date getLastseen() {

        return lastseen;
    }

    public void setLastseen(Date lastseen) {

        this.lastseen = lastseen;
    }

    public Date getLastbackup() {

        return lastbackup;
    }

    public void setLastbackup(Date lastbackup) {

        this.lastbackup = lastbackup;
    }

    public String getType() {

        return type;
    }

    public void setType(String type) {

        this.type = type;
    }

    public String getHost() {

        return host;
    }

    public void setHost(String host) {

        this.host = host;
    }

    public String getUser() {

        return user;
    }

    public void setUser(String user) {

        this.user = user;
    }

    public String getPass() {

        return pass;
    }

    public void setPass(String pass) {

        this.pass = pass;
    }

    public java.io.File getBackupFolderPath() {

        return new java.io.File(this.getPath() + java.io.File.separator + this.getIdentifier());
    }

    public String getPath() {

        return path;
    }

    public void setPath(String path) {

        this.path = path;
    }


    @Override
    public int hashCode() {

        int hash = 0;
        hash += (id != null ? id.hashCode() : 0);
        return hash;
    }

    @Override
    public boolean equals(Object object) {
        // TODO: Warning - this method won't work in the case the id fields are not set
        if (!(object instanceof Storage)) {
            return false;
        }
        Storage other = (Storage) object;
        if ((this.id == null && other.id != null) || (this.id != null && !this.id.equals(other.id))) {
            return false;
        }
        return true;
    }


    @Override
    public String toString() {

        return "com.blucou.backup.DomainClasses.Storage[ id=" + id + " ]";
    }

    /**
     * @return the usedSpace
     */
    public long getUsedSpace() {

        return usedSpace;
    }

    /**
     * @param usedSpace the usedSpace to set
     */
    public void setUsedSpace(long usedSpace) {

        this.usedSpace = usedSpace;
    }

}
