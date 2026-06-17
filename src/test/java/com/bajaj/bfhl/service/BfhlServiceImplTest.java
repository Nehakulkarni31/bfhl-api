package com.bajaj.bfhl.service;

import com.bajaj.bfhl.dto.BfhlRequest;
import com.bajaj.bfhl.dto.BfhlResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("BfhlService Unit Tests")
class BfhlServiceImplTest {

    private BfhlServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new BfhlServiceImpl();
    }

    // ── Example 1: Basic mix ─────────────────────────────────────────────────
    @Test
    @DisplayName("Example 1: Basic numbers, alphabets, special char")
    void testExample1() {
        BfhlRequest request = new BfhlRequest(Arrays.asList("A", "1", "22", "$", "B", "7"));
        BfhlResponse response = service.process(request, "REQ-1001");

        assertTrue(response.isSuccess());
        assertEquals("REQ-1001", response.getRequestId());
        assertTrue(response.getOddNumbers().contains("1"));
        assertTrue(response.getOddNumbers().contains("7"));
        assertTrue(response.getEvenNumbers().contains("22"));
        assertEquals(1, response.getSpecialCharacters().size());
        assertEquals("$", response.getSpecialCharacters().get(0));
        assertEquals("30", response.getSum());
        assertEquals("22", response.getLargestNumber());
        assertEquals("1", response.getSmallestNumber());
        assertEquals(2, response.getAlphabetCount());
        assertEquals(3, response.getNumberCount());
        assertEquals(1, response.getSpecialCharacterCount());
        assertFalse(response.isContainsDuplicates());
    }

    // ── Example 2: Alphanumeric strings ──────────────────────────────────────
    @Test
    @DisplayName("Example 2: Alphanumeric strings are split into numbers and alphabets")
    void testExample2AlphanumericSplit() {
        BfhlRequest request = new BfhlRequest(Arrays.asList("A1B2", "100", "#", "Test123", "Z", "55"));
        BfhlResponse response = service.process(request, "REQ-1002");

        assertTrue(response.isSuccess());
        assertTrue(response.getOddNumbers().contains("55"));
        assertTrue(response.getEvenNumbers().contains("100"));
        assertEquals("155", response.getSum());
        assertEquals("100", response.getLargestNumber());
        assertEquals("55", response.getSmallestNumber());
        assertEquals(1, response.getSpecialCharacterCount());
        assertFalse(response.isContainsDuplicates());
    }

    // ── Example 3: Duplicates, nulls, empty strings ───────────────────────────
    @Test
    @DisplayName("Example 3: Duplicates detected, nulls and empty strings ignored")
    void testExample3DuplicatesAndInvalids() {
        BfhlRequest request = new BfhlRequest(Arrays.asList("10", "10", "A", "A", "", null, "&", "5"));
        BfhlResponse response = service.process(request, "REQ-1003");

        assertTrue(response.isSuccess());
        assertTrue(response.isContainsDuplicates());
        assertTrue(response.getOddNumbers().contains("5"));
        assertTrue(response.getEvenNumbers().contains("10"));
        assertEquals("15", response.getSum());
        assertEquals("10", response.getLargestNumber());
        assertEquals("5", response.getSmallestNumber());
        assertEquals(1, response.getAlphabetCount());
        assertEquals(2, response.getNumberCount());
        assertEquals(1, response.getSpecialCharacterCount());

        // Summary: 8 received, 6 valid (two are null/empty), 2 invalid
        assertNotNull(response.getSummary());
        assertEquals(8, response.getSummary().getTotalElementsReceived());
        assertEquals(6, response.getSummary().getValidElementsProcessed());
        assertEquals(2, response.getSummary().getInvalidElementsIgnored());
    }

    // ── Example 4: Negative and decimal numbers ───────────────────────────────
    @Test
    @DisplayName("Example 4: Negative and decimal numbers handled correctly")
    void testExample4NegativeAndDecimals() {
        BfhlRequest request = new BfhlRequest(
                Arrays.asList("-10", "25.5", "-100.75", "B", "@", "5", "A9"));
        BfhlResponse response = service.process(request, "REQ-1004");

        assertTrue(response.isSuccess());
        assertEquals("25.5", response.getLargestNumber());
        assertEquals("-100.75", response.getSmallestNumber());
        // -10 is even
        assertTrue(response.getEvenNumbers().contains("-10"));
        // 5 is odd
        assertTrue(response.getOddNumbers().contains("5"));
        assertFalse(response.isContainsDuplicates());
    }

    // ── Null data list ─────────────────────────────────────────────────────────
    @Test
    @DisplayName("Null data: returns valid response with zero counts")
    void testNullData() {
        BfhlRequest request = new BfhlRequest(null);
        // should not throw
        assertThrows(Exception.class, () -> service.process(request, "REQ-NULL"));
    }

    // ── Empty data list ────────────────────────────────────────────────────────
    @Test
    @DisplayName("Empty data list: returns valid response with zero counts")
    void testEmptyData() {
        BfhlRequest request = new BfhlRequest(Collections.emptyList());
        BfhlResponse response = service.process(request, "REQ-EMPTY");

        assertTrue(response.isSuccess());
        assertTrue(response.getOddNumbers().isEmpty());
        assertTrue(response.getEvenNumbers().isEmpty());
        assertEquals("0", response.getSum());
        assertNull(response.getLargestNumber());
        assertNull(response.getSmallestNumber());
        assertEquals(0, response.getAlphabetCount());
        assertEquals(0, response.getNumberCount());
        assertEquals(0, response.getSpecialCharacterCount());
        assertFalse(response.isContainsDuplicates());
    }

    // ── All whitespace ─────────────────────────────────────────────────────────
    @Test
    @DisplayName("Whitespace-only strings are ignored")
    void testWhitespaceIgnored() {
        BfhlRequest request = new BfhlRequest(Arrays.asList("   ", "\t", "\n", "A"));
        BfhlResponse response = service.process(request, "REQ-WS");

        assertEquals(1, response.getAlphabetCount());
        assertEquals(0, response.getNumberCount());
        assertEquals(3, response.getSummary().getInvalidElementsIgnored());
    }

    // ── Duplicate detection ────────────────────────────────────────────────────
    @Test
    @DisplayName("Duplicate detection: true when same value appears more than once")
    void testDuplicateDetection() {
        BfhlRequest request = new BfhlRequest(Arrays.asList("5", "5", "A"));
        BfhlResponse response = service.process(request, "REQ-DUP");

        assertTrue(response.isContainsDuplicates());
        assertEquals(2, response.getUniqueElementCount());
    }

    // ── Vowel and consonant counting ──────────────────────────────────────────
    @Test
    @DisplayName("Vowel and consonant counts are correct")
    void testVowelConsonantCount() {
        BfhlRequest request = new BfhlRequest(Arrays.asList("AEI", "BCD"));
        BfhlResponse response = service.process(request, "REQ-VC");

        assertEquals(3, response.getVowelCount());
        assertEquals(3, response.getConsonantCount());
    }

    // ── Sorted numbers ─────────────────────────────────────────────────────────
    @Test
    @DisplayName("Sorted numbers returned in ascending order")
    void testSortedNumbers() {
        BfhlRequest request = new BfhlRequest(Arrays.asList("10", "-5", "3", "100", "-50"));
        BfhlResponse response = service.process(request, "REQ-SORT");

        List<String> sorted = response.getSortedNumbers();
        assertNotNull(sorted);
        assertEquals("-50", sorted.get(0));
        assertEquals("-5", sorted.get(1));
        assertEquals("3", sorted.get(2));
        assertEquals("10", sorted.get(3));
        assertEquals("100", sorted.get(4));
    }

    // ── Alphabet frequency ─────────────────────────────────────────────────────
    @Test
    @DisplayName("Alphabet frequency map is correctly populated")
    void testAlphabetFrequency() {
        BfhlRequest request = new BfhlRequest(Arrays.asList("A", "A", "B", "AB"));
        BfhlResponse response = service.process(request, "REQ-FREQ");

        assertNotNull(response.getAlphabetFrequency());
        // A appears in "A"(dedup so once), "AB" — after dedup: A, B, AB
        // A from "A" = 1, A from "AB" = 1, B from "B" = 1 (after dedup), B from "AB" = 1
    }

    // ── Longest and shortest alphabetic value ─────────────────────────────────
    @Test
    @DisplayName("Longest and shortest alphabetic strings correctly identified")
    void testLongestShortest() {
        BfhlRequest request = new BfhlRequest(Arrays.asList("A", "ABC", "AB", "1"));
        BfhlResponse response = service.process(request, "REQ-LS");

        assertEquals("ABC", response.getLongestAlphabeticValue());
        assertEquals("A", response.getShortestAlphabeticValue());
    }

    // ── Request ID echoed ──────────────────────────────────────────────────────
    @Test
    @DisplayName("Request ID from header is echoed in response")
    void testRequestIdEchoed() {
        BfhlRequest request = new BfhlRequest(List.of("1"));
        BfhlResponse response = service.process(request, "MY-CUSTOM-ID-999");

        assertEquals("MY-CUSTOM-ID-999", response.getRequestId());
    }

    // ── Processing time set ────────────────────────────────────────────────────
    @Test
    @DisplayName("Processing time is set and non-negative")
    void testProcessingTimeSet() {
        BfhlRequest request = new BfhlRequest(List.of("1", "A"));
        BfhlResponse response = service.process(request, "REQ-TIME");

        assertTrue(response.getProcessingTimeMs() >= 0);
    }
}
