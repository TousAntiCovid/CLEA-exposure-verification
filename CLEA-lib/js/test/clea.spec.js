import * as clea from '../src/js/clea.js'
import {DATA_SET} from './dataset.js';

function hexToBytes(hex) {
    let bytes = new Uint8Array(Math.ceil(hex.length / 2));
    for (let i = 0, c = 0; c < hex.length; i++, c += 2)
        bytes[i] = parseInt(hex.substr(c, 2), 16);
    return bytes;
}
function logEncodingDataAndResult(conf, result) {
    let message = conf.SK_L_HEX+","
            +conf.SK_MCTA_HEX+","
            +conf.SK_SA_HEX+","
            +result.substring(clea.COUNTRY_SPECIFIC_PREFIX.length)+","
            +conf.staff+","
            +conf.CRIexp+","
            +conf.venueType+","
            +conf.venueCategory1+","
            +conf.venueCategory2+","
            +conf.periodDuration;
    message += ",\"" +navigator.userAgent+"\"";
    if (typeof conf.locContactMsg !== 'undefined') {
        message += ","+conf.locContactMsg.locationPhone+
            ","+conf.locContactMsg.locationRegion+","+conf.locContactMsg.locationPin;
    }
    console.log(JSON.stringify({filter_key: 'crypto-filter', message: message}));
}
function configurationFromRun(run) {
    let conf =  Object.assign({}, run); // clone run
    conf.SK_L_HEX = conf.SK_L;
    conf.PK_SA_HEX = conf.PK_SA;
    conf.SK_SA_HEX = conf.SK_SA;
    conf.PK_MCTA_HEX = conf.PK_MCTA;
    conf.SK_MCTA_HEX = conf.SK_MCTA;
    conf.SK_L = hexToBytes(conf.SK_L_HEX);        
    conf.PK_SA = hexToBytes(conf.PK_SA_HEX);
    conf.PK_MCTA = hexToBytes(conf.PK_MCTA_HEX);

    if ((typeof conf.locationPhone !== 'undefined') && (typeof conf.locationRegion !== 'undefined') 
            && (typeof conf.locationPIN !== 'undefined')) {
        conf.locContactMsg = {
                    locationPhone: conf.locationPhone,
                    locationRegion: conf.locationRegion,
                    locationPin: conf.locationPIN
                }
    }
    return conf;
}

let runs = DATA_SET;

describe('concatBuffer()', function () {
    it('should concat correctly', function () {
        let a = new Uint8Array(2);
        let b = new Uint8Array(3);
        let c = new Uint8Array(5);
        a[0] = 1;
        a[1] = 2;
        b[0] = 3;
        b[1] = 4;
        c[0] = 1;
        c[1] = 2;
        c[2] = 3;
        c[3] = 4;

        let result = clea.concatBuffer(a.buffer, b.buffer);

        expect(result.byteLength).to.be.equal(c.buffer.byteLength);
        let resultInt8Array = new Int8Array(result);
        for (let i = 0; i != result.byteLength; i++) {
            expect(resultInt8Array[i]).to.be.equal(c[i]);
        }
    });
});


describe('getNtpUtc()', function () {
    it('getNtpUtc(true) should return equivalent result', function () {
        let ntpUtc1 = clea.getNtpUtc(true);
        let ntpUtc2 = clea.getNtpUtc(true);
        let ntpUtc3 = clea.getNtpUtc(true);
        expect(ntpUtc3).to.be.eq(ntpUtc1);
        expect(ntpUtc2).to.be.eq(ntpUtc1);
        expect(ntpUtc3).to.be.eq(ntpUtc2);
    });

    it('getNtpUtc(false) should return equivalent result', function () {
        let ntpUtc1 = clea.getNtpUtc(false);
        let ntpUtc2 = clea.getNtpUtc(false);
        let ntpUtc3 = clea.getNtpUtc(false);
        expect(ntpUtc3).to.be.eq(ntpUtc1);
        expect(ntpUtc2).to.be.eq(ntpUtc1);
        expect(ntpUtc3).to.be.eq(ntpUtc2);
    });
});

