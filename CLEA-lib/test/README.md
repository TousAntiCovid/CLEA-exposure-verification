# Proof of concept tests for the implementation of dynamic location specific part (LSP) encoding/decoding according to the CLEA protocol

## Objectives

The QR code of a location/event, dynamic, which must be scanned at the entrance contains a URL ("deep link") structured by a prefix, (for example for France: https://tac.gouv.fr/), followed by the 'location Specific Part' coded in base64. This directory contains tests to demonstrate a cycle of encoding, in C or Java, and decoding, in Java, of a LSP according to the [protocol Cléa](https://hal.inria.fr/hal-03146022).

This Proof of Concept can be used as a basis for:

* Validate the implementation of the encryption algorithms
* Generate locationSpecific Part for specialised devices

### Dépendancies

* Python 3
  
### Files description

* `params_in.json`: a set of input parameters
* `test_clea.py`: test executable in Python

The test cycle for each set is as follows:

1. params_in.json -> [lsp_encode] -> lsp_out.json (private key + lsp in base64 format)
2. lsp_out.json -> [lsp_decode] -> params_out.json
3. test ok if the parameters in `params_in.json` encoded  are identical to those decoded in `params_out.json` at the end of the chain

### Use

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
