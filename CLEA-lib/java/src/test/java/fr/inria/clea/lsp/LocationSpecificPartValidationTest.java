package fr.inria.clea.lsp;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.Instant;
import java.util.Random;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import fr.inria.clea.lsp.LocationSpecificPart.LocationSpecificPartBuilder;
import fr.inria.clea.lsp.exception.CleaInvalidLocationMessageException;

public class LocationSpecificPartValidationTest {

    @SuppressWarnings("rawtypes")
    private LocationSpecificPartBuilder locationSpecificPartBuilder;
    private LocationSpecificPartValidator validator;
    
    @BeforeEach
    public void setUp() {
        validator = new LocationSpecificPartValidator();
        Instant now = Instant.now();
        byte[] encryptedLocationContactMessage = new byte[CleaEciesEncoder.LOC_BYTES_SIZE];
        new Random().nextBytes(encryptedLocationContactMessage);
        byte[] locationTemporarySecretKey =  new byte[LocationSpecificPart.LOCATION_TEMPORARY_SECRET_KEY_SIZE];
        new Random().nextBytes(encryptedLocationContactMessage);
        locationSpecificPartBuilder = LocationSpecificPart.builder()
                .version(0)
                .type(0)
                .countryCode(250)
                .staff(false)
                .locationTemporaryPublicId(UUID.randomUUID())
                .qrCodeRenewalIntervalExponentCompact(0)
                .venueType(0)
                .venueCategory1(0)
                .venueCategory2(0)
                .periodDuration(24)
                .compressedPeriodStartTime(0)
                .qrCodeValidityStartTime(now)
                .locationTemporarySecretKey(locationTemporarySecretKey)
                .encryptedLocationContactMessage(encryptedLocationContactMessage);
    }

    @Test
    public void testWhenLocationSpecificParttIsValidThenValidationSucceeds() throws CleaInvalidLocationMessageException {
        this.validator.validateMessage(locationSpecificPartBuilder.build());
    }

    @Test
    public void testWhenVersionHasNegativeValueThenValidationFails() {
        LocationSpecificPart lsp = locationSpecificPartBuilder.version(-1).build();
        
        CleaInvalidLocationMessageException exception = assertThrows(CleaInvalidLocationMessageException.class, () -> {
            this.validator.validateMessage(lsp);
        });
        
        assertThat(exception.getViolations().size()).isEqualTo(1);
        assertThat(exception.getViolations()).anyMatch(violation -> violation.getMessage().equals(LocationSpecificPart.VERSION_VALIDATION_MESSAGE));
    }

    @Test
    public void testWhenVersionGreaterThan8ThenValidationFails() {
        LocationSpecificPart lsp = locationSpecificPartBuilder.version(9).build();
        
        CleaInvalidLocationMessageException exception = assertThrows(CleaInvalidLocationMessageException.class, () -> {
            this.validator.validateMessage(lsp);
        });
        
        assertThat(exception.getViolations().size()).isEqualTo(1);
        assertThat(exception.getViolations()).anyMatch(violation -> violation.getMessage().equals(LocationSpecificPart.VERSION_VALIDATION_MESSAGE));
    }

    @Test
    public void testWhenTypeHasNegativeValueThenValidationFails() {
        LocationSpecificPart lsp = locationSpecificPartBuilder.type(-1).build();
        
        CleaInvalidLocationMessageException exception = assertThrows(CleaInvalidLocationMessageException.class, () -> {
            this.validator.validateMessage(lsp);
        });
        
        assertThat(exception.getViolations().size()).isEqualTo(1);
        assertThat(exception.getViolations()).anyMatch(violation -> violation.getMessage().equals(LocationSpecificPart.TYPE_VALIDATION_MESSAGE));
    }

    @Test
    public void testWhenTypeGreaterThan8ThenValidationFails() {
        LocationSpecificPart lsp = locationSpecificPartBuilder.type(9).build();
        
        CleaInvalidLocationMessageException exception = assertThrows(CleaInvalidLocationMessageException.class, () -> {
            this.validator.validateMessage(lsp);
        });
        
        assertThat(exception.getViolations().size()).isEqualTo(1);
        assertThat(exception.getViolations()).anyMatch(violation -> violation.getMessage().equals(LocationSpecificPart.TYPE_VALIDATION_MESSAGE));
    }
    
