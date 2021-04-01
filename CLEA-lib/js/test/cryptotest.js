const assert = require('chai').assert;
const csv=require('csvtojson')
const { spawn } = require('child_process');




describe('Crypto', function() {


    describe('test suite for crypto', function () {
        it('should return ', function() {
            csv()
                .fromFile('./crypto.csv')
                .then((jsonObj) => {
                    jsonObj.forEach(function (cryptoItem) {
                        console.log(cryptoItem);
                        assert.equal([1, 2, 3].indexOf(4), -1);
                        const javaproc = spawn('java', ['-cp',
                            '../java/target/clea-crypto-0.0.1-SNAPSHOT-jar-with-dependencies.jar ',
                            'fr.inria.clea.lsp.LspEncoderDecoder', 'decode',
                            cryptoItem.result,
                            cryptoItem.sk_l,
                            cryptoItem.pk_sa]);

                        javaproc.stdout.on('data', (data) => {
                            console.log(data.toString());
                        });

                        javaproc.stderr.on('data', (data) => {
                            console.error(data.toString());
                        });

                        javaproc.on('exit', (code) => {
                            console.log(`Child exited with code ${code}`);
                        });
                    })
                    /*cryptoList.forEach(function (cryptoItem) {
                        it('should return ok', async () => {
                            console.log(cryptoItem);
                            assert.equal([1, 2, 3].indexOf(4), -1);
                        });*/
                })
        });
    });

});
