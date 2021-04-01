#!/usr/bin/env python
# -*- coding: utf-8 -*-

# Copyright Inria 2021
#
import json
import subprocess
import os
import argparse
import unittest
import sys

CSV_LSP_TST = 'testLSPDecoding.csv'
CSV_LOC_TST = 'testLocationDecoding.csv'
CSV_TEST_FILES_GENERATION = False

class CleaEncoderInteroperabilityTestCase(unittest.TestCase):
    def setUp(self):
        # clean output files
        self.ENC_IN = 'encode_in.json'
        self.ENC_OUT = 'encode_out.json'
        self.DEC_OUT = 'decode_out.json'
        if os.path.exists(self.ENC_OUT):
            os.remove(self.ENC_OUT)
        if os.path.exists(self.DEC_OUT):
            os.remove(self.DEC_OUT)
    
    @unittest.skip(reason="need to build C lib before running this test")
    def testCEncodingAndJavaDecoding(self):
        lsps_encode(self.ENC_IN, self.ENC_OUT, java=False)
        # encode_out.json -> [lsps_decode] -> decode_out.json
        lsps_decode(self.ENC_OUT, self.DEC_OUT)
        # compare parameters input or generated (time, ltid) and output paramaters
        lsps_cmp(self.ENC_IN, self.ENC_OUT, self.DEC_OUT, CSV_LSP_TST, CSV_LOC_TST)

    def testJavaEncodingAndJavaDecoding(self):
        lsps_encode(self.ENC_IN, self.ENC_OUT, java=True)
        # encode_out.json -> [lsps_decode] -> decode_out.json
        lsps_decode(self.ENC_OUT, self.DEC_OUT)
        # compare parameters input or generated (time, ltid) and output paramaters
        lsps_cmp(self.ENC_IN, self.ENC_OUT, self.DEC_OUT, CSV_LSP_TST, CSV_LOC_TST)


""" Test interoperability between C/Java location Specific Part (LSP) encoding
and Java LSP decoding
"""

# Java executable can encode or decode
# a Clé location Specific Part (LSP)
CMD_JAVA = ['java', '-cp',
            '../java/target/clea-crypto-0.0.2-SNAPSHOT-jar-with-dependencies.jar',
            'fr.inria.clea.lsp.LspEncoderDecoder']
# C executable can encode t a Cléa LSP
CMD_C = ['../c/build/test_clea']


def run_cmd(cmd_with_args):
    """ Run a executable with arguments and set the output
         as a sequence of word separated by space

    Parameters
    ----------
    cmd_with_args: used by a shell command

    Returns
    -------
    result as a sequence of word separated by space
    """
    with subprocess.Popen(cmd_with_args, stdout=subprocess.PIPE) as process:
        process.wait()
        out = process.stdout.read()
        process.kill()
    
    outs = out.decode().replace("\n", "")
    outs = outs.split('=VALUES=')
    results = outs[1].split(' ')
    return results


def lsp_encode(cfg, java=False):
    """ Encode a LSP using a dictionnary of Cléa parameters as input

    Parameters
    ----------
    cfg: dictionnary containing parameters staff, CRIexp, venueType ...

    Returns
    -------
    a dictionnary with "lspBase64" and EC private keys: 'PK_SA', 'PK_MCTA'
    """
    cmd = []
    if java:
        cmd.extend(CMD_JAVA)
        cmd.append('encode')
    else:
        cmd.extend(CMD_C)
    cmd.append(str(cfg['staff']))
    cmd.append(str(cfg['countryCode']))
    cmd.append(str(cfg['CRIexp']))
    cmd.append(str(cfg['venueType']))
    cmd.append(str(cfg['venueCategory1']))
    cmd.append(str(cfg['venueCategory2']))
    cmd.append(str(cfg['periodDuration']))
    cmd.append(cfg['PK_SA'])
    cmd.append(cfg['PK_MCTA'])
    cmd.append(cfg['SK_L'])

    phone = cfg.get('locationPhone')
    region = cfg.get('locationRegion')
    pin = cfg.get('locationPIN')
    if (phone is not None and region is not None and pin is not None):
        cmd.append(str(phone))
        cmd.append(str(region))
        cmd.append(str(pin))

    vals = run_cmd(cmd)
    if len(vals) == 4:
        lsp_base_64 = {"lsp_base64": vals[0],
                       "LTId": vals[1],
                       "ct_periodStart": int(vals[2]),
                       "t_qrStart": int(vals[3]),
                       "SK_SA": cfg["SK_SA"],
                       "SK_MCTA": cfg["SK_MCTA"]}
    else:
        lsp_base_64 = {"Error": "lsp_encode failed"}
    return lsp_base_64


