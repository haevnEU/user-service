package de.haevn.userservice.v2.gdpr;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "app.gdpr.data-processing")
@Getter
@Setter
public final class DataProcessing {
    @JsonProperty("automated_decision_making")
    private String automatedDecisionMaking;

    @JsonProperty("processed_data_categories")
    private List<String> processedDataCategories;

    @JsonProperty("retention_policy")
    private String retentionPolicy;

    @JsonProperty("recipients")
    private List<String> recipients;
}
