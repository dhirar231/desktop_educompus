package com.educompus.service;

import com.educompus.model.SessionLive;
import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.extensions.java6.auth.oauth2.AuthorizationCodeInstalledApp;
import com.google.api.client.extensions.jetty.auth.oauth2.LocalServerReceiver;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow;
import com.google.api.client.googleapis.auth.oauth2.GoogleClientSecrets;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.client.util.DateTime;
import com.google.api.client.util.store.FileDataStoreFactory;
import com.google.api.services.calendar.Calendar;
import com.google.api.services.calendar.CalendarScopes;
import com.google.api.services.calendar.model.Event;
import com.google.api.services.calendar.model.EventDateTime;
import com.google.api.services.calendar.model.EventReminder;

import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Service d'intégration avec Google Calendar.
 *
 * <p>Permet de créer, mettre à jour et supprimer des événements Google Calendar
 * correspondant aux sessions live EduCompus.
 *
 * <p>Nécessite un fichier {@code credentials.json} dans les ressources.
 * Si absent, toutes les opérations sont ignorées silencieusement.
 */
public final class GoogleCalendarService {

    private static final String APPLICATION_NAME = "EduCompus Session Live";
    private static final JsonFactory JSON_FACTORY  = GsonFactory.getDefaultInstance();
    private static final String TOKENS_DIR         = "tokens_calendar";
    private static final String CALENDAR_ID        = "primary";
    private static final List<String> SCOPES       =
            Collections.singletonList(CalendarScopes.CALENDAR);

    // ── API publique ──────────────────────────────────────────────────────────

    /**
     * Crée un événement Google Calendar pour une session live.
     *
     * @param session La session à synchroniser
     * @return L'ID de l'événement Google Calendar créé, ou null si échec/non configuré
     */
    public String creerEvenement(SessionLive session) {
        if (!isConfigured()) return null;
        try {
            Calendar service = buildService();
            Event event = buildEvent(session);
            Event created = service.events().insert(CALENDAR_ID, event).execute();
            System.out.println("[Calendar] Événement créé : " + created.getId());
            return created.getId();
        } catch (Exception e) {
            System.err.println("[Calendar] Erreur création événement: " + e.getMessage());
            return null;
        }
    }

    /**
     * Met à jour un événement Google Calendar existant.
     *
     * @param session La session avec les nouvelles données (doit avoir googleEventId)
     */
    public void mettreAJourEvenement(SessionLive session) {
        if (!isConfigured() || !session.estSynchroniseeCalendar()) return;
        try {
            Calendar service = buildService();
            Event event = buildEvent(session);
            service.events().update(CALENDAR_ID, session.getGoogleEventId(), event).execute();
            System.out.println("[Calendar] Événement mis à jour : " + session.getGoogleEventId());
        } catch (Exception e) {
            System.err.println("[Calendar] Erreur mise à jour événement: " + e.getMessage());
        }
    }

    /**
     * Supprime un événement Google Calendar.
     *
     * @param googleEventId L'ID de l'événement à supprimer
     */
    public void supprimerEvenement(String googleEventId) {
        if (!isConfigured() || googleEventId == null || googleEventId.isBlank()) return;
        try {
            Calendar service = buildService();
            service.events().delete(CALENDAR_ID, googleEventId).execute();
            System.out.println("[Calendar] Événement supprimé : " + googleEventId);
        } catch (Exception e) {
            System.err.println("[Calendar] Erreur suppression événement: " + e.getMessage());
        }
    }

    /**
     * Vérifie si les credentials Google Calendar sont disponibles et non vides.
     */
    public static boolean isConfigured() {
        try (InputStream in = GoogleCalendarService.class.getResourceAsStream("/credentials.json")) {
            if (in == null) return false;
            // Vérifier que le fichier contient un vrai client_id (pas l'exemple)
            String content = new String(in.readAllBytes());
            return content.contains("client_id") && 
                   !content.contains("YOUR_CLIENT_ID") &&
                   !content.contains("\"client_id\":\"\"");
        } catch (Exception e) {
            return false;
        }
    }

    // ── Construction de l'événement ───────────────────────────────────────────

    private Event buildEvent(SessionLive session) {
        Event event = new Event();

        // Titre
        event.setSummary("🔴 Session Live : " + safe(session.getNomCours()));

        // Description avec lien
        event.setDescription(
            "Session live EduCompus\n" +
            "Cours : " + safe(session.getNomCours()) + "\n" +
            "Lien de connexion : " + safe(session.getLien()) + "\n\n" +
            "Rejoignez la session en cliquant sur le lien ci-dessus."
        );

        // Lieu = lien de la session
        event.setLocation(safe(session.getLien()));

        // Date/heure de début
        LocalDateTime debut = LocalDateTime.of(session.getDate(), session.getHeure());
        ZonedDateTime debutZoned = debut.atZone(ZoneId.systemDefault());
        String debutRfc3339 = debutZoned.format(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ssXXX"));

        // Date/heure de fin (1h après par défaut)
        ZonedDateTime finZoned = debutZoned.plusHours(1);
        String finRfc3339 = finZoned.format(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ssXXX"));

        EventDateTime start = new EventDateTime()
                .setDateTime(new DateTime(debutRfc3339))
                .setTimeZone(ZoneId.systemDefault().getId());
        EventDateTime end = new EventDateTime()
                .setDateTime(new DateTime(finRfc3339))
                .setTimeZone(ZoneId.systemDefault().getId());

        event.setStart(start);
        event.setEnd(end);

        // Rappels : 30 min et 10 min avant
        EventReminder reminder30 = new EventReminder()
                .setMethod("popup").setMinutes(30);
        EventReminder reminder10 = new EventReminder()
                .setMethod("popup").setMinutes(10);
        EventReminder emailReminder = new EventReminder()
                .setMethod("email").setMinutes(30);

        Event.Reminders reminders = new Event.Reminders()
                .setUseDefault(false)
                .setOverrides(Arrays.asList(reminder30, reminder10, emailReminder));
        event.setReminders(reminders);

        return event;
    }

    // ── Authentification OAuth2 ───────────────────────────────────────────────

    private Calendar buildService() throws Exception {
        NetHttpTransport transport = GoogleNetHttpTransport.newTrustedTransport();
        Credential credential = getCredentials(transport);
        return new Calendar.Builder(transport, JSON_FACTORY, credential)
                .setApplicationName(APPLICATION_NAME)
                .build();
    }

    private Credential getCredentials(NetHttpTransport transport) throws Exception {
        InputStream in = GoogleCalendarService.class.getResourceAsStream("/credentials.json");
        if (in == null) throw new IllegalStateException("credentials.json introuvable");

        GoogleClientSecrets secrets = GoogleClientSecrets.load(JSON_FACTORY, new InputStreamReader(in));

        GoogleAuthorizationCodeFlow flow = new GoogleAuthorizationCodeFlow.Builder(
                transport, JSON_FACTORY, secrets, SCOPES)
                .setDataStoreFactory(new FileDataStoreFactory(new File(TOKENS_DIR)))
                .setAccessType("offline")
                .build();

        LocalServerReceiver receiver = new LocalServerReceiver.Builder().setPort(8889).build();
        return new AuthorizationCodeInstalledApp(flow, receiver).authorize("user");
    }

    private static String safe(String s) {
        return s == null ? "" : s.trim();
    }
}
