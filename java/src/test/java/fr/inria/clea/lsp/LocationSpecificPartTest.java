/* 
* Copyright (C) Inria, 2021
*/
package fr.inria.clea.lsp;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.security.InvalidAlgorithmParameterException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.spec.InvalidKeySpecException;
import java.util.Base64;

import org.bouncycastle.crypto.InvalidCipherTextException;
import org.bouncycastle.util.encoders.Hex;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;

import fr.devnied.bitlib.BytesUtils;
import fr.inria.clea.lsp.utils.TimeUtils;

/**
 * Some locationSpecificPart (LSP) CLEA tests
 * 
 * @see <a href="https://hal.inria.fr/hal-03146022">CLEA protocol</a>
 */
class LocationSpecificPartTest {

    /* Example of a permanent Location Secret Key used for the tests */
    private final String permanentLocationSecretKey = "23c9b8f36ac1c0cddaf869c3733b771c3dc409416a9695df40397cea53e7f39e21f76925fc0c74ca6ee7c7eafad92473fd85758bab8f45fe01aac504";
    private CleaEciesEncoder cleaEciesEncoder;
    private String[] serverAuthorityKeyPair;
    private String[] manualContactTracingAuthorityKeyPair;

    @BeforeEach
    public void setUp() throws Exception {
        cleaEciesEncoder = new CleaEciesEncoder();
        serverAuthorityKeyPair = cleaEciesEncoder.genKeysPair(true);
        System.out.println("Server Authority Private Key: " + serverAuthorityKeyPair[0]);
        System.out.println("Server Authority Public Key : " + serverAuthorityKeyPair[1]);
        manualContactTracingAuthorityKeyPair = cleaEciesEncoder.genKeysPair(true);
    }
    
    @Test
    public void testCleaEciesEncodingAndDecodingOfData() throws NoSuchAlgorithmException, InvalidKeySpecException, NoSuchProviderException, InvalidAlgorithmParameterException, IllegalStateException, InvalidCipherTextException, IOException {
        /* Message to encrypt and decrypt */
        String plainText = "9F7213093CEDBBE66356550296A37DD18077E8646185EA2EA0EAFE88630F8C861A2E05F35BB2D863A28841CF";
        String headerText = "7D1BBFB6CAD6C2E862A7AEAD7DA27FB814";
        byte[] message = Hex.decode(plainText);
        byte[] header = Hex.decode(headerText);
        byte[] encrypted = cleaEciesEncoder.encrypt(header, message, serverAuthorityKeyPair[1]);
        /* Display */
        System.out.println("***" + BytesUtils.bytesToString(header));
        System.out.println("PLAINTEXT : " + plainText);
        System.out.println("CODEDTEXT : " + BytesUtils.bytesToString(encrypted));

        /* Decrypt and test the result */
        byte[] decrypted = cleaEciesEncoder.decrypt(encrypted, serverAuthorityKeyPair[0], true);

        assertThat(decrypted).containsExactly(cleaEciesEncoder.concat(header, message));
    }

    @Test
    public void testEncodinsAndDecodingOfALocationMessage() throws CleaEncryptionException {
        int periodStartTime = TimeUtils.hourRoundedCurrentTimeTimestamp32();
        LocationContact locationContact = new LocationContact("0612150292", "01234567", periodStartTime);
        Location location = Location.builder()
                .contact(locationContact)
                .manualContactTracingAuthorityPublicKey(manualContactTracingAuthorityKeyPair[1])
                .permanentLocationSecretKey(permanentLocationSecretKey)
                .build();
        
        byte[] encryptedLocationContactMessage = location.getLocationContactMessageEncrypted();
        LocationContact decodedLocationContact = new LocationContactMessageEncoder(manualContactTracingAuthorityKeyPair[0]).decode(encryptedLocationContactMessage);
        
        assertThat(decodedLocationContact).isEqualTo(locationContact);
    }
    
    @Test
    public void testLocationSpecificPartEncoding() throws CleaEncryptionException {
        int periodStartTime = TimeUtils.hourRoundedCurrentTimeTimestamp32();
        LocationContact locationContact = new LocationContact("33800130000", "01234567", periodStartTime);
        LocationSpecificPart lsp = LocationSpecificPart.builder()
            .staff(false)
            .countryCode(592)
            .qrCodeRenewalIntervalExponentCompact(10)
            .venueType(15)
            .venueCategory1(0)
            .venueCategory2(2)
            .periodDuration(3)
            .qrCodeValidityStartTime(periodStartTime * 3600)
            .build();
        Location location = Location.builder()
                .contact(locationContact)
                .locationSpecificPart(lsp)
                .manualContactTracingAuthorityPublicKey(manualContactTracingAuthorityKeyPair[1])
                .serverAuthorityPublicKey(serverAuthorityKeyPair[1])
                .permanentLocationSecretKey(permanentLocationSecretKey)
                .build();
        location.setPeriodStartTime(periodStartTime);
        
        byte[] encryptedLocationSpecificPart = location.getLocationSpecificPartEncrypted();
        
        assertThat(encryptedLocationSpecificPart).isNotNull();
        assertThat(encryptedLocationSpecificPart).isEqualTo(0); // TODO replace by expected value
    }

