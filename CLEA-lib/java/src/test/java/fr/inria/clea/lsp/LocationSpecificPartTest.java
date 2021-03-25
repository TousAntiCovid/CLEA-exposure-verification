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
import java.util.Arrays;
import java.util.Base64;
import java.util.Random;
import java.util.UUID;

import org.bouncycastle.crypto.InvalidCipherTextException;
import org.bouncycastle.util.encoders.Hex;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvFileSource;

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
    public void testCleaEciesEncodingAndDecodingOfData()
            throws NoSuchAlgorithmException, InvalidKeySpecException, NoSuchProviderException,
            InvalidAlgorithmParameterException, IllegalStateException, InvalidCipherTextException, IOException {
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
        LocationContact locationContact = new LocationContact("33800130000", 11, "012345", periodStartTime);
        Location location = Location.builder().contact(locationContact)
                .manualContactTracingAuthorityPublicKey(manualContactTracingAuthorityKeyPair[1])
                .permanentLocationSecretKey(permanentLocationSecretKey).build();

        byte[] encryptedLocationContactMessage = location.getLocationContactMessageEncrypted();
        LocationContact decodedLocationContact = new LocationContactMessageEncoder(
                manualContactTracingAuthorityKeyPair[0]).decode(encryptedLocationContactMessage);

        assertThat(decodedLocationContact).isEqualTo(locationContact);
    }

    @Test
    public void testEncodingAndDecodingOfALocationSpecificPart() throws CleaEncryptionException, CleaEncodingException {
        int periodStartTime = TimeUtils.hourRoundedCurrentTimeTimestamp32();
        LocationContact locationContact = new LocationContact("33800130000", 11, "012345", periodStartTime);
        /* Encode a LSP with location */
        LocationSpecificPart lsp = LocationSpecificPart.builder().staff(true).countryCode(33)
                .qrCodeRenewalIntervalExponentCompact(2).venueType(4).venueCategory1(0).venueCategory2(0)
                .periodDuration(3).build();
        Location location = Location.builder().contact(locationContact).locationSpecificPart(lsp)
                .manualContactTracingAuthorityPublicKey(manualContactTracingAuthorityKeyPair[1])
                .serverAuthorityPublicKey(serverAuthorityKeyPair[1])
                .permanentLocationSecretKey(permanentLocationSecretKey).build();
        location.setPeriodStartTime(periodStartTime);

        /* Encode a LSP with location */
        String encryptedLocationSpecificPart = location.getLocationSpecificPartEncryptedBase64();
        assertThat(encryptedLocationSpecificPart).isNotNull();
        /* Decode the encoded LSP */
        LocationSpecificPart decodedLsp = new LocationSpecificPartDecoder(serverAuthorityKeyPair[0])
                .decrypt(encryptedLocationSpecificPart);

        assertThat(decodedLsp).isEqualTo(lsp);
        assertThat(lsp.getEncryptedLocationContactMessage()).isNotNull();
    }

    @Test
    public void testEncodingAndDecodingOfALocationSpecificPartWithoutLocationContact()
            throws CleaEncryptionException, CleaEncodingException {
        int periodStartTime = TimeUtils.hourRoundedCurrentTimeTimestamp32();
        /* Encode a LSP with location */
        LocationSpecificPart lsp = LocationSpecificPart.builder().staff(true).countryCode(33)
                .qrCodeRenewalIntervalExponentCompact(2).venueType(4).venueCategory1(0).venueCategory2(0)
                .periodDuration(3).build();
        Location location = Location.builder().locationSpecificPart(lsp)
                .serverAuthorityPublicKey(serverAuthorityKeyPair[1])
                .permanentLocationSecretKey(permanentLocationSecretKey).build();
        location.setPeriodStartTime(periodStartTime);
        location.setQrCodeValidityStartTime(periodStartTime, periodStartTime+120);

        /* Encode a LSP with location */
        String encryptedLocationSpecificPart = location.getLocationSpecificPartEncryptedBase64();
        /* Decode the encoded LSP */
        LocationSpecificPart decodedLsp = new LocationSpecificPartDecoder(serverAuthorityKeyPair[0])
                .decrypt(encryptedLocationSpecificPart);

        assertThat(decodedLsp).isEqualTo(lsp);
    }

    @Disabled("compute new String values with c lib")
    @Test
    public void testDecryptionFromMessageEncryptedByCleaCLibrary() throws NoSuchAlgorithmException,
            InvalidKeySpecException, IllegalStateException, InvalidCipherTextException, IOException {
        /* EC private key from C package */
        final String privateKey = "34af7f978c5a17772867d929e0b800dd2db74608322d73f2f0cfd19cdcaeccc8";
        /* message encrypted, from C package */
        final String cipherText_S = "7d1bbfb6cad6c2e862a7aead7da27fb814cff9dbda33b7277d4bf507f04e5b901d7f4e09ff48b0f2ba8aebb4640d074f1daef17524b6f319f30e6548277e0575039d2560cfcf45d6873e4bf33a03c7c598bb6f1b7c3c4c5873ad35ff5be33e18a815af751d8cde256c7bd8318f04";
        /* message in plain text, from C package */
        final String plainText_S = "7D1BBFB6CAD6C2E862A7AEAD7DA27FB8149F7213093CEDBBE66356550296A37DD18077E8646185EA2EA0EAFE88630F8C861A2E05F35BB2D863A28841CF";

        /* String -> bytes array */
        byte[] cipherText = Hex.decode(cipherText_S);
        byte[] plainTextMessage = Hex.decode(plainText_S);

        /* Java decrypt the message using the EC private key privKey */
        CleaEciesEncoder cleaEncoder = new CleaEciesEncoder();
        byte[] decryptedMessage = cleaEncoder.decrypt(cipherText, privateKey, true);

        assertThat(decryptedMessage).containsExactly(plainTextMessage);
    }

    @Disabled("Keep this piece of code as example how to generate a Qrcode image")
    @Test
    public void testQrCodeGeneration() throws Exception {
        int periodStartTime = TimeUtils.hourRoundedCurrentTimeTimestamp32();
        /* Encode the LSP */
        LocationSpecificPart lsp = LocationSpecificPart.builder().staff(true).countryCode(33)
                .qrCodeRenewalIntervalExponentCompact(2).venueType(4).venueCategory1(0).venueCategory2(0)
                .periodDuration(3).build();
        Location location = Location.builder().locationSpecificPart(lsp)
                .serverAuthorityPublicKey(serverAuthorityKeyPair[1])
                .permanentLocationSecretKey(permanentLocationSecretKey).build();
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
     * Testing the decoding of a LSP in base64 (encoded by C lib)
     * testLSPDecoding.csv values are generated by the interoperability test
     * launched manually in python (in project/test)
     */
    @ParameterizedTest
    @CsvFileSource(resources = "/testLSPDecoding.csv", numLinesToSkip = 1)
    public void testDecodingOfLocationSpecificPartInBase64(int staff, int countryCode, String locationTemporaryPublicID,
            int qrCodeRenewalIntervalExponentCompact, int venueType, int venueCat1, int venueCat2, int periodDuration,
            int periodStartTime, long qrStartTime, String serverAuthoritySecretKey, String serverAuthorityPublicKey,
            String lspbase64) throws CleaEncryptionException, CleaEncodingException {
        LocationSpecificPartDecoder decoder = new LocationSpecificPartDecoder(serverAuthoritySecretKey);
        LocationSpecificPart lsp = decoder.decrypt(lspbase64);

        assertThat(lsp.isStaff()).isEqualTo(staff == 1);
        assertThat(lsp.getCountryCode()).isEqualTo(countryCode);
        assertThat(lsp.getQrCodeRenewalIntervalExponentCompact()).isEqualTo(qrCodeRenewalIntervalExponentCompact);
        assertThat(lsp.getLocationTemporaryPublicId()).isEqualTo(UUID.fromString(locationTemporaryPublicID));
        assertThat(lsp.getPeriodDuration()).isEqualTo(periodDuration);
        assertThat(lsp.getVenueType()).isEqualTo(venueType);
        assertThat(lsp.getVenueCategory1()).isEqualTo(venueCat1);
        assertThat(lsp.getVenueCategory2()).isEqualTo(venueCat2);
        assertThat(lsp.getCompressedPeriodStartTime()).isEqualTo(periodStartTime);
        /* Be careful the int qrCodeValidityStartTime is unsigned */
        System.out.println("qrCodeValidityStartTime = " + Integer.toUnsignedString(lsp.getQrCodeValidityStartTime()));
        int tstQrCodeValidityStartTime = Integer.compareUnsigned(lsp.getQrCodeValidityStartTime(), (int) qrStartTime);
        assertThat(tstQrCodeValidityStartTime).isEqualTo(0);
    }

    /**
     * Testing the encoding/decoding of a LSP in base64 testLSPDecoding.csv values
     * are generated by the interoperability test launched manually in python (in
     * project/test)
     */
    @ParameterizedTest
    @CsvFileSource(resources = "/testLSPDecoding.csv", numLinesToSkip = 1)
    public void testEncodingDecodingOfLSPSpecificPartInBase64(int staff, int countryCode,
            String locationTemporaryPublicID, int qrCodeRenewalIntervalExponentCompact, int venueType, int venueCat1,
            int venueCat2, int periodDuration, int periodStartTime, long qrStartTime, String serverAuthoritySecretKey,
            String serverAuthorityPublicKey, String lspbase64) throws CleaEncryptionException, CleaEncodingException {
        /* Use only testLSPDecoding.csv parameters to have a variety of parameters */
        /* times parameters and location are generated */
        int myPeriodStartTime = TimeUtils.hourRoundedCurrentTimeTimestamp32();
        Random rn = new Random();
        int nbDigits = rn.nextInt(6) + 10;
        String phone = generateRandomDigits(nbDigits);
        int region = rn.nextInt(255);
        String pinCode = generateRandomDigits(6);
        LocationContact locationContact = new LocationContact(phone, region, pinCode, myPeriodStartTime);
        /* Encode a LSP with location */
        LocationSpecificPart lsp = LocationSpecificPart.builder().staff(staff == 1).countryCode(countryCode)
                .qrCodeRenewalIntervalExponentCompact(qrCodeRenewalIntervalExponentCompact).venueType(venueType)
                .venueCategory1(venueCat1).venueCategory2(venueCat2).periodDuration(periodDuration).build();
        Location location = Location.builder().locationSpecificPart(lsp).contact(locationContact)
                .manualContactTracingAuthorityPublicKey(manualContactTracingAuthorityKeyPair[1])
                .serverAuthorityPublicKey(serverAuthorityPublicKey)
                .permanentLocationSecretKey(permanentLocationSecretKey).build();
        location.setPeriodStartTime(myPeriodStartTime);

        /* Encode a LSP with location */
        String encryptedLocationSpecificPart = location.getLocationSpecificPartEncryptedBase64();
        /* Decode the encoded LSP */
        LocationSpecificPart decodedLsp = new LocationSpecificPartDecoder(serverAuthoritySecretKey)
                .decrypt(encryptedLocationSpecificPart);

        assertThat(decodedLsp).isEqualTo(lsp);
    }

    /**
     * Testing the decoding of a Location inside a LSP in base64 (encoded by C lib)
     * testLocationDecoding.csv values are generated by the interoperability test
     * launched manually in python (in project/test)
     */
    @ParameterizedTest
    @CsvFileSource(resources = "/testLocationDecoding.csv", numLinesToSkip = 1)
    public void testDecodingOfLocationOnlyInBase64(String locationPhone, int locationRegion, String locationPin, long t_periodStart,
            String serverAuthoritySecretKey, String serverAuthorityPublicKey,
            String manualContactTracingAuthoritySecretKey, String manualContactTracingAuthorityPublicKey,
            String lspbase64) throws CleaEncryptionException, CleaEncodingException {
        /* Decode the encoded LSP */
        LocationSpecificPartDecoder decoder = new LocationSpecificPartDecoder(serverAuthoritySecretKey);
        LocationSpecificPart lsp = decoder.decrypt(lspbase64);
        System.out.println("EncryptedLocationContactMessage= "+Arrays.toString(lsp.getEncryptedLocationContactMessage()));

        byte[] encryptedLocationContactMessage = lsp.getEncryptedLocationContactMessage();
        LocationContact decodedLocationContact = new LocationContactMessageEncoder(
                manualContactTracingAuthoritySecretKey).decode(encryptedLocationContactMessage);
        System.out.println("decodedLocationContact= "+decodedLocationContact);

        assertThat(decodedLocationContact.getLocationPhone()).isEqualTo(locationPhone);
        assertThat(decodedLocationContact.getLocationRegion()).isEqualTo(locationRegion);
        assertThat(decodedLocationContact.getLocationPin()).isEqualTo(locationPin);
        /* Be careful the int PeriodStartTime is unsigned */
        System.out.println("PeriodStartTime = " + Integer.toUnsignedString(decodedLocationContact.getPeriodStartTime()));
        int tstPeriodStartTime = Integer.compareUnsigned(decodedLocationContact.getPeriodStartTime(), (int) t_periodStart);
        assertThat(tstPeriodStartTime).isEqualTo(0);
    }

    @Test
    public void testLocationSpecificPartBase64EciesDecryption() throws NoSuchAlgorithmException,
            InvalidKeySpecException, IllegalStateException, InvalidCipherTextException, IOException {
        /* EC private key from C package */
        final String privateKey = "3108f08b1485adb6f72cfba1b55c7484c906a2a3a0a027c78dcd991ca64c97bd";
        /* message encrypted, from C package */
        final String cipherTextBase64 = "AHHp6U8wrVQuWDomdZfDS0BHC45n72pzlmAhqE7AZp3hTWt2cuUOJ78nNeZSJCrpjpl3glMI49yjLEoIi73wqsSbja1sMH0XzuNoAssCV53wTItE3Nxg+J3FI78/W6uWD8IU+dn0YEroJwH2y1g=";
        /* plain text message byte array */
        byte[] plainTextBytes = { (byte) 0x00, (byte) 0x71, (byte) 0xE9, (byte) 0xE9, (byte) 0x4F, (byte) 0x30,
                (byte) 0xAD, (byte) 0x54, (byte) 0x2E, (byte) 0x58, (byte) 0x3A, (byte) 0x26, (byte) 0x75, (byte) 0x97,
                (byte) 0xC3, (byte) 0x4B, (byte) 0x40, (byte) 0x89, (byte) 0x43, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF,
                (byte) 0x10, (byte) 0x36, (byte) 0x50, (byte) 0xE3, (byte) 0xFB, (byte) 0xC4, (byte) 0x2F, (byte) 0x13,
                (byte) 0xAD, (byte) 0x1A, (byte) 0x0B, (byte) 0x2C, (byte) 0x7B, (byte) 0xD2, (byte) 0xAD, (byte) 0xD1,
                (byte) 0xC6, (byte) 0xCB, (byte) 0x4E, (byte) 0xDF, (byte) 0x03, (byte) 0x92, (byte) 0x76, (byte) 0x0A,
                (byte) 0xA7, (byte) 0xCB, (byte) 0xFE, (byte) 0xE8, (byte) 0x09, (byte) 0x0B, (byte) 0x97, (byte) 0x08,
                (byte) 0x00, (byte) 0x19, (byte) 0x96, (byte) 0xEA, (byte) 0xEB, (byte) 0x4B, (byte) 0xAF };
        /* String -> bytes array */
        byte[] cipherText = Base64.getDecoder().decode(cipherTextBase64);
        System.out.println("CIFFER LSP " + BytesUtils.bytesToString(cipherText));

        /* Java decrypt the message using the EC private key privKey */
        byte[] message = new CleaEciesEncoder().decrypt(cipherText, privateKey, true);

        assertThat(message).isEqualTo(plainTextBytes);
    }

    /**
     * Generates a random long with n digits
     */
    private String generateRandomDigits(int n) {
        String randomNumString = "";
        Random r = new Random();
        /* Generate the first digit from 0-9 */
        randomNumString += r.nextInt(10);
        /* Generate the remaining digits between 0-9 */
        for (int x = 1; x < n; x++) {
            randomNumString += r.nextInt(9);
        }
    
        return randomNumString;
    }
}
