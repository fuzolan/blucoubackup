package com.blucou.backup;

import org.bouncycastle.bcpg.HashAlgorithmTags;
import org.bouncycastle.bcpg.SymmetricKeyAlgorithmTags;
import org.bouncycastle.bcpg.sig.Features;
import org.bouncycastle.bcpg.sig.KeyFlags;
import org.bouncycastle.crypto.generators.RSAKeyPairGenerator;
import org.bouncycastle.crypto.params.RSAKeyGenerationParameters;
import org.bouncycastle.openpgp.*;
import org.bouncycastle.openpgp.operator.PBESecretKeyEncryptor;
import org.bouncycastle.openpgp.operator.PGPDigestCalculator;
import org.bouncycastle.openpgp.operator.bc.*;
import sun.misc.BASE64Decoder;
import sun.misc.BASE64Encoder;

import java.io.BufferedOutputStream;
import java.io.Console;
import java.io.FileOutputStream;
import java.io.IOException;
import java.math.BigInteger;
import java.security.NoSuchProviderException;
import java.security.SecureRandom;
import java.util.Date;
import java.util.Iterator;

public class CryptoKeyService {

    private static CryptoKeyService instance = new CryptoKeyService();

    private PGPPublicKey pubKey;

    private PGPPrivateKey privKey;

    private CryptoKeyService() {

    }

    /**
     * @return the instance
     */
    public static CryptoKeyService getInstance() {

        return instance;
    }

    public final static PGPKeyRingGenerator generateKeyRingGenerator(String id, char[] pass) throws Exception {

        return generateKeyRingGenerator(id, pass, 0xc0);
    }

    public final static PGPKeyRingGenerator generateKeyRingGenerator(String id, char[] pass, int s2kcount) throws Exception {
        // This object generates individual key-pairs.
        RSAKeyPairGenerator kpg = new RSAKeyPairGenerator();

        // Boilerplate RSA parameters, no need to change anything
        // except for the RSA key-size (2048). You can use whatever
        // key-size makes sense for you -- 4096, etc.
        kpg.init(
                new RSAKeyGenerationParameters(
                        BigInteger.valueOf(0x10001), new SecureRandom(), ConfigurationService.getInstance().getConfig().getInt("keyLengthInByte"), 12
                )
        );

        // First create the master (signing) key with the generator.
        PGPKeyPair rsakp_sign = new BcPGPKeyPair(PGPPublicKey.RSA_SIGN, kpg.generateKeyPair(), new Date());
        // Then an encryption subkey.
        PGPKeyPair rsakp_enc = new BcPGPKeyPair(PGPPublicKey.RSA_ENCRYPT, kpg.generateKeyPair(), new Date());

        // Add a self-signature on the id
        PGPSignatureSubpacketGenerator signhashgen = new PGPSignatureSubpacketGenerator();

        // Add signed metadata on the signature.
        // 1) Declare its purpose
        signhashgen.setKeyFlags(false, KeyFlags.SIGN_DATA | KeyFlags.CERTIFY_OTHER);
        // 2) Set preferences for secondary crypto algorithms to use
        //    when sending messages to this key.
        signhashgen.setPreferredSymmetricAlgorithms(
                false, new int[]{SymmetricKeyAlgorithmTags.AES_256, SymmetricKeyAlgorithmTags.AES_192, SymmetricKeyAlgorithmTags.AES_128}
        );
        signhashgen.setPreferredHashAlgorithms(
                false, new int[]{HashAlgorithmTags.SHA256, HashAlgorithmTags.SHA1, HashAlgorithmTags.SHA384, HashAlgorithmTags.SHA512, HashAlgorithmTags.SHA224,}
        );
        // 3) Request senders add additional checksums to the
        //    message (useful when verifying unsigned messages.)
        signhashgen.setFeature(false, Features.FEATURE_MODIFICATION_DETECTION);

        // Create a signature on the encryption subkey.
        PGPSignatureSubpacketGenerator enchashgen = new PGPSignatureSubpacketGenerator();
        // Add metadata to declare its purpose
        enchashgen.setKeyFlags(false, KeyFlags.ENCRYPT_COMMS | KeyFlags.ENCRYPT_STORAGE);

        // Objects used to encrypt the secret key.
        PGPDigestCalculator sha1Calc = new BcPGPDigestCalculatorProvider().get(HashAlgorithmTags.SHA1);
        PGPDigestCalculator sha256Calc = new BcPGPDigestCalculatorProvider().get(HashAlgorithmTags.SHA256);

        // bcpg 1.48 exposes this API that includes s2kcount. Earlier
        // versions use a default of 0x60.
        PBESecretKeyEncryptor pske = (new BcPBESecretKeyEncryptorBuilder(PGPEncryptedData.AES_256, sha256Calc, s2kcount)).build(pass);

        // Finally, create the keyring itself. The constructor
        // takes parameters that allow it to generate the self
        // signature.
        PGPKeyRingGenerator keyRingGen = new PGPKeyRingGenerator(
                PGPSignature.POSITIVE_CERTIFICATION, rsakp_sign, id, sha1Calc, signhashgen.generate(), null, new BcPGPContentSignerBuilder(
                rsakp_sign.getPublicKey().getAlgorithm(), HashAlgorithmTags.SHA1
        ), pske
        );

        // Add our encryption subkey, together with its signature.
        keyRingGen.addSubKey(rsakp_enc, enchashgen.generate(), null);
        return keyRingGen;
    }

