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

    private static final DateTimeFormatter FMT      = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    private static final DateTimeFormatter FMT_DATE = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    // ── Palette moderne ───────────────────────────────────────────────────────
    private static final Color C_BLEU_FONCE  = new Color(15,  23,  42);   // slate-900
    private static final Color C_BLEU        = new Color(37,  99, 235);   // blue-600
    private static final Color C_BLEU_CLAIR  = new Color(219, 234, 254);  // blue-100
    private static final Color C_VIOLET      = new Color(124, 58, 237);   // violet-600
    private static final Color C_VERT        = new Color(16, 185, 129);   // emerald-500
    private static final Color C_ORANGE      = new Color(245, 158, 11);   // amber-500
    private static final Color C_ROUGE       = new Color(239, 68,  68);   // red-500
    private static final Color C_GRIS_CLAIR  = new Color(248, 250, 252);  // slate-50
    private static final Color C_GRIS        = new Color(226, 232, 240);  // slate-200
    private static final Color C_TEXTE       = new Color(15,  23,  42);   // slate-900
    private static final Color C_MUTED       = new Color(100, 116, 139);  // slate-500
    private static final Color C_BLANC       = Color.WHITE;

    // ── API publique ──────────────────────────────────────────────────────────

    public File genererPng(Commande cmd, List<LigneCommande> lignes, Livraison liv) throws Exception {
        PDDocument doc = new PDDocument();
        PDPage page = new PDPage(PDRectangle.A4);
        doc.addPage(page);

        float W = page.getMediaBox().getWidth();
        float H = page.getMediaBox().getHeight();

        PDPageContentStream cs = new PDPageContentStream(doc, page);
        cs.setNonStrokingColor(C_BLANC);
        cs.addRect(0, 0, W, H); cs.fill();

        dessiner(cs, W, H, cmd, lignes, liv);
        cs.close();

        org.apache.pdfbox.rendering.PDFRenderer renderer =
                new org.apache.pdfbox.rendering.PDFRenderer(doc);
        java.awt.image.BufferedImage image = renderer.renderImageWithDPI(
                0, 300, org.apache.pdfbox.rendering.ImageType.RGB);
        doc.close();

        String nom = "Facture_EduCampus_" + cmd.getId() + ".png";
        File dest = new File(System.getProperty("user.home") + "/Downloads/" + nom);
        javax.imageio.ImageIO.write(image, "PNG", dest);
        return dest;
    }

    // ── Dessin principal ──────────────────────────────────────────────────────

    private void dessiner(PDPageContentStream cs, float W, float H,
                          Commande cmd, List<LigneCommande> lignes, Livraison liv) throws Exception {

        float mG = 45f, mD = W - 45f;
        float y  = H;

        // ── Bandeau header dégradé simulé (deux rectangles) ──
        cs.setNonStrokingColor(C_BLEU_FONCE);
        cs.addRect(0, H - 90, W, 90); cs.fill();

        // Accent violet à droite
        cs.setNonStrokingColor(C_VIOLET);
        cs.addRect(W - 120, H - 90, 120, 90); cs.fill();

        // Ligne décorative bas du header
        cs.setNonStrokingColor(C_BLEU);
        cs.addRect(0, H - 92, W, 3); cs.fill();

        // Logo texte
        cs.setNonStrokingColor(C_BLANC);
        cs.beginText(); cs.setFont(PDType1Font.HELVETICA_BOLD, 24);
        cs.newLineAtOffset(mG, H - 42); cs.showText("EduCampus"); cs.endText();

        cs.setNonStrokingColor(new Color(148, 163, 184));
        cs.beginText(); cs.setFont(PDType1Font.HELVETICA, 9);
        cs.newLineAtOffset(mG, H - 58); cs.showText("Marketplace des produits educatifs"); cs.endText();

        // FACTURE + numéro
        cs.setNonStrokingColor(C_BLANC);
        cs.beginText(); cs.setFont(PDType1Font.HELVETICA_BOLD, 20);
        cs.newLineAtOffset(mD - 130, H - 38); cs.showText("FACTURE"); cs.endText();

        cs.setNonStrokingColor(new Color(196, 181, 253)); // violet-300
        cs.beginText(); cs.setFont(PDType1Font.HELVETICA_BOLD, 13);
        cs.newLineAtOffset(mD - 130, H - 56); cs.showText("#" + String.format("%06d", cmd.getId())); cs.endText();

        y = H - 110;

        // ── Bloc statut paiement ──
        y = dessinerStatutPaiement(cs, mG, mD, y, cmd);
        y -= 14;

        // ── Deux colonnes : commande + livraison ──
        y = dessinerInfos2Col(cs, mG, mD, y, cmd, liv);
        y -= 18;

        // ── Tableau articles ──
        y = dessinerTableau(cs, mG, mD, y, lignes);
        y -= 16;

        // ── Récapitulatif financier ──
        y = dessinerRecap(cs, mG, mD, y, lignes, cmd.getTotal());
        y -= 20;

        // ── Message de remerciement ──
        dessinerMerci(cs, mG, mD, y);

        // ── Pied de page ──
        dessinerPied(cs, W, cmd);
    }

    // ── Statut paiement ───────────────────────────────────────────────────────

    private float dessinerStatutPaiement(PDPageContentStream cs,
            float mG, float mD, float y, Commande cmd) throws Exception {

        // Badge "PAYE" vert
        cs.setNonStrokingColor(new Color(209, 250, 229)); // emerald-100
        cs.addRect(mG, y - 28, 90, 28); cs.fill();
        cs.setStrokingColor(C_VERT);
        cs.setLineWidth(1.5f);
        cs.addRect(mG, y - 28, 90, 28); cs.stroke();

        cs.setNonStrokingColor(C_VERT);
        cs.beginText(); cs.setFont(PDType1Font.HELVETICA_BOLD, 11);
        cs.newLineAtOffset(mG + 10, y - 18); cs.showText("PAYE"); cs.endText();

        // Date émission
        cs.setNonStrokingColor(C_MUTED);
        cs.beginText(); cs.setFont(PDType1Font.HELVETICA, 8);
        cs.newLineAtOffset(mG + 100, y - 10);
        cs.showText("Emis le : " + (cmd.getDateCommande() != null
                ? cmd.getDateCommande().format(FMT) : "—")); cs.endText();

        cs.beginText(); cs.setFont(PDType1Font.HELVETICA, 8);
        cs.newLineAtOffset(mG + 100, y - 22);
        cs.showText("Methode : Stripe  |  Devise : TND"); cs.endText();

        return y - 28;
    }

    // ── Infos 2 colonnes ──────────────────────────────────────────────────────

    private float dessinerInfos2Col(PDPageContentStream cs,
            float mG, float mD, float y,
            Commande cmd, Livraison liv) throws Exception {

        float colW = (mD - mG - 16) / 2f;
        float col2 = mG + colW + 16;
        float hauteur = 110f;

        // Fond col 1
        cs.setNonStrokingColor(C_GRIS_CLAIR);
        cs.addRect(mG, y - hauteur, colW, hauteur); cs.fill();
        cs.setStrokingColor(C_GRIS);
        cs.setLineWidth(0.5f);
        cs.addRect(mG, y - hauteur, colW, hauteur); cs.stroke();

        // Fond col 2
        cs.setNonStrokingColor(C_GRIS_CLAIR);
        cs.addRect(col2, y - hauteur, colW, hauteur); cs.fill();
        cs.setStrokingColor(C_GRIS);
        cs.addRect(col2, y - hauteur, colW, hauteur); cs.stroke();

        // Titres
        cs.setNonStrokingColor(C_BLEU);
        cs.beginText(); cs.setFont(PDType1Font.HELVETICA_BOLD, 9);
        cs.newLineAtOffset(mG + 8, y - 14); cs.showText("DETAILS COMMANDE"); cs.endText();

        cs.beginText(); cs.setFont(PDType1Font.HELVETICA_BOLD, 9);
        cs.newLineAtOffset(col2 + 8, y - 14); cs.showText("LIVRAISON"); cs.endText();

        // Séparateur titre
        cs.setStrokingColor(C_BLEU);
        cs.setLineWidth(1f);
        cs.moveTo(mG + 8, y - 18); cs.lineTo(mG + colW - 8, y - 18); cs.stroke();
        cs.moveTo(col2 + 8, y - 18); cs.lineTo(col2 + colW - 8, y - 18); cs.stroke();

        // Données col 1
        String[][] d1 = {
            {"Commande", "#" + cmd.getId()},
            {"Date", cmd.getDateCommande() != null ? cmd.getDateCommande().format(FMT) : "—"},
            {"Paiement", "Stripe"},
            {"Statut", "Confirme"}
        };
        float yl = y - 32;
        for (String[] row : d1) {
            cs.setNonStrokingColor(C_MUTED);
            cs.beginText(); cs.setFont(PDType1Font.HELVETICA, 8);
            cs.newLineAtOffset(mG + 8, yl); cs.showText(row[0]); cs.endText();
            cs.setNonStrokingColor(C_TEXTE);
            cs.beginText(); cs.setFont(PDType1Font.HELVETICA_BOLD, 8);
            cs.newLineAtOffset(mG + 70, yl); cs.showText(safe(row[1])); cs.endText();
            yl -= 16;
        }

        // Données col 2
        String[][] d2 = {
            {"Adresse", liv != null ? liv.getAdresse() : "—"},
            {"Ville",   liv != null ? liv.getVille()   : "—"},
            {"Tel",     liv != null && liv.getPhoneNumber() != null ? liv.getPhoneNumber() : "—"},
            {"Statut",  liv != null ? liv.getStatusLivraison().replace("_", " ") : "—"},
            {"Date",    liv != null && liv.getDateLivraison() != null
                        ? liv.getDateLivraison().format(FMT_DATE) : "Non precisee"}
        };
        yl = y - 32;
        for (String[] row : d2) {
            cs.setNonStrokingColor(C_MUTED);
            cs.beginText(); cs.setFont(PDType1Font.HELVETICA, 8);
            cs.newLineAtOffset(col2 + 8, yl); cs.showText(row[0]); cs.endText();
            cs.setNonStrokingColor(C_TEXTE);
            cs.beginText(); cs.setFont(PDType1Font.HELVETICA_BOLD, 8);
            cs.newLineAtOffset(col2 + 55, yl); cs.showText(safe(row[1])); cs.endText();
            yl -= 16;
        }

        return y - hauteur;
    }

    // ── Tableau articles ──────────────────────────────────────────────────────

    private float dessinerTableau(PDPageContentStream cs,
            float mG, float mD, float y, List<LigneCommande> lignes) throws Exception {

        float lT = mD - mG;
        float[] colW = { lT * 0.44f, lT * 0.19f, lT * 0.15f, lT * 0.22f };
        String[] ent = { "PRODUIT", "PRIX UNIT.", "QTE", "SOUS-TOTAL" };

        // En-tête
        cs.setNonStrokingColor(C_BLEU_FONCE);
        cs.addRect(mG, y - 24, lT, 24); cs.fill();

        cs.setNonStrokingColor(C_BLANC);
        float xC = mG + 8;
        for (int i = 0; i < ent.length; i++) {
            cs.beginText(); cs.setFont(PDType1Font.HELVETICA_BOLD, 8);
            cs.newLineAtOffset(xC, y - 16); cs.showText(ent[i]); cs.endText();
            xC += colW[i];
        }
        y -= 24;

        // Lignes
        boolean pair = false;
        for (LigneCommande lc : lignes) {
            Color bg = pair ? C_GRIS_CLAIR : C_BLANC;
            cs.setNonStrokingColor(bg);
            cs.addRect(mG, y - 22, lT, 22); cs.fill();

            // Ligne séparatrice fine
            cs.setStrokingColor(C_GRIS);
            cs.setLineWidth(0.3f);
            cs.moveTo(mG, y - 22); cs.lineTo(mD, y - 22); cs.stroke();

            cs.setNonStrokingColor(C_TEXTE);
            xC = mG + 8;
            String[] vals = {
                tronquer(lc.getNomProduit(), 32),
                String.format("%.2f TND", lc.getPrixUnitaire()),
                "x" + lc.getQuantite(),
                String.format("%.2f TND", lc.getPrixUnitaire() * lc.getQuantite())
            };
            for (int i = 0; i < vals.length; i++) {
                boolean isTotal = (i == 3);
                cs.setNonStrokingColor(isTotal ? C_BLEU : C_TEXTE);
                cs.beginText();
                cs.setFont(isTotal ? PDType1Font.HELVETICA_BOLD : PDType1Font.HELVETICA, 9);
                cs.newLineAtOffset(xC, y - 15); cs.showText(safe(vals[i])); cs.endText();
                xC += colW[i];
            }
            y -= 22;
            pair = !pair;
        }

        // Bordure bas tableau
        cs.setStrokingColor(C_BLEU_FONCE);
        cs.setLineWidth(1.5f);
        cs.moveTo(mG, y); cs.lineTo(mD, y); cs.stroke();

        return y;
    }

    // ── Récapitulatif financier ───────────────────────────────────────────────

    private float dessinerRecap(PDPageContentStream cs,
            float mG, float mD, float y,
            List<LigneCommande> lignes, double total) throws Exception {

        double sousTotal = lignes.stream()
                .mapToDouble(l -> l.getPrixUnitaire() * l.getQuantite()).sum();

        float xD = mD - 200f;

        // Sous-total
        cs.setNonStrokingColor(C_MUTED);
        cs.beginText(); cs.setFont(PDType1Font.HELVETICA, 9);
        cs.newLineAtOffset(xD, y - 14); cs.showText("Sous-total"); cs.endText();
        cs.setNonStrokingColor(C_TEXTE);
        cs.beginText(); cs.setFont(PDType1Font.HELVETICA, 9);
        cs.newLineAtOffset(mD - 80, y - 14);
        cs.showText(String.format("%.2f TND", sousTotal)); cs.endText();

        // Livraison
        cs.setNonStrokingColor(C_MUTED);
        cs.beginText(); cs.setFont(PDType1Font.HELVETICA, 9);
        cs.newLineAtOffset(xD, y - 28); cs.showText("Livraison"); cs.endText();
        cs.setNonStrokingColor(C_VERT);
        cs.beginText(); cs.setFont(PDType1Font.HELVETICA_BOLD, 9);
        cs.newLineAtOffset(mD - 80, y - 28); cs.showText("GRATUITE"); cs.endText();

        // Séparateur
        cs.setStrokingColor(C_GRIS);
        cs.setLineWidth(0.5f);
        cs.moveTo(xD, y - 34); cs.lineTo(mD, y - 34); cs.stroke();

        // Total — bandeau bleu
        cs.setNonStrokingColor(C_BLEU);
        cs.addRect(xD - 10, y - 58, mD - xD + 10, 22); cs.fill();

        cs.setNonStrokingColor(C_BLANC);
        cs.beginText(); cs.setFont(PDType1Font.HELVETICA_BOLD, 11);
        cs.newLineAtOffset(xD, y - 50); cs.showText("TOTAL TTC"); cs.endText();

        cs.beginText(); cs.setFont(PDType1Font.HELVETICA_BOLD, 13);
        cs.newLineAtOffset(mD - 100, y - 50);
        cs.showText(String.format("%.2f TND", total)); cs.endText();

        return y - 60;
    }

    // ── Message de remerciement ───────────────────────────────────────────────

    private void dessinerMerci(PDPageContentStream cs,
            float mG, float mD, float y) throws Exception {
        float larg = mD - mG;

        cs.setNonStrokingColor(new Color(239, 246, 255)); // blue-50
        cs.addRect(mG, y - 36, larg, 36); cs.fill();
        cs.setStrokingColor(C_BLEU_CLAIR);
        cs.setLineWidth(0.5f);
        cs.addRect(mG, y - 36, larg, 36); cs.stroke();

        cs.setNonStrokingColor(C_BLEU);
        cs.beginText(); cs.setFont(PDType1Font.HELVETICA_BOLD, 10);
        cs.newLineAtOffset(mG + larg / 2 - 80, y - 16);
        cs.showText("Merci pour votre confiance !"); cs.endText();

        cs.setNonStrokingColor(C_MUTED);
        cs.beginText(); cs.setFont(PDType1Font.HELVETICA, 8);
        cs.newLineAtOffset(mG + larg / 2 - 100, y - 28);
        cs.showText("EduCampus - Votre partenaire pour reussir vos etudes"); cs.endText();
    }

    // ── Pied de page ─────────────────────────────────────────────────────────

    private void dessinerPied(PDPageContentStream cs, float W, Commande cmd) throws Exception {
        // Bande sombre en bas
        cs.setNonStrokingColor(C_BLEU_FONCE);
        cs.addRect(0, 0, W, 32); cs.fill();

        cs.setNonStrokingColor(new Color(148, 163, 184));
        cs.beginText(); cs.setFont(PDType1Font.HELVETICA, 7.5f);
        cs.newLineAtOffset(45, 12);
        cs.showText("EduCampus  |  Facture #" + String.format("%06d", cmd.getId())
                + "  |  Document genere automatiquement  |  Non contractuel");
        cs.endText();

        // Accent coloré
        cs.setNonStrokingColor(C_BLEU);
        cs.addRect(0, 30, W, 2); cs.fill();
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private String safe(String s) {
        if (s == null) return "—";
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
        return s.length() > max ? s.substring(0, max - 1) + "..." : s;
    }
}
