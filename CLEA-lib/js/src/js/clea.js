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

export async function decryptDeeplink(data, sk_sa_in, sk_mcta_in)
{
  try {
    var lsp_base64 = data.split("https://tac.gouv.fr?v=0#").join('');
    if (data == lsp_base64) {
      console.error("Bad url base");
    }

    let sk_sa = new Uint8Array(sk_sa_in.match(/.{1,2}/g).map(b => parseInt(b, 16)));
    let sk_mcta = new Uint8Array(sk_mcta_in.match(/.{1,2}/g).map(b => parseInt(b, 16)));

    //replace because we use B64 url safe format without padding
    // 4 char ascii en B64 = 3 octets. If too much data then there is padding with ==, that are stripped from the end
    lsp_base64 = lsp_base64.replace(/_/g, '/').replace(/-/g, '+').replace(/={1,2}$/, '');

    // lsp = content of qr code
    var lsp = Uint8Array.from(atob(lsp_base64), c => c.charCodeAt(0));

    var decoded =
        {
          version: -1,
          lspType: -1,
          LTId: "",
          staff: false,
          CRIexp: -1,
          venueType: -1,
          venueCategory1: -1,
          venueCategory2: -1,
          periodDuration: -1,
          ct_periodStart: -1,
          t_qrStart: -1,
          LTKey: "",
          locationMsg: null
        }

    decoded.version = lsp[0] >>> 5;
    decoded.lspType = (lsp[0] >>> 2) & 0x7;
    decoded.LTId = buf2bn(lsp.slice(1, 17)).toString(16);

    var dec = await decode(lsp.slice(0, 17), lsp.slice(17), sk_sa);

    decoded.staff = ((dec[0] & 0x80) >>> 7);
    decoded.CRIexp = ((dec[1] & 0x3) << 3) | (dec[2] >>> 5);
    decoded.venueType = dec[2] & 0x1F;
    decoded.venueCategory1 = dec[3] >>> 4;
    decoded.venueCategory2 = dec[3] & 0xF;
    decoded.periodDuration = dec[4];
    decoded.ct_periodStart = (dec[5] << 16) | (dec[6] << 8) | dec[7];
    decoded.t_qrStart = ((dec[8] << 24) | (dec[9] << 16) | (dec[10] << 8) | dec[11]) >>> 0;
    decoded.LTKey = buf2bn(dec.slice(12, 44)).toString(16);

    // If contact location message is present, decode it
    //0x40 because:
    /*
      contact location message presence flag is 2nd bit of first byte
      01000000 = 0x40
     */
    if (dec[0] & 0x40) {
      try {
        dec = await decode(null, dec.slice(44), sk_mcta)

        decoded.locationMsg = {
          locationPhone: bcd2str(dec.slice(0, 8), true, true),
          locationRegion: dec[8],
          locationPin: bcd2str(dec.slice(9, 12, false, false))
        }
      } catch (e) {
        console.error("Error decoding location specific message")
        console.error(e);
      }
    }

    return decoded;
  } catch (e) {
    console.error("Error decoding global message")
    console.error(e);
  }
}

/*
  LSP:  17 octets non cryptés = header
        X octets cryptés = message
        clé privée = sk
        manual contact tracing authority key = mcta
 */
async function decode(header, message, sk)
{

  /*
    size of message part where is stored information to decode key (asymetrically encrypted), at the end of the message
   */
  const C0_BYTE_SIZE = 33;

  var C0 = message.slice(message.length - C0_BYTE_SIZE);
  var C0_Q = ecdh_raw_pubkey_decompressed(C0);
  var S = multiply(buf2bn(sk), C0_Q).x;

  var buf = concat(concat(C0, bn2buf(S)), new Uint8Array(4));
  var kdf1 = await crypto.subtle.digest("SHA-256", buf);

  var derivedKey = await crypto.subtle.importKey(
      "raw",
      kdf1,
      "AES-GCM",
      true,
      ["decrypt"]
  );

  console.log("C0 = 0" + buf2bn(C0).toString(16));
  console.log("S = " + S.toString(16));
  console.log("KDF = " + buf2bn(new Uint8Array(await crypto.subtle.exportKey("raw", derivedKey))).toString(16));

  const iv = new Uint8Array([0xf0, 0xf1, 0xf2, 0xf3, 0xf4, 0xf5, 0xf6, 0xf7, 0xf8, 0xf9, 0xfa, 0xfb]);

  var algo = {name: "AES-GCM", iv: iv, tagLength: 128};

  if(header)
  {
    algo.additionalData = header;
  }

  var decoded = await crypto.subtle.decrypt(
      algo,
      derivedKey,
      message.slice(0, message.length - C0_BYTE_SIZE));

  return new Uint8Array(decoded);
}

// SECP256R1 curve parameters
const curve =
    {
      p: 2n ** 256n - 2n ** 224n + 2n ** 192n + 2n ** 96n - 1n,
      g: {x: 0x6B17D1F2E12C4247F8BCE6E563A440F277037D812DEB33A0F4A13945D898C296n,
        y: 0x4FE342E2FE1A7F9B8EE7EB4A7C0F9E162BCE33576B315ECECBB6406837BF51F5n},
      a: 115792089210356248762697446949407573530086143415290314195533631308867097853948n,
      b: 41058363725152142129326129780047268409114441015993725554835256314039467401291n
    }

function multiply(n, a)
{
  function mod(a)
  {
    var ret = a % curve.p;
    return ret >= 0 ? ret : curve.p + ret;
  }

  function invert(n)
  {
    var a = mod(n);
    var b = curve.p;
    var x = 0n, y = 1n, u = 1n, v = 0n;

    while(a !== 0n)
    {
      var q = b / a;
      var r = b % a;

      var m = x - u * q;
      var n = y - v * q;

      b = a;
      a = r;
      x = u;
      y = v;
      u = m;
      v = n;
    }

    return mod(x);
  }

  function add(a, b)
  {
    if (a.x === 0n || a.y === 0n) return b;
    if (b.x === 0n || b.y === 0n) return a;
    if (a.x === b.x && a.y === b.y) return double(a);
    if (a.x === b.x && a.y !== b.y) return {x: 0n, y: 0n};

    var lam = mod((b.y - a.y) * invert(b.x - a.x));
    var x = mod(lam * lam - a.x - b.x);
    var y = mod(lam * (a.x - x) - a.y);

    return {x: x, y: y};
  }

  function double(p)
  {
    var lam = mod((3n * p.x ** 2n + curve.a) * invert(2n * p.y));
    var x = mod(lam * lam - 2n * p.x);
    var y = mod(lam * (p.x - x) - p.y);

    return {x: x, y: y};
  }

  var p = {x: 0n, y: 0n};
  var d = {x: a.x, y: a.y};

  while(n > 0n)
  {
    if(n & 1n)
    {
      p = add(p, d);
    }

    d = double(d);

    n >>= 1n;
  }

  return p;
}

function concat(b1, b2)
{
  if(!b1)
  {
    return b2;
  }

  if(!b2)
  {
    return b1;
  }

  var b = new Uint8Array(b1.length + b2.length);

  b.set(b1, 0);
  b.set(b2, b1.length);

  return b;
}

function buf2bn(buf)
{
  var hex = [];

  buf.forEach(function (i) {
    var h = i.toString(16);
    if (h.length % 2) { h = '0' + h; }
    hex.push(h);
  });

  return BigInt("0x" + hex.join(""));
}

function bn2buf(bn)
{
  var hex = bn.toString(16);

  if(hex.length % 2)
  {
    hex = "0" + hex;
  }

  var buf = new Uint8Array(hex.length / 2);
  for(var i = 0; i < hex.length / 2; i++)
  {
    buf[i] = parseInt(hex.slice(2 * i, 2 * i + 2), 16);
  }

  return buf;
}

function bcd2str(buf, stop_seq, stop_half)
{
  var out = "";

  var idx = 0;
  buf.forEach(function(n)
  {
    if(((n >>> 4) != 0xF) || !stop_seq)
    {
      out += (n >>> 4);
    }

    if (!stop_half || !(idx==buf.length-1) ) {
      if( ((n & 0xF) != 0xF) || !stop_seq)
      {
        out += (n & 0xF);
      }
    }
    idx +=1;
  });

  return out;
}

function ecdh_raw_pubkey_decompressed(compressed)
{
  // Convert compressed byte array to X coordinate
  var x = buf2bn(compressed.slice(1));

  // Compute Y from X
  var y = square_root(x ** 3n + curve.a * x + curve.b);

  if(((compressed[0] == 0x2) && (y % 2n == 1n)) ||
      ((compressed[0] == 0x3) && (y % 2n == 0n)))
  {
    y = curve.p - y;
  }

  return {x: x, y: y};
}

function square_root(a)
{
  // Compute a ** ((p + 1) / 4)
  var x = 1n;
  var y = a;
  var b = (curve.p + 1n) / 4n;

  while(b > 0n)
  {
    if(b % 2n == 1n)
    {
      x = (x * y) % curve.p;
    }

    y = (y * y) % curve.p;

    b /= 2n;
  }

  return x % curve.p;
}