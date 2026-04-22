package com.educompus.service;

import com.educompus.model.Commande;
import com.educompus.model.LigneCommande;
import com.educompus.model.Livraison;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;

import java.awt.Color;
import java.io.File;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class FacturePNGService {

    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    private static final DateTimeFormatter FMT_DATE = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    // Couleurs thème EduCampus
    private static final Color COULEUR_PRIMAIRE  = new Color(6, 106, 201);   // -edu-primary
    private static final Color COULEUR_TEXTE     = new Color(36, 41, 45);    // -edu-text
    private static final Color COULEUR_MUTED     = new Color(116, 117, 121); // -edu-text-muted
    private static final Color COULEUR_BORDURE   = new Color(221, 224, 227); // -edu-border
    private static final Color COULEUR_SURFACE   = new Color(245, 247, 249); // -edu-surface

    /**
     * Génère la facture PDF et la sauvegarde dans le dossier téléchargements.
     * @return le fichier PDF généré
     */
    public File generer(Commande cmd, List<LigneCommande> lignes, Livraison liv) throws Exception {
        PDDocument doc = new PDDocument();
        PDPage page = new PDPage(PDRectangle.A4);
        doc.addPage(page);

        float largeur = page.getMediaBox().getWidth();
        float hauteur = page.getMediaBox().getHeight();
        float margeG  = 50f;
        float margeD  = largeur - 50f;
        float y       = hauteur - 50f;

        PDPageContentStream cs = new PDPageContentStream(doc, page);
        dessinerContenu(cs, largeur, hauteur, margeG, margeD, y, cmd, lignes, liv);
        cs.close();

        String nomFichier = "Facture_EduCampus_" + cmd.getId() + ".pdf";
        File dest = new File(System.getProperty("user.home") + "/Downloads/" + nomFichier);
        doc.save(dest);
        doc.close();
        return dest;
    }

    /**
     * Génère la facture et la sauvegarde en PNG (300 DPI).
     * @return le fichier PNG généré
     */
    public File genererPng(Commande cmd, List<LigneCommande> lignes, Livraison liv) throws Exception {
        // 1. Générer le PDF en mémoire
        PDDocument doc = new PDDocument();
        PDPage page = new PDPage(PDRectangle.A4);
        doc.addPage(page);

        float largeur = page.getMediaBox().getWidth();
        float hauteur = page.getMediaBox().getHeight();
        float margeG  = 50f;
        float margeD  = largeur - 50f;
        float y       = hauteur - 50f;

        PDPageContentStream cs = new PDPageContentStream(doc, page);
        // Fond blanc explicite
        cs.setNonStrokingColor(Color.WHITE);
        cs.addRect(0, 0, largeur, hauteur);
        cs.fill();

        y = dessinerContenu(cs, largeur, hauteur, margeG, margeD, y, cmd, lignes, liv);
        cs.close();

        // 2. Rendre la page en BufferedImage (300 DPI)
        org.apache.pdfbox.rendering.PDFRenderer renderer = new org.apache.pdfbox.rendering.PDFRenderer(doc);
        java.awt.image.BufferedImage image = renderer.renderImageWithDPI(0, 300,
                org.apache.pdfbox.rendering.ImageType.RGB);
        doc.close();

        // 3. Sauvegarder en PNG
        String nomFichier = "Facture_EduCampus_" + cmd.getId() + ".png";
        File dest = new File(System.getProperty("user.home") + "/Downloads/" + nomFichier);
        javax.imageio.ImageIO.write(image, "PNG", dest);
        return dest;
    }

    // ── Méthode de dessin partagée ────────────────────────────────────────────

    private float dessinerContenu(PDPageContentStream cs,
            float largeur, float hauteur, float margeG, float margeD, float y,
            Commande cmd, List<LigneCommande> lignes, Livraison liv) throws Exception {

        // Bandeau header
        cs.setNonStrokingColor(COULEUR_PRIMAIRE);
        cs.addRect(0, hauteur - 80, largeur, 80);
        cs.fill();

        cs.setNonStrokingColor(Color.WHITE);
        cs.beginText(); cs.setFont(PDType1Font.HELVETICA_BOLD, 22);
        cs.newLineAtOffset(margeG, hauteur - 45); cs.showText("EduCampus"); cs.endText();

        cs.beginText(); cs.setFont(PDType1Font.HELVETICA, 10);
        cs.newLineAtOffset(margeG, hauteur - 62);
        cs.showText("Marketplace des produits educatifs"); cs.endText();

        cs.beginText(); cs.setFont(PDType1Font.HELVETICA_BOLD, 18);
        cs.newLineAtOffset(margeD - 120, hauteur - 45); cs.showText("FACTURE"); cs.endText();

        cs.beginText(); cs.setFont(PDType1Font.HELVETICA, 10);
        cs.newLineAtOffset(margeD - 120, hauteur - 62); cs.showText("#" + cmd.getId()); cs.endText();

        y = hauteur - 100;
        y = dessinerBloc2Colonnes(cs, margeG, margeD, y,
                "Informations de commande",
                new String[]{
                    "Numero de commande : #" + cmd.getId(),
                    "Date               : " + (cmd.getDateCommande() != null ? cmd.getDateCommande().format(FMT) : "—"),
                    "Statut paiement    : Confirme",
                    "Methode paiement   : Stripe"
                },
                "Informations de livraison",
                new String[]{
                    "Adresse  : " + (liv != null ? liv.getAdresse() : "—"),
                    "Ville    : " + (liv != null ? liv.getVille() : "—"),
                    "Tel      : " + (liv != null && liv.getPhoneNumber() != null ? liv.getPhoneNumber() : "—"),
                    "Statut   : " + (liv != null ? liv.getStatusLivraison() : "—"),
                    "Livraison: " + (liv != null && liv.getDateLivraison() != null
                            ? liv.getDateLivraison().format(FMT_DATE) : "Non precisee")
                });

        y -= 20;
        y = dessinerTableau(cs, margeG, margeD, y, lignes);
        y -= 20;
        y = dessinerTotal(cs, margeG, margeD, y, cmd.getTotal());
        dessinerPied(cs, largeur, cmd);
        return y;
    }

    // ── Helpers de dessin ─────────────────────────────────────────────────────

    private float dessinerBloc2Colonnes(PDPageContentStream cs,
            float margeG, float margeD, float y,
            String titre1, String[] lignes1,
            String titre2, String[] lignes2) throws Exception {

        float colLargeur = (margeD - margeG - 20) / 2;
        float col2X = margeG + colLargeur + 20;
        float yDepart = y;

        // Fond gris léger
        cs.setNonStrokingColor(COULEUR_SURFACE);
        float hauteurBloc = Math.max(lignes1.length, lignes2.length) * 16 + 40;
        cs.addRect(margeG, y - hauteurBloc, margeD - margeG, hauteurBloc);
        cs.fill();

        // Titre col 1
        cs.setNonStrokingColor(COULEUR_PRIMAIRE);
        cs.beginText();
        cs.setFont(PDType1Font.HELVETICA_BOLD, 10);
        cs.newLineAtOffset(margeG + 8, y - 18);
        cs.showText(titre1);
        cs.endText();

        // Titre col 2
        cs.beginText();
        cs.setFont(PDType1Font.HELVETICA_BOLD, 10);
        cs.newLineAtOffset(col2X + 8, y - 18);
        cs.showText(titre2);
        cs.endText();

        // Lignes col 1
        cs.setNonStrokingColor(COULEUR_TEXTE);
        float yLigne = y - 34;
        for (String l : lignes1) {
            cs.beginText();
            cs.setFont(PDType1Font.HELVETICA, 9);
            cs.newLineAtOffset(margeG + 8, yLigne);
            cs.showText(safe(l));
            cs.endText();
            yLigne -= 15;
        }

        // Lignes col 2
        yLigne = y - 34;
        for (String l : lignes2) {
            cs.beginText();
            cs.setFont(PDType1Font.HELVETICA, 9);
            cs.newLineAtOffset(col2X + 8, yLigne);
            cs.showText(safe(l));
            cs.endText();
            yLigne -= 15;
        }

        return y - hauteurBloc - 10;
    }

    private float dessinerTableau(PDPageContentStream cs,
            float margeG, float margeD, float y,
            List<LigneCommande> lignes) throws Exception {

        float largeurTotale = margeD - margeG;
        float[] colW = { largeurTotale * 0.45f, largeurTotale * 0.20f,
                         largeurTotale * 0.17f, largeurTotale * 0.18f };
        String[] entetes = { "Produit", "Prix unitaire", "Quantite", "Sous-total" };

        // En-tête tableau
        cs.setNonStrokingColor(COULEUR_PRIMAIRE);
        cs.addRect(margeG, y - 22, largeurTotale, 22);
        cs.fill();

        cs.setNonStrokingColor(Color.WHITE);
        float xCol = margeG + 6;
        for (int i = 0; i < entetes.length; i++) {
            cs.beginText();
            cs.setFont(PDType1Font.HELVETICA_BOLD, 9);
            cs.newLineAtOffset(xCol, y - 15);
            cs.showText(entetes[i]);
            cs.endText();
            xCol += colW[i];
        }
        y -= 22;

        // Lignes du tableau
        boolean pair = false;
        for (LigneCommande lc : lignes) {
            if (pair) {
                cs.setNonStrokingColor(COULEUR_SURFACE);
                cs.addRect(margeG, y - 20, largeurTotale, 20);
                cs.fill();
            }
            pair = !pair;

            cs.setNonStrokingColor(COULEUR_TEXTE);
            xCol = margeG + 6;
            String[] vals = {
                tronquer(lc.getNomProduit(), 35),
                String.format("%.2f TND", lc.getPrixUnitaire()),
                String.valueOf(lc.getQuantite()),
                String.format("%.2f TND", lc.getPrixUnitaire() * lc.getQuantite())
            };
            for (int i = 0; i < vals.length; i++) {
                cs.beginText();
                cs.setFont(PDType1Font.HELVETICA, 9);
                cs.newLineAtOffset(xCol, y - 14);
                cs.showText(safe(vals[i]));
                cs.endText();
                xCol += colW[i];
            }

            // Ligne séparatrice
            cs.setStrokingColor(COULEUR_BORDURE);
            cs.setLineWidth(0.3f);
            cs.moveTo(margeG, y - 20);
            cs.lineTo(margeD, y - 20);
            cs.stroke();

            y -= 20;
        }
        return y;
    }

    private float dessinerTotal(PDPageContentStream cs,
            float margeG, float margeD, float y, double total) throws Exception {

        float largeur = 200f;
        float xDebut  = margeD - largeur;

        // Fond bleu
        cs.setNonStrokingColor(COULEUR_PRIMAIRE);
        cs.addRect(xDebut, y - 30, largeur, 30);
        cs.fill();

        cs.setNonStrokingColor(Color.WHITE);
        cs.beginText();
        cs.setFont(PDType1Font.HELVETICA_BOLD, 12);
        cs.newLineAtOffset(xDebut + 10, y - 20);
        cs.showText("TOTAL : " + String.format("%.2f TND", total));
        cs.endText();

        return y - 40;
    }

    private void dessinerPied(PDPageContentStream cs, float largeur, Commande cmd) throws Exception {
        float y = 40f;
        cs.setNonStrokingColor(COULEUR_PRIMAIRE);
        cs.setLineWidth(1f);
        cs.moveTo(50, y + 15);
        cs.lineTo(largeur - 50, y + 15);
        cs.stroke();

        cs.setNonStrokingColor(COULEUR_MUTED);
        cs.beginText();
        cs.setFont(PDType1Font.HELVETICA, 8);
        cs.newLineAtOffset(50, y);
        cs.showText("EduCampus - Marketplace des produits educatifs  |  Facture #"
                + cmd.getId() + "  |  Document genere automatiquement");
        cs.endText();
    }

    private String safe(String s) {
        if (s == null) return "—";
        // PDFBox Type1 ne supporte pas les caractères non-latin1
        return s.replace("é","e").replace("è","e").replace("ê","e").replace("ë","e")
                .replace("à","a").replace("â","a").replace("ä","a")
                .replace("ù","u").replace("û","u").replace("ü","u")
                .replace("î","i").replace("ï","i").replace("ô","o").replace("ö","o")
                .replace("ç","c").replace("ñ","n").replace("É","E").replace("È","E")
                .replace("À","A").replace("Ù","U").replace("Ô","O").replace("Ç","C")
                .replaceAll("[^\\x00-\\xFF]", "?");
    }

    private String tronquer(String s, int max) {
        if (s == null) return "";
        return s.length() > max ? s.substring(0, max - 1) + "…" : s;
    }
}
