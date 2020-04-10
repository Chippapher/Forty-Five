package gui;

import gameLogic.*;
import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseListener;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.Objects;

public class CardPanel extends JPanel {

    private BufferedImage image;

    public boolean isHighlighted() {
        return highlighted;
    }
    public void highllight(){
        this.highlighted = true;
        this.setBorder(BorderFactory.createLineBorder(new Color(255,223,0), 3));
    }
    public void unHighlight(){
        this.highlighted = false;
        this.setBorder(null);
    }

    private boolean highlighted;

    protected Card c;
    private Dimension dim;

    public CardPanel(Card c , MouseListener l){
        new CardPanel(c, 100, false, l);
    }
    public CardPanel(Card card, int independentDim, boolean width, MouseListener listener) {
        this.setOpaque(false);
        if (listener != null) {
            this.addMouseListener(listener);
        }
        setIndependantDim(independentDim, width);
        setMinimumSize(dim);
//        setMaximumSize(dim);
        setPreferredSize(dim);
        this.c = card;
    }

    private void initImage() {
        try {
            image = ImageIO.read(new File(String.format("cards_png/PNG/%s", cardToPath())));
        } catch (IOException ex) {
            System.out.println("failed");
            System.err.println(ex);
        }
    }

    public Card getCard() {
        return c;
    }

    public void setIndependantDim(int dim) {
        setIndependantDim(dim, false);
    }

    public void setIndependantDim(int dim, boolean width) {
        if (width) {
            this.dim = new Dimension(dim, (int) (dim * (1 / .65)));
        } else {
            this.dim = new Dimension((int) (dim * .65), dim);
        }
    }

    @SuppressWarnings("UnnecessaryLocalVariable")
    @Override
    protected void paintComponent(Graphics g) {
        revalidate();
        super.paintComponent(g);
        if (image == null) {
            initImage();
        }
        g.drawImage(image, 0, 0, this.getWidth(), this.getHeight(), this);
    }

    protected String cardToPath() {

        var v = this.c.getValue();
        var s = switch (this.c.getSuit()) {
            case HEARTS -> "H";
            case DIAMONDS -> "D";
            case CLUBS -> "C";
            case SPADES -> "S";
        };

        if (v > 1 && v < 11) {
            return String.format("%d%s.png", v, s);
        } else {
            return switch (v) {
                case 13 -> String.format("K%s.png", s);
                case 12 -> String.format("Q%s.png", s);
                case 11 -> String.format("J%s.png", s);
                //there should only be one option left, if not, god help us
                default -> String.format("A%s.png", s);
            };
        }

    }
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof CardPanel)) return false;
        CardPanel cardPanel = (CardPanel) o;
        return c.equals(cardPanel.c);
    }

    @Override
    public int hashCode() {
        return Objects.hash(c);
    }

    @Override
    public String toString() {
        return "gui.CardPanel{" +
                "c=" + c +
                '}';
    }
}
