package gameLogic;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;

public class Player {
    private String name;
    private Set<Card> hand;
    private PlayerView userView;
    private Game game;
    private Card opponentsPlay;
    private boolean robOccurred;
    private boolean playerDisconnected;

    public Player(){
        this("");
    }

    public Player(String name){
        this.name = name;
        this.hand = ConcurrentHashMap.newKeySet();
        this.opponentsPlay = null;
    }

    public void registerGame(Game g){
        game = g;
    }
    public void registerPlayerView(PlayerView uv){
        if (uv == null) {
            throw new NullPointerException("registerUserView requires non null argument");
        }
        if(userView == null) {
            userView = uv;
            game.notifyReady(this);
        } else {
            userView = uv;
            this.playerReconnected();

        }
    }



    /* query methods */
    public String getName(){
        return this.name;
    }
    public String getOpponentName(){
        return game.getOpponentName(this);
    }
    public Card getCard() {
        System.out.println("asking for card");
        Card card = null;
        try {
            card = userView.getCard().get();
        } catch (InterruptedException | ExecutionException e) {
            e.printStackTrace();
        }
        System.out.println("card received");
        removeCard(card);
        return card;
    }
    public boolean getRob() {
        Optional<Card> pitch = Optional.empty();
        try {
            pitch = userView.getRob().get();
            pitch.ifPresent(c -> swap(c, game.getTopCard()));
        } catch (InterruptedException | ExecutionException e) {
            e.printStackTrace();
        }
        robOccurred = pitch.isPresent();

        return robOccurred;
    }
    public Set<Card> getHand() {
        return new HashSet<>(hand);
    }
    public boolean hasAceOfTrump(Suit trump){
        for(Card c : hand){
            if(c.getValue() == 1 && c.getSuit() == trump){
                return true;
            }
        }
        return false;
    }

    // TODO: 2020-04-05 jack of trumps does not compel ace of hearts
    public Set<Card> getEligiblePlays(){
        if(opponentsPlay == null){
            return hand;
        }
        var trumpSuit = game.getTopCard().getSuit();
        Set<Card> set = new HashSet<>();
        for (Card c :
                hand) {
            set.add(new Card(c));
        }
        var opponentSuit = opponentsPlay.getSuit();
        //this is reneging logic
        if(
                !(opponentsPlay.isTrump(trumpSuit) &&
                hand
                .stream()
                .filter(c -> c.isTrump(trumpSuit))
                        //renege-able
                .allMatch(c -> Game.trumpPrecedence(c) >= 1)
                )
                &&
                hand
                        .stream()
                        .anyMatch(c-> c.getSuit() == opponentSuit)
        ) {
            set.removeIf(c -> !c.isTrump(trumpSuit) && c.getSuit() != opponentSuit);
        }
        return set;

    }

    /*update methods*/
    public void setName(String name){
        this.name = name;
    }
    public void dealCard(Card c) throws IllegalArgumentException{
        if(!hand.add(c)){
            throw new IllegalArgumentException(
                    String.format("Attempt to add card: %s to player: %s's hand failed because card already in hand",
                            c,
                            name));
        }
        if (userView != null) {
            userView.notifyHandChange();
        }
    }
    public void reset(){
        hand.clear();
        robOccurred = false;
        opponentsPlay = null;
        userView.reset();
    }

    /*notify methods*/

    /**
     * @param pointsMap must be a map with the keys being this player, and their opponent. the nested maps keys should
     *                  be "hand" and "game"
     * */
    public void notifyPointChange(Map<Player,Map<String, Integer>> pointsMap){
        pointsMap.forEach((k, v) ->{
            if (k.equals(this)) {
                userView.setHandPoints(v.get("hand"));
                userView.setMatchPoints(v.get("game"));
            } else {
                userView.setOpponentHandPoints(v.get("hand"));
                userView.setOpponentMatchPoints(v.get("game"));
            }
        });
    }
    /**
     * @param thisPlayer should be positive if it is this player, negative if it is opponent, 0 if neither
     * */
    public void notifyBestTrump(int thisPlayer){
        //this should probably just notify the userview, so the user knows where the extra points game from
    }
    public void notifyTrump(Card c){
        try {
            userView.setTrump(c);
        } catch(NullPointerException e){
            System.err.printf("Error ocurred while setting trump for %s", name);
            e.printStackTrace();
        }
    }
    public void notifyOpponentsPlay(Card c){
        opponentsPlay = c;
        if (userView == null) {
            return ;
        }
        userView.addToPlayZone(c);
    }
    public void notifyNewLift(){
        opponentsPlay = null;
        userView.resetPlayZone();
    }
    //notify the user that the opponent robbed
    public void notifyOpponentRob(){
        robOccurred = true;
        userView.setTrump(null);
    }
    public void notifyUserDisconnect(){
        this.playerDisconnected = true;
        game.notifyPlayerDisconnect();
    }

    public CompletableFuture<Boolean> promptRematch(String winner){
        return userView.promptRematch(winner);
    }


    /*helper methods*/

    private void removeCard(Card c) throws IllegalArgumentException{
        if(!hand.remove(c)){
            throw new IllegalArgumentException(
                    String.format("card: %s not contained in player: %s's hand", c, name)
            );
        }
        if (userView != null) {
            userView.notifyHandChange();
        }
    }
    private void swap(Card old, Card replacement){
        if(hand.remove(old)){
            hand.add(replacement);
        }
    }


    public boolean isDisconnected() {
        return playerDisconnected;
    }

    private void playerReconnected() {
        userView.notifyHandChange();
        if(robOccurred){
            userView.setTrump(game.getTopCard());
            userView.setTrump(null);
        }

    }

}
