# Proof of concept in Java for the implementation of Location Specific Part (LSP) encoding in accordance with the CLEA protocol

## Objectives

The QR code of a location/event, dynamic, which must be scanned at the entrance contains an URL ("deep link") structured by a prefix (for example for France: https://tac.gouv.fr/), followed by the 'location Specific Part' coded in base64. This directory gives an example of encoding in Java language of the 'location Specific Part' of the QR code according to the [protocol Cléa](https://hal.inria.fr/hal-03146022).

This Proof of Concept can be used as a basis for:

* Validating the Java implementation of the encryption/decryption algorithms
* Decoding the generator outputs of the specialised devices in the test phase
* To be used as an elementary brick to emulate generators for demonstrators
* To be used as an example to generate the location Specific Part for tablets, if required
* To be used as an example to decode the location Specific Part

### Dependencies

* https://github.com/devnied/Bit-lib4j
  * bits and bytes manipulation
  * Apache License 2.0
* https://www.bouncycastle.org/fr/
  * https://github.com/bcgit/bc-java
  * crypto library
  * license is an adaptation of the MIT X11 License and should be read as such.
* https://commons.apache.org/proper/commons-net/
  * timestamp NTP
  * Apache license 2.0
* https://opensource.google/projects/zxing
  * Qrcode image generation
  * use only for tests (test5)
  * Apache License 2.0
  
### Files description

* `Data.java`: Parameters stored in the LSP
* `Ecies.java`: Encryption algorithms and Elliptical Keys. Implementation of the scheme based on ISO18033-2 with the characteristics set by Cléa: encapsulation of ECIES-KEM keys, the SECP256R1 curve is used for the ECDH key pair, KDF1 based on HMAC-SHA256 and DEM based on AES-256-CTR.
* `Encode.java`: LSP Encoding in base64 using parameters and time. Daughter class of Data and containing an Ecies instance.
* `Decode.java`: Decoding of a Qrcode in base64 to extract all the parameters. Daughter class of Data and containing an Ecies instance.
* `Test.java`: Package Tests

### Installation and Use

* clone the project and install it (`mvn install`)
* `java  -cp  target/clea-lsp-0.0.1-SNAPSHOT-jar-with-dependencies.jar clea.lsp.Test`

Possibility to use encoding and decoding of a LSP using the main executable (`main/Test.java`)

* `Usage: Test [read  qrcode64 privKey] [build staff countryCode CRIexp venueType venueCategory1 venueCategory2 periodDuration pubkey]`
* the result is displayed on the console
* used for C(encoding)/Java(decoding) operability tests in the project `../test`.
* if the executable is launched without parameters, the functional tests of Test are launched (test1-8)

## TODO

* Assert on BitUtils size (verification)
* Assert on bit size parameters
* Exceptions recovery

## Useful web links

* https://julien-millau.fr/projects/Manipulation-de-bit.html
* https://github.com/devnied/Bit-lib4j
* https://stackoverflow.com/questions/17893609/convert-uuid-to-byte-that-works-when-using-uuid-nameuuidfrombytesb
* https://www.service-public.fr/professionnels-entreprises/vosdroits/F32351
* http://koclab.cs.ucsb.edu/teaching/cren/project/2013/xia2.pdf
* https://www.nominet.uk/how-elliptic-curve-cryptography-encryption-works/
* http://digital.csic.es/bitstream/10261/32671/1/V2-I2-P7-13.pdf
* http://tutorials.jenkov.com/java-cryptography/cipher.html
