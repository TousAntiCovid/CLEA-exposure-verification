/* 
 * Copyright (C) Inria, 2021
 */
package fr.inria.clea.lsp;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

import fr.inria.clea.lsp.utils.TimeUtils;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import lombok.experimental.SuperBuilder;
import lombok.extern.slf4j.Slf4j;

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
@Slf4j
public class LocationSpecificPart {
    public static final short LOCATION_TEMPORARY_SECRET_KEY_SIZE = 64;
    public static final String VERSION_VALIDATION_MESSAGE = "Version should have a value between 0 and 8 (included)";
    public static final String TYPE_VALIDATION_MESSAGE = "Type should have a value between 0 and 8 (included)";
    public static final String COUNTRY_CODE_VALIDATION_MESSAGE = "Country code should have a value between 0 and 4096 (included)";
    public static final String LOCATION_TEMPORARY_PUBLIC_ID_VALIDATION_MESSAGE = "Location temporary public Id must not be null";
    public static final String QRCODE_RENEWAL_INTERVAL_VALIDATION_MESSAGE = "QR-code renewal interval exponent compact should have a value between 0 and 32 (included)";
    public static final String VENUE_TYPE_VALIDATION_MESSAGE = "Venue type should have a value between 0 and 32 (included)";
    public static final String VENUE_CAT1_VALIDATION_MESSAGE = "Venue type should have a value between 0 and 16 (included)";
    public static final String VENUE_CAT2_VALIDATION_MESSAGE = "Venue type should have a value between 0 and 16 (included)";
    public static final String PERIOD_DURATION_VALIDATION_MESSAGE = "Period duration should have a value between 0 and 255 (included)";
    public static final String COMPRESSED_PERIOD_START_TIME_VALIDATION_MESSAGE = "Compressed period start time should have a value between 0 and 16777216 (included)";
    public static final String QR_CODE_VALIDITY_START_TIME_VALIDATION_MESSAGE = "QR-code validity start time must not be null";
    public static final String LOCATION_TEMPORARY_SECRET_KEY_VALIDATION_MESSAGE = "Location temporary secret key must not be null";
    public static final String LOCATION_TEMPORARY_SECRET_KEY_SIZE_VALIDATION_MESSAGE = "Location temporary secret key must have a size of " + LOCATION_TEMPORARY_SECRET_KEY_SIZE + " bytes";
    
    /* Clea protocol version number */
    @Builder.Default
    @Min(value = 0, message = VERSION_VALIDATION_MESSAGE)
    @Max(value = 8, message = VERSION_VALIDATION_MESSAGE)
    protected int version = 0;
    
    /*
     * LSP type, in order to be able to use multiple formats in parallel in the
     * future.
     */
    @Builder.Default
    @Min(value = 0, message = TYPE_VALIDATION_MESSAGE)
    @Max(value = 8, message = TYPE_VALIDATION_MESSAGE)
    protected int type = 0;
    
    /*
     * Country code, coded as the ISO 3166-1 country code, for instance 0x250 for
     * France
     */
    @Builder.Default
    @Min(value = 0, message = COUNTRY_CODE_VALIDATION_MESSAGE)
    @Max(value = 4096, message = COUNTRY_CODE_VALIDATION_MESSAGE)
    protected int countryCode = 250;
    
    /* regular users or staff member of the location */
    protected boolean staff;
    
    /*
     * Location Temporary public universally unique Identifier (UUID), specific to a
     * given location at a given period.
     */
    @Setter
    @NotNull(message= LOCATION_TEMPORARY_PUBLIC_ID_VALIDATION_MESSAGE)
    protected UUID locationTemporaryPublicId;
    
    /*
     * qrCodeRenewalInterval value in a compact manner, as the exponent of a power
     * of two.
     */
    @Min(value = 0, message = QRCODE_RENEWAL_INTERVAL_VALIDATION_MESSAGE)
    @Max(value = 32, message = QRCODE_RENEWAL_INTERVAL_VALIDATION_MESSAGE)
    protected int qrCodeRenewalIntervalExponentCompact;
    
    /* Type of the location/venue */
    @Min(value = 0, message = VENUE_TYPE_VALIDATION_MESSAGE)
    @Max(value = 32, message = VENUE_TYPE_VALIDATION_MESSAGE)
    protected int venueType;
    
    /* Reserved: a first level of venue category */
    @Min(value = 0, message = VENUE_CAT1_VALIDATION_MESSAGE)
    @Max(value = 16, message = VENUE_CAT1_VALIDATION_MESSAGE)
    protected int venueCategory1;
    
    /* Reserved: a second level of venue category */
    @Min(value = 0, message = VENUE_CAT2_VALIDATION_MESSAGE)
    @Max(value = 16, message = VENUE_CAT2_VALIDATION_MESSAGE)
    protected int venueCategory2;
    
    /* Duration, in terms of number of hours, of the period */
    @Min(value = 0, message = PERIOD_DURATION_VALIDATION_MESSAGE)
    @Max(value = 255, message = PERIOD_DURATION_VALIDATION_MESSAGE)
    protected int periodDuration;
    
    /* Starting time of the period in a compressed manner (round hour) */
    @Min(value = 0, message = COMPRESSED_PERIOD_START_TIME_VALIDATION_MESSAGE)
    @Max(value = 16777216, message = COMPRESSED_PERIOD_START_TIME_VALIDATION_MESSAGE)
    protected int compressedPeriodStartTime;
    
    /* Starting time of the QR code validity timespan in seconds */
    @Setter
    @NotNull(message= QR_CODE_VALIDITY_START_TIME_VALIDATION_MESSAGE)
    protected Instant qrCodeValidityStartTime;
    
    /* Temporary location key for the period */
    @Setter
    @NotNull(message= LOCATION_TEMPORARY_SECRET_KEY_VALIDATION_MESSAGE)
    @Size(min = LOCATION_TEMPORARY_SECRET_KEY_SIZE, 
        max = LOCATION_TEMPORARY_SECRET_KEY_SIZE, message= LOCATION_TEMPORARY_SECRET_KEY_SIZE_VALIDATION_MESSAGE)
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
        if (Objects.isNull(periodStartTime)) {
           log.error("Period start time not set. Null value provided");
           return;
        }
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
