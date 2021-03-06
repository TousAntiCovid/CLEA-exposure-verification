/* 
 * Copyright (C) Inria, 2021
 */
import QRCode from 'qrcodejs2';
import $ from 'jquery';
import 'regenerator-runtime/runtime'
import {cleaStartNewPeriod} from './clea'

(() => {

    let form = document.getElementById('clea');
    form.addEventListener('change', function() {
        generateQrcode();
    });

    /**
     * Generate a QR code
     *  - generate the LSP using the clea.js function: cleaStartNewPeriod
     *  - generate the Qrcode adding the prefix for France http://tac.gouv.fr?v=0# to LSP
     * 
     */
    async function generateQrcode() {
        $('#qrcode').html('');   // clear the code.
        if (isValid()) {
            let qrcode = new QRCode("qrcode", {
                width: 500,
                height: 500,
                correctLevel: QRCode.CorrectLevel.M
            });
            let conf = {
                SK_L: hexToBytes($("#sk_l").val()),
                PK_SA: hexToBytes($("#pk_sa").val()),
                PK_MCTA: hexToBytes($("#pk_mcta").val()),

                staff: $("#staff").prop("checked"),
                CRIexp: parseInt(Math.log(parseInt($("#CRI").val())) / Math.log(2)),
                venueType: parseInt($("#venueType").val()),
                venueCategory1: parseInt($("#venueCategory1").val()),
                venueCategory2: parseInt($("#venueCategory2").val()),
                periodDuration: parseInt($("#periodDuration").val()),
                locContactMsg: null
            };

            let phone = $("#locationPhone").val()

            if (phone) {
                conf.locContactMsg = {
                    locationPhone: parseInt(phone),
                    locationPin: parseInt($("#locationPin").val())
                }
            }

            let b64 = await cleaStartNewPeriod(conf);
			let newDeeplink = "https://tac.gouv.fr?v=0#" + b64;
			let deeplinks = document.getElementById('deeplinks').innerHTML;
			document.getElementById('deeplinks').innerHTML = deeplinks  + "<p><a href="+newDeeplink+">" + newDeeplink + "</a></p>";
            qrcode.makeCode(newDeeplink);
        }
    }

    /**
     * Check if the form is valid
     *
     * @return {boolean}
     */
    function isValid() {
        let sk_l = document.getElementById('sk_l');
        let pk_sa = document.getElementById('pk_sa');
        let pk_mcta = document.getElementById('pk_mcta');
        let cri = document.getElementById('CRI');
        let periodDuration = document.getElementById('periodDuration');
        let locationPhone = document.getElementById('locationPhone');
        let locationPin = document.getElementById('locationPin');

        locationPhone.setCustomValidity('');
        if (locationPhone.value) {
            let locationPhoneExtract = locationPhone.value.match(/\d/g);
            if (!locationPhoneExtract || locationPhoneExtract.length !== 10) {
                locationPhone.className = 'invalid';
                locationPhone.setCustomValidity('Location phone is invalid');
            }
            if (locationPhoneExtract) {
                if (!locationPin.value) {
                    locationPin.setCustomValidity('Secret digit PIN must contain exactly 6 characters');
                } else {
                    locationPin.setCustomValidity('');
                    locationPin.checkValidity();
                }
            }
        }
        let valid = sk_l.validity.valid && pk_sa.validity.valid && pk_mcta.validity.valid
            && cri.validity.valid && periodDuration.validity.valid && locationPhone.validity.valid && locationPin.validity.valid;
        return valid;
    }

    /**
     * Convert a hex string to a byte array
     *
     * @param {string} hex hexa string
     * @return {bytes array} 
     */
    function hexToBytes(hex) {
        let bytes = new Uint8Array(Math.ceil(hex.length / 2));
        for (let i = 0, c = 0; c < hex.length; i++, c += 2)
            bytes[i] = parseInt(hex.substr(c, 2), 16);
        return bytes;
    }

    // check the browser compatibility
    if (/msie\s|trident\/|edge\//i.test(window.navigator.userAgent)) {
        $("#unsupportedBrowser").removeClass("hidden");
        $("#clea").addClass("hidden");
    } else {
        // Generate a Qr code when the page is loaded
        generateQrcode();
        // renew the Qrcode every 10 secondes
        setInterval(generateQrcode, 10000);
    }
})();