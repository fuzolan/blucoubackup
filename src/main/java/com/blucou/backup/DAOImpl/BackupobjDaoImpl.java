/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.blucou.backup.DAOImpl;

import com.blucou.backup.DAOInterfaces.BackupobjDao;
import com.blucou.backup.DomainClasses.Backupobj;
import com.j256.ormlite.dao.BaseDaoImpl;
import com.j256.ormlite.support.ConnectionSource;

import java.sql.SQLException;

/**
 * @author Fuzolan
 */
public class BackupobjDaoImpl extends BaseDaoImpl<Backupobj, String> implements BackupobjDao {
    public BackupobjDaoImpl(ConnectionSource connectionSource) throws SQLException {
        super(connectionSource, Backupobj.class);
    }

}
