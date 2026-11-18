package com.exo_PMT.pmt_backend.service;

import com.exo_PMT.pmt_backend.entity.ProjectMember;
import com.exo_PMT.pmt_backend.entity.User;
import com.exo_PMT.pmt_backend.model.ProjectMemberStatus;
import com.exo_PMT.pmt_backend.repository.ProjectMemberRepository;
import com.exo_PMT.pmt_backend.repository.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.Locale;
import java.util.regex.Pattern;

@Service
public class UserService {
    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$");

    private final UserRepository userRepository;
    private final ProjectMemberRepository projectMemberRepository;

    public UserService(UserRepository userRepository, ProjectMemberRepository projectMemberRepository) {
        this.userRepository = userRepository;
        this.projectMemberRepository = projectMemberRepository;
    }

    @Transactional
    public User register(String name, String email, String password) {
        String normalizedName = name == null ? "" : name.trim();
        String normalizedEmail = normalizeEmail(email);
        requireValidCredentials(normalizedEmail, password);
        if (normalizedName.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Le nom est obligatoire.");
        }
        if (userRepository.findByEmail(normalizedEmail).isPresent()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Cette adresse e-mail est déjà utilisée.");
        }

        User user = new User();
        user.setName(normalizedName);
        user.setEmail(normalizedEmail);
        user.setPassword(password);
        User savedUser = userRepository.save(user);
        acceptPendingInvitations(savedUser);
        return savedUser;
    }

    public User login(String email, String password) {
        String normalizedEmail = normalizeEmail(email);
        requireValidCredentials(normalizedEmail, password);

        return userRepository.findByEmail(normalizedEmail)
                .filter(user -> user.getPassword().equals(password))
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.UNAUTHORIZED,
                        "Adresse e-mail ou mot de passe incorrect."
                ));
    }

    static boolean isValidEmail(String normalizedEmail) {
        return EMAIL_PATTERN.matcher(normalizedEmail).matches();
    }

    static String normalizeEmail(String email) {
        return email == null ? "" : email.trim().toLowerCase(Locale.ROOT);
    }

    private void acceptPendingInvitations(User user) {
        for (ProjectMember invitation : projectMemberRepository.findByEmailAndStatus(
                user.getEmail(),
                ProjectMemberStatus.INVITED
        )) {
            invitation.setUserId(user.getId());
            invitation.setStatus(ProjectMemberStatus.ACTIVE);
            invitation.setJoinedAt(LocalDateTime.now());
        }
    }

    private void requireValidCredentials(String email, String password) {
        if (!isValidEmail(email)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "L'adresse e-mail est invalide.");
        }
        if (password == null || password.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Le mot de passe est obligatoire.");
        }
    }
}
