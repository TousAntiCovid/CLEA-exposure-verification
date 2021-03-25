#include <stdbool.h>
#include "hash/sha256.h"
#include "external_deps/time.h"
#include "hmac-sha256.h"
#include "ecies.h"
#include "clea.h"

static uint32_t t_periodStart;            // Starting time of the period, NTP UTC timestamp limited to the 32-bit seconds field (starting at a round hour)
static uint8_t LTKey[SHA256_DIGEST_SIZE]; // Temporary location secret key
static uint8_t LTId[16];                  // Temporary location public universally unique Identifier
static const uint8_t version = 0;
static const uint8_t qrType = 0;

static void compute_TLKey(void);
static void to_base64(uint8_t *in, uint8_t *out, uint8_t n);
static uint32_t get_ntp_utc(bool round);

void clea_init(void)
{
    ecies_init();
}

int32_t clea_start_new_period(uint8_t ptr_LTId[16], uint32_t *ptr_ct_periodStart, uint32_t *ptr_t_qrStart)
{
    t_periodStart = get_ntp_utc(true);

    compute_TLKey();

    // Compute LTId
    hmac_sha256_128(LTKey, sizeof(LTKey), (uint8_t *)"1", 1, LTId);

    // set LTId return for test purpose
    if (ptr_LTId != NULL)
    {
        for (int i = 0; i < 16; i++)
        {
            ptr_LTId[i] = LTId[i];
        }
    }

    return clea_renew_qrcode(ptr_ct_periodStart, ptr_t_qrStart);
}

int32_t clea_renew_qrcode(uint32_t *ptr_ct_periodStart, uint32_t *ptr_t_qrStart)
{
#define CLEAR_HEADER_SIZE (1 + sizeof(LTId))
#define MSG_SIZE (44)
#define LOC_MSG_SIZE (16)
#define TAG_AND_KEY (49)

    static uint8_t LSP[CLEAR_HEADER_SIZE + MSG_SIZE + LOC_MSG_SIZE + 2 * TAG_AND_KEY];
    uint8_t loc_msg_start, i, cpt = 0;
    clea_conf_t *c = &clea_conf;
    uint32_t t_qrStart = get_ntp_utc(false);
    uint32_t ct_periodStart = t_periodStart / 3600;
    uint32_t r;

    LSP[cpt++] = ((version & 0x7) << 5) | ((qrType & 0x7) << 2); // Implicit padding with zeroes

    for (i = 0; i < sizeof(LTId); i++)
    {
        LSP[cpt++] = LTId[i];
    }

    LSP[cpt++] = (c->staff & 0x1) << 7 | ((c->locContactMsgPresent & 0x1) << 6) | ((c->countryCode & 0xFC0) >> 6);
    LSP[cpt++] = ((c->countryCode & 0x3F) << 2) | ((c->CRIexp & 0x18) >> 3);
    LSP[cpt++] = ((c->CRIexp & 0x7) << 5) | (c->venueType & 0x1F);
    LSP[cpt++] = ((c->venueCategory1 & 0xF) << 4) | (c->venueCategory2 & 0xF);
    LSP[cpt++] = c->periodDuration;
    LSP[cpt++] = (ct_periodStart >> 16) & 0xFF; // multi-byte numbers are stored with the big endian convention as required by the specification
    LSP[cpt++] = (ct_periodStart >> 8) & 0xFF;
    LSP[cpt++] = ct_periodStart & 0xFF;
    LSP[cpt++] = (t_qrStart >> 24) & 0xFF;
    LSP[cpt++] = (t_qrStart >> 16) & 0xFF;
    LSP[cpt++] = (t_qrStart >> 8) & 0xFF;
    LSP[cpt++] = t_qrStart & 0xFF;

    for (i = 0; i < sizeof(LTKey); i++)
    {
        LSP[cpt++] = LTKey[i];
    }

    // Add location contact block if present
    if (c->locContactMsgPresent)
    {
        loc_msg_start = cpt;
        // Encode 56 first bits (7 bytes + 4 bits)
        for (i = 0; i < sizeof(c->locationPhone) - 1; i++)
        {
            LSP[cpt++] = c->locationPhone[i];
        }
        // Encode the last 4 bits + 4 bits of padding
        LSP[cpt++] = c->locationPhone[sizeof(c->locationPhone)-1] & 0xF0;

        LSP[cpt++] = c->locationRegion & 0xFF;

        for (i = 0; i < sizeof(c->locationPin); i++)
        {
            LSP[cpt++] = c->locationPin[i];
        }

        LSP[cpt++] = (t_periodStart >> 24) & 0xFF;
        LSP[cpt++] = (t_periodStart >> 16) & 0xFF;
        LSP[cpt++] = (t_periodStart >> 8) & 0xFF;
        LSP[cpt++] = t_periodStart & 0xFF;

        // Encode location contact block first
        r = ecies_encode(PK_MCTA, &(LSP[loc_msg_start]), 0, LOC_MSG_SIZE);

        if (r)
        {
            return r;
        }

        cpt += TAG_AND_KEY;
    }

    // Encode the global message
    r = ecies_encode(PK_SA, LSP, CLEAR_HEADER_SIZE, cpt - CLEAR_HEADER_SIZE);

    if (r)
    {
        return r;
    }

    to_base64(LSP, LSP_base64, cpt + TAG_AND_KEY);

    // set parameters return for test purpose
    if (ptr_t_qrStart != NULL)
    {
        *ptr_t_qrStart = t_qrStart;
    }
    if (ptr_ct_periodStart != NULL)
    {
        *ptr_ct_periodStart = ct_periodStart;
    }

    return 0;
}

