/* 
* Copyright (C) Inria, 2021
*/
package fr.inria.clea.lsp;

import java.util.Calendar;
import java.util.Base64;

import org.apache.commons.net.ntp.TimeStamp;

import fr.devnied.bitlib.BitUtils;
import fr.devnied.bitlib.BytesUtils;

/**
 * locationSpecificPart (LSP) contents encoder respecting the CLEA protocol
 * 
 * @see <a href="https://hal.inria.fr/hal-03146022">CLEA protocol</a>
 *
 */
public class Encode extends Data {

    /* locationSpecificPart (LSP) in binary format */
    private byte[] LSP;
    /* locationSpecificPart (LSP) in base64 format */
    private String LSP64;
    /* ECIES crytography */
    private Ecies ecies;
    /* Permanent location secret key */
    String SK_L;
    /* EC public key in String format used to encrypt the message part */
    String PK_SA;
    /* EC public key in String format used to encrypt the locContactMsg */
    String PK_MCTA;
    /* Constant to convert hour in milli-second */
    final int ONE_HOUR_IN_S = 3600;
    /* Display or not on console intermediate results */
    private boolean debug;

    /**
     * Constructor
     * 
     * @param SK_L    Permanent location secret key
     * @param PK_SA   EC public key in String format used to decrypt the message
     *                part
     * @param PK_MCTA EC public key in String format used to decrypt the
     *                locContactMsg
     * @param debug   Display or not intermediate results for debug purpose
     */
    public Encode(String SK_L, String PK_SA, String PK_MCTA, boolean debug) throws Exception {

        this.SK_L = SK_L;
        this.PK_SA = PK_SA;
        this.PK_MCTA = PK_MCTA;
        this.ecies = new Ecies(debug);
        this.debug = debug;
    }

    /**
     * Encode the data header in binary format: | version | LSPtype | pad | LTId |
     * 
     * @return data header in binary format
     */
    private byte[] encodeHeader() {

        BitUtils header = new BitUtils(8 * Ecies.HEADER_BYTES_SIZE);
        /* version (3 bits) */
        header.setNextInteger(this.version, 3);
        /* LSPtype (3 bits) */
        header.setNextInteger(this.LSPtype, 3);
        /* padding (2 bits) */
        header.setNextInteger(0x0, 2);
        /* LTId (16 bytes) */
        byte[] uuidB = new byte[16];
        uuidB = this.ecies.UuidasBytes(this.LTId);
        header.setNextByte(uuidB, 128);

        return header.getData();
    }

    /**
     * Encode the data locContactMsg in binary format: | locationPhone | locationPin
     * | t_periodStart |
     * 
     * @return data message in binary format
     */
    private byte[] encodelocContactMsg() {

        BitUtils loc = new BitUtils(8 * Ecies.LOC_BYTES_SIZE);
        int digit, i, iend;

        /* locationPhone: 8 bytes with 4-bit nibble by digit (0xf when empty) */
        for (i = 0; i < this.locationPhone.length(); i++) {
            /* convert the char in its value */
            digit = this.locationPhone.charAt(i) - 48;
            loc.setNextInteger(digit, 4);
        }
        /* 0xf for the remaining 4-bit nibbles up to sixteen */
        iend = i;
        for (i = iend; i < 16; i++) {
            loc.setNextInteger(0x0f, 4);
        }

        /* locationPIN: 4 bytes with 4-bit nibble by digit */
        for (i = 0; i < this.locationPIN.length(); i++) {
            /* convert the char in its value */
            digit = this.locationPIN.charAt(i) - 48;
            loc.setNextInteger(digit, 4);
        }

        /* t_periodStart (32 bits) */
        loc.setNextInteger(this.t_periodStart, 32);

        return loc.getData();
    }

    /**
     * Encode the data message in binary format: | Staff | pad2 |CRIexp | vType |
     * vCat1 | vCat2 | countryCode | | periodDuration | ct_periodStart | t_qrStart |
     * LTKey |
     * 
     * @return data message in binary format
     * @throws Exception
     */
    private byte[] encodeMsg() throws Exception {

        BitUtils msg = new BitUtils(8 * Ecies.MSG_BYTES_SIZE);

        /* staff (1 bit) */
        msg.setNextInteger(this.staff, 1);
        /* locContactMsgPresent (1 bit) */
        msg.setNextInteger(this.locContactMsgPresent, 1);
        /* countryCode (12 bits) */
        msg.setNextInteger(this.countryCode, 12);
        /* CRIexp (5 bits) */
        msg.setNextInteger(this.CRIexp, 5);
        /* venueType (5 bits) */
        msg.setNextInteger(this.venueType, 5);
        /* venueCategory1 (4 bits) */
        msg.setNextInteger(this.venueCategory1, 4);
        /* venueCategory2 (4 bits) */
        msg.setNextInteger(this.venueCategory2, 4);
        /* periodDuration (1 byte) */
        msg.setNextInteger(this.periodDuration, 8);
        /* ct_periodStart (24 bits) */
        msg.setNextInteger(this.ct_periodStart, 24);
        /* t_qrStart (32 bits) */
        msg.setNextInteger(this.t_qrStart, 32);
        /* LTKey (32 bytes) */
        msg.setNextByte(this.LTKey, 256);
        /* Encode the locContactMsg with encryption if required */
        if (this.locContactMsgPresent == 1) {
            /* Encode the locContactMsg with encryption */
            byte[] loc = this.encodelocContactMsg();
            /* Encrypt locContactMsg */
            byte[] locCrypted = this.encrypt(null, loc, this.PK_MCTA);
            /* Add the encrypted locContactMsg msg */
            byte[] Msg = ecies.concat(msg.getData(), locCrypted);
            return Msg;
        } else {
            return msg.getData();
        }
    }

