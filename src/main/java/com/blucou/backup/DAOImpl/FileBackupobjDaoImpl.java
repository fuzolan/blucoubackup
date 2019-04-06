/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.blucou.backup.DAOImpl;

import com.blucou.backup.DAOInterfaces.FileBackupobjDao;
import com.blucou.backup.DomainClasses.FileBackupobj;
import com.j256.ormlite.dao.BaseDaoImpl;
import com.j256.ormlite.support.ConnectionSource;

import java.sql.SQLException;

/**
 * @author Fuzolan
 */
public class FileBackupobjDaoImpl extends BaseDaoImpl<FileBackupobj, String> implements FileBackupobjDao {
    public FileBackupobjDaoImpl(ConnectionSource connectionSource) throws SQLException {
        super(connectionSource, FileBackupobj.class);
    }

}