    @Test
    public void testEncodingAndDecodingOfALocationSpecificPart() throws CleaEncryptionException {
        int periodStartTime = TimeUtils.hourRoundedCurrentTimeTimestamp32();
        LocationContact locationContact = new LocationContact("0612150292", "01234567", periodStartTime);
        /* Encode a LSP with location */
        LocationSpecificPart lsp = LocationSpecificPart.builder()
                .staff(true)
                .countryCode(33)
                .qrCodeRenewalIntervalExponentCompact(2)
                .venueType(4)
                .venueCategory1(0)
                .venueCategory2(0)
                .periodDuration(3)
                .build();
        Location location = Location.builder()
                .contact(locationContact)
                .locationSpecificPart(lsp)
                .manualContactTracingAuthorityPublicKey(manualContactTracingAuthorityKeyPair[1])
                .serverAuthorityPublicKey(serverAuthorityKeyPair[1])
                .permanentLocationSecretKey(permanentLocationSecretKey)
                .build();
        location.setPeriodStartTime(periodStartTime);
        
        /* Encode a LSP with location */
        String encryptedLocationSpecificPart = location.getLocationSpecificPartEncryptedBase64();
        /* Decode the encoded LSP */
        LocationSpecificPart decodedLsp = new LocationSpecificPartDecoder(serverAuthorityKeyPair[0]).decrypt(encryptedLocationSpecificPart);
        
        assertThat(decodedLsp).isEqualTo(lsp);
    }
    
    @Test
    public void testEncodingAndDecodingOfALocationSpecificPartWithoutLocationContact() throws CleaEncryptionException {
        int periodStartTime = TimeUtils.hourRoundedCurrentTimeTimestamp32();
        /* Encode a LSP with location */
        LocationSpecificPart lsp = LocationSpecificPart.builder()
                .staff(true)
                .countryCode(33)
                .qrCodeRenewalIntervalExponentCompact(2)
                .venueType(4)
                .venueCategory1(0)
                .venueCategory2(0)
                .periodDuration(3)
                .build();
        Location location = Location.builder()
                .locationSpecificPart(lsp)
                .serverAuthorityPublicKey(serverAuthorityKeyPair[1])
                .permanentLocationSecretKey(permanentLocationSecretKey)
                .build();
        location.setPeriodStartTime(periodStartTime);
        
        /* Encode a LSP with location */
        String encryptedLocationSpecificPart = location.getLocationSpecificPartEncryptedBase64();
        /* Decode the encoded LSP */
        LocationSpecificPart decodedLsp = new LocationSpecificPartDecoder(serverAuthorityKeyPair[0]).decrypt(encryptedLocationSpecificPart);
        
        assertThat(decodedLsp).isEqualTo(lsp);
    }

    @Test
    public void testDecryptionFromMessageEncryptedByCleaCLibrary() throws NoSuchAlgorithmException, InvalidKeySpecException, IllegalStateException, InvalidCipherTextException, IOException {
        /* EC private key from C package */
        final String privateKey = "7422c9883c3f6c5ac70c0a08a24b5d524f36edefa04f599e316fa23ef74a4a0f";
        /* message encrypted, from C package */
        final String cipherText_S = "2f1376e97378d2e19a5d3b15cf4ff1802f971e4dc357f727098b5e0ba114318f3cdc0af35728c2ccf24641f879110fbc63f7dc9c002247d9073fc46f4e6bb2a85ae19c8e2db9de9071b1887bfe0388c333e8b8d89271f70406a86bfab0d44fc54641f1dda61292a5fe32ce128eb4";
        /* message in plain text, from C package */
        final String plainText_S = "2f1376e97378d2e19a5d3b15cf4ff1802ffd9be701f74d6f15d4a3f22fbcd296eb6bdf21ba03410a01f816b91cb2c74d709544912811e8a867dd44b8d6";

        /* String -> bytes array */
        byte[] cipherText = Hex.decode(cipherText_S);
        byte[] plainTextMessage = Hex.decode(plainText_S);

        /* Java decrypt the message using the EC private key privKey */
        CleaEciesEncoder cleaEncoder = new CleaEciesEncoder();
        byte[] decryptedMessage = cleaEncoder.decrypt(cipherText, privateKey, true);

        assertThat(decryptedMessage).containsExactly(plainTextMessage);
    }

