package gameLogic;

import java.util.HashSet;

public class Deck {
    private int cardsInDeck;
    private HashSet<Card> cardsUsed;
    public Deck(){
        cardsInDeck = 52;
        cardsUsed = new HashSet<>();
    }
    void shuffle(){
        cardsUsed.clear();
        cardsInDeck = 52;
    }
    public Card getCard() throws  IllegalStateException{
        if(cardsInDeck < 1){
            throw new IllegalStateException("There are no cards left in the deck");
        }
        Card card;
        do{
            card = new Card(randomSuit(), randomValue());
        } while(!cardsUsed.add(card));
        cardsInDeck--;
        return card;
    }
    private Suit randomSuit(){
        return switch ((int)(Math.random()*4)){
            case 0 -> Suit.SPADES;
            case 1 -> Suit.CLUBS;
            case 2 -> Suit.HEARTS;
            case 3 -> Suit.DIAMONDS;
            default -> throw new IllegalStateException("Unexpected value: " + (int) (Math.random() * 4));
        };
    }
    private int randomValue(){
        return (int)(Math.random() * 13)+ 1;
    }
}
