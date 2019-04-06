/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.blucou.backup;

import org.apache.commons.io.FileUtils;
import org.bouncycastle.bcpg.ArmoredOutputStream;
import org.bouncycastle.openpgp.*;
import org.bouncycastle.openpgp.operator.bc.BcPGPDataEncryptorBuilder;
import org.bouncycastle.openpgp.operator.bc.BcPublicKeyDataDecryptorFactory;
import org.bouncycastle.openpgp.operator.bc.BcPublicKeyKeyEncryptionMethodGenerator;

import java.io.*;
import java.security.NoSuchProviderException;
import java.security.SecureRandom;
import java.util.Date;
import java.util.Iterator;

/**
 * @author Fuzolan
 */

class CryptoService {

    private static CryptoService instance = new CryptoService();

    private CryptoService() {

    }

    /**
     * @return the instance
     */
    public static CryptoService getInstance() {

        if (instance == null) {
            instance = new CryptoService();
        }
        return instance;
    }

    void encrypt(java.io.File source, java.io.File destination) throws IOException, NoSuchProviderException, PGPException {

        FileInputStream fis = FileUtils.openInputStream(source);
        FileOutputStream fos = FileUtils.openOutputStream(destination, true);

        byte[] buffer = new byte[(int) source.length()];
        fis.read(buffer, 0, (int) source.length());
        fos.write(this._encrypt(buffer, destination.getName(), false, false, source.length()));


        fos.flush();
        fis.close();
        fos.close();
        fos = null;
        fis = null;
        buffer = null;
    }

    protected void encrypt(byte[] buffer, java.io.File destination, long numBytes) throws IOException, NoSuchProviderException, PGPException {

        FileOutputStream fos = FileUtils.openOutputStream(destination, true);

        fos.write(this._encrypt(buffer, destination.getName(), false, false, numBytes));

        fos.flush();

        fos.close();
        fos = null;
        buffer = null;

    }


    protected void decrypt(File source, File destination, boolean strip) throws IOException, NoSuchProviderException, PGPException, Exception {
        //System.out.println("##Decrypt## " + source.getName() + " -> " + destination.getName());
        FileInputStream fis = FileUtils.openInputStream(source);
        FileOutputStream fos = FileUtils.openOutputStream(destination, true);

        if (strip && source.getName().endsWith(".jpg")) {
            fis.skip(3705); //skip picture
        }

        this._decrypt(fis, fos);

        fis.close();
        fos.close();
        fis = null;
        fos = null;
    }


    protected byte[] _encrypt(
            byte[] buffer, String fileName, boolean withIntegrityCheck, boolean armor, long length
    ) throws IOException, PGPException, NoSuchProviderException {

        if (fileName == null) {
            fileName = PGPLiteralData.CONSOLE;
        }

        ByteArrayOutputStream encOut = new ByteArrayOutputStream();

        OutputStream out = encOut;
        if (armor) {
            out = new ArmoredOutputStream(out);
        }

        ByteArrayOutputStream bOut = new ByteArrayOutputStream();

        PGPCompressedDataGenerator comData = new PGPCompressedDataGenerator(
                ConfigurationService.getInstance().getConfig().getInt("compressionAlgo"), ConfigurationService.getInstance().getConfig().getInt("compression")
        );
        OutputStream cos = comData.open(bOut); // open it with the final
        // destination
        PGPLiteralDataGenerator lData = new PGPLiteralDataGenerator();
        // we want to generate compressed data. This might be a user option
        // later,
        // in which case we would pass in bOut.
        OutputStream pOut = lData.open(
                cos, // the compressed output stream
                PGPLiteralData.BINARY, fileName, // "filename" to store
                length, // length of clear data
                new Date() // current time
        );
        pOut.write(buffer);

        lData.close();
        comData.close();


        PGPEncryptedDataGenerator cPk = new PGPEncryptedDataGenerator(
                new BcPGPDataEncryptorBuilder(PGPEncryptedData.CAST5).setWithIntegrityPacket(withIntegrityCheck).setSecureRandom(new SecureRandom())
        );

        cPk.addMethod(new BcPublicKeyKeyEncryptionMethodGenerator(CryptoKeyService.getInstance().getPubKey()).setSecureRandom(new SecureRandom()));

        byte[] bytes = bOut.toByteArray();

        OutputStream cOut = cPk.open(out, bytes.length);

        cOut.write(bytes); // obtain the actual bytes from the compressed stream

        cOut.close();
        pOut.close();
        out.close();
        bOut.close();
        encOut.close();
        cos.close();
        bytes = null;
        cOut = null;
        pOut = null;
        out = null;
        bOut = null;
        cos = null;
        lData = null;
        comData = null;
        cPk = null;

        return encOut.toByteArray();
    }


    //
    // Private class method _decrypt
    //
    protected void _decrypt(InputStream in, OutputStream out) throws Exception {

        in = PGPUtil.getDecoderStream(in);//new ArmoredInputStream(in);
        PGPPublicKey pubkey = CryptoKeyService.getInstance().getPubKey();
        PGPPrivateKey privkey = CryptoKeyService.getInstance().getPrivKey();


        PGPObjectFactory pgpF = new PGPObjectFactory(in);

        //PGPEncryptedDataList enc = (PGPEncryptedDataList) pgpF.nextObject();

        PGPEncryptedDataList enc;

        Object o = pgpF.nextObject();
        //
        // the first object might be a PGP marker packet.
        //
        if (o instanceof PGPEncryptedDataList) {
            enc = (PGPEncryptedDataList) o;
        } else {
            enc = (PGPEncryptedDataList) pgpF.nextObject();
        }

        Iterator<PGPPublicKeyEncryptedData> it = enc.getEncryptedDataObjects();
        PGPPrivateKey sKey = privkey;
        PGPPublicKeyEncryptedData pbe = null;
        pbe = it.next();

        InputStream clear = pbe.getDataStream(new BcPublicKeyDataDecryptorFactory(privkey));
        PGPObjectFactory plainFact = new PGPObjectFactory(clear);

        PGPCompressedData compressedData = (PGPCompressedData) plainFact.nextObject();
        plainFact = new PGPObjectFactory(compressedData.getDataStream());

        PGPLiteralData ld = (PGPLiteralData) plainFact.nextObject();

        InputStream unc = ld.getInputStream();

        byte[] buf = new byte[32768];
        int len;
        while ((len = unc.read(buf)) > 0) {
            out.write(buf, 0, len);
        }
        unc.close();
        out.close();

    }

}