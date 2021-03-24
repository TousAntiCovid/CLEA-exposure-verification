/* 
 * Copyright (C) Inria, 2021
 */
package fr.inria.clea.lsp;

import java.util.UUID;

import javax.validation.constraints.Max;

import org.bouncycastle.util.Arrays;

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
    

    public LocationSpecificPart decrypt(LocationSpecificPartDecoder decoder) throws CleaEncryptionException, CleaEncodingException {
        return decoder.decrypt(this.binaryEncoded());
    }

    protected byte[] binaryEncoded() {
        return Arrays.concatenate(this.binaryEncodedHeader(), encryptedLocationMessage);
    }
    
    public byte[] binaryEncodedHeader() {
        return new LocationSpecificPartEncoder().binaryEncodedHeader(this.type, this.version, this.locationTemporaryPublicId);
    }
}
