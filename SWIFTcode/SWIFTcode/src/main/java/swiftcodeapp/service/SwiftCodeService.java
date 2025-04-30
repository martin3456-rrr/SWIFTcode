package swiftcodeapp.service;

import jakarta.annotation.PostConstruct;
import swiftcodeapp.entity.SwiftCode;
import swiftcodeapp.repository.SwiftCodeRepository;
import swiftcodeapp.web.dto.BranchSwiftCodeDTO;
import swiftcodeapp.web.dto.CountrySwiftCodesResponse;
import swiftcodeapp.web.dto.HeadquarterSwiftCodeResponse;
import swiftcodeapp.web.dto.SwiftCodeDTO;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class SwiftCodeService {

    private static final Logger log = LoggerFactory.getLogger(SwiftCodeService.class);

    @Autowired
    private SwiftCodeRepository swiftCodeRepository;

    @Value("${swift.data.file:classpath:swift_codes.csv}")
    private Resource swiftDataFile;

    @PostConstruct
    @Transactional
    public void loadSwiftData() {
        if (swiftDataFile.exists() && swiftDataFile.isReadable()) {
            log.info("Starting SWIFT data loading from: {}", swiftDataFile.getFilename());
            List<SwiftCode> codesToSave = new ArrayList<>();
            List<SwiftCode> potentialBranches = new ArrayList<>();

            try (Reader reader = new InputStreamReader(swiftDataFile.getInputStream());
                 CSVParser csvParser = new CSVParser(reader, CSVFormat.DEFAULT
                         .withFirstRecordAsHeader()
                         .withIgnoreHeaderCase(true)
                         .withTrim())) {

                for (CSVRecord record : csvParser) {
                    try {
                        SwiftCode swiftCode = mapCsvRecordToSwiftCode(record);
                        codesToSave.add(swiftCode);
                        if (!swiftCode.isHeadquarter()) {
                            potentialBranches.add(swiftCode);
                        }
                        log.debug("Parsed SWIFT code: {}", swiftCode.getSwiftCode());
                    } catch (Exception e) {
                        log.error("Failed to parse record #{}: {}. Error: {}", record.getRecordNumber(), record.toMap(), e.getMessage());
                    }
                }

                if (!codesToSave.isEmpty()) {
                    log.info("Saving {} initial SWIFT codes to database...", codesToSave.size());
                    swiftCodeRepository.saveAll(codesToSave);
                    log.info("Initial SWIFT codes saved.");

                    log.info("Linking branches to headquarters...");
                    int linkedCount = 0;
                    List<SwiftCode> branchesToUpdate = new ArrayList<>();
                    for (SwiftCode branch : potentialBranches) {
                        if (branch.getSwiftCode() != null && branch.getSwiftCode().length() >= 8) {
                            String potentialHeadquarterCode = branch.getSwiftCode().substring(0, 8) + "XXX";
                            Optional<SwiftCode> headquarterOpt = codesToSave.stream()
                                    .filter(sc -> potentialHeadquarterCode.equals(sc.getSwiftCode()))
                                    .findFirst()
                                    .or(() -> swiftCodeRepository.findBySwiftCode(potentialHeadquarterCode));

                            if (headquarterOpt.isPresent()) {
                                branch.setHeadquarterSwiftCode(headquarterOpt.get().getSwiftCode());
                                branchesToUpdate.add(branch);
                                linkedCount++;
                                log.debug("Linking branch {} to headquarter {}", branch.getSwiftCode(), headquarterOpt.get().getSwiftCode());
                            }
                        }
                    }
                    if (!branchesToUpdate.isEmpty()) {
                        swiftCodeRepository.saveAll(branchesToUpdate);
                        log.info("Linked {} branches to their headquarters.", linkedCount);
                    } else {
                        log.info("No branches found to link or headquarters not found.");
                    }


                } else {
                    log.warn("No valid SWIFT codes found in the CSV file to save.");
                }

            } catch (IOException e) {
                log.error("Error reading SWIFT data file {}: {}", swiftDataFile.getFilename(), e.getMessage(), e);
            } catch (Exception e) {
                log.error("Unexpected error during SWIFT data loading: {}", e.getMessage(), e);
            }
        } else {
            log.warn("SWIFT data file not found or not readable at: {}", swiftDataFile.getFilename());
        }
    }


    private SwiftCode mapCsvRecordToSwiftCode(CSVRecord record) {
        log.debug("Available headers: {}", record.getParser().getHeaderMap().keySet());
        String swiftCode = record.isSet("SWIFT CODE") ? record.get("SWIFT CODE") : null;
        String bankName = record.isSet("NAME") ? record.get("NAME") : null;
        String address = record.isSet("ADDRESS") ? record.get("ADDRESS") : null;
        String countryISO2 = record.isSet("COUNTRY ISO2 CODE") ? record.get("COUNTRY ISO2 CODE") : null;
        String countryName = record.isSet("COUNTRY NAME") ? record.get("COUNTRY NAME") : null;

        if (swiftCode == null || swiftCode.isBlank()) {
            throw new IllegalArgumentException("Missing or empty SWIFT code in record #" + record.getRecordNumber());
        }

        boolean isHeadquarter = swiftCode.toUpperCase().endsWith("XXX");

        return new SwiftCode(swiftCode, bankName, address, countryISO2, countryName, isHeadquarter, null);
    }


    @Transactional(readOnly = true)
    public Optional<Object> getSwiftCodeDetails(String swiftCode) {
        log.debug("Querying database for SWIFT code: {}", swiftCode);
        Optional<SwiftCode> swiftCodeOptional = swiftCodeRepository.findBySwiftCode(swiftCode);

        if (swiftCodeOptional.isPresent()) {
            SwiftCode code = swiftCodeOptional.get();
            log.debug("Found SWIFT code: {}. Is headquarter: {}", swiftCode, code.isHeadquarter());
            if (code.isHeadquarter()) {
                log.debug("Finding branches for headquarter: {}", code.getSwiftCode());
                List<SwiftCode> branches = swiftCodeRepository.findByHeadquarterSwiftCode(code.getSwiftCode());
                log.debug("Found {} branches for {}", branches.size(), code.getSwiftCode());
                List<BranchSwiftCodeDTO> branchDTOs = branches.stream()
                        .map(branch -> new BranchSwiftCodeDTO(
                                branch.getSwiftCode(),
                                branch.getBankName(),
                                branch.getAddress(),
                                branch.getCountryISO2(),
                                branch.isHeadquarter()
                        ))
                        .collect(Collectors.toList());

                return Optional.of(new HeadquarterSwiftCodeResponse(
                        code.getSwiftCode(),
                        code.getBankName(),
                        code.getAddress(),
                        code.getCountryISO2(),
                        code.getCountryName(),
                        code.isHeadquarter(),
                        branchDTOs
                ));
            } else {
                return Optional.of(new SwiftCodeDTO(
                        code.getSwiftCode(),
                        code.getBankName(),
                        code.getAddress(),
                        code.getCountryISO2(),
                        code.getCountryName(),
                        code.isHeadquarter()
                ));
            }
        }
        log.warn("SWIFT code not found in database: {}", swiftCode);
        return Optional.empty();
    }

    @Transactional(readOnly = true)
    public Optional<CountrySwiftCodesResponse> getSwiftCodesByCountry(String countryISO2) {
        String upperCaseCountryISO2 = countryISO2.toUpperCase();
        log.debug("Querying database for SWIFT codes for country: {}", upperCaseCountryISO2);
        List<SwiftCode> swiftCodes = swiftCodeRepository.findByCountryISO2(upperCaseCountryISO2);

        if (!swiftCodes.isEmpty()) {
            log.debug("Found {} SWIFT codes for country: {}", swiftCodes.size(), upperCaseCountryISO2);
            List<SwiftCodeDTO> swiftCodeDTOs = swiftCodes.stream()
                    .map(code -> new SwiftCodeDTO(
                            code.getSwiftCode(),
                            code.getBankName(),
                            code.getAddress(),
                            code.getCountryISO2(),
                            code.getCountryName(),
                            code.isHeadquarter()
                    ))
                    .collect(Collectors.toList());
            String countryName = swiftCodes.get(0).getCountryName();

            return Optional.of(new CountrySwiftCodesResponse(
                    upperCaseCountryISO2,
                    countryName,
                    swiftCodeDTOs
            ));
        }
        log.warn("No SWIFT codes found in database for country: {}", upperCaseCountryISO2);
        return Optional.empty();
    }

    @Transactional
    public SwiftCode addSwiftCode(SwiftCodeDTO swiftCodeDTO) {
        String codeToAdd = swiftCodeDTO.getSwiftCode();
        log.debug("Attempting to add SWIFT code: {}", codeToAdd);
        if (swiftCodeRepository.existsById(codeToAdd)) {
            log.warn("Attempted to add duplicate SWIFT code: {}", codeToAdd);
            throw new IllegalArgumentException("SWIFT code already exists: " + codeToAdd);
        }

        SwiftCode swiftCode = new SwiftCode(
                codeToAdd,
                swiftCodeDTO.getBankName(),
                swiftCodeDTO.getAddress(),
                swiftCodeDTO.getCountryISO2(),
                swiftCodeDTO.getCountryName(),
                swiftCodeDTO.getIsHeadquarter(),
                null
        );

        if (!swiftCode.isHeadquarter() && swiftCode.getSwiftCode().length() >= 8) {
            String potentialHeadquarterCode = swiftCode.getSwiftCode().substring(0, 8) + "XXX";
            log.debug("Checking for potential headquarter {} for new branch {}", potentialHeadquarterCode, swiftCode.getSwiftCode());
            swiftCodeRepository.findBySwiftCode(potentialHeadquarterCode)
                    .ifPresent(headquarter -> {
                        log.info("Linking new branch {} to headquarter {}", swiftCode.getSwiftCode(), headquarter.getSwiftCode());
                        swiftCode.setHeadquarterSwiftCode(headquarter.getSwiftCode());
                    });
        }

        if (swiftCode.isHeadquarter()) {
            String headquarterPrefix = swiftCode.getSwiftCode().substring(0, 8);
            log.debug("Checking for existing branches for new headquarter {}", swiftCode.getSwiftCode());
            List<SwiftCode> potentialBranches = swiftCodeRepository.findAll().stream()
                    .filter(sc -> !sc.isHeadquarter() && sc.getSwiftCode().startsWith(headquarterPrefix) && sc.getHeadquarterSwiftCode() == null)
                    .toList();

            if (!potentialBranches.isEmpty()) {
                log.info("Found {} existing branches to link to new headquarter {}", potentialBranches.size(), swiftCode.getSwiftCode());
                potentialBranches.forEach(branch -> branch.setHeadquarterSwiftCode(swiftCode.getSwiftCode()));
                swiftCodeRepository.saveAll(potentialBranches);
            }
        }

        log.info("Saving new SWIFT code to database: {}", swiftCode.getSwiftCode());
        return swiftCodeRepository.save(swiftCode);
    }

    @Transactional
    public boolean deleteSwiftCode(String swiftCode) {
        log.debug("Attempting to delete SWIFT code: {}", swiftCode);
        Optional<SwiftCode> codeToDeleteOpt = swiftCodeRepository.findBySwiftCode(swiftCode);
        if (codeToDeleteOpt.isPresent()) {
            SwiftCode codeToDelete = codeToDeleteOpt.get();
            if (codeToDelete.isHeadquarter()) {
                log.warn("Deleting headquarter {}. Unlinking associated branches.", swiftCode);
                List<SwiftCode> branches = swiftCodeRepository.findByHeadquarterSwiftCode(swiftCode);
                if (!branches.isEmpty()) {
                    branches.forEach(branch -> branch.setHeadquarterSwiftCode(null));
                    swiftCodeRepository.saveAll(branches);
                    log.info("Unlinked {} branches from deleted headquarter {}", branches.size(), swiftCode);
                }
            }
            swiftCodeRepository.deleteById(swiftCode);
            log.info("Successfully deleted SWIFT code: {}", swiftCode);
            return true;
        }
        log.warn("Attempted to delete non-existent SWIFT code: {}", swiftCode);
        return false;
    }
}