package com.seveneleven.config;

import com.seveneleven.entity.User;
import com.seveneleven.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class DataLoader implements CommandLineRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) {
        if (userRepository.count() == 0) {
            log.info("Seeding default users...");
            
            User admin = User.builder()
                    .email("admin@7eleven.com")
                    .password(passwordEncoder.encode("Admin@123"))
                    .role(User.Role.ROLE_ADMIN)
                    .isDeleted(false)
                    .build();
            
            User user = User.builder()
                    .email("user@7eleven.com")
                    .password(passwordEncoder.encode("User@123"))
                    .role(User.Role.ROLE_USER)
                    .isDeleted(false)
                    .build();

            userRepository.save(admin);
            userRepository.save(user);
            
            log.info("Default users created successfully!");
        }
    }
}
