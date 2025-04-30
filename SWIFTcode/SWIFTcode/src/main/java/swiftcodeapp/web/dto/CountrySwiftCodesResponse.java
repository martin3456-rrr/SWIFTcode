package swiftcodeapp.web.dto;

import java.util.List;

public class CountrySwiftCodesResponse {
    private String countryISO2;
    private String countryName;
    private List<SwiftCodeDTO> swiftCodes;

    public CountrySwiftCodesResponse(String countryISO2, String countryName, List<SwiftCodeDTO> swiftCodes) {
        this.countryISO2 = countryISO2;
        this.countryName = countryName;
        this.swiftCodes = swiftCodes;
    }

    // Getters and Setters
    public String getCountryISO2() {
        return countryISO2;
    }

    public void setCountryISO2(String countryISO2) {
        this.countryISO2 = countryISO2;
    }

    public String getCountryName() {
        return countryName;
    }

    public void setCountryName(String countryName) {
        this.countryName = countryName;
    }

    public List<SwiftCodeDTO> getSwiftCodes() {
        return swiftCodes;
    }

    public void setSwiftCodes(List<SwiftCodeDTO> swiftCodes) {
        this.swiftCodes = swiftCodes;
    }
}
