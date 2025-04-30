package swiftcodeapp.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "swift_codes")
public class SwiftCode {

    @Id
    private String swiftCode;
    private String bankName;
    private String address;
    private String countryISO2;
    private String countryName;
    private boolean isHeadquarter;
    private String headquarterSwiftCode;

    public SwiftCode() {
    }

    public SwiftCode(String swiftCode, String bankName, String address, String countryISO2, String countryName, boolean isHeadquarter, String headquarterSwiftCode) {
        this.swiftCode = swiftCode;
        this.bankName = bankName;
        this.address = address;
        this.countryISO2 = countryISO2 != null ? countryISO2.toUpperCase() : null;
        this.countryName = countryName != null ? countryName.toUpperCase() : null;
        this.isHeadquarter = isHeadquarter;
        this.headquarterSwiftCode = headquarterSwiftCode;
    }

    // Getters and Setters
    public String getSwiftCode() {
        return swiftCode;
    }

    public void setSwiftCode(String swiftCode) {
        this.swiftCode = swiftCode;
    }

    public String getBankName() {
        return bankName;
    }

    public void setBankName(String bankName) {
        this.bankName = bankName;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getCountryISO2() {
        return countryISO2;
    }

    public void setCountryISO2(String countryISO2) {
        this.countryISO2 = countryISO2 != null ? countryISO2.toUpperCase() : null;
    }

    public String getCountryName() {
        return countryName;
    }

    public void setCountryName(String countryName) {
        this.countryName = countryName != null ? countryName.toUpperCase() : null;
    }

    public boolean isHeadquarter() {
        return isHeadquarter;
    }

    public void setHeadquarter(boolean headquarter) {
        isHeadquarter = headquarter;
    }

    public String getHeadquarterSwiftCode() {
        return headquarterSwiftCode;
    }

    public void setHeadquarterSwiftCode(String headquarterSwiftCode) {
        this.headquarterSwiftCode = headquarterSwiftCode;
    }

    @Override
    public String toString() {
        return "SwiftCode{" +
                "swiftCode='" + swiftCode + '\'' +
                ", bankName='" + bankName + '\'' +
                ", address='" + address + '\'' +
                ", countryISO2='" + countryISO2 + '\'' +
                ", countryName='" + countryName + '\'' +
                ", isHeadquarter=" + isHeadquarter +
                ", headquarterSwiftCode='" + headquarterSwiftCode + '\'' +
                '}';
    }
}
