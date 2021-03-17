/* 
 * Copyright (C) Inria, 2021
 */

/*
* Global variable of json output results
*/
var OUTPUT = [];


/**
 * Generate a list of Location Specific Part (LSP)
 *  
 *  output results on global OUTPUT json format `encode_out.json`
 * 
 * @param {json} conf_tests loaded from file `encode_in.json`
 */
async function generateLsps(conf_tests) {
    var i = 0;
    var vResult = "";
    for (const conf of conf_tests) {
        vResult += "Encode " + i + "\n";
        conf.SK_L = hexToBytes(conf.SK_L);
        conf.PK_SA = hexToBytes(conf.PK_SA);
        conf.PK_MCTA = hexToBytes(conf.PK_MCTA);
        if ((("locationPhone" in conf) == true) && (("locationPIN" in conf) == true)) {
            conf['locContactMsg'] = {
                locationPhone: conf.locationPhone,
                locationPin: conf.locationPIN
            }
        }
        var b64 = await cleaStartNewPeriod(conf);
        vResult += b64 + "\n";
        lsp_base_64 = {
            lsp_base64: b64,
            LTId: bytesToUuid(gConf.LTId),
            ct_periodStart: gConf.ct_periodStart,
            t_qrStart: gConf.t_qrStart,
            SK_SA: conf.SK_SA,
            SK_MCTA: conf.SK_MCTA,
        }
        OUTPUT.push(lsp_base_64);
        i++;
    }

    document.getElementById('area').value = vResult;

}

/** 
 * Convert a hex string to a byte array
 *
 * @param {string} hex hexa string
 * @return {bytes array} 
 */
function hexToBytes(hex) {
    var bytes = new Uint8Array(Math.ceil(hex.length / 2));
    for (i = 0, c = 0; c < hex.length; i++, c += 2)
        bytes[i] = parseInt(hex.substr(c, 2), 16);
    return bytes;
}

/** 
 * Convert a bytes array to a hex string
 *
 * @param {bytes array} buffer
 * @return {string} hexa string
 */
function bytesToHex(buffer) {
    var bytes = new Uint8Array(buffer);
    for (var hex = [], i = 0; i < bytes.length; i++) {
        var current = bytes[i] < 0 ? bytes[i] + 256 : bytes[i];
        hex.push((current >>> 4).toString(16));
        hex.push((current & 0xF).toString(16));
    }
    return hex.join("");
}

/** 
 * Convert a bytes array to UUID
 *
 * @param {bytes array} buffer of 32 bytes
 * @return {string} UUUID format
 */
function bytesToUuid(buffer) {

    const string = bytesToHex(buffer);
    var uuid = string.substring(0, 8) + '-' + string.substring(8, 12) + '-' + string.substring(12, 16) + '-';
    uuid += string.substring(16, 20) + '-' + string.substring(20, 32);

    return uuid;
}

/** 
 * Write the json file
 * 
 * global variable OUTPUT to be written
 * 
 */
async function write_lsps() {
    console.log("OUT", OUTPUT);
}