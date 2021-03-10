package fr.inria.clea.lsp;

public class LspEncoderDecoder {

    /**
     * Main
     * 
     * @see README.md
     * 
     */
    public static void main(String[] args) throws Exception {

        final String help = "Usage: LspEncoderDecoder [decode  lsp64 privKey] [encode staff countryCode CRIexp venueType venueCategory1 venueCategory2 periodDuration locationPhone locationPin pubkey]";
        LocationSpecificPartTest tests = new LocationSpecificPartTest();

        if (args.length == 0) {
            tests.test2(false);
        } else if ("encode".equals(args[0])) {
            if ((args.length == 13) || (args.length == 11)) {
                int staff = Integer.parseInt(args[1]);
                int countryCode = Integer.parseInt(args[2]);
                int CRIexp = Integer.parseInt(args[3]);
                int venueType = Integer.parseInt(args[4]);
                int venueCategory1 = Integer.parseInt(args[5]);
                int venueCategory2 = Integer.parseInt(args[6]);
                int periodDuration = Integer.parseInt(args[7]);
                final String PK_SA = args[8];
                final String PK_MCTA = args[9];
                final String SK_L = args[10];
                Encode lsp = new Encode(SK_L, PK_SA, PK_MCTA, false);
                if (args.length == 13) {
                    final String locationPhone = args[11];
                    final String locationPin = args[12];
                    lsp.setParam(staff, countryCode, CRIexp, venueType, venueCategory1, venueCategory2, periodDuration,
                            locationPhone, locationPin);
                } else {
                    lsp.setParam(staff, countryCode, CRIexp, venueType, venueCategory1, venueCategory2, periodDuration);
                }
                lsp.startNewPeriod();
                final String valuesToreturn = lsp.getLSPTobase64() + " " + lsp.LTId + " "
                        + Integer.toUnsignedString(lsp.ct_periodStart) + " " + Integer.toUnsignedString(lsp.t_qrStart);
                System.out.println(valuesToreturn);
            } else {
                System.out.println(help);
            }
        } else if ("decode".equals(args[0])) {
            if (args.length == 4) {
                String lsp64 = args[1];
                String SK_SA = args[2];
                String SK_MCTA = args[3];
                Decode lsp = new Decode(SK_SA, SK_MCTA, false);
                lsp.getLSP(lsp64);
                System.out.print(lsp.staff + " " + lsp.countryCode + " " + lsp.CRIexp + " " + lsp.venueType + " "
                        + lsp.venueCategory1 + " " + lsp.venueCategory2 + " " + lsp.periodDuration + " " + lsp.LTId
                        + " " + Integer.toUnsignedString(lsp.ct_periodStart) + " "
                        + Integer.toUnsignedString(lsp.t_qrStart));
                if (lsp.locContactMsgPresent == 1) {
                    System.out.print(" " + lsp.locationPhone + " " + lsp.locationPIN);
                }
                System.out.println();
            } else {
                System.out.println(help);
            }
        } else {
            System.out.println(help);
        }
    }

}
