package fr.inria.clea.lsp;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.Instant;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import fr.inria.clea.lsp.LocationContact.LocationContactBuilder;
import fr.inria.clea.lsp.exception.CleaInvalidLocationContactMessageException;

public class LocationContactValidationTest {

    private LocationContactBuilder locationContactBuilder;

    @BeforeEach
    public void setUp() {
        Instant periodStartTime = Instant.now();
        locationContactBuilder = LocationContact.builder()
                .locationPhone("061122334455")
                .locationPin("123456")
                .periodStartTime(periodStartTime);
    }

    @Test
    public void testWhenLocationContactIsValidThenValidationSucceeds() throws CleaInvalidLocationContactMessageException {
        new LocationContactValidator().validateMessage(locationContactBuilder.build());
    }

    @Test
    public void testWhenPinCodeHasMoreThan6DigitsThenValidationFails() {
        LocationContact locationContact = locationContactBuilder.locationPin("1234567").build();
        
        CleaInvalidLocationContactMessageException exception = assertThrows(CleaInvalidLocationContactMessageException.class, () -> {
            new LocationContactValidator().validateMessage(locationContact);
        });
        
        assertThat(exception.getViolations().size()).isEqualTo(1);
        assertThat(exception.getViolations()).anyMatch(violation -> violation.getMessage().equals(LocationContact.PIN_VALIDATION_MESSAGE));
    }

    @Test
    public void testWhenPinCodeHasLessThan6DigitsThenValidationFails() {
        LocationContact locationContact = locationContactBuilder.locationPin("12345").build();
        
        CleaInvalidLocationContactMessageException exception = assertThrows(CleaInvalidLocationContactMessageException.class, () -> {
            new LocationContactValidator().validateMessage(locationContact);
        });
        
        assertThat(exception.getViolations().size()).isEqualTo(1);
        assertThat(exception.getViolations()).anyMatch(violation -> violation.getMessage().equals(LocationContact.PIN_VALIDATION_MESSAGE));
    }

    @Test
    public void testWhenLocationPhoneIsEmptyThenValidationFails() {
        LocationContact locationContact = locationContactBuilder.locationPhone("").build();
        
        CleaInvalidLocationContactMessageException exception = assertThrows(CleaInvalidLocationContactMessageException.class, () -> {
            new LocationContactValidator().validateMessage(locationContact);
        });
        
        assertThat(exception.getViolations().size()).isEqualTo(1);
        assertThat(exception.getViolations()).anyMatch(violation -> violation.getMessage().equals(LocationContact.PHONE_VALIDATION_MESSAGE));
    }

    @Test
    public void testWhenLocationPhoneIsNullThenValidationFails() {
        LocationContact locationContact = locationContactBuilder.locationPhone(null).build();
        
        CleaInvalidLocationContactMessageException exception = assertThrows(CleaInvalidLocationContactMessageException.class, () -> {
            new LocationContactValidator().validateMessage(locationContact);
        });
        
        assertThat(exception.getViolations().size()).isEqualTo(1);
        assertThat(exception.getViolations()).anyMatch(violation -> violation.getMessage().equals(LocationContact.PHONE_VALIDATION_MESSAGE));
    }

    @Test
    public void testWhenPeriodStartTimeIsNullThenValidationFails() {
        LocationContact locationContact = locationContactBuilder.periodStartTime(null).build();
        
        CleaInvalidLocationContactMessageException exception = assertThrows(CleaInvalidLocationContactMessageException.class, () -> {
            new LocationContactValidator().validateMessage(locationContact);
        });
        
        assertThat(exception.getViolations().size()).isEqualTo(1);
        assertThat(exception.getViolations()).anyMatch(violation -> violation.getMessage().equals(LocationContact.PERIOD_START_TIME_VALIDATION_MESSAGE));
    }
}
