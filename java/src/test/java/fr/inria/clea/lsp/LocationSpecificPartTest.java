/* 
* Copyright (C) Inria, 2021
*/
package fr.inria.clea.lsp;

import java.io.File;
import java.io.FileOutputStream;
import java.util.Arrays;
import java.util.Base64;

import org.bouncycastle.util.encoders.Hex;
import org.junit.jupiter.api.Test;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;

import fr.devnied.bitlib.BytesUtils;

/**
 * Some locationSpecificPart (LSP) CLEA tests
 * 
 * @see <a href="https://hal.inria.fr/hal-03146022">CLEA protocol</a>
 */
class LocationSpecificPartTest {

    /* Exemple of a SK_L used for the tests */
    final String SK_L = "23c9b8f36ac1c0cddaf869c3733b771c3dc409416a9695df40397cea53e7f39e21f76925fc0c74ca6ee7c7eafad92473fd85758bab8f45fe01aac504";

    /**
     * test1 - encryption/decryption of 'agnostic' data to test Ecies
     * 
     * @param debug Display or not intermediate results for debug purpose
     */
    @Test
    public void test1() throws Exception {

        /* Testing ECIES */
        Ecies ecies = new Ecies(true);
        String[] keyspair = ecies.genKeysPair(true);
        System.out.println("Private Key: " + keyspair[0]);
        System.out.println("Public Key : " + keyspair[1]);

        /* Message to encrypt and decrypt */
        String plaintext = "9F7213093CEDBBE66356550296A37DD18077E8646185EA2EA0EAFE88630F8C861A2E05F35BB2D863A28841CF";
        String headertext = "7D1BBFB6CAD6C2E862A7AEAD7DA27FB814";
        byte[] message = Hex.decode(plaintext);
        byte[] header = Hex.decode(headertext);
        byte[] out1 = ecies.encrypt(header, message, keyspair[1]);
        /* Display */
        System.out.println("***" + BytesUtils.bytesToString(header));
        System.out.println("PLAINTEXT : " + plaintext);
        System.out.println("CODEDTEXT : " + BytesUtils.bytesToString(out1));

        /* Decrypt and test the result */
        byte[] out2 = ecies.decrypt(out1, keyspair[0], true);

        if (!Arrays.equals(out2, ecies.concat(header, message))) {
            System.out.println("TEST ENCRYPT/DECRYPT FAILED: \nOriginal:\n"
                    + BytesUtils.bytesToString(ecies.concat(header, message)));
            System.out.println("\nResult:\n" + BytesUtils.bytesToString(out2));
        } else {
            System.out.println("TEST ENCRYPT/DECRYPT OK");
        }
    }

    /**
     * test2 - Example of encoding a locationSpecificPart (LSP) with parameters
     * 
     * @param debug Display or not intermediate results for debug purpose
     */
    @Test
    public void test2() throws Exception {

        /* Generate 2 keys pairs */
        Ecies ecies = new Ecies(true);
        String[] keyspair = ecies.genKeysPair(true);
        String[] keyspair1 = ecies.genKeysPair(true);

        Encode lsp = new Encode(SK_L, keyspair[1], keyspair1[1], true);

        lsp.setParam(0, 592, 10, 15, 0, 2, 3, "33800130000", "01234567");
        lsp.startNewPeriod();
        System.out.println("LSP " + BytesUtils.bytesToString(lsp.getLSP()));
    }

    /**
     * test3 - Example of Encoding/Decoding a locationSpecificPart (LSP)
     * 
     * @param debug Display or not intermediate results for debug purpose
     */
    @Test
    public void test3() throws Exception {

        /* Generate 2 keys pairs */
        Ecies ecies = new Ecies(true);
        String[] keyspair = ecies.genKeysPair(true);
        String[] keyspair1 = ecies.genKeysPair(true);

        /* Encoder and Decoder */
        Decode lspOut = new Decode(keyspair[0], keyspair1[0], true);
        Encode lspIn = new Encode(SK_L, keyspair[1], keyspair1[1], true);

        /* Encode a LSP with location */
        System.out.println("---- Encode LSP (with loc)");
        lspIn.setParam(1, 33, 2, 4, 0, 0, 3, "0612150292", "01234567");
        lspIn.startNewPeriod();
        final String LSP64 = lspIn.getLSPTobase64();
        lspIn.displayData("Data input");
        /* Decode the encoded LSP */
        System.out.println("---- Decode LSP (with loc)");
        lspOut.getLSP(LSP64);
        lspOut.displayData("Data output");

        /* Encode a LSP without location */
        System.out.println("---- Encode LSP (without loc)");
        lspIn.setParam(1, 33, 2, 4, 0, 0, 3);
        lspIn.startNewPeriod();
        final String LSP64_2 = lspIn.getLSPTobase64();
        lspIn.displayData("Data input");
        /* Decode the encoded LSP */
        System.out.println("---- Decode LSP (without loc)");
        lspOut.getLSP(LSP64_2);
        lspOut.displayData("Data output");
    }

