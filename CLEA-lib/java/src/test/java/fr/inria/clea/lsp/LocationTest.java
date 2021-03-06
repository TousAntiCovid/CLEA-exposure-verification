package fr.inria.clea.lsp;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import fr.inria.clea.lsp.exception.CleaCryptoException;
import fr.inria.clea.lsp.exception.CleaEncryptionException;

// @ExtendWith(MockitoJUnitRunner.class)
public class LocationTest {

    /* Example of a permanent Location Secret Key used for the tests */
    private final String permanentLocationSecretKey = "23c9b8f36ac1c0cddaf869c3733b771c3dc409416a9695df40397cea53e7f39e21f76925fc0c74ca6ee7c7eafad92473fd8575";
    private String[] serverAuthorityKeyPair;
    private String[] manualContactTracingAuthorityKeyPair;
    private Instant periodStartTime;
    private LocationContact locationContact;
    //@Mock
    private LocationSpecificPart lsp;
    private Location location;

    @BeforeEach
    public void setUp() throws Exception {
        CleaEciesEncoder cleaEciesEncoder = new CleaEciesEncoder();
        serverAuthorityKeyPair = cleaEciesEncoder.genKeysPair(true);
        manualContactTracingAuthorityKeyPair = cleaEciesEncoder.genKeysPair(true);
        periodStartTime = Instant.now().truncatedTo(ChronoUnit.HOURS);
        locationContact = new LocationContact("3380013000", 10, "012345", periodStartTime);
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
        
        Instant qrCodeValidityStartTime = periodStartTime.plus(qrCodeRenewalInterval, ChronoUnit.SECONDS);
        location.setQrCodeValidityStartTime(periodStartTime, qrCodeValidityStartTime);
        assertThat(lsp.getQrCodeValidityStartTime()).isEqualTo(qrCodeValidityStartTime);
    }

    @Test
    public void testWhenSettingQrCodeValidityStartTimeWithValidQrCodeRenewalIntervalThenQrValidityStartTimeNotUpdated() {
        int qrCodeRenewalIntervalExponentCompact = 2;
        int periodDuration = 3;
        lsp = Mockito.spy(newLocationSpecificPart(qrCodeRenewalIntervalExponentCompact, periodDuration));
        location = newLocation(locationContact, lsp);
        
        location.setQrCodeValidityStartTime(periodStartTime, periodStartTime.minus(1, ChronoUnit.SECONDS));
        location.setQrCodeValidityStartTime(periodStartTime, periodStartTime.plus(1, ChronoUnit.SECONDS));
        location.setQrCodeValidityStartTime(periodStartTime, periodStartTime.plus(periodDuration + 1, ChronoUnit.HOURS));

        verify(lsp, never()).setQrCodeValidityStartTime(Mockito.any());
    }

    @Test
    public void testWhenSettingQrCodeValidityStartTimeWithNoQrCodeRenewalIntervalThenQrValidityStartTimeNotUpdated() {
        int qrCodeRenewalIntervalExponentCompact = LocationSpecificPart.QRCODE_NO_RENEWAL;
        int periodDuration = 3;

        lsp = newLocationSpecificPart(qrCodeRenewalIntervalExponentCompact, periodDuration);
        location = newLocation(locationContact, lsp);

        location.setQrCodeValidityStartTime(periodStartTime, periodStartTime);
        assertThat(lsp.getQrCodeValidityStartTime()).isEqualTo(periodStartTime);

        Instant newQrCodeValidityStartTime = periodStartTime.plus(1, ChronoUnit.SECONDS);
        location.setQrCodeValidityStartTime(periodStartTime, newQrCodeValidityStartTime);
        assertThat(lsp.getQrCodeValidityStartTime()).isNotEqualTo(newQrCodeValidityStartTime);
    }

    @Test
    public void testWhenSettingQrCodeValidityStartTimeWithNoQrCodeRenewalIntervalThenQrValidityStartTimeUpdatedWhenNewPeriod() throws CleaEncryptionException {
        int qrCodeRenewalIntervalExponentCompact = LocationSpecificPart.QRCODE_NO_RENEWAL;
        int periodDuration = 3;

        lsp = newLocationSpecificPart(qrCodeRenewalIntervalExponentCompact, periodDuration);
        location = newLocation(locationContact, lsp);

        location.setQrCodeValidityStartTime(periodStartTime, periodStartTime);
        assertThat(lsp.getQrCodeValidityStartTime()).isEqualTo(periodStartTime);

        
        periodStartTime = periodStartTime.plus(3, ChronoUnit.HOURS);
        location.setPeriodStartTime(periodStartTime);
        location.setQrCodeValidityStartTime(periodStartTime, periodStartTime);
        assertThat(lsp.getQrCodeValidityStartTime()).isEqualTo(periodStartTime);
    }

    @Test
    public void testNewDeepLink() throws CleaCryptoException {
        int qrCodeRenewalIntervalExponentCompact = 2;
        int periodDuration = 3;

        lsp = newLocationSpecificPart(qrCodeRenewalIntervalExponentCompact, periodDuration);
        location = newLocation(locationContact, lsp);

        String deepLink = location.newDeepLink();
        assertThat(deepLink).isNotEmpty();
        assertThat(deepLink).startsWith(Location.COUNTRY_SPECIFIC_PREFIX);

        String deepLink2 = location.newDeepLink(periodStartTime);
        assertThat(deepLink2).isNotEmpty();
        assertThat(deepLink2).startsWith(Location.COUNTRY_SPECIFIC_PREFIX);

        String deepLink3 = location.newDeepLink(periodStartTime, periodStartTime);
        assertThat(deepLink3).isNotEmpty();
        assertThat(deepLink3).startsWith(Location.COUNTRY_SPECIFIC_PREFIX);
        
        assertThat(deepLink).isNotIn(deepLink2, deepLink3);
        assertThat(deepLink2).isNotIn(deepLink, deepLink3);
        assertThat(deepLink3).isNotIn(deepLink, deepLink2);
    }

    protected LocationSpecificPart newLocationSpecificPart(int qrCodeRenewalIntervalExponentCompact,
            int periodDuration) {
        return LocationSpecificPart.builder().staff(true)
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
