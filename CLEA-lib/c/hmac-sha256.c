#include "hash/sha256.h"
#include "hmac-sha256.h"

/* This implementation is based on RFC2104 and RFC4868
 * https://tools.ietf.org/html/rfc2104
 * https://tools.ietf.org/html/rfc4868
 */

#define IPAD (0x36)
#define OPAD (0x5C)

void hmac_sha256(uint8_t *key, uint8_t key_size, uint8_t *msg, uint8_t len, uint8_t *mac)
{
    static uint8_t block[2 * SHA256_BLOCK_SIZE];
    static uint8_t block2[SHA256_BLOCK_SIZE + SHA256_DIGEST_SIZE];
    uint8_t i;

    // (1) append zeros to the end of the key to create a SHA256_BLOCK_SIZE byte array
    // (2) XOR with ipad
    for(i = 0; i < SHA256_BLOCK_SIZE; i++)
    {
        if(i < key_size)
        {
            block[i] = key[i] ^ IPAD;
        }
        else
        {
            block[i] = IPAD;
        }
    }

    // (3) append the stream of data 'msg' to the SHA256_BLOCK_SIZE byte string resulting from step (2)
    for(i = 0; i < SHA256_BLOCK_SIZE; i++)
    {
        if(i < len)
        {
            block[i + SHA256_BLOCK_SIZE] = msg[i];
        }
        else
        {
            break;
        }
    }

    // (4) apply H to the stream generated in step (3)
    sha256(block, SHA256_BLOCK_SIZE + len, &(block2[SHA256_BLOCK_SIZE]));

    // (5) XOR the key with opad
    // (6) append the H result from step (4)
    for(i = 0; i < SHA256_BLOCK_SIZE; i++)
    {
        if(i < key_size)
        {
            block2[i] = key[i] ^ OPAD;
        }
        else
        {
            block2[i] = OPAD;
        }
    }

    // (7) apply H to the stream generated in step (6) and output the result
    sha256(block2, SHA256_BLOCK_SIZE + SHA256_DIGEST_SIZE, mac);

}

void hmac_sha256_128(uint8_t *key, uint8_t key_size, uint8_t *msg, uint8_t len, uint8_t *mac)
{
    static uint8_t block[SHA256_DIGEST_SIZE];
    uint8_t i;

    hmac_sha256(key, key_size, msg, len, block);

    for(i = 0; i < SHA256_DIGEST_SIZE / 2; i++)
    {
        mac[i] = block[i];
    }
}