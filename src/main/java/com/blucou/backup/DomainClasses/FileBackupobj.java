/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.blucou.backup.DomainClasses;

import com.blucou.backup.DAOImpl.FileBackupobjDaoImpl;
import com.j256.ormlite.field.DataType;
import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;

import javax.xml.bind.annotation.XmlRootElement;
import java.io.Serializable;
import java.util.Date;

/**
 * @author Fuzolan
 */

@DatabaseTable(tableName = "file_backupobj", daoClass = FileBackupobjDaoImpl.class)
@XmlRootElement

public class FileBackupobj implements Serializable {

    private static final long serialVersionUID = 1L;

    @DatabaseField(generatedId = true)
    private Long id;

    @DatabaseField(columnName = "backupobj_id", index = true)
    private long backupobjId;

    @DatabaseField(foreign = true, index = true)
    private File file;

    @DatabaseField(canBeNull = false)
    private String name;

    @DatabaseField(dataType = DataType.LONG_STRING, format = "UTF8-BIN")
    private String absolutePath;

    @DatabaseField(canBeNull = false)
    private long updated;

    @DatabaseField(canBeNull = false)
    private Date created;

    @DatabaseField(canBeNull = false)
    private String owner;

    @DatabaseField(canBeNull = false)
    private String group;

    @DatabaseField(canBeNull = false)
    private int rights;

    @DatabaseField(canBeNull = false)
    private Date lastchecked;

    @DatabaseField(index = true)
    private Date lastbackup;

    @DatabaseField(canBeNull = false)
    private int modifiedCounter;

    @DatabaseField(index = true)
    private boolean deleted;

    @DatabaseField(defaultValue = "0", index = true)
    private boolean obsolete;

    public FileBackupobj() {

    }

    public FileBackupobj(Long id) {

        this.id = id;
    }

    public FileBackupobj(Long id, long backupobjId, File file, String name, long updated, Date created, String owner, String group, int rights, Date lastchecked, Date lastbackup, int modifiedCounter, boolean deleted) {

        this.id = id;
        this.backupobjId = backupobjId;
        //this.fileId = fileId;
        this.file = file;
        this.name = name;
        this.updated = updated;
        this.created = created;
        this.owner = owner;
        this.group = group;
        this.rights = rights;
        this.lastchecked = lastchecked;
        this.lastbackup = lastbackup;
        this.modifiedCounter = modifiedCounter;
        this.deleted = deleted;
    }

    public Long getId() {

        return id;
    }

    public void setId(Long id) {

        this.id = id;
    }

    public long getBackupobjId() {

        return backupobjId;
    }

    public void setBackupobjId(long backupobjId) {

        this.backupobjId = backupobjId;
    }

    /*
    public long getFileId() {
        return fileId;
    }

    public void setFileId(long fileId) {
        this.fileId = fileId;
    }*/

    public File getFile() {

        return file;
    }

    public void setFile(File file) {

        this.file = file;
    }

    public String getName() {

        return name;
    }

    public void setName(String name) {

        this.name = name;
    }

    public String getAbsolutePath() {

        return absolutePath;
    }

    public void setAbsolutePath(String absolutePath) {

        this.absolutePath = absolutePath;
    }

    public long getUpdated() {

        return updated;
    }

    public void setUpdated(long updated) {

        this.updated = updated;
    }

    public Date getCreated() {

        return created;
    }

    public void setCreated(Date created) {

        this.created = created;
    }

    public String getOwner() {

        return owner;
    }

    public void setOwner(String owner) {

        this.owner = owner;
    }

    public String getGroup() {

        return group;
    }

    public void setGroup(String group) {

        this.group = group;
    }

    public int getRights() {

        return rights;
    }

    public void setRights(int rights) {

        this.rights = rights;
    }

    public Date getLastchecked() {

        return lastchecked;
    }

    public void setLastchecked(Date lastchecked) {

        this.lastchecked = lastchecked;
    }

    public Date getLastbackup() {

        return lastbackup;
    }

    public void setLastbackup(Date lastbackup) {

        this.lastbackup = lastbackup;
    }

    public int getModifiedCounter() {

        return modifiedCounter;
    }

    public void setModifiedCounter(int modifiedCounter) {

        this.modifiedCounter = modifiedCounter;
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
        if (!(object instanceof FileBackupobj)) {
            return false;
        }
        FileBackupobj other = (FileBackupobj) object;
        if ((this.id == null && other.id != null) || (this.id != null && !this.id.equals(other.id))) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {

        return "com.blucou.backup.DomainClasses.FileBackupobj[ id=" + id + " ]";
    }

    /**
     * @return the deleted
     */
    public boolean isDeleted() {

        return deleted;
    }

    /**
     * @return the pathHash
     */
    /*public String getPathHash() {
        return pathHash;
    }*/

    /**
     * @param pathHash the pathHash to set
     */
    /*public void setPathHash(String pathHash) {
        this.pathHash = pathHash;
    }*/
    public void setDeleted(boolean deleted) {

        this.deleted = deleted;
    }

    /**
     * @return the obsolete
     */
    public boolean isObsolete() {

        return obsolete;
    }

    /**
     * @param obsolete the obsolete to set
     */
    public void setObsolete(boolean obsolete) {

        this.obsolete = obsolete;
    }

}
