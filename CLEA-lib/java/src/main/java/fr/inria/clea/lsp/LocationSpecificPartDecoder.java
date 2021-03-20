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
import fr.inria.clea.lsp.EncryptedLocationSpecificPart.EncryptedLocationSpecificPartBuilder;
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

    public LocationSpecificPartDecoder(String servertAuthoritySecretKey) {
        this.cleaEciesEncoder = new CleaEciesEncoder();
        this.serverAuthoritySecretKey = servertAuthoritySecretKey;
    }

    /**
     * Unpack the data decrypted header (binary format): 
     * | version | LSPtype | pad | LTId | to extract parameters
     */
    private EncryptedLocationSpecificPart decodeHeader(byte[] binaryLocationSpecificPart) {
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
     */
    public EncryptedLocationSpecificPart decrypt(String lspBase64) throws CleaEncryptionException {
        byte[] encryptedLocationSpecificPart = Base64.getDecoder().decode(lspBase64);
        log.debug("Base 64 decoded LSP: {}", encryptedLocationSpecificPart);
        byte[] binaryLocationSpecificPart;
        try {
            binaryLocationSpecificPart = this.cleaEciesEncoder.decrypt(encryptedLocationSpecificPart, this.serverAuthoritySecretKey, true);
        } catch (NoSuchAlgorithmException | InvalidKeySpecException | IllegalStateException | InvalidCipherTextException
                | IOException e) {
            throw new CleaEncryptionException(e);
        }
        return this.decodeHeader(binaryLocationSpecificPart);
                //this.decodeMessage(binaryLocationSpecificPart);
    }
}
