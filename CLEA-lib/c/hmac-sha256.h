#ifndef HMAC_SHA256__
#define HMAC_SHA256__

#include <stdint.h>

/** HMAC-SHA256 as defined in RFC 4868 and RFC2104
 * @param key Byte array containing the key
 * @param key_size Key size in bytes, i.e. key array size, key_size MUST be less than 512 bits (64 bytes)
 * @param msg Byte array containing the message whose signature will be computed
 * @param len Size in bytes of the message, in this implementation the length of the message must be less than 64 bytes
 * @param mac Byte array of the computed signature. This array MUST be 32 bytes long (i.e. 256-bits) 
 */
void hmac_sha256(uint8_t *key, uint8_t key_size, uint8_t *msg, uint8_t len, uint8_t *mac);

/** HMAC-SHA256-128 as defined in RFC 4868 and RFC2104
 * @param key Byte array containing the key
 * @param key_size Key size in bytes, i.e. key array size, key_size MUST be less than 512 bits (64 bytes)
 * @param msg Byte array containing the message whose signature will be computed
 * @param len Size in bytes of the message, in this implementation the length of the message must be less than 64 bytes
 * @param mac Byte array of the computed signature. This array MUST be 16 bytes long (i.e. 128-bits) 
 */
void hmac_sha256_128(uint8_t *key, uint8_t key_size, uint8_t *msg, uint8_t len, uint8_t *mac);

#endif