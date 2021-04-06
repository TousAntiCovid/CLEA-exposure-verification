const assert = require('chai').assert;
const csv=require('csvtojson')
const { spawn } = require('child_process');
// setup : load cvs file
let cryptoList;
csv({noheader: true,
            headers:['sk_l','pk_mcta','pk_sa','result','staff','CRIexp','venueType','venueCategory1','venueCategory2','countryCode','periodDuration','browser']})
    .fromFile('./crypto.csv')
    .then((jsonObj)=> {
        cryptoList = jsonObj;
    })


setTimeout(function() {

    describe('test suite for crypto', function () {

            cryptoList.forEach(function (cryptoItem) {
                it('test on [' + cryptoItem.browser + '] with ' + cryptoItem.staff + ' ' + cryptoItem.CRIexp + ' ' + cryptoItem.venueType + ' ' + cryptoItem.venueCategory1
                    + ' ' + cryptoItem.venueCategory2 + ' ' + cryptoItem.countryCode + ' ' + cryptoItem.periodDuration , async () => {
                    await new Promise((resolve) => {
                        let result = '';

                        let javadir = process.cwd();
                        const javaproc = spawn('java', ['-cp', javadir+'/clea-crypto.jar',
                            'fr.inria.clea.lsp.LspEncoderDecoder', 'decode',
                            cryptoItem.result,
                            cryptoItem.sk_sa,
                            cryptoItem.sk_mcta]);

                        javaproc.stdout.on('data', (data) => {
                            console.log(data.toString());
                            let str = data.toString().replace(/(\r\n|\n|\r)/gm, "").trim();
                            if (str && str.length > 0 ) {
                                result = str.split(' ');
                            }
                        });

                        javaproc.stderr.on('data', (data) => {
                            console.error(data.toString());
                        });

                        javaproc.on('exit', (code) => {
                            console.log(`Child exited with code ${code}`);
                            assert.equal(0, code);
                            assert.equal(result[0], cryptoItem.staff);
                            assert.equal(result[1], cryptoItem.countryCode);
                            assert.equal(result[2], cryptoItem.CRIexp);
                            assert.equal(result[3], cryptoItem.venueType);
                            assert.equal(result[4], cryptoItem.venueCategory1);
                            assert.equal(result[5], cryptoItem.venueCategory2);
                            assert.equal(result[6], cryptoItem.periodDuration);
                            resolve();
                        });
                    })
                })
            })


        });
run();
}, 300);