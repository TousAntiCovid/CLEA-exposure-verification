package fr.inria.clea.lsp;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import fr.inria.clea.lsp.utils.TimeUtils;

// @ExtendWith(MockitoJUnitRunner.class)
public class LocationTest {

    /* Example of a permanent Location Secret Key used for the tests */
    private final String permanentLocationSecretKey = "23c9b8f36ac1c0cddaf869c3733b771c3dc409416a9695df40397cea53e7f39e21f76925fc0c74ca6ee7c7eafad92473fd85758bab8f45fe01aac504";
    private String[] serverAuthorityKeyPair;
    private String[] manualContactTracingAuthorityKeyPair;
    private int periodStartTime;
    private LocationContact locationContact;
    //@Mock
    private LocationSpecificPart lsp;
    private Location location;

    @BeforeEach
    public void setUp() throws Exception {
        CleaEciesEncoder cleaEciesEncoder = new CleaEciesEncoder();
        serverAuthorityKeyPair = cleaEciesEncoder.genKeysPair(true);
        manualContactTracingAuthorityKeyPair = cleaEciesEncoder.genKeysPair(true);
        periodStartTime = TimeUtils.hourRoundedCurrentTimeTimestamp32();
        locationContact = new LocationContact("0612150292", "01234567", periodStartTime);
    }

    @Test
    public void testWhenSettingQrCodeValidityStartTimeWithValidQrCodeRenewalIntervalThenQrValidityStartTimeUpdated() {
        int qrCodeRenewalIntervalExponentCompact = 2;
        int qrCodeRenewalInterval = 1 << qrCodeRenewalIntervalExponentCompact;
        int periodDuration = 3;
        lsp = newLocationSpecificPart(qrCodeRenewalIntervalExponentCompact, periodDuration);
        location = newLocation(locationContact, lsp);
        
        location.setQrCodeValidityStartTime(periodStartTime, periodStartTime);
        assertThat(lsp.getQrCodeValidityStartTime()).isEqualTo(periodStartTime);
        
        location.setQrCodeValidityStartTime(periodStartTime, periodStartTime + qrCodeRenewalInterval);
        assertThat(lsp.getQrCodeValidityStartTime()).isEqualTo(periodStartTime + qrCodeRenewalInterval);
    }

    @Test
    public void testWhenSettingQrCodeValidityStartTimeWithValidQrCodeRenewalIntervalThenQrValidityStartTimeNotUpdated() {
        int qrCodeRenewalIntervalExponentCompact = 2;
        int periodDuration = 3;
        lsp = Mockito.spy(newLocationSpecificPart(qrCodeRenewalIntervalExponentCompact, periodDuration));
        location = newLocation(locationContact, lsp);
        
        location.setQrCodeValidityStartTime(periodStartTime, periodStartTime - 1);
        location.setQrCodeValidityStartTime(periodStartTime, periodStartTime + 1);
        location.setQrCodeValidityStartTime(periodStartTime, periodStartTime + (periodDuration + 1) * 3600);

        verify(lsp, never()).setQrCodeValidityStartTime(Mockito.anyInt());
    }

    @Test
    public void testWhenSettingQrCodeValidityStartTimeWithNoQrCodeRenewalIntervalThenQrValidityStartTimeNotUpdated() {
        int qrCodeRenewalIntervalExponentCompact = 0x1F;
        int periodDuration = 3;

        lsp = newLocationSpecificPart(qrCodeRenewalIntervalExponentCompact, periodDuration);
        location = newLocation(locationContact, lsp);

        location.setQrCodeValidityStartTime(periodStartTime, periodStartTime);
        assertThat(lsp.getQrCodeValidityStartTime()).isEqualTo(periodStartTime);

        location.setQrCodeValidityStartTime(periodStartTime, periodStartTime + 1);
        assertThat(lsp.getQrCodeValidityStartTime()).isNotEqualTo(periodStartTime + 1);
    }

    @Test
    public void testNewDeepLink() throws CleaEncryptionException {
        int qrCodeRenewalIntervalExponentCompact = 2;
        int periodDuration = 3;

        lsp = newLocationSpecificPart(qrCodeRenewalIntervalExponentCompact, periodDuration);
        location = newLocation(locationContact, lsp);

        String deepLink = location.newDeepLink();
        assertThat(deepLink).isNotEmpty();
        assertThat(deepLink).startsWith(Location.COUNTRY_SPECIFIC_PREFIX);

        deepLink = location.newDeepLink(periodStartTime);
        assertThat(deepLink).isNotEmpty();
        assertThat(deepLink).startsWith(Location.COUNTRY_SPECIFIC_PREFIX);

        deepLink = location.newDeepLink(periodStartTime, periodStartTime);
        assertThat(deepLink).isNotEmpty();
        assertThat(deepLink).startsWith(Location.COUNTRY_SPECIFIC_PREFIX);
    }

    protected LocationSpecificPart newLocationSpecificPart(int qrCodeRenewalIntervalExponentCompact,
            int periodDuration) {
        return LocationSpecificPart.builder().staff(true).countryCode(33)
                .qrCodeRenewalIntervalExponentCompact(qrCodeRenewalIntervalExponentCompact).venueType(4)
                .venueCategory1(0).venueCategory2(0).periodDuration(periodDuration).build();
    }

    protected Location newLocation(LocationContact locationContact, LocationSpecificPart lsp) {
        return Location.builder().contact(locationContact).locationSpecificPart(lsp)
                .manualContactTracingAuthorityPublicKey(manualContactTracingAuthorityKeyPair[1])
                .serverAuthorityPublicKey(serverAuthorityKeyPair[1])
                .permanentLocationSecretKey(permanentLocationSecretKey).build();
    }

}
