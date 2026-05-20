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
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.regex.Pattern;

import static java.lang.Math.sqrt;

@Service
public class UserService {

    private static final Logger logger = LoggerFactory.getLogger(UserService.class);

    @Value("${app.backend.url:http://localhost}")
    private String baseUrl;

    @Value("${app.avatars.port:8080}")
    private String port;

    private final UserRepository userRepository;
    private final FilterRepository filterRepository;
    private final FriendshipService friendshipService;

    private static final String USER_NOT_FOUND = "User not found for Google ID: ";
    private static final int XP_VOTED_INC = 10;
    private static final int XP_REPORTED_INC = 10;
    private static final int WALKING_METERS_PER_MINUTE = 75;
    private static final String USER_BANNED_MESSAGE = "El compte esta permanentment baneiat i no pot accedir a l'aplicacio.";
    private static final String USER_SUSPENDED_MESSAGE = "El compte esta suspes temporalment i no pot accedir a l'aplicacio.";
    private static final Pattern AVATAR_PRIZE_ID_PATTERN = Pattern.compile("^A\\d+_AVT(?:_.*)?$");
    private static final List<String> AVATAR_EXTENSIONS = List.of(".jpeg", ".jpg");
    private final NotificationService notificationService;
    private static final SecureRandom sr = new SecureRandom();

    public UserService(UserRepository userRepository, FilterRepository filterRepository, NotificationService notificationService, FriendshipService friendshipService) {
        this.userRepository = userRepository;
        this.filterRepository = filterRepository;
        this.notificationService = notificationService;
        this.friendshipService = friendshipService;
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
        UserResponseDTO u = this.getUserByGoogleId(googleId);
        int rowsAffected = userRepository.decrementPendingRewards(googleId);
        if (rowsAffected == 0) throw new BadRequestException("No rewards available to open.");
        PremiDTO p = pickRandomPrize(u);
        if (!Objects.equals(p.getUrl(), "XP")) userRepository.insertUserPrize(googleId, p.getId());
        else p.setUrl("NONE");
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

        for (String contactId : emergencyContactsGoogleId) {
            if (!friendshipService.existsFriendship(contactId, userGoogleId))
                throw new BadRequestException("Cannot add emergency contact if it is not a friend. No friendship exists between " + userGoogleId + " and " + contactId);
            userRepository.addEmergencyContact(userGoogleId, contactId);
        }

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
    }

    public void updateToken(String googleId, String token) {
        User u = userRepository.findByGoogleId(googleId)
                .orElseThrow(() -> new ResourceNotFoundException(USER_NOT_FOUND + googleId));
        u.setFcmToken(token);
        userRepository.save(u);
    }


    public boolean getUserStatusEmergency(String googleId) {
        User u = userRepository.findByGoogleId(googleId)
                .orElseThrow(() -> new ResourceNotFoundException(USER_NOT_FOUND + googleId));
        return u.getIsInEmergency();
    }

    @Transactional
    public void toggleUserStatusEmergency(String googleId) {
        User u = userRepository.findByGoogleId(googleId)
                .orElseThrow(() -> new ResourceNotFoundException(USER_NOT_FOUND + googleId));

        boolean status = !u.getIsInEmergency();
        u.setIsInEmergency(status);
        userRepository.save(u);

        List<String> users = getEmergencyContactsGoogleIds(googleId);

        //emergencia -> status = 1 -> avisa als contactes que estiguin pendents.
        //no emergencia -> status = 0 -> avisa als contactes que ja ha acabat tot.
        notificationService.sendEmergency(users, status, u.getUsername());
    }

    @Transactional
    public List<String> getEmergencyContactsGoogleIds(String googleId) {
        getUserProfileByGoogleId(googleId);
        return userRepository.getEmergencyContacts(googleId);
    }

    // --- MÉTODOS PRIVADOS DE AYUDA ---

    private PremiDTO pickRandomPrize(UserResponseDTO u) {
        Oddity oddity = pickRandomOddity();
        List<Premi> premisOddity = userRepository.getPrizesByOddity(oddity.getId());

        if (premisOddity.isEmpty()) throw new BadRequestException("No prizes found for oddity " + oddity.getId());

        int random = sr.nextInt(premisOddity.size());
        Premi premi = premisOddity.get(random);
        if (userRepository.userHasPrize(u.getGoogleId(), premi.getId())) {
            logger.warn("Prize {} already obtained by user, picking another one.", premi.getId());
            PremiDTO p = new PremiDTO();
            p.setUrl("XP");
            long xp = compensationXP(oddity, u);
            p.setId("XP_" + oddity.getId() + "_" + xp);
            return p;
        }
        PremiDTO prizeDto = new PremiDTO(premi);
        prizeDto.setUrl(resolvePrizeUrl(premi));
        return prizeDto;
    }

    private String resolvePrizeUrl(Premi premi) {
        String prizeId = premi.getId();
        if (prizeId == null || !AVATAR_PRIZE_ID_PATTERN.matcher(prizeId).matches()) {
            return premi.getUrl();
        }

        String cleanBaseUrl = baseUrl;
        while (cleanBaseUrl.endsWith("/")) {
            cleanBaseUrl = cleanBaseUrl.substring(0, cleanBaseUrl.length() - 1);
        }
        String cleanPort = port != null && !port.isEmpty() ? port : "8080";

        for (String extension : AVATAR_EXTENSIONS) {
            String resourcePath = "static/avatars/" + prizeId + extension;
            if (new ClassPathResource(resourcePath).exists()) {
                return cleanBaseUrl + ":" + cleanPort + "/avatars/" + prizeId + extension;
            }
        }

        return premi.getUrl();
    }

    private long compensationXP(Oddity oddity, UserResponseDTO us) {
        long lvl = us.getLevel();
        long nextLevelXp = getPointsByLevel(lvl + 1) - getPointsByLevel(lvl) ;
        long xp = Math.round(nextLevelXp * oddity.getPercentageLvlCompensation());
        userRepository.findByGoogleId(us.getGoogleId()).ifPresent(u -> {
            u.setPoints(u.getPoints() + xp);
            userRepository.save(u);
            calculateLevel(u);
        });
        return xp;
    }

    private Oddity pickRandomOddity() {
        List<Oddity> oddities = userRepository.getOdities();
        if (oddities.isEmpty()) throw new BadRequestException("No hi ha rareses configurades.");

        double totalWeight = oddities.stream().mapToDouble(Oddity::getProbability).sum();
        double r = sr.nextDouble() * totalWeight;

        double sum = 0.0;
        for (Oddity p : oddities) {
            sum += p.getProbability();
            if (r <= sum) {
                return p;
            }
        }
        return oddities.getLast();
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

    private Long getPointsByLevel(Long level) {
        return (long) Math.pow((level-1)/0.1, 2);
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