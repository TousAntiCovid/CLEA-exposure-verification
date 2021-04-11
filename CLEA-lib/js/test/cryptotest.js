const expect = require('chai').expect;
const csv=require('csvtojson');
const { spawn } = require('child_process');
const glob = require("glob");
// setup : load cvs file
let cryptoList;
csv({noheader: true,
            headers:['sk_l','sk_mcta','sk_sa','result','staff','CRIexp','venueType','venueCategory1','venueCategory2','periodDuration','browser', 'locationPhone', 'locationRegion', 'locationPIN']})
    .fromFile('./crypto.csv')
    .then((jsonObj)=> {
        cryptoList = jsonObj;
    })


setTimeout(function() {

    describe('test suite for crypto', function () {
        let cleaCryptoJarPath;
        glob("../java/target/clea-crypto-*-jar-with-dependencies.jar", function (error, files) {
            cleaCryptoJarPath = files[0];
        });

            cryptoList.forEach(function (cryptoItem) {
                it('test on [' + cryptoItem.browser + '] with ' + cryptoItem.staff + ' ' + cryptoItem.CRIexp + ' ' + cryptoItem.venueType + ' ' + cryptoItem.venueCategory1
                    + ' ' + cryptoItem.venueCategory2 + ' '  + cryptoItem.periodDuration , async () => {
                    await new Promise((resolve) => {
                        let result = '';
                        const javaproc = spawn('java', ['-cp', cleaCryptoJarPath,
                            'fr.inria.clea.lsp.LspEncoderDecoder', 'decode',
                            cryptoItem.result,
                            cryptoItem.sk_sa,
                            cryptoItem.sk_mcta]);

                        javaproc.stdout.on('data', (data) => {
                            console.log(data.toString());
                            let str = data.toString().replace(/(\r\n|\n|\r)/gm, "").trim();
                            if (str && str.length > 0 ) {
                                // remove =VALUES=
                                result = str.substring(8).split(' ');
                            }
                        });

                        javaproc.stderr.on('data', (data) => {
                            console.error(data.toString());
                        });

                        javaproc.on('exit', (code) => {
                            console.log(`Child exited with code ${code}`);
                            expect(code).to.equal(0);
                            expect([9, 12]).to.include(result.length);
                            expect(result[0]).to.equal(cryptoItem.staff);
                            expect(result[1]).to.equal(cryptoItem.CRIexp);
                            expect(result[2]).to.equal(cryptoItem.venueType);
                            expect(result[3]).to.equal(cryptoItem.venueCategory1);
                            expect(result[4]).to.equal(cryptoItem.venueCategory2);
                            expect(result[5]).to.equal(cryptoItem.periodDuration);
                            // result[6] LTId
                            // result[7] ct_periodStart
                            // result[8] t_qrStart
                            if (result.length == 12) {
                                console.log('checking location');
                                expect(result[9]).to.equal(cryptoItem.locationPhone);
                                expect(result[10]).to.equal(cryptoItem.locationRegion);
                                expect(result[11]).to.equal(cryptoItem.locationPIN);
                            }
                            resolve();
                        });
                    })
                })
            })


        });
run();
}, 300);