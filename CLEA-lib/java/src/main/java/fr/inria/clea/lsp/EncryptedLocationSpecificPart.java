/* 
 * Copyright (C) Inria, 2021
 */
package fr.inria.clea.lsp;

import java.util.Arrays;
import java.util.UUID;

import javax.validation.constraints.Max;

import fr.devnied.bitlib.BitUtils;
import fr.inria.clea.lsp.LocationSpecificPart.LocationSpecificPartBuilder;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

/**
 * LocationSpecificPart (LSP) contents data respecting the CLEA protocol
 * 
 * @see <a href="https://hal.inria.fr/hal-03146022">CLEA protocol</a>
 */
@Builder
@Getter
@EqualsAndHashCode
@ToString
public class EncryptedLocationSpecificPart {
    /* Clea protocol version number */
    @Builder.Default
    @Max(value = 8)
    private int version = 0;
    
    /*
     * LSP type, in order to be able to use multiple formats in parallel in the
     * future.
     */
    @Builder.Default
    @Max(value = 8)
    private int type = 0;
    
    /*
     * Location Temporary public universally unique Identifier (UUID), specific to a
     * given location at a given period.
     */
    private UUID locationTemporaryPublicId;
    
    /*
     * Location Specific Part in binary format without the Header (non-encrypted data) and encrypted.
     */
    private byte[] encryptedLocationMessage;
    
    /**
     * Unpack the data message (binary format) :
     * | Staff | pad2 |CRIexp | vType |
     * vCat1 | vCat2 | countryCode | | periodDuration | ct_periodStart | t_qrStart |
     * LTKey | to extract parameters
     */
    public LocationSpecificPart decodeMessage() {
        byte[] messageBinary = Arrays.copyOfRange(encryptedLocationMessage, 0, CleaEciesEncoder.MSG_BYTES_SIZE);
        BitUtils message = new BitUtils(messageBinary);
        byte[] encryptedLocationContactMessage = Arrays.copyOfRange(encryptedLocationMessage, 
                CleaEciesEncoder.MSG_BYTES_SIZE, encryptedLocationMessage.length);
        if (encryptedLocationContactMessage.length == 0) {
            encryptedLocationContactMessage = null;
        }
        
        LocationSpecificPartBuilder locationSpecificPartbuilder = LocationSpecificPart.builder()
            .version(version)
            .type(type)
            .locationTemporaryPublicId(locationTemporaryPublicId)
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
        return locationSpecificPartbuilder.build();            
    }
}
