# Proof of concept in Javascript for the implementation of Location Specific Part (LSP) encoding in accordance with the CLEA protocol

## Objectives

The QR code of a location/event, dynamic, which must be scanned at the entrance contains an URL ("deep link") structured by a prefix (for example for France: https://tac.gouv.fr/), followed by the 'location Specific Part' coded in base64. This directory gives an example of encoding in Java language of the 'location Specific Part' of the QR code according to the [protocol Cléa](https://hal.inria.fr/hal-03146022).

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
* `qrcode.min.js`: javascript to manage the display of the qrcode and the widget to input parameters

When the html page is loaded, the current qrcode is displayed and you can input new config values. Every 10 secondes, a new qrcode is generated.

### Web browser supported

validation list in progress:

* Firefox 86
* Chrome 89
