#ifndef CLEA_H__
#define CLEA_H__

#include <stdint.h>

#define SK_L_SIZE (51)

/* Permanent location secret key, this key is not defined in the clea library itself,
   it should be defined outside as it depends on the hardware specific storage location */
extern uint8_t SK_L[SK_L_SIZE];

#define PUBLIC_KEY_SIZE (64)

/* Public key of the authority in charge of the backend server
   i.e. point on the SECP256R1 curve in the "uncompressed" form
   this key should be 512-bit-long */
extern uint8_t PK_SA[PUBLIC_KEY_SIZE];

/* Public key of the authority in charge of the manual contact tracing
   i.e. point on the SECP256R1 curve in the "uncompressed" form
   this key should be 512-bit-long */
extern uint8_t PK_MCTA[PUBLIC_KEY_SIZE];

/* QRCode generator configuration */
typedef struct
{
    uint8_t staff;                // 1 bit
    uint8_t locContactMsgPresent; // 1 bit
    uint8_t CRIexp;               // 5 bits: exponent of renewal interval un minutes: RI = 2**CRIexp minutes
    uint8_t venueType;            // 5 bits
    uint8_t venueCategory1;       // 4 bits
    uint8_t venueCategory2;       // 4 bits
    uint8_t periodDuration;       // 8 bits: period duration in hours

    // Location contact
    uint8_t locationPhone[8]; // 60 bits, 4 bits per digit, pad with 0xF (=> 15 digits max)
    uint8_t locationRegion;   // 8 bits: coarse grain geographical information for the location
    uint8_t locationPin[3];   // 4 bits per digit (=> 6 digits)
} clea_conf_t;

extern clea_conf_t clea_conf;

#define LSP_BASE64_SIZE (237)
extern uint8_t LSP_base64[LSP_BASE64_SIZE];

void clea_init(void);

int32_t clea_start_new_period(uint8_t ptr_LTId[16],  uint32_t *ptr_ct_periodStart, uint32_t *ptr_t_qrStart);

int32_t clea_renew_qrcode(uint32_t *ptr_ct_periodStart, uint32_t *ptr_t_qrStart);

#endif