    /**
     * Encrypt, respecting CLEA protocol: | header | msg |
     * 
     * @param header associated data
     * @param msg    message to encrypt
     * @param Pubkey EC public key
     * 
     * @return data encrypted in binary format (bytes array)
     */
    private byte[] encrypt(byte[] header, byte[] msg, String Pubkey) throws Exception {

        byte[] encrypted_msg = this.ecies.encrypt(header, msg, Pubkey);
        if (this.debug == true)
            System.out.println("DBG_enc::MSG " + msg.length);
        if (this.debug == true)
            System.out.println("DBG_enc::ENCRYPT " + encrypted_msg.length);

        return encrypted_msg;
    }

    /**
     * Get the NTP/UTC format time in seconds
     * 
     * @param true/false hour rounded (multiple of 3600 sec) or not
     * @return NTP/UTC format time in seconds
     */
    private int getNtpUtc(boolean round) {
        
        /* Convert calendar time in NTP Timestamp */
        Calendar cal = Calendar.getInstance();
        TimeStamp timeStamp = new TimeStamp(cal.getTime());
        int currentTime = (int) timeStamp.getSeconds();

        if (round) {
            /* Number of hours */
            int th =  Integer.divideUnsigned(currentTime, ONE_HOUR_IN_S);
            /* Number of ms since the last round hour */
            int rem = Integer.remainderUnsigned(currentTime, ONE_HOUR_IN_S);
            /*
             * Round the hour, i.e. if we are closer to the next round hour than the last
             * one, round to the next hour
             */
            if (Integer.compareUnsigned(rem, ONE_HOUR_IN_S / 2) == 1 ){
                th++;
            }

            currentTime = th * ONE_HOUR_IN_S;
        }

        return currentTime;
    }

    /**
     * Start a new period to generate a new LSP computing LTKey (Temporary location
     * 256-bits secret key) and LTId (Temporary location public UUID)
     * 
     */
    public void startNewPeriod() throws Exception {

        this.t_periodStart = this.getNtpUtc(true);
        this.ct_periodStart = Integer.divideUnsigned(this.t_periodStart, this.ONE_HOUR_IN_S);

        /* compute_LTKey(t_periodStart) = SHA256(SK_L | t_periodStart) */
        this.LTKey = this.ecies.compute_LTKey(t_periodStart, this.SK_L);
        /* Compute LTId(t_periodStart) = HMAC-SHA-256-128(LTKey(t_periodStart), "1") */
        this.LTId = this.ecies.compute_LTId(this.LTKey); 
        if (this.debug) {
            System.out.println("DBG_enc::TIME " + Integer.toUnsignedString(this.t_periodStart)  + " " + Integer.toUnsignedString(this.ct_periodStart));
            System.out.println("DBG_enc:LTKey*" + BytesUtils.bytesToString(this.LTKey) + "*");
            System.out.println("DBG_enc::LTId " + LTId.toString());
        }
        /* Generate a new locationSpecificPart (LSP) */
        this.renewLSP();
    }

    /**
     * Generate a new locationSpecificPart (LSP)
     * 
     */
    public void renewLSP() throws Exception {

        /* Period starting time */
        this.t_qrStart = this.getNtpUtc(false);
        /* Encode binary data without 'plaintext' */
        byte[] header = this.encodeHeader();
        byte[] msg = this.encodeMsg();
        /* Encrypt data in binary and base64 format */
        this.LSP = this.encrypt(header, msg, this.PK_SA);
        this.LSP64 = Base64.getEncoder().encodeToString(this.LSP);
        if (this.debug) {
            System.out.println("DBG_enc::TIME " + Integer.toUnsignedString(this.t_qrStart) + " " + Integer.toUnsignedString(this.ct_periodStart));
            this.Display("DBG_enc::Header", header);
            this.Display("DBG_enc::Msg", msg);
            this.Display("DBG_enc:Final Binary Qrcode", this.LSP);
            System.out.println("DBG_enc::Final Base64 Qrcode"); 
            System.out.println(this.LSP64);
            System.out.println("DBG_enc:: Size bytes= " + this.LSP.length + " base64= " + this.LSP64.length());
        }
    }

    /**
     * Get the locationSpecificPart (LSP) encrypted in base64
     * 
     * @return locationSpecificPart (LSP) encrypted in base64
     * 
     */
    public String getLSPTobase64() {
        return this.LSP64;
    }

    /**
     * Get the locationSpecificPart (LSP) encrypted in binary format
     * 
     * @return locationSpecificPart (LSP) encrypted in base64
     * 
     */
    public byte[] getLSP() {
        return this.LSP;
    }

}
