# Proof of concept tests for the implementation of dynamic location specific part (LSP) encoding/decoding according to the CLEA protocol

## Objectives

The QR code of a location/event, dynamic, which must be scanned at the entrance contains a URL ("deep link") structured by a prefix, (for example for France: https://tac.gouv.fr?v=0#), followed by the 'location Specific Part' coded in base64. This directory contains tests to demonstrate a cycle of encoding, in C or Java, and decoding, in Java, of a LSP according to the [protocol Cléa](https://hal.inria.fr/hal-03146022).

This Proof of Concept can be used as a basis for:

* Validate the implementation of the encryption algorithms
* Generate locationSpecific Part for specialised devices

### Dependencies

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
3. test ok if the parameters in `encode_in.json` encoded  are identical to those decoded in `decode_out.json` at the end of the chain

To launch the tests use `python3 test_clea.py`:

The test cycle uses the C encoder and the Java decoder.

```shell
>python3 test_clea.py --csvtest
Encode LSP 1
Encode LSP 2
Encode LSP 3
Encode LSP 4
Encode LSP 5
Decode LSP 1
Decode LSP 2
Decode LSP 3
Decode LSP 4
Decode LSP 5
.
----------------------------------------------------------------------
Ran 2 tests in 7.949s>python3 test_clea.py 
```

To generate CSV files to be used by java decoding tests, run `python3 test_clea.py --csvtest`

It is also possible to test only the Java decoding phase `python3 test_clea.py --noencode` using as inputs the `encode_in.json` and `encode_out.json` (computed by a tier).

### Javascript

For debugging purpose, the web_decoder tool allows to decode in real-time a qrcode image captured by a webcam. To launch this tool, you need to load the webpage `web_decoder/index.html` in a web browser.

You should note that the reference decoder is the one in Java.
