package gameLogic;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
/**
 *
 * player viewn is an interface that represents the user's interface with the program (command-line, local gui, etx);
 *
 * */
public interface PlayerView {
    void setTrump(Card c);

    CompletableFuture<Card> getCard();

    CompletableFuture<Optional<Card>> getRob() throws ExecutionException, InterruptedException;

    void addToPlayZone(Card c);

    void resetPlayZone();

    void setOpponentMatchPoints(int points);

    void setOpponentHandPoints(int points);

    void setMatchPoints(int points);

    void setHandPoints(int points);

    void notifyHandChange();

    CompletableFuture<Boolean> promptRematch(String winner);
    void reset();
}
