package fr.inria.clea.lsp;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

import java.time.Instant;

import org.junit.jupiter.api.Test;

import fr.inria.clea.lsp.utils.TimeUtils;

public class TimeUtilsTest {
    @Test
    public void testCanGetCurrentNtpTime() {
        long now = System.currentTimeMillis() / 1000 + TimeUtils.SECONDS_FROM_01_01_1900_TO_01_01_1970;
        assertThat(TimeUtils.currentNtpTime()).isCloseTo(now, within(2L));
    }
    
    @Test
    public void testCanGetNtpTimestampFromInstant() {
        Instant instant = Instant.parse("1970-01-01T00:00:00.00Z");
        assertThat(TimeUtils.ntpTimestampFromInstant(instant)).isEqualTo(TimeUtils.SECONDS_FROM_01_01_1900_TO_01_01_1970);
    }
    
    @Test
    public void testCanGetInstantFromTimestamp() {
        assertThat(TimeUtils.instantFromTimestamp(TimeUtils.SECONDS_FROM_01_01_1900_TO_01_01_1970)).isEqualTo("1970-01-01T00:00:00.00Z");
    }

    @Test
    public void testTimestampIsRoundedDownToTheNextLongWhenModuloLowerThanHalfTimeRoundingValue() {
        assertThat(TimeUtils.hourRoundedTimestamp(1606141610)).isEqualTo(1606140000);
    }

    @Test
    public void testTimestampIsRoundedUpToTheNextLongWhenModuloGreaterThanHalfTimeRoundingValue() {
        assertThat(TimeUtils.hourRoundedTimestamp(1606142300)).isEqualTo(1606143600);
    }

    @Test
    public void testTimestampIsNotRoundedWhenMultipleOfTimeRoundingValue() {
        assertThat(TimeUtils.hourRoundedTimestamp(1606140000)).isEqualTo(1606140000);
    }

    @Test
    public void testTimestampIsRoundedUpToTheNextLongWhenModuloIsEqualToHalfTimeRoundingValue() {
        assertThat(TimeUtils.hourRoundedTimestamp(1606141800)).isEqualTo(1606143600);
    }

    @Test
    public void testTimestampFromInstantIsRoundedDownWhenModuloLowerThanHalfTimeRoundingValue(){
        Instant instant = Instant.parse("1970-01-01T00:10:00.00Z");
        assertThat(TimeUtils.hourRoundedTimeTimestampFromInstant(instant)).isEqualTo(TimeUtils.SECONDS_FROM_01_01_1900_TO_01_01_1970);
    }

    @Test
    public void testTimestampFromInstantIsRoundedUpWhenModuloGreaterThanHalfTimeRoundingValue(){
        Instant instant = Instant.parse("1969-12-31T23:50:00.00Z");
        assertThat(TimeUtils.hourRoundedTimeTimestampFromInstant(instant)).isEqualTo(TimeUtils.SECONDS_FROM_01_01_1900_TO_01_01_1970);
    }

    @Test
    public void testTimestampFromInstantIsNotRoundedWhenMultipleOfTimeRoundingValue(){
        Instant instant = Instant.parse("1970-01-01T00:00:00.00Z");
        assertThat(TimeUtils.hourRoundedTimeTimestampFromInstant(instant)).isEqualTo(TimeUtils.SECONDS_FROM_01_01_1900_TO_01_01_1970);
    }
    
    @Test
    public void testTimestampFromInstantIsRoundedUpToTheNextLongWhenModuloIsEqualToHalfTimeRoundingValue(){
        Instant instant = Instant.parse("1969-12-31T23:30:00.00Z");
        assertThat(TimeUtils.hourRoundedTimeTimestampFromInstant(instant)).isEqualTo(TimeUtils.SECONDS_FROM_01_01_1900_TO_01_01_1970);
    }

    @Test
    public void testIntTimestampFromInstantIsRoundedDownWhenModuloLowerThanHalfTimeRoundingValue(){
        Instant instant = Instant.parse("1970-01-01T00:10:00.00Z");
        assertThat(TimeUtils.hourRoundedTimeTimestamp32FromInstant(instant)).isEqualTo((int)TimeUtils.SECONDS_FROM_01_01_1900_TO_01_01_1970);
    }

    @Test
    public void testIntTimestampFromInstantIsRoundedUpWhenModuloGreaterThanHalfTimeRoundingValue(){
        Instant instant = Instant.parse("1969-12-31T23:50:00.00Z");
        assertThat(TimeUtils.hourRoundedTimeTimestamp32FromInstant(instant)).isEqualTo((int)TimeUtils.SECONDS_FROM_01_01_1900_TO_01_01_1970);
    }

    @Test
    public void testIntTimestampFromInstantIsNotRoundedWhenMultipleOfTimeRoundingValue(){
        Instant instant = Instant.parse("1970-01-01T00:00:00.00Z");
        assertThat(TimeUtils.hourRoundedTimeTimestamp32FromInstant(instant)).isEqualTo((int)TimeUtils.SECONDS_FROM_01_01_1900_TO_01_01_1970);
    }
    
    @Test
    public void testIntTimestampFromInstantIsRoundedUpToTheNextLongWhenModuloIsEqualToHalfTimeRoundingValue(){
        Instant instant = Instant.parse("1969-12-31T23:30:00.00Z");
        assertThat(TimeUtils.hourRoundedTimeTimestamp32FromInstant(instant)).isEqualTo((int)TimeUtils.SECONDS_FROM_01_01_1900_TO_01_01_1970);
    }
}
