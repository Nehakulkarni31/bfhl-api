package com.bajaj.bfhl.service;

import com.bajaj.bfhl.dto.BfhlRequest;
import com.bajaj.bfhl.dto.BfhlResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class BfhlServiceImpl implements BfhlService {

    private static final Logger log = LoggerFactory.getLogger(BfhlServiceImpl.class);
    private static final Set<Character> VOWELS = Set.of('A', 'E', 'I', 'O', 'U');

    @Override
    public BfhlResponse process(BfhlRequest request, String requestId) {
        long startTime = System.currentTimeMillis();
        log.info("Processing request: requestId={}, dataSize={}", requestId,
                request.getData() == null ? 0 : request.getData().size());

        // ── 1. Raw element count before cleaning ──────────────────────────────
        int totalReceived = request.getData() == null ? 0 : request.getData().size();

        // ── 2. Clean: filter null, blank, whitespace-only ────────────────────
        List<String> raw = new ArrayList<>();
        if (request.getData() != null) {
            for (Object item : request.getData()) {
                if (item == null) continue;
                String s = item.toString().trim();
                if (s.isEmpty()) continue;
                raw.add(s);
            }
        }
        int validCount = raw.size();
        int invalidCount = totalReceived - validCount;

        // ── 3. Duplicate detection (before dedup) ────────────────────────────
        Set<String> seen = new LinkedHashSet<>();
        boolean containsDuplicates = false;
        for (String s : raw) {
            if (!seen.add(s)) {
                containsDuplicates = true;
            }
        }
        List<String> deduplicated = new ArrayList<>(seen);
        int uniqueElementCount = deduplicated.size();

        // ── 4. Classify each element ──────────────────────────────────────────
        List<BigDecimal> numbers = new ArrayList<>();
        List<String> alphabeticStrings = new ArrayList<>();  // whole words like "ABC", "xyz"
        List<String> individualAlphabets = new ArrayList<>(); // individual chars (for count/freq)
        List<String> specialCharacters = new ArrayList<>();

        for (String element : deduplicated) {
            String type = classify(element);
            switch (type) {
                case "NUMBER" -> numbers.add(new BigDecimal(element));
                case "ALPHA" -> {
                    alphabeticStrings.add(element.toUpperCase());
                    for (char c : element.toCharArray()) {
                        individualAlphabets.add(String.valueOf(c).toUpperCase());
                    }
                }
                case "SPECIAL" -> specialCharacters.add(element);
                case "ALPHANUMERIC" -> {
                    // Extract numbers and alphabets from alphanumeric strings
                    StringBuilder numPart = new StringBuilder();
                    StringBuilder alphaPart = new StringBuilder();
                    boolean negative = element.startsWith("-");
                    String toScan = negative ? element.substring(1) : element;

                    for (char c : toScan.toCharArray()) {
                        if (Character.isDigit(c) || c == '.') {
                            numPart.append(c);
                        } else if (Character.isLetter(c)) {
                            alphaPart.append(c);
                            individualAlphabets.add(String.valueOf(c).toUpperCase());
                        }
                    }
                    if (numPart.length() > 0) {
                        try {
                            String numStr = negative ? "-" + numPart : numPart.toString();
                            numbers.add(new BigDecimal(numStr));
                        } catch (NumberFormatException ignored) {}
                    }
                    if (alphaPart.length() > 0) {
                        alphabeticStrings.add(alphaPart.toString().toUpperCase());
                    }
                }
            }
        }

        // ── 5. Numbers processing ──────────────────────────────────────────────
        List<String> oddNumbers = new ArrayList<>();
        List<String> evenNumbers = new ArrayList<>();
        BigDecimal sum = BigDecimal.ZERO;

        for (BigDecimal n : numbers) {
            sum = sum.add(n);
            // For odd/even: use the integer part
            BigDecimal intPart = n.setScale(0, RoundingMode.DOWN);
            if (intPart.remainder(new BigDecimal("2")).abs().compareTo(BigDecimal.ZERO) != 0) {
                oddNumbers.add(formatNumber(n));
            } else {
                evenNumbers.add(formatNumber(n));
            }
        }

        String sumStr = formatNumber(sum);

        // Sorted numbers
        List<BigDecimal> sortedNums = new ArrayList<>(numbers);
        Collections.sort(sortedNums);
        List<String> sortedNumbers = sortedNums.stream()
                .map(this::formatNumber)
                .collect(Collectors.toList());

        // Largest & smallest
        String largestNumber = null;
        String smallestNumber = null;
        if (!numbers.isEmpty()) {
            largestNumber = formatNumber(Collections.max(numbers));
            smallestNumber = formatNumber(Collections.min(numbers));
        }

        // ── 6. Alphabet processing ─────────────────────────────────────────────
        // alphabet_frequency: count each individual letter across all inputs
        Map<String, Integer> alphabetFrequency = new TreeMap<>();
        for (String ch : individualAlphabets) {
            alphabetFrequency.merge(ch, 1, Integer::sum);
        }

        // vowel_count and consonant_count from individual alphabets
        int vowelCount = 0;
        int consonantCount = 0;
        for (String ch : individualAlphabets) {
            char c = ch.charAt(0);
            if (VOWELS.contains(c)) vowelCount++;
            else consonantCount++;
        }

        // longest and shortest alphabetic string (from pure-alpha words/chars)
        String longestAlphabeticValue = null;
        String shortestAlphabeticValue = null;
        if (!alphabeticStrings.isEmpty()) {
            longestAlphabeticValue = alphabeticStrings.stream()
                    .max(Comparator.comparingInt(String::length))
                    .orElse(null);
            shortestAlphabeticValue = alphabeticStrings.stream()
                    .min(Comparator.comparingInt(String::length))
                    .orElse(null);
        }

        // ── 7. Build response ──────────────────────────────────────────────────
        BfhlResponse response = new BfhlResponse();
        response.setSuccess(true);
        response.setRequestId(requestId);
        response.setOddNumbers(oddNumbers);
        response.setEvenNumbers(evenNumbers);
        response.setAlphabets(alphabeticStrings.isEmpty() ? individualAlphabets : alphabeticStrings);
        response.setSpecialCharacters(specialCharacters);
        response.setSum(sumStr);
        response.setLargestNumber(largestNumber);
        response.setSmallestNumber(smallestNumber);
        response.setAlphabetCount(individualAlphabets.size());
        response.setNumberCount(numbers.size());
        response.setSpecialCharacterCount(specialCharacters.size());
        response.setContainsDuplicates(containsDuplicates);
        response.setUniqueElementCount(uniqueElementCount);
        response.setSortedNumbers(sortedNumbers);
        response.setVowelCount(vowelCount);
        response.setConsonantCount(consonantCount);
        response.setAlphabetFrequency(alphabetFrequency.isEmpty() ? null : alphabetFrequency);
        response.setLongestAlphabeticValue(longestAlphabeticValue);
        response.setShortestAlphabeticValue(shortestAlphabeticValue);
        response.setSummary(new BfhlResponse.Summary(totalReceived, validCount, invalidCount));

        long processingTime = System.currentTimeMillis() - startTime;
        response.setProcessingTimeMs(processingTime);

        log.info("Completed request: requestId={}, processingTimeMs={}", requestId, processingTime);
        return response;
    }

    /**
     * Classify a non-null, non-blank string into:
     * NUMBER, ALPHA, SPECIAL, ALPHANUMERIC
     */
    private String classify(String s) {
        // Pure number: optional minus, digits, optional decimal
        if (s.matches("-?\\d+(\\.\\d+)?")) return "NUMBER";

        // Pure alpha: only letters (length >= 1)
        if (s.matches("[a-zA-Z]+")) return "ALPHA";

        // Single special character
        if (s.length() == 1 && !Character.isLetterOrDigit(s.charAt(0))) return "SPECIAL";

        // Multi-char special (e.g., "##") — treat as special
        if (s.chars().noneMatch(Character::isLetterOrDigit)) return "SPECIAL";

        // Alphanumeric: contains both letters and digits (and possibly special chars)
        return "ALPHANUMERIC";
    }

    /**
     * Format BigDecimal: strip trailing zeros, but keep at least integer format
     */
    private String formatNumber(BigDecimal n) {
        if (n == null) return null;
        // strip trailing zeros after decimal point
        BigDecimal stripped = n.stripTrailingZeros();
        // if scale is negative (e.g., 1E+2), use plain string
        return stripped.scale() <= 0 ? stripped.toPlainString() : stripped.toPlainString();
    }
}