def lsp_decode(cfg):
    """ Decode a LSP

    Parameters
    ----------
    cfg:  a dictionnary with "lspBase64" and "privKey"

    Returns
    -------
    a dictionnary containing parameters staff, CRIexp, venueType ...
    """
    cmd = []
    cmd.extend(CMD_JAVA)
    cmd.append('decode')
    cmd.append(str(cfg['lsp_base64']))
    cmd.append(cfg['SK_SA'])
    cmd.append(cfg['SK_MCTA'])
    vals = run_cmd(cmd)
    if len(vals) == 10 or len(vals) == 13:
        lsp_dict = {"staff": int(vals[0]),
                    "countryCode": int(vals[1]),
                    "CRIexp": int(vals[2]),
                    "venueType": int(vals[3]),
                    "venueCategory1": int(vals[4]),
                    "venueCategory2": int(vals[5]),
                    "periodDuration": int(vals[6]),
                    "LTId": vals[7],
                    "ct_periodStart": int(vals[8]),
                    "t_qrStart": int(vals[9]),
                    "SK_SA": cfg['SK_SA'],
                    "SK_MCTA": cfg['SK_MCTA']}
        if len(vals) == 13:
            lsp_dict["locationPhone"] = vals[10]
            lsp_dict["locationRegion"] = int(vals[11])
            lsp_dict["locationPIN"] = vals[12]
    else:
        lsp_dict = {"Error": "lsp_decode failed"}
    return lsp_dict


def lsps_encode(inputfilename, outputfilename, java=True):
    """ Encode a list of Cléa LSP

    Parameters
    ----------
    inputfilename: json format file which contains a list of
                    dictionnary (see params_in.json, for example)
                    as inputs
    outputfilename: json format file which contains a list of the
                    LSP in base64 and the EC private key as output
    """
    with open(inputfilename) as fid:
        inputs = json.load(fid)
        idxt = 1
        lsp_base_64s = []
        for cfg in inputs:
            print('Encode LSP', idxt)
            lsp_base_64 = lsp_encode(cfg, java)
            lsp_base_64s.append(lsp_base_64)
            idxt = idxt + 1

        with open(outputfilename, 'w') as outfile:
            json.dump(lsp_base_64s, outfile, indent=4)


def lsps_decode(inputfilename, outputfilename):
    """ Decode a list of Cléa location Specific Part (LSP)

    Parameters
    ----------
    inputfilename: json format file which contains a list of the
                    LSP in base64 and the EC private key as inputs
    outputfilename: json format file which contains a list of
                    dictionnary (see params_in.json, for example)
                    as output
    """
    with open(inputfilename,) as fid:
        inputs = json.load(fid)
        idxt = 1
        lsps = []
        for cfg in inputs:
            print('Decode LSP', idxt)
            lsp_param = lsp_decode(cfg)
            lsps.append(lsp_param)
            idxt = idxt + 1

        with open(outputfilename, 'w') as outfile:
            json.dump(lsps, outfile, indent=4)


def lsp_cmp(enc_in, enc_out, dec_out):
    """
    Check if the parameters of a LSP encoded and decoded
    are identical

    Parameters
    ----------
    enc_in: input dict LSP encoder
    enc_out: output dict LSP encoder
    dec_out: output dict LSP decoder
    """
    assert enc_in.get("Error") is None
    assert dec_out.get("Error") is None

    assert enc_in['staff'] == dec_out['staff']
    assert enc_in['CRIexp'] == dec_out['CRIexp']
    assert enc_in['venueType'] == dec_out['venueType']
    assert enc_in['venueCategory1'] == dec_out['venueCategory1']
    assert enc_in['venueCategory2'] == dec_out['venueCategory2']
    assert enc_in['countryCode'] == dec_out['countryCode']
    assert enc_in['periodDuration'] == dec_out['periodDuration']
    assert enc_out['LTId'] == dec_out['LTId']
    assert enc_out['ct_periodStart'] == dec_out['ct_periodStart']
    assert enc_out['t_qrStart'] == dec_out['t_qrStart']
    nbr = int(enc_in.get('locationPhone') is not None) + \
        int(enc_in.get('locationPIN') is not None) + \
        int(enc_in.get('locationRegion') is not None) + \
        int(dec_out.get('locationPhone') is not None) + \
        int(dec_out.get('locationRegion') is not None) + \
        int(dec_out.get('locationPIN') is not None)
    if nbr == 6:
        assert enc_in['locationPhone'] == dec_out['locationPhone']
        assert enc_in['locationRegion'] == dec_out['locationRegion']
        assert enc_in['locationPIN'] == dec_out['locationPIN']
    else:
        assert nbr == 0, 'LocationMsg failed'

