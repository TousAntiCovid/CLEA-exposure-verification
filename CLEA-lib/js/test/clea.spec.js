import * as clea from '../src/js/clea.js'


//var concatBuffer = app.__get__('concatBuffer');

function hexToBytes(hex) {
    var bytes = new Uint8Array(Math.ceil(hex.length / 2));
    for (var i = 0, c = 0; c < hex.length; i++, c += 2)
        bytes[i] = parseInt(hex.substr(c, 2), 16);
    return bytes;
}

var runs = [
    {
        conf: {
            SK_L_HEX: '23c9b8f36ac1c0cddaf869c3733b771c3dc409416a9695df40397cea53e7f39e21f76925fc0c74ca6ee7c7eafad92473fd85758bab8f45fe01aac504',
            SK_L: hexToBytes('23c9b8f36ac1c0cddaf869c3733b771c3dc409416a9695df40397cea53e7f39e21f76925fc0c74ca6ee7c7eafad92473fd85758bab8f45fe01aac504'),
            PK_SA_HEX: '04c14d9db89a3dd8da8a366cf26cd67f1de468fb5dc15f240b0d2b96dbdb5f39af962cb0bdc0bafcc9e523bf5cd4eba420c51758f987457954d32f1003bbaaf1c5',
            PK_SA: hexToBytes('04c14d9db89a3dd8da8a366cf26cd67f1de468fb5dc15f240b0d2b96dbdb5f39af962cb0bdc0bafcc9e523bf5cd4eba420c51758f987457954d32f1003bbaaf1c5'),
            PK_MCTA_HEX: '04c14d9db89a3dd8da8a366cf26cd67f1de468fb5dc15f240b0d2b96dbdb5f39af962cb0bdc0bafcc9e523bf5cd4eba420c51758f987457954d32f1003bbaaf1c5',
            PK_MCTA: hexToBytes('04c14d9db89a3dd8da8a366cf26cd67f1de468fb5dc15f240b0d2b96dbdb5f39af962cb0bdc0bafcc9e523bf5cd4eba420c51758f987457954d32f1003bbaaf1c5'),
            staff: 0,
            CRIexp: 5,
            venueType: 12,
            venueCategory1: 0,
            venueCategory2: 0,
            countryCode: 492,
            periodDuration: 3,
            locContactMsg: null,
        }, header: 'ADjJN37ySvNMcCc4pwfYoB'
    },
    {
        conf: {
            SK_L_HEX: '23c9b8f36ac1c0cddaf869c3733b771c3dc409416a9695df40397cea53e7f39e21f76925fc0c74ca6ee7c7eafad92473fd85758bab8f45fe01aac504',
            SK_L: hexToBytes('23c9b8f36ac1c0cddaf869c3733b771c3dc409416a9695df40397cea53e7f39e21f76925fc0c74ca6ee7c7eafad92473fd85758bab8f45fe01aac504'),
            PK_SA_HEX: '04c14d9db89a3dd8da8a366cf26cd67f1de468fb5dc15f240b0d2b96dbdb5f39af962cb0bdc0bafcc9e523bf5cd4eba420c51758f987457954d32f1003bbaaf1c5',
            PK_SA: hexToBytes('04c14d9db89a3dd8da8a366cf26cd67f1de468fb5dc15f240b0d2b96dbdb5f39af962cb0bdc0bafcc9e523bf5cd4eba420c51758f987457954d32f1003bbaaf1c5'),
            PK_MCTA_HEX: '04c14d9db89a3dd8da8a366cf26cd67f1de468fb5dc15f240b0d2b96dbdb5f39af962cb0bdc0bafcc9e523bf5cd4eba420c51758f987457954d32f1003bbaaf1c5',
            PK_MCTA: hexToBytes('04c14d9db89a3dd8da8a366cf26cd67f1de468fb5dc15f240b0d2b96dbdb5f39af962cb0bdc0bafcc9e523bf5cd4eba420c51758f987457954d32f1003bbaaf1c5'),
            staff: 1,
            CRIexp: 31,
            venueType: 31,
            venueCategory1: 15,
            venueCategory2: 15,
            countryCode: 4095,
            periodDuration: 255,
            locContactMsg: null,
        }, header: 'ADjJN37ySvNMcCc4pwfYoB'
    },
    {
        conf: {
            SK_L_HEX: '23c9b8f36ac1c0cddaf869c3733b771c3dc409416a9695df40397cea53e7f39e21f76925fc0c74ca6ee7c7eafad92473fd85758bab8f45fe01aac504',
            SK_L: hexToBytes('23c9b8f36ac1c0cddaf869c3733b771c3dc409416a9695df40397cea53e7f39e21f76925fc0c74ca6ee7c7eafad92473fd85758bab8f45fe01aac504'),
            PK_SA_HEX: '045f802c016b2d14ef4d7ef01617c67c7506c0cd08aed3e4bcaf34ef5ffaddebb70a073d82c37bc874ce6705cec8b1c4a03b2ccd8f28b0c5034fb8774f2e97b1a4',
            PK_SA: hexToBytes('045f802c016b2d14ef4d7ef01617c67c7506c0cd08aed3e4bcaf34ef5ffaddebb70a073d82c37bc874ce6705cec8b1c4a03b2ccd8f28b0c5034fb8774f2e97b1a4'),
            PK_MCTA_HEX: '045f802c016b2d14ef4d7ef01617c67c7506c0cd08aed3e4bcaf34ef5ffaddebb70a073d82c37bc874ce6705cec8b1c4a03b2ccd8f28b0c5034fb8774f2e97b1a4',
            PK_MCTA: hexToBytes('045f802c016b2d14ef4d7ef01617c67c7506c0cd08aed3e4bcaf34ef5ffaddebb70a073d82c37bc874ce6705cec8b1c4a03b2ccd8f28b0c5034fb8774f2e97b1a4'),
            staff: 1,
            CRIexp: 31,
            venueType: 31,
            venueCategory1: 15,
            venueCategory2: 15,
            countryCode: 592,
            periodDuration: 255,
            locContactMsg: null,
        }, header: 'ADjJN37ySvNMcCc4pwfYoB'
    },
    {
        conf: {
            SK_L_HEX: '23c9b8f36ac1c0cddaf869c3733b771c3dc409416a9695df40397cea53e7f39e21f76925fc0c74ca6ee7c7eafad92473fd85758bab8f45fe01aac504',
            SK_L: hexToBytes('23c9b8f36ac1c0cddaf869c3733b771c3dc409416a9695df40397cea53e7f39e21f76925fc0c74ca6ee7c7eafad92473fd85758bab8f45fe01aac504'),
            PK_SA_HEX: '04c14d9db89a3dd8da8a366cf26cd67f1de468fb5dc15f240b0d2b96dbdb5f39af962cb0bdc0bafcc9e523bf5cd4eba420c51758f987457954d32f1003bbaaf1c5',
            PK_SA: hexToBytes('04c14d9db89a3dd8da8a366cf26cd67f1de468fb5dc15f240b0d2b96dbdb5f39af962cb0bdc0bafcc9e523bf5cd4eba420c51758f987457954d32f1003bbaaf1c5'),
            PK_MCTA_HEX: '045f802c016b2d14ef4d7ef01617c67c7506c0cd08aed3e4bcaf34ef5ffaddebb70a073d82c37bc874ce6705cec8b1c4a03b2ccd8f28b0c5034fb8774f2e97b1a4',
            PK_MCTA: hexToBytes('045f802c016b2d14ef4d7ef01617c67c7506c0cd08aed3e4bcaf34ef5ffaddebb70a073d82c37bc874ce6705cec8b1c4a03b2ccd8f28b0c5034fb8774f2e97b1a4'),
            staff: 1,
            CRIexp: 5,
            venueType: 31,
            venueCategory1: 3,
            venueCategory2: 1,
            countryCode: 202,
            periodDuration: 10,
            locContactMsg: null,
        }, header: 'ADjJN37ySvNMcCc4pwfYoB'
    }
];

