package com.matteopaciolla.prbe.util;

import org.junit.jupiter.api.Test;

import java.math.BigInteger;
import java.time.Instant;
import java.util.Date;

import static org.junit.jupiter.api.Assertions.*;

class Base36StringUtilsTest {

    @Test
    void bigIntegerToString_shouldConvertBigIntegerToBase36String() {
        BigInteger bigInt = new BigInteger("1234567890");
        String result = Base36StringUtils.bigIntegerToString(bigInt);
        assertEquals("kf12oi", result);
    }

    @Test
    void bigIntegerToString_shouldThrowExceptionWhenNull() {
        assertThrows(IllegalArgumentException.class, () -> Base36StringUtils.bigIntegerToString(null));
    }

    @Test
    void stringToBigInteger_shouldConvertBase36StringToBigInteger() {
        String input = "kf12oi";
        BigInteger result = Base36StringUtils.stringToBigInteger(input);
        assertEquals(new BigInteger("1234567890"), result);
    }

    @Test
    void stringToBigInteger_shouldThrowExceptionWhenNull() {
        assertThrows(IllegalArgumentException.class, () -> Base36StringUtils.stringToBigInteger(null));
    }

    @Test
    void stringToBigInteger_shouldThrowExceptionWhenNotMatchingRegex() {
        assertThrows(IllegalArgumentException.class, () -> Base36StringUtils.stringToBigInteger("kf12oi!"));
    }

    @Test
    void shuffleBase36_shouldReturnShuffledBase36String() {
        String input = "kf12oi";
        String result = Base36StringUtils.shuffleBase36(input);
        assertEquals("nls2gj", result);
    }

    @Test
    void shuffleBase36_shouldThrowExceptionWhenNull() {
        assertThrows(IllegalArgumentException.class, () -> Base36StringUtils.shuffleBase36(null));
    }

    @Test
    void shuffleBase36_shouldThrowExceptionWhenNotMatchingRegex() {
        assertThrows(IllegalArgumentException.class, () -> Base36StringUtils.shuffleBase36("kf12oi!"));
    }

    @Test
    void unshuffleBase36_shouldReturnOriginalBase36String() {
        String input = "kf12oi";
        String shuffled = Base36StringUtils.shuffleBase36(input);
        String result = Base36StringUtils.unshuffleBase36(shuffled);
        assertEquals(input, result);
    }

    @Test
    void unshuffleBase36_shouldThrowExceptionWhenNull() {
        assertThrows(IllegalArgumentException.class, () -> Base36StringUtils.unshuffleBase36(null));
    }

    @Test
    void unshuffleBase36_shouldThrowExceptionWhenNotMatchingRegex() {
        assertThrows(IllegalArgumentException.class, () -> Base36StringUtils.unshuffleBase36("kf12oi!"));
    }

    @Test
    void encodeTimestamp_shouldReturnBase36String() {
        String result = Base36StringUtils.encodeTimestamp();
        assertNotNull(result);
    }

    @Test
    void decodeTimestamp_shouldReturnLong() {
        String input = Base36StringUtils.encodeTimestamp();
        long result = Base36StringUtils.decodeTimestamp(input);
        assertTrue(result > 0);
    }

    @Test
    void shuffleTimestamp_shouldReturnShuffledBase36String() {
        long currentTimestamp = System.currentTimeMillis();
        // This test creates a shuffled base36 string from the current millisecond timestamp
        String shuffledTimestamp = Base36StringUtils.shuffleTimestamp(currentTimestamp);
        // Print the current millisecond timestamp and the shuffled base36 string
        System.out.println("Current millisecond timestamp: " + currentTimestamp);
        // Print a string representation of the date time from the current millisecond timestamp formatted as "yyyy-MM-dd HH:mm:ss"
        System.out.println("Current date time: " + Date.from(Instant.ofEpochMilli(currentTimestamp)));
        System.out.println("Shuffled base36 timestamp: " + shuffledTimestamp);
        // Print a string representation of the date time from the shuffled base36 string formatted as "yyyy-MM-dd HH:mm:ss"
        System.out.println("Shuffled date time: " + Date.from(Instant.ofEpochMilli(Base36StringUtils.decodeTimestamp(shuffledTimestamp))));
        // Print the unshuffled base36 string from the shuffled base36 string
        System.out.println("Unshuffled base36 timestamp: " + Base36StringUtils.unshuffleBase36(shuffledTimestamp));
        // Print the unshuffled date time from the shuffled base36 string
        System.out.println("Unshuffled date time: " + Date.from(Instant.ofEpochMilli(Base36StringUtils.unshuffleTimestamp(shuffledTimestamp))));

        // Print the shuffled timestamp of the given date time 2060-01-01 00:00:00
        long timestamp = Instant.parse("2060-01-01T00:00:00Z").toEpochMilli();
        String shuffledTimestamp2060 = Base36StringUtils.shuffleTimestamp(timestamp);
        System.out.println("Shuffled base36 timestamp of 2060-01-01 00:00:00: " + shuffledTimestamp2060);

        // Print the unshuffled timestamp of the string "66666666"
        String unshuffledTimestamp = Base36StringUtils.unshuffleBase36("66666666");
        System.out.println("Unshuffled base36 timestamp of 66666666: " + unshuffledTimestamp);

        //Print the date time of the unshuffled timestamp
        System.out.println("Unshuffled date time of 66666666: " + Date.from(Instant.ofEpochMilli(Base36StringUtils.decodeTimestamp(unshuffledTimestamp))));
    }
}