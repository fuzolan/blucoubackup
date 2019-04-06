/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.blucou.backup.DAOImpl;

import com.blucou.backup.DAOInterfaces.StorageDao;
import com.blucou.backup.DomainClasses.Storage;
import com.j256.ormlite.dao.BaseDaoImpl;
import com.j256.ormlite.support.ConnectionSource;

import java.sql.SQLException;

/**
 * @author Fuzolan
 */
public class StorageDaoImpl extends BaseDaoImpl<Storage, String> implements StorageDao {
    public StorageDaoImpl(ConnectionSource connectionSource) throws SQLException {
        super(connectionSource, Storage.class);
    }

}
