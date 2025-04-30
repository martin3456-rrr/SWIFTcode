package swiftcodeapp.web.controller;

import jakarta.validation.Valid;
import swiftcodeapp.service.SwiftCodeService;
import swiftcodeapp.web.dto.CountrySwiftCodesResponse;
import swiftcodeapp.web.dto.SwiftCodeDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;


import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/v1/swift-codes")
public class SwiftCodeController {

    private static final Logger log = LoggerFactory.getLogger(SwiftCodeController.class);

    @Autowired
    private SwiftCodeService swiftCodeService;

    @GetMapping("/{swiftCode}")
    public ResponseEntity<?> getSwiftCodeDetails(@PathVariable String swiftCode) {
        log.info("Received request to get details for SWIFT code: {}", swiftCode);
        Optional<Object> details = swiftCodeService.getSwiftCodeDetails(swiftCode);
        return details.<ResponseEntity<?>>map(ResponseEntity::ok)
                .orElseGet(() -> {
                    log.warn("SWIFT code not found: {}", swiftCode);
                    return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("message", "SWIFT code not found"));
                });
    }

    @GetMapping("/country/{countryISO2code}")
    public ResponseEntity<?> getSwiftCodesByCountry(@PathVariable String countryISO2code) {
        log.info("Received request to get SWIFT codes for country: {}", countryISO2code);
        Optional<CountrySwiftCodesResponse> response = swiftCodeService.getSwiftCodesByCountry(countryISO2code);
        return response.<ResponseEntity<?>>map(ResponseEntity::ok)
                .orElseGet(() -> {
                    log.warn("No SWIFT codes found for country: {}", countryISO2code);
                    return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("message", "No SWIFT codes found for country: " + countryISO2code));
                });
    }

    @PostMapping
    public ResponseEntity<?> addSwiftCode(@Valid @RequestBody SwiftCodeDTO swiftCodeDTO) {
        log.info("Received request to add SWIFT code: {}", swiftCodeDTO.getSwiftCode());
        swiftCodeService.addSwiftCode(swiftCodeDTO);
        log.info("SWIFT code added successfully: {}", swiftCodeDTO.getSwiftCode());
        return ResponseEntity.status(HttpStatus.CREATED).body(Map.of("message", "SWIFT code added successfully"));
    }

    @DeleteMapping("/{swiftCode}")
    public ResponseEntity<?> deleteSwiftCode(@PathVariable String swiftCode) {
        log.info("Received request to delete SWIFT code: {}", swiftCode);
        boolean deleted = swiftCodeService.deleteSwiftCode(swiftCode);
        if (deleted) {
            log.info("SWIFT code deleted successfully: {}", swiftCode);
            return ResponseEntity.ok(Map.of("message", "SWIFT code deleted successfully"));
        } else {
            log.warn("Attempted to delete non-existent SWIFT code: {}", swiftCode);
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("message", "SWIFT code not found"));
        }
    }
}