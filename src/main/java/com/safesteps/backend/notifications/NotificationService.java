package com.safesteps.backend.notifications;

import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.Message;
import com.safesteps.backend.domain.users.model.User;
import com.safesteps.backend.domain.users.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import java.util.HashMap;
import java.util.Map;

@Service
public class NotificationService {

    private static final Logger logger = LoggerFactory.getLogger(NotificationService.class);

    private final SimpMessagingTemplate messagingTemplate;
    private final UserRepository userRepository;

    private static final String EMERGENCY_TITLE = "EMERGENCY_TITLE";
    private static final String EMERGENCY_BODY = "EMERGENCY_BODY";

    private static final String MSG_TITLE = "NEW_MESSAGE_TITLE";
    private static final String MSG_BODY = "NEW_MESSAGE_BODY";

    private static final String FRIEND_REQ_TITLE = "FRIEND_REQ_TITLE";
    private static final String FRIEND_REQ_BODY = "FRIEND_REQ_BODY";

    public NotificationService(SimpMessagingTemplate messagingTemplate, UserRepository userRepository) {
        this.messagingTemplate = messagingTemplate;
        this.userRepository = userRepository;
    }

    // todo msg_body should be msg
    public void sendMessage(String toGoogleId, Object msg) {
        send(toGoogleId, MSG_TITLE, MSG_BODY, msg, "/queue/messages");
    }

    public void sendEmergency(String toGoogleId, Object emergency) {
        send(toGoogleId, EMERGENCY_TITLE, EMERGENCY_BODY, emergency, "/queue/emergency");
    }

    public void sendLocationUpdate(String toGoogleId, Object coords) {
        webSocketNotification(toGoogleId, null, null, coords, "/queue/location");
    }

    public void sendFriendRequest(String toGoogleId, Object requestInfo) {
        send(toGoogleId, FRIEND_REQ_TITLE, FRIEND_REQ_BODY, requestInfo, "/queue/requests");
    }

    // todo: Eliminar aquesta funcio un cop s'han provat les ws notifications
    public void sendNotification(String toGoogleId, String title, String body, int type) {
        switch (type) {
            case 1 -> sendMessage(toGoogleId, null);
            case 2 -> sendEmergency(toGoogleId, null);
            case 3 -> sendFriendRequest(toGoogleId, null);
            case 4 -> sendLocationUpdate(toGoogleId, null);
            case 5 -> firebaseNotification(userRepository.findByGoogleId(toGoogleId).map(User::getFcmToken).orElse(null), title, body);
            case 6 -> webSocketNotification(toGoogleId, title, body, null, "/queue/notifications");
            default -> logger.warn("Unknown notification type: {}", type);
        }
    }

    /**
     * Metode principal per a enviar notificacions.
     * Decideix entre WebSocket o Firebase segons l'estat de connexió de l'usuari.
     * Si falla el ws, envia via firebase
     */
    private void send(String googleId, String title, String body, Object payload, String path) {
        if (googleId == null) {
            logger.error("Google Id is null");
            return;
        }

        User user = userRepository.findByGoogleId(googleId).orElse(null);
        if (user == null) {
            logger.error("User not found for notification: {}", googleId);
            return;
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


    //push notifications
    private void firebaseNotification(String fcmToken, String title, String body) {
        if (fcmToken == null || fcmToken.isEmpty()) {
            logger.warn("Skipping push: No FCM token for this user.");
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
