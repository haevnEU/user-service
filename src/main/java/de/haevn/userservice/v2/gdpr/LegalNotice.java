package de.haevn.userservice.v2.gdpr;


import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
@Getter
public final class LegalNotice {

    @Value("${app.gdpr.international-data-transfer}")
    @JsonProperty("international_data_transfer")
    private String internationalDataTransfer;

    @Value("${app.gdpr.purpose-of-processing}")
    @JsonProperty("purpose_of_processing")
    private String purposeOfProcessing;

    @Value("${app.gdpr.legal-basis}")
    @JsonProperty("legal_basis")
    private String legalBasis;

    @Value("${app.gdpr.storage-duration}")
    @JsonProperty("storage_duration")
    private String storageDuration;

    @Value("${app.gdpr.data-recipients}")
    @JsonProperty("data_recipients")
    private String dataRecipients;

    @Value("${app.gdpr.cryptographic-note}")
    @JsonProperty("cryptographic_note")
    private String cryptographicNote;

    @Value("${app.gdpr.rights-of-the-data-subject}")
    @JsonProperty("rights_of_the_data_subject")
    private String rightsOfTheDataSubject;

    @Value("${app.gdpr.data-source}")
    @JsonProperty("data_source")
    private String dataSource;
}