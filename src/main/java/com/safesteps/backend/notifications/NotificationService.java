package com.safesteps.backend.notifications;

import com.google.firebase.FirebaseApp;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.Message;
import com.safesteps.backend.domain.chats.dto.MessageResponseDTO;
import com.safesteps.backend.domain.users.model.FriendshipStatus;
import com.safesteps.backend.domain.users.model.User;
import com.safesteps.backend.domain.users.repository.UserRepository;
import com.safesteps.backend.notifications.dto.EmergencyLocation;
import com.safesteps.backend.notifications.dto.FriendRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class NotificationService {

    private static final Logger logger = LoggerFactory.getLogger(NotificationService.class);

    private final SimpMessagingTemplate messagingTemplate;
    private final UserRepository userRepository;

    private static final String EMERGENCY_TITLE = "EMERGENCY_TITLE";
    private static final String EMERGENCY_BODY = "EMERGENCY_BODY";

    private static final String EMERGENCY_END_TITLE = "EMERGENCY_END_TITLE";
    private static final String EMERGENCY_END_BODY = "EMERGENCY_END_BODY";

    private static final String MSG_TITLE = "NEW_MESSAGE_TITLE";
    private static final String MSG_BODY = "NEW_MESSAGE_BODY";

    private static final String FRIEND_REQ_TITLE = "FRIEND_REQ_TITLE";
    private static final String FRIEND_REQ_BODY = "FRIEND_REQ_BODY";

    public NotificationService(SimpMessagingTemplate messagingTemplate, UserRepository userRepository) {
        this.messagingTemplate = messagingTemplate;
        this.userRepository = userRepository;
    }

    public void sendMessage(List<String> toGoogleId, MessageResponseDTO responseDTO) {
        send(toGoogleId, MSG_TITLE, MSG_BODY, responseDTO, "/queue/messages");
    }

    public void sendEmergency(List<String> toGoogleId, boolean isInEmergency, String usernameInDanger) {
        String title = isInEmergency ? EMERGENCY_TITLE : EMERGENCY_END_TITLE;
        String body = isInEmergency ? EMERGENCY_BODY : EMERGENCY_END_BODY;
        send(toGoogleId, title, body, usernameInDanger, "/queue/emergency");
    }

    public void sendLocationUpdate(List<String> toGoogleIds, EmergencyLocation coords) {
        for (String toGoogleId : toGoogleIds) {
            webSocketNotification(toGoogleId, null, null, coords, "/queue/location");
        }

    }

    public void sendFriendRequest(String toGoogleId, FriendRequest fr) {
        String title = FRIEND_REQ_TITLE;
        String body = FRIEND_REQ_BODY;
        if (fr.getStatus() == FriendshipStatus.ACCEPTED) {
            title = "FRIEND_ACC_TITLE";
            body = "FRIEND_ACC_BODY";
        } else if (fr.getStatus() == FriendshipStatus.REJECTED) {
            title = "FRIEND_REJ_TITLE";
            body = "FRIEND_REJ_BODY";
        }
        send(List.of(toGoogleId), title, body, fr, "/queue/requests");
    }


    /**
     * Metode principal per a enviar notificacions.
     * Decideix entre WebSocket o Firebase segons l'estat de connexió de l'usuari.
     * Si falla el ws, envia via firebase
     */
    private void send(List<String> googleIds, String title, String body, Object payload, String path) {
        for (String googleId : googleIds) {
            if (googleId == null) {
                logger.error("Google Id is null");
                continue;
            }

            User user = userRepository.findByGoogleId(googleId).orElse(null);
            if (user == null) {
                logger.error("User not found for notification: {}", googleId);
                continue;
            }

            boolean isOnline = user.getIsOnline() != null && user.getIsOnline();

            if (isOnline) {
                boolean beenSent = webSocketNotification(googleId, title, body, payload, path);
                if (!beenSent) {
                    logger.error("WebSocket failed, falling back to Push for: {}", googleId);
                    firebaseNotification(user.getFcmToken(), title, body);
                }
            } else {
                firebaseNotification(user.getFcmToken(), title, body);
            }
        }
    }


    //push notifications
    private void firebaseNotification(String fcmToken, String title, String body) {
        if (fcmToken == null || fcmToken.isEmpty()) {
            logger.warn("Skipping push: No FCM token for this user.");
            return;
        }
        if (FirebaseApp.getApps().isEmpty()) {
            logger.warn("Firebase no inicialitzat");
            return;
        }
        try {
            Message message = Message.builder()
                    .setToken(fcmToken)
                    .putData("title_loc_key", title)
                    .putData("body_loc_key", body)
                    .build();

            FirebaseMessaging.getInstance().send(message);
            logger.info("Push notification sent to token: {}.", fcmToken);
        } catch (Exception e) {
            logger.error("Error while sending push notification to googleId: {}. Error: {}", fcmToken, e.getMessage());
        }
    }

    //ws notifications
    private boolean webSocketNotification(String googleId, String titleKey, String bodyKey, Object payload, String path) {
        try {
            Map<String, Object> newPayload = new HashMap<>();
            newPayload.put("data", payload);
            newPayload.put("titleKey", titleKey);
            newPayload.put("bodyKey", bodyKey);

            messagingTemplate.convertAndSendToUser(googleId, path, newPayload);
            logger.info("Notification sent via WebSocket to googleId: {}", googleId);
            return true;
        } catch (Exception e) {
            logger.error("WS error for {}: {}", googleId, e.getMessage());
            userRepository.updateOnlineStatus(googleId, false);
            return false;
        }
    }
}
