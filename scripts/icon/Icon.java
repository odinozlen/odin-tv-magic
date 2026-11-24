import java.awt.*;
import java.awt.geom.*;
import java.awt.image.BufferedImage;
import java.io.File;
import javax.imageio.ImageIO;

public class Icon {
    static final Color A = new Color(0x0B1030), B = new Color(0x3D5BFF), C = new Color(0x9A5CFF);

    static void bg(Graphics2D g, int w, int h, int radius) {
        Shape clip = radius > 0 ? new RoundRectangle2D.Float(0, 0, w, h, radius * 2, radius * 2) : new Rectangle2D.Float(0, 0, w, h);
        g.setClip(clip);
        g.setPaint(new LinearGradientPaint(0, 0, w, h, new float[]{0f, 0.55f, 1f}, new Color[]{A, B, C}));
        g.fill(clip);
        // soft violet glow bottom-right, light top-left
        g.setPaint(new RadialGradientPaint(new Point2D.Float(w * 1.0f, h * 1.05f), Math.max(w, h) * 0.75f,
                new float[]{0f, 1f}, new Color[]{new Color(138, 92, 255, 170), new Color(138, 92, 255, 0)}));
        g.fill(clip);
        g.setPaint(new RadialGradientPaint(new Point2D.Float(w * 0.15f, h * 0.05f), Math.max(w, h) * 0.6f,
                new float[]{0f, 1f}, new Color[]{new Color(255, 255, 255, 40), new Color(255, 255, 255, 0)}));
        g.fill(clip);
    }

    /** remote D-pad ring = the "O" of Odin */
    static void mark(Graphics2D g, float cx, float cy, float r) {
        float stroke = r * 0.30f;
        g.setStroke(new BasicStroke(stroke, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.setColor(Color.WHITE);
        g.draw(new Ellipse2D.Float(cx - r, cy - r, 2 * r, 2 * r));
        // the magic: a sparkle in the middle and a small one on the ring
        g.fill(sparkle(cx, cy, r * 0.46f));
        g.fill(sparkle(cx + r * 0.98f, cy - r * 0.98f, r * 0.30f));
        // four small arrows on the ring
        g.setColor(new Color(14, 22, 54, 200));
        float t = stroke * 0.32f;
        for (int i = 0; i < 4; i++) {
            AffineTransform old = g.getTransform();
            g.rotate(Math.PI / 2 * i, cx, cy);
            Path2D p = new Path2D.Float();
            p.moveTo(cx, cy - r - t);
            p.lineTo(cx - t * 1.2f, cy - r + t * 0.6f);
            p.lineTo(cx + t * 1.2f, cy - r + t * 0.6f);
            p.closePath();
            g.fill(p);
            g.setTransform(old);
        }
    }

    /** four-pointed star with curved sides */
    static Shape sparkle(float cx, float cy, float r) {
        float k = r * 0.16f;
        Path2D p = new Path2D.Float();
        p.moveTo(cx, cy - r);
        p.quadTo(cx + k, cy - k, cx + r, cy);
        p.quadTo(cx + k, cy + k, cx, cy + r);
        p.quadTo(cx - k, cy + k, cx - r, cy);
        p.quadTo(cx - k, cy - k, cx, cy - r);
        p.closePath();
        return p;
    }

    static BufferedImage icon(int s, boolean rounded) {
        BufferedImage img = new BufferedImage(s, s, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();
        hints(g);
        bg(g, s, s, rounded ? (int) (s * 0.225) : 0);
        mark(g, s / 2f, s / 2f, s * 0.27f);
        g.dispose();
        return img;
    }

    static BufferedImage banner(int w, int h) {
        BufferedImage img = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();
        hints(g);
        bg(g, w, h, 0);
        mark(g, h * 0.46f, h / 2f, h * 0.19f);
        g.setColor(Color.WHITE);
        g.setFont(new Font("Helvetica Neue", Font.BOLD, (int) (h * 0.21)));
        g.drawString("Odin TV", h * 0.84f, h * 0.54f);
        g.setColor(new Color(0xD9C9FF));
        g.setFont(new Font("Helvetica Neue", Font.BOLD, (int) (h * 0.17)));
        g.drawString("Magic", h * 0.85f, h * 0.76f);
        g.dispose();
        return img;
    }

    static void hints(Graphics2D g) {
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        g.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
    }

    /** render big, scale down smoothly */
    static BufferedImage scaled(BufferedImage src, int w, int h) {
        BufferedImage out = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = out.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
        g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        g.drawImage(src, 0, 0, w, h, null);
        g.dispose();
        return out;
    }

    public static void main(String[] a) throws Exception {
        String out = a[0];
        BufferedImage square = icon(1024, false), round = icon(1024, true);
        ImageIO.write(scaled(square, 180, 180), "png", new File(out, "web-icon.png"));       // apple-touch-icon (iOS rounds it)
        ImageIO.write(scaled(square, 512, 512), "png", new File(out, "web-icon-512.png"));
        ImageIO.write(scaled(round, 192, 192), "png", new File(out, "app-icon.png"));        // Android launcher icon
        ImageIO.write(scaled(banner(1280, 720), 320, 180), "png", new File(out, "banner.png")); // Android TV banner (mdpi)
        ImageIO.write(scaled(banner(1280, 720), 640, 360), "png", new File(out, "banner-xhdpi.png"));
        ImageIO.write(scaled(round, 96, 96), "png", new File(out, "icon-xhdpi.png"));
        ImageIO.write(scaled(round, 512, 512), "png", new File(out, "preview.png"));
    }
}
