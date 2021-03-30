#include <stdio.h>
#include "test_util.h"

uint8_t parse(char *string, uint8_t *array)
{
    uint8_t i = 0, k, c;

    while(*string)
    {
        array[i] = 0;

        for(k = 0; k < 2; k++)
        {
            c = (*string++) - '0';

            if(c >= 10)
            {
                c -= 'a' - '0' - 10;
            }

            array[i] |= c << (4 - 4 * k);
        }

        i++;
    }

    return i;
}

uint8_t parse_bcd(char *string, uint8_t *array, uint8_t size)
{
    uint8_t i = 0, c, k;

    while(*string)
    {
        if(i / 2 >= size)
        {
            return 1;
        }

        c = (*string++) - '0';

        if(i % 2 == 0)
        {
            array[i / 2] = (c << 4) | 0x0F;
        }
        else
        {
            array[i / 2] &= 0xF0 | c;
        }

        i++;
    }

    for(k = i / 2 + i%2; k < size; k++)
    {
        array[k] = 0xFF;
    }

    return 0;
}

void dump(uint8_t *buffer, uint8_t size)
{
    uint8_t i;

    for(i = 0; i < size; i++)
    {
        printf("%02x", buffer[i]);
    }
    printf("\n");
}