    @Test
    public void testQrCodeGeneration() throws Exception {
        int periodStartTime = TimeUtils.hourRoundedCurrentTimeTimestamp32();
        /* Encode the LSP */
        LocationSpecificPart lsp = LocationSpecificPart.builder()
                .staff(true)
                .countryCode(33)
                .qrCodeRenewalIntervalExponentCompact(2)
                .venueType(4)
                .venueCategory1(0)
                .venueCategory2(0)
                .periodDuration(3)
                .build();
        Location location = Location.builder()
                .locationSpecificPart(lsp)
                .serverAuthorityPublicKey(serverAuthorityKeyPair[1])
                .permanentLocationSecretKey(permanentLocationSecretKey)
                .build();
        location.setPeriodStartTime(periodStartTime);

        /* QR-code = "country-specific-prefix" / "Base64(location-specific-part)" */
        String qrCode = "https://tac.gouv.fr/" + location.getLocationSpecificPartEncryptedBase64();

        /* encode Qrcode with default parameters, level L */
        BitMatrix bitMatrix = new QRCodeWriter().encode(qrCode, BarcodeFormat.QR_CODE, 200, 200);

        /* generate an image */
        String imageFormat = "png";
        String outputFileName = "./qrcode-" + lsp.getQrCodeValidityStartTime() + "." + imageFormat;
        FileOutputStream fileOutputStream = new FileOutputStream(new File(outputFileName));
        MatrixToImageWriter.writeToStream(bitMatrix, imageFormat, fileOutputStream);
        fileOutputStream.close();
    }

    /**
     * Testing base64 decoding from C or Java
     */
    @Test
    public void testBase64DecodingFromCToJava() {

        String lsp64C = "APJexM7Ntkr9l2JO6mpD3HWO9OkU9nDygdP16KhNAiR9JUr05mT9+5kvJbZph/GdRbIqpQCwgFlYkWEr633BiYhJ+x/pc581PYG4aF2ZzjDJfrY5PfZodBKEiWH+Qegtp3x2bw4sfbCM8OPIvPtU7ooyyzj9h7RdKp4GfgeCz9YdikJ8uJYusQaFILrICqswxQzQPhLVHsnkMjAVpayCAxUOVgZbqj5m8lNcMhCxog==";
        String lsp64J = "AJMxSV4mHDX9iqjedPJRtpd7XTx53/ZCrZ4l53yFT7CSbiksSWm6vXApD+XeHT5nLEPbVRPXQoY8PJaTakCQNXYa2EUb8UW62n7sMua+UmZwDnf/9OPOVwWyGacP5L94sv0fCk7XnjBbDLhtORCGrdiwkOm3UniGc8gyP41zneHQSmbfzq6kEzFCX2kfKQIXIsFVyCFp7M4KdpVb2oWg2Q/FZr63cBdObwI9mrImCw==";
        Base64.getDecoder().decode(lsp64C);

        // TODO: I do not understand this test. Result of the decoding is not used
        System.out.println("Qrcode size C=" + lsp64C.length() + " Java=" + lsp64J.length());
    }

    @Test
    public void testDecodingOfLocationSpecificPartInBase64() throws CleaEncryptionException {
        final String lsp_base64 = "AJi9dKxk4aXcRhF9lIIiGchvIbwtd2BE72nelq4/+uF0T0hE/GA0hFpEpuhVi+Xla8irZbGRmcDIfMqs0e8j/eChcYTeHo+bjWyN2GsHo+F5F46o0cM0IWuw/1MgctXYFCUw53zPL2Cs1ERN3HTpxnL9us2y//P+r8qV39YnmjFUj61Rlrosk2r81NO6BQImmg5sSV31rOTWXNrUwNQSTmXki0E+hfLgi9aMeWMnXQ==";
        final String servertAuthoritySecretKey = "34af7f978c5a17772867d929e0b800dd2db74608322d73f2f0cfd19cdcaeccc8";
//        final String SK_MCTA = "3108f08b1485adb6f72cfba1b55c7484c906a2a3a0a027c78dcd991ca64c97bd";

        LocationSpecificPartDecoder decoder = new LocationSpecificPartDecoder(servertAuthoritySecretKey);

        LocationSpecificPart lsp = decoder.decrypt(lsp_base64);
        // TODO: add assertions 
        // assertThat(lsp.getCountryCode()).isEqualTo(??);
        System.out.println(lsp);
    }

    @Test
    public void testLocationSpecificPartBase64EciesDecryption() throws NoSuchAlgorithmException, InvalidKeySpecException, IllegalStateException, InvalidCipherTextException, IOException {
        /* EC private key from C package */
        final String privateKey = "34af7f978c5a17772867d929e0b800dd2db74608322d73f2f0cfd19cdcaeccc8";
        /* message encrypted, from C package */
        final String cipherTextBase64 = "AA3sinpPVKOpedLxzpMbgS3G4d4Up1vDcNQ28fZKB8cBnBjNYV0bPGoaBxnFFab/iM56uzSoDQ0i+N5B9shw5bBmutRONcWQBhNr1ug/0sZ62UaiWZjqfYDmpANJHfv0Kao3DUJvPLHep7N9uNlOywOhHISXoFsCNvikOv8o3hN9j7vbtW6xjILpbQP01gNtiNqliKDGWCSo/g4xlFjlbiWo4E1bL5UiWHAS9KYj8g==";

        /* String -> bytes array */
        byte[] cipherText = Base64.getDecoder().decode(cipherTextBase64);
        System.out.println("CIFFER LSP " + BytesUtils.bytesToString(cipherText));

        /* Java decrypt the message using the EC private key privKey */
        byte[] message = new CleaEciesEncoder().decrypt(cipherText, privateKey, true);
        System.out.println("PLAIN LSP: " + BytesUtils.bytesToString(message));
        // TODO: add assertions 
    }

}
