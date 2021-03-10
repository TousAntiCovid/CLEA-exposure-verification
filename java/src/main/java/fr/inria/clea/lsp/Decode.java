/* 
* Copyright (C) Inria, 2021
*/
package fr.inria.clea.lsp;

import java.util.Arrays;
import java.util.Base64;

import fr.devnied.bitlib.BitUtils;

/**
 * locationSpecificPart (LSP) contents reader respecting the CLEA protocol
 * 
 * @see <a href="https://hal.inria.fr/hal-03146022">CLEA protocol</a>
 *
 */
public class Decode extends Data {

    /* locationSpecificPart (LSP) in binary format */
    private byte[] LSP;
    /* locationSpecificPart (LSP) in binary format decrypted */
    private byte[] uncrypted;
    /* ECIES cryptography */
    private Ecies ecies;
    /* EC private key in String format used to decrypt the message part */
    String SK_SA;
    /* EC private key in String format used to decrypt the locContactMsg */
    String SK_MCTA;
    /* Display or not on console intermediate results */
    boolean debug;

    /**
     * Constructor
     * 
     * @param SK_SA EC Private key in string format required to decrypt the LSP
     * @param debug Display or not intermediate results for debug purpose
     */
    public Decode(String SK_SA, String SK_MCTA, boolean debug) throws Exception {
        this.ecies = new Ecies(debug);
        this.SK_SA = SK_SA;
        this.SK_MCTA = SK_MCTA;
        this.debug = debug;
    }

    /**
     * Decode locationSpecificPart (LSP) in base64 in binary format
     * 
     * @param LSP64
     */
    private void LSPInByte(String LSP64) {
        this.LSP = Base64.getDecoder().decode(LSP64);
    }

    /**
     * Unpack the data decrypted header (binary format): | version | LSPtype | pad |
     * LTId | to extract parameters
     * 
     */
    private void getHeader() {
        byte[] headerB = Arrays.copyOfRange(this.uncrypted, 0, Ecies.HEADER_BYTES_SIZE);
        BitUtils header = new BitUtils(headerB);

        /* version (3 bits) */
        this.version = header.getNextInteger(3);
        /* LSPtype (3 bits) */
        this.LSPtype = header.getNextInteger(3);
        /* padding (2 bits) */
        int pad = header.getNextInteger(2);
        assert (pad == 0) : "LSP decoding, padding error";
        /* LTId (16 bytes) */
        byte[] uuidB = new byte[16];
        uuidB = header.getNextByte(128);
        this.LTId = this.ecies.BytesasUuid(uuidB);

        if (this.debug) {
            System.out.println("DBG_read:: version " + this.version);
            System.out.println("DBG_read:: LSPtype " + this.LSPtype);
            System.out.println("DBG_read:: UUID " + this.LTId);
        }
    }

    /**
     * Unpack the data message (binary format) : | Staff | pad2 |CRIexp | vType |
     * vCat1 | vCat2 | countryCode | | periodDuration | ct_periodStart | t_qrStart |
     * LTKey | to extract parameters
     * 
     */
    private void getMsg() throws Exception {

        byte[] msgB = Arrays.copyOfRange(this.uncrypted, Ecies.HEADER_BYTES_SIZE,
                Ecies.HEADER_BYTES_SIZE + Ecies.MSG_BYTES_SIZE);
        BitUtils msg = new BitUtils(msgB);

        /* staff (1 bit) */
        this.staff = msg.getNextInteger(1); 
        /* locContactMsgPresent (1 bit) */
        this.locContactMsgPresent = msg.getNextInteger(1);
        /* countryCode (12 bits) */
        this.countryCode = msg.getNextInteger(12);
        /* CRIexp (5 bits) */
        this.CRIexp = msg.getNextInteger(5);
        /* venueType (5 bits) */
        this.venueType = msg.getNextInteger(5);
        /* venueCategory1 (4 bits) */
        this.venueCategory1 = msg.getNextInteger(4);
        /* venueCategory2 (4 bits) */
        this.venueCategory2 = msg.getNextInteger(4);
        /* periodDuration (1 byte) */
        this.periodDuration = msg.getNextInteger(8);  
        /* ct_periodStart (24 bits) */
        this.ct_periodStart = msg.getNextInteger(24);
        /* t_qrStart (32 bits) */
        this.t_qrStart = msg.getNextInteger(32);
        /* LTKey (32 bytes) */
        this.LTKey = msg.getNextByte(256);
    }

    /**
     * Unpack the data locContactMsg (binary format) : | locationPhone | locationPIN
     * | t_periodStart |
     * 
     */
    private void getLocMsg() throws Exception {

        byte[] locBCrypted = Arrays.copyOfRange(this.uncrypted,
                Ecies.HEADER_BYTES_SIZE + Ecies.MSG_BYTES_SIZE, this.uncrypted.length);

        /* Decrypt the data */
        byte[] locB = this.ecies.decrypt(locBCrypted, this.SK_MCTA , false);
        BitUtils loc = new BitUtils(locB);

        int i, digit;
        /*
         * locationPhone: 8 bytes with 4-bit nibble by digit (0xf when empty) => 16
         * digits max
         */
        this.locationPhone = "";
        for (i = 0; i < 16; i++) {
            /* unpack the 4-bit nibbles */
            digit = loc.getNextInteger(4);
            if (digit != 0xf) {
                this.locationPhone += Integer.toString(digit);
            }
        }

        /* locationPIN: 4 bytes with 4-bit nibble by digit => 8 digits */
        this.locationPIN = "";
        for (i = 0; i < 8; i++) {
            /* unpack the 4-bit nibbles */
            this.locationPIN += Integer.toString(loc.getNextInteger(4));
        }

        /* t_periodStart (32 bits) */
        this.t_periodStart = loc.getNextInteger(32);
    }

    /**
     * Decrypt and unpack a locationSpecificPart (LSP)
     * 
     * @param LSP64 LSP in base64
     * 
     */
    public void getLSP(String LSP64) throws Exception {
        LSPInByte(LSP64);
        if (this.debug)
            this.Display("DBG_read:LSP", this.LSP);
        this.uncrypted = this.ecies.decrypt(this.LSP, this.SK_SA, true);
        this.getHeader();
        this.getMsg();
        if (this.locContactMsgPresent == 1) {
            this.getLocMsg();
        }
    }

}