static void compute_TLKey(void)
{
    static uint8_t buffer[64]; // 512-bit input data buffer
    uint8_t i;

    /* Concatenate secret key and starting time of the period */
    for (i = 0; i < sizeof(SK_L); i++)
    {
        buffer[i] = SK_L[i];
    }

    buffer[i++] = (t_periodStart >> 24) & 0xFF;
    buffer[i++] = (t_periodStart >> 16) & 0xFF;
    buffer[i++] = (t_periodStart >> 8) & 0xFF;
    buffer[i] = t_periodStart;

    // Update temporary location secret key
    sha256(buffer, sizeof(buffer), LTKey);
}

/*
 * Base64 encoding based on Jouni Malinen code
 * http://web.mit.edu/freebsd/head/contrib/wpa/src/utils/base64.c
 */
static void to_base64(uint8_t *in, uint8_t *out, uint8_t n)
{
    static const uint8_t base64_table[65] = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/";
    uint8_t *pos;
    const uint8_t *end, *start;

    end = in + n;
    start = in;
    pos = out;

    while (end - start >= 3)
    {
        *pos++ = base64_table[start[0] >> 2];
        *pos++ = base64_table[((start[0] & 0x03) << 4) | (start[1] >> 4)];
        *pos++ = base64_table[((start[1] & 0x0f) << 2) | (start[2] >> 6)];
        *pos++ = base64_table[start[2] & 0x3f];
        start += 3;
    }

    if (end - start)
    {
        *pos++ = base64_table[start[0] >> 2];

        if (end - start == 1)
        {
            *pos++ = base64_table[(start[0] & 0x03) << 4];
            *pos++ = '=';
        }
        else
        {
            *pos++ = base64_table[((start[0] & 0x03) << 4) | (start[1] >> 4)];
            *pos++ = base64_table[(start[1] & 0x0f) << 2];
        }
        *pos++ = '=';
    }

    // Add terminal zero to string
    *pos = 0;
}

uint32_t get_ntp_utc(bool round)
{
#define ONE_HOUR_IN_MS 3600000

    uint64_t t;
    uint32_t rem, th;

    get_ms_time(&t);

    if (round)
    {
        th = t / ONE_HOUR_IN_MS;  // Number of hours since the epoch
        rem = t % ONE_HOUR_IN_MS; // Number of ms since the last round hour

        // Round the hour, i.e. if we are closer to the next round
        // hour than the last one, round to the next hour
        if (rem > ONE_HOUR_IN_MS / 2)
        {
            th++;
        }

        t = th * 3600; // Convert h in s
    }
    else
    {
        t /= 1000; // Convert ms in s
    }

    // Convert to hour and add the shift from UNIX epoch to NTP UTC
    return t + 2208988800;
}