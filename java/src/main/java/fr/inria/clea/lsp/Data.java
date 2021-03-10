/* 
* Copyright (C) Inria, 2021
*/
package fr.inria.clea.lsp;

import java.util.UUID;
import fr.devnied.bitlib.BytesUtils;

/**
 * locationSpecificPart (LSP) contents data respecting the CLEA protocol
 * 
 * @see <a href="https://hal.inria.fr/hal-03146022">CLEA protocol</a>
 *
 */
class Data {

    /* Protocol version */
    int version;
    /*
     * LSP type, in order to be able to use multiple formats in parallel in the
     * future.
     */
    int LSPtype;
    /*
     * Location Temporary public universally unique Identifier (UUID), specific to a
     * given location at a given period.
     */
    UUID LTId;
    /* 0/1 regular users or staff member of the location */
    int staff;
    /* 0/1 indicates the locContactMsg is absent/present in the Msg */
    int locContactMsgPresent;
    /*
     * Country code, coded as the ISO 3166-1 country code, for instance 0x250 for
     * France
     */
    int countryCode;
    /*
     * qrCodeRenewalInterval value in a compact manner, as the exponent of a power
     * of two.
     */
    int CRIexp;
    /* Type of the location/venue */
    int venueType;
    /* Reserved: a first level of venue category */
    int venueCategory1;
    /* Reserved: a second level of venue category */
    int venueCategory2;
    /* Duration, in terms of number of hours, of the period */
    int periodDuration;
    /* Starting time of the period in a compressed manner (round hour) */
    int ct_periodStart;
    /* Starting time of the QR code validity timespan in seconds */
    int t_qrStart;
    /* Temporary location key for the period */
    byte[] LTKey = new byte[32];
    /* Phone number of the location contact person, one digit = one character */
    String locationPhone;
    /* Secret 8 digit PIN, one digit = one character */
    String locationPIN;
    /* Starting time of the period in seconds */
    int t_periodStart;

    /**
     * Set LSP parameters
     * 
     * @param staff          [0-1] regular users or staff member of the location
     * @param countryCode    [0-0x0f/15] Country code, for instance 0x250 for France
     * @param CRIexp         [0-0x1f/31] qrCodeRenewalInterval value in a compact
     *                       manner, as the exponent of a power of two.
     * @param venueType      [0-0x1f/31] Type of the location/venue
     * @param venueCategory1 [0-0x0f/15] Reserved: a first level of venue category
     * @param venueCategory2 [0-0x0f/15] Reserved: a first level of venue category
     * @param periodDuration [0-0xff/255] Duration, in terms of number of hours, of
     *                       the period
     * @param locationPhone  [String of 16 digit max] Phone number of the location
     *                       contact perso
     * @param locationPIN    [String of 8 digit] Secret 8 digit PIN
     * 
     */
    public void setParam(int staff, int countryCode, int CRIexp, int venueType, int venueCategory1, int venueCategory2,
            int periodDuration, String locationPhone, String locationPIN) {
        this.version = 0x0;
        this.LSPtype = 0x0;
        this.staff = staff;
        this.locContactMsgPresent = 0x1;
        this.countryCode = countryCode;
        this.CRIexp = CRIexp;
        this.venueType = venueType;
        this.venueCategory1 = venueCategory1;
        this.venueCategory2 = venueCategory2;
        this.periodDuration = periodDuration;
        this.locationPhone = locationPhone;
        this.locationPIN = locationPIN;
    }

    /**
     * Set LSP parameters
     * 
     * @param staff          [0-1] regular users or staff member of the location
     * @param countryCode    [0-0x0f/15] Country code, for instance 0x250 for France
     * @param CRIexp         [0-0x1f/31] qrCodeRenewalInterval value in a compact
     *                       manner, as the exponent of a power of two.
     * @param venueType      [0-0x1f/31] Type of the location/venue
     * @param venueCategory1 [0-0x0f/15] Reserved: a first level of venue category
     * @param venueCategory2 [0-0x0f/15] Reserved: a first level of venue category
     * @param periodDuration [0-0xff/255] Duration, in terms of number of hours, of
     *                       the period
     * 
     */

    public void setParam(int staff, int countryCode, int CRIexp, int venueType, int venueCategory1, int venueCategory2,
            int periodDuration) {
        this.version = 0x0;
        this.LSPtype = 0x0;
        this.staff = staff;
        this.locContactMsgPresent = 0x0;
        this.countryCode = countryCode;
        this.CRIexp = CRIexp;
        this.venueType = venueType;
        this.venueCategory1 = venueCategory1;
        this.venueCategory2 = venueCategory2;
        this.periodDuration = periodDuration; 
        this.locationPhone = "";
        this.locationPIN = "";
    }

    /**
     * Display bytes array on console for debug purpose
     * 
     * @param label header on display
     * @param data  byte array to be display with a sequence of bytes
     */
    public void Display(String label, byte[] data) {
        System.out.println(label);
        System.out.println("-----------------");
        System.out.println(BytesUtils.bytesToString(data));
        System.out.println("-----------------");
    }

    /**
     * Display LSP data parameters for debug purpose
     * 
     * @param label header on display
     */
    public void displayData(String label) {
        System.out.println(label);
        System.out.println("-----------------");
        System.out.println("version:" + this.version + " LSPType:" + this.LSPtype + " LTId:" + this.LTId.toString());
        System.out.print("staff:" + this.staff + " C:" + this.locContactMsgPresent + " countryCode:" + this.countryCode
                + " CRIexp:" + this.CRIexp);
        System.out.println(" venueType:" + this.venueType + " venueCategory1:" + this.venueCategory1
                + " venueCategory2:" + this.venueCategory2);
        System.out.println("periodDuration:" + this.periodDuration + " t_qrStart:" + this.t_qrStart + " ct_periodStart:"
                + this.ct_periodStart);
        System.out.println("LTKey:" + BytesUtils.bytesToString(this.LTKey));
        if (this.locContactMsgPresent == 1) {
            System.out.println("locationPhone:" + this.locationPhone + " locationPIN:" + this.locationPIN
                    + " t_periodStart:" + this.t_periodStart);
        }
        System.out.println("-----------------");
    }
}
