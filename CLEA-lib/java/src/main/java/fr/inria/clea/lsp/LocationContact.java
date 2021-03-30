package fr.inria.clea.lsp;

import java.time.Instant;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

import lombok.AccessLevel;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Builder
@Getter
@EqualsAndHashCode
@ToString
public class LocationContact {
    public static final String PHONE_VALIDATION_MESSAGE = "Location phone is mandatory";
    public static final String PIN_VALIDATION_MESSAGE = "Secret digit PIN must contain exactly 6 characters";
    public static final String PERIOD_START_TIME_VALIDATION_MESSAGE = "Period start time must not be null";

    /* Phone number of the location contact person, one digit = one character */
    @NotBlank(message= PHONE_VALIDATION_MESSAGE)
    String locationPhone;
    /* Secret 6 digit PIN, one digit = one character */
    // TODO: set max to 6 when CSV files used for tests are updated
    @Size(min = 6, max = 8, 
            message = PIN_VALIDATION_MESSAGE)
    String locationPin;
    /* Starting time of the period in seconds */
    @NotNull(message= PERIOD_START_TIME_VALIDATION_MESSAGE)
    @Setter(AccessLevel.PROTECTED)
    Instant periodStartTime;
}