describe('renewLocationSpecificPart()', function () {
    describe('test suite for renewLocationSpecificPart()', function () {
        it('should return something with the right length and the right header', async () => {
            let conf = configurationFromRun(runs[0]);
            let location = await clea.newLocation(conf.SK_L, conf.PK_SA, conf.PK_MCTA);
            let periodStartTime = clea.getNtpUtc(true);
            let qrCodeValidityStartTime = clea.getNtpUtc(false);
            let locationSpecificPart = await clea.newLocationSpecificPart(location, conf.venueType, conf.venueCategory1, conf.venueCategory2, conf.periodDuration, periodStartTime, conf.CRIexp, qrCodeValidityStartTime);
            qrCodeValidityStartTime = clea.getNtpUtc(false);
            await clea.renewLocationSpecificPart(locationSpecificPart, qrCodeValidityStartTime);
            let result = await clea.newDeepLink(location, locationSpecificPart, conf.staff);

            logEncodingDataAndResult(conf, result);
            expect([147, 234]).to.include(result.length - clea.COUNTRY_SPECIFIC_PREFIX.length);
        });
    });
});

describe('newDeepLink()', function () {
    describe('test suite for newDeepLink()', function () {
        runs.forEach(function (run) {
            it('should return a result with 147 length', async () => {
                let conf = configurationFromRun(run);
                let location = await clea.newLocation(conf.SK_L, conf.PK_SA, conf.PK_MCTA);

                let periodStartTime = clea.getNtpUtc(true);
                let qrCodeValidityStartTime = clea.getNtpUtc(false);
                let locationSpecificPart = await clea.newLocationSpecificPart(location, conf.venueType, conf.venueCategory1, conf.venueCategory2, conf.periodDuration, periodStartTime, conf.CRIexp, qrCodeValidityStartTime);
                let result = await clea.newDeepLink(location, locationSpecificPart, conf.staff);

                logEncodingDataAndResult(conf, result);
                expect([147, 234]).to.include(result.length - clea.COUNTRY_SPECIFIC_PREFIX.length);
            });
        });
    });
});

describe('getInt64Bytes()', function () {
    it('should return the correct result for 56', async () => {
        let expected = new Uint8Array(8);
        expected[7] = 56;

        let result = clea.getInt64Bytes(56);

        expect(result.length).to.be.eq(expected.length);
        for (let i = 0; i != result.length; i++) {
            expect(result[i]).to.be.equal(expected[i]);
        }
    });

    it('should return the correct result for 331', function () {
        let expected = new Uint8Array(8);
        expected[6] = 1;
        expected[7] = 75;

        let result = clea.getInt64Bytes(331);

        expect(result.length).to.be.eq(expected.length);
        for (let i = 0; i != result.length; i++) {
            expect(result[i]).to.be.equal(expected[i]);
        }
    });

});

describe('parseBcd()', function () {
    it('should return the correct result for parseBcd(\'0667089908\',8)', function () {
        let expected = new Uint8Array(8);
        expected[0] = 6;
        expected[1] = 103;
        expected[2] = 8;
        expected[3] = 153;
        expected[4] = 8;
        expected[5] = 255;
        expected[6] = 255;
        expected[7] = 255;

        let result = clea.parseBcd('0667089908', 8);

        expect(result.length).to.be.eq(expected.length);
        for (let i = 0; i != result.length; i++) {
            expect(result[i]).to.be.equal(expected[i]);
        }
    });
});

describe('ecdhRawPubKeyCompressed()', function () {
    it('should return the correct result', function () {
        let c = new Uint8Array(5);
        c[0] = 4;
        c[1] = 9;
        c[2] = 11;
        c[3] = 7;

        let result = clea.ecdhRawPubKeyCompressed(c);

        let expected = new Uint8Array(3);
        expected[0] = 2;
        expected[1] = 9;
        expected[2] = 11;
        expect(result.byteLength).to.be.equal(expected.buffer.byteLength);
        let resultInt8Array = new Int8Array(result);
        for (let i = 0; i != result.byteLength; i++) {
            expect(resultInt8Array[i]).to.be.equal(expected[i]);
        }
    });
});

describe('encrypt()', function () {
    it('should return something with the right length and the right header', async () => {
        let header = new Uint8Array(5);
        header[0] = 4;
        header[1] = 9;
        header[2] = 11;
        header[3] = 7;
        let message = new Uint8Array(8);
        message[0] = 5;
        message[1] = 4;
        message[2] = 10;
        message[3] = 8;

        let result = await clea.encrypt(header, message, hexToBytes('04c14d9db89a3dd8da8a366cf26cd67f1de468fb5dc15f240b0d2b96dbdb5f39af962cb0bdc0bafcc9e523bf5cd4eba420c51758f987457954d32f1003bbaaf1c5'));

        expect(result.byteLength).to.be.equal(62);
        let resultInt8Array = new Int8Array(result);
        for (let i = 0; i != header.length; i++) {
            expect(resultInt8Array[i]).to.be.equal(header[i]);
        }
    });
});
