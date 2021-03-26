package fr.inria.clea.lsp;

import java.io.IOException;
import java.security.InvalidAlgorithmParameterException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.spec.InvalidKeySpecException;

import org.bouncycastle.crypto.InvalidCipherTextException;

import fr.devnied.bitlib.BitUtils;
import fr.inria.clea.lsp.utils.TimeUtils;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class LocationContactMessageEncoder {
    private String manualContactTracingAuthorityPublicKey;
    private CleaEciesEncoder cleaEncoder;
    
    public LocationContactMessageEncoder(String manualContactTracingAuthorityPublicKey) {
        super();
        this.manualContactTracingAuthorityPublicKey = manualContactTracingAuthorityPublicKey;
        cleaEncoder = new CleaEciesEncoder();
    }

    public byte[] encode(LocationContact message) throws CleaEncryptionException {
        try {
            byte[] messageBinary = this.getBinaryMessage(message);
            byte[] encryptedLocationContactMessage = cleaEncoder.encrypt(null, messageBinary, manualContactTracingAuthorityPublicKey);
            log.debug("DBG_enc::MSG " + messageBinary.length);
            log.debug("DBG_enc::ENCRYPT " + encryptedLocationContactMessage.length);

            return encryptedLocationContactMessage;
        } catch (NoSuchAlgorithmException | InvalidKeySpecException | NoSuchProviderException
                | InvalidAlgorithmParameterException | IllegalStateException | InvalidCipherTextException
                | IOException e) {
            throw new CleaEncryptionException(e);
        }
    }
    
    /**
     * Encode the data locContactMsg in binary format: 
     * | locationPhone | locationPin | t_periodStart |
     * 
     * @return message in binary format
     */
    public byte[] getBinaryMessage(LocationContact message) {
        BitUtils locationContactMessage = new BitUtils(8 * CleaEciesEncoder.LOC_BYTES_SIZE);
        int digit, i, iend;

        /* locationPhone: 8 bytes with 4-bit nibble by digit (0xf when empty) */
        for (i = 0; i < message.getLocationPhone().length(); i++) {
            /* convert the char in its value */
            digit = message.getLocationPhone().charAt(i) - 48;
            locationContactMessage.setNextInteger(digit, 4);
        }
        /* 0xf for the remaining 4-bit nibbles up to sixteen */
        iend = i;
        for (i = iend; i < 16; i++) {
            locationContactMessage.setNextInteger(0x0f, 4);
        }

        /* locationPIN: 4 bytes with 4-bit nibble by digit */
        for (i = 0; i <  message.getLocationPin().length(); i++) {
            /* convert the char in its value */
            digit = message.getLocationPin().charAt(i) - 48;
            locationContactMessage.setNextInteger(digit, 4);
        }

        /* t_periodStart (32 bits) */
        int periodStartTime = (int) TimeUtils.ntpTimestampFromInstant(message.getPeriodStartTime());
        locationContactMessage.setNextInteger(periodStartTime, 32);

        return locationContactMessage.getData();
    }
    
    /**
     * Decode an encrypted Location contact message:
     * | locationPhone | locationPIN | t_periodStart |
     * @throws CleaEncryptionException 
     */
    public LocationContact decode(byte[] encryptedLocationContactMessage) throws CleaEncryptionException {
        try {
            /* Decrypt the data */
            byte[] binaryLocationContactMessage = cleaEncoder.decrypt(encryptedLocationContactMessage, this.manualContactTracingAuthorityPublicKey , false);
            BitUtils bitLocationContactMessage = new BitUtils(binaryLocationContactMessage);

            int i, digit;
            /*
             * locationPhone: 8 bytes with 4-bit nibble by digit (0xf when empty) => 16
             * digits max
             */
            StringBuilder locationPhone = new StringBuilder(16);
            for (i = 0; i < 16; i++) {
                /* unpack the 4-bit nibbles */
                digit = bitLocationContactMessage.getNextInteger(4);
                if (digit != 0xf) {
                    locationPhone.append(digit);
                }
            }

            /* locationPIN: 4 bytes with 4-bit nibble by digit => 8 digits */
            StringBuilder locationPin = new StringBuilder(8);
            for (i = 0; i < 8; i++) {
                /* unpack the 4-bit nibbles */
                locationPin.append(bitLocationContactMessage.getNextInteger(4));
            }

            /* t_periodStart (32 bits) */
            int periodStartTime = bitLocationContactMessage.getNextInteger(32);
            
            return new LocationContact(locationPhone.toString(), 
                    locationPin.toString(), TimeUtils.instantFromTimestamp(periodStartTime));
        } catch (NoSuchAlgorithmException | InvalidKeySpecException | IllegalStateException | InvalidCipherTextException
                | IOException e) {
            throw new CleaEncryptionException(e);
        }
    }
}
