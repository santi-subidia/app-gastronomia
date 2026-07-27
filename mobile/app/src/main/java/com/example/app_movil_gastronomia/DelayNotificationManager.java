package com.example.app_movil_gastronomia;

import android.Manifest;
import android.app.Activity;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;

import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.Observer;

import com.example.app_movil_gastronomia.core.SignalRService;
import com.example.app_movil_gastronomia.core.TokenManager;
import com.example.app_movil_gastronomia.data.dto.signalr.DemoraRegistradaMessage;

import java.util.HashSet;
import java.util.Set;

public final class DelayNotificationManager {
    public static final int PERMISSION_REQUEST_CODE = 1001;

    private static final String CHANNEL_ID = "demoras_channel";

    private final Activity activity;
    private final LifecycleOwner lifecycleOwner;
    private final SignalRService signalRService;
    private final TokenManager tokenManager;
    private final Set<Integer> notifiedDemoraIds = new HashSet<>();
    private final Observer<DemoraRegistradaMessage> observer = this::onDelayRegistered;
    private boolean bound;

    public DelayNotificationManager(
            Activity activity,
            LifecycleOwner lifecycleOwner,
            SignalRService signalRService,
            TokenManager tokenManager) {
        this.activity = activity;
        this.lifecycleOwner = lifecycleOwner;
        this.signalRService = signalRService;
        this.tokenManager = tokenManager;
    }

    public void bind() {
        if (bound) return;
        signalRService.getDemoraRegistrada().observe(lifecycleOwner, observer);
        bound = true;
    }

    private void onDelayRegistered(DemoraRegistradaMessage message) {
        if (message == null || !isCajero()) return;
        if (!notifiedDemoraIds.add(message.getDemoraId())) return;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
                && ContextCompat.checkSelfPermission(activity, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(
                    activity,
                    new String[]{Manifest.permission.POST_NOTIFICATIONS},
                    PERMISSION_REQUEST_CODE);
            return;
        }

        createNotificationChannel();

        String content = activity.getString(
                R.string.delay_notification_content,
                message.getDemoraMinutos(),
                message.getSector(),
                message.getPedidoId());
        String bigText = content;
        if (message.getObservaciones() != null && !message.getObservaciones().isEmpty()) {
            bigText += "\n" + activity.getString(
                    R.string.delay_notification_observations,
                    message.getObservaciones());
        }

        Intent launchIntent = new Intent(activity, MainActivity.class);
        launchIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        launchIntent.putExtra("pedidoId", message.getPedidoId());

        PendingIntent pendingIntent = PendingIntent.getActivity(
                activity,
                message.getDemoraId(),
                launchIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        NotificationCompat.Builder notification = new NotificationCompat.Builder(activity, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_warning_24dp)
                .setContentTitle(activity.getString(R.string.delay_notification_title))
                .setContentText(content)
                .setStyle(new NotificationCompat.BigTextStyle().bigText(bigText))
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true)
                .setContentIntent(pendingIntent);

        NotificationManagerCompat.from(activity).notify(message.getDemoraId(), notification.build());
    }

    private boolean isCajero() {
        String role = tokenManager.getRole();
        return "Cajero".equalsIgnoreCase(role);
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return;

        NotificationChannel channel = new NotificationChannel(
                CHANNEL_ID,
                activity.getString(R.string.delay_notification_channel_name),
                NotificationManager.IMPORTANCE_HIGH);
        channel.setDescription(activity.getString(R.string.delay_notification_channel_description));

        NotificationManager notificationManager = activity.getSystemService(NotificationManager.class);
        if (notificationManager != null) {
            notificationManager.createNotificationChannel(channel);
        }
    }
}
