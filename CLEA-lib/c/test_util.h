#ifndef TEST_UTIL_H__
#define TEST_UTIL_H__

#include <stdint.h>

uint8_t parse(char *string, uint8_t *array);
uint8_t parse_bcd(char *string, uint8_t *array, uint8_t size);
void dump(uint8_t *buffer, uint8_t size);

#endif