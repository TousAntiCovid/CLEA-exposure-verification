const assert = require('chai').assert;
const csv=require('csvtojson')
const { spawn } = require('child_process');
// setup : load cvs file
let cryptoList;
csv()
    .fromFile('./crypto.csv')
    .then((jsonObj)=> {
        cryptoList = jsonObj;
        console.log(cryptoList);
    })

setTimeout(function() {

    describe('test suite for crypto', function () {

            cryptoList.forEach(function (cryptoItem) {
                it('test ' + cryptoItem.result + 'key ' + cryptoItem.sk_l +'/' + cryptoItem.pk_sa, async () => {
                    await new Promise((resolve) => {
                        let result;

                        const javaproc = spawn('java', ['-cp',
                            '../java/target/clea-crypto-0.0.1-SNAPSHOT-jar-with-dependencies.jar ',
                            'fr.inria.clea.lsp.LspEncoderDecoder', 'decode',
                            cryptoItem.result,
                            cryptoItem.sk_l,
                            cryptoItem.pk_sa]);

                        javaproc.stdout.on('data', (data) => {
                            console.log(data.toString());
                            result = data.toString().trim().split(' ');
                            //assert.isTrue(true);
                        });

                        javaproc.stderr.on('data', (data) => {
                            console.error(data.toString());
                            //assert.isTrue(false);
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