import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.geom.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.Random;

public final class PatternGen {
  public static void main(String[] args) throws Exception {
    int size = 640;
    BufferedImage img = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
    Graphics2D g = img.createGraphics();
    g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
    g.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);

    g.setComposite(AlphaComposite.Src);
    g.setColor(new Color(0,0,0,0));
    g.fillRect(0,0,size,size);

    Random r = new Random(1337);
    for (int i = 0; i < 18; i++) {
      double x = r.nextDouble() * size;
      double y = r.nextDouble() * size;
      double s = 0.55 + r.nextDouble() * 0.75;
      double rot = (r.nextDouble() * 2 - 1) * Math.toRadians(18);
      int kind = r.nextInt(3);

      Graphics2D gg = (Graphics2D) g.create();
      gg.translate(x, y);
      gg.rotate(rot);
      gg.scale(s, s);
      gg.translate(-64, -64);

      Color stroke = new Color(6, 106, 201, 26);
      gg.setColor(stroke);
      gg.setStroke(new BasicStroke(3.0f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));

      if (kind == 0) drawCap(gg);
      else if (kind == 1) drawBook(gg);
      else drawBuilding(gg);

      gg.dispose();
    }

    g.dispose();

    File out = new File(args.length > 0 ? args[0] : "edu-pattern.png");
    File parent = out.getParentFile();
    if (parent != null) parent.mkdirs();
    ImageIO.write(img, "png", out);
  }

  private static void drawCap(Graphics2D g) {
    Path2D top = new Path2D.Double();
    top.moveTo(22, 54);
    top.lineTo(64, 32);
    top.lineTo(106, 54);
    top.lineTo(64, 76);
    top.closePath();
    g.draw(top);

    RoundRectangle2D band = new RoundRectangle2D.Double(38, 74, 52, 16, 10, 10);
    g.draw(band);

    g.draw(new Line2D.Double(86, 58, 104, 70));
    g.draw(new Line2D.Double(104, 70, 98, 86));
    g.draw(new Ellipse2D.Double(94, 86, 10, 10));
  }

  private static void drawBook(Graphics2D g) {
    RoundRectangle2D left = new RoundRectangle2D.Double(26, 38, 38, 72, 12, 12);
    RoundRectangle2D right = new RoundRectangle2D.Double(64, 38, 38, 72, 12, 12);
    g.draw(left);
    g.draw(right);
    g.draw(new Line2D.Double(64, 40, 64, 108));

    for (int i = 0; i < 4; i++) {
      double yy = 54 + i * 12;
      g.draw(new Line2D.Double(36, yy, 56, yy));
      g.draw(new Line2D.Double(72, yy, 92, yy));
    }
  }

  private static void drawBuilding(Graphics2D g) {
    Path2D roof = new Path2D.Double();
    roof.moveTo(24, 54);
    roof.lineTo(64, 28);
    roof.lineTo(104, 54);
    g.draw(roof);

    RoundRectangle2D base = new RoundRectangle2D.Double(30, 54, 68, 58, 10, 10);
    g.draw(base);

    for (int i = 0; i < 4; i++) {
      double xx = 40 + i * 14;
      g.draw(new Line2D.Double(xx, 62, xx, 106));
    }

    RoundRectangle2D door = new RoundRectangle2D.Double(58, 86, 12, 26, 10, 10);
    g.draw(door);
  }
}
