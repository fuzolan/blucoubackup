/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.blucou.backup;

/**
 * @author Fuzolan
 */

import com.blucou.backup.DomainClasses.*;
import com.j256.ormlite.dao.Dao;
import com.j256.ormlite.dao.DaoManager;
import com.j256.ormlite.jdbc.JdbcConnectionSource;
import com.j256.ormlite.jdbc.JdbcPooledConnectionSource;
import com.j256.ormlite.table.TableUtils;

import java.sql.SQLException;
import java.util.logging.Level;
import java.util.logging.Logger;

public class DatabaseService {

    private static DatabaseService instance = new DatabaseService();

    private JdbcConnectionSource connectionSource;


    private DatabaseService() {

    }


    public static DatabaseService getInstance() {

        return instance;
    }

    protected void initDatabase(String type, String host, String db, String user, String pass, Boolean dropTables, Boolean createTables) {

        try {
            if (type.equals("mysql")) {
                Class.forName("com.mysql.jdbc.Driver");
                this.setConnectionSource(new JdbcPooledConnectionSource("jdbc:mysql://" + host + "/" + db, user, pass));
            } else {
                Class.forName("org.sqlite.JDBC");
                String dbConnectionString = "jdbc:sqlite:" + host;
                this.setConnectionSource(new JdbcConnectionSource(dbConnectionString));
            }


            try {
                if (dropTables) {
                    //Alle Tabellen werden gedroppt
                    System.out.println("Drop tables from database...");
                    TableUtils.dropTable(this.getConnectionSource(), File.class, true);
                    TableUtils.dropTable(this.getConnectionSource(), FileBackupobj.class, true);
                    TableUtils.dropTable(this.getConnectionSource(), Backupobj.class, true);
                    TableUtils.dropTable(this.getConnectionSource(), Storage.class, true);
                    TableUtils.dropTable(this.getConnectionSource(), FileStorage.class, true);
                    TableUtils.dropTable(this.getConnectionSource(), FileBlock.class, true);
                }

            } catch (Exception e) {
                Logger.getLogger(DatabaseService.class.getName()).log(Level.SEVERE, null, e);
            }


            if (createTables) {
                //Alle Tabllen werden anhand der Klassen erneut erstellt
                System.out.println("Create tables...");
                TableUtils.createTableIfNotExists(this.getConnectionSource(), File.class);
                TableUtils.createTableIfNotExists(this.getConnectionSource(), FileBackupobj.class);
                TableUtils.createTableIfNotExists(this.getConnectionSource(), Backupobj.class);
                TableUtils.createTableIfNotExists(this.getConnectionSource(), Storage.class);
                TableUtils.createTableIfNotExists(this.getConnectionSource(), FileStorage.class);
                TableUtils.createTableIfNotExists(this.getConnectionSource(), FileBlock.class);
            }

            if (createTables) {
                if (type.equals("mysql")) {
                    this.getFileBackupobjDaoImpl().executeRawNoArgs("CREATE INDEX absolutepath_idx ON file_backupobj (absolutepath(255))");
                } else {
                    this.getFileBackupobjDaoImpl().executeRawNoArgs("CREATE INDEX absolutepath_idx ON file_backupobj (absolutepath)");
                }

            }


        } catch (Exception e) {
            Logger.getLogger(DatabaseService.class.getName()).log(Level.SEVERE, null, e);
        }

    }

    protected void closeDatabase() {

        try {
            if (this.getConnectionSource() != null) {
                this.getConnectionSource().close();
            }

        } catch (SQLException ex) {
            Logger.getLogger(DatabaseService.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    /**
     * @return the connectionSource
     */
    protected JdbcConnectionSource getConnectionSource() {

        return connectionSource;
    }

    /**
     * @param connectionSource the connectionSource to set
     */
    protected void setConnectionSource(JdbcConnectionSource connectionSource) {

        this.connectionSource = connectionSource;
    }

    /**
     * @return the fileDaoImpl
     */
    protected Dao<File, Long> getFileDaoImpl() throws SQLException {

        return DaoManager.createDao(connectionSource, File.class);
    }

    /**
     * @return the backupobjDaoImpl
     */
    protected Dao<Backupobj, Long> getBackupobjDaoImpl() throws SQLException {

        return DaoManager.createDao(connectionSource, Backupobj.class);
    }


    /**
     * @return the fileStorageDaoImpl
     */
    protected synchronized Dao<FileStorage, Long> getFileStorageDaoImpl() throws SQLException {

        return DaoManager.createDao(connectionSource, FileStorage.class);
    }

    /**
     * @return the fileBackupobjDaoImpl
     */
    protected Dao<FileBackupobj, Long> getFileBackupobjDaoImpl() throws SQLException {

        return DaoManager.createDao(connectionSource, FileBackupobj.class);
    }

    /**
     * @return the storageDaoImpl
     */
    protected Dao<Storage, Long> getStorageDaoImpl() throws SQLException {

        return DaoManager.createDao(connectionSource, Storage.class);
    }

    /**
     * @return the fileBlockDaoImpl
     */
    public Dao<FileBlock, Long> getFileBlockDaoImpl() throws SQLException {

        return DaoManager.createDao(connectionSource, FileBlock.class);
    }


}
