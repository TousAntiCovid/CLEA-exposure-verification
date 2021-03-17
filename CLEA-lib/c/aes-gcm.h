#ifndef AES_GCM__
#define AES_GCM__

#include <stdint.h>

#define AES_BLOCK_SIZE 16

int32_t aes_gcm_encode(const uint8_t *key, const uint8_t *iv, uint8_t iv_len, const uint8_t *plain, uint8_t plain_len, const uint8_t *aad, uint8_t aad_len, uint8_t *crypt, uint8_t *tag);

#endif