    protected void init() throws Exception {

        if (ConfigurationService.getInstance().getConfig().getString("privateKey").isEmpty()) {
            //Keys erzeugen
            generate(null);
        } else {
            readPublicKey();
            readPrivateKey(null);
        }
    }

    protected void init(String pass) throws Exception {

        if (ConfigurationService.getInstance().getConfig().getString("publicKey").isEmpty()) {
            //Keys erzeugen
            generate(pass.toCharArray());
        } else {
            readPublicKey();
            readPrivateKey(pass.toCharArray());
        }
    }

    protected void init(boolean onlyPublic) throws Exception {

        if (ConfigurationService.getInstance().getConfig().getString("publicKey").isEmpty()) {
            generate(null);
        }
        if (!onlyPublic) {
            readPrivateKey(null);
        } else {
            readPublicKey();
        }
    }

    /**
     * liest public key aus config
     *
     * @throws IOException
     * @throws PGPException
     */
    public void readPublicKey() throws IOException, PGPException {

        BASE64Decoder b64 = new BASE64Decoder();


        PGPPublicKeyRingCollection pgpPub = new PGPPublicKeyRingCollection(b64.decodeBuffer(ConfigurationService.getInstance().getConfig().getString("publicKey")));
        Iterator rIt = pgpPub.getKeyRings();

        while (rIt.hasNext()) {
            PGPPublicKeyRing kRing = (PGPPublicKeyRing) rIt.next();
            Iterator kIt = kRing.getPublicKeys();

            while (kIt.hasNext()) {
                PGPPublicKey k = (PGPPublicKey) kIt.next();

                if (k.isEncryptionKey()) {
                    pubKey = k;
                }
            }
        }
        if (this.getPubKey() == null) {
            throw new IllegalArgumentException(
                    "Can't find encryption key in key ring."
            );
        }
    }

    public void readPrivateKey(char[] pass) throws IOException, PGPException, NoSuchProviderException {

        try {
            if (pass == null) {
                Console cons;
                if (!((cons = System.console()) != null && (pass = cons.readPassword("[%s]", "Pleaser enter passphrase to read secured private-key and hit enter:")) != null)) {
                    throw new IllegalArgumentException("Something goes wrong with passwordinput");
                }
            }
        } catch (Exception e) {//fos jdk5
            if (pass == null) {
                System.out.println("Pleaser enter passphrase to read secured private-key and hit enter:");
            }
        }

        //System.out.println(pass);
        BASE64Decoder b64 = new BASE64Decoder();
        PGPSecretKeyRingCollection pgpSec = new PGPSecretKeyRingCollection(b64.decodeBuffer(ConfigurationService.getInstance().getConfig().getString("privateKey")));

        PGPSecretKey pgpSecKey = pgpSec.getSecretKey(this.getPubKey().getKeyID());

        if (pgpSecKey == null) {
            throw new IllegalArgumentException(
                    "Can't find decryption key in key ring."
            );
        }

        this.privKey = pgpSecKey.extractPrivateKey(new BcPBESecretKeyDecryptorBuilder(new BcPGPDigestCalculatorProvider()).build(pass));

    }

    // Note: s2kcount is a number between 0 and 0xff that controls the
    // number of times to iterate the password hash before use. More
    // iterations are useful against offline attacks, as it takes more
    // time to check each password. The actual number of iterations is
    // rather complex, and also depends on the hash function in use.
    // Refer to Section 3.7.1.3 in rfc4880.txt. Bigger numbers give
    // you more iterations.  As a rough rule of thumb, when using
    // SHA256 as the hashing function, 0x10 gives you about 64
    // iterations, 0x20 about 128, 0x30 about 256 and so on till 0xf0,
    // or about 1 million iterations. The maximum you can go to is
    // 0xff, or about 2 million iterations.  I'll use 0xc0 as a
    // default -- about 130,000 iterations.

    public void generate(char[] pass) throws Exception {


        if (pass == null) {
            Console cons;
            if (!((cons = System.console()) != null && (pass = cons.readPassword("[%s]", "Pleaser enter passphrase to generate secured private-key and hit enter:")) != null)) {
                throw new IllegalArgumentException("Something goes wrong with passwordinput");
            }

        }
        System.out.print(pass);

        System.out.println("Generate Keyring (" + ConfigurationService.getInstance().getConfig().getInt("keyLengthInByte") + " Byte)");
        PGPKeyRingGenerator krgen = generateKeyRingGenerator("blucoubackup", pass);

        System.out.println("Dump public key to key.pkr");
        // Generate public key ring, dump to file.
        PGPPublicKeyRing pkr = krgen.generatePublicKeyRing();
        BufferedOutputStream pubout = new BufferedOutputStream(new FileOutputStream("key.pkr"));

        pkr.encode(pubout);
        pubout.close();

        System.out.println("Dump private key to key.skr");
        // Generate private key, dump to file.
        PGPSecretKeyRing skr = krgen.generateSecretKeyRing();
        BufferedOutputStream secout = new BufferedOutputStream(new FileOutputStream("key.skr"));
        skr.encode(secout);
        secout.close();

        System.out.println("Dump keys in config");
        //In Config abspeichern
        BASE64Encoder b64 = new BASE64Encoder();
        ConfigurationService.getInstance().getConfig().setProperty("publicKey", b64.encode(pkr.getEncoded()));
        ConfigurationService.getInstance().getConfig().setProperty("privateKey", b64.encode(skr.getEncoded()));

    }

    /**
     * @return the pubKey
     */
    public PGPPublicKey getPubKey() {

        return pubKey;
    }

    /**
     * @return the privKey
     */
    public PGPPrivateKey getPrivKey() {

        return privKey;
    }
}