package com.safesteps.backend.domain.users.service;

import com.safesteps.backend.domain.common.exception.ResourceNotFoundException;
import com.safesteps.backend.domain.common.exception.BadRequestException;
import com.safesteps.backend.domain.common.exception.UserBannedException;
import com.safesteps.backend.domain.common.exception.UserSuspendedException;
import com.safesteps.backend.domain.incidents.model.Vote;
import com.safesteps.backend.domain.users.dto.FilterRequestDTO;
import com.safesteps.backend.domain.users.dto.PremiDTO;
import com.safesteps.backend.domain.users.dto.RouteCompletionResponseDTO;
import com.safesteps.backend.domain.users.dto.UserRequestDTO;
import com.safesteps.backend.domain.users.dto.UserResponseDTO;
import com.safesteps.backend.domain.users.dto.UserProfileDTO;
import com.safesteps.backend.domain.users.dto.UserSearchResultDTO;
import com.safesteps.backend.domain.users.model.*;
import com.safesteps.backend.domain.users.repository.FilterRepository;
import com.safesteps.backend.domain.users.repository.UserRepository;
import com.safesteps.backend.notifications.NotificationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

import static java.lang.Math.sqrt;

@Service
public class UserService {

    private static final Logger logger = LoggerFactory.getLogger(UserService.class);


    private final UserRepository userRepository;
    private final FilterRepository filterRepository;

    private static final String USER_NOT_FOUND = "User not found for Google ID: ";
    private static final int XP_VOTED_INC = 10;
    private static final int XP_REPORTED_INC = 10;
    private static final int WALKING_METERS_PER_MINUTE = 75;
    private static final String USER_BANNED_MESSAGE = "El compte esta permanentment baneiat i no pot accedir a l'aplicacio.";
    private static final String USER_SUSPENDED_MESSAGE = "El compte esta suspes temporalment i no pot accedir a l'aplicacio.";
    private final NotificationService notificationService;

    public UserService(UserRepository userRepository, FilterRepository filterRepository, NotificationService notificationService) {
        this.userRepository = userRepository;
        this.filterRepository = filterRepository;
        this.notificationService = notificationService;
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
            throw new UserBannedException(USER_BANNED_MESSAGE);
        }
        if (user.getStatus() == UserStatus.SUSPENDED) {
            throw new UserSuspendedException(USER_SUSPENDED_MESSAGE);
        }

