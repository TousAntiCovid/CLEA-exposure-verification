/* 
 * Copyright (C) Inria, 2021
 */

/*
global configuration
*/
let gConf = {
  LTKey: new Uint8Array(32),
  LTId: new Uint8Array(16),
  t_periodStart: getNtpUtc(true),
  ct_periodStart: 0,
  t_qrStart: 0
}


// Display debug information on console
let verbose = false;


/**
 * Start a new period to generate a new LSP computing LTKey (Temporary location
 * 256-bits secret key) and LTId (Temporary location public UUID)
 * 
 * use gConf (see above)
 * @param {conf} config user configuration
 *   conf = {SK_L, PK_SA, PK_MCTA, 
 *           staff, CRIexp, venueType, venueCategory1, venueCategory2,
 *           countryCode, periodDuration, locContactMsg}
 * 
 * @return
 */
export async function cleaStartNewPeriod(config) {
  gConf.t_periodStart = getNtpUtc(true);

  // Compute LTKey
  let tmp = new Uint8Array(64);
  tmp.set(config.SK_L, 0);
  tmp[60] = (gConf.t_periodStart >> 24) & 0xFF;
  tmp[61] = (gConf.t_periodStart >> 16) & 0xFF;
  tmp[62] = (gConf.t_periodStart >> 8) & 0xFF;
  tmp[63] = gConf.t_periodStart;
  gConf.LTKey = await crypto.subtle.digest("SHA-256", tmp)

  // Compute LTId
  let one = new Uint8Array(1);
  one[0] = 0x31; // '1'
  let key = await crypto.subtle.importKey(
    "raw",
    gConf.LTKey, {
      name: "HMAC",
      hash: "SHA-512"
    },
    true,
    ["sign"]
  );
  gConf.LTId = new Uint8Array(await crypto.subtle.sign("HMAC", key, one), 0, 16); // HMAC-SHA256-128
  
  return cleaRenewLSP(config);
}

/**
 * Generate a new locationSpecificPart (LSP)
 * 
 * use gConf (see above)
 * @param {conf} config user configuration
 *   conf = {SK_L, PK_SA, PK_MCTA, 
 *           staff, CRIexp, venueType, venueCategory1, venueCategory2,
 *           countryCode, periodDuration, locContactMsg}
 * 
 * @return {string} encoded LSP in Base64 format
 */
