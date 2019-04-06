/*
* http://stackoverflow.com/questions/13526112/how-to-use-commandline-and-options-in-apache-commons-cli
 */

package com.blucou.backup;

/**
 * @author Fuzolan
 */

import org.apache.commons.cli.*;

import java.util.ArrayList;
import java.util.HashMap;

public class BlucouBackupCLIOptions {

    private static BlucouBackupCLIOptions singletonObj = null;

    private HashMap<String, Object> options = new HashMap<String, Object>();

    private ArrayList<String> arguments = new ArrayList<String>();

    private BlucouBackupCLIOptions(Options optsdef, String[] args) throws UnrecognizedOptionException, MissingArgumentException, MissingOptionException, AlreadySelectedException, ParseException {
        //***** (blindly) parse the command line *****
        CommandLineParser parser = new GnuParser();
        CommandLine cmdline = parser.parse(optsdef, args);

        for (Option opt : cmdline.getOptions()) {
            String key = opt.getOpt();
            if (opt.hasArgs()) {
                options.put(key, opt.getValuesList());
            } else {
                options.put(key, opt.getValue());
            }
        }

        for (String str : cmdline.getArgs()) {
            if (str.length() > 0) {
                arguments.add(str);
            }
        }
    }

    public static BlucouBackupCLIOptions getopts() {

        if (singletonObj == null) {
            throw new IllegalStateException("[BlucouBackupCLIOptions] Command line not yet initialized.");
        }

        return singletonObj;
    }

    public static synchronized void initialize(Options optsdef, String[] args) throws UnrecognizedOptionException, MissingArgumentException, MissingOptionException, AlreadySelectedException, ParseException {

        if (singletonObj == null) {
            singletonObj = new BlucouBackupCLIOptions(optsdef, args);
        } else {
            throw new IllegalStateException("[BlucouBackupCLIOptions] Command line already initialized.");
        }
    }

    public static void printProgress(long blocks, long oldBlock, long newBlock) {

        int processedBlocks = (int) (newBlock + oldBlock);
        blocks = blocks < processedBlocks ? processedBlocks : blocks;
        int newSize = (int) (newBlock * 100 / blocks) / 2;
        int oldSize = (int) (oldBlock * 100 / blocks) / 2;

        int restSize = 50 - oldSize - newSize;

        String newBlocks = new String(new char[newSize]).replace('\0', '+');
        String oldBlocks = new String(new char[oldSize]).replace('\0', '-');
        String restBlocks = restSize <= 0 ? "" : new String(new char[restSize]).replace('\0', ' ');

        String output = "|" + newBlocks + oldBlocks + restBlocks + "| " + processedBlocks + "\r";

        System.out.print(output);
    }

    //****************************************************************************
    //----- prevent cloning -----
    public Object clone() throws CloneNotSupportedException {

        throw new CloneNotSupportedException();
    }

    //****************************************************************************
    public boolean isset(String opt) {

        return options.containsKey(opt);
    }

    //****************************************************************************
    public Object getopt(String opt) {

        Object rc = null;

        if (options.containsKey(opt)) {
            rc = options.get(opt);
        }

        return rc;
    }
}