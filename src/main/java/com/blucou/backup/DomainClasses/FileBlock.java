/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.blucou.backup.DomainClasses;

import com.blucou.backup.DAOImpl.FileBlockDaoImpl;
import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;

import javax.xml.bind.annotation.XmlRootElement;
import java.io.Serializable;

/**
 * @author Fuzolan
 */

//@Table(name = "file_block")
@DatabaseTable(tableName = "file_block", daoClass = FileBlockDaoImpl.class)
@XmlRootElement

public class FileBlock implements Serializable {

    private static final long serialVersionUID = 1L;

    @DatabaseField(generatedId = true)
    private Long id;

    @DatabaseField(foreign = true, index = true)
    private File file;

    @DatabaseField(foreign = true, index = true)
    private File block;

    @DatabaseField(index = true)
    private long blockNr;

    public FileBlock() {

    }

    public FileBlock(Long id) {

        this.id = id;
    }

    public FileBlock(Long id, File file, File block, long blockNr) {

        this.id = id;
        this.file = file;
        this.block = block;
        this.blockNr = blockNr;
    }

    public Long getId() {

        return id;
    }

    public void setId(Long id) {

        this.id = id;
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
        if (!(object instanceof FileBlock)) {
            return false;
        }
        FileBlock other = (FileBlock) object;
        if ((this.id == null && other.id != null) || (this.id != null && !this.id.equals(other.id))) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {

        return "com.blucou.backup.DomainClasses.FileBlocks[ id=" + id + " ]";
    }

    /**
     * @return the file
     */
    public File getFile() {

        return file;
    }

    /**
     * @param file the file to set
     */
    public void setFile(File file) {

        this.file = file;
    }

    /**
     * @return the block
     */
    public File getBlock() {

        return block;
    }

    /**
     * @param block the block to set
     */
    public void setBlock(File block) {

        this.block = block;
    }

    /**
     * @return the blockNr
     */
    public long getBlockNr() {

        return blockNr;
    }

    /**
     * @param blockNr the blockNr to set
     */
    public void setBlockNr(long blockNr) {

        this.blockNr = blockNr;
    }


}
