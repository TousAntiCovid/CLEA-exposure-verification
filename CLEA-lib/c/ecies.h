#ifndef ECIES_H__
#define ECIES_H__

#include <stdint.h>

// This is an implementation of the ISO18033-2 standard with the following characteristics
// ECIES-KEM encapsulation is used 
// ECDH uses the SECP256R1 curve
// KDF is KDF1 based on HMAC-SHA256
// DEM is AES-256-CTR

/** Initialize ECIES scheme
 * @param key The ECDH public key, this key is supposed to be 512-bit-long (64 bytes)
 */
void ecies_init(void);

/** Encode message using ECIES scheme
 * @param msg The byte array containing the message to be encoded. The content is composed of a clear part, followed by a part to be encrypted. This array will be transformed by encrypting the right part and appending the key and tag, so this buffer need to had additionnal 264 + 128bits, i.e. 49 bytes
 * @param clear_len The length of the clear part of message
 * @param crypt_len The length of the encrypted part of the message
 * @return 0 if everything went fine -1 otherwise
 */
int32_t ecies_encode(uint8_t *key, uint8_t *msg, uint8_t clear_len, uint8_t crypt_len);

#endif