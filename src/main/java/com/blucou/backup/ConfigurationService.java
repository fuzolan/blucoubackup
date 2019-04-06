/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.blucou.backup;

import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.PropertiesConfiguration;

import java.net.MalformedURLException;
import java.util.ArrayList;

/**
 * @author Fuzolan
 */
public class ConfigurationService {

    protected static ConfigurationService instance = new ConfigurationService();

    private PropertiesConfiguration config;

    /**
     * @return the instance
     */
    public static ConfigurationService getInstance() {

        return instance;
    }

    protected void init() throws ConfigurationException, MalformedURLException {

        java.io.File log4jconfig = new java.io.File(".", java.io.File.separatorChar + "log4j.properties");
        if (!log4jconfig.exists()) {
            //besteht nicht und muss erst erstellt werden

            PropertiesConfiguration config_log4j = new PropertiesConfiguration(log4jconfig);
            //Standardwerte schreiben
            config_log4j.addProperty("log4j.rootLogger", "error, stdout, R");
            config_log4j.addProperty("log4j.appender.stdout", "org.apache.log4j.ConsoleAppender");
            config_log4j.addProperty("log4j.appender.stdout.Threshold", "ERROR");
            config_log4j.addProperty("log4j.appender.stdout.layout", "org.apache.log4j.PatternLayout");
            config_log4j.addProperty("log4j.appender.stdout.layout.ConversionPattern", "%m%n");
            config_log4j.addProperty("log4j.appender.R", "org.apache.log4j.RollingFileAppender");
            config_log4j.addProperty("log4j.appender.R.File", "debug.log");
            config_log4j.addProperty("log4j.appender.R.MaxFileSize", "5MB");
            config_log4j.addProperty("log4j.appender.R.MaxBackupIndex", "1");
            config_log4j.addProperty("log4j.appender.R.layout", "org.apache.log4j.PatternLayout");
            config_log4j.addProperty("log4j.appender.R.layout.ConversionPattern", "%5p %d [%t] - %c (%F:%L)- %m%n");
            config_log4j.addProperty("log4j.logger.org.apache.commons.configuration", "ERROR");
            config_log4j.addProperty("log4j.logger.com.j256.ormlite", "ERROR");
            config_log4j.addProperty("log4j.logger.com.blucou.backup", "WARN");

            config_log4j.save();

        }
        System.setProperty("log4j.configuration", new java.io.File(".", java.io.File.separatorChar + "log4j.properties").toURI().toURL().toString());

        //normale config
        java.io.File configFile = new java.io.File("blucoubackup.properties");
        config = new PropertiesConfiguration(configFile);
        //config.setListDelimiter(':');
        config.setAutoSave(true);

        if (!configFile.exists()) {
            //Datei neu erstellen
            config.addProperty("dbtype", "sqlite");
            config.addProperty("host", "database.db");
            config.addProperty("db", "database");
            config.addProperty("user", "database_user");
            config.addProperty("saveAsPicture", "false");
            config.addProperty("pass", "database_user_password");
            config.addProperty("keyLengthInByte", 4096);
            config.addProperty("blockSize", 1024);
            config.addProperty("publicKey", "");
            config.addProperty("privateKey", "");
            config.addProperty("checkDirectoryIntervalInDays", 0);
            config.addProperty("keepDeletedInDays", 60);
            config.addProperty("keepObsoleteInDays", 60);
            config.addProperty("compressionAlgo", "2");
            config.addProperty("compression", 9);
            ArrayList<String> blacklist = new ArrayList();
            ArrayList<String> fileblacklist = new ArrayList();
            blacklist.add("/tmp");
            blacklist.add("/var/tmp");
            config.addProperty("dirBlacklist", blacklist);
            config.addProperty("fileBlacklist", fileblacklist);
            System.out.println("A new config was written. Please edit blucoubackup.properties and try again");
            System.exit(1);

        }


    }

    /**
     * @return the config
     */
    protected PropertiesConfiguration getConfig() {

        return config;
    }

    protected String readcfg(String property) {

        return this.getConfig().getProperty(property).toString();
    }

}
