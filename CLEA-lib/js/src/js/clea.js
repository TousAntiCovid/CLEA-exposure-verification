/* 
 * Copyright (C) Inria, 2021
 */

export const COUNTRY_SPECIFIC_PREFIX = "https://tac.gouv.fr?v=0#";

// Display debug information on console
let verbose = false;

async function generateLocationTemporarySecretKey(SK_L, periodStartTime) {
  let tmp = new Uint8Array(64);
  tmp.set(SK_L, 0);
  tmp[60] = (periodStartTime >> 24) & 0xFF;
  tmp[61] = (periodStartTime >> 16) & 0xFF;
  tmp[62] = (periodStartTime >> 8) & 0xFF;
  tmp[63] = periodStartTime;
  return await crypto.subtle.digest("SHA-256", tmp);
}

async function generateLocationTemporaryId(LTKey) {
  let one = new Uint8Array(1);
  one[0] = 0x31; // '1'
  let key = await crypto.subtle.importKey(
    "raw",
    LTKey, {
      name: "HMAC",
      hash: "SHA-256"
    },
    true,
    ["sign"]
  );
  return new Uint8Array(await crypto.subtle.sign("HMAC", key, one), 0, 16); // HMAC-SHA256-128
}

export function newLocation(permanentLocationSecretKey, serverAuthorityPublicKey, manualContactTracingAuthorityPublicKey) {
  return {
    permanentLocationSecretKey,
    serverAuthorityPublicKey,
    manualContactTracingAuthorityPublicKey,
  };
}

export async function newLocationSpecificPart(location, venueType, venueCategory1, venueCategory2, periodDuration, periodStartTime, qrCodeRenewalIntervalExponentCompact, qrCodeValidityStartTime) {
  let locationTemporarySecretKey = await generateLocationTemporarySecretKey(location.permanentLocationSecretKey);
  let locationTemporaryPublicId = await generateLocationTemporaryId(locationTemporarySecretKey);
  return {
    locationTemporarySecretKey,
    locationTemporaryPublicId,
    venueType,
    venueCategory1,
    venueCategory2,
    periodDuration,
    periodStartTime,
    qrCodeRenewalIntervalExponentCompact,
    qrCodeValidityStartTime,
  };
}

export function renewLocationSpecificPart(locationSpecificPart, qrCodeValidityStartTime) {
  locationSpecificPart.qrCodeValidityStartTime = qrCodeValidityStartTime;
}

export async function newDeepLink(location, locationSpecificPart, staff) {
  let header = buildHeader(0, 0, locationSpecificPart.locationTemporaryPublicId); // FIXME version and qrType
  let msg = await buildMessage(location, locationSpecificPart, staff);
  let output = await encrypt(header, msg, location.serverAuthorityPublicKey);

  // Convert output to Base64url
  let base64url = btoa((Array.from(new Uint8Array(output))).map(ch => String.fromCharCode(ch)).join('')).replace(/\+/g, '-').replace(/\//g, '_').replace(/={1,2}$/, '');
  return COUNTRY_SPECIFIC_PREFIX + base64url;
}

export async function newDeepLinks(location, locationSpecificPart) {
  let staffDeepLink = newDeepLink(location, locationSpecificPart, true);
  let visitorsDeepLink = newDeepLink(location, locationSpecificPart, false);
  { staffDeepLink; visitorsDeepLink }
}

function buildHeader(version, qrType, locationTemporaryPublicId) {
  const CLEAR_HEADER_SIZE = 17;

  let header = new Uint8Array(CLEAR_HEADER_SIZE);

  // Fill header
  header[0] = ((version & 0x7) << 5) | ((qrType & 0x7) << 2);
  header.set(locationTemporaryPublicId, 1);

  return header;
}

async function buildMessage(location, locationSpecificPart, staff) {
  const MSG_SIZE = 44;
  const LOC_MSG_SIZE = 16;
  const TAG_AND_KEY = 49;
  let msg = new Uint8Array(MSG_SIZE + (locationSpecificPart.locContactMsg ? LOC_MSG_SIZE + TAG_AND_KEY : 0));
  let loc_msg = new Uint8Array(LOC_MSG_SIZE);

  let compressedPeriodStartTime = locationSpecificPart.periodStartTime / 3600;
  let qrCodeValidityStartTime = locationSpecificPart.qrCodeValidityStartTime;

  let reserved = 0x0; // reserved for specification evolution
  msg[0] = ((staff & 0x1) << 7) | (locationSpecificPart.locContactMsg ? 0x40 : 0) | ((reserved & 0xFC0) >>> 6);
  msg[1] = ((reserved & 0x3F) << 2) | ((locationSpecificPart.qrCodeRenewalIntervalExponentCompact & 0x18) >> 3);
  msg[2] = ((locationSpecificPart.qrCodeRenewalIntervalExponentCompact & 0x7) << 5) | (locationSpecificPart.venueType & 0x1F);
  msg[3] = ((locationSpecificPart.venueCategory1 & 0xF) << 4) | (locationSpecificPart.venueCategory2 & 0xF);
  msg[4] = locationSpecificPart.periodDuration;
  msg[5] = (compressedPeriodStartTime >> 16) & 0xFF; // multi-byte numbers are stored with the big endian convention as required by the specification
  msg[6] = (compressedPeriodStartTime >> 8) & 0xFF;
  msg[7] = compressedPeriodStartTime & 0xFF;
  msg[8] = (qrCodeValidityStartTime >> 24) & 0xFF;
  msg[9] = (qrCodeValidityStartTime >> 16) & 0xFF;
  msg[10] = (qrCodeValidityStartTime >> 8) & 0xFF;
  msg[11] = qrCodeValidityStartTime & 0xFF;
  msg.set(new Uint8Array(locationSpecificPart.locationTemporarySecretKey), 12);

  if (locationSpecificPart.locContactMsg) {
    const phone = parseBcd(locationSpecificPart.locContactMsg.locationPhone, 8);
    loc_msg.set(phone, 0);
    // Max digit is 15, the last 4 bits are set to 0 (pad)
    loc_msg[7] = loc_msg[7] & 0xF0;
    loc_msg[8] = locationSpecificPart.locContactMsg.locationRegion & 0xFF;
    const pin = parseBcd(locationSpecificPart.locContactMsg.locationPin, 3);
    loc_msg.set(pin, 9);
    let encrypted_loc_msg = await encrypt(new Uint8Array(0), loc_msg, location.manualContactTracingAuthorityPublicKey);
    msg.set(new Uint8Array(encrypted_loc_msg), 44);
  }

  return msg;
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