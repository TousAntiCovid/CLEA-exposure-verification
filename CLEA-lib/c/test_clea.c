#include <stdio.h>
#include <unistd.h>
#include <stdlib.h>
#include <string.h>
#include "clea.h"
#include "test_util.h"

uint8_t SK_L[SK_L_SIZE];
uint8_t PK_SA[PUBLIC_KEY_SIZE];
uint8_t PK_MCTA[PUBLIC_KEY_SIZE];

clea_conf_t clea_conf;
uint8_t LSP_base64[LSP_BASE64_SIZE];

void print_qrcode()
{
    for (uint8_t i = 0; i < sizeof(LSP_base64); i++)
    {
        if (LSP_base64[i] == 0)
            break;
        printf("%c", LSP_base64[i]);
    }
}

void print_uuid(uint8_t uuid[16])
{
    printf(" %02x%02x%02x%02x-%02x%02x-%02x%02x-%02x%02x-%02x%02x%02x%02x%02x%02x",\
    uuid[0], uuid[1], uuid[2], uuid[3], uuid[4], uuid[5], uuid[6], uuid[7],\
    uuid[8], uuid[9], uuid[10], uuid[11], uuid[12], uuid[13], uuid[14], uuid[15]);
}

void usage(char *s, char *err)
{
    printf("ERROR: %s\n\n", err);
    printf("Usage: %s staff countryCode CRIexp venueType venueCategory1 venueCategory2 periodDuration PK_SA PK_MCTA SK_L [locationPhone locationPin]\n\n", s);
    printf("locationPhone: 15-digit-max international phone number\n");
    printf("locationPin: 8-digit-max pin code\n");
    exit(1);
}

#define USAGE(err)           \
    {                        \
        usage(argv[0], err); \
    }

int main(int argc, char *argv[])
{
    uint32_t r;
    uint32_t t_qrStart, ct_periodStart;
    uint8_t LTId[16];

    if ((argc == 13) || (argc == 11))
    {
        clea_conf.staff = atoi(argv[1]);
        clea_conf.locContactMsgPresent = (argc == 13);
        clea_conf.countryCode = atoi(argv[2]);
        clea_conf.CRIexp = atoi(argv[3]);
        clea_conf.venueType = atoi(argv[4]);
        clea_conf.venueCategory1 = atoi(argv[5]);
        clea_conf.venueCategory2 = atoi(argv[6]);
        clea_conf.periodDuration = atoi(argv[7]);
        // Skip "04" for uncompressed keys
        parse(&(argv[8][2]), PK_SA);
        parse(&(argv[9][2]), PK_MCTA);
        parse(argv[10], SK_L);

        if (clea_conf.locContactMsgPresent)
        {
            if (parse_bcd(argv[11], clea_conf.locationPhone, sizeof(clea_conf.locationPhone)))
            {
                USAGE("Too many digits in locationPhone");
            }
            if (parse_bcd(argv[12], clea_conf.locationPin, sizeof(clea_conf.locationPin)))
            {
                USAGE("Too many digits in locationPin");
            }
        }

        clea_init();
        r = clea_start_new_period(LTId, &ct_periodStart, &t_qrStart);

        if (r)
        {
            printf("Error while creating LSP: %d\n", r);
            exit(r);
        }
 
        print_qrcode();
        print_uuid(LTId);
        printf(" %lu %lu\n",ct_periodStart, t_qrStart);
    }
    else
    {
        USAGE("Wrong number of arguments");
    }

    return 0;
}