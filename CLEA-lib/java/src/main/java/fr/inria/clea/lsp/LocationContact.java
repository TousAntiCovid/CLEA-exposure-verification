package fr.inria.clea.lsp;

import javax.validation.constraints.Max;

import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import lombok.AccessLevel;

@Builder
@Getter
@EqualsAndHashCode
@ToString
public class LocationContact {
    /* Phone number of the location contact person, one digit = one character, 15 digits max */
    @Max(value = 15)
    String locationPhone;
    /* Coarse grain geographical information for the location */
    @Max(value = 255)
    int locationRegion;
    /* Secret 6 digit PIN, one digit = one character */
    @Max(value = 6)
    String locationPin;
    /* Starting time of the period in seconds */
    @Setter(AccessLevel.PROTECTED)
    int periodStartTime;
}
