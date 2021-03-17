package fr.inria.clea.lsp;

import javax.validation.constraints.Max;

import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

@Builder
@Getter
@EqualsAndHashCode
@ToString
public class LocationContact {
    /* Phone number of the location contact person, one digit = one character */
    String locationPhone;
    /* Secret 6 digit PIN, one digit = one character */
    @Max(value = 6)
    String locationPin;
    /* Starting time of the period in seconds */
    int periodStartTime;
}
