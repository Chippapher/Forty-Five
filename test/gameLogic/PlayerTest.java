package gameLogic;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Arrays;

public class PlayerTest {
    @Test
    @DisplayName("Testing DisplayValue")
    void displayValueTest(){
        var player = new Player("name");
        var game = new Game();
        player.registerGame(game);
        game.topCard = new Card(Suit.CLUBS, 3);
        player.dealCard(new Card(Suit.HEARTS, 2));
        player.dealCard(new Card(Suit.HEARTS, 3));
        player.dealCard(new Card(Suit.HEARTS, 4));
        player.dealCard(new Card(Suit.HEARTS, 5));
        player.dealCard(new Card(Suit.CLUBS, 5));

        player.notifyOpponentsPlay(new Card(Suit.CLUBS, 11));
        Assertions.assertTrue(player.getEligiblePlays().containsAll(Arrays.asList(new Card(Suit.HEARTS, 2),
                new Card(Suit.HEARTS, 3),
                new Card(Suit.HEARTS, 4),
                new Card(Suit.HEARTS, 5),
                new Card(Suit.CLUBS, 5))));
    }
}