    /**
     * test4 - Testing Ecies decrypting a message crypted by the C lib
     * 
     * @param debug Display or not intermediate results for debug purpose
     */
    @Test
    public void test4() throws Exception {

        /* EC private key from C package */
        final String privKey = "7422c9883c3f6c5ac70c0a08a24b5d524f36edefa04f599e316fa23ef74a4a0f";
        /* message encrypted, from C package */
        final String cipherText_S = "2f1376e97378d2e19a5d3b15cf4ff1802f971e4dc357f727098b5e0ba114318f3cdc0af35728c2ccf24641f879110fbc63f7dc9c002247d9073fc46f4e6bb2a85ae19c8e2db9de9071b1887bfe0388c333e8b8d89271f70406a86bfab0d44fc54641f1dda61292a5fe32ce128eb4";
        /* message in plain text, from C package */
        final String plainText_S = "2f1376e97378d2e19a5d3b15cf4ff1802ffd9be701f74d6f15d4a3f22fbcd296eb6bdf21ba03410a01f816b91cb2c74d709544912811e8a867dd44b8d6";

        /* String -> bytes array */
        byte[] cipherText = Hex.decode(cipherText_S);
        byte[] plainText = Hex.decode(plainText_S);

        /* Java decrypt the message using the EC private key privKey */
        Ecies ecies = new Ecies(true);
        byte[] msg = ecies.decrypt(cipherText, privKey, true);
        System.out.println("MSG " + BytesUtils.bytesToString(msg));

        /* Test if the msg decrypted == message in plain text, from C package */
        if (!Arrays.equals(plainText, msg)) {
            System.out.println("TEST ENCRYPT/DECRYPT FAILED: \nOriginal:\n" + BytesUtils.bytesToString(plainText));
            System.out.println("\nResult:\n" + BytesUtils.bytesToString(msg));
        } else {
            System.out.println("TEST ENCRYPT/DECRYPT OK");
        }
    }

    /**
     * test5 - Testing the encoding with the lsp generation and its image
     * 
     * @param debug Display or not intermediate results for debug purpose
     */
    @Test
    public void test5() throws Exception {

        int size = 200;
        /* Generate and display 2 keys pair */
        Ecies ecies = new Ecies(true);
        String[] keyspair = ecies.genKeysPair(true);
        System.out.println("Private Key : " + keyspair[0]);
        System.out.println("Public Key  : " + keyspair[1]);
        String[] keyspair1 = ecies.genKeysPair(true);
        System.out.println("Private Key1: " + keyspair[0]);
        System.out.println("Public Key1 : " + keyspair[1]);

        /* Encode the LSP */
        Encode lsp = new Encode(SK_L, keyspair[1], keyspair1[1], true);
        lsp.setParam(1, 33, 2, 4, 0, 0, 3, "0612150292", "01234567");
        lsp.startNewPeriod();

        /* Qrcode = "country-specific-prefix" / "Base64(location-specific-part)" */
        String Qrcode = "https://tac.gouv.fr/" + lsp.getLSPTobase64();

        /* encode Qrcode with default parameters, level L */
        BitMatrix bitMatrix = new QRCodeWriter().encode(Qrcode, BarcodeFormat.QR_CODE, size, size);

        /* generate an image */
        String imageFormat = "png";
        String outputFileName = "./qrcode-" + lsp.t_qrStart + "." + imageFormat;
        /* write in a file */
        FileOutputStream fileOutputStream = new FileOutputStream(new File(outputFileName));
        MatrixToImageWriter.writeToStream(bitMatrix, imageFormat, fileOutputStream);
        fileOutputStream.close();
    }

