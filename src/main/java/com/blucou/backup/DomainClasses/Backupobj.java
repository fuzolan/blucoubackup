/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.blucou.backup.DomainClasses;

import com.blucou.backup.DAOImpl.BackupobjDaoImpl;
import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;

import javax.xml.bind.annotation.XmlRootElement;
import java.io.Serializable;
import java.util.Date;

/**
 * @author Fuzolan
 */

@DatabaseTable(tableName = "backupobj", daoClass = BackupobjDaoImpl.class)
@XmlRootElement

public class Backupobj implements Serializable {
    private static final long serialVersionUID = 1L;

    @DatabaseField(generatedId = true)
    private Long id;

    @DatabaseField(canBeNull = false)
    private String name;

    @DatabaseField(canBeNull = false, columnName = "root_path")
    private long rootPath;

    @DatabaseField(canBeNull = false, columnName = "root_file_backupobj_id")
    private long rootFileBackupobjId;

    @DatabaseField(canBeNull = false)
    private Date lastbackup;

    @DatabaseField(canBeNull = false, columnName = "client_identifier")
    private String clientIdentifier;

    public Backupobj() {
    }

    public Backupobj(Long id) {
        this.id = id;
    }

    public Backupobj(Long id, String name, long rootFileBackupobjId, Date lastbackup, String clientIdentifier) {
        this.id = id;
        this.name = name;
        this.rootFileBackupobjId = rootFileBackupobjId;
        this.lastbackup = lastbackup;
        this.clientIdentifier = clientIdentifier;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public long getRootFileBackupobjId() {
        return rootFileBackupobjId;
    }

    public void setRootFileBackupobjId(long rootFileBackupobjId) {
        this.rootFileBackupobjId = rootFileBackupobjId;
    }

    public Date getLastbackup() {
        return lastbackup;
    }

    public void setLastbackup(Date lastbackup) {
        this.lastbackup = lastbackup;
    }

    public String getClientIdentifier() {
        return clientIdentifier;
    }

    public void setClientIdentifier(String clientIdentifier) {
        this.clientIdentifier = clientIdentifier;
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
        if (!(object instanceof Backupobj)) {
            return false;
        }
        Backupobj other = (Backupobj) object;
        if ((this.id == null && other.id != null) || (this.id != null && !this.id.equals(other.id))) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return "com.blucou.backup.DomainClasses.Backupobj[ id=" + id + " ]";
    }

}