    @Test
    public void testWhenCountryCodeHasnegativeValueThenValidationFails() {
        LocationSpecificPart lsp = locationSpecificPartBuilder.countryCode(-1).build();
        
        CleaInvalidLocationMessageException exception = assertThrows(CleaInvalidLocationMessageException.class, () -> {
            this.validator.validateMessage(lsp);
        });
        
        assertThat(exception.getViolations().size()).isEqualTo(1);
        assertThat(exception.getViolations()).anyMatch(violation -> violation.getMessage().equals(LocationSpecificPart.COUNTRY_CODE_VALIDATION_MESSAGE));
    }

    @Test
    public void testWhenCountryCodeGreaterThan4096ThenValidationFails() {
        LocationSpecificPart lsp = locationSpecificPartBuilder.countryCode(4097).build();
        
        CleaInvalidLocationMessageException exception = assertThrows(CleaInvalidLocationMessageException.class, () -> {
            this.validator.validateMessage(lsp);
        });
        
        assertThat(exception.getViolations().size()).isEqualTo(1);
        assertThat(exception.getViolations()).anyMatch(violation -> violation.getMessage().equals(LocationSpecificPart.COUNTRY_CODE_VALIDATION_MESSAGE));
    }

    @Test
    public void testWhenLocationTemporaryPublicIdIsIsNullThenValidationFails() {
        LocationSpecificPart lsp = locationSpecificPartBuilder.locationTemporaryPublicId(null).build();
        
        CleaInvalidLocationMessageException exception = assertThrows(CleaInvalidLocationMessageException.class, () -> {
            this.validator.validateMessage(lsp);
        });
        
        assertThat(exception.getViolations().size()).isEqualTo(1);
        assertThat(exception.getViolations()).anyMatch(violation -> violation.getMessage().equals(LocationSpecificPart.LOCATION_TEMPORARY_PUBLIC_ID_VALIDATION_MESSAGE));
    }

    @Test
    public void testWhenQrCodeRenewalIntervalExponentCompactHasNegativeValueThenValidationFails() {
        LocationSpecificPart lsp = locationSpecificPartBuilder.qrCodeRenewalIntervalExponentCompact(-1).build();
        
        CleaInvalidLocationMessageException exception = assertThrows(CleaInvalidLocationMessageException.class, () -> {
            this.validator.validateMessage(lsp);
        });
        
        assertThat(exception.getViolations().size()).isEqualTo(1);
        assertThat(exception.getViolations()).anyMatch(violation -> violation.getMessage().equals(LocationSpecificPart.QRCODE_RENEWAL_INTERVAL_VALIDATION_MESSAGE));
    }

    @Test
    public void testWhenQrCodeRenewalIntervalExponentGreaterThan32ThenValidationFails() {
        LocationSpecificPart lsp = locationSpecificPartBuilder.qrCodeRenewalIntervalExponentCompact(33).build();
        
        CleaInvalidLocationMessageException exception = assertThrows(CleaInvalidLocationMessageException.class, () -> {
            this.validator.validateMessage(lsp);
        });
        
        assertThat(exception.getViolations().size()).isEqualTo(1);
        assertThat(exception.getViolations()).anyMatch(violation -> violation.getMessage().equals(LocationSpecificPart.QRCODE_RENEWAL_INTERVAL_VALIDATION_MESSAGE));
    }

    @Test
    public void testWhenVenueTypeHasNegativeValueThenValidationFails() {
        LocationSpecificPart lsp = locationSpecificPartBuilder.venueType(-1).build();
        
        CleaInvalidLocationMessageException exception = assertThrows(CleaInvalidLocationMessageException.class, () -> {
            this.validator.validateMessage(lsp);
        });
        
        assertThat(exception.getViolations().size()).isEqualTo(1);
        assertThat(exception.getViolations()).anyMatch(violation -> violation.getMessage().equals(LocationSpecificPart.VENUE_TYPE_VALIDATION_MESSAGE));
    }

