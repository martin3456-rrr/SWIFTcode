package swiftcodeapp.web.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import swiftcodeapp.service.SwiftCodeService;
import swiftcodeapp.web.dto.*;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(SwiftCodeController.class)
class SwiftCodeControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private SwiftCodeService swiftCodeService;

    @Autowired
    private ObjectMapper objectMapper;

    private HeadquarterSwiftCodeResponse headquarterResponse;
    private SwiftCodeDTO branchDTO;
    private CountrySwiftCodesResponse countryResponse;

    @BeforeEach
    void setUp() {
        BranchSwiftCodeDTO branchInHQ = new BranchSwiftCodeDTO("DEUTDEFF500", "DEUTSCHE BANK AG", "FRANKFURT AM MAIN", "DE", false);
        headquarterResponse = new HeadquarterSwiftCodeResponse("DEUTDEFFXXX", "DEUTSCHE BANK AG HQ", "FRANKFURT AM MAIN", "DE", "GERMANY", true, Collections.singletonList(branchInHQ));
        branchDTO = new SwiftCodeDTO("DEUTDEFF500", "DEUTSCHE BANK AG Branch", "FRANKFURT BRANCH", "DE", "GERMANY", false);

        List<SwiftCodeDTO> codesForCountry = Arrays.asList(
                new SwiftCodeDTO("DEUTDEFFXXX", "DEUTSCHE BANK AG HQ", "FRANKFURT AM MAIN", "DE", "GERMANY", true),
                branchDTO
        );
        countryResponse = new CountrySwiftCodesResponse("DE", "GERMANY", codesForCountry);
    }

    @Test
    void getSwiftCodeDetails_HeadquarterFound_Returns200() throws Exception {
        given(swiftCodeService.getSwiftCodeDetails("DEUTDEFFXXX")).willReturn(Optional.of(headquarterResponse));

        mockMvc.perform(get("/v1/swift-codes/{swiftCode}", "DEUTDEFFXXX"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.swiftCode", is("DEUTDEFFXXX")))
                .andExpect(jsonPath("$.isHeadquarter", is(true)))
                .andExpect(jsonPath("$.bankName", is("DEUTSCHE BANK AG HQ")))
                .andExpect(jsonPath("$.branches", hasSize(1)))
                .andExpect(jsonPath("$.branches[0].swiftCode", is("DEUTDEFF500")));
    }

    @Test
    void getSwiftCodeDetails_BranchFound_Returns200() throws Exception {
        given(swiftCodeService.getSwiftCodeDetails("DEUTDEFF500")).willReturn(Optional.of(branchDTO));

        mockMvc.perform(get("/v1/swift-codes/{swiftCode}", "DEUTDEFF500"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.swiftCode", is("DEUTDEFF500")))
                .andExpect(jsonPath("$.isHeadquarter", is(false)))
                .andExpect(jsonPath("$.bankName", is("DEUTSCHE BANK AG Branch")))
                .andExpect(jsonPath("$.branches").doesNotExist());
    }

    @Test
    void getSwiftCodeDetails_NotFound_Returns404() throws Exception {
        given(swiftCodeService.getSwiftCodeDetails("UNKNOWN")).willReturn(Optional.empty());

        mockMvc.perform(get("/v1/swift-codes/{swiftCode}", "UNKNOWN"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message", is("SWIFT code not found")));
    }

    @Test
    void getSwiftCodesByCountry_Found_Returns200() throws Exception {
        given(swiftCodeService.getSwiftCodesByCountry("DE")).willReturn(Optional.of(countryResponse));

        mockMvc.perform(get("/v1/swift-codes/country/{countryISO2code}", "DE"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.countryISO2", is("DE")))
                .andExpect(jsonPath("$.countryName", is("GERMANY")))
                .andExpect(jsonPath("$.swiftCodes", hasSize(2)))
                .andExpect(jsonPath("$.swiftCodes[0].swiftCode", is("DEUTDEFFXXX")))
                .andExpect(jsonPath("$.swiftCodes[1].swiftCode", is("DEUTDEFF500")));
    }

    @Test
    void getSwiftCodesByCountry_NotFound_Returns404() throws Exception {
        given(swiftCodeService.getSwiftCodesByCountry("XX")).willReturn(Optional.empty());

        mockMvc.perform(get("/v1/swift-codes/country/{countryISO2code}", "XX"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message", is("No SWIFT codes found for country: XX")));
    }

    @Test
    void addSwiftCode_ValidInput_Returns201() throws Exception {
        SwiftCodeDTO validInput = new SwiftCodeDTO("NEWCODE1XXX", "New Bank", "New Address", "PL", "POLAND", true);
        given(swiftCodeService.addSwiftCode(any(SwiftCodeDTO.class))).willReturn(null);

        mockMvc.perform(post("/v1/swift-codes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validInput)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.message", is("SWIFT code added successfully")));
    }

    @Test
    void addSwiftCode_InvalidInput_Returns400() throws Exception {
        SwiftCodeDTO invalidInput = new SwiftCodeDTO("NEWCODE1XXX", "New Bank", "New Address", "PLN", "POLAND", true);

        mockMvc.perform(post("/v1/swift-codes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidInput)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.countryISO2", containsString("must be 2 characters")));
    }

    @Test
    void addSwiftCode_Duplicate_Returns409() throws Exception {
        SwiftCodeDTO duplicateInput = new SwiftCodeDTO("DEUTDEFFXXX", "Duplicate Bank", "Some Address", "DE", "GERMANY", true);
        given(swiftCodeService.addSwiftCode(any(SwiftCodeDTO.class)))
                .willThrow(new IllegalArgumentException("SWIFT code already exists: DEUTDEFFXXX"));

        mockMvc.perform(post("/v1/swift-codes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(duplicateInput)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message", is("SWIFT code already exists: DEUTDEFFXXX")));
    }

    @Test
    void deleteSwiftCode_Exists_Returns200() throws Exception {
        given(swiftCodeService.deleteSwiftCode("DEUTDEFF500")).willReturn(true);

        mockMvc.perform(delete("/v1/swift-codes/{swiftCode}", "DEUTDEFF500"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message", is("SWIFT code deleted successfully")));
    }

    @Test
    void deleteSwiftCode_NotExists_Returns404() throws Exception {
        given(swiftCodeService.deleteSwiftCode("UNKNOWN")).willReturn(false);

        mockMvc.perform(delete("/v1/swift-codes/{swiftCode}", "UNKNOWN"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message", is("SWIFT code not found")));
    }
}