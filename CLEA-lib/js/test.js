(() =>
{
    var qrcode = new QRCode("qrcode", {width: 500, height: 500, correctLevel: QRCode.CorrectLevel.M});

    async function generate_qrcode()
    {
        var conf = {
            SK_L: hexToBytes($("#sk_l").val()),
            PK_SA: hexToBytes($("#pk_sa").val()),
            PK_MCTA: hexToBytes($("#pk_mcta").val()),

            staff: $("#staff").prop("checked"),
            CRIexp: parseInt(Math.log(parseInt($("#CRI").val())) / Math.log(2)),
            venueType: parseInt($("#venueType").val()),
            venueCategory1: parseInt($("#venueCategory1").val()),
            venueCategory2: parseInt($("#venueCategory2").val()),
            countryCode: parseInt($("#countryCode").val()),
            periodDuration: parseInt($("#periodDuration").val()),
            locContactMsg: null
        };

        var phone = $("#locationPhone").val()

        if(phone)
        {
            conf.locContactMsg =
            {
                locationPhone: parseInt(phone),
                locationPin: parseInt($("#locationPin").val())
            }
        }

        console.log(conf);

        var b64 = await clea_start_new_period(conf);

        qrcode.makeCode("http://tac.gouv.fr/" + b64);
    }

    // Convert a hex string to a byte array
    function hexToBytes(hex)
    {
        var bytes = new Uint8Array(Math.ceil(hex.length / 2));
        for (i = 0, c = 0; c < hex.length; i++, c += 2)
            bytes[i] = parseInt(hex.substr(c, 2), 16);
        return bytes;
    }

    generate_qrcode();
    setInterval(generate_qrcode, 10000);
})();