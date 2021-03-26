/* 
* Copyright (C) Inria, 2021
*/
package fr.inria.clea.lsp;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.SecureRandom;
import java.security.Security;
import java.security.spec.ECGenParameterSpec;
import java.security.spec.InvalidKeySpecException;
import java.time.Instant;
import java.util.Arrays;
import java.util.UUID;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import org.bouncycastle.crypto.InvalidCipherTextException;
import org.bouncycastle.crypto.digests.SHA256Digest;
import org.bouncycastle.crypto.engines.AESEngine;
import org.bouncycastle.crypto.generators.KDF1BytesGenerator;
import org.bouncycastle.crypto.modes.GCMBlockCipher;
import org.bouncycastle.crypto.params.AEADParameters;
import org.bouncycastle.crypto.params.ISO18033KDFParameters;
import org.bouncycastle.crypto.params.KeyParameter;
import org.bouncycastle.jce.ECNamedCurveTable;
import org.bouncycastle.jce.interfaces.ECPrivateKey;
import org.bouncycastle.jce.interfaces.ECPublicKey;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.jce.spec.ECParameterSpec;
import org.bouncycastle.jce.spec.ECPrivateKeySpec;
import org.bouncycastle.jce.spec.ECPublicKeySpec;
import org.bouncycastle.math.ec.ECPoint;
import org.bouncycastle.util.encoders.Hex;

import fr.inria.clea.lsp.utils.TimeUtils;
import lombok.extern.slf4j.Slf4j;

/**
 * Encryption/Decription respecting ECIES-KEM (Elliptic Curve Integrated
 * Encryption Scheme with Key encapsulation mechanisms )
 * 
 * @see <a href="https://hal.inria.fr/hal-03146022">CLEA protocol</a>
 * @see <a href="https://www.iso.org/standard/37971.html">ISO/IEC 18033-2:2006:
 *      Information technology — Security techniques — Encryption algorithms —
 *      Part 2: Asymmetric ciphers</a>
 * @see <a href="https://www.shoup.net/iso/">V. Shoup, “ISO 18033-2: A Standard
 *      for Public-Key Encryption”, 2006</a>
 * 
 */
@Slf4j
public class CleaEciesEncoder {

    /* Type of the elliptic curve */
    private final String curveName = "secp256r1";
    /* Parameter iv fixed 96-bits for AES-256-GCM */
    private final byte[] iv = { (byte) 0xf0, (byte) 0xf1, (byte) 0xf2, (byte) 0xf3, (byte) 0xf4, (byte) 0xf5,
            (byte) 0xf6, (byte) 0xf7, (byte) 0xf8, (byte) 0xf9, (byte) 0xfa, (byte) 0xfb };

    /* Size in bytes of the ephemeral public key */
    public static final int C0_BYTES_SIZE = 33;
    /* Size in bytes of data header not encrypted (additional data in AES-256-GCM) */
    public static final int HEADER_BYTES_SIZE = 17;
    /* Size in bytes of the message to be encrypted with AES-256-GCM */
    public static final int MSG_BYTES_SIZE = 44;
    /* Size in bytes of TAG embedded in the AES-256-GCM encryption */
    public static final int TAG_BYTES_SIZE = 16;
    /* Size in bytes of locContactMsg to be encrypted with AES-256-GCM */
    public static final int LOC_BYTES_SIZE = 16;

    public CleaEciesEncoder() {
        Security.addProvider(new BouncyCastleProvider());
    }

    /**
     * generate a keys pair (private/public) for decryption/encryption
     * 
     * @param publicKeyCompressed True to get the compressed format of the public key
     *                          i.e. [02 or 03 | X] otherwise in uncompressed format
     *                          i.e. [04|X|Y]
     * 
     * @return String[2] = [privateKey, publicKey]
     */
    public String[] genKeysPair(boolean publicKeyCompressed) throws Exception {
        KeyPairGenerator kpg = KeyPairGenerator.getInstance("EC", "BC");
        kpg.initialize(new ECGenParameterSpec(curveName), new SecureRandom());
        KeyPair keyPair = kpg.generateKeyPair();
        ECPrivateKey privKey = (ECPrivateKey) keyPair.getPrivate();
        ECPublicKey pubKey = (ECPublicKey) keyPair.getPublic();

        String[] keyspair = new String[2];
        keyspair[0] = privKey.getD().toString(16);
        keyspair[1] = Hex.toHexString(pubKey.getQ().getEncoded(publicKeyCompressed));

        return keyspair;
    }

