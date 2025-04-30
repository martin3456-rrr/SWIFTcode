package swiftcodeapp.repository;

import swiftcodeapp.entity.SwiftCode;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface SwiftCodeRepository extends JpaRepository<SwiftCode, String>{
    Optional<SwiftCode> findBySwiftCode(String swiftCode);
    List<SwiftCode> findByCountryISO2(String countryISO2);
    List<SwiftCode> findByHeadquarterSwiftCode(String headquarterSwiftCode);
}
