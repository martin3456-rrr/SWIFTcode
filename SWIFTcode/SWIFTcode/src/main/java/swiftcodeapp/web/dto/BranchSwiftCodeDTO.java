package swiftcodeapp.web.dto;

public class BranchSwiftCodeDTO {
        private String swiftCode;
        private String bankName;
        private String address;
        private String countryISO2;
        private boolean isHeadquarter;

        public BranchSwiftCodeDTO(String swiftCode, String bankName, String address, String countryISO2, boolean isHeadquarter) {
            this.swiftCode = swiftCode;
            this.bankName = bankName;
            this.address = address;
            this.countryISO2 = countryISO2;
            this.isHeadquarter = isHeadquarter;
        }

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
            this.countryISO2 = countryISO2;
        }

        public boolean isHeadquarter() {
            return isHeadquarter;
        }

        public void setHeadquarter(boolean headquarter) {
            isHeadquarter = headquarter;
        }
}

