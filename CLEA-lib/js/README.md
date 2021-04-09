# Proof of concept in Javascript for the implementation of Location Specific Part (LSP) encoding in accordance with the CLEA protocol

## Objectives

The QR code of a location/event, dynamic, which must be scanned at the entrance contains an URL ("deep link") structured by a prefix (for example for France: https://tac.gouv.fr?v=0#), followed by the 'location Specific Part' coded in base64. This directory gives an example of encoding in Java language of the 'location Specific Part' of the QR code according to the [protocol Cléa](https://hal.inria.fr/hal-03146022).

This Proof of Concept can be used as a basis for:

* Validating the Javascript implementation of the encryption algorithms
* To be used as an elementary brick to generate Qrcode using a web-browser

## Highlighted Dependencies

This javascript is dedicated to run on web-browser implementing the [Web Cryptography API](https://www.w3.org/TR/WebCryptoAPI/) recommended by W3C.

### Files description

* `clea.js`: LSP Encoding in base64 using parameters and time respecting the CLEA protocol.

An example of use of `clea.js` is done by :

* `index.html`: html page to be loaded on a web-browser
* `index.js`: javascript getting config values and calling periodically the qrcode generator

When the html page is loaded, the current qrcode is displayed and you can input new config values. Every 10 secondes, a new qrcode is generated.

### Dependencies
* `qrcodejs2` version 0.0.2 : javascript to manage the display of the qrcode and the widget to input parameters
* `jquery` version 3.4.1

### Web browser supported

validation list in progress:

* Firefox 86
* Chrome 89
## Continuous Integration


### installation

```shell
apt-get install -y firefox-esr chromium
export CHROME_BIN=chromium
export FIREFOX_BIN=firefox-esr
cd ../java; mvn install
cd ../js
npm install
```

### build

```shell
npm run build
```

### development deployment

To test the example, run a local web server with the following command and browse http://localhost:1234 
```shell
npm run dev
```

### Test

* `npm test` : run unit tests on clea.js and build a csv file with all produced encrypted results. Encryption results are produced from data in `CLEA-lib/test/encode_in.json`. The file is copied to js/test/dataset.js and udpated to allow its inclusion as a standard JavaScript file.
* `npm run testcrypto` : interoperabilty tests to check the javascript encryption (crypto.csv file produced by `npm test`) against the java decryption