        return new UserResponseDTO(user);
    }

    @Transactional(readOnly = true)
    public UserProfileDTO getUserProfile(String googleId) {
        User user = userRepository.findByGoogleId(googleId)
                .orElseThrow(() -> new ResourceNotFoundException(USER_NOT_FOUND + googleId));

        if (user.getStatus() == UserStatus.BANNED) {
            throw new UserBannedException(USER_BANNED_MESSAGE);
        }
        if (user.getStatus() == UserStatus.SUSPENDED) {
            throw new UserSuspendedException(USER_SUSPENDED_MESSAGE);
        }

        return new UserProfileDTO(user);
    }

    @Transactional(readOnly = true)
    public List<UserSearchResultDTO> searchUsersByUsername(String query) {
        return userRepository.findByUsernameContainingIgnoreCaseOrEmailContainingIgnoreCase(query, query)
                .stream()
                .filter(u -> Boolean.FALSE.equals(u.getIsAnonymous()))
                .filter(u -> u.getStatus() != UserStatus.BANNED)
                .map(UserSearchResultDTO::new)
                .toList();
    }

    @Transactional(readOnly = true)
    public UserProfileDTO getUserProfileByEmail(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found for email: " + email));

        return new UserProfileDTO(user);
    }

    @Transactional(readOnly = true)
    public UserProfileDTO getUserProfileByGoogleId(String googleId) {
        User user = userRepository.findByGoogleId(googleId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found for googleId: " + googleId));

        return new UserProfileDTO(user);
    }

    @Transactional(readOnly = true)
    public UserResponseDTO getUserByEmail(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found for email: " + email));

        if (user.getStatus() == UserStatus.BANNED) {
            throw new UserBannedException(USER_BANNED_MESSAGE);
        }
        if (user.getStatus() == UserStatus.SUSPENDED) {
            throw new UserSuspendedException(USER_SUSPENDED_MESSAGE);
        }

        return new UserResponseDTO(user);
    }

    @Transactional
    public UserResponseDTO createUser(UserRequestDTO req) {
        Optional<User> existingByGoogleId = userRepository.findByGoogleId(req.getGoogleId());
        if (existingByGoogleId.isPresent()) {
            User user = existingByGoogleId.get();
            if (user.getStatus() == UserStatus.BANNED) {
                throw new UserBannedException(USER_BANNED_MESSAGE);
            }
            if (user.getStatus() == UserStatus.SUSPENDED) {
                throw new UserSuspendedException(USER_SUSPENDED_MESSAGE);
            }
            throw new BadRequestException("User already exists with the provided email or Google ID.");
        }

        Optional<User> existingByEmail = userRepository.findByEmail(req.getEmail());
        if (existingByEmail.isPresent()) {
            User user = existingByEmail.get();
            if (user.getStatus() == UserStatus.BANNED) {
                throw new UserBannedException(USER_BANNED_MESSAGE);
            }
            if (user.getStatus() == UserStatus.SUSPENDED) {
                throw new UserSuspendedException(USER_SUSPENDED_MESSAGE);
            }
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

    @Transactional
    public PremiDTO openPrize(String googleId) {
        this.getUserByGoogleId(googleId);
        int rowsAffected = userRepository.decrementPendingRewards(googleId);
        if (rowsAffected == 0) throw new BadRequestException("No rewards available to open.");
        List<Premi> premis = userRepository.getUserAvailablePrizes(googleId);
        PremiDTO p = pickRandomPrize(premis);
        userRepository.insertUserPrize(googleId, p.getId());
        return p;
    }

    @Transactional
    public RouteCompletionResponseDTO completeRoute(String googleId, double meters) {
        if (!Double.isFinite(meters) || meters < 0) {
            throw new BadRequestException("Route meters must be zero or greater.");
        }

        User user = userRepository.findByGoogleId(googleId)
                .orElseThrow(() -> new ResourceNotFoundException(USER_NOT_FOUND + googleId));

        Long previousLevel = user.getLevel();
        Long pointsAdded = calculateRoutePoints(meters);
        user.setPoints(user.getPoints() + pointsAdded);
        userRepository.save(user);
        calculateLevel(user);

        return new RouteCompletionResponseDTO(
                user.getLevel(),
                user.getLevel() > previousLevel,
                pointsAdded,
                user.getPoints(),
                user.getRecompenses()
        );
    }

    @Transactional
    public List<UserProfileDTO> newEmergencyContact(String userGoogleId, List<String> emergencyContactsGoogleId) {
        if (emergencyContactsGoogleId.contains(userGoogleId))
            throw new BadRequestException("Cannot add yourself as an emergency contact.");

        for (String contactId : emergencyContactsGoogleId)
            userRepository.addEmergencyContact(userGoogleId, contactId);

        return getEmergencyContacts(userGoogleId);
    }

    @Transactional
    public void deleteEmergencyContact(String userGoogleId, List<String> emergencyContactsGoogleId) {
        if (emergencyContactsGoogleId != null && !emergencyContactsGoogleId.isEmpty())
            userRepository.deleteEmergencyContacts(userGoogleId, emergencyContactsGoogleId);

    }

    public List<UserProfileDTO> getEmergencyContacts(String googleId) {
        List<String> contactIds = userRepository.getEmergencyContacts(googleId);
        if (contactIds.isEmpty()) return List.of();

        List<User> contacts = userRepository.findByGoogleIdIn(contactIds);

        return contacts.stream()
                .map(UserProfileDTO::new)
                .toList();
    public void updateToken(String googleId, String token) {
        User u = userRepository.findByGoogleId(googleId)
                .orElseThrow(() -> new ResourceNotFoundException(USER_NOT_FOUND + googleId));
        u.setFcmToken(token);
        userRepository.save(u);
    }

    public void sendPushNotification(String googleId, String title, String body, int type) {
        try {
            notificationService.sendNotification(googleId, title, body, type);
        } catch (ResourceNotFoundException e) {
            // si no existeix l'usuari o no te token, NO petem l'execucio, nomes loggem l'error i retornem (l'user no ho ha de saber)
            logger.error("Error while getting fcm token for googleId: {}.", googleId);
        }

    }

    // --- MÉTODOS PRIVADOS DE AYUDA ---


    private PremiDTO pickRandomPrize(List<Premi> premis) {
        double totalWeight = 0;
        for (Premi p : premis) totalWeight += p.getProbability();

        SecureRandom sr = new SecureRandom();
        double r = sr.nextDouble() * totalWeight;

        double sum = 0.0;

        Premi result = null;

        for (Premi p : premis) {
            sum += p.getProbability();
            if (r <= sum) {
                result = p;
                break;
            }
        }
        if (!premis.isEmpty() && result == null) result = premis.getFirst();
        if (result == null) throw new BadRequestException("No prizes available to open.");
        return new PremiDTO(result);
    }

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

    private Long calculateRoutePoints(double meters) {
        long estimatedMinutes = Math.round(meters / WALKING_METERS_PER_MINUTE);
        if (estimatedMinutes == 0 && meters > 0) return 1L;
        return estimatedMinutes;
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