    /**
     * Get an EC private key (ECPrivateKey)
     * 
     * @param privateKey string format of the key
     * 
     * @return ECPrivateKey
     */
    private ECPrivateKey getECPrivateKey(String privateKey) throws NoSuchAlgorithmException, InvalidKeySpecException {

        BigInteger keyInt = new BigInteger(privateKey, 16);

        ECParameterSpec ecParameterSpec = ECNamedCurveTable.getParameterSpec(curveName);
        ECPrivateKeySpec privateKeySpec = new ECPrivateKeySpec(keyInt, ecParameterSpec);
        KeyFactory keyFactory = KeyFactory.getInstance("EC");
        ECPrivateKey privKey = (ECPrivateKey) keyFactory.generatePrivate(privateKeySpec);

        return privKey;
    }

    /**
     * Get an EC public key (ECPublicKey)
     * 
     * @param publicKey string format of the key
     * @throws NoSuchAlgorithmException 
     * @throws InvalidKeySpecException 
     */
    private ECPublicKey getECPublicKey(String publicKey) throws NoSuchAlgorithmException, InvalidKeySpecException {

        byte[] keyBytes = Hex.decode(publicKey);
        ECParameterSpec ecParameterSpec = ECNamedCurveTable.getParameterSpec(curveName);
        ECPublicKeySpec publicKeySpec = new ECPublicKeySpec(ecParameterSpec.getCurve().decodePoint(keyBytes),
                ecParameterSpec);
        KeyFactory keyFactory = KeyFactory.getInstance("EC");
        ECPublicKey pubKey = (ECPublicKey) keyFactory.generatePublic(publicKeySpec);

        return pubKey;
    }

    /**
     * Encrypt data respecting ECIES-KEM using: - SECP256R1 ECDH as KEM - KDF1 using
     * SHA256 hash as KDF - AES-256-GCM with a fixed 96-bits IV as DEM and TAG.
     * 
     * @param header  First HEADER_BYTES_SIZE bytes in Cléa protocol take as associated data in the
     *                scheme. If null no additional data required
     * @param message Message of MSG_BYTES_SIZE bytes in Cléa protocol
     * @param publicKey  EC public key required for encryption in String format
     * 
     * @return return data encrypted [header | encrypted message with tag |
     *         C0=ephemeral public key]
     * @throws IOException 
     */
    public byte[] encrypt(byte[] header, byte[] message, String publicKey) 
            throws NoSuchAlgorithmException, InvalidKeySpecException, NoSuchProviderException, 
                InvalidAlgorithmParameterException, IllegalStateException, InvalidCipherTextException, IOException {
        /* Public Key */
        ECPublicKey ecPublicKey = getECPublicKey(publicKey);
        /* Generate C0 */
        KeyPairGenerator kpg = KeyPairGenerator.getInstance("EC", "BC");
        kpg.initialize(new ECGenParameterSpec(curveName), new SecureRandom());
        KeyPair keyPair = kpg.generateKeyPair();
        BigInteger r = ((ECPrivateKey) keyPair.getPrivate()).getD(); // r
        ECPoint C0_Q = ((ECPublicKey) keyPair.getPublic()).getQ(); // r * G
        byte C0[] = C0_Q.getEncoded(true); // C0 = E(r * G)

        /* Generate secret S */
        ECPoint S_Q = ecPublicKey.getQ().multiply(r); // S = r * D(PK_HA)
        //byte S[] = S_Q.getEncoded(true); // E(S)
        byte S[] = S_Q.normalize().getAffineXCoord().getEncoded(); // S.X

        /* Generate AES key using KDF1 */
        KDF1BytesGenerator kdf = new KDF1BytesGenerator(new SHA256Digest());
        byte[] key = new byte[32]; // 256-bits AES key
        kdf.init(new ISO18033KDFParameters(concat(C0, S)));
        kdf.generateBytes(key, 0, key.length);

        /* Encode message with AES-GCM */
        GCMBlockCipher cipher = new GCMBlockCipher(new AESEngine());
        AEADParameters params = new AEADParameters(new KeyParameter(key), 8 * TAG_BYTES_SIZE, iv);
        cipher.init(true, params);
        byte[] out = new byte[message.length + TAG_BYTES_SIZE];

        if (header != null) {
            cipher.processAADBytes(header, 0, header.length);
        }
        int pos = cipher.processBytes(message, 0, message.length, out, 0);
        cipher.doFinal(out, pos);
        if (header != null) {
            out = concat(header, out);
        }

        return concat(out, C0);
    }

