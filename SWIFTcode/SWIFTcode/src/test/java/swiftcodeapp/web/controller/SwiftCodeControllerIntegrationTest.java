package swiftcodeapp.web.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.*;
import org.testcontainers.junit.jupiter.Testcontainers;
import swiftcodeapp.entity.SwiftCode;
import swiftcodeapp.testcontainers.AbstractIntegrationTest;
import swiftcodeapp.web.dto.CountrySwiftCodesResponse;
import swiftcodeapp.web.dto.HeadquarterSwiftCodeResponse;
import swiftcodeapp.web.dto.SwiftCodeDTO;

import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;


@Testcontainers
public class SwiftCodeControllerIntegrationTest extends AbstractIntegrationTest {

    private final String BASE_URL = "/v1/swift-codes";
    @BeforeEach
    void setUp() {
        //swiftCodeRepository.deleteAll();
    }
    @Test
    void testAddAndGetSwiftCode() throws Exception {
        SwiftCodeDTO hqDto = new SwiftCodeDTO("TESTHQPLXXX", "Test Bank HQ", "Test Address HQ", "PL", "POLAND", true);
        ResponseEntity<Map> postHqResponse = testRestTemplate.postForEntity(
                createURL(BASE_URL),
                hqDto,
                Map.class
        );

        assertEquals(HttpStatus.CREATED, postHqResponse.getStatusCode());
        assertThat(postHqResponse.getBody()).containsKey("message");
        assertThat(postHqResponse.getBody().get("message")).isEqualTo("SWIFT code added successfully");

        Optional<SwiftCode> hqInDb = swiftCodeRepository.findBySwiftCode("TESTHQPLXXX");
        assertTrue(hqInDb.isPresent());
        assertEquals("Test Bank HQ", hqInDb.get().getBankName());

        SwiftCodeDTO branchDto = new SwiftCodeDTO("TESTHQPL123", "Test Bank Branch", "Test Address Branch", "PL", "POLAND", false);
        ResponseEntity<Map> postBranchResponse = testRestTemplate.postForEntity(
                createURL(BASE_URL),
                branchDto,
                Map.class
        );

        assertEquals(HttpStatus.CREATED, postBranchResponse.getStatusCode());

        Optional<SwiftCode> branchInDb = swiftCodeRepository.findBySwiftCode("TESTHQPL123");
        assertTrue(branchInDb.isPresent());
        assertEquals("TESTHQPLXXX", branchInDb.get().getHeadquarterSwiftCode(), "Branch should be linked to the HQ");

        ResponseEntity<HeadquarterSwiftCodeResponse> getHqResponse = testRestTemplate.getForEntity(
                createURL(BASE_URL + "/TESTHQPLXXX"),
                HeadquarterSwiftCodeResponse.class
        );

        assertEquals(HttpStatus.OK, getHqResponse.getStatusCode());
        HeadquarterSwiftCodeResponse hqDetails = getHqResponse.getBody();
        assertNotNull(hqDetails);
        assertTrue(hqDetails.getIsHeadquarter());
        assertEquals("TESTHQPLXXX", hqDetails.getSwiftCode());
        assertEquals("POLAND", hqDetails.getCountryName());
        assertThat(hqDetails.getBranches()).hasSize(1);
        assertEquals("TESTHQPL123", hqDetails.getBranches().get(0).getSwiftCode());

        ResponseEntity<SwiftCodeDTO> getBranchResponse = testRestTemplate.getForEntity(
                createURL(BASE_URL + "/TESTHQPL123"),
                SwiftCodeDTO.class
        );

        assertEquals(HttpStatus.OK, getBranchResponse.getStatusCode());
        SwiftCodeDTO branchDetails = getBranchResponse.getBody();
        assertNotNull(branchDetails);
        assertFalse(branchDetails.getIsHeadquarter());
        assertEquals("TESTHQPL123", branchDetails.getSwiftCode());

        ResponseEntity<Map> getNonExistentResponse = testRestTemplate.getForEntity(
                createURL(BASE_URL + "/NONEXISTENT"),
                Map.class
        );
        assertEquals(HttpStatus.NOT_FOUND, getNonExistentResponse.getStatusCode());
        assertThat(getNonExistentResponse.getBody()).containsKey("message");
    }

