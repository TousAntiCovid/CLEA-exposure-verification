# Proof of concept for the implementation of dynamic Location Specific Part (LSP) encoding/decoding according to the CLEA protocol

The Cluster Exposure Verification (Cléa) protocol is intended to notify participants of a private event (e.g. wedding or private party) or present in a commercial or public place (e.g. bar, restaurant, sports centre or train) that has become a cluster because a certain number of people present at the same time have been COVID+ tested. The protocol is described in detail in the document [The Cluster Exposure Verification (Cléa) Protocol: Specifications of the Lightweight Version](https://hal.inria.fr/hal-03146022/)

The aim of this development is to demonstrate an implementation of part of this protocol at the level of the dynamic Qr code encoding/decoding specification described in part `3.4- Dynamic QR code generation within the device` of the document.

In particular, it is foreseen that the display of the QR code dynamically is carried out by a specialised device with a microcontroller (example: MICROCHIP microcontroller, PIC32MM0256GPM036-I/M2) with low computing capacities. Moreover, it is planned to encrypt certain data using algorithms (e.g. ECIES-KEM) that are not necessarily found in 'standard off-the-shelf libraries'. This raised at least two points of attention which are illustrated in this implementation.

This repertoire therefore includes

* a C implementation (see [README.md](c/README.md)) of the LSP encoding (base64 data)
* a Java implementation (see [README.md](java/README.md)) of the encoding/decoding of LSP
* Tests to demonstrate a cycle of encoding, in C or Java, and decoding, in Java, of a LSP (see [README.md](test/README.md)).

This work can serve as inspiration for a C implementation dedicated to specialised devices or to more generic devices (PC, tablet, telephone) in Java.

This source code is subject to the terms under the Mozilla Public License 2.0 (see [LICENSE.md](LICENSE.md)).