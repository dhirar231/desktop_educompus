import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.geom.*;
import java.awt.image.BufferedImage;
import java.io.File;

public final class IconGen {
  public static void main(String[] args) throws Exception {
    int size = 256;
    BufferedImage img = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
    Graphics2D g = img.createGraphics();
    g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
    g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

    // Background gradient
    GradientPaint gp = new GradientPaint(0, 0, new Color(7, 92, 186), size, size, new Color(0, 164, 255));
    g.setPaint(gp);
    RoundRectangle2D bg = new RoundRectangle2D.Double(16, 16, size - 32, size - 32, 64, 64);
    g.fill(bg);

    // Soft highlight
    g.setComposite(AlphaComposite.SrcOver.derive(0.18f));
    g.setColor(Color.WHITE);
    g.fill(new Ellipse2D.Double(-30, -40, 220, 220));
    g.setComposite(AlphaComposite.SrcOver);

    // "EC" text
    g.setColor(Color.WHITE);
    Font font = new Font("Segoe UI", Font.BOLD, 104);
    g.setFont(font);
    String text = "EC";
    FontMetrics fm = g.getFontMetrics();
    int tw = fm.stringWidth(text);
    int th = fm.getAscent();
    int x = (size - tw) / 2;
    int y = (size + th) / 2 - 18;

    // Subtle shadow
    g.setComposite(AlphaComposite.SrcOver.derive(0.20f));
    g.setColor(new Color(0, 0, 0));
    g.drawString(text, x + 3, y + 4);
    g.setComposite(AlphaComposite.SrcOver);
    g.setColor(Color.WHITE);
    g.drawString(text, x, y);

    // Bottom accent line
    g.setComposite(AlphaComposite.SrcOver.derive(0.22f));
    g.setStroke(new BasicStroke(10f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
    g.setColor(Color.WHITE);
    g.draw(new Line2D.Double(72, 190, 184, 190));

    g.dispose();

    File out = new File(args.length > 0 ? args[0] : "app-icon.png");
    File parent = out.getParentFile();
    if (parent != null) parent.mkdirs();
    ImageIO.write(img, "png", out);
  }
}
