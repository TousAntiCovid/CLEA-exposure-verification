/* 
* Copyright (C) Inria, 2021
*/
package fr.inria.clea.lsp;

import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.util.Arrays;
import java.util.Base64;
import java.util.Objects;

import org.bouncycastle.crypto.InvalidCipherTextException;

import fr.devnied.bitlib.BitUtils;
import fr.inria.clea.lsp.EncryptedLocationSpecificPart.EncryptedLocationSpecificPartBuilder;
import fr.inria.clea.lsp.LocationSpecificPart.LocationSpecificPartBuilder;
import fr.inria.clea.lsp.exception.CleaEncodingException;
import fr.inria.clea.lsp.exception.CleaEncryptionException;
import fr.inria.clea.lsp.utils.TimeUtils;
import lombok.extern.slf4j.Slf4j;

/**
 * LocationSpecificPart (LSP) contents reader respecting the CLEA protocol
 * 
 * @see <a href="https://hal.inria.fr/hal-03146022">CLEA protocol</a>
 */
@Slf4j
public class LocationSpecificPartDecoder {

    private CleaEciesEncoder cleaEciesEncoder;
    /* EC private key in String format used to decrypt the message part */
    String serverAuthoritySecretKey;

    /**
     * This default constructor should only be used for decoding.
     * The server secret key is mandatory to decrypt.
     */
    public LocationSpecificPartDecoder() {
        this.cleaEciesEncoder = new CleaEciesEncoder();
    }

    public LocationSpecificPartDecoder(String serverAuthoritySecretKey) {
        this();
        this.serverAuthoritySecretKey = serverAuthoritySecretKey;
    }

    /**
     * Unpack the data header (binary format, already base64 decrypted): 
     * | version | LSPtype | pad | LTId | to extract parameters
     */
    public EncryptedLocationSpecificPart decodeHeader(byte[] binaryLocationSpecificPart) throws CleaEncodingException {
        if (binaryLocationSpecificPart.length < CleaEciesEncoder.HEADER_BYTES_SIZE + CleaEciesEncoder.MSG_BYTES_SIZE) {
            throw new CleaEncodingException("Bad message length: " + binaryLocationSpecificPart.length);
        }
        byte[] headerBinary = Arrays.copyOfRange(binaryLocationSpecificPart, 0, CleaEciesEncoder.HEADER_BYTES_SIZE);
        BitUtils header = new BitUtils(headerBinary);

        EncryptedLocationSpecificPartBuilder builder = EncryptedLocationSpecificPart.builder()
            .version(header.getNextInteger(3))
            .type(header.getNextInteger(3));
        /* padding (2 bits) */
        int pad = header.getNextInteger(2);
        assert (pad == 0) : "LSP decoding, padding error";
        /* LTId (16 bytes) */
        byte[] uuidBinary = new byte[16];
        uuidBinary = header.getNextByte(128);
        builder.locationTemporaryPublicId(this.cleaEciesEncoder.bytesToUuid(uuidBinary));
        byte[] messageBinary = Arrays.copyOfRange(binaryLocationSpecificPart, CleaEciesEncoder.HEADER_BYTES_SIZE,
                binaryLocationSpecificPart.length);
        builder.encryptedLocationMessage(messageBinary);
        return builder.build();
    }
    
    /**
     * Decrypt and unpack a location Specific Part (LSP)
     * 
     * @param lspBase64 Location Specific Part in base64
     * @throws CleaEncryptionException 
     * @throws CleaEncodingException 
     */
    public LocationSpecificPart decrypt(String lspBase64) throws CleaEncryptionException, CleaEncodingException {
        /* The decoder is compliant with or without padding */
        byte[] encryptedLocationSpecificPart = Base64.getUrlDecoder().decode(lspBase64);
        log.debug("Base 64 decoded LSP: {}", encryptedLocationSpecificPart);
        return this.decrypt(encryptedLocationSpecificPart);
    }
    
