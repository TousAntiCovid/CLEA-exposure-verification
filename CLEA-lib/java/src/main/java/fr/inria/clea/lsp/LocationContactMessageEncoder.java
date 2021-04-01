package fr.inria.clea.lsp;

import java.io.IOException;
import java.security.InvalidAlgorithmParameterException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.spec.InvalidKeySpecException;
import java.util.Set;

import javax.validation.ConstraintViolation;
import javax.validation.Validation;
import javax.validation.Validator;
import javax.validation.ValidatorFactory;

import org.bouncycastle.crypto.InvalidCipherTextException;

import fr.devnied.bitlib.BitUtils;
import fr.inria.clea.lsp.exception.CleaCryptoException;
import fr.inria.clea.lsp.exception.CleaEncryptionException;
import fr.inria.clea.lsp.exception.CleaInvalidLocationContactMessageException;
import fr.inria.clea.lsp.utils.TimeUtils;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class LocationContactMessageEncoder {
    private String manualContactTracingAuthorityPublicKey;
    private CleaEciesEncoder cleaEncoder;
    private Validator validator;
    
    public LocationContactMessageEncoder(String manualContactTracingAuthorityPublicKey) {
        super();
        this.manualContactTracingAuthorityPublicKey = manualContactTracingAuthorityPublicKey;
        cleaEncoder = new CleaEciesEncoder();
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    public byte[] encode(LocationContact message) throws CleaCryptoException {
        this.validateMessage(message);
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

    protected void validateMessage(LocationContact message) throws CleaInvalidLocationContactMessageException {
        Set<ConstraintViolation<LocationContact>> violations = validator.validate(message);
        for (ConstraintViolation<LocationContact> violation : violations) {
            log.error(violation.getMessage()); 
        }
        if (!violations.isEmpty()) {
            throw new CleaInvalidLocationContactMessageException(violations);
        }
    }
    
    /**
     * Encode the data locContactMsg in binary format: 
     * | locationPhone | pad | locationRegion | locationPin | t_periodStart |
     * 
     * @return message in binary format
     */
    public byte[] getBinaryMessage(LocationContact message) {
        BitUtils locationContactMessage = new BitUtils(8 * CleaEciesEncoder.LOC_BYTES_SIZE);
        int digit, i, iend;

        /* locationPhone: 60 bits with 4-bit nibble by digit (0xf when empty) */
        for (i = 0; i < message.getLocationPhone().length(); i++) {
            /* convert the char in its value */
            digit = message.getLocationPhone().charAt(i) - 48;
            locationContactMessage.setNextInteger(digit, 4);
        }
        /* 0xf for the remaining 4-bit nibbles up to fifteen */
        iend = i;
        for (i = iend; i < 15; i++) {
            locationContactMessage.setNextInteger(0x0f, 4);
        }
 
        /* padding (4 bits) */
        locationContactMessage.setNextInteger(0x0, 4);
        
        /* t_periodStart (8 bits) */
        locationContactMessage.setNextInteger(message.getLocationRegion(), 8);

        /* locationPIN: 3 bytes with 4-bit nibble by digit */
        for (i = 0; i <  message.getLocationPin().length(); i++) {
            /* convert the char in its value */
            digit = message.getLocationPin().charAt(i) - 48;
            locationContactMessage.setNextInteger(digit, 4);
        }

        /* t_periodStart (32 bits) */
        long periodStartTime = TimeUtils.ntpTimestampFromInstant(message.getPeriodStartTime());
        locationContactMessage.setNextLong(periodStartTime, 32);

        return locationContactMessage.getData();
    }
    
    /**
     * Decode an encrypted Location contact message:
     * | locationPhone | locationPIN | t_periodStart |
     * @throws CleaEncryptionException 
     */
    public LocationContact decode(byte[] encryptedLocationContactMessage) throws CleaCryptoException {
        try {
            /* Decrypt the data */
            byte[] binaryLocationContactMessage = cleaEncoder.decrypt(encryptedLocationContactMessage, this.manualContactTracingAuthorityPublicKey , false);
            BitUtils bitLocationContactMessage = new BitUtils(binaryLocationContactMessage);

            int i, digit;
            /*
             * locationPhone: 60 bits with 4-bit nibble by digit (0xf when empty) => 15
             * digits max
             */
            StringBuilder locationPhone = new StringBuilder(15);
            for (i = 0; i < 15; i++) {
                /* unpack the 4-bit nibbles */
                digit = bitLocationContactMessage.getNextInteger(4);
                if (digit != 0xf) {
                    locationPhone.append(digit);
                }
            }
            
            /* padding (4 bits) */
            int pad = bitLocationContactMessage.getNextInteger(4);
            assert (pad == 0) : "LSP decoding, padding error";
 
            /* locationRegion (1 octet) */
            int locationRegion = bitLocationContactMessage.getNextInteger(8);

            /* locationPIN: 3 bytes with 4-bit nibble by digit => 6 digits */
            StringBuilder locationPin = new StringBuilder(6);
            for (i = 0; i < 6; i++) {
                /* unpack the 4-bit nibbles */
                locationPin.append(bitLocationContactMessage.getNextInteger(4));
            }
            
            /* t_periodStart (32 bits) */
            long periodStartTime = bitLocationContactMessage.getNextLong(32);
            
            LocationContact locationContact = new LocationContact(locationPhone.toString(), 
<<<<<<< HEAD
                    locationRegion, locationPin.toString(), TimeUtils.instantFromTimestamp(periodStartTime));
=======
                            locationRegion, locationPin.toString(), TimeUtils.instantFromTimestamp(periodStartTime));
>>>>>>> mybranch
            this.validateMessage(locationContact);            
            return locationContact;
            
        } catch (NoSuchAlgorithmException | InvalidKeySpecException | IllegalStateException | InvalidCipherTextException
                | IOException e) {
            throw new CleaEncryptionException(e);
        }
    }
}
