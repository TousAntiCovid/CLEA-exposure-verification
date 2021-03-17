var t_periodStart = get_ntp_utc(true);
var LTId = new Uint8Array(16);
var LTKey = new Uint8Array(32);
// RPG
var ct_periodStart;
var t_qrStart;

async function clea_start_new_period(config) {
  t_periodStart = get_ntp_utc(true);

  // Compute LTKey
  var tmp = new Uint8Array(64);
  tmp.set(config.SK_L, 0);
  tmp[60] = (t_periodStart >> 24) & 0xFF;
  tmp[61] = (t_periodStart >> 16) & 0xFF;
  tmp[62] = (t_periodStart >> 8) & 0xFF;
  tmp[63] = t_periodStart;
  LTKey = await crypto.subtle.digest("SHA-256", tmp)

  // Compute LTId
  var one = new Uint8Array(1);
  one[0] = 0x31; // '1'
  var key = await crypto.subtle.importKey(
    "raw",
    LTKey, {
      name: "HMAC",
      hash: "SHA-512"
    },
    true,
    ["sign"]
  );
  LTId = new Uint8Array(await crypto.subtle.sign("HMAC", key, one), 0, 16); // HMAC-SHA256-128

  return clea_renew_qrcode(config);
}

async function clea_renew_qrcode(config) {
  const CLEAR_HEADER_SIZE = 17;
  const MSG_SIZE = 44;
  const LOC_MSG_SIZE = 16;
  const TAG_AND_KEY = 49;

  t_qrStart = get_ntp_utc(false);
  ct_periodStart = t_periodStart / 3600;
  
  var header = new Uint8Array(CLEAR_HEADER_SIZE);
  var msg = new Uint8Array(MSG_SIZE + (config.locContactMsg ? LOC_MSG_SIZE + TAG_AND_KEY : 0));
  var loc_msg = new Uint8Array(LOC_MSG_SIZE);

  // Fill header
  header[0] = ((config.version & 0x7) << 5) | ((config.qrType & 0x7) << 2);
  header.set(LTId, 1);

  // Fill message
  msg[0] = ((config.staff & 0x1) << 7) | (config.locContactMsg ? 0x40 : 0) | ((config.countryCode & 0xFC0) >>> 6);
  msg[1] = ((config.countryCode & 0x3F) << 2) | ((config.CRIexp & 0x18) >> 3);
  msg[2] = ((config.CRIexp & 0x7) << 5) | (config.venueType & 0x1F);
  msg[3] = ((config.venueCategory1 & 0xF) << 4) | (config.venueCategory2 & 0xF);
  msg[4] = config.periodDuration;
  msg[5] = (ct_periodStart >> 16) & 0xFF; // multi-byte numbers are stored with the big endian convention as required by the specification
  msg[6] = (ct_periodStart >> 8) & 0xFF;
  msg[7] = ct_periodStart & 0xFF;
  msg[8] = (t_qrStart >> 24) & 0xFF;
  msg[9] = (t_qrStart >> 16) & 0xFF;
  msg[10] = (t_qrStart >> 8) & 0xFF;
  msg[11] = t_qrStart & 0xFF;
  msg.set(LTKey, 12);

  if (config.locContactMsg) {
    const phone = parse_bcd(config.locContactMsg.locationPhone, 8);
    loc_msg.set(phone, 0);
    const pin = parse_bcd(config.locContactMsg.locationPin, 4);
    loc_msg.set(pin, 8);
  
    encrypted_loc_msg = await encrypt(new Uint8Array(0), loc_msg, config.PK_MCTA);
    msg.set(new Uint8Array(encrypted_loc_msg), 44);
  }

  output = await encrypt(header, msg, config.PK_SA);

  // Convert output to Base64
  return btoa((Array.from(new Uint8Array(output))).map(ch => String.fromCharCode(ch)).join(''));
}

async function encrypt(header, message, publicKey) {
  // Step 1: Import the publicKey Q
  var ECPubKey = await crypto.subtle.importKey(
    "raw",
    publicKey, {
      name: "ECDH",
      namedCurve: "P-256"
    },
    true,
    []
  );

  // Step 2: Generate a transient EC key pair (r, C0 = rG)
  var EcKeyPair = await crypto.subtle.generateKey({
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

  C0_Q = ecdh_raw_pubkey_compressed(new Uint8Array(C0));

  print_buf("C0", C0_Q);

  // Step 4: Generate a shared secret (S = rQ)
  var S = await crypto.subtle.deriveBits({
      name: "ECDH",
      namedCurve: "P-256",
      public: ECPubKey
    },
    EcKeyPair.privateKey,
    256
  );

  print_buf("S", S);

  // Step5: Compute the AES encryption key K = KDF1(C0 | S)
  var tmp = concatBuffer(concatBuffer(C0_Q, S), new ArrayBuffer(4));
  var kdf1 = await crypto.subtle.digest("SHA-256", tmp)

  print_buf("C0 | S | 0x00000000", tmp);
  print_buf("KDF1", kdf1)

  var derivedKey = await crypto.subtle.importKey(
    "raw",
    kdf1,
    "AES-GCM",
    false,
    ["encrypt"]
  );

  // Step6: Encrypt the data
  const iv = new Uint8Array([0xf0, 0xf1, 0xf2, 0xf3, 0xf4, 0xf5, 0xf6, 0xf7, 0xf8, 0xf9, 0xfa, 0xfb]);

  var encoded = await crypto.subtle.encrypt({
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

  print_buf("output", output);

  return output;
}

function get_ntp_utc(round) {
  const ONE_HOUR_IN_MS = 3600000;

  var t = Date.now();

  if (round) {
    var th = Math.floor(t / ONE_HOUR_IN_MS); // Number of hours since the epoch
    var rem = t % ONE_HOUR_IN_MS; // Number of ms since the last round hour

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
 * @private
 * @param {ArrayBuffers} buf1 The first buffer.
 * @param {ArrayBuffers} buf2 The second buffer.
 * @return {ArrayBuffers} The new ArrayBuffer created out of the two.
 */
function concatBuffer(buf1, buf2) {
  var out = new Uint8Array(buf1.byteLength + buf2.byteLength);
  out.set(new Uint8Array(buf1), 0);
  out.set(new Uint8Array(buf2), buf1.byteLength);
  return out.buffer;
}

function ecdh_raw_pubkey_compressed(ec_raw_pubkey) {
  const u8full = new Uint8Array(ec_raw_pubkey)
  const len = u8full.byteLength
  const u8 = u8full.slice(0, 1 + len >>> 1) // drop `y`
  u8[0] = 0x2 | (u8full[len - 1] & 0x01) // encode sign of `y` in first bit
  return u8.buffer
}

function print_buf(name, b) {
  console.log(name + " = " + Array.from(new Uint8Array(b)).map(u => ("0" + u.toString(16)).slice(-2)).join(""))
}

function getInt64Bytes(x) {
  var bytes = [];
  var i = 8;
  do {
    bytes[--i] = x & (255);
    x = x >> 8;
  } while (i)
  return bytes;
}

function parse_bcd(string, size) {
  var i = 0, ip, k;
  var array = new Uint8Array(size);
  
  for (i = 0; i < string.length; i++) {
    digit = string.charAt(i) - '0';
    if (i % 2 == 0) {
      ip = i / 2;
      array[ip] = (digit << 4) | 0x0F;
    } else { 
      ip = (i / 2) - 0.5;
      array[ip] &=  (0xF0 | digit); 
    }
  }

  for (k = ip + 1; k < size; k++) {
    array[k] = 0xFF;
  }
  return array;
}