    /**
     * Decrypt and unpack a location Specific Part (LSP)
     * 
     * @param encryptedLocationSpecificPart Location Specific Part base64-decoded
     * @throws CleaEncryptionException 
     * @throws CleaEncodingException 
     */
    public LocationSpecificPart decrypt(byte[] encryptedLocationSpecificPart) throws CleaEncryptionException, CleaEncodingException {
        if (Objects.isNull(serverAuthoritySecretKey)) {
            throw new CleaEncryptionException("Cannot encrypt, serverAuthoritySecretKey is null!");
        }
        EncryptedLocationSpecificPart encryptedLsp = this.decodeHeader(encryptedLocationSpecificPart);
        return this.decrypt(encryptedLsp);
    }
    
    /**
     * Decrypt and unpack a location Specific Part (LSP)
     * 
     * @param encryptedLocationSpecificPart Encrypted Location Specific Part (Header decoded)
     * @throws CleaEncryptionException 
     * @throws CleaEncodingException 
     */
    public LocationSpecificPart decrypt(EncryptedLocationSpecificPart encryptedLocationSpecificPart) throws CleaEncryptionException, CleaEncodingException {
        byte[] binaryLocationSpecificPart;
        try {
            binaryLocationSpecificPart = this.cleaEciesEncoder.decrypt(
                    encryptedLocationSpecificPart.binaryEncoded(), this.serverAuthoritySecretKey, true);
        } catch (NoSuchAlgorithmException | InvalidKeySpecException | IllegalStateException 
                | InvalidCipherTextException | IOException e) {
            throw new CleaEncryptionException(e);
        }
        return this.decodeMessage(binaryLocationSpecificPart, newLocationSpecificPartBuilder(encryptedLocationSpecificPart));
    }
    
    private LocationSpecificPartBuilder newLocationSpecificPartBuilder(EncryptedLocationSpecificPart encryptedLocationSpecificPart) {
        return LocationSpecificPart.builder()
            .version(encryptedLocationSpecificPart.getVersion())
            .type(encryptedLocationSpecificPart.getType())
            .locationTemporaryPublicId(encryptedLocationSpecificPart.getLocationTemporaryPublicId());
    }
    
    /**
     * Unpack the data message (binary format) :
     * | Staff | pad2 |CRIexp | vType |
     * vCat1 | vCat2 | reserved | | periodDuration | ct_periodStart | t_qrStart |
     * LTKey | to extract parameters
     */
    public LocationSpecificPart decodeMessage(byte[] binaryLocationSpecificPart, LocationSpecificPartBuilder locationSpecificPartbuilder) {
        byte[] messageBinary = Arrays.copyOfRange(binaryLocationSpecificPart, CleaEciesEncoder.HEADER_BYTES_SIZE, 
                CleaEciesEncoder.HEADER_BYTES_SIZE + CleaEciesEncoder.MSG_BYTES_SIZE);
        BitUtils message = new BitUtils(messageBinary);
        byte[] encryptedLocationContactMessage = Arrays.copyOfRange(binaryLocationSpecificPart, 
                CleaEciesEncoder.HEADER_BYTES_SIZE + CleaEciesEncoder.MSG_BYTES_SIZE, binaryLocationSpecificPart.length);
        if (encryptedLocationContactMessage.length == 0) {
            encryptedLocationContactMessage = null;
        }
        
        locationSpecificPartbuilder
            .staff(message.getNextInteger(1) == 1);
        message.getNextInteger(1); // skip locationContactMessagePresent
        message.getNextInteger(12); // skip reserved 
        locationSpecificPartbuilder
            .qrCodeRenewalIntervalExponentCompact(message.getNextInteger(5))
            .venueType(message.getNextInteger(5))
            .venueCategory1(message.getNextInteger(4))
            .venueCategory2(message.getNextInteger(4))
            .periodDuration(message.getNextInteger(8))
            .compressedPeriodStartTime(message.getNextInteger(24))
            .qrCodeValidityStartTime(TimeUtils.instantFromTimestamp(message.getNextLong(32)))
            .locationTemporarySecretKey(message.getNextByte(256))
            .encryptedLocationContactMessage(encryptedLocationContactMessage);
        return locationSpecificPartbuilder.build();            
    }
}
