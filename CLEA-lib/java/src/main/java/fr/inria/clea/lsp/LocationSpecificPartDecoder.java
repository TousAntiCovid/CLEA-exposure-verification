/* 
* Copyright (C) Inria, 2021
*/
package fr.inria.clea.lsp;

import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.util.Arrays;
import java.util.Base64;

import org.bouncycastle.crypto.InvalidCipherTextException;

import fr.devnied.bitlib.BitUtils;
import fr.inria.clea.lsp.LocationSpecificPart.LocationSpecificPartBuilder;
import lombok.extern.slf4j.Slf4j;

/**
 * locationSpecificPart (LSP) contents reader respecting the CLEA protocol
 * 
 * @see <a href="https://hal.inria.fr/hal-03146022">CLEA protocol</a>
 */
@Slf4j
public class LocationSpecificPartDecoder {

    private CleaEciesEncoder cleaEciesEncoder;
    /* EC private key in String format used to decrypt the message part */
    String serverAuthoritySecretKey;

    public LocationSpecificPartDecoder(String servertAuthoritySecretKey) {
        this.cleaEciesEncoder = new CleaEciesEncoder();
        this.serverAuthoritySecretKey = servertAuthoritySecretKey;
    }

    /**
     * Unpack the data message (binary format) :
     * | Staff | pad2 |CRIexp | vType |
     * vCat1 | vCat2 | countryCode | | periodDuration | ct_periodStart | t_qrStart |
     * LTKey | to extract parameters
     */
    private LocationSpecificPart decodeMessage(byte[] binaryLocationSpecificPart) {
        byte[] messageBinary = Arrays.copyOfRange(binaryLocationSpecificPart, CleaEciesEncoder.HEADER_BYTES_SIZE,
                CleaEciesEncoder.HEADER_BYTES_SIZE + CleaEciesEncoder.MSG_BYTES_SIZE);
        BitUtils message = new BitUtils(messageBinary);
        byte[] encryptedLocationContactMessage = Arrays.copyOfRange(binaryLocationSpecificPart, 
                CleaEciesEncoder.HEADER_BYTES_SIZE + CleaEciesEncoder.MSG_BYTES_SIZE, binaryLocationSpecificPart.length);
        if (encryptedLocationContactMessage.length == 0) {
            encryptedLocationContactMessage = null;
        }
        
        LocationSpecificPartBuilder locationSpecificPartbuilder = LocationSpecificPart.builder()
            .staff(message.getNextInteger(1) == 1);
        message.getNextInteger(1); // skip locationContactMessagePresent
        locationSpecificPartbuilder
            .countryCode(message.getNextInteger(12))
            .qrCodeRenewalIntervalExponentCompact(message.getNextInteger(5))
            .venueType(message.getNextInteger(5))
            .venueCategory1(message.getNextInteger(4))
            .venueCategory2(message.getNextInteger(4))
            .periodDuration(message.getNextInteger(8))
            .compressedPeriodStartTime(message.getNextInteger(24))
            .qrCodeValidityStartTime(message.getNextInteger(32))
            .locationTemporarySecretKey(message.getNextByte(256))
            .encryptedLocationContactMessage(encryptedLocationContactMessage);
        this.setHeader(binaryLocationSpecificPart, locationSpecificPartbuilder);
        return locationSpecificPartbuilder.build();            
    }
    
    /**
     * Unpack the data decrypted header (binary format): | version | LSPtype | pad |
     * LTId | to extract parameters
     */
    private void setHeader(byte[] binaryLocationSpecificPart, LocationSpecificPartBuilder locationSpecificPartbuilder) {
        byte[] headerBinary = Arrays.copyOfRange(binaryLocationSpecificPart, 0, CleaEciesEncoder.HEADER_BYTES_SIZE);
        BitUtils header = new BitUtils(headerBinary);

        locationSpecificPartbuilder
            .version(header.getNextInteger(3))
            .type(header.getNextInteger(3));
        /* padding (2 bits) */
        int pad = header.getNextInteger(2);
        assert (pad == 0) : "LSP decoding, padding error";
        /* LTId (16 bytes) */
        byte[] uuidBinary = new byte[16];
        uuidBinary = header.getNextByte(128);
        locationSpecificPartbuilder.locationTemporaryPublicId(this.cleaEciesEncoder.bytesToUuid(uuidBinary));
    }
    
    /**
     * Decrypt and unpack a location Specific Part (LSP)
     * 
     * @param lspBase64 Location Specific Part in base64
     * @throws CleaEncryptionException 
     */
    public LocationSpecificPart decrypt(String lspBase64) throws CleaEncryptionException {
        byte[] encryptedLocationSpecificPart = Base64.getDecoder().decode(lspBase64);
        log.debug("Base 64 decoded LSP: {}", encryptedLocationSpecificPart);
        byte[] binaryLocationSpecificPart;
        try {
            binaryLocationSpecificPart = this.cleaEciesEncoder.decrypt(encryptedLocationSpecificPart, this.serverAuthoritySecretKey, true);
        } catch (NoSuchAlgorithmException | InvalidKeySpecException | IllegalStateException | InvalidCipherTextException
                | IOException e) {
            throw new CleaEncryptionException(e);
        }
        return this.decodeMessage(binaryLocationSpecificPart);
    }
}
