package swiftcodeapp.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.io.Resource;
import swiftcodeapp.entity.SwiftCode;
import swiftcodeapp.repository.SwiftCodeRepository;
import swiftcodeapp.web.dto.*;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SwiftCodeServiceTest {

    @Mock
    private SwiftCodeRepository swiftCodeRepository;

    @Mock
    private Resource swiftDataFile;

    @InjectMocks
    private SwiftCodeService swiftCodeService;

    private SwiftCode headquarter;
    private SwiftCode branch;
    private SwiftCodeDTO branchDTO;

    @BeforeEach
    void setUp() {
        headquarter = new SwiftCode("DEUTDEFFXXX", "DEUTSCHE BANK AG", "FRANKFURT AM MAIN", "DE", "GERMANY", true, null);
        branch = new SwiftCode("DEUTDEFF500", "DEUTSCHE BANK AG", "FRANKFURT AM MAIN", "DE", "GERMANY", false, "DEUTDEFFXXX");
        branchDTO = new SwiftCodeDTO(branch.getSwiftCode(), branch.getBankName(), branch.getAddress(), branch.getCountryISO2(), branch.getCountryName(), branch.isHeadquarter());
    }

    @Test
    void testLoadSwiftData_Success() throws IOException {
        String csvData = "swiftCode,bankName,address,countryISO2,countryName\n" +
                "DEUTDEFFXXX,DEUTSCHE BANK AG,FRANKFURT AM MAIN,DE,GERMANY\n" +
                "DEUTDEFF500,DEUTSCHE BANK AG,FRANKFURT AM MAIN,DE,GERMANY";
        InputStream inputStream = new ByteArrayInputStream(csvData.getBytes());

        when(swiftDataFile.exists()).thenReturn(true);
        when(swiftDataFile.isReadable()).thenReturn(true);
        when(swiftDataFile.getInputStream()).thenReturn(inputStream);
        when(swiftDataFile.getFilename()).thenReturn("test.csv");

        when(swiftCodeRepository.saveAll(anyList())).thenAnswer(invocation -> invocation.getArgument(0));
        when(swiftCodeRepository.findBySwiftCode("DEUTDEFFXXX")).thenReturn(Optional.of(headquarter));


        swiftCodeService.loadSwiftData();

        verify(swiftCodeRepository, atLeast(1)).saveAll(anyList());
    }

    @Test
    void testLoadSwiftData_FileNotExists() {
        when(swiftDataFile.exists()).thenReturn(false);
        when(swiftDataFile.getFilename()).thenReturn("swift_codes.csv");

        swiftCodeService.loadSwiftData();
        verify(swiftCodeRepository, never()).saveAll(anyList());
        verify(swiftCodeRepository, never()).findBySwiftCode(anyString());
    }


    @Test
    void getSwiftCodeDetails_HeadquarterFound() {
        when(swiftCodeRepository.findBySwiftCode("DEUTDEFFXXX")).thenReturn(Optional.of(headquarter));
        when(swiftCodeRepository.findByHeadquarterSwiftCode("DEUTDEFFXXX")).thenReturn(Collections.singletonList(branch));

        Optional<Object> result = swiftCodeService.getSwiftCodeDetails("DEUTDEFFXXX");

        assertTrue(result.isPresent());
        assertTrue(result.get() instanceof HeadquarterSwiftCodeResponse);
        HeadquarterSwiftCodeResponse response = (HeadquarterSwiftCodeResponse) result.get();
        assertEquals("DEUTDEFFXXX", response.getSwiftCode());
        assertEquals(1, response.getBranches().size());
        assertEquals("DEUTDEFF500", response.getBranches().get(0).getSwiftCode());

        verify(swiftCodeRepository, times(1)).findBySwiftCode("DEUTDEFFXXX");
        verify(swiftCodeRepository, times(1)).findByHeadquarterSwiftCode("DEUTDEFFXXX");
    }

    @Test
    void getSwiftCodeDetails_BranchFound() {
        when(swiftCodeRepository.findBySwiftCode("DEUTDEFF500")).thenReturn(Optional.of(branch));

        Optional<Object> result = swiftCodeService.getSwiftCodeDetails("DEUTDEFF500");

        assertTrue(result.isPresent());
        assertTrue(result.get() instanceof SwiftCodeDTO);
        SwiftCodeDTO response = (SwiftCodeDTO) result.get();
        assertEquals("DEUTDEFF500", response.getSwiftCode());
        assertFalse(response.getIsHeadquarter());

        verify(swiftCodeRepository, times(1)).findBySwiftCode("DEUTDEFF500");
        verify(swiftCodeRepository, never()).findByHeadquarterSwiftCode(anyString());
    }

    @Test
    void getSwiftCodeDetails_NotFound() {
        when(swiftCodeRepository.findBySwiftCode("UNKNOWNCODE")).thenReturn(Optional.empty());

        Optional<Object> result = swiftCodeService.getSwiftCodeDetails("UNKNOWNCODE");

        assertFalse(result.isPresent());
        verify(swiftCodeRepository, times(1)).findBySwiftCode("UNKNOWNCODE");
    }

    @Test
    void getSwiftCodesByCountry_Found() {
        List<SwiftCode> codes = Arrays.asList(headquarter, branch);
        when(swiftCodeRepository.findByCountryISO2("DE")).thenReturn(codes);

        Optional<CountrySwiftCodesResponse> result = swiftCodeService.getSwiftCodesByCountry("de");

        assertTrue(result.isPresent());
        CountrySwiftCodesResponse response = result.get();
        assertEquals("DE", response.getCountryISO2());
        assertEquals("GERMANY", response.getCountryName());
        assertEquals(2, response.getSwiftCodes().size());

        verify(swiftCodeRepository, times(1)).findByCountryISO2("DE");
    }

    @Test
    void getSwiftCodesByCountry_NotFound() {
        when(swiftCodeRepository.findByCountryISO2("XX")).thenReturn(Collections.emptyList());

        Optional<CountrySwiftCodesResponse> result = swiftCodeService.getSwiftCodesByCountry("XX");

        assertFalse(result.isPresent());
        verify(swiftCodeRepository, times(1)).findByCountryISO2("XX");
    }

    @Test
    void addSwiftCode_NewBranch_LinksToExistingHeadquarter() {
        SwiftCodeDTO newBranchDTO = new SwiftCodeDTO("DEUTDEFF600", "DEUTSCHE BANK AG", "CITY BRANCH", "DE", "GERMANY", false);
        SwiftCode newBranchEntity = new SwiftCode(newBranchDTO.getSwiftCode(), newBranchDTO.getBankName(), newBranchDTO.getAddress(), newBranchDTO.getCountryISO2(), newBranchDTO.getCountryName(), newBranchDTO.getIsHeadquarter(), "DEUTDEFFXXX");

        when(swiftCodeRepository.existsById("DEUTDEFF600")).thenReturn(false);
        when(swiftCodeRepository.findBySwiftCode("DEUTDEFFXXX")).thenReturn(Optional.of(headquarter));
        when(swiftCodeRepository.save(any(SwiftCode.class))).thenReturn(newBranchEntity);


        SwiftCode savedCode = swiftCodeService.addSwiftCode(newBranchDTO);

        assertNotNull(savedCode);
        assertEquals("DEUTDEFF600", savedCode.getSwiftCode());
        assertEquals("DEUTDEFFXXX", savedCode.getHeadquarterSwiftCode());

        verify(swiftCodeRepository, times(1)).existsById("DEUTDEFF600");
        verify(swiftCodeRepository, times(1)).findBySwiftCode("DEUTDEFFXXX");
        verify(swiftCodeRepository, times(1)).save(any(SwiftCode.class));
    }

    @Test
    void addSwiftCode_Duplicate() {
        when(swiftCodeRepository.existsById(branch.getSwiftCode())).thenReturn(true);

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            swiftCodeService.addSwiftCode(branchDTO);
        });

        assertTrue(exception.getMessage().contains("already exists"));

        verify(swiftCodeRepository, times(1)).existsById(branch.getSwiftCode());
        verify(swiftCodeRepository, never()).save(any(SwiftCode.class));
    }


    @Test
    void deleteSwiftCode_Exists() {
        when(swiftCodeRepository.findBySwiftCode(branch.getSwiftCode())).thenReturn(Optional.of(branch));
        doNothing().when(swiftCodeRepository).deleteById(branch.getSwiftCode());

        boolean deleted = swiftCodeService.deleteSwiftCode(branch.getSwiftCode());

        assertTrue(deleted);
        verify(swiftCodeRepository, times(1)).findBySwiftCode(branch.getSwiftCode());
        verify(swiftCodeRepository, times(1)).deleteById(branch.getSwiftCode());
    }

    @Test
    void deleteSwiftCode_Headquarter_UnlinksBranches() {
        when(swiftCodeRepository.findBySwiftCode(headquarter.getSwiftCode())).thenReturn(Optional.of(headquarter));
        when(swiftCodeRepository.findByHeadquarterSwiftCode(headquarter.getSwiftCode())).thenReturn(Collections.singletonList(branch));
        doNothing().when(swiftCodeRepository).deleteById(headquarter.getSwiftCode());


        boolean deleted = swiftCodeService.deleteSwiftCode(headquarter.getSwiftCode());

        assertTrue(deleted);
        verify(swiftCodeRepository, times(1)).saveAll(anyList());
        verify(swiftCodeRepository, times(1)).deleteById(headquarter.getSwiftCode());
    }


    @Test
    void deleteSwiftCode_NotExists() {
        when(swiftCodeRepository.findBySwiftCode("UNKNOWNCODE")).thenReturn(Optional.empty());
        boolean deleted = swiftCodeService.deleteSwiftCode("UNKNOWNCODE");
        assertFalse(deleted);
        verify(swiftCodeRepository, times(1)).findBySwiftCode("UNKNOWNCODE");
        verify(swiftCodeRepository, never()).deleteById(anyString());
    }
}