/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.blucou.backup.DomainClasses;

import com.blucou.backup.DAOImpl.FileDaoImpl;
import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;

import javax.xml.bind.annotation.XmlRootElement;
import java.io.Serializable;

/**
 * @author Fuzolan
 */

@DatabaseTable(daoClass = FileDaoImpl.class)
//@Table(name = "file")
@XmlRootElement

public class File implements Serializable {

    private static final long serialVersionUID = 1L;

    @DatabaseField(generatedId = true)
    private Long id;

    @DatabaseField(index = true, width = 32)
    private String md5;

    @DatabaseField(index = true, width = 12)
    private String crc32;

    @DatabaseField(index = true, width = 128)

    private String sha512;

    @DatabaseField(canBeNull = false, columnName = "filesize")
    private long fileSize;


    public File() {

    }

    public File(Long id) {

        this.id = id;
    }

    public File(Long id, String md5, String crc32, String sha512, long fileSize) {

        this.id = id;
        this.md5 = md5;
        this.crc32 = crc32;
        this.sha512 = sha512;
        this.fileSize = fileSize;

    }

    public Long getId() {

        return id;
    }

    public void setId(Long id) {

        this.id = id;
    }

    public String getMd5() {

        return md5;
    }

    public void setMd5(String md5) {

        this.md5 = md5;
    }

    public String getCrc32() {

        return crc32;
    }

    public void setCrc32(String crc32) {

        this.crc32 = crc32;
    }

    public String getSha512() {

        return sha512;
    }

    public void setSha512(String sha512) {

        this.sha512 = sha512;
    }

    public long getFileSize() {

        return this.fileSize;
    }

    public void setFileSize(long fileSize) {

        this.fileSize = fileSize;
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
        if (!(object instanceof File)) {
            return false;
        }
        File other = (File) object;
        if ((this.id == null && other.id != null) || (this.id != null && !this.id.equals(other.id))) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {

        return "com.blucou.backup.DomainClasses.File[ id=" + id + " ]";
    }


}
