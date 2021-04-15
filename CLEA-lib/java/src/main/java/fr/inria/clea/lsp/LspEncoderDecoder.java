package fr.inria.clea.lsp;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

import fr.devnied.bitlib.BytesUtils;
import fr.inria.clea.lsp.Location.LocationBuilder;
import fr.inria.clea.lsp.exception.CleaCryptoException;
import fr.inria.clea.lsp.utils.TimeUtils;

public class LspEncoderDecoder {

    /**
     * Main
     * 
     * @see README.md
     */
    public static void main(String[] args) throws Exception {
        final String help = "Usage: LspEncoderDecoder [gen-keys] [decode  lsp64 privKey] [encode staff CRIexp venueType venueCategory1 venueCategory2 periodDuration locationPhone locationPin pubkey]";

        if (args.length == 0) {
            System.out.println(help);
            System.exit(0);
        }

        if ("encode".equals(args[0]) && ((args.length == 13) || (args.length == 10))) {
            encodeLsp(args);
        } else if ("decode".equals(args[0]) && args.length == 4) {
            decodeLsp(args);
        } else if ("gen-keys".equals(args[0])) {
            generateKeyPair();
        } else {
            System.out.println(help);
        }
    }

    protected static void generateKeyPair() throws Exception {
        String[] keyPair = new CleaEciesEncoder().genKeysPair(true);
        System.out.println("Clea EC Private Key: " + keyPair[0]);
        System.out.println("Clea EC Public Key : " + keyPair[1]);
    }

    protected static void decodeLsp(String[] args) throws CleaCryptoException {
        String lspBase64 = args[1];
        String serverAuthoritySecretKey = args[2];
        String manualContactTracingAuthoritySecretKey = args[3];
        LocationSpecificPartDecoder lspDecoder = new LocationSpecificPartDecoder(serverAuthoritySecretKey);
        LocationSpecificPart lsp = lspDecoder.decrypt(lspBase64);

        String valuesToreturn =  "=VALUES="+ (lsp.isStaff()? 1 : 0) +  " " + lsp.getQrCodeRenewalIntervalExponentCompact()  + " " + lsp.getVenueType(); 
        valuesToreturn += " " + lsp.getVenueCategory1() + " " + lsp.getVenueCategory2() + " " + lsp.getPeriodDuration() + " " + lsp.getLocationTemporaryPublicId();
        valuesToreturn += " " + Integer.toUnsignedString(lsp.getCompressedPeriodStartTime()) + " " + TimeUtils.ntpTimestampFromInstant(lsp.getQrCodeValidityStartTime());
        valuesToreturn += " " + BytesUtils.bytesToStringNoSpace(lsp.getLocationTemporarySecretKey()).toLowerCase();

        if (lsp.isLocationContactMessagePresent()) {
            LocationContactMessageEncoder contactMessageDecode = new LocationContactMessageEncoder(manualContactTracingAuthoritySecretKey);
            LocationContact locationContact = contactMessageDecode.decode(lsp.getEncryptedLocationContactMessage());
            valuesToreturn += " " + locationContact.getLocationPhone() + " " + locationContact.getLocationRegion() + " " + locationContact.getLocationPin();
        }  
        System.out.println(valuesToreturn);
    }

    protected static void encodeLsp(String[] args) throws CleaCryptoException {
        int staff = Integer.parseInt(args[1]);
        int qrCodeRenewalIntervalExponentCompact = Integer.parseInt(args[2]);
        int venueType = Integer.parseInt(args[3]);
        int venueCategory1 = Integer.parseInt(args[4]);
        int venueCategory2 = Integer.parseInt(args[5]);
        int periodDuration = Integer.parseInt(args[6]);
        final String serverAuthorityPublicKey = args[7];
        final String manualContactTracingAuthorityPublicKey = args[8];
        final String permanentLocationSecretKey = args[9];
        
        Instant periodStartTime = Instant.now().truncatedTo(ChronoUnit.HOURS);
        /* Encode a LSP with location */
        LocationSpecificPart lsp = LocationSpecificPart.builder()
                .staff(staff == 1)
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
            final String locationPhone = args[10];
            final int locationRegion = Integer.parseInt(args[11]);
            final String locationPin = args[12];
            locationBuilder.contact( new LocationContact(locationPhone, locationRegion, locationPin, periodStartTime));
        }

        Location location = locationBuilder.build();
        location.setPeriodStartTime(periodStartTime);
        //location.setQrCodeValidityStartTime(periodStartTime, (int) TimeUtils.currentNtpTime());
        location.getLocationSpecificPart().setQrCodeValidityStartTime(Instant.now().truncatedTo(ChronoUnit.HOURS));
        
        String encryptedLocationSpecificPart = location.getLocationSpecificPartEncryptedBase64();
        String LTKey =  BytesUtils.bytesToStringNoSpace(location.getLocationSpecificPart().getLocationTemporarySecretKey()).toLowerCase();

        final String valuesToreturn = "=VALUES=" + encryptedLocationSpecificPart + " " 
                + location.getLocationSpecificPart().getLocationTemporaryPublicId() + " "
                + Integer.toUnsignedString(location.getLocationSpecificPart().getCompressedPeriodStartTime()) + " "
                + TimeUtils.ntpTimestampFromInstant(location.getLocationSpecificPart().getQrCodeValidityStartTime()) + " "
                + LTKey ;
        System.out.println(valuesToreturn);
    }

}
