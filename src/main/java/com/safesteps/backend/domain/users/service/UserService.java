package com.safesteps.backend.domain.users.service;

import com.safesteps.backend.domain.common.exception.ResourceNotFoundException;
import com.safesteps.backend.domain.common.exception.BadRequestException;
import com.safesteps.backend.domain.common.exception.UserForbiddenException;
import com.safesteps.backend.domain.incidents.model.Vote;
import com.safesteps.backend.domain.users.dto.FilterRequestDTO;
import com.safesteps.backend.domain.users.dto.UserRequestDTO;
import com.safesteps.backend.domain.users.dto.UserResponseDTO;
import com.safesteps.backend.domain.users.model.User;
import com.safesteps.backend.domain.users.model.UserFilter;
import com.safesteps.backend.domain.users.model.UserStatus;
import com.safesteps.backend.domain.users.repository.FilterRepository;
import com.safesteps.backend.domain.users.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.function.Consumer;

import static java.lang.Math.sqrt;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final FilterRepository filterRepository;

    private static final String USER_NOT_FOUND = "User not found for Google ID: ";
    private static final int XP_VOTED_INC = 10;
    private static final int XP_REPORTED_INC = 10;

    public UserService(UserRepository userRepository, FilterRepository filterRepository) {
        this.userRepository = userRepository;
        this.filterRepository = filterRepository;
    }

    @Transactional(readOnly = true)
    public List<UserResponseDTO> getAllUsers() {
        return userRepository.findAll().stream()
                .map(UserResponseDTO::new)
                .toList();
    }

    @Transactional(readOnly = true)
    public UserResponseDTO getUserByGoogleId(String googleId) {
        User user = userRepository.findByGoogleId(googleId)
                .orElseThrow(() -> new ResourceNotFoundException(USER_NOT_FOUND + googleId));

        if (user.getStatus() == UserStatus.BANNED) {
            throw new UserForbiddenException(
                    "El compte esta permanentment baneiat i no pot accedir a l'aplicacio.",
                    "USER_BANNED"
            );
        }
        if (user.getStatus() == UserStatus.SUSPENDED) {
            throw new UserForbiddenException(
                    "El compte esta suspes temporalment i no pot accedir a l'aplicacio.",
                    "USER_SUSPENDED"
            );
        }

        return new UserResponseDTO(user);
    }

    @Transactional(readOnly = true)
    public UserResponseDTO getUserByEmail(String email) {
        return userRepository.findByEmail(email)
                .map(UserResponseDTO::new)
                .orElseThrow(() -> new ResourceNotFoundException("User not found for email: " + email));
    }

    @Transactional
    public UserResponseDTO createUser(UserRequestDTO req) {
        if (userRepository.existsByEmail(req.getEmail()) || userRepository.existsByGoogleId(req.getGoogleId())) {
            throw new BadRequestException("User already exists with the provided email or Google ID.");
        }

        User user = new User();
        user.setEmail(req.getEmail());
        user.setUsername(req.getUsername());
        user.setGoogleId(req.getGoogleId());
        user.setPictureUrl(req.getPictureUrl());
        user.setLanguage(req.getLanguage());
        user.setIsAnonymous(req.getIsAnonymous());
        user.setPoints(0L);
        user.setLevel(1L);
        user.setReputacio(1);

        User savedUser = userRepository.save(user);

        // Crear filtros por defecto para el nuevo usuario
        UserFilter defaultFilter = new UserFilter();
        defaultFilter.setGoogleId(savedUser.getGoogleId());
        filterRepository.save(defaultFilter);

        return new UserResponseDTO(savedUser);
    }

    @Transactional
    public UserResponseDTO updateUser(String googleId, UserRequestDTO req) {
        return userRepository.findByGoogleId(googleId).map(user -> {
            user.setUsername(req.getUsername());
            user.setPictureUrl(req.getPictureUrl());
            user.setLanguage(req.getLanguage());
            user.setIsAnonymous(req.getIsAnonymous());
            return new UserResponseDTO(userRepository.save(user));
        }).orElseThrow(() -> new ResourceNotFoundException(USER_NOT_FOUND + googleId));
    }

    @Transactional
    public void deleteUserByGoogleId(String googleId) {
        User user = userRepository.findByGoogleId(googleId)
                .orElseThrow(() -> new ResourceNotFoundException(USER_NOT_FOUND + googleId));

        filterRepository.deleteById(googleId);
        userRepository.delete(user);
    }

    @Transactional
    public UserFilter updateFilters(String googleId, FilterRequestDTO req) {
        return filterRepository.findById(googleId).map(f -> {
            // Actualización con los nuevos nombres en camelCase
            updateIfPresent(req.getComissaries(), f::setComissaries);
            updateIfPresent(req.getFetsPenals(), f::setFetsPenals);
            updateIfPresent(req.getCameresSeguretat(), f::setCameresSeguretat);
            updateIfPresent(req.getInfraccions(), f::setInfraccions);
            updateIfPresent(req.getFontsAigua(), f::setFontsAigua);
            updateIfPresent(req.getBancs(), f::setBancs);
            updateIfPresent(req.getContaminacioAcustica(), f::setContaminacioAcustica);
            updateIfPresent(req.getEscalesMecaniques(), f::setEscalesMecaniques);
            updateIfPresent(req.getArbres(), f::setArbres);
            updateIfPresent(req.getRefugisClimatics(), f::setRefugisClimatics);
            updateIfPresent(req.getQualitatAire(), f::setQualitatAire);

            return filterRepository.save(f);
        }).orElseThrow(() -> new ResourceNotFoundException("User filters not found for Google ID: " + googleId));
    }

    @Transactional
    public UserResponseDTO updateLanguage(String googleId, String language) {
        return userRepository.findByGoogleId(googleId).map(user -> {
            user.setLanguage(language);
            return new UserResponseDTO(userRepository.save(user));
        }).orElseThrow(() -> new ResourceNotFoundException(USER_NOT_FOUND + googleId));
    }

    @Transactional
    public void updateUserReliability(List<Vote> votes, boolean isIncidentAccepted, String creatorGoogleId) {
        for (Vote v : votes) {
            userRepository.findByGoogleId(v.getGoogleId())
                    .ifPresent(user -> processUserReliability(user, v, isIncidentAccepted));
        }

        if (creatorGoogleId != null) {
            userRepository.findByGoogleId(creatorGoogleId)
                    .ifPresent(creator -> processCreatorReliability(creator, isIncidentAccepted));
        }
    }

    @Transactional
    public void rewardForExpiredIncident(List<Vote> votes, String creatorGoogleId) {
        for (Vote v : votes) {
            userRepository.findByGoogleId(v.getGoogleId()).ifPresent(u -> {
                u.setPoints(u.getPoints() + XP_VOTED_INC);
                userRepository.save(u);
                calculateLevel(u);
            });
        }
        if (creatorGoogleId != null) {
            userRepository.findByGoogleId(creatorGoogleId).ifPresent(c -> {
                c.setPoints(c.getPoints() + XP_REPORTED_INC);
                userRepository.save(c);
                calculateLevel(c);
            });
        }
    }

    // --- MÉTODOS PRIVADOS DE AYUDA ---

    private void processUserReliability(User user, Vote vote, boolean isIncidentAccepted) {
        boolean isVoteCorrect = (isIncidentAccepted && vote.getScore() > 0) || (!isIncidentAccepted && vote.getScore() < 0);

        if (isVoteCorrect) {
            user.setReputacio(user.getReputacio() + 0.01);
            user.setPoints(user.getPoints() + XP_VOTED_INC);
        } else {
            user.setReputacio(user.getReputacio() - 0.01);
        }
        userRepository.save(user);
        calculateLevel(user);
    }

    private void processCreatorReliability(User creator, boolean isIncidentAccepted) {
        if (isIncidentAccepted) {
            creator.setReputacio(creator.getReputacio() + 0.01);
            creator.setPoints(creator.getPoints() + XP_REPORTED_INC);
        } else {
            creator.setReputacio(creator.getReputacio() - 0.01);
        }
        userRepository.save(creator);
        calculateLevel(creator);
    }

    private void calculateLevel(User u) {
        int lvl = u.getLevel().intValue();
        Long points = u.getPoints();
        Long currLvl = getLevelByPoints(points);
        if (currLvl > lvl) {
            long diff = currLvl - lvl;
            long rew = u.getRecompenses() == null? 0L : u.getRecompenses();
            u.setLevel(currLvl);
            u.setRecompenses(rew+diff);
            userRepository.save(u);
        }
    }

    private Long getLevelByPoints(Long points) {
        return (long) (0.1*sqrt(points) + 1);
    }

    /**
     * Helper para actualizar solo si el valor no es nulo (evita pisar datos con nulls del DTO)
     */
    private <T> void updateIfPresent(T value, Consumer<T> setter) {
        if (value != null) {
            setter.accept(value);
        }
    }
}