package com.bajaj.bfhl.service;

import com.bajaj.bfhl.dto.BfhlRequest;
import com.bajaj.bfhl.dto.BfhlResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
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

        // ── 1. Count before cleaning ──────────────────────────────────────────
        int totalReceived = request.getData() == null ? 0 : request.getData().size();

        // ── 2. Clean: remove null, empty, whitespace-only ─────────────────────
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

        // ── 3. Deduplicate (preserve order, detect duplicates) ────────────────
        Set<String> seen = new LinkedHashSet<>();
        boolean containsDuplicates = false;
        for (String s : raw) {
            if (!seen.add(s)) containsDuplicates = true;
        }
        List<String> deduped = new ArrayList<>(seen);
        int uniqueElementCount = deduped.size();

        // ── 4. Classify and accumulate ────────────────────────────────────────
        List<BigDecimal> numbers = new ArrayList<>();
        List<String> alphabetsField = new ArrayList<>();    // for response "alphabets" field
        List<String> individualChars = new ArrayList<>();   // for count/freq/vowels
        List<String> specialCharacters = new ArrayList<>();

        for (String element : deduped) {
            String type = classify(element);
            switch (type) {
                case "NUMBER" -> numbers.add(new BigDecimal(element));

                case "ALPHA" -> {
                    // Pure alpha string: keep as whole word in alphabets field
                    alphabetsField.add(element.toUpperCase());
                    for (char c : element.toCharArray()) {
                        individualChars.add(String.valueOf(c).toUpperCase());
                    }
                }

                case "SPECIAL" -> specialCharacters.add(element);

                case "ALPHANUMERIC" -> {
                    // Extract contiguous letter groups (e.g. "Test99"→"TEST", "A1B2"→"A","B")
                    // Digits do NOT contribute to numbers per spec examples
                    StringBuilder group = new StringBuilder();
                    for (char c : element.toCharArray()) {
                        if (Character.isLetter(c)) {
                            group.append(c);
                        } else {
                            if (group.length() > 0) {
                                String g = group.toString().toUpperCase();
                                alphabetsField.add(g);
                                for (char lc : g.toCharArray()) {
                                    individualChars.add(String.valueOf(lc));
                                }
                                group.setLength(0);
                            }
                        }
                    }
                    // flush remaining group
                    if (group.length() > 0) {
                        String g = group.toString().toUpperCase();
                        alphabetsField.add(g);
                        for (char lc : g.toCharArray()) {
                            individualChars.add(String.valueOf(lc));
                        }
                    }
                }
            }
        }

        // ── 5. Process numbers ────────────────────────────────────────────────
        // Only INTEGER values go into odd/even classification (decimals excluded)
        List<String> oddNumbers = new ArrayList<>();
        List<String> evenNumbers = new ArrayList<>();
        BigDecimal sum = BigDecimal.ZERO;

        for (BigDecimal n : numbers) {
            sum = sum.add(n);
            // Only classify as odd/even if it has no fractional part
            if (n.stripTrailingZeros().scale() <= 0) {
                // Safely check odd/even for arbitrarily large numbers to prevent ArithmeticException
                if (n.remainder(new BigDecimal("2")).compareTo(BigDecimal.ZERO) == 0) {
                    evenNumbers.add(formatNumber(n));
                } else {
                    oddNumbers.add(formatNumber(n));
                }
            }
            // decimal numbers contribute to sum/largest/smallest but not odd/even
        }

        String sumStr = formatNumber(sum);
        String largestNumber = numbers.isEmpty() ? null : formatNumber(Collections.max(numbers));
        String smallestNumber = numbers.isEmpty() ? null : formatNumber(Collections.min(numbers));

        List<String> sortedNumbers = numbers.stream()
                .sorted()
                .map(this::formatNumber)
                .collect(Collectors.toList());

        // ── 6. Alphabet stats ─────────────────────────────────────────────────
        Map<String, Integer> alphabetFrequency = new TreeMap<>();
        for (String ch : individualChars) {
            alphabetFrequency.merge(ch, 1, Integer::sum);
        }

        int vowelCount = 0, consonantCount = 0;
        for (String ch : individualChars) {
            if (VOWELS.contains(ch.charAt(0))) vowelCount++;
            else consonantCount++;
        }

        String longestAlpha = alphabetsField.isEmpty() ? null :
                alphabetsField.stream().max(Comparator.comparingInt(String::length)).orElse(null);
        String shortestAlpha = alphabetsField.isEmpty() ? null :
                alphabetsField.stream().min(Comparator.comparingInt(String::length)).orElse(null);

        // ── 7. Build response ─────────────────────────────────────────────────
        BfhlResponse response = new BfhlResponse();
        response.setSuccess(true);
        response.setRequestId(requestId);
        response.setOddNumbers(oddNumbers);
        response.setEvenNumbers(evenNumbers);
        response.setAlphabets(alphabetsField);
        response.setSpecialCharacters(specialCharacters);
        response.setSum(sumStr);
        response.setLargestNumber(largestNumber);
        response.setSmallestNumber(smallestNumber);
        response.setAlphabetCount(individualChars.size());
        response.setNumberCount(numbers.size());
        response.setSpecialCharacterCount(specialCharacters.size());
        response.setContainsDuplicates(containsDuplicates);
        response.setUniqueElementCount(uniqueElementCount);
        response.setSortedNumbers(sortedNumbers);
        response.setVowelCount(vowelCount);
        response.setConsonantCount(consonantCount);
        response.setAlphabetFrequency(alphabetFrequency.isEmpty() ? null : alphabetFrequency);
        response.setLongestAlphabeticValue(longestAlpha);
        response.setShortestAlphabeticValue(shortestAlpha);
        response.setSummary(new BfhlResponse.Summary(totalReceived, validCount, invalidCount));
        response.setProcessingTimeMs(System.currentTimeMillis() - startTime);

        log.info("Completed: requestId={}", requestId);
        return response;
    }

    private String classify(String s) {
        if (s.matches("-?\\d+(\\.\\d+)?")) return "NUMBER";
        if (s.matches("[a-zA-Z]+")) return "ALPHA";
        if (s.length() == 1 && !Character.isLetterOrDigit(s.charAt(0))) return "SPECIAL";
        if (s.chars().noneMatch(Character::isLetterOrDigit)) return "SPECIAL";
        return "ALPHANUMERIC";
    }

    private String formatNumber(BigDecimal n) {
        if (n == null) return null;
        return n.stripTrailingZeros().toPlainString();
    }
}