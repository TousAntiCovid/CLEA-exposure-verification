/* 
 * Copyright (C) Inria, 2021
 */
package fr.inria.clea.lsp;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

import javax.validation.constraints.Max;

import fr.inria.clea.lsp.utils.TimeUtils;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import lombok.experimental.SuperBuilder;

/**
 * LocationSpecificPart (LSP) contents data respecting the CLEA protocol
 * 
 * @see <a href="https://hal.inria.fr/hal-03146022">CLEA protocol</a>
 */
@SuperBuilder(toBuilder = true)
@Getter
@EqualsAndHashCode
@AllArgsConstructor
@ToString
public class LocationSpecificPart {
    /* Clea protocol version number */
    @Builder.Default
    @Max(value = 8)
    protected int version = 0;
    
    /*
     * LSP type, in order to be able to use multiple formats in parallel in the
     * future.
     */
    @Builder.Default
    @Max(value = 8)
    protected int type = 0;
    
    /*
     * Country code, coded as the ISO 3166-1 country code, for instance 0x250 for
     * France
     */
    @Builder.Default
    @Max(value = 4096)
    protected int countryCode = 33;
    
    /* regular users or staff member of the location */
    protected boolean staff;
    
    /*
     * Location Temporary public universally unique Identifier (UUID), specific to a
     * given location at a given period.
     */
    @Setter
    protected UUID locationTemporaryPublicId;
    
    /*
     * qrCodeRenewalInterval value in a compact manner, as the exponent of a power
     * of two.
     */
    @Max(value = 32)
    protected int qrCodeRenewalIntervalExponentCompact;
    
    /* Type of the location/venue */
    @Max(value = 32)
    protected int venueType;
    
    /* Reserved: a first level of venue category */
    @Max(value = 16)
    protected int venueCategory1;
    
    /* Reserved: a second level of venue category */
    @Max(value = 16)
    protected int venueCategory2;
    
    /* Duration, in terms of number of hours, of the period */
    @Max(value = 255)
    protected int periodDuration;
    
    /* Starting time of the period in a compressed manner (round hour) */
    @Max(value = 16777216)
    protected int compressedPeriodStartTime;
    
    /* Starting time of the QR code validity timespan in seconds */
    @Setter
    protected Instant qrCodeValidityStartTime;
    
    /* Temporary location key for the period */
    @Setter
    protected byte[] locationTemporarySecretKey;
    
    @Setter
    protected byte[] encryptedLocationContactMessage;
    
    /**
     *  Indicates if the location contact message is present in the message 
     */
    public boolean isLocationContactMessagePresent() {
        return Objects.nonNull(this.encryptedLocationContactMessage);
    }

    public Instant getPeriodStartTime() {
        return TimeUtils.instantFromTimestamp((long) this.compressedPeriodStartTime * TimeUtils.NB_SECONDS_PER_HOUR);
    }
    
    public void setPeriodStartTime(Instant periodStartTime) {
        long periodStartTimeAsNtpTimestamp = TimeUtils.ntpTimestampFromInstant(periodStartTime);
        this.compressedPeriodStartTime = (int) (periodStartTimeAsNtpTimestamp / TimeUtils.NB_SECONDS_PER_HOUR);
    }
    
    /**
     * @return the number of seconds between a new QR code generation.
     */
    public int getQrCodeRenewalInterval() {
        if (this.qrCodeRenewalIntervalExponentCompact == 0x1F) {
            return 0;
        }
        return (int) Math.pow(2, this.qrCodeRenewalIntervalExponentCompact);
    }
    
}
