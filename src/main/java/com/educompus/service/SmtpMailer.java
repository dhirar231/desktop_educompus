package com.educompus.service;

import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.Socket;
import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;

public final class SmtpMailer {
    public record MailConfig(
            String host,
            int port,
            String username,
            String password,
            String fromAddress,
            boolean startTls,
            boolean ssl
    ) {
        public boolean isConfigured() {
            return host != null && !host.isBlank() && fromAddress != null && !fromAddress.isBlank();
        }

        public String summary() {
            if (!isConfigured()) {
                return "SMTP non configure. Utilisez EDUCOMPUS_SMTP_HOST, EDUCOMPUS_SMTP_PORT, EDUCOMPUS_SMTP_USERNAME, EDUCOMPUS_SMTP_PASSWORD, EDUCOMPUS_MAIL_FROM.";
            }
            return host + ":" + port + " | from=" + fromAddress + (startTls ? " | STARTTLS" : "") + (ssl ? " | SSL" : "");
        }
    }

    public MailConfig loadConfig() {
        Map<String, String> dotenv = loadDotenvConfig();
        String host = firstNonBlank(
                System.getProperty("educompus.smtp.host"),
                System.getenv("EDUCOMPUS_SMTP_HOST"),
                dotenv.get("EDUCOMPUS_SMTP_HOST")
        );
        boolean ssl = parseBoolean(
                System.getProperty("educompus.smtp.ssl"),
                System.getenv("EDUCOMPUS_SMTP_SSL"),
                dotenv.get("EDUCOMPUS_SMTP_SSL")
        );
        boolean startTls = parseBoolean(
                System.getProperty("educompus.smtp.starttls"),
                System.getenv("EDUCOMPUS_SMTP_STARTTLS"),
                dotenv.get("EDUCOMPUS_SMTP_STARTTLS")
        );
        int defaultPort = ssl ? 465 : (startTls ? 587 : 25);
        int port = parseInt(firstNonBlank(
                System.getProperty("educompus.smtp.port"),
                System.getenv("EDUCOMPUS_SMTP_PORT"),
                dotenv.get("EDUCOMPUS_SMTP_PORT")
        ), defaultPort);
        String username = firstNonBlank(
                System.getProperty("educompus.smtp.username"),
                System.getenv("EDUCOMPUS_SMTP_USERNAME"),
                dotenv.get("EDUCOMPUS_SMTP_USERNAME")
        );
        String password = firstNonBlank(
                System.getProperty("educompus.smtp.password"),
                System.getenv("EDUCOMPUS_SMTP_PASSWORD"),
                dotenv.get("EDUCOMPUS_SMTP_PASSWORD")
        );
        String fromAddress = firstNonBlank(
                System.getProperty("educompus.mail.from"),
                System.getenv("EDUCOMPUS_MAIL_FROM"),
                dotenv.get("EDUCOMPUS_MAIL_FROM"),
                System.getProperty("mail.from"),
                System.getenv("MAIL_FROM_ADDRESS"),
                dotenv.get("MAIL_FROM_ADDRESS")
        );

        MailConfig directConfig = new MailConfig(host, port, username, password, fromAddress, startTls, ssl);
        if (directConfig.isConfigured()) {
            return directConfig;
        }

        MailConfig dsnConfig = parseMailerDsn(firstNonBlank(
                System.getProperty("mailer.dsn"),
                System.getenv("MAILER_DSN"),
                dotenv.get("MAILER_DSN")
        ), fromAddress);
        if (dsnConfig != null && dsnConfig.isConfigured()) {
            return dsnConfig;
        }

        return directConfig;
    }

    private Map<String, String> loadDotenvConfig() {
        Map<String, String> values = new LinkedHashMap<>();
        File desktopDir = new File(System.getProperty("user.dir")).getAbsoluteFile();
        loadEnvFile(new File(desktopDir, ".env"), values);
        loadEnvFile(new File(desktopDir, ".env.local"), values);
        String appEnv = firstNonBlank(System.getenv("APP_ENV"), values.get("APP_ENV"), "dev");
        loadEnvFile(new File(desktopDir, ".env." + appEnv), values);
        loadEnvFile(new File(desktopDir, ".env." + appEnv + ".local"), values);
        return values;
    }

