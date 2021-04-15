# Proof of concept in Java for the implementation of Location Specific Part (LSP) encoding in accordance with the CLEA protocol

## Objectives

The QR code of a location/event, dynamic, which must be scanned at the entrance contains an URL ("deep link") structured by a prefix (for example for France: https://tac.gouv.fr?v=0#), followed by the 'location Specific Part' coded in base64. This directory gives an example of encoding in Java language of the 'location Specific Part' of the QR code according to the [protocol Cléa](https://hal.inria.fr/hal-03146022).

This Proof of Concept can be used as a basis for:

* Validating the Java implementation of the encryption/decryption algorithms
* Decoding the generator outputs of the specialised devices in the test phase
* To be used as an elementary brick to emulate generators for demonstrators
* To be used as an example to generate the location Specific Part for tablets, if required
* To be used as an example to decode the location Specific Part

### Highlighted Dependencies

* https://www.bouncycastle.org/fr/
  * https://github.com/bcgit/bc-java
  * crypto library
  * license is an adaptation of the MIT X11 License and should be read as such.
* https://github.com/devnied/Bit-lib4j
  * bits and bytes manipulation
  * Apache License 2.0
* https://opensource.google/projects/zxing
  * Qrcode image generation
  * use only for tests (test5)
  * Apache License 2.0

All dependencies are noted in the `pom.xml`
  
### Files description

* `utils/TimeUtils.java`:
* `CleaEciesEncoder.java`: Encryption/Decription respecting ECIES-KEM (Elliptic Curve Integrated Encryption Scheme with Key encapsulation mechanisms)
* `CleaEncryptionException.java`: Generic Clea exception thrown when something went wrong while encoding / decoding.
* `Location.java`: Location QRcode management
* `LocationContact.java`: Location Contact data (phone number, pin code, starting time) in plain text
* `LocationContactMessageEncoder.java`: LocationContact encoding/decoding
* `LocationSpecificPart.java`: Location Specific Part data in plain text
* `LocationSpecificPartEncoder.java`: Location Specific Part encoding
* `LocationSpecificPartDecoder.java`: Location Specific Part decoding
* `LspEncoderDecoder.java`: Main executable used for encoding)/Java(decoding) operability tests

### Installation and Use

* clone the project and test it
* test: `mvn test`
* install: `mvn package`

Possibility to use encoding and decoding of a LSP using the main executable (`LspEncoderDecoder.java`)

* `java  -jar  java/target/clea-lsp-*-jar-with-dependencies.jar`
* `Usage: LspEncoderDecoder [gen-keys] [decode  lsp64 privKey] [encode staff countryCode CRIexp venueType venueCategory1 venueCategory2 periodDuration locationPhone locationPin pubkey]`
* the result is displayed on the console
* To generate a Clea key pair, use `java  -jar  java/target/clea-lsp-*-jar-with-dependencies.jar gen-keys`. Generated keys will be displayed on std output.
* used for C(encoding)/Java(decoding) operability tests in the project `../test`.

## TODO

* Modification for JS lib compatibility
* More tests

## Useful web links

* https://julien-millau.fr/projects/Manipulation-de-bit.html
* https://github.com/devnied/Bit-lib4j
* http://digital.csic.es/bitstream/10261/32671/1/V2-I2-P7-13.pdf
* https://www.bouncycastle.org/
* http://tutorials.jenkov.com/java-cryptography/cipher.html

