#include "ecies.h"
#include "hash/sha256.h"
#include "libec.h"
#include "aes-gcm.h"

static const ec_str_params *secp256r1;
static ec_params curve;
static prj_pt Q;

static void compress_point(prj_pt *P, uint8_t *buf);
static void kdf(uint8_t *in, uint8_t len, uint8_t *out, uint8_t out_len);

void ecies_init(void)
{
    // Initialize ECIES to use SECP256R1
    secp256r1 = ec_get_curve_params_by_type(SECP256R1);
	import_params(&curve, secp256r1);
}
#include <stdio.h>
int32_t ecies_encode(uint8_t *key, uint8_t *msg, uint8_t clear_len, uint8_t crypt_len)
{
    #define KDF_INPUT_SIZE (2 * (32 + 1) + 4) // 2 compressed points + 1 32-bit integer
    nn r;
    prj_pt C0, S;
    uint8_t buf[255]; // Must contain at least crypt_len bytes
    uint8_t K[32]; // Secret 256-bit key
    uint8_t iv[12] = {0xf0, 0xf1, 0xf2, 0xf3, 0xf4, 0xf5, 0xf6, 0xf7, 0xf8, 0xf9, 0xfa, 0xfb}; // Constant IV as each key is different for every encoding
    uint8_t i;

    // Load key
    prj_pt_init(&Q, &(curve.ec_curve));
    
    // Import key from buffer and ensure loading went well
    if(prj_pt_import_from_aff_buf(&Q, key, 2 * BYTECEIL(curve.ec_fp.p_bitlen), &(curve.ec_curve)))
    {
        return -1;
    }

    // Ensure the key is valid
    if(!prj_pt_is_on_curve(&Q))
    {
        return -2;
    }

    // Generate a random number r between 1 and base point order - 1 and C0 = rG
    nn_init(&r, 0);
	prj_pt_init(&C0, &(curve.ec_curve));

    // Draw a random number and ensure the associated C0
    // is not the point at infinity
    do
    {
        if(nn_get_random_mod(&r, &(curve.ec_gen_order)))
        {
            return -3;
        }

        // Compute C0 = rG
        prj_pt_mul_monty(&C0, &r, &(curve.ec_gen));
        prj_pt_unique(&C0, &C0);
    }
    while(prj_pt_iszero(&C0));

    // Compute S = rQ
	prj_pt_init(&S, &(curve.ec_curve));
    prj_pt_mul_monty(&S, &r, &Q);
	prj_pt_unique(&S, &S);

    // Compute K = KDF(E(C0) | E(S))
    compress_point(&C0, buf);
    compress_point(&S, &(buf[33]));
    kdf(buf, KDF_INPUT_SIZE, K, sizeof(K));

    // append compressed C0 at the end of the output buffer
    for(i = 0; i < 33; i++)
    {
        msg[clear_len + crypt_len + 16 + i] = buf[i];
    }
    
    // Encrypt message using AES256 and K as key
    if(aes_gcm_encode(K, iv, sizeof(iv), &(msg[clear_len]), crypt_len, msg, clear_len, buf, &(msg[clear_len + crypt_len])))
    {
        return -4;
    }
    
    // Copy encrypted data in the right place
    for(i = 0; i < crypt_len; i++)
    {
        msg[clear_len + i] = buf[i];
    }
    
    return 0;
}

static void compress_point(prj_pt *P, uint8_t *buf)
{
    uint8_t tmp[64];
    uint8_t i;

    prj_pt_export_to_aff_buf(P, tmp, 2 * BYTECEIL(curve.ec_fp.p_bitlen));

    for(i = 0; i < sizeof(tmp) / 2; i++)
    {
        buf[i + 1] = tmp[i];
    }

    // ANSI X9.62 encoding
    buf[0] = (tmp[sizeof(tmp) - 1] % 2) | 0x2;
}

static void kdf(uint8_t *in, uint8_t len, uint8_t *out, uint8_t out_len)
{
    uint8_t i;

    for(i = 0; i < out_len / SHA256_DIGEST_SIZE; i++)
    {
        in[len - 4] = i; // this is KDF1, change to i + 1 for KDF2
        in[len - 3] = 0;
        in[len - 2] = 0;
        in[len - 1] = 0;

        sha256(in, len, &(out[i * SHA256_DIGEST_SIZE]));
    }
}