package de.haevn.userservice.v2.cron;

import de.haevn.userservice.v2.service.UserService;
import java.time.Instant;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestTemplate;

@Log4j2
@Component
@RequiredArgsConstructor
public class HRWorksScheduler {
    private final UserService  userService;
    private final RestClient restClient;
    @Scheduled(cron = "0 0 1 * * ?")
    @Transactional
    public void syncWithHRWorks() {
        log.info("Start synchronizing with HR works");
        // todo run sync with HR Works to ensure that all data is correct an dno changes are missed

    }
}
