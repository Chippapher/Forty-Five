package gameLogic;

public class Card {
    private Suit suit;
    private int value;
    public Card(Suit s, int v){
        suit = s;
        value = v;
    }
    public Card(String c){
        value = switch(c.substring(0,1)){
            case  "A" -> 1;
            case  "J" -> 11;
            case  "Q" -> 12;
            case  "K" -> 13;
            default -> (c.length() == 2) ? Integer.parseInt(c.substring(0,1)) : Integer.parseInt(c.substring(0,2));
        };
        suit = switch ((c.length() == 2) ? c.substring(1) : c.substring(2)){
            case  "S" -> Suit.SPADES;
            case  "C" -> Suit.CLUBS;
            case  "H" -> Suit.HEARTS;
            case  "D" -> Suit.DIAMONDS;
            default -> null;
        };
    }
    /**
     * copy contructor
     * */
    public Card(Card c) {
        this.suit = c.getSuit();
        this.value = c.getValue();
    }

    public Suit getSuit(){
        return suit;
    }
    public  int getValue(){
        return value;
    }
    public boolean isTrump(Suit trumpSuit){
        return this.suit == trumpSuit || (this.suit == Suit.HEARTS && this.value == 1);
    }
    @Override
    public boolean equals(Object o){
        var otherCard = (Card)o;
        return (suit == otherCard.getSuit() && value == otherCard.getValue());
    }
    @Override
    public int hashCode(){
        return 31 * value + 31 * switch (suit){
            case SPADES -> 0;
            case CLUBS -> 1;
            case HEARTS -> 2;
            case DIAMONDS -> 3;
        };
    }
    @Override
    public String toString(){
        String v = switch(value){
            case 1 -> "A";
            case 11 -> "J";
            case 12 -> "Q";
            case 13 -> "K";
            default -> ((Integer)value).toString();
        };
        return v + switch (suit){
            case SPADES -> "S" ;
            case CLUBS -> "C";
            case HEARTS -> "H";
            case DIAMONDS -> "D";
        };
    }
}

