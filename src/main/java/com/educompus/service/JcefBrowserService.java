package com.educompus.service;

import me.friwi.jcefmaven.CefAppBuilder;
import me.friwi.jcefmaven.CefInitializationException;
import me.friwi.jcefmaven.impl.progress.ConsoleProgressHandler;
import me.friwi.jcefmaven.UnsupportedPlatformException;
import org.cef.CefApp;
import org.cef.CefClient;
import org.cef.browser.CefBrowser;
import org.cef.browser.CefFrame;
import org.cef.handler.CefDisplayHandlerAdapter;
import org.cef.handler.CefLoadHandlerAdapter;

import javax.swing.JDialog;
import javax.swing.ImageIcon;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Image;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;

public final class JcefBrowserService {
    private static final Object LOCK = new Object();
    private static final JcefBrowserService INSTANCE = new JcefBrowserService();
    private static volatile CefApp app;
    private static volatile CefClient client;
    private static volatile boolean shutdownHookRegistered;
    private final Map<String, BrowserDialogHandle> openDialogs = new ConcurrentHashMap<>();

    private JcefBrowserService() {
    }

    public static JcefBrowserService getInstance() {
        return INSTANCE;
    }

    public BrowserDialogHandle openMeetingDialog(String title, String url) throws Exception {
        String safeUrl = safe(url);
        if (safeUrl.isBlank()) {
            throw new IllegalArgumentException("Meeting URL is empty.");
        }
        String key = safeUrl;
        BrowserDialogHandle existing = openDialogs.get(key);
        if (existing != null && existing.isShowing()) {
            existing.show();
            existing.load(safeUrl);
            return existing;
        }

        CefClient sharedClient = getOrCreateClient();
        CefBrowser browser = sharedClient.createBrowser("about:blank", false, false);
        CountDownLatch latch = new CountDownLatch(1);
        DialogRef dialogRef = new DialogRef();

        SwingUtilities.invokeLater(() -> {
            try {
                JDialog dialog = new JDialog((java.awt.Frame) null, safe(title), false);

                JPanel content = new JPanel(new BorderLayout());
                content.setPreferredSize(new Dimension(1280, 800));
                content.add(browser.getUIComponent(), BorderLayout.CENTER);

                dialog.setContentPane(content);
                dialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
                dialog.setMinimumSize(new Dimension(960, 640));
                dialog.pack();
                dialog.setSize(new Dimension(1400, 900));
                dialog.setLocationRelativeTo(null);
                Image appIcon = appIcon();
                if (appIcon != null) {
                    dialog.setIconImage(appIcon);
                }
                dialog.addWindowListener(new WindowAdapter() {
                    @Override
                    public void windowOpened(WindowEvent e) {
                        browser.createImmediately();
                        browser.setFocus(true);
                        browser.loadURL(safeUrl);
                        log("Meeting dialog opened");
                    }

                    @Override
                    public void windowClosing(WindowEvent e) {
                        log("Meeting dialog closing");
                    }

                    @Override
                    public void windowClosed(WindowEvent e) {
                        openDialogs.remove(key);
                        try {
                            browser.close(true);
                        } catch (Exception ex) {
                            log("Browser close failed: " + ex.getMessage());
                        }
                    }
                });
                dialogRef.dialog = dialog;
                dialog.setVisible(true);
            } finally {
                latch.countDown();
            }
        });

        latch.await();
        if (dialogRef.dialog == null) {
            throw new IllegalStateException("Meeting dialog could not be created.");
        }

        BrowserDialogHandle handle = new BrowserDialogHandle(sharedClient, browser, dialogRef.dialog);
        openDialogs.put(key, handle);
        return handle;
    }

    private void attachLogging(CefClient client) {
        client.addDisplayHandler(new CefDisplayHandlerAdapter() {
            @Override
            public void onAddressChange(CefBrowser browser, CefFrame frame, String newUrl) {
                if (frame != null && frame.isMain()) {
                    log("Address changed to " + newUrl);
                }
            }
        });
        client.addLoadHandler(new CefLoadHandlerAdapter() {
            @Override
            public void onLoadingStateChange(CefBrowser browser, boolean isLoading, boolean canGoBack, boolean canGoForward) {
                log("Loading state changed: isLoading=" + isLoading + ", canGoBack=" + canGoBack + ", canGoForward=" + canGoForward);
            }

            @Override
            public void onLoadEnd(CefBrowser browser, CefFrame frame, int httpStatusCode) {
                if (frame != null && frame.isMain()) {
                    log("Load finished for " + frame.getURL() + " with HTTP status " + httpStatusCode);
                }
            }

            @Override
            public void onLoadError(CefBrowser browser, CefFrame frame, ErrorCode errorCode, String errorText, String failedUrl) {
                if (frame != null && frame.isMain()) {
                    log("Load error for " + failedUrl + ": " + errorCode + " - " + errorText);
                }
            }
        });
    }

    private CefClient getOrCreateClient() throws IOException, UnsupportedPlatformException, InterruptedException, CefInitializationException {
        synchronized (LOCK) {
            if (client != null) {
                return client;
            }
            Exception lastFailure = null;
            for (int attempt = 1; attempt <= 2; attempt++) {
                try {
                    app = buildApp();
                    if (isInitializationFailed(app)) {
                        throw new IllegalStateException("JCEF is in state " + app.getState());
                    }
                    client = app.createClient();
                    attachLogging(client);
                    registerShutdownHook();
                    return client;
                } catch (IllegalStateException | IOException | UnsupportedPlatformException | InterruptedException | CefInitializationException e) {
                    lastFailure = e;
                    log("JCEF init attempt " + attempt + " failed: " + safe(e.getMessage()));
                    resetAppState();
                    cleanupRuntimeArtifacts();
                }
            }

            if (lastFailure instanceof IOException io) {
                throw io;
            }
            if (lastFailure instanceof UnsupportedPlatformException up) {
                throw up;
            }
            if (lastFailure instanceof InterruptedException ie) {
                Thread.currentThread().interrupt();
                throw ie;
            }
            if (lastFailure instanceof CefInitializationException cie) {
                throw cie;
            }
            if (lastFailure instanceof IllegalStateException ise) {
                throw ise;
            }
            throw new IllegalStateException("Unable to initialize JCEF.");
        }
    }