def lsps_cmp(enc_in_file, enc_out_file, dec_out_file, csv_lsp_file, csv_loc_file):
    """
    Compare a list of LSP parameters
    ----------
    enc_in_file: input file in json with user parameters for the lsp encoder
    enc_out_file: output file in json of the lsp encoder with generated lsp
                parameters (time, TLId)
    dec_out_file: output file in json of the lsp decoder
    csv_lsp_file: save LSP encoding/decoding results to be updated when necessary
                    for junit5 test in ../java/src/test/resources
    csv_loc_file: save location encoding/decoding results to be updated when necessary
                    for junit5 test in ../java/src/test/resources
    """
    with open(enc_in_file) as fid1, \
            open(enc_out_file) as fid2, \
            open(dec_out_file) as fid3:
        enc_in_s = json.load(fid1)
        enc_out_s = json.load(fid2)
        dec_out_s = json.load(fid3)
        assert len(enc_in_s) == len(enc_out_s) == len(dec_out_s), "problem with number of tests"
        for idx, _ in enumerate(enc_in_s):
            lsp_cmp(enc_in_s[idx], enc_out_s[idx], dec_out_s[idx])
            if CSV_TEST_FILES_GENERATION:
                save_lsp_encoding_decoding_results(enc_in_s[idx], enc_out_s[idx], csv_lsp_file, csv_loc_file)
    
def save_lsp_encoding_decoding_results(enc_in, enc_out, csv_lsp_file, csv_loc_file):
    sep = ', '
    if csv_lsp_file is not None:
        row = str(enc_in['staff']) + sep + str(enc_in['countryCode']) + sep
        row += str(enc_out['LTId']) + sep + str(enc_in['CRIexp']) + sep
        row += str(enc_in['venueType']) + sep + str(enc_in['venueCategory1']) + sep
        row += str(enc_in['venueCategory2']) + sep + str(enc_in['periodDuration']) + sep
        row += str(enc_out['ct_periodStart']) + sep + str(enc_out['t_qrStart']) + sep
        row += str(enc_in['SK_SA']) + sep + str(enc_in['PK_SA']) + sep
        row += str(enc_out['lsp_base64'])
        with open(csv_lsp_file, 'a') as outFile:
            outFile.write(row + '\n')
    if csv_loc_file is not None and enc_in.get('locationPhone') is not None:
        row = str(enc_in['locationPhone']) + sep + str(enc_in['locationRegion']) + sep
        row += str(enc_in['locationPIN']) + sep + str(enc_out['ct_periodStart']*3600)
        row += sep + str(enc_in['SK_SA']) + sep + str(enc_in['PK_SA']) + sep
        row += str(enc_in['SK_MCTA']) + sep + str(enc_in['PK_MCTA']) + sep
        row += str(enc_out['lsp_base64'])
        with open(csv_loc_file, 'a') as outFile:
            outFile.write(row + '\n')

if __name__ == "__main__":
    # Parse command line arguments
    parser = argparse.ArgumentParser()
    parser.add_argument("--csvtest",
                        help="saving file testDecoding.csv",
                        action="store_true")
    args = parser.parse_args()
    
    if args.csvtest:
        CSV_TEST_FILES_GENERATION =  True
        with open(CSV_LSP_TST, "w") as outFile:
            HEADER = 'staff, countryCode, LTId, CRIexp, venueType, venueCat1, venueCat2, periodDuration, ct_periodStart, t_qrStart, SK_SA, PK_SA, lsp_base64\n'
            outFile.write(HEADER)
        with open(CSV_LOC_TST, "w") as outFile:
            HEADER = 'locationPhone, locationRegion, locationPin, t_periodStart, SK_SA, PK_SA, SK_MCTA, PK_MCTA, lsp_base64\n'
            outFile.write(HEADER)
    
    sys.argv = [ sys.argv[0] ]
    
    unittest.main()