    private void loadEnvFile(File file, Map<String, String> values) {
        if (file == null || !file.exists() || !file.isFile()) {
            return;
        }
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(new BufferedInputStream(java.nio.file.Files.newInputStream(file.toPath())), StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                String trimmed = safe(line);
                if (trimmed.isBlank() || trimmed.startsWith("#")) {
                    continue;
                }
                int idx = trimmed.indexOf('=');
                if (idx <= 0) {
                    continue;
                }
                String key = safe(trimmed.substring(0, idx));
                String value = trimQuotes(trimmed.substring(idx + 1).trim());
                if (!key.isBlank()) {
                    values.put(key, value);
                }
            }
        } catch (Exception ignored) {
            // ignore malformed or missing local env files
        }
    }

    private MailConfig parseMailerDsn(String dsn, String fallbackFrom) {
        String raw = safe(dsn);
        if (raw.isBlank()) {
            return null;
        }
        try {
            URI uri = URI.create(raw);
            String scheme = safe(uri.getScheme()).toLowerCase();
            boolean ssl = "smtps".equals(scheme);
            boolean startTls = !ssl;
            int port = uri.getPort() > 0 ? uri.getPort() : (ssl ? 465 : 587);
            String userInfo = safe(uri.getUserInfo());
            String username = "";
            String password = "";
            int sep = userInfo.indexOf(':');
            if (sep >= 0) {
                username = decodeUrlPart(userInfo.substring(0, sep));
                password = decodeUrlPart(userInfo.substring(sep + 1));
            } else if (!userInfo.isBlank()) {
                username = decodeUrlPart(userInfo);
            }
            String fromAddress = firstNonBlank(fallbackFrom, username);
            return new MailConfig(uri.getHost(), port, username, password, fromAddress, startTls, ssl);
        } catch (Exception ignored) {
            return null;
        }
    }

    private static String decodeUrlPart(String value) {
        try {
            return URLDecoder.decode(safe(value), StandardCharsets.UTF_8);
        } catch (Exception ignored) {
            return safe(value);
        }
    }

    private static String trimQuotes(String value) {
        String v = safe(value);
        if (v.length() >= 2) {
            if ((v.startsWith("\"") && v.endsWith("\"")) || (v.startsWith("'") && v.endsWith("'"))) {
                return v.substring(1, v.length() - 1);
            }
        }
        return v;
    }

    public void send(MailConfig config, String to, String subject, String body) throws IOException {
        if (config == null || !config.isConfigured()) {
            throw new IllegalStateException("SMTP non configure.");
        }
        String recipient = safe(to);
        if (recipient.isBlank()) {
            throw new IllegalArgumentException("Destinataire vide.");
        }

        try (SmtpConnection conn = open(config)) {
            conn.expect(220);
            ehlo(conn);

            if (config.startTls() && !config.ssl()) {
                conn.command("STARTTLS");
                conn.expect(220);
                conn.upgradeToTls();
                ehlo(conn);
            }

            if (!safe(config.username()).isBlank()) {
                conn.command("AUTH LOGIN");
                conn.expect(334);
                conn.command(base64(config.username()));
                conn.expect(334);
                conn.command(base64(config.password()));
                conn.expect(235);
            }

            conn.command("MAIL FROM:<" + config.fromAddress() + ">");
            conn.expect(250);
            conn.command("RCPT TO:<" + recipient + ">");
            conn.expectAny(250, 251);
            conn.command("DATA");
            conn.expect(354);
            conn.writeRaw(buildMessage(config.fromAddress(), recipient, subject, body));
            conn.writeRaw("\r\n.\r\n");
            conn.expect(250);
            conn.command("QUIT");
        }
    }

    private static void ehlo(SmtpConnection conn) throws IOException {
        conn.command("EHLO educompus-desktop");
        conn.expect(250);
    }

    private static String buildMessage(String from, String to, String subject, String body) {
        String normalizedBody = safe(body).replace("\r\n", "\n").replace('\r', '\n').replace("\n", "\r\n");
        if (!normalizedBody.endsWith("\r\n")) {
            normalizedBody += "\r\n";
        }
        return ""
                + "From: <" + from + ">\r\n"
                + "To: <" + to + ">\r\n"
                + "Subject: " + encodeHeader(subject) + "\r\n"
                + "MIME-Version: 1.0\r\n"
                + "Content-Type: text/plain; charset=UTF-8\r\n"
                + "Content-Transfer-Encoding: 8bit\r\n"
                + "\r\n"
                + normalizedBody;
    }

    private SmtpConnection open(MailConfig config) throws IOException {
        Socket socket;
        if (config.ssl()) {
            socket = SSLSocketFactory.getDefault().createSocket(config.host(), config.port());
            ((SSLSocket) socket).startHandshake();
        } else {
            socket = new Socket(config.host(), config.port());
        }
        return new SmtpConnection(socket);
    }

    private static String encodeHeader(String value) {
        String raw = safe(value);
        if (raw.isBlank()) {
            return "";
        }
        return "=?UTF-8?B?" + Base64.getEncoder().encodeToString(raw.getBytes(StandardCharsets.UTF_8)) + "?=";
    }

    private static String base64(String value) {
        return Base64.getEncoder().encodeToString(safe(value).getBytes(StandardCharsets.UTF_8));
    }

    private static boolean parseBoolean(String... values) {
        String value = firstNonBlank(values);
        return "1".equals(value) || "true".equalsIgnoreCase(value) || "yes".equalsIgnoreCase(value);
    }

    private static int parseInt(String value, int fallback) {
        try {
            return value == null || value.isBlank() ? fallback : Integer.parseInt(value.trim());
        } catch (Exception ignored) {
            return fallback;
        }
    }

    private static String firstNonBlank(String... values) {
        if (values == null) {
            return "";
        }
        for (String value : values) {
            if (value != null && !value.trim().isBlank()) {
                return value.trim();
            }
        }
        return "";
    }

    private static String safe(String value) {
        return value == null ? "" : value.trim();
    }

    private static final class SmtpConnection implements AutoCloseable {
        private Socket socket;
        private BufferedReader reader;
        private OutputStream out;

        private SmtpConnection(Socket socket) throws IOException {
            this.socket = socket;
            resetStreams();
        }

        private void resetStreams() throws IOException {
            reader = new BufferedReader(new InputStreamReader(new BufferedInputStream(socket.getInputStream()), StandardCharsets.UTF_8));
            out = new BufferedOutputStream(socket.getOutputStream());
        }

        private void command(String value) throws IOException {
            writeRaw(value + "\r\n");
        }

        private void writeRaw(String value) throws IOException {
            out.write(value.getBytes(StandardCharsets.UTF_8));
            out.flush();
        }

        private void expect(int code) throws IOException {
            int actual = readCode();
            if (actual != code) {
                throw new IOException("Reponse SMTP inattendue: " + actual + " (attendu " + code + ")");
            }
        }

        private void expectAny(int... codes) throws IOException {
            int actual = readCode();
            for (int code : codes) {
                if (actual == code) {
                    return;
                }
            }
            throw new IOException("Reponse SMTP inattendue: " + actual);
        }

        private int readCode() throws IOException {
            String line = reader.readLine();
            if (line == null || line.length() < 3) {
                throw new IOException("Connexion SMTP interrompue.");
            }
            String last = line;
            while (line.length() >= 4 && line.charAt(3) == '-') {
                line = reader.readLine();
                if (line == null) {
                    throw new IOException("Connexion SMTP interrompue.");
                }
                last = line;
            }
            return Integer.parseInt(last.substring(0, 3));
        }

        private void upgradeToTls() throws IOException {
            SSLSocketFactory factory = (SSLSocketFactory) SSLSocketFactory.getDefault();
            SSLSocket sslSocket = (SSLSocket) factory
                    .createSocket(socket, socket.getInetAddress().getHostAddress(), socket.getPort(), true);
            sslSocket.startHandshake();
            socket = sslSocket;
            resetStreams();
        }

        @Override
        public void close() throws IOException {
            if (socket != null) {
                socket.close();
            }
        }
    }
}