    @Test
    void testGetSwiftCodesByCountry() {
        SwiftCodeDTO hqDto = new SwiftCodeDTO("TESTBYPLXXX", "Country Test Bank HQ", "Address HQ", "PL", "POLAND", true);
        SwiftCodeDTO branchDto = new SwiftCodeDTO("TESTBYPL123", "Country Test Bank Branch", "Address Branch", "PL", "POLAND", false);
        testRestTemplate.postForEntity(createURL(BASE_URL), hqDto, Map.class);
        testRestTemplate.postForEntity(createURL(BASE_URL), branchDto, Map.class);


        ResponseEntity<CountrySwiftCodesResponse> getPlResponse = testRestTemplate.getForEntity(
                createURL(BASE_URL + "/country/PL"),
                CountrySwiftCodesResponse.class
        );

        assertEquals(HttpStatus.OK, getPlResponse.getStatusCode());
        CountrySwiftCodesResponse plDetails = getPlResponse.getBody();
        assertNotNull(plDetails);
        assertEquals("PL", plDetails.getCountryISO2());
        assertEquals("POLAND", plDetails.getCountryName());
        assertThat(plDetails.getSwiftCodes().size()).isGreaterThanOrEqualTo(2);
        assertTrue(plDetails.getSwiftCodes().stream().anyMatch(c -> "TESTBYPLXXX".equals(c.getSwiftCode())));
        assertTrue(plDetails.getSwiftCodes().stream().anyMatch(c -> "TESTBYPL123".equals(c.getSwiftCode())));

        ResponseEntity<Map> getXxResponse = testRestTemplate.getForEntity(
                createURL(BASE_URL + "/country/XX"),
                Map.class
        );
        assertEquals(HttpStatus.NOT_FOUND, getXxResponse.getStatusCode());
        assertThat(getXxResponse.getBody()).containsKey("message");
        assertThat((String)getXxResponse.getBody().get("message")).contains("No SWIFT codes found for country: XX");
    }


    @Test
    void testAddSwiftCode_Duplicate() {
        SwiftCodeDTO hqDto = new SwiftCodeDTO("DUPETESTXXX", "Dupe Test Bank HQ", "Dupe Address HQ", "DE", "GERMANY", true);
        ResponseEntity<Map> firstPostResponse = testRestTemplate.postForEntity(createURL(BASE_URL), hqDto, Map.class);
        assertEquals(HttpStatus.CREATED, firstPostResponse.getStatusCode());

        ResponseEntity<Map> secondPostResponse = testRestTemplate.postForEntity(createURL(BASE_URL), hqDto, Map.class);
        assertEquals(HttpStatus.CONFLICT, secondPostResponse.getStatusCode());
        assertThat(secondPostResponse.getBody()).containsKey("message");
        assertThat((String)secondPostResponse.getBody().get("message")).contains("SWIFT code already exists: DUPETESTXXX");
    }

    @Test
    void testAddSwiftCode_ValidationErrors() {
        SwiftCodeDTO invalidSwiftDto = new SwiftCodeDTO("SHORT", "Invalid", "Invalid Addr", "PL", "POLAND", true);
        ResponseEntity<Map> responseSwift = testRestTemplate.postForEntity(createURL(BASE_URL), invalidSwiftDto, Map.class);
        assertEquals(HttpStatus.BAD_REQUEST, responseSwift.getStatusCode());
        assertThat(responseSwift.getBody()).containsKey("swiftCode");

        SwiftCodeDTO invalidCountryDto = new SwiftCodeDTO("VALIDCDEXXX", "Valid Bank", "Valid Addr", "PLN", "POLAND", true);
        ResponseEntity<Map> responseCountry = testRestTemplate.postForEntity(createURL(BASE_URL), invalidCountryDto, Map.class);
        assertEquals(HttpStatus.BAD_REQUEST, responseCountry.getStatusCode());
        assertThat(responseCountry.getBody()).containsKey("countryISO2");
    }

    @Test
    void testDeleteSwiftCode() {
        SwiftCodeDTO dtoToDelete = new SwiftCodeDTO("DELTESTCODE", "To Delete Bank", "Del Address", "GB", "UNITED KINGDOM", false);
        ResponseEntity<Map> postResponse = testRestTemplate.postForEntity(createURL(BASE_URL), dtoToDelete, Map.class);
        assertEquals(HttpStatus.CREATED, postResponse.getStatusCode());

        ResponseEntity<Map> deleteResponse = testRestTemplate.exchange(
                createURL(BASE_URL + "/DELTESTCODE"),
                HttpMethod.DELETE,
                null,
                Map.class
        );
        assertEquals(HttpStatus.OK, deleteResponse.getStatusCode());
        assertThat(deleteResponse.getBody()).containsKey("message");
        assertThat(deleteResponse.getBody().get("message")).isEqualTo("SWIFT code deleted successfully");

        ResponseEntity<Map> getResponse = testRestTemplate.getForEntity(
                createURL(BASE_URL + "/DELTESTCODE"),
                Map.class
        );
        assertEquals(HttpStatus.NOT_FOUND, getResponse.getStatusCode());

        ResponseEntity<Map> deleteNonExistentResponse = testRestTemplate.exchange(
                createURL(BASE_URL + "/NONEXISTENTDEL"),
                HttpMethod.DELETE,
                null,
                Map.class
        );
        assertEquals(HttpStatus.NOT_FOUND, deleteNonExistentResponse.getStatusCode());
    }
}