/*
 * Adapted from wpa_supplicant code:
 * 
 * Galois/Counter Mode (GCM) and GMAC with AES
 *
 * Copyright (c) 2012, Jouni Malinen <j@w1.fi>
 *
 * This software may be distributed under the terms of the BSD license.
 * See README for more details.
 */
#include <stddef.h>
#include "aes_internal.h"
#include "aes-gcm.h"
#include "aes.h"

static struct AES_ctx ctx;

static inline void memset(uint8_t *s, uint8_t v, uint8_t n)
{
	uint8_t i;

	for(i = 0; i < n; i++)
	{
		s[i] = v;
	}
}

static inline void memcpy(uint8_t *d, const uint8_t *s, uint8_t n)
{
	uint8_t i;

	for(i = 0; i < n; i++)
	{
		d[i] = s[i];
	}
}

static void inc32(uint8_t *block)
{
	uint32_t val;
	val = AES_GET_BE32(block + AES_BLOCK_SIZE - 4);
	val++;
	AES_PUT_BE32(block + AES_BLOCK_SIZE - 4, val);
}

static void xor_block(uint8_t *dst, const uint8_t *src)
{
	uint8_t i;

	for(i = 0; i < 16; i++)
	{
		dst[i] ^= src[i];
	}
}

static void shift_right_block(uint8_t *v)
{
	uint32_t val;

	val = AES_GET_BE32(v + 12);
	val >>= 1;
	if (v[11] & 0x01)
		val |= 0x80000000;
	AES_PUT_BE32(v + 12, val);

	val = AES_GET_BE32(v + 8);
	val >>= 1;
	if (v[7] & 0x01)
		val |= 0x80000000;
	AES_PUT_BE32(v + 8, val);

	val = AES_GET_BE32(v + 4);
	val >>= 1;
	if (v[3] & 0x01)
		val |= 0x80000000;
	AES_PUT_BE32(v + 4, val);

	val = AES_GET_BE32(v);
	val >>= 1;
	AES_PUT_BE32(v, val);
}

/* Multiplication in GF(2^128) */
static void gf_mult(const uint8_t *x, const uint8_t *y, uint8_t *z)
{
	uint8_t v[16];
	int32_t i, j;

	memset(z, 0, 16); /* Z_0 = 0^128 */
	memcpy(v, y, 16); /* V_0 = Y */

	for(i = 0; i < 16; i++)
	{
		for(j = 0; j < 8; j++)
		{
			if(x[i] & 1 << (7 - j))
			{
				/* Z_(i + 1) = Z_i XOR V_i */
				xor_block(z, v);
			}
			else
			{
				/* Z_(i + 1) = Z_i */
			}

			if(v[15] & 0x01)
			{
				/* V_(i + 1) = (V_i >> 1) XOR R */
				shift_right_block(v);
				/* R = 11100001 || 0^120 */
				v[0] ^= 0xe1;
			}
			else
			{
				/* V_(i + 1) = V_i >> 1 */
				shift_right_block(v);
			}
		}
	}
}

inline static void ghash_start(uint8_t *y)
{
	/* Y_0 = 0^128 */
	memset(y, 0, 16);
}

static void ghash(const uint8_t *h, const uint8_t *x, uint8_t xlen, uint8_t *y)
{
	uint8_t m, i;
	const uint8_t *xpos = x;
	uint8_t tmp[16];

	m = xlen / 16;

	for(i = 0; i < m; i++)
	{
		/* Y_i = (Y^(i-1) XOR X_i) dot H */
		xor_block(y, xpos);
		xpos += 16;

		/* dot operation:
		 * multiplication operation for binary Galois (finite) field of
		 * 2^128 elements */
		gf_mult(y, h, tmp);
		memcpy(y, tmp, 16);
	}

	if(x + xlen > xpos)
	{
		/* Add zero padded last block */
		uint8_t last = x + xlen - xpos;
		memcpy(tmp, xpos, last);
		memset(tmp + last, 0, sizeof(tmp) - last);

		/* Y_i = (Y^(i-1) XOR X_i) dot H */
		xor_block(y, tmp);

		/* dot operation:
		 * multiplication operation for binary Galois (finite) field of
		 * 2^128 elements */
		gf_mult(y, h, tmp);
		memcpy(y, tmp, 16);
	}

	/* Return Y_m */
}


static void aes_gctr(const uint8_t *icb, const uint8_t *x, uint8_t xlen, uint8_t *y)
{
	uint8_t i, n, last;
	uint8_t cb[AES_BLOCK_SIZE], tmp[AES_BLOCK_SIZE];
	const uint8_t *xpos = x;
	uint8_t *ypos = y;

	if(xlen == 0)
	{
		return;
	}

	n = xlen / 16;

	memcpy(cb, icb, AES_BLOCK_SIZE);

	/* Full blocks */
	for (i = 0; i < n; i++)
	{
		memcpy(ypos, cb, AES_BLOCK_SIZE);
		AES_ECB_encrypt(&ctx, ypos);
		xor_block(ypos, xpos);
		xpos += AES_BLOCK_SIZE;
		ypos += AES_BLOCK_SIZE;
		inc32(cb);
	}

	last = x + xlen - xpos;
	if (last)
	{
		/* Last, partial block */
		memcpy(tmp, cb, AES_BLOCK_SIZE);
		AES_ECB_encrypt(&ctx, tmp);
		for (i = 0; i < last; i++)
		{
			*ypos++ = *xpos++ ^ tmp[i];
		}
	}
}

static int32_t aes_gcm_init_hash_subkey(const uint8_t *key, uint8_t *H)
{
	AES_init_ctx(&ctx, key);

	/* Generate hash subkey H = AES_K(0^128) */
	memset(H, 0, AES_BLOCK_SIZE);
	AES_ECB_encrypt(&ctx, H);

	return 0;
}


static void aes_gcm_prepare_j0(const uint8_t *iv, uint8_t iv_len, const uint8_t *H, uint8_t *J0)
{
	uint8_t len_buf[16];

	if(iv_len == 12)
	{
		/* Prepare block J_0 = IV || 0^31 || 1 [len(IV) = 96] */
		memcpy(J0, iv, iv_len);
		memset(J0 + iv_len, 0, AES_BLOCK_SIZE - iv_len);
		J0[AES_BLOCK_SIZE - 1] = 0x01;
	}
	else
	{
		/*
		 * s = 128 * ceil(len(IV)/128) - len(IV)
		 * J_0 = GHASH_H(IV || 0^(s+64) || [len(IV)]_64)
		 */
		ghash_start(J0);
		ghash(H, iv, iv_len, J0);
		AES_PUT_BE64(len_buf, 0);
		AES_PUT_BE64(len_buf + 8, iv_len * 8);
		ghash(H, len_buf, sizeof(len_buf), J0);
	}
}


static void aes_gcm_gctr(const uint8_t *J0, const uint8_t *in, uint8_t len, uint8_t *out)
{
	uint8_t J0inc[AES_BLOCK_SIZE];

	if(len == 0)
	{
		return;
	}

	memcpy(J0inc, J0, AES_BLOCK_SIZE);
	inc32(J0inc);
	aes_gctr(J0inc, in, len, out);
}

static void aes_gcm_ghash(const uint8_t *H, const uint8_t *aad, uint8_t aad_len, const uint8_t *crypt, uint8_t crypt_len, uint8_t *S)
{
	uint8_t len_buf[16];

	/*
	 * u = 128 * ceil[len(C)/128] - len(C)
	 * v = 128 * ceil[len(A)/128] - len(A)
	 * S = GHASH_H(A || 0^v || C || 0^u || [len(A)]64 || [len(C)]64)
	 * (i.e., zero padded to block size A || C and lengths of each in bits)
	 */
	ghash_start(S);
	ghash(H, aad, aad_len, S);
	ghash(H, crypt, crypt_len, S);
	AES_PUT_BE64(len_buf, aad_len * 8);
	AES_PUT_BE64(len_buf + 8, crypt_len * 8);
	ghash(H, len_buf, sizeof(len_buf), S);

	//aes_hexdump_key(MSG_EXCESSIVE, "S = GHASH_H(...)", S, 16);
}

/**
 * aes_gcm_encode - GCM-AE_K(IV, P, A)
 */
int32_t aes_gcm_encode(const uint8_t *key, const uint8_t *iv, uint8_t iv_len, const uint8_t *plain, uint8_t plain_len, const uint8_t *aad, uint8_t aad_len, uint8_t *crypt, uint8_t *tag)
{
	uint8_t H[AES_BLOCK_SIZE];
	uint8_t J0[AES_BLOCK_SIZE];
	uint8_t S[16];

	if(aes_gcm_init_hash_subkey(key, H))
	{
		return -1;
	}

	aes_gcm_prepare_j0(iv, iv_len, H, J0);

	/* C = GCTR_K(inc_32(J_0), P) */
	aes_gcm_gctr(J0, plain, plain_len, crypt);

	aes_gcm_ghash(H, aad, aad_len, crypt, plain_len, S);

	/* T = MSB_t(GCTR_K(J_0, S)) */
	aes_gctr(J0, S, sizeof(S), tag);

	/* Return (C, T) */

	return 0;
}