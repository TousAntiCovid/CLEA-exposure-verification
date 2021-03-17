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
#ifndef AES_INTERNAL_H__
#define AES_INTERNAL_H__

#include <stdint.h>

static inline uint16_t AES_GET_BE16(const uint8_t *a)
{
	return (a[0] << 8) | a[1];
}

static inline void AES_PUT_BE16(uint8_t *a, uint16_t val)
{
	a[0] = val >> 8;
	a[1] = val & 0xff;
}

static inline uint16_t AES_GET_LE16(const uint8_t *a)
{
	return (a[1] << 8) | a[0];
}

static inline void AES_PUT_LE16(uint8_t *a, uint16_t val)
{
	a[1] = val >> 8;
	a[0] = val & 0xff;
}

static inline uint32_t AES_GET_BE24(const uint8_t *a)
{
	return (a[0] << 16) | (a[1] << 8) | a[2];
}

static inline void AES_PUT_BE24(uint8_t *a, uint32_t val)
{
	a[0] = (val >> 16) & 0xff;
	a[1] = (val >> 8) & 0xff;
	a[2] = val & 0xff;
}

static inline uint32_t AES_GET_BE32(const uint8_t *a)
{
	return (a[0] << 24) | (a[1] << 16) | (a[2] << 8) | a[3];
}

static inline void AES_PUT_BE32(uint8_t *a, uint32_t val)
{
	a[0] = (val >> 24) & 0xff;
	a[1] = (val >> 16) & 0xff;
	a[2] = (val >> 8) & 0xff;
	a[3] = val & 0xff;
}

static inline uint32_t AES_GET_LE32(const uint8_t *a)
{
	return (a[3] << 24) | (a[2] << 16) | (a[1] << 8) | a[0];
}

static inline void AES_PUT_LE32(uint8_t *a, uint32_t val)
{
	a[3] = (val >> 24) & 0xff;
	a[2] = (val >> 16) & 0xff;
	a[1] = (val >> 8) & 0xff;
	a[0] = val & 0xff;
}

static inline uint64_t AES_GET_BE64(const uint8_t *a)
{
	return (((uint64_t) a[0]) << 56) | (((uint64_t) a[1]) << 48) |
            (((uint64_t) a[2]) << 40) | (((uint64_t) a[3]) << 32) |
            (((uint64_t) a[4]) << 24) | (((uint64_t) a[5]) << 16) |
            (((uint64_t) a[6]) << 8) | ((uint64_t) a[7]);
}

static inline void AES_PUT_BE64(uint8_t *a, uint64_t val)
{
	a[0] = val >> 56;
	a[1] = val >> 48;
	a[2] = val >> 40;
	a[3] = val >> 32;
	a[4] = val >> 24;
	a[5] = val >> 16;
	a[6] = val >> 8;
	a[7] = val & 0xff;
}

static inline uint64_t AES_GET_LE64(const uint8_t *a)
{
	return (((uint64_t) a[7]) << 56) | (((uint64_t) a[6]) << 48) |
            (((uint64_t) a[5]) << 40) | (((uint64_t) a[4]) << 32) |
            (((uint64_t) a[3]) << 24) | (((uint64_t) a[2]) << 16) |
            (((uint64_t) a[1]) << 8) | ((uint64_t) a[0]);
}

static inline void AES_PUT_LE64(uint8_t *a, uint64_t val)
{
	a[7] = val >> 56;
	a[6] = val >> 48;
	a[5] = val >> 40;
	a[4] = val >> 32;
	a[3] = val >> 24;
	a[2] = val >> 16;
	a[1] = val >> 8;
	a[0] = val & 0xff;
}

#endif