    /**
     * test6 - Testing base64 decoding from C or Java
     * 
     * @param debug Display or not intermediate results for debug purpose
     */
    @Test
    public void test6() throws Exception {

        String lsp64C = "APJexM7Ntkr9l2JO6mpD3HWO9OkU9nDygdP16KhNAiR9JUr05mT9+5kvJbZph/GdRbIqpQCwgFlYkWEr633BiYhJ+x/pc581PYG4aF2ZzjDJfrY5PfZodBKEiWH+Qegtp3x2bw4sfbCM8OPIvPtU7ooyyzj9h7RdKp4GfgeCz9YdikJ8uJYusQaFILrICqswxQzQPhLVHsnkMjAVpayCAxUOVgZbqj5m8lNcMhCxog==";
        String lsp64J = "AJMxSV4mHDX9iqjedPJRtpd7XTx53/ZCrZ4l53yFT7CSbiksSWm6vXApD+XeHT5nLEPbVRPXQoY8PJaTakCQNXYa2EUb8UW62n7sMua+UmZwDnf/9OPOVwWyGacP5L94sv0fCk7XnjBbDLhtORCGrdiwkOm3UniGc8gyP41zneHQSmbfzq6kEzFCX2kfKQIXIsFVyCFp7M4KdpVb2oWg2Q/FZr63cBdObwI9mrImCw==";
        Base64.getDecoder().decode(lsp64C);

        System.out.println("Qrcode size C=" + lsp64C.length() + " Java=" + lsp64J.length());
    }

    /**
     * test7 - Testing a LSP base 64 Java decoding
     * 
     * @param debug Display or not intermediate results for debug purpose
     */
    @Test
    public void test7() throws Exception {

        final String lsp_base64 = "AJi9dKxk4aXcRhF9lIIiGchvIbwtd2BE72nelq4/+uF0T0hE/GA0hFpEpuhVi+Xla8irZbGRmcDIfMqs0e8j/eChcYTeHo+bjWyN2GsHo+F5F46o0cM0IWuw/1MgctXYFCUw53zPL2Cs1ERN3HTpxnL9us2y//P+r8qV39YnmjFUj61Rlrosk2r81NO6BQImmg5sSV31rOTWXNrUwNQSTmXki0E+hfLgi9aMeWMnXQ==";
        final String SK_SA = "34af7f978c5a17772867d929e0b800dd2db74608322d73f2f0cfd19cdcaeccc8";
        final String SK_MCTA = "3108f08b1485adb6f72cfba1b55c7484c906a2a3a0a027c78dcd991ca64c97bd";

        Decode lspOut = new Decode(SK_SA, SK_MCTA, true);

        lspOut.getLSP(lsp_base64);
        lspOut.displayData("Lsp decoded");
    }

    /**
     * test8- Testing a LSP base 64 Java Ecies Decryption
     * 
     * @param debug Display or not intermediate results for debug purpose
     */
    @Test
    public void test8() throws Exception {

        /* EC private key from C package */
        final String privKey = "34af7f978c5a17772867d929e0b800dd2db74608322d73f2f0cfd19cdcaeccc8";
        /* message encrypted, from C package */
        final String cipherText_64 = "AA3sinpPVKOpedLxzpMbgS3G4d4Up1vDcNQ28fZKB8cBnBjNYV0bPGoaBxnFFab/iM56uzSoDQ0i+N5B9shw5bBmutRONcWQBhNr1ug/0sZ62UaiWZjqfYDmpANJHfv0Kao3DUJvPLHep7N9uNlOywOhHISXoFsCNvikOv8o3hN9j7vbtW6xjILpbQP01gNtiNqliKDGWCSo/g4xlFjlbiWo4E1bL5UiWHAS9KYj8g==";

        /* String -> bytes array */
        byte[] cipherText = Base64.getDecoder().decode(cipherText_64);
        System.out.println("CIFFER LSP " + BytesUtils.bytesToString(cipherText));

        /* Java decrypt the message using the EC private key privKey */
        Ecies ecies = new Ecies(true);
        byte[] msg = ecies.decrypt(cipherText, privKey, true);
        System.out.println("PLAIN LSP: " + BytesUtils.bytesToString(msg));
    }

}
