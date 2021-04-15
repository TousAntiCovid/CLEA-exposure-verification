#include <stdio.h>
#include <stdlib.h>
#include <stddef.h>
#include <stdint.h>
#include <stdbool.h>
#include <unistd.h>
#include <sys/time.h>
#include "VG/openvg.h"
#include "VG/vgu.h"
#include "fontinfo.h"
#include "shapes.h"
#include "qrcodegen.h"
#include "external_deps/time.h"
#include "clea.h"

uint8_t SK_L[SK_L_SIZE] = {17, 50, 178, 117, 142, 93, 11, 228, 16, 198, 218, 172, 28, 203, 30, 149, 105, 157, 195, 247, 12, 226, 62, 13, 255, 126, 84, 96, 87, 163, 80, 97, 213, 211, 66, 20, 248, 127, 9, 64, 84, 127, 135, 165, 127, 171, 14, 78, 34, 229, 223};
uint8_t PK_HA[PUBLIC_KEY_SIZE] = {0xbc, 0x05, 0x51, 0x56, 0x7e, 0xcc, 0x32, 0x77, 0x99, 0x8f, 0x4b, 0xa9, 0xaf, 0x0c, 0xa6, 0x35, 0xdd, 0xe3, 0x6b, 0xd2, 0x01, 0x55, 0x95, 0xbc, 0xab, 0xa2, 0x74, 0x0c, 0xcd, 0xc0, 0x6e, 0x7f, 0x78, 0x0d, 0x9e, 0xd2, 0xf4, 0x7f, 0xbc, 0xc5, 0xb4, 0xb4, 0x65, 0x49, 0x73, 0x3e, 0x2f, 0x9f, 0xb0, 0xf5, 0xc5, 0x86, 0x7a, 0x08, 0xec, 0xf6, 0x45, 0x16, 0xf9, 0x8d, 0x4c, 0x8a, 0x7e, 0xd5};
uint8_t PK_MCTA[PUBLIC_KEY_SIZE] = {0xbc, 0x05, 0x51, 0x56, 0x7e, 0xcc, 0x32, 0x77, 0x99, 0x8f, 0x4b, 0xa9, 0xaf, 0x0c, 0xa6, 0x35, 0xdd, 0xe3, 0x6b, 0xd2, 0x01, 0x55, 0x95, 0xbc, 0xab, 0xa2, 0x74, 0x0c, 0xcd, 0xc0, 0x6e, 0x7f, 0x78, 0x0d, 0x9e, 0xd2, 0xf4, 0x7f, 0xbc, 0xc5, 0xb4, 0xb4, 0x65, 0x49, 0x73, 0x3e, 0x2f, 0x9f, 0xb0, 0xf5, 0xc5, 0x86, 0x7a, 0x08, 0xec, 0xf6, 0x45, 0x16, 0xf9, 0x8d, 0x4c, 0x8a, 0x7e, 0xd5};

clea_conf_t clea_conf = 
{
    .staff                = 0,
    .locContactMsgPresent = 1,
    .CRIexp               = 7,  // ~ 2min renewal interval
    .venueType            = 10,
    .venueCategory1       = 0,
    .venueCategory2       = 0,
    .periodDuration       = 1,  // 1 hour
    .locationPhone        = {0x33, 0x80, 0x01, 0x30, 0x00, 0x0F, 0xFF, 0xFF}, // +33 800 130 000
    .locationPin          = {0xDE, 0xAD, 0xBE, 0xEF} // "DEADBEEF"
};

uint8_t LSP_base64[LSP_BASE64_SIZE];

void display_qrcode(int width, int height)
{
	uint8_t content[24 + sizeof(LSP_base64)] = "http://tac.gouv.fr?v=0#";
	uint8_t qr[qrcodegen_BUFFER_LEN_MAX];
	uint8_t tempBuffer[qrcodegen_BUFFER_LEN_MAX];
	bool ok;

    for(uint8_t i = 0; i < sizeof(LSP_base64); i++)
    {
        content[19 + i] = LSP_base64[i];
    }
	content[sizeof(content) - 1] = 0;

	ok = qrcodegen_encodeText(content, tempBuffer, qr, qrcodegen_Ecc_MEDIUM, qrcodegen_VERSION_MIN, qrcodegen_VERSION_MAX, qrcodegen_Mask_AUTO, true);

	Start(width, height);

	Background(255, 255, 255);

	if(ok)
	{
		int size = qrcodegen_getSize(qr);
		
		int cell_size = height / (size + 2);

		int ox = (width - cell_size * size) / 2;
		int oy = (height - cell_size * size) / 2;

		for (int y = 0; y < size; y++)
		{
			for (int x = 0; x < size; x++)
			{
				if(qrcodegen_getModule(qr, x, y))
				{
					if(x < size / 2)
					{
						Fill(5, 0, 140, 1);
					}
					else
					{
						Fill(235, 0, 36, 1);
					}
				}
				else
				{
					Fill(255, 255, 255, 1);
				}
				Rect(ox + x * cell_size, oy + y * cell_size, cell_size, cell_size);
			}
		}
	}
	else
	{
		printf("Error generating QRCode\n");
	}

	End();
}

void get_time(uint32_t *hour, uint32_t *minute)
{
	struct timeval tv;

	gettimeofday(&tv, NULL);

	*minute = (tv.tv_sec / 60) % 60;
	*hour = (tv.tv_sec / 3600) % 24;
}

void main()
{
	uint32_t last_hour, last_minute;
	uint32_t hour, minute;
	int width, height;

    clea_init();

	init(&width, &height);

	get_time(&hour, &minute);
	last_hour = hour - clea_conf.periodDuration;
	last_minute = minute;
	clea_start_new_period(NULL, NULL, NULL);
	display_qrcode(width, height);
			
	printf("Started at %02d:%02d\n", hour, minute);

	while(true)
	{
		get_time(&hour, &minute);

		if((minute == 0) && (((hour - last_hour + 24) % 24) >= clea_conf.periodDuration))
		{
			last_hour = hour;
			last_minute = minute;
			clea_start_new_period(NULL, NULL, NULL);
			display_qrcode(width, height);

			printf("New period at %02d:%02d\n", hour, minute);
		}
		else if(((minute - last_minute + 60) % 60) >= (1 << clea_conf.CRIexp) / 60)
		{
			last_minute = minute;
			clea_renew_qrcode(NULL, NULL);
			display_qrcode(width, height);
			
			printf("Renewed QRCode at %02d:%02d\n", hour, minute);
		}

		sleep(60);
	}

	finish();
	exit(0);
}
