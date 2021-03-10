# Proof of concept in C for the implementation of Location Specific Part (LSP) encoding according to the CLEA protocol

## Objectives

The QR code of a location/event, dynamic, which must be scanned at the entrance contains an URL ("deep link") structured by a prefix (for example for France: https://tac.gouv.fr/), followed by the 'location Specific Part' coded in base64. This directory gives an example of encoding in C language of the 'location Specific Part' of the QR code according to the [protocol Cléa](https://hal.inria.fr/hal-03146022).

This Proof of Concept can be used as a basis for:

* Validate the implementation of the encryption algorithms
* Generate locationSpecific Part  for specialised devices

## Dependancies

* https://github.com/kokke/tiny-AES-c
  * tiny_aes, AES128/192/256 in C small and portable
  * license [unlicense](http://unlicense.org/)
* https://github.com/ANSSI-FR/libecc
  * libecc, crypto. lib. based on elliptic curve
  * Dual licenses BSD and GPLv2

## Files description

* aes_gsm encoding, wp_supplicant sources adaptation(license BSD) to implement GCM (Galois/Counter Mode) and GMAC
  * `aes-gcm.c/h`: function `aes_gcm_encode` implementation
  * `aes256-gcm_test_vectors.rsp`: Vectors test of [NIST](https://csrc.nist.gov/projects/cryptographic-algorithm-validation-program)
  * `aes_internal.h`
  * `test_aes256-gcm.py`: launching tests from test vectors (executable, need Python3)
* Implementation, based on [RFC2104](https://tools.ietf.org/html/rfc2104) and [RFC4868](https://tools.ietf.org/html/rfc4868)
  * `hmac-sha256.c/h`: functions `hmac_sha256` et `hmac_sha256_128` implementation
  * `test_hmac-sha256-128.c`: executable tests
* Implementation of the ISO18033-2 based scheme with the characteristics specified by Cléa:: key encapsulation  ECIES-KEM, curve SECP256R1 is used for the pair keys ECDH, KDF1 based on HMAC-SHA256 and DEM based on AES-256-CTR
  * `ecies.c/h` `ecies_init` and `ecies_encode` implementation
  * `test_ecies.py`: executable tests on radom data
* LSP Cléa Encoding
  * `clea.c/h` functions `clea_init`, `clea_start_new_period` and  `clea_renew_qrcode` implementation
  * `test_clea.c`  executable test that periodically launch the generation of a 'location Specific Part' (LSP)
  * `build_clea.c` executable used to test consistency  with C encoding and Java decoding (see `../test`)
* `test_util.c/h` functions `parse` (char [] -> byte[]) et `dump` (print byte[]) for tests
* Exploring Qr code display on Raspberry Pi (sample work)
  * `rpi_clea.c` example of QR code display on Raspberry Pi
  * `openvg/` OpenVG graphics library adapted for Raspberry Pi
  * `qrcodegen/` library for graphic generation of QR code https://www.nayuki.io/page/qr-code-generator-library
* `libecc.patch` patch to apply to the extern lib `libecc`. The extern libraries `libecc` et `tiny_aes` are git cloned via par the cmake process
* `CMakeLists.txt` Project compilation

## Installation and use

Clone the project and install it

```shell
> mkdir build
> cd build
> cmake ..
> make
```

```shell
>./test_aes-gcm
Usage: ./test_aes-gcm key iv plain_text aad cipher_text tag

>./test_hmac-sha256-128
Test #1 passed
Test #2 passed
Test #3 passed

>./test_ecies
Usage: ./test_ecies random priv_key data

> test_clea
AE7ovhtS8OmTGOVkk/5kcp6G0r5FBDCbQQffyCRE4U5LSG2Muxk12q/iWhpnHVucPMIkAJ5UZVFnfGHjmt3B8xFVERSoCT0fiBtFE7GGZUSQVvvZJ3ujtdLTqVVsO2ONOsAuRqHNOqXdzlQlWzua2X2qOahDScgC1IHe00ftKR6aOrdCpn1ZA2XJeWmt6wIyJX+XKF/qUqL7/p0Bj9NvorGxWRmIST3c+OFHkbBLsw==
AE7ovhtS8OmTGOVkk/5kcp6yyiQaTivN8kC8MT9FaGlcMYXetqJm9hzUQZhlvV4DQFrPdXASsNuHPfrbQWHkwLtktrEj/y6DuTwQz774KyVtknUE6oMpBp8inzQaHx4mrimPqQa1vEkI7BiRKKbtcVYT6H7LYcHZq5sOKCIE4Pmm6mJcGXmU6CJWMhVZpAORdEQswqnR9k1gNibRhhQhVmzs+WAGVOGJeANUyj+rzA==

> ./build_clea 
Usage: build_clea staff CRIexp venueType venueCategory1 venueCategory2 countryCode periodDuration locationPhone locationPin PK_SA PK_MCTA
```
