package swiftcodeapp.web.dto;

import java.util.List;

public class HeadquarterSwiftCodeResponse extends SwiftCodeDTO {
    private List<BranchSwiftCodeDTO> branches;

    public HeadquarterSwiftCodeResponse(String swiftCode, String bankName, String address, String countryISO2, String countryName, boolean isHeadquarter, List<BranchSwiftCodeDTO> branches) {
        super(swiftCode, bankName, address, countryISO2, countryName, isHeadquarter);
        this.branches = branches;
    }

    public List<BranchSwiftCodeDTO> getBranches() {
        return branches;
    }

    public void setBranches(List<BranchSwiftCodeDTO> branches) {
        this.branches = branches;
    }
}
