/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.blucou.backup;
//package com.jbeckup.DAOInterfaces;

import com.mysql.jdbc.exceptions.jdbc4.CommunicationsException;
import org.apache.commons.cli.*;

import java.util.logging.Level;
import java.util.logging.Logger;


/**
 * @author Fuzolan
 */
public class BlucouBackup {

    /**
     * @param args the command line arguments
     */

    //Platzersparnis über Blockgenerierung ermitteln
    //select block_id, f.`filesize`, count(*), f.`filesize`*count(*)/1024 as "Ersparnis in kb"  from file_block fb, file f where f.id = fb.block_id group by block_id having count(*) > 1 order by count(*) desc
    public static void main(String[] args) {

        //init configuration
        ConfigurationService cfg = ConfigurationService.getInstance();
        try {
            cfg.init();
        } catch (Exception ex) {
            System.out.println(ex.getMessage());
        }

        try {
            String appdir = (new java.io.File(BlucouBackup.class.getProtectionDomain().getCodeSource().getLocation().toURI())).getAbsolutePath();
            System.setProperty("user.dir", appdir);
        } catch (Exception ex) {
            Logger.getLogger(BlucouBackup.class.getName()).log(Level.SEVERE, null, ex);
        }
        System.out.println("Workingdirectory: " + System.getProperty("user.dir"));
        //***** build up options *****
        Options options = OptionsBuilder.invoke();


        // ... .... ...

        //***** process command line *****

        try {
            BlucouBackupCLIOptions.initialize(options, args);
        } catch (MissingArgumentException ex) {
            System.out.println(ex.getMessage());
            System.exit(1);
        } catch (MissingOptionException ex) {
            Logger.getLogger(BlucouBackup.class.getName()).log(Level.WARNING, null, ex);
        } catch (AlreadySelectedException ex) {
            Logger.getLogger(BlucouBackup.class.getName()).log(Level.WARNING, null, ex);
        } catch (ParseException ex) {
            Logger.getLogger(BlucouBackup.class.getName()).log(Level.WARNING, null, ex);
        }


        CommandLineParser parser = new GnuParser();
        CommandLine cmd = null;
        try {

            cmd = parser.parse(options, args);
            if (cmd.getOptions().length == 0 || cmd.hasOption("?") || cmd.hasOption("help")) {
                //show programm usage
                HelpFormatter formatter = new HelpFormatter();
                formatter.printHelp("java -Xmx256m -jar blucoubackup.jar", options, true);
                System.exit(0);
            }

        } catch (ParseException ex) {
            Logger.getLogger(BlucouBackup.class.getName()).log(Level.SEVERE, null, ex);
        }


        try {

            if (cmd.hasOption("keygen")) {
                CryptoKeyService.getInstance().generate(null);
                System.exit(0);
            }


            DatabaseService databaseService = DatabaseService.getInstance();
            boolean dropTables = false;
            boolean createTables = false;
            if (cmd.hasOption("setup_database_and_lose_old_data")) {
                dropTables = true;
                createTables = true;
            }
            databaseService.initDatabase(cfg.readcfg("dbtype"), cfg.readcfg("host"), cfg.readcfg("db"), cfg.readcfg("user"), cfg.readcfg("pass"), dropTables, createTables);

            //Backupobj
            BackupObjService backupObjService = new BackupObjService();
            try {
                backupObjService.init();
            } catch (CommunicationsException e) {
                Logger.getLogger(BlucouBackup.class.getName()).log(Level.SEVERE, "Can't connect to database!");
                Logger.getLogger(BlucouBackup.class.getName()).log(Level.FINE, null, e);
            }


            if (cmd.hasOption("us")) {
                StorageService.UpdateUsedSpace();
            }


            if (cmd.hasOption("s")) {
                //init Storage
                StorageService storageService = new StorageService();
                String[] storages = cmd.getOptionValues("s");
                for (int i = 0; i < storages.length; i++) {
                    storageService.initStorage(storages[i]);

                }
            }


            if (cmd.hasOption("delete_storage_and_lose_data")) {
                //delete a storage
                StorageService.deleteStorage();
            }

            if (cmd.hasOption("validate_and_delete_lost_files")) {
                //validate and delete
                StorageService storageService = new StorageService();
                storageService.validateBackupFiles();
            }

            if (cmd.hasOption("c")) {
                //init Storage
                StorageService storageService = new StorageService();
                storageService.cleanup();
            }

            if (cmd.hasOption("quota")) {
                StorageService.setStorageQuota();
            }


            if (cmd.hasOption("validate")) {
                String[] arg = cmd.getOptionValues("validate");
                FileService.validateFile(Long.parseLong(arg[0]), new java.io.File(arg[1]));
            }


            if (cmd.hasOption("b")) {
                //Test FS

                CryptoKeyService.getInstance().init(true);
                long timeStart = System.currentTimeMillis();
                String[] backupSources = cmd.getOptionValues("b");
                for (int i = 0; i < backupSources.length; i++) {
                    backupObjService.backup(new java.io.File(backupSources[i]), false);

                }
                System.out.println("\nBackup completed in " + ((System.currentTimeMillis() - timeStart) / 1000) + " seconds");
            }

            if (cmd.hasOption("r")) {
                //RESTORE
                String path = "";
                boolean deleted = false;
                int obsolete = 0;
                //CryptoKeyService.getInstance().init("fisch501");
                CryptoKeyService.getInstance().init();
                if (cmd.hasOption("rdel")) {
                    deleted = true;
                }
                if (cmd.hasOption("robs")) {
                    obsolete = Integer.valueOf(cmd.getOptionValue("robs"));
                }
                if (cmd.hasOption("rpath")) {
                    path = cmd.getOptionValue("rpath");
                }
                StorageService.restoreFromStorage(backupObjService.getBackupobj(), new java.io.File(cmd.getOptionValue("r")), path, deleted, obsolete);
            }


            if (cmd.hasOption("m")) {
                String arg = cmd.getOptionValue("m");
                FileService f = new FileService(backupObjService.getBackupobj());
                f.markDeletedFiles(backupObjService.getBackupobj(), arg);
            }


            databaseService.closeDatabase();


        } catch (Exception ex) {
            Logger.getLogger(BlucouBackup.class.getName()).log(Level.SEVERE, null, ex);
        }

    }

