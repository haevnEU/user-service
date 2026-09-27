package de.haevn.userservice;

import de.haevn.userservice.v2.model.SsoApplication;
import de.haevn.userservice.v2.model.SsoUser;
import de.haevn.userservice.v2.repository.ApplicationRepository;
import de.haevn.userservice.v2.repository.SsoUserRepository;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;


@RequiredArgsConstructor
@Component
@Slf4j
class UserInitializer implements CommandLineRunner {
private final ApplicationRepository applicationRepository;
    private final SsoUserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    @Override
    public void run(String... args) throws Exception {

        System.out.println("==============================");
        System.out.println("Create Test data");
        System.out.println("==============================");
        createAdmin();
        System.out.println("==============================");
    }

    private void createAdmin(){
        // In deinem UserInitializer:
        SsoUser admin = new SsoUser();
        admin.setId("PER-101");
        admin.setFirstName("Root");
        admin.setLastName("root");
        admin.setMail("root@localhost.de");
        admin.setOrganizationalUnitId("N/A");
        admin.setPassword(passwordEncoder.encode("12345678"));
        admin.setRoles(List.of("ROLE_ADMIN", "ROLE_USER"));
        admin.setActive(true);
        admin.setNew(true);

        System.out.println("Create Admin");
        System.out.println("E-Mail: " +  admin.getMail());
        System.out.println("Password: 12345678");

        userRepository.save(admin);
    }

}