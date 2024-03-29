package com.example.accessibilityplay;

import android.util.Log;

import androidx.annotation.NonNull;

import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

public class NotificationService extends FirebaseMessagingService {
    @Override
    public void onMessageReceived(@NonNull RemoteMessage message) {
        Log.d("Receiver", "onMessageReceived: " + message.getNotification().getBody());
        CoreService.coreService.broadcastInStudySurvey(message.getNotification().getTitle(), message.getNotification().getBody());
        super.onMessageReceived(message);
    }

    @Override
    public void onNewToken(@NonNull String token) {
        super.onNewToken(token);
    }
}
