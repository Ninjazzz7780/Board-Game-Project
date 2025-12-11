import javax.swing.*;
import java.awt.*;

public class DicePanel extends JPanel {
    private int value = 1;
    private Color color = Color.BLACK;

    public DicePanel() {
        setBackground(Color.WHITE);
        setBorder(BorderFactory.createLineBorder(Color.GRAY));
    }

    public void setValue(int value, Color color) {
        this.value = value;
        this.color = color;
        repaint();
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2d = (Graphics2D) g;
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        int w = getWidth();
        int h = getHeight();

        g2d.setColor(color);
        g2d.setFont(new Font("Arial", Font.BOLD, w / 2));
        FontMetrics fm = g2d.getFontMetrics();
        String text = String.valueOf(value);
        int x = (w - fm.stringWidth(text)) / 2;
        int y = (h - fm.getHeight()) / 2 + fm.getAscent();

        g2d.drawString(text, x, y);
    }
}