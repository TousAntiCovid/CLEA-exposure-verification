#!/usr/bin/python3
import subprocess
import sys

class TestVector:
    WAIT_CONFIG = 0
    WAIT_PARAMS = 1
    WAIT_NEXT = 2

    def __init__(self, filename):
        self.test_count = 0
        self.f = open(filename, "r")
        self.state = TestVector.WAIT_CONFIG
        self.__init_config()
        self.__init_params()

    def __init_config(self):
        self.key_len = -1
        self.iv_len = -1
        self.pt_len = -1
        self.aad_len = -1
        self.tag_len = -1

    def __init_params(self):
        self.count = -1
        self.key = ""
        self.iv = ""
        self.pt = ""
        self.aad = ""
        self.ct = ""
        self.tag = ""

    def parse(self):    
        for l in self.f.readlines():
            nl = l[:-1]
            if len(nl) > 0:
                if nl[0] != "#":
                    self.__fsm_feed_line(nl)

    def __fsm_feed_line(self, l):
        if self.state == TestVector.WAIT_CONFIG:
            if l[0] == "[":
                key_val = l[1:-1].split(" = ");

                if key_val[0] == "Keylen":
                    self.key_len = int(key_val[1])
                elif key_val[0] == "IVlen":
                    self.iv_len = int(key_val[1])
                elif key_val[0] == "PTlen":
                    self.pt_len = int(key_val[1])
                elif key_val[0] == "AADlen":
                    self.aad_len = int(key_val[1])
                elif key_val[0] == "Taglen":
                    self.tag_len = int(key_val[1])
                    self.state = TestVector.WAIT_PARAMS
                else:
                    raise Exception("Unknown key '%s'" % key_val[0])
            else:
                raise Exception("Unexpected line while waiting for configuration: %s" % l)
        elif self.state == TestVector.WAIT_PARAMS:
            key_val = l.split(" = ")

            if key_val[0] == "Count":
                self.count = int(key_val[1])
            elif key_val[0] == "Key":
                self.key = key_val[1]
            elif key_val[0] == "IV":
                self.iv = key_val[1]
            elif key_val[0] == "PT":
                self.pt = key_val[1]
            elif key_val[0] == "AAD":
                self.aad = key_val[1]
            elif key_val[0] == "CT":
                self.ct = key_val[1]
            elif key_val[0] == "Tag":
                self.tag = key_val[1]
                self.__execute_test()
                self.state = TestVector.WAIT_NEXT
            else:
                raise Exception("Unknown key '%s'" % key_val[0])
        elif self.state == TestVector.WAIT_NEXT:
            if l[0] == "[":
                self.state = TestVector.WAIT_CONFIG
                self.__init_config()
                self.__fsm_feed_line(l)
            else:
                self.state = TestVector.WAIT_PARAMS
                self.__init_params()
                self.__fsm_feed_line(l)

    def __execute_test(self):
        if self.tag_len == 128:
            self.test_count += 1
            s = subprocess.Popen(["./build/test_aes-gcm", self.key, self.iv, self.pt, self.aad, self.ct, self.tag], stdout = subprocess.PIPE)
            s.wait()
            if s.returncode == 0:
                print("Test #%d: PASS" % self.test_count)
            else:
                print("Test #%d: FAIL" % self.test_count)
                print("%s\n" % s.stdout.read().decode())

tv = TestVector("aes256-gcm_test_vectors.rsp")

tv.parse()
