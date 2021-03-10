#include <stdio.h>
#include "ecies.h"
#include "libec.h"
#include "test_util.h"

uint8_t priv_key[32];
uint8_t pub_key[2 * sizeof(priv_key)];

uint8_t random[32];

uint8_t out[110];
uint8_t clear_len = 17;
uint8_t crypt_len = 44;

int get_random(uint8_t *buf, uint16_t len)
{
    uint16_t i;

    for(i = 0; i < len; i++)
    {
        buf[i] = random[i];
    }

    return 0;
}

int main(int argc, char **argv)
{
    nn priv;
    prj_pt pub;
    const ec_str_params *secp256r1;
    ec_params curve;

    // Parse command line
    if(argc != 4)
    {
        printf("Usage: %s random priv_key data\n", argv[0]);
        return -1;
    }
    parse(argv[1], random);
    parse(argv[2], priv_key);
    parse(argv[3], out);

    // Initialize elliptic curve
    secp256r1 = ec_get_curve_params_by_type(SECP256R1);
	import_params(&curve, secp256r1);

    // Generate public key from private key
    nn_init_from_buf(&priv, priv_key, sizeof(priv_key));
    prj_pt_init(&pub, &(curve.ec_curve));
    prj_pt_mul_monty(&pub, &priv, &(curve.ec_gen));
	prj_pt_unique(&pub, &pub);
    prj_pt_export_to_aff_buf(&pub, pub_key, 2 * BYTECEIL(curve.ec_fp.p_bitlen));

    // Init
    ecies_init();

    // Encode
    ecies_encode(pub_key, out, clear_len, crypt_len);

    // Print output
    dump(out, sizeof(out));

    return 0;
}