    @Test
    public void testWhenVenueTypeGreaterThan32ThenValidationFails() {
        LocationSpecificPart lsp = locationSpecificPartBuilder.venueType(33).build();
        
        CleaInvalidLocationMessageException exception = assertThrows(CleaInvalidLocationMessageException.class, () -> {
            this.validator.validateMessage(lsp);
        });
        
        assertThat(exception.getViolations().size()).isEqualTo(1);
        assertThat(exception.getViolations()).anyMatch(violation -> violation.getMessage().equals(LocationSpecificPart.VENUE_TYPE_VALIDATION_MESSAGE));
    }
    
    @Test
    public void testWhenVenueCategorie1HasNegativeValueThenValidationFails() {
        LocationSpecificPart lsp = locationSpecificPartBuilder.venueCategory1(-1).build();
        
        CleaInvalidLocationMessageException exception = assertThrows(CleaInvalidLocationMessageException.class, () -> {
            this.validator.validateMessage(lsp);
        });
        
        assertThat(exception.getViolations().size()).isEqualTo(1);
        assertThat(exception.getViolations()).anyMatch(violation -> violation.getMessage().equals(LocationSpecificPart.VENUE_CAT1_VALIDATION_MESSAGE));
    }

    @Test
    public void testWhenVenueCategorie1GreaterThan16ThenValidationFails() {
        LocationSpecificPart lsp = locationSpecificPartBuilder.venueCategory1(17).build();
        
        CleaInvalidLocationMessageException exception = assertThrows(CleaInvalidLocationMessageException.class, () -> {
            this.validator.validateMessage(lsp);
        });
        
        assertThat(exception.getViolations().size()).isEqualTo(1);
        assertThat(exception.getViolations()).anyMatch(violation -> violation.getMessage().equals(LocationSpecificPart.VENUE_CAT1_VALIDATION_MESSAGE));
    }

    @Test
    public void testWhenVenueCategorie2HasNegativeValueThenValidationFails() {
        LocationSpecificPart lsp = locationSpecificPartBuilder.venueCategory2(-1).build();
        
        CleaInvalidLocationMessageException exception = assertThrows(CleaInvalidLocationMessageException.class, () -> {
            this.validator.validateMessage(lsp);
        });
        
        assertThat(exception.getViolations().size()).isEqualTo(1);
        assertThat(exception.getViolations()).anyMatch(violation -> violation.getMessage().equals(LocationSpecificPart.VENUE_CAT2_VALIDATION_MESSAGE));
    }

    @Test
    public void testWhenVenueCategorie2GreaterThan16ThenValidationFails() {
        LocationSpecificPart lsp = locationSpecificPartBuilder.venueCategory2(17).build();
        
        CleaInvalidLocationMessageException exception = assertThrows(CleaInvalidLocationMessageException.class, () -> {
            this.validator.validateMessage(lsp);
        });
        
        assertThat(exception.getViolations().size()).isEqualTo(1);
        assertThat(exception.getViolations()).anyMatch(violation -> violation.getMessage().equals(LocationSpecificPart.VENUE_CAT2_VALIDATION_MESSAGE));
    }

    @Test
    public void testWhenPeriodDurationHasnegativeValueThenValidationFails() {
        LocationSpecificPart lsp = locationSpecificPartBuilder.periodDuration(-1).build();
        
        CleaInvalidLocationMessageException exception = assertThrows(CleaInvalidLocationMessageException.class, () -> {
            this.validator.validateMessage(lsp);
        });
        
        assertThat(exception.getViolations().size()).isEqualTo(1);
        assertThat(exception.getViolations()).anyMatch(violation -> violation.getMessage().equals(LocationSpecificPart.PERIOD_DURATION_VALIDATION_MESSAGE));
    }

    @Test
    public void testWhenPeriodDurationIsGreaterThan255ThenValidationFails() {
        LocationSpecificPart lsp = locationSpecificPartBuilder.periodDuration(256).build();
        
        CleaInvalidLocationMessageException exception = assertThrows(CleaInvalidLocationMessageException.class, () -> {
            this.validator.validateMessage(lsp);
        });
        
        assertThat(exception.getViolations().size()).isEqualTo(1);
        assertThat(exception.getViolations()).anyMatch(violation -> violation.getMessage().equals(LocationSpecificPart.PERIOD_DURATION_VALIDATION_MESSAGE));
    }


    @Test
    public void testWhenCompressedPeriodStartTimeHasnegativeValueThenValidationFails() {
        LocationSpecificPart lsp = locationSpecificPartBuilder.compressedPeriodStartTime(-1).build();
        
        CleaInvalidLocationMessageException exception = assertThrows(CleaInvalidLocationMessageException.class, () -> {
            this.validator.validateMessage(lsp);
        });
        
        assertThat(exception.getViolations().size()).isEqualTo(1);
        assertThat(exception.getViolations()).anyMatch(violation -> violation.getMessage().equals(LocationSpecificPart.COMPRESSED_PERIOD_START_TIME_VALIDATION_MESSAGE));
    }

    @Test
    public void testWhenCompressedPeriodStartTimeIsGreaterThanMaxAllowedValueThenValidationFails() {
        LocationSpecificPart lsp = locationSpecificPartBuilder.compressedPeriodStartTime(16777217).build();
        
        CleaInvalidLocationMessageException exception = assertThrows(CleaInvalidLocationMessageException.class, () -> {
            this.validator.validateMessage(lsp);
        });
        
        assertThat(exception.getViolations().size()).isEqualTo(1);
        assertThat(exception.getViolations()).anyMatch(violation -> violation.getMessage().equals(LocationSpecificPart.COMPRESSED_PERIOD_START_TIME_VALIDATION_MESSAGE));
    }
    
    @Test
    public void testWhenQrCodeValidityStartTimeIsIsNullThenValidationFails() {
        LocationSpecificPart lsp = locationSpecificPartBuilder.qrCodeValidityStartTime(null).build();
        
        CleaInvalidLocationMessageException exception = assertThrows(CleaInvalidLocationMessageException.class, () -> {
            this.validator.validateMessage(lsp);
        });
        
        assertThat(exception.getViolations().size()).isEqualTo(1);
        assertThat(exception.getViolations()).anyMatch(violation -> violation.getMessage().equals(LocationSpecificPart.QR_CODE_VALIDITY_START_TIME_VALIDATION_MESSAGE));
    }
    
    @Test
    public void testWhenLocationTemporarySecretKeyIsNullThenValidationFails() {
        LocationSpecificPart lsp = locationSpecificPartBuilder.locationTemporarySecretKey(null).build();
        
        CleaInvalidLocationMessageException exception = assertThrows(CleaInvalidLocationMessageException.class, () -> {
            this.validator.validateMessage(lsp);
        });
        
        assertThat(exception.getViolations().size()).isEqualTo(1);
        assertThat(exception.getViolations()).anyMatch(violation -> violation.getMessage().equals(LocationSpecificPart.LOCATION_TEMPORARY_SECRET_KEY_VALIDATION_MESSAGE));
    }
    
    @Test
    public void testWhenLocationTemporarySecretKeyDoesNotHaveExpectedSizeThenValidationFails() {
        byte[] locationTemporarySecretKey = new byte[LocationSpecificPart.LOCATION_TEMPORARY_SECRET_KEY_SIZE - 1];
        new Random().nextBytes(locationTemporarySecretKey);
        LocationSpecificPart lsp = locationSpecificPartBuilder.locationTemporarySecretKey(locationTemporarySecretKey).build();
        
        CleaInvalidLocationMessageException exception = assertThrows(CleaInvalidLocationMessageException.class, () -> {
            this.validator.validateMessage(lsp);
        });
        
        assertThat(exception.getViolations().size()).isEqualTo(1);
        assertThat(exception.getViolations()).anyMatch(violation -> violation.getMessage().equals(LocationSpecificPart.LOCATION_TEMPORARY_SECRET_KEY_SIZE_VALIDATION_MESSAGE));
    }
    
    
    @Test
    public void testWhenLocationTemporaryPublicIdIsNullThenValidationFails() {
        LocationSpecificPart lsp = locationSpecificPartBuilder.locationTemporaryPublicId(null).build();
        
        CleaInvalidLocationMessageException exception = assertThrows(CleaInvalidLocationMessageException.class, () -> {
            this.validator.validateMessage(lsp);
        });
        
        assertThat(exception.getViolations().size()).isEqualTo(1);
        assertThat(exception.getViolations()).anyMatch(violation -> violation.getMessage().equals(LocationSpecificPart.LOCATION_TEMPORARY_PUBLIC_ID_VALIDATION_MESSAGE));
    }
}