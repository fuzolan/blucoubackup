/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.blucou.backup.DomainClasses;

import com.blucou.backup.DAOImpl.FileStorageDaoImpl;
import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;

import javax.xml.bind.annotation.XmlRootElement;
import java.io.Serializable;

/**
 * @author Fuzolan
 */
//@Table(name = "file_storage")
@DatabaseTable(tableName = "file_storage", daoClass = FileStorageDaoImpl.class)
@XmlRootElement
public class FileStorage implements Serializable {

    private static final long serialVersionUID = 1L;

    @DatabaseField(generatedId = true)
    private Long id;

    @DatabaseField(foreign = true, index = true)
    private File file;

    @DatabaseField(foreign = true, foreignAutoRefresh = true)
    private Storage storage;

    @DatabaseField
    private String name;

    @DatabaseField(canBeNull = false)
    private String filePath;

    @DatabaseField
    private long fileSize;

    public FileStorage() {

    }

    public FileStorage(Long id) {

        this.id = id;
    }

    public FileStorage(Long id, File file, Storage storage, String name, String filePath) {

        this.id = id;
        this.file = file;
        this.storage = storage;
        this.name = name;
        this.filePath = filePath;
    }

    public Long getId() {

        return id;
    }

    public void setId(Long id) {

        this.id = id;
    }

    public File getFile() {

        return file;
    }

    public void setFile(File file) {

        this.file = file;
    }

    public Storage getStorage() {

        return storage;
    }

    public void setStorage(Storage storage) {

        this.storage = storage;
    }

    public String getName() {

        return name;
    }

    public void setName(String name) {

        this.name = name;
    }

    public String getFilePath() {

        return filePath;
    }

    public void setFilePath(String filePath) {

        this.filePath = filePath;
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
        if (!(object instanceof FileStorage)) {
            return false;
        }
        FileStorage other = (FileStorage) object;
        if ((this.id == null && other.id != null) || (this.id != null && !this.id.equals(other.id))) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {

        return "com.blucou.backup.DomainClasses.FileStorage[ id=" + id + " ]";
    }

    /**
     * @return the size
     */
    public long getFileSize() {

        return fileSize;
    }

    /**
     * @param fileSize the size to set
     */
    public void setFileSize(long fileSize) {

        this.fileSize = fileSize;
    }

}
