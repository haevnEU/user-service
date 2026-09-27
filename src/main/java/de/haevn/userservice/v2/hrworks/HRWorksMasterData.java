package de.haevn.userservice.v2.hrworks;

import de.haevn.userservice.v2.model.SsoUser;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

public record HRWorksMasterData(String personId, String firstName, String lastName, ContactData contactData,
                                ContractData contractData, String organizationalUnitId) {
    public static Optional<SsoUser> toSsoUser(final HRWorksMasterData masterData) {
        SsoUser ssoUser = new SsoUser();
        ssoUser.setId(masterData.personId());
        if (masterData.contractData == null || masterData.contractData().entryDate == null) {
            return Optional.empty();
        }

        final LocalDate hrWorksDate = masterData.contractData().entryDate();
        ssoUser.setRegistrationDate(hrWorksDate.atStartOfDay().toInstant(ZoneOffset.UTC));

        if (masterData.contactData() == null) {
            return Optional.empty();
        }
        ssoUser.setMail(masterData.contactData().businessEmail());


        ssoUser.setFirstName(masterData.firstName());
        ssoUser.setLastName(masterData.lastName());
        ssoUser.setOrganizationalUnitId(masterData.organizationalUnitId());
        ssoUser.setRoles(List.of("ROLE_USER"));
        return Optional.of(ssoUser);
    }

    public record ContactData(String businessEmail) {
    }


    public record ContractData(LocalDate entryDate) {
    }
}