    /**
     * Decrypt data respecting ECIES-KEM using: - SECP256R1 ECDH as KEM - KDF1 using
     * SHA256 hash as KDF - AES-256-GCM with a fixed 96-bits IV as DEM and TAG.
     * 
     * @param encryptedMessage Message of 44 bytes in Cléa protocol
     * @param privateKeyString       EC private key required for decryption in String format
     * @param header        indicates if there is an header (HEADER_BYTES_SIZE) as
     *                      additionnal data or not
     * @return return data decrypted [header | decrypted message]
     */
    public byte[] decrypt(byte[] encryptedMessage, String privateKeyString, boolean header) 
            throws NoSuchAlgorithmException, InvalidKeySpecException, IOException, IllegalStateException, InvalidCipherTextException { 
        /* Private Key */
        ECPrivateKey privateKey = getECPrivateKey(privateKeyString);
        /* Get C0 */
        ECParameterSpec ecParameterSpec = ECNamedCurveTable.getParameterSpec(curveName);
        byte C0[] = Arrays.copyOfRange(encryptedMessage, encryptedMessage.length - C0_BYTES_SIZE, encryptedMessage.length);
        ECPoint C0_Q = ecParameterSpec.getCurve().decodePoint(C0);

        /* Generate secret S */
        ECPoint S_Q = C0_Q.multiply(privateKey.getD()); // S = x * D(C0)
        //byte S[] = S_Q.getEncoded(true); // E(S)   
        byte S[] = S_Q.normalize().getAffineXCoord().getEncoded(); // S.X
    
        /* Generate AES key using KDF1 */
        KDF1BytesGenerator kdf = new KDF1BytesGenerator(new SHA256Digest());
        byte[] key = new byte[32]; // 256-bits AES key
        kdf.init(new ISO18033KDFParameters(concat(C0, S)));
        kdf.generateBytes(key, 0, key.length);

        /* Decode message with AES-GCM */
        GCMBlockCipher cipher = new GCMBlockCipher(new AESEngine());
        AEADParameters params = new AEADParameters(new KeyParameter(key), 8 * TAG_BYTES_SIZE, iv);
        cipher.init(false, params);

        byte[] out;
        int pos;
        /* With or without header as additional data */
        if (header) {
            out = new byte[encryptedMessage.length - C0_BYTES_SIZE - HEADER_BYTES_SIZE - TAG_BYTES_SIZE];
            cipher.processAADBytes(encryptedMessage, 0, HEADER_BYTES_SIZE);
            pos = cipher.processBytes(encryptedMessage, HEADER_BYTES_SIZE,
                    encryptedMessage.length - C0_BYTES_SIZE - HEADER_BYTES_SIZE, out, 0);
            cipher.doFinal(out, pos);
            out = concat(Arrays.copyOfRange(encryptedMessage, 0, HEADER_BYTES_SIZE), out);
        } else {
            out = new byte[encryptedMessage.length - C0_BYTES_SIZE - TAG_BYTES_SIZE];
            pos = cipher.processBytes(encryptedMessage, 0, encryptedMessage.length - C0_BYTES_SIZE, out, 0);
            cipher.doFinal(out, pos);
        }

        return out;
    }