export async function cleaRenewLSP(config) {
  const CLEAR_HEADER_SIZE = 17;
  const MSG_SIZE = 44;
  const LOC_MSG_SIZE = 16;
  const TAG_AND_KEY = 49;

  gConf.t_qrStart = getNtpUtc(false);
  gConf.ct_periodStart = gConf.t_periodStart / 3600;

  let header = new Uint8Array(CLEAR_HEADER_SIZE);
  let msg = new Uint8Array(MSG_SIZE + (config.locContactMsg ? LOC_MSG_SIZE + TAG_AND_KEY : 0));
  let loc_msg = new Uint8Array(LOC_MSG_SIZE);

  // Fill header
  header[0] = ((config.version & 0x7) << 5) | ((config.qrType & 0x7) << 2);
  header.set(gConf.LTId, 1);

  // Fill message
  msg[0] = ((config.staff & 0x1) << 7) | (config.locContactMsg ? 0x40 : 0) | ((config.countryCode & 0xFC0) >>> 6);
  msg[1] = ((config.countryCode & 0x3F) << 2) | ((config.CRIexp & 0x18) >> 3);
  msg[2] = ((config.CRIexp & 0x7) << 5) | (config.venueType & 0x1F);
  msg[3] = ((config.venueCategory1 & 0xF) << 4) | (config.venueCategory2 & 0xF);
  msg[4] = config.periodDuration;
  msg[5] = (gConf.ct_periodStart >> 16) & 0xFF; // multi-byte numbers are stored with the big endian convention as required by the specification
  msg[6] = (gConf.ct_periodStart >> 8) & 0xFF;
  msg[7] = gConf.ct_periodStart & 0xFF;
  msg[8] = (gConf.t_qrStart >> 24) & 0xFF;
  msg[9] = (gConf.t_qrStart >> 16) & 0xFF;
  msg[10] = (gConf.t_qrStart >> 8) & 0xFF;
  msg[11] = gConf.t_qrStart & 0xFF;
  msg.set(gConf.LTKey, 12);

  if (config.locContactMsg) {
    const phone = parseBcd(config.locContactMsg.locationPhone, 8);
    loc_msg.set(phone, 0);
    // Max digit is 15, the last 4 bits are set to 0 (pad)
    loc_msg[7] = loc_msg[7] & 0xF0;
    loc_msg[8] = config.locContactMsg.locationRegion & 0xFF;
    const pin = parseBcd(config.locContactMsg.locationPin, 3);
    loc_msg.set(pin, 9);
    let encrypted_loc_msg = await encrypt(new Uint8Array(0), loc_msg, config.PK_MCTA);
    msg.set(new Uint8Array(encrypted_loc_msg), 44);
  }

  let output = await encrypt(header, msg, config.PK_SA);
  
  // Convert output to Base64
  return btoa((Array.from(new Uint8Array(output))).map(ch => String.fromCharCode(ch)).join('')).replace(/\+/g, '-').replace(/\//g, '_');
}


/**
 * Encrypt, respecting CLEA protocol: | header | msg |
 * 
 * @param {Uint8Array} header associated data
 * @param {Uint8Array} msg    message to encrypt
 * @param {string} publicKey EC public key
 * 
 * @return {Uint8Array} data encrypted in binary format (bytes array)
 */
export async function encrypt(header, message, publicKey) {
  // Step 1: Import the publicKey Q
  let ECPubKey = await crypto.subtle.importKey(
    "raw",
    publicKey, {
      name: "ECDH",
      namedCurve: "P-256"
    },
    true,
    []
  );

  // Step 2: Generate a transient EC key pair (r, C0 = rG)
  let EcKeyPair = await crypto.subtle.generateKey({
      name: "ECDH",
      namedCurve: "P-256"
    },
    true,
    ["deriveKey", "deriveBits"]
  );

  // Step 3: Export C0 compressed
  const C0 = await crypto.subtle.exportKey(
    "raw",
    EcKeyPair.publicKey
  );

  let C0_Q = ecdhRawPubKeyCompressed(new Uint8Array(C0));

  if (verbose) printBuf("C0", C0_Q);

  // Step 4: Generate a shared secret (S = rQ)
  let S = await crypto.subtle.deriveBits({
      name: "ECDH",
      namedCurve: "P-256",
      public: ECPubKey
    },
    EcKeyPair.privateKey,
    256
  );

  if (verbose) printBuf("S", S);

  // Step5: Compute the AES encryption key K = KDF1(C0 | S)
  let tmp = concatBuffer(concatBuffer(C0_Q, S), new ArrayBuffer(4));
  let kdf1 = await crypto.subtle.digest("SHA-256", tmp)

  if (verbose) printBuf("C0 | S | 0x00000000", tmp);
  if (verbose) printBuf("KDF1", kdf1)

  let derivedKey = await crypto.subtle.importKey(
    "raw",
    kdf1,
    "AES-GCM",
    false,
    ["encrypt"]
  );

  // Step6: Encrypt the data
  const iv = new Uint8Array([0xf0, 0xf1, 0xf2, 0xf3, 0xf4, 0xf5, 0xf6, 0xf7, 0xf8, 0xf9, 0xfa, 0xfb]);

  let encoded = await crypto.subtle.encrypt({
      name: "AES-GCM",
      iv: iv,
      additionalData: header,
      tagLength: 128
    },
    derivedKey,
    message);

  // Step7: Concatenation
  const out_msg = concatBuffer(header, encoded);
  const output = concatBuffer(out_msg, C0_Q);

  if (verbose) printBuf("output", output);

  return output;
}

/**
 * Compress an elliptic Curve Public key
 *
 * @param {Uint8Array/Array Buffer} ec_raw_pubkey public key
 * @return {Uint8Array} public key compressed
 */
export function ecdhRawPubKeyCompressed(ec_raw_pubkey) {
  const u8full = new Uint8Array(ec_raw_pubkey)
  const len = u8full.byteLength
  const u8 = u8full.slice(0, 1 + len >>> 1) // drop `y`
  u8[0] = 0x2 | (u8full[len - 1] & 0x01) // encode sign of `y` in first bit
  return u8.buffer
}

/**
 * Get the NTP/UTC format time in seconds
 * 
 * @param {boolean} round hour rounded (multiple of 3600 sec) or not
 * @return {integer} NTP/UTC format time in seconds
 */
export function getNtpUtc(round) {
  const ONE_HOUR_IN_MS = 3600000;

  let t = Date.now();

  if (round) {
    let th = Math.floor(t / ONE_HOUR_IN_MS); // Number of hours since the epoch
    let rem = t % ONE_HOUR_IN_MS; // Number of ms since the last round hour

    // Round the hour, i.e. if we are closer to the next round
    // hour than the last one, round to the next hour
    if (rem > ONE_HOUR_IN_MS / 2) {
      th++;
    }

    t = th * 3600;
  } else {
    t /= 1000;
  }

  // Convert to hour and add the shift from UNIX epoch to NTP UTC
  return Math.floor(t) + 2208988800;
}

/**
 * Creates a new Uint8Array based on two different ArrayBuffers
 *
 * @param {ArrayBuffers} buf1 The first buffer.
 * @param {ArrayBuffers} buf2 The second buffer.
 * @return {Uint8Array} The new buffer created out of the two.
 */
export function concatBuffer(buf1, buf2) {
  let out = new Uint8Array(buf1.byteLength + buf2.byteLength);
  out.set(new Uint8Array(buf1), 0);
  out.set(new Uint8Array(buf2), buf1.byteLength);
  return out.buffer;
}

/**
 * Display on console an array bytes contents
 *
 * @param {string} name name of the bytes array to be displayed
 * @param {ArrayBuffers} buf array bytes
 */
function printBuf(name, buf) {
  console.log(name + " = " + Array.from(new Uint8Array(buf)).map(u => ("0" + u.toString(16)).slice(-2)).join(""))
}

/**
 * Convert a 64 bits int in a bytes array
 *
 * @param {integer} val to be converted
 * @return {Uint8Array} bytes array
 */
export function getInt64Bytes(val) {
  let bytes = [];
  let i = 8;
  do {
    bytes[--i] = val & (255);
    val = val >> 8;
  } while (i)
  return bytes;
}

/**
 * Parse a string composed by digits ([0..9])
 * to fill a bytes array storing as a set of 
 * 4-bit sub-fields that each contain a digit.
 * padding is done by 0xF
 *
 * @param {string} string composed by digits ([0..9])
 * @param {integer} size max number of digits to parse
 * @return {Uint8Array} bytes array
 */
export function parseBcd(string, size) {
  let i = 0,
    ip, k;
  let array = new Uint8Array(size);

  for (i = 0; i < string.length; i++) {
    let digit = string.charAt(i) - '0';
    if (i % 2 == 0) {
      ip = i / 2;
      array[ip] = (digit << 4) | 0x0F;
    } else {
      ip = (i / 2) - 0.5;
      array[ip] &= (0xF0 | digit);
    }
  }

  for (k = ip + 1; k < size; k++) {
    array[k] = 0xFF;
  }
  return array;
}