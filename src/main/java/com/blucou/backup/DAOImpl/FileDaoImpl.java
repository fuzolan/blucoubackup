/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.blucou.backup.DAOImpl;

import com.blucou.backup.DAOInterfaces.FileDao;
import com.blucou.backup.DomainClasses.File;
import com.j256.ormlite.dao.BaseDaoImpl;
import com.j256.ormlite.support.ConnectionSource;

import java.sql.SQLException;

/**
 * @author Fuzolan
 */
public class FileDaoImpl extends BaseDaoImpl<File, String> implements FileDao {
    public FileDaoImpl(ConnectionSource connectionSource) throws SQLException {
        super(connectionSource, File.class);
    }

}