    private CefApp buildApp() throws IOException, UnsupportedPlatformException, InterruptedException, CefInitializationException {
        CefAppBuilder builder = new CefAppBuilder();
        File installDir = jcefInstallDir();
        File cacheDir = new File(installDir, "cache");
        Files.createDirectories(installDir.toPath());
        Files.createDirectories(cacheDir.toPath());

        builder.setInstallDir(installDir);
        builder.setProgressHandler(new ConsoleProgressHandler());
        builder.getCefSettings().windowless_rendering_enabled = false;
        builder.getCefSettings().persist_session_cookies = false;
        builder.getCefSettings().cache_path = cacheDir.getAbsolutePath();
        builder.addJcefArgs("--no-first-run");
        builder.addJcefArgs("--disable-session-crashed-bubble");
        builder.addJcefArgs("--disable-default-apps");
        builder.addJcefArgs("--autoplay-policy=no-user-gesture-required");
        builder.addJcefArgs("--disable-features=WinUseBrowserSpellChecker");
        builder.addJcefArgs("--enable-media-stream");
        builder.addJcefArgs("--use-fake-ui-for-media-stream");
        builder.addJcefArgs("--enable-usermedia-screen-capturing");
        builder.addJcefArgs("--allow-http-screen-capture");
        builder.addJcefArgs("--disable-usb-keyboard-detect");
        log("Initializing JCEF in " + installDir.getAbsolutePath());
        return builder.build();
    }

    private static File jcefInstallDir() {
        return new File(System.getProperty("user.dir"), "var/jcef-bundle").getAbsoluteFile();
    }

    private void cleanupRuntimeArtifacts() {
        tryDelete(new File(jcefInstallDir(), "install.lock"));
        clearDirectory(new File(jcefInstallDir(), "cache").toPath().toFile());
    }

    private static void tryDelete(File file) {
        try {
            if (file != null && file.exists()) {
                Files.deleteIfExists(file.toPath());
            }
        } catch (Exception ex) {
            log("Unable to delete " + file + ": " + ex.getMessage());
        }
    }

    private static void clearDirectory(File dir) {
        if (dir == null || !dir.exists() || !dir.isDirectory()) {
            return;
        }
        File[] files = dir.listFiles();
        if (files == null) {
            return;
        }
        for (File file : files) {
            if (file.isDirectory()) {
                clearDirectory(file);
            }
            tryDelete(file);
        }
    }

    private void resetAppState() {
        try {
            if (client != null) {
                client.dispose();
            }
        } catch (Exception ex) {
            log("Client dispose failed during reset: " + ex.getMessage());
        } finally {
            client = null;
        }

        try {
            if (app != null) {
                app.dispose();
            }
        } catch (Exception ex) {
            log("App dispose failed during reset: " + ex.getMessage());
        } finally {
            app = null;
        }
    }

    private void registerShutdownHook() {
        if (shutdownHookRegistered) {
            return;
        }
        shutdownHookRegistered = true;
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            try {
                if (client != null) {
                    client.dispose();
                }
            } catch (Exception ex) {
                log("JCEF client dispose failed: " + ex.getMessage());
            }
            try {
                if (app != null) {
                    log("Disposing JCEF application");
                    app.dispose();
                }
            } catch (Exception ex) {
                log("JCEF dispose failed: " + ex.getMessage());
            }
        }, "jcef-shutdown"));
    }

    private static boolean isInitializationFailed(CefApp cefApp) {
        return cefApp == null || String.valueOf(cefApp.getState()).contains("INITIALIZATION_FAILED");
    }

    private static String safe(String value) {
        return value == null ? "" : value.trim();
    }

    private static void log(String message) {
        System.out.println("[JCEF] " + message);
    }

    private static Image appIcon() {
        try {
            URL url = JcefBrowserService.class.getResource("/assets/images/app-icon.png");
            return url == null ? null : new ImageIcon(url).getImage();
        } catch (Exception ignored) {
            return null;
        }
    }

    private static final class DialogRef {
        private JDialog dialog;
    }

    public static final class BrowserDialogHandle {
        private final CefBrowser browser;
        private final JDialog dialog;

        private BrowserDialogHandle(CefClient client, CefBrowser browser, JDialog dialog) {
            this.browser = browser;
            this.dialog = dialog;
        }

        public void load(String url) {
            String targetUrl = safe(url);
            log("Requesting browser.loadURL(" + targetUrl + ")");
            SwingUtilities.invokeLater(() -> {
                browser.setFocus(true);
                browser.loadURL(targetUrl);
                if (dialog != null) {
                    dialog.toFront();
                    dialog.repaint();
                }
            });
        }

        public boolean isShowing() {
            return dialog != null && dialog.isDisplayable();
        }

        public void show() {
            if (dialog == null) {
                return;
            }
            SwingUtilities.invokeLater(() -> {
                dialog.setVisible(true);
                dialog.toFront();
            });
        }

        public void close() {
            if (dialog == null) {
                return;
            }
            SwingUtilities.invokeLater(dialog::dispose);
        }
    }
}