    /**
     * Compute the LTKey (Temporary Location Key) respecting Cléa protocol
     * LTKey(t_periodStart) = SHA256(SK_L | t_periodStart)
     * 
     * @param periodStartTime Starting time of the period in seconds(NTP timestamp
     *                      limited to the 32-bit seconds field)
     * @param permanentLocationSecretKey  Permanent location secret key (hexastring format)
     * @return LTKey (Temporary Location Key)
     * @throws CleaEncryptionException 
     */
    public byte[] computeLocationTemporarySecretKey(String permanentLocationSecretKey, Instant periodStartTime) throws CleaEncryptionException {
        log.info("permanentLocationSecretKey: {}, periodStartTime= {}", permanentLocationSecretKey, periodStartTime);
        try {
            byte[] concatKey = this.concat(instantToBytes(periodStartTime), Hex.decode(permanentLocationSecretKey));
            MessageDigest msg = MessageDigest.getInstance("SHA-256");
            byte[] locationTemporarySecretKey = msg.digest(concatKey);

            return locationTemporarySecretKey;
        } catch (IOException | NoSuchAlgorithmException e) {
            throw new CleaEncryptionException(e);
        }
    }

    /**
     * Compute the LTId (Temporary Location UUID) respecting Cléa protocol
     * LTId(t_periodStart) = HMAC-SHA-256-128(LTKey(t_periodStart), "1")
     * 
     * @param locationTemporarySecretKey (Temporary Location Key)
     * @return LTId (Temporary Location UUID)
     * @throws CleaEncryptionException 
     */
    public UUID computeLocationTemporaryPublicId(byte[] locationTemporarySecretKey) throws CleaEncryptionException {
        try {
            UUID locationTemporaryPublicID;
            Mac hmacSha256;
    
            /* LTId(t_periodStart) = HMAC-SHA-256-128(LTKey(t_periodStart), "1") */
            hmacSha256 = Mac.getInstance("HmacSHA256");
            SecretKeySpec secretKeySpec = new SecretKeySpec(locationTemporarySecretKey, "HmacSHA256");
            hmacSha256.init(secretKeySpec);
            String message = "1";
            byte[] tlidB = hmacSha256.doFinal(message.getBytes("UTF-8"));
            /* Convert in UUID format */
            byte[] tlidBTrunc = Arrays.copyOfRange(tlidB, 0, 16);
            locationTemporaryPublicID = UUID.nameUUIDFromBytes(tlidBTrunc);
    
            return locationTemporaryPublicID;
        } catch (InvalidKeyException | NoSuchAlgorithmException | IllegalStateException | UnsupportedEncodingException e) {
            throw new CleaEncryptionException(e);
        }
    }

    private byte[] instantToBytes(Instant periodStartTime) {
        long timestamp = TimeUtils.ntpTimestampFromInstant(periodStartTime);
        return intToBytes((int) timestamp);
    }

    /**
     * convert an int in 32 bytes array
     * 
     * @param data int to be converted
     * @return 32 bytes array
     */
    private static byte[] intToBytes(int data) {
        return new byte[] { 
                (byte) ((data >> 24) & 0xff), 
                (byte) ((data >> 16) & 0xff), 
                (byte) ((data >> 8) & 0xff),
                (byte) ((data >> 0) & 0xff) };
    }

    /**
     * concat two bytes array in one
     * 
     * @param part1 first bytes array
     * @param part2 second bytes array
     * @return bytes array concatenation
     */
    protected byte[] concat(byte[] part1, byte[] part2) throws IOException {
    
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        outputStream.write(part1);
        outputStream.write(part2);
        byte concatParts[] = outputStream.toByteArray();
    
        return concatParts;
    }

    /**
     * convert 32 bytes array in UUID format
     * 
     * @param bytes 32 bytes array
     * @return UUID
     */
    protected UUID bytesToUuid(byte[] bytes) {
        ByteBuffer bb = ByteBuffer.wrap(bytes);
        long firstLong = bb.getLong();
        long secondLong = bb.getLong();
    
        return new UUID(firstLong, secondLong);
    }

    /**
     * convert a UUID format in 32 bytes array
     * 
     * @param uuid UUID
     * @return 32 bytes array
     */
    protected byte[] uuidToBytes(UUID uuid) {
        ByteBuffer buffer = ByteBuffer.wrap(new byte[16]);
        buffer.putLong(uuid.getMostSignificantBits());
        buffer.putLong(uuid.getLeastSignificantBits());
    
        return buffer.array();
    }

}
