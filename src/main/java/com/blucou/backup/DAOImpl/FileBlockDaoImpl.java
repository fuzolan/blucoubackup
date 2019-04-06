/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.blucou.backup.DAOImpl;

import com.blucou.backup.DAOInterfaces.FileBlockDao;
import com.blucou.backup.DomainClasses.FileBlock;
import com.j256.ormlite.dao.BaseDaoImpl;
import com.j256.ormlite.support.ConnectionSource;

import java.sql.SQLException;

/**
 * @author Fuzolan
 */
public class FileBlockDaoImpl extends BaseDaoImpl<FileBlock, String> implements FileBlockDao {
    public FileBlockDaoImpl(ConnectionSource connectionSource) throws SQLException {
        super(connectionSource, FileBlock.class);
    }

}
