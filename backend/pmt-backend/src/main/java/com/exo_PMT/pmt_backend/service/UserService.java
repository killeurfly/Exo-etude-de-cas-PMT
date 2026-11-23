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

/**
 * Gestion des comptes : inscription, connexion et normalisation des adresses e-mail.
 * <p>
 * L'authentification est volontairement simple (mot de passe stocké en clair, pas de jeton) :
 * la sécurisation du backend n'est pas demandée par l'énoncé. Le client identifie ensuite
 * l'utilisateur par son {@code id}, passé en paramètre {@code userId} aux autres endpoints.
 */
@Service
public class UserService {
    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$");

    private final UserRepository userRepository;
    private final ProjectMemberRepository projectMemberRepository;

    public UserService(UserRepository userRepository, ProjectMemberRepository projectMemberRepository) {
        this.userRepository = userRepository;
        this.projectMemberRepository = projectMemberRepository;
    }

    /**
     * Crée un compte, puis active les invitations qui attendaient cette adresse e-mail :
     * la personne rejoint directement les projets où elle avait été invitée.
     *
     * @param name     nom affiché (espaces de début et de fin retirés)
     * @param email    adresse e-mail, normalisée avant d'être enregistrée
     * @param password mot de passe
     * @return l'utilisateur enregistré
     * @throws ResponseStatusException 400 si le nom est vide, l'e-mail invalide ou le mot de passe vide ;
     *                                 409 si l'adresse e-mail est déjà utilisée
     */
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

    /**
     * Vérifie les identifiants d'un utilisateur.
     *
     * @param email    adresse e-mail (la casse et les espaces sont ignorés)
     * @param password mot de passe
     * @return l'utilisateur correspondant
     * @throws ResponseStatusException 400 si l'e-mail est invalide ou le mot de passe vide ;
     *                                 401 si l'e-mail est inconnu ou le mot de passe incorrect
     */
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

    /**
     * Vérifie qu'une adresse déjà normalisée a la forme {@code nom@domaine.ext}.
     * Partagée avec {@link ProjectMemberService} pour valider les invitations.
     */
    static boolean isValidEmail(String normalizedEmail) {
        return EMAIL_PATTERN.matcher(normalizedEmail).matches();
    }

    /**
     * Met une adresse sous sa forme canonique (espaces retirés, minuscules) : deux saisies
     * qui ne diffèrent que par la casse désignent ainsi le même compte. {@code null} devient {@code ""}.
     */
    static String normalizeEmail(String email) {
        return email == null ? "" : email.trim().toLowerCase(Locale.ROOT);
    }

    /**
     * Passe à ACTIVE les invitations en attente adressées à l'e-mail du nouvel utilisateur.
     * Pas d'appel à {@code save} : les entités chargées sont suivies par la transaction de
     * {@link #register} et enregistrées à sa fin.
     */
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

    /** Lève une erreur 400 si l'e-mail est invalide ou si le mot de passe est vide. */
    private void requireValidCredentials(String email, String password) {
        if (!isValidEmail(email)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "L'adresse e-mail est invalide.");
        }
        if (password == null || password.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Le mot de passe est obligatoire.");
        }
    }
}
