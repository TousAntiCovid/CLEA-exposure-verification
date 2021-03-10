#include <stdio.h>
#include <stdint.h>
#include "aes-gcm.h"
#include "test_util.h"

uint8_t key[32];
uint8_t iv[128];
uint8_t plain[64];
uint8_t aad[1024];
uint8_t crypt[64];
uint8_t tag[16];

uint8_t crypt_buf[64];
uint8_t plain_buf[64];
uint8_t tag_buf[16];

int main(int argc, char **argv)
{
    uint8_t result, ret;
    uint8_t i;
    uint8_t iv_size, pt_size, tag_size, aad_size;

    if(argc != 7)
    {
        printf("Usage: %s key iv plain_text aad cipher_text tag\n", argv[0]);
        return -1;
    }

    parse(argv[1], key);
    iv_size =  parse(argv[2], iv);
    pt_size =  parse(argv[3], plain);
    aad_size = parse(argv[4], aad);
    /* size */ parse(argv[5], crypt);
    tag_size = parse(argv[6], tag);

    result = aes_gcm_encode(key,
                            iv, iv_size,
                            plain, pt_size,
                            aad, aad_size,
                            crypt_buf, tag_buf);
    ret = result;
    printf("t3 aes_gcm encrypt result %s\n", result == 0 ? "PASS" : "FAIL");
    
    result = 0;
    for(i = 0; i < pt_size; i++)
    {
        if(crypt[i] != crypt_buf[i])
        {
            result = 1;
            ret = 1;
            break;
        }
    }
    printf("t3 aes_gcm encrypt crypt  %s\n", result == 0 ? "PASS" : "FAIL");
    if(result)
    {
        printf("Expected value: \n");
        for(i = 0; i < pt_size; i++)
        {
            printf("%02x ", crypt[i]);
        }
        printf("\nActual result: \n");
        for(i = 0; i < pt_size; i++)
        {
            printf("%02x ", crypt_buf[i]);
        }
        printf("\n\n");
    }
    
    result = 0;
    for(i = 0; i < tag_size; i++)
    {
        if(tag[i] != tag_buf[i])
        {
            result = 1;
            ret = 1;
            break;
        }
    }
    printf("t3 aes_gcm encrypt tag    %s\n", result == 0 ? "PASS" : "FAIL");
    if(result)
    {
        printf("Expected value: \n");
        for(i = 0; i < tag_size; i++)
        {
            printf("%02x ", tag[i]);
        }
        printf("\nActual result: \n");
        for(i = 0; i < tag_size; i++)
        {
            printf("%02x ", tag_buf[i]);
        }
        printf("\n\n");
    }
    
    return ret;
}
