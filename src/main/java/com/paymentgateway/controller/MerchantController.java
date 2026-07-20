package com.paymentgateway.controller;

import com.paymentgateway.dto.request.MerchantRegistrationRequest;
import com.paymentgateway.dto.response.MerchantRegistrationResponse;
import com.paymentgateway.service.MerchantService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/merchants")
@RequiredArgsConstructor
@Slf4j
public class MerchantController {

    private final MerchantService merchantService;

    @PostMapping("/register")
    public ResponseEntity<MerchantRegistrationResponse> registerMerchant(
            @Valid @RequestBody MerchantRegistrationRequest request) {

        log.info("Received merchant registration request for email: {}",
                request.getEmail());

        MerchantRegistrationResponse response =
                merchantService.registerMerchant(request);

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
}
