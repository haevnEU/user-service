package de.haevn.userservice.v2.hrworks;

import de.haevn.userservice.v2.model.SsoUser;
import de.haevn.userservice.v2.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestClient;

@Log4j2
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v2/hrworks")
public class HRWorksController {
    private final UserService userService;
    private final RestClient restClient;

    @PostMapping("/webhook")
    public ResponseEntity<Void> handleHRworksWebhook(@RequestBody HRWorksWebhookEvent event) {
        log.info("Webhook empfangen! Event-Typ: {}, Person-ID: {}", event.eventType(), event.personId());

        return switch (event.eventType()) {
            case PERSON_CHANGED -> updateEmployee(event);
            case PERSON_CREATED -> createNewEmployee(event);
            case PERSON_DELETED -> offboardingEmployee(event);
        };
    }

    ResponseEntity<Void> createNewEmployee(HRWorksWebhookEvent event) {
        final HRWorksMasterData masterData =
            restClient.get().uri("/v2/persons/{personId}/master-data", event.personId())
                .retrieve()
                .body(HRWorksMasterData.class);
        if(masterData == null) {
            return ResponseEntity.notFound().build();
        }

        final SsoUser ssoUser = HRWorksMasterData.toSsoUser(masterData).orElseThrow();
        userService.create(ssoUser);
        return ResponseEntity.ok().build();
    }

    ResponseEntity<Void> updateEmployee(HRWorksWebhookEvent event) {
        final HRWorksMasterData masterData = restClient.get().uri("/v2/persons/{personId}/master-data", event.personId())
            .retrieve()
            .body(HRWorksMasterData.class);
        if(masterData == null) {
            return ResponseEntity.notFound().build();
        }

        final SsoUser ssoUser = HRWorksMasterData.toSsoUser(masterData).orElseThrow();
        userService.update(ssoUser);
        return ResponseEntity.ok().build();
    }

    ResponseEntity<Void> offboardingEmployee(HRWorksWebhookEvent event) {
        userService.terminateEmployee(event.personId());
        return ResponseEntity.ok().build();
    }
}
