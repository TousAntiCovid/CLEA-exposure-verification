package fr.inria.clea.lsp;

import java.time.Instant;

import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
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
    public static final String REGION_VALIDATION_MESSAGE = "Location phone is mandatory";
    public static final String PIN_VALIDATION_MESSAGE = "Secret digit PIN must contain exactly 6 characters";
    public static final String PERIOD_START_TIME_VALIDATION_MESSAGE = "Period start time must not be null";

    /* Phone number of the location contact person, one digit = one character */
    @NotBlank(message= PHONE_VALIDATION_MESSAGE)
    String locationPhone;
    /* Coarse grain geographical information for the location */  
    @Min(value = 0, message = REGION_VALIDATION_MESSAGE)
    @Max(value = 255, message = REGION_VALIDATION_MESSAGE)
    int locationRegion;
    /* Secret 6 digit PIN, one digit = one character */
    @Size(min = 6, max = 6, 
            message = PIN_VALIDATION_MESSAGE)
    String locationPin;
    /* Starting time of the period in seconds */
    @NotNull(message= PERIOD_START_TIME_VALIDATION_MESSAGE)
    @Setter(AccessLevel.PROTECTED)
    Instant periodStartTime;
}
