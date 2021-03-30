function start_decoder()
{
  var video = $("#webcam");
  var canvasElement = $("#qr-canvas");
  var canvas = canvasElement[0].getContext("2d");

  function drawLine(begin, end, color) {
    canvas.beginPath();
    canvas.moveTo(begin.x, begin.y);
    canvas.lineTo(end.x, end.y);
    canvas.lineWidth = 4;
    canvas.strokeStyle = color;
    canvas.stroke();
  }

  // Use facingMode: environment to attemt to get the front camera on phones
  navigator.mediaDevices.getUserMedia({ video: { facingMode: "environment" } }).then(function(stream) {
    video.attr("playsinline", true); // required to tell iOS safari we don't want fullscreen
    video[0].srcObject = stream;
    video[0].play();
    requestAnimationFrame(tick);
  });

  async function tick()
  {
    if(video[0].readyState === video[0].HAVE_ENOUGH_DATA)
    {
      var w = video[0].videoWidth;
      var h = video[0].videoHeight;

      canvasElement.attr("width", w);
      canvasElement.attr("height", h);
      canvas.drawImage(video[0], 0, 0, w, h);

      var imageData = canvas.getImageData(0, 0, canvasElement.attr("width"), canvasElement.attr("height"));
      var code = jsQR(imageData.data, imageData.width, imageData.height, {inversionAttempts: "dontInvert"});

      if(code)
      {
        drawLine(code.location.topLeftCorner, code.location.topRightCorner, "#FF3B58");
        drawLine(code.location.topRightCorner, code.location.bottomRightCorner, "#FF3B58");
        drawLine(code.location.bottomRightCorner, code.location.bottomLeftCorner, "#FF3B58");
        drawLine(code.location.bottomLeftCorner, code.location.topLeftCorner, "#FF3B58");

        await got_content(code.data);
      }
    }
    setTimeout(() => requestAnimationFrame(tick), 100); // 10 FPS
  }
}

function show_result(msg)
{
  var key = ["version", "lspType", "LTId", "staff", "CRIexp", "venueType", "venueCategory1", "venueCategory2", "countryCode", "periodDuration", "ct_periodStart", "t_qrStart", "LTKey"];

  for(i in key)
  {
    $("#" +  key[i]).html(msg[key[i]])
  }

  if(msg.locationMsg)
  {
    $("#locationPhone").html(msg.locationMsg.locationPhone);
    $("#locationRegion").html(msg.locationMsg.locationRegion);
    $("#locationPin").html(msg.locationMsg.locationPin);
  }
}

async function got_content(data)
{
  $("#qrcode_content").html(data);

  var lsp_base64 = data.split("/").slice(3).join("/");
  var lsp = Uint8Array.from(atob(lsp_base64), c => c.charCodeAt(0));
  var sk_sa = new Uint8Array($("#sk_sa").val().match(/.{1,2}/g).map(b => parseInt(b, 16)));
  var sk_mcta = new Uint8Array($("#sk_mcta").val().match(/.{1,2}/g).map(b => parseInt(b, 16)));

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
    countryCode: -1,
    periodDuration: -1,
    ct_periodStart: -1,
    t_qrStart: -1,
    LTKey: "",
    locationMsg: null
  }

  decoded.version = lsp[0] >>> 5;
  decoded.lspType = (lsp[0] >>> 2) & 0x7;
  decoded.LTId = buf2bn(lsp.slice(1, 17)).toString(16);

  try
  {
    var dec = await decode(lsp.slice(0, 17), lsp.slice(17), sk_sa);
  
    decoded.staff = ((dec[0] & 0x80) >>> 7);
    decoded.countryCode = ((dec[0] & 0x3F) << 6) | (dec[1] >>> 2);
    decoded.CRIexp = ((dec[1] & 0x3) << 3) | (dec[2] >>> 5);
    decoded.venueType = dec[2] & 0x1F;
    decoded.venueCategory1 = dec[3] >>> 4;
    decoded.venueCategory2 = dec[3] & 0xF;
    decoded.periodDuration = dec[4];
    decoded.ct_periodStart = (dec[5] << 16) | (dec[6] << 8) | dec[7];
    decoded.t_qrStart = ( (dec[8] << 24) | (dec[9] << 16) | (dec[10] << 8) | dec[11] ) >>>0;
    decoded.LTKey = buf2bn(dec.slice(12, 44)).toString(16);

    // If contact location message is present, decode it
    if(dec[0] & 0x40)
    {
      try
      {
        dec = await decode(null, dec.slice(44), sk_mcta)

        decoded.locationMsg = {locationPhone: bcd2str(dec.slice(0, 8), true, true), locationRegion: dec[8], locationPin: bcd2str(dec.slice(9, 12, false, false))}
      }
      catch(e)
      {
        console.error("Error decoding location specific message")
        console.error(e);
      }
    }

    show_result(decoded);
  }
  catch(e)
  {
    console.error("Error decoding global message")
    console.error(e);
  }
}

async function decode(header, message, sk)
{
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
    algo["additionalData"] = header;
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