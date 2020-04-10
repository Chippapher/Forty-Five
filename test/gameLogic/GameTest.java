package gameLogic;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class GameTest {

    @Test
    void leaderBeatsTest(){
        //five beats jack
        Assertions.assertTrue(Game.leaderBeats(new Card(Suit.CLUBS, 5), new Card(Suit.CLUBS, 11), Suit.CLUBS));
        Assertions.assertFalse(Game.leaderBeats(new Card(Suit.CLUBS, 11), new Card(Suit.CLUBS, 5), Suit.CLUBS));

        //five beats aces
        Assertions.assertTrue(Game.leaderBeats(new Card(Suit.CLUBS, 5), new Card(Suit.CLUBS, 1), Suit.CLUBS));
        Assertions.assertTrue(Game.leaderBeats(new Card(Suit.CLUBS, 5), new Card(Suit.HEARTS, 1), Suit.CLUBS));
        Assertions.assertFalse(Game.leaderBeats(new Card(Suit.CLUBS, 1), new Card(Suit.CLUBS, 5), Suit.CLUBS));
        Assertions.assertFalse(Game.leaderBeats(new Card(Suit.HEARTS, 1), new Card(Suit.CLUBS, 5),  Suit.CLUBS));

        //five beats rando-trump;
        Assertions.assertTrue(Game.leaderBeats(new Card(Suit.CLUBS, 5), new Card(Suit.CLUBS, 2), Suit.CLUBS));

        //five beats non-trump;
        Assertions.assertTrue(Game.leaderBeats(new Card(Suit.CLUBS, 5), new Card(Suit.HEARTS, 11), Suit.CLUBS));

        //not follow suit, no trump
        Assertions.assertTrue(Game.leaderBeats(new Card(Suit.HEARTS, 2), new Card(Suit.DIAMONDS, 13), Suit.CLUBS));

        //not follow suit, trump
        Assertions.assertFalse(Game.leaderBeats(new Card(Suit.HEARTS, 2), new Card(Suit.DIAMONDS, 2), Suit.DIAMONDS));

        //follow suit, no trump;
        Assertions.assertTrue(Game.leaderBeats(new Card(Suit.DIAMONDS, 11), new Card(Suit.DIAMONDS, 6), Suit.CLUBS));
        Assertions.assertFalse(Game.leaderBeats(new Card(Suit.DIAMONDS, 6), new Card(Suit.DIAMONDS, 11), Suit.CLUBS));

        //follow suit, trump
        Assertions.assertTrue(Game.leaderBeats(new Card(Suit.DIAMONDS, 11), new Card(Suit.DIAMONDS, 6), Suit.DIAMONDS));
        Assertions.assertFalse(Game.leaderBeats(new Card(Suit.DIAMONDS, 6), new Card(Suit.DIAMONDS, 11), Suit.DIAMONDS));
    }
}
