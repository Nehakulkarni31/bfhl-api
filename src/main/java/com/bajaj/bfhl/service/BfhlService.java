package com.bajaj.bfhl.service;

import com.bajaj.bfhl.dto.BfhlRequest;
import com.bajaj.bfhl.dto.BfhlResponse;

public interface BfhlService {
    BfhlResponse process(BfhlRequest request, String requestId);
}
