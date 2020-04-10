package gui;

import java.awt.event.MouseListener;
import gameLogic.*;

public class SuitPanel extends CardPanel {
    private Suit suit;
    public SuitPanel(Card c, MouseListener l) {
        super(c, 180, false , l);
        this.suit = c.getSuit();



    }

    @Override
    protected String cardToPath(){
        return String.format("%s.png", switch(suit){
            case SPADES -> "spade";
            case CLUBS -> "club";
            case DIAMONDS -> "diamond";
            case HEARTS -> "heart";
        });
    }
}
