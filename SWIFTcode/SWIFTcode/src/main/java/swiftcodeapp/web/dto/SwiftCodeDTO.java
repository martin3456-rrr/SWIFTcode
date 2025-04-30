package swiftcodeapp.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.Pattern;

public class SwiftCodeDTO {

    @NotBlank(message = "SWIFT code cannot be blank")
    @Size(min = 8, max = 11, message = "SWIFT code must be between 8 and 11 characters")
    @Pattern(regexp = "^[A-Z0-9]{8}([A-Z0-9]{3})?$", message = "Invalid SWIFT code format")
    private String swiftCode;

    @NotBlank(message = "Bank name cannot be blank")
    private String bankName;

    @NotBlank(message = "Address cannot be blank")
    private String address;

    @NotBlank(message = "Country ISO2 code cannot be blank")
    @Size(min = 2, max = 2, message = "Country ISO2 code must be 2 characters")
    @Pattern(regexp = "^[A-Z]{2}$", message = "Invalid Country ISO2 format")
    private String countryISO2;

    @NotBlank(message = "Country name cannot be blank")
    private String countryName;

    @NotNull(message = "isHeadquarter flag must be provided")
    private Boolean isHeadquarter;

    public SwiftCodeDTO(String swiftCode, String bankName, String address, String countryISO2, String countryName, Boolean isHeadquarter) {
        this.swiftCode = swiftCode;
        this.bankName = bankName;
        this.address = address;
        this.countryISO2 = countryISO2;
        this.countryName = countryName;
        this.isHeadquarter = isHeadquarter;
    }

    public String getSwiftCode() { return swiftCode; }
    public void setSwiftCode(String swiftCode) { this.swiftCode = swiftCode; }
    public String getBankName() { return bankName; }
    public void setBankName(String bankName) { this.bankName = bankName; }
    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }
    public String getCountryISO2() { return countryISO2; }
    public void setCountryISO2(String countryISO2) { this.countryISO2 = countryISO2; }
    public String getCountryName() { return countryName; }
    public void setCountryName(String countryName) { this.countryName = countryName; }
    public Boolean getIsHeadquarter() { return isHeadquarter; }
    public void setIsHeadquarter(Boolean headquarter) { isHeadquarter = headquarter; }
}