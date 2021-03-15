package fr.inria.clea.lsp.utils;

import java.time.Instant;

public class TimeUtils {
    public static final int NB_SECONDS_PER_HOUR = 3600;
    //  Number of seconds to fill the gap between UNIX timestamp (1/1/1970) and NTP timestamp (1/1/1900)
    public final static long SECONDS_FROM_01_01_1900_TO_01_01_1970 = 2208988800L;
    
    /**
     * @return the current time expressed as the number of seconds since January 1st, 1900 
     * (NTP timestamp), by convention in the UTC (Coordinated Universal Time) timezone.
     */
    public static long currentNtpTime() {
        return ntpTimestampFromInstant(Instant.now());
    }
    
    public static long ntpTimestampFromInstant(Instant instant) {
        return instant.getEpochSecond() + SECONDS_FROM_01_01_1900_TO_01_01_1970;
    }

    public static Instant instantFromTimestamp(long ntpTimestamp) {
        return Instant.ofEpochSecond(ntpTimestamp - SECONDS_FROM_01_01_1900_TO_01_01_1970);
    }

    /**
     * Get timestamp rounded to the closest hour.
     * @param timestamp the timestamp in seconds
     * @return the rounded timestamp
     */
    public static long hourRoundedTimestamp(long timestamp) {
        long timestampPlusHalfTimeRounding = timestamp + NB_SECONDS_PER_HOUR/2;
        return timestampPlusHalfTimeRounding - (timestampPlusHalfTimeRounding % NB_SECONDS_PER_HOUR);
    }
    
    /**
     * @return the current timestamp in seconds rounded to the closest hour.
     */
    public static long hourRoundedCurrentTimeTimestamp() {
        return hourRoundedTimestamp(currentNtpTime());
    }

    /**
     * @return the current timestamp in seconds rounded to the closest hour 
     *      limited to 32 bits (java int).
     */
    public static int hourRoundedCurrentTimeTimestamp32() {
        return (int) hourRoundedCurrentTimeTimestamp();
    }
}
