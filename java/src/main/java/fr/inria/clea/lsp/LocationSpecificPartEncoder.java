/* 
* Copyright (C) Inria, 2021
*/
package fr.inria.clea.lsp;

import java.io.IOException;

import fr.devnied.bitlib.BitUtils;
import lombok.extern.slf4j.Slf4j;

/**
 * locationSpecificPart (LSP) contents encoder respecting the CLEA protocol
 * 
 * @see <a href="https://hal.inria.fr/hal-03146022">CLEA protocol</a>
 */
@Slf4j
public class LocationSpecificPartEncoder {
    /* ECIES crytography */
    private CleaEciesEncoder cleaEciesEncoder;
    private String serverAuthorityPublicKey;

    public LocationSpecificPartEncoder(String serverAuthorityPublicKey) {
        this.cleaEciesEncoder = new CleaEciesEncoder();
        this.serverAuthorityPublicKey = serverAuthorityPublicKey;
    }
  
    public byte[] encode(LocationSpecificPart locationSpecificPart) throws CleaEncryptionException {
        byte[] header = this.binaryEncodedHeader(locationSpecificPart);
        byte[] msg = this.binaryEncodedMessage(locationSpecificPart);
        byte[] encryptedLocationSpecificPart = this.encrypt(header, msg, this.serverAuthorityPublicKey);
        log.debug("Final Binary QR-code: {}", encryptedLocationSpecificPart);
        return encryptedLocationSpecificPart;
    }

    /**
     * Encode the data header in binary format: | version | LSPtype | pad | LTId |
     * 
     * @return data header in binary format
     */
    private byte[] binaryEncodedHeader(LocationSpecificPart locationSpecificPart) {
        BitUtils header = new BitUtils(8 * CleaEciesEncoder.HEADER_BYTES_SIZE);
        /* version (3 bits) */
        header.setNextInteger(locationSpecificPart.getVersion(), 3);
        /* LSPtype (3 bits) */
        header.setNextInteger(locationSpecificPart.getType(), 3);
        /* padding (2 bits) */
        header.setNextInteger(0x0, 2);
        /* LTId (16 bytes) */
        byte[] uuidB = new byte[16];
        uuidB = this.cleaEciesEncoder.uuidToBytes(locationSpecificPart.getLocationTemporaryPublicId());
        header.setNextByte(uuidB, 128);

        return header.getData();
    }

    /**
     * Encode the data message in binary format: | Staff | pad2 |CRIexp | vType |
     * vCat1 | vCat2 | countryCode | | periodDuration | ct_periodStart | t_qrStart |
     * LTKey |
     * 
     * @return data message in binary format
     * @throws CleaEncryptionException 
     * @throws Exception
     */
    private byte[] binaryEncodedMessage(LocationSpecificPart locationSpecificPart) throws CleaEncryptionException {
        BitUtils message = new BitUtils(8 * CleaEciesEncoder.MSG_BYTES_SIZE);

        /* staff (1 bit) */
        message.setNextInteger(locationSpecificPart.isStaff() ? 1 : 0, 1);
        /* locContactMsgPresent (1 bit) */
        message.setNextInteger(locationSpecificPart.isLocationContactMessagePresent() ? 1 : 0, 1);
        /* countryCode (12 bits) */
        message.setNextInteger(locationSpecificPart.getCountryCode(), 12);
        /* CRIexp (5 bits) */
        message.setNextInteger(locationSpecificPart.getQrCodeRenewalIntervalExponentCompact(), 5);
        /* venueType (5 bits) */
        message.setNextInteger(locationSpecificPart.getVenueType(), 5);
        /* venueCategory1 (4 bits) */
        message.setNextInteger(locationSpecificPart.getVenueCategory1(), 4);
        /* venueCategory2 (4 bits) */
        message.setNextInteger(locationSpecificPart.getVenueCategory2(), 4);
        /* periodDuration (1 byte) */
        message.setNextInteger(locationSpecificPart.getPeriodDuration(), 8);
        /* ct_periodStart (24 bits) */
        message.setNextInteger(locationSpecificPart.getCompressedPeriodStartTime(), 24);
        /* t_qrStart (32 bits) */
        message.setNextInteger(locationSpecificPart.getQrCodeValidityStartTime(), 32);
        /* LTKey (32 bytes) */
        message.setNextByte(locationSpecificPart.getLocationTemporarySecretKey(), 256);
        /* Encode the locContactMsg with encryption if required */
        if (locationSpecificPart.isLocationContactMessagePresent()) {
            /* Add the encrypted locationContactMessage */
            try {
                return cleaEciesEncoder.concat(message.getData(), locationSpecificPart.getEncryptedLocationContactMessage());
            } catch (IOException e) {
                log.error("Cannot concatenate data", e);
                throw new CleaEncryptionException(e);
            }
        } else {
            return message.getData();
        }
    }

    /**
     * Encrypt, respecting CLEA protocol: | header | msg |
     * 
     * @param header associated data
     * @param message    message to encrypt
     * @param publicKey EC public key
     * 
     * @return data encrypted in binary format (bytes array)
     * @throws CleaEncryptionException 
     */
    private byte[] encrypt(byte[] header, byte[] message, String publicKey) throws CleaEncryptionException {
        try {
            byte[] encryptedMessage = this.cleaEciesEncoder.encrypt(header, message, publicKey);
            log.debug("message length: {}, encrypted message length: {} ", message.length, encryptedMessage.length);
            return encryptedMessage;            
        } catch (Exception e) {
            log.error("Cannot encrypt Location Specific Part", e);
            throw new CleaEncryptionException(e);
        }
    }
    
}
