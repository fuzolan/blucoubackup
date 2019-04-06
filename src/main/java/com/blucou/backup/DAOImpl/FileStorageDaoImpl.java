/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.blucou.backup.DAOImpl;

import com.blucou.backup.DAOInterfaces.FileStorageDao;
import com.blucou.backup.DomainClasses.FileStorage;
import com.j256.ormlite.dao.BaseDaoImpl;
import com.j256.ormlite.support.ConnectionSource;

import java.sql.SQLException;

/**
 * @author Fuzolan
 */
public class FileStorageDaoImpl extends BaseDaoImpl<FileStorage, String> implements FileStorageDao {
    public FileStorageDaoImpl(ConnectionSource connectionSource) throws SQLException {
        super(connectionSource, FileStorage.class);
    }

}