    private static class OptionsBuilder {
        private static Options invoke() {
            Options options = new Options();
            options.addOption("keygen", false, "Generates new Key. WARNING: Previous backup-Files could get lost!!!");

            Option option_s = new Option("s", "storage", true, "Path of storage");
            option_s.setArgs(10);
            options.addOption(option_s);

            Option option_b = new Option("b", "backup", true, "Check path to backup something into storage");
            option_b.setArgs(10);
            options.addOption(option_b);

            Option option_c = new Option("c", "cleanup", false, "Cleanup Database (obsolete, deleted, inconsiten");
            options.addOption(option_c);

            Option option_quota = new Option("quota", false, "Set a quota for a storage");
            options.addOption(option_quota);

            Option option_updatestorage = new Option("us", "updatestorage", false, "Update UsedSpace for connected storages");
            options.addOption(option_updatestorage);

            Option option_validate = new Option("validate", true, "Par1:fileid Par2:path of file");
            option_validate.setArgs(2);
            option_validate.setValueSeparator('#');
            options.addOption(option_validate);

            Option option_deletestorage = new Option("delete_storage_and_lose_data", false, "Delete storage from database - !!!No valid backup!!! rerun backup on all machines!!!");
            options.addOption(option_deletestorage);

            options.addOption("setup_database_and_lose_old_data", false, "Tables where written...old data could get lost");

            options.addOption(new Option("restorebyfileid", true, "Restore a single file by id"));

            options.addOption(new Option("r", "restore", true, "Restore current machine to given path"));
            options.addOption(new Option("rdel", "restore-deleted", false, "Restoreoption: Deleted files will also be restored"));
            options.addOption(new Option("robs", "restore-obsolete", true, "Restoreoption: Number of versions to restore"));
            options.addOption(new Option("rpath", "restore-path", true, "Restoreoption: path to restore"));
            options.addOption(new Option("validate_and_delete_lost_files", false, "Delete lost files from connected Storages"));

            options.addOption(new Option("m", "mark-deleted", true, "mark deleted files for a specific path"));

            options.addOption("?", "help", false, "Show usage");
            return options;
        }
    }
}
