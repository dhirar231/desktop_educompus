package com.educompus.service;

import com.educompus.model.Commande;
import com.educompus.model.LigneCommande;
import com.educompus.model.Produit;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

/**
 * Génère un certificat de garantie PNG (Java2D, sans dépendance externe).
 * Réservé aux produits de type "Materiel" — garantie 1 an.
 */
public class GarantiePNGService {

    private static final int W = 900;
    private static final int H = 560;
    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    // Palette
    private static final Color C_DARK    = new Color(10,  15,  40);
    private static final Color C_BLUE    = new Color(37,  99, 235);
    private static final Color C_VIOLET  = new Color(109, 40, 217);
    private static final Color C_GREEN   = new Color(16, 185, 129);
    private static final Color C_WHITE   = Color.WHITE;
    private static final Color C_MUTED   = new Color(148, 163, 184);
    private static final Color C_SURFACE = new Color(241, 245, 249);
    private static final Color C_BORDER  = new Color(203, 213, 225);

    public File generer(Commande cmd, LigneCommande lc, Produit produit) throws Exception {
        BufferedImage img = new BufferedImage(W, H, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = img.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,    RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        dessiner(g, cmd, lc, produit);
        g.dispose();

        String nom = "Garantie_" + produit.getId() + "_cmd" + cmd.getId() + ".png";
        File dest = new File(System.getProperty("user.home") + "/Downloads/" + nom);
        ImageIO.write(img, "PNG", dest);
        return dest;
    }

    private void dessiner(Graphics2D g, Commande cmd, LigneCommande lc, Produit produit) {
        // ── Fond dégradé ──────────────────────────────────────────────────────
        GradientPaint bg = new GradientPaint(0, 0, C_DARK, W, H, new Color(30, 27, 75));
        g.setPaint(bg);
        g.fillRect(0, 0, W, H);

        // ── Cercles décoratifs (arrière-plan) ─────────────────────────────────
        g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.08f));
        g.setColor(C_BLUE);
        g.fillOval(-80, -80, 320, 320);
        g.setColor(C_VIOLET);
        g.fillOval(W - 200, H - 200, 350, 350);
        g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 1f));

        // ── Carte centrale blanche ────────────────────────────────────────────
        int cx = 40, cy = 40, cw = W - 80, ch = H - 80;
        g.setColor(C_WHITE);
        g.fill(new RoundRectangle2D.Float(cx, cy, cw, ch, 24, 24));

        // Bordure subtile
        g.setColor(C_BORDER);
        g.setStroke(new BasicStroke(1.5f));
        g.draw(new RoundRectangle2D.Float(cx, cy, cw, ch, 24, 24));

        // ── Bandeau header dans la carte ──────────────────────────────────────
        GradientPaint header = new GradientPaint(cx, cy, C_BLUE, cx + cw, cy, C_VIOLET);
        g.setPaint(header);
        g.fill(new RoundRectangle2D.Float(cx, cy, cw, 72, 24, 24));
        // Remplir le bas arrondi du header
        g.fillRect(cx, cy + 48, cw, 24);

        // Texte header
        g.setColor(C_WHITE);
        g.setFont(new Font("SansSerif", Font.BOLD, 22));
        drawCentered(g, "CERTIFICAT DE GARANTIE", W / 2, cy + 34);

        g.setFont(new Font("SansSerif", Font.PLAIN, 11));
        g.setColor(new Color(196, 181, 253));
        drawCentered(g, "EduCampus Marketplace  —  Produits Matériels", W / 2, cy + 56);

        // ── Badge "VALIDE" ────────────────────────────────────────────────────
        int bx = W / 2 - 52, by = cy + 82;
        g.setColor(new Color(209, 250, 229));
        g.fill(new RoundRectangle2D.Float(bx, by, 104, 28, 14, 14));
        g.setColor(C_GREEN);
        g.setStroke(new BasicStroke(1.5f));
        g.draw(new RoundRectangle2D.Float(bx, by, 104, 28, 14, 14));
        g.setFont(new Font("SansSerif", Font.BOLD, 11));
        drawCentered(g, "✓  GARANTIE VALIDE", W / 2, by + 18);

        // ── Infos produit ─────────────────────────────────────────────────────
        int iy = cy + 136;
        g.setColor(C_DARK);
        g.setFont(new Font("SansSerif", Font.BOLD, 17));
        drawCentered(g, safe(produit.getNom()), W / 2, iy);

        g.setColor(C_MUTED);
        g.setFont(new Font("SansSerif", Font.PLAIN, 11));
        drawCentered(g, produit.getCategorie() + "  ·  " + produit.getType(), W / 2, iy + 20);

        // Séparateur
        g.setColor(C_BORDER);
        g.setStroke(new BasicStroke(1f));
        g.drawLine(cx + 60, iy + 32, cx + cw - 60, iy + 32);

        // ── Deux colonnes d'infos ─────────────────────────────────────────────
        int col1x = cx + 60, col2x = W / 2 + 20;
        int rowY = iy + 56;

        // Colonne gauche
        infoRow(g, col1x, rowY,      "N° Commande",   "#" + cmd.getId());
        infoRow(g, col1x, rowY + 36, "Produit ID",    "#" + produit.getId());
        infoRow(g, col1x, rowY + 72, "Prix d'achat",  String.format("%.2f TND", lc.getPrixUnitaire()));
        infoRow(g, col1x, rowY + 108,"Quantite",      "x" + lc.getQuantite());

        // Colonne droite
        LocalDate dateAchat = cmd.getDateCommande() != null
                ? cmd.getDateCommande().toLocalDate() : LocalDate.now();
        LocalDate dateExpir = dateAchat.plusYears(1);

        infoRow(g, col2x, rowY,      "Date d'achat",  dateAchat.format(FMT));
        infoRow(g, col2x, rowY + 36, "Debut garantie",dateAchat.format(FMT));
        infoRow(g, col2x, rowY + 72, "Fin garantie",  dateExpir.format(FMT));
        infoRow(g, col2x, rowY + 108,"Duree",         "12 mois");

        // ── Bandeau durée garantie ────────────────────────────────────────────
        int banY = rowY + 148;
        GradientPaint banBg = new GradientPaint(cx + 60, banY, new Color(239, 246, 255),
                cx + cw - 60, banY, new Color(237, 233, 254));
        g.setPaint(banBg);
        g.fill(new RoundRectangle2D.Float(cx + 60, banY, cw - 120, 38, 10, 10));
        g.setColor(C_BLUE);
        g.setStroke(new BasicStroke(1f));
        g.draw(new RoundRectangle2D.Float(cx + 60, banY, cw - 120, 38, 10, 10));

        g.setColor(C_BLUE);
        g.setFont(new Font("SansSerif", Font.BOLD, 13));
        drawCentered(g, "🛡  Garantie constructeur 1 an — Valable jusqu'au " + dateExpir.format(FMT),
                W / 2, banY + 24);

        // ── Pied de page ──────────────────────────────────────────────────────
        int footY = cy + ch - 18;
        g.setColor(C_MUTED);
        g.setFont(new Font("SansSerif", Font.PLAIN, 9));
        drawCentered(g, "EduCampus  |  Ce certificat est généré automatiquement  |  Non contractuel",
                W / 2, footY);
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private void infoRow(Graphics2D g, int x, int y, String label, String value) {
        g.setColor(C_MUTED);
        g.setFont(new Font("SansSerif", Font.PLAIN, 10));
        g.drawString(label, x, y);
        g.setColor(C_DARK);
        g.setFont(new Font("SansSerif", Font.BOLD, 12));
        g.drawString(safe(value), x, y + 16);
    }

    private void drawCentered(Graphics2D g, String text, int cx, int y) {
        FontMetrics fm = g.getFontMetrics();
        int tw = fm.stringWidth(text);
        g.drawString(text, cx - tw / 2, y);
    }

    private String safe(String s) {
        if (s == null) return "—";
        return s.replaceAll("[^\\x00-\\xFF]", "?");
    }
}