before(function(done){
    console.log(JSON.stringify({filter_key: 'crypto-filter', message: 'sk_l,pk_mcta,pk_sa,result'}));
    done();
});

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
        for (var i = 0; i != result.byteLength; i++) {
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

describe('cleaRenewLSP()', function () {

    it('should return something with the right lenght and the right header', async () => {
        let sk_l = '23c9b8f36ac1c0cddaf869c3733b771c3dc409416a9695df40397cea53e7f39e21f76925fc0c74ca6ee7c7eafad92473fd85758bab8f45fe01aac504';
        let pk_sa = '04c14d9db89a3dd8da8a366cf26cd67f1de468fb5dc15f240b0d2b96dbdb5f39af962cb0bdc0bafcc9e523bf5cd4eba420c51758f987457954d32f1003bbaaf1c5';
        let conf = {
            SK_L: hexToBytes(sk_l),
            PK_SA: hexToBytes(pk_sa),
            //PK_MCTA: '',
            staff: 0,
            CRIexp: 5,
            venueType: 12,
            venueCategory1: 0,
            venueCategory2: 0,
            countryCode: 492,
            periodDuration: 3,
            //locContactMsg:''
        }
        let result = await clea.cleaRenewLSP(conf);
        console.log(JSON.stringify({filter_key: 'crypto-filter', message: sk_l+","+pk_sa+","+pk_sa+","+result}));
        expect(result).to.length(148)
        expect(result.startsWith('AAAAAAAAAAAAAAAAAAAAAA')).to.be.true;
    })

});

describe('cleaStartNewPeriod()', function () {
    describe('test suite for cleaStartNewPeriod()', function () {
        runs.forEach(function (run) {
            it('should return a result with 148 length', async () => {
                let result = await clea.cleaStartNewPeriod(run.conf);
                console.log(result);
                console.log(JSON.stringify({filter_key: 'crypto-filter', message: run.conf.SK_L_HEX+","+run.conf.PK_SA_HEX+","+run.conf.PK_MCTA_HEX+","+result}));
                expect(result).to.length(148);
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
        for (var i = 0; i != result.length; i++) {
            expect(result[i]).to.be.equal(expected[i]);
        }
    });

    it('should return the correct resul for 331', function () {
        let expected = new Uint8Array(8);
        expected[6] = 1;
        expected[7] = 75;
        let result = clea.getInt64Bytes(331);
        expect(result.length).to.be.eq(expected.length);
        for (var i = 0; i != result.length; i++) {
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
        for (var i = 0; i != result.length; i++) {
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
        for (var i = 0; i != result.byteLength; i++) {
            expect(resultInt8Array[i]).to.be.equal(expected[i]);
        }
    });
});

describe('encrypt()', function () {
    it('should return somethinh with the right length and the right header', async () => {
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
        for (var i = 0; i != header.length; i++) {
            expect(resultInt8Array[i]).to.be.equal(header[i]);
        }
    });
})
