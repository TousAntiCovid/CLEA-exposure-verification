#!/usr/bin/python3
import subprocess
import random
import sys

class TestECIES:
    def __init__(self):
        pass

    def execute_test(self):
        r = "%x" % random.randint(0, 2**255)
        key = "%x" % random.randint(0, 2**255)
        data = "%x" % random.randint(0, 2**(61 * 8))

        print("Random number: %s" % r)
        print("Private key: %s" % key)
        print("Plain text: %s" % data)

        s = subprocess.Popen(["./build/test_ecies", r, key, data], stdout = subprocess.PIPE)
        s.wait()
        out = s.stdout.read().decode()[:-1]

        print("Cipher text: %s" % out)

t = TestECIES()

t.execute_test()
