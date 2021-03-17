# Proof of concept tests for the implementation of dynamic location specific part (LSP) encoding/decoding according to the CLEA protocol

## Objectives

The QR code of a location/event, dynamic, which must be scanned at the entrance contains a URL ("deep link") structured by a prefix, (for example for France: https://tac.gouv.fr/), followed by the 'location Specific Part' coded in base64. This directory contains tests to demonstrate a cycle of encoding, in C or Java, and decoding, in Java, of a LSP according to the [protocol Cléa](https://hal.inria.fr/hal-03146022).

This Proof of Concept can be used as a basis for:

* Validate the implementation of the encryption algorithms
* Generate locationSpecific Part for specialised devices

### Dépendancies

* Python 3
  
### Files description

* `encode_in.json`: a set of input parameters
* `test_clea.py`: test executable in Python
* `test_clea.html`: html page test for javascript CLEA
* `test_clea.js`: javascript embedded in `test_clea.html`

### C/Java

The test cycle for each set is as follows:

1. encode_in.json -> [lsp_encode] -> encode_out.json (private key + lsp in base64 format)
2. encode_out.json -> [lsp_decode] -> decode_out.json
3. test ok if the parameters in `encode_in.json` encoded  are identical to those decoded in `de   code_out.json` at the end of the chain

To launch the test cyle use `test_clea.py`:

```bash
python test_clea.py --help
usage: test_clea.py [-h] [--noencode] [--java]

optional arguments:
  -h, --help  show this help message and exit
  --noencode  test only the decoding part
  --java      encoding part with Java lib (C lib by default)
```

By default, the test cycle uses the C encoder (see below). The option `-java` allows to use the Java encoder.

```shell
>python3 test_clea.py 
qrcode build 1
qrcode build 2
qrcode read 1
qrcode read 2
TEST PASS: 1
TEST PASS: 2
ALL TESTS PASS
```

### Javascript

For using the javacript encoder in phase 1. of the test cycle, we need to load the page `test_clea.html`, load the json file `encode_in.json` (button browse...) and save the results in `encode_out.json` (button Write).

It is now possible to test the Java decoding to check interoperability using `python3 test_clea.py --noencode`. The option allows the skip the C or java encoding performed by your web-browser.