/* 
 * Copyright (C) Inria, 2021
 */
import QRCode from 'qrcodejs2';
import $ from 'jquery';
import 'regenerator-runtime/runtime'
import {cleaStartNewPeriod} from './clea'

(() => {
    let verbose = true
    var qrcode = new QRCode("qrcode", {
        width: 500,
        height: 500,
        correctLevel: QRCode.CorrectLevel.M
    });

    /**
     * Generate a QR code
     *  - generate the LSP using the clea.js function: cleaStartNewPeriod
     *  - generate the Qrcode adding the prefix for France http://tac.gouv.fr/ to LSP
     * 
     */
    async function generateQrcode() {
        var conf = {
            SK_L: hexToBytes($("#sk_l").val()),
            PK_SA: hexToBytes($("#pk_sa").val()),
            PK_MCTA: hexToBytes($("#pk_mcta").val()),

            staff: $("#staff").prop("checked"),
            CRIexp: parseInt(Math.log(parseInt($("#CRI").val())) / Math.log(2)),
            venueType: parseInt($("#venueType").val()),
            venueCategory1: parseInt($("#venueCategory1").val()),
            venueCategory2: parseInt($("#venueCategory2").val()),
            countryCode: parseInt($("#countryCode").val()),
            periodDuration: parseInt($("#periodDuration").val()),
            locContactMsg: null
        };

        var phone = $("#locationPhone").val()

        if (phone) {
            conf.locContactMsg = {
                locationPhone: parseInt(phone),
                locationPin: parseInt($("#locationPin").val())
            }
        }

        console.log(conf);

        var b64 = await cleaStartNewPeriod(conf);

        qrcode.makeCode("http://tac.gouv.fr/" + b64);
    }

    /** 
     * Convert a hex string to a byte array
     *
     * @param {string} hex hexa string
     * @return {bytes array} 
     */
    function hexToBytes(hex) {
        var bytes = new Uint8Array(Math.ceil(hex.length / 2));
        for (var i = 0, c = 0; c < hex.length; i++, c += 2)
            bytes[i] = parseInt(hex.substr(c, 2), 16);
        return bytes;
    }

    // Generate a Qr code when the page is loaded
    generateQrcode();
    // renew the Qrcode every 10 secondes
    setInterval(generateQrcode, 10000);
})();