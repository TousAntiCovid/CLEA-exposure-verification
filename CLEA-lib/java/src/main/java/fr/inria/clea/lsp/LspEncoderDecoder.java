package fr.inria.clea.lsp;

import fr.inria.clea.lsp.Location.LocationBuilder;
import fr.inria.clea.lsp.utils.TimeUtils;

public class LspEncoderDecoder {

    /**
     * Main
     * 
     * @see README.md
     */
    public static void main(String[] args) throws Exception {
        final String help = "Usage: LspEncoderDecoder [decode  lsp64 privKey] [encode staff countryCode CRIexp venueType venueCategory1 venueCategory2 periodDuration locationPhone locationPin pubkey]";

        if (args.length == 0) {
            System.out.println(help);
            System.exit(0);
        }

        if ("encode".equals(args[0]) && ((args.length == 13) || (args.length == 11))) {
            encodeLsp(args);
        } else if ("decode".equals(args[0]) && args.length == 4) {
            decodeLsp(args);
        } else {
            System.out.println(help);
        }
    }

    protected static void decodeLsp(String[] args) throws CleaEncryptionException {
        String lspBase64 = args[1];
        String serverAuthoritySecretKey = args[2];
        String manualContactTracingAuthoritySecretKey = args[3];
        LocationSpecificPartDecoder lspDecoder = new LocationSpecificPartDecoder(serverAuthoritySecretKey);
        LocationSpecificPart lsp = lspDecoder.decrypt(lspBase64);
      
        String valuesToreturn =  (lsp.isStaff()? 1 : 0) + " " + lsp.getCountryCode() + " " + lsp.getQrCodeRenewalIntervalExponentCompact()  + " " + lsp.getVenueType(); 
        valuesToreturn += " " + lsp.getVenueCategory1() + " " + lsp.getVenueCategory2() + " " + lsp.getPeriodDuration() + " " + lsp.getLocationTemporaryPublicId();
        valuesToreturn += " " + Integer.toUnsignedString(lsp.getCompressedPeriodStartTime()) + " " + Integer.toUnsignedString(lsp.getQrCodeValidityStartTime());

        if (lsp.isLocationContactMessagePresent()) {
            LocationContactMessageEncoder contactMessageDecode = new LocationContactMessageEncoder(manualContactTracingAuthoritySecretKey);
            LocationContact locationContact = contactMessageDecode.decode(lsp.getEncryptedLocationContactMessage());
            valuesToreturn += " " + locationContact.getLocationPhone() + " " + locationContact.getLocationPin();
        }  
        System.out.println(valuesToreturn);
    }

    protected static void encodeLsp(String[] args) throws CleaEncryptionException {
        int staff = Integer.parseInt(args[1]);
        int countryCode = Integer.parseInt(args[2]);
        int qrCodeRenewalIntervalExponentCompact = Integer.parseInt(args[3]);
        int venueType = Integer.parseInt(args[4]);
        int venueCategory1 = Integer.parseInt(args[5]);
        int venueCategory2 = Integer.parseInt(args[6]);
        int periodDuration = Integer.parseInt(args[7]);
        final String serverAuthorityPublicKey = args[8];
        final String manualContactTracingAuthorityPublicKey = args[9];
        final String permanentLocationSecretKey = args[10];
        
        int periodStartTime = TimeUtils.hourRoundedCurrentTimeTimestamp32();
        /* Encode a LSP with location */
        LocationSpecificPart lsp = LocationSpecificPart.builder()
                .staff(staff == 1)
                .countryCode(countryCode)
                .qrCodeRenewalIntervalExponentCompact(qrCodeRenewalIntervalExponentCompact)
                .venueType(venueType)
                .venueCategory1(venueCategory1)
                .venueCategory2(venueCategory2)
                .periodDuration(periodDuration)
                .build();
        LocationBuilder locationBuilder = Location.builder()
                .locationSpecificPart(lsp)
                .manualContactTracingAuthorityPublicKey(manualContactTracingAuthorityPublicKey)
                .serverAuthorityPublicKey(serverAuthorityPublicKey)
                .permanentLocationSecretKey(permanentLocationSecretKey);
        
        if (args.length == 13) {
            final String locationPhone = args[11];
            final String locationPin = args[12];
            locationBuilder.contact( new LocationContact(locationPhone, locationPin, periodStartTime));
        }

        Location location = locationBuilder.build();
        location.setPeriodStartTime(periodStartTime);
        //location.setQrCodeValidityStartTime(periodStartTime, (int) TimeUtils.currentNtpTime());
        location.getLocationSpecificPart().setQrCodeValidityStartTime( (int) TimeUtils.currentNtpTime());
        
        String encryptedLocationSpecificPart = location.getLocationSpecificPartEncryptedBase64();
        
        final String valuesToreturn = encryptedLocationSpecificPart + " " 
                + location.getLocationSpecificPart().getLocationTemporaryPublicId() + " "
                + Integer.toUnsignedString(location.getLocationSpecificPart().getCompressedPeriodStartTime()) + " "
                + Integer.toUnsignedString(location.getLocationSpecificPart().getQrCodeValidityStartTime());
        System.out.println(valuesToreturn);
    }

}
