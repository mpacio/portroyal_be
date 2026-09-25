package com.matteopaciolla.prbe.util;

import org.springframework.stereotype.Component;

import java.math.BigInteger;
import java.util.Map;
import java.util.TreeMap;

@Component
public class Base36StringUtils {
    // Custom base36 characters for encoding
    private static final char[] BASE36_ALPHABET = "0123456789abcdefghijklmnopqrstuvwxyz".toCharArray();
    private static final char[] BASE36_ALPHABET_SHUFFLED = "is2huvx4okc09m1lawjtnp5ygfdq3z8e7rb6".toCharArray();

    // Create a mapping from the base36 alphabet to the shuffled base36 alphabet
    private static final Map<Character, Character> BASE36_ALPHABET_MAPPING = new TreeMap<>();
    static {
        for (int i = 0; i < BASE36_ALPHABET.length; i++) {
            BASE36_ALPHABET_MAPPING.put(BASE36_ALPHABET[i], BASE36_ALPHABET_SHUFFLED[i]);
        }
    }

    //create a mapping from the shuffled base36 alphabet to the base36 alphabet
    private static final Map<Character, Character> BASE36_ALPHABET_MAPPING_INVERSE = new TreeMap<>();
    static {
        for (int i = 0; i < BASE36_ALPHABET_SHUFFLED.length; i++) {
            BASE36_ALPHABET_MAPPING_INVERSE.put(BASE36_ALPHABET_SHUFFLED[i], BASE36_ALPHABET[i]);
        }
    }

    // Converts a BigInteger to a string matching the regex ^[0-9a-z]+$
    public static String bigIntegerToString(BigInteger bigInt) {
        if (bigInt == null) {
            throw new IllegalArgumentException("BigInteger cannot be null");
        }
        // Convert and return the BigInteger to a base 36 string
        return bigInt.toString(36);
    }

    // Converts a string matching the regex ^[0-9a-z]+$ to a BigInteger
    public static BigInteger stringToBigInteger(String input) {
        if (input == null || !input.matches("^[0-9a-z]+$")) {
            throw new IllegalArgumentException("Input must be a string matching the regex ^[0-9a-z]+$");
        }
        // Convert the base 36 string back to a BigInteger
        return new BigInteger(input, 36);
    }

    // Encodes the current millisecond timestamp to a base36 string
    public static String encodeTimestamp() {
        return bigIntegerToString(BigInteger.valueOf(System.currentTimeMillis()));
    }

    // Encodes a millisecond timestamp to a base36 string
    public static String encodeTimestamp(long timestamp) {
        return bigIntegerToString(BigInteger.valueOf(timestamp));
    }

    // Decodes a base36 string to the original millisecond timestamp
    public static long decodeTimestamp(String input) {
        return stringToBigInteger(input).longValue();
    }

    // Create a shuffled base36 string from a base36 string
    public static String shuffleBase36(String input) {
        if (input == null || !input.matches("^[0-9a-z]+$")) {
            throw new IllegalArgumentException("Input must be a string matching the regex ^[0-9a-z]+$");
        }
        // Create a new StringBuilder to store the shuffled base36 string
        StringBuilder shuffled = new StringBuilder();
        // Iterate through each character in the input string
        for (char c : input.toCharArray()) {
            // Append the shuffled character to the StringBuilder
            shuffled.append(BASE36_ALPHABET_MAPPING.get(c));
        }
        // Return the shuffled base36 string
        return shuffled.toString();
    }

    // Create a base36 shuffled string from the current millisecond timestamp
    public static String shuffleTimestamp() {
        return shuffleBase36(encodeTimestamp());
    }

    // Create a base36 shuffled string from a millisecond timestamp
    public static String shuffleTimestamp(long timestamp) {
        return shuffleBase36(encodeTimestamp(timestamp));
    }

    // Unshuffle a shuffled base36 string to the original base36 string
    public static String unshuffleBase36(String input) {
        if (input == null || !input.matches("^[0-9a-z]+$")) {
            throw new IllegalArgumentException("Input must be a string matching the regex ^[0-9a-z]+$");
        }
        // Create a new StringBuilder to store the unshuffled base36 string
        StringBuilder unshuffled = new StringBuilder();
        // Iterate through each character in the input string
        for (char c : input.toCharArray()) {
            // Append the unshuffled character to the StringBuilder
            unshuffled.append(BASE36_ALPHABET_MAPPING_INVERSE.get(c));
        }
        // Return the unshuffled base36 string
        return unshuffled.toString();
    }

    // Unshuffle a shuffled base36 timestamp to the original millisecond timestamp
    public static long unshuffleTimestamp(String input) {
        return decodeTimestamp(unshuffleBase36(input));
    }
}
