package gameLogic;

import gui.UserView;

import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.function.Predicate;

// TODO: 2020-04-01 gGame should construct the players and expose methods of registering a userView

public class Game implements Runnable{
    private Player p1, p2;
    private Deck deck;
    private Player dealer;
    private Player leader;
    private Player follower;
    String winner;

    private boolean p1ready, p2ready;
    Card topCard;
    private Map<Player, Map<String, Integer>> pointsMap;

    private ArrayList<Alertable> terminationListeners;

    public Game(Player player1, Player player2){
        p1 = player1;
        p2 = player2;

        p1.registerGame(this);
        p2.registerGame(this);

        deck = new Deck();

        pointsMap = new HashMap<>();
        pointsMap.put(p1, new HashMap<>());
        pointsMap.put(p2, new HashMap<>());

        pointsMap.get(p1).put("game", 0);
        pointsMap.get(p2).put("game", 0);
        pointsMap.get(p1).put("hand", 0);
        pointsMap.get(p2).put("hand", 0);
        terminationListeners = new ArrayList<>();
        winner = "";
    }

    private void reset(){
        deck = new Deck();

        updatePointsMap(0,0, p1);
        updatePointsMap(0,0, p2);
        winner = "";
        p1.reset();
        p2.reset();
    }
    public Game(){
        this(new Player(), new Player());
    }

    public Player getPlayer1() {
        return p1;
    }
    public Player getPlayer2() {
        return p2;
    }

    synchronized void notifyReady(Player p){
        if(p.equals(p1)){
            p1ready = true;
        } else {
            p2ready = true;
        }

        if(p1ready && p2ready){
            Thread gameThread = new Thread(this, String.format("Game Thread for players : %s & %s", p1.getName(), p2.getName()));
            gameThread.start();
        }
    }

    public void run() {
        dealer = (int)(Math.random() * 2) == 0 ? p1:p2;
        follower = dealer;
        leader = follower == p1 ? p2:p1;
        while(noOneHasWon()){
            playHand();
            swapDealer();
            resetHandPoints(p1);
            resetHandPoints(p2);
        }

        var r1 = p1.promptRematch(winner);
        var r2 = p2.promptRematch(winner);
        try {
            if(r1.get() && r2.get()){
                this.reset();
                this.run();
            } else {
                gameTerminated();
            }
        } catch (InterruptedException | ExecutionException e) {
            e.printStackTrace();
        }

    }

    // I need some mechanism by which to resend a request to the user in the event of a disconnection.
    // perhaps a try/catch thing? this might be  a major refactor...
    private void playHand() {
        //pre-play setup
        deck.shuffle();
        for (int i = 0; i < 5; i++) {
            p1.dealCard(deck.getCard());
            p2.dealCard(deck.getCard());
        }
        topCard = deck.getCard();
        Suit trump = topCard.getSuit();
        p1.notifyTrump(topCard);
        p2.notifyTrump(topCard);

        System.out.println("trump is:" + trump);

        var playerWithBestTrump = getPlayerWithBestTrump(p1,p2,trump);


        // according to bicycle, the official robbing logic, and the robbing logic implemented here are pretty different
       if (topCard.getValue() == 1) {
           if(dealer.getRob()){
               leader.notifyOpponentRob();
           }
        } else if (p1.hasAceOfTrump(trump)) {
           //check if p1 wants to swap
           if(p1.getRob()){
               p2.notifyOpponentRob();
           }
        } else if (p2.hasAceOfTrump(trump)) {
           //check if p2 wants to swap
           if(p2.getRob()){
               p1.notifyOpponentRob();
           }
       }

        while(follower.getHand().size() > 0) {
            Card leadCard;
            Card followCard;

           leadCard = leader.getCard();

           follower.notifyOpponentsPlay(leadCard);
           followCard = follower.getCard();

           leader.notifyOpponentsPlay(followCard);

           if (leaderBeats(leadCard, followCard, trump)) {
                incrementHandPoints(leader);
           } else {
               incrementHandPoints(follower);
               swapLeader();
           }

            if(pointsMap.get(p1).get("game") == 45 || pointsMap.get(p2).get("game") == 45){
                winner = (pointsMap.get(p1).get("game") == 45)? p1.getName(): p2.getName();
                return;
            }
           p1.notifyNewLift();
           p2.notifyNewLift();
       }
       if(playerWithBestTrump.isPresent()){
           var p = playerWithBestTrump.get();
           p1.notifyBestTrump(p.equals(p1)? 1: -1);
           p2.notifyBestTrump(p.equals(p2)? 1: -1);
           incrementHandPoints(p);
       } else {
           p1.notifyBestTrump(0);
           p2.notifyBestTrump(0);
       }
        if(pointsMap.get(p1).get("game") == 45 || pointsMap.get(p2).get("game") == 45){
            winner = (pointsMap.get(p1).get("game") == 45)? p1.getName(): p2.getName();
            return;
        }

    }
    private void updatePointsMap(int game, int hand, Player player){
        this.pointsMap.get(player).put("game", game);
        this.pointsMap.get(player).put("hand", hand);
        p1.notifyPointChange(pointsMap);
        p2.notifyPointChange(pointsMap);

    }
    private void incrementHandPoints(Player player){
        updatePointsMap(this.pointsMap.get(player).get("game") + 5, this.pointsMap.get(player).get("hand") + 5, player);
    }
    private void resetHandPoints(Player player){
        updatePointsMap(this.pointsMap.get(player).get("game"), 0, player);
    }

    public static void main(String[] args) throws ExecutionException, InterruptedException {
        var g = new Game();
        g.run();
    }
    boolean noOneHasWon(){
        return !(pointsMap.get(p1).get("game") >= 45 || pointsMap.get(p2).get("game") >= 45);
    }

    /** i need to choose where the dealer state lives. currently (march 11) there are variables and methods in the player class that also track this state*/
    void swapDealer(){
        if (dealer.equals(p1)){
            dealer = p2;
            leader = p1;
            follower = p2;
        } else {
            dealer = p1;
            leader = p2;
            follower = p1;
        }
    }
    void swapLeader(){
        if(leader.equals(p1)){
            leader = p2;
            follower = p1;
        } else {
            leader = p1;
            follower = p2;
        }
    }

    private Optional<Player> getPlayerWithBestTrump(Player player1, Player player2, Suit trump){
        Predicate<Card> hasTrumps = (card -> (card.getSuit().equals(trump) ||
                (card.getValue() ==1 && card.getSuit().equals(Suit.HEARTS))));
        Comparator<Card> betterTrump = (c1, c2)-> {
            if(trumpPrecedence(c1) != trumpPrecedence(c2)){
                return trumpPrecedence(c1) - trumpPrecedence(c2);
            } else {
                return hrlb(c1, c2)? 1:-1;
            }
        };

        var bestTrumpHand1 = player1.getHand()
                .stream()
                .filter(hasTrumps)
                .max(betterTrump);

        var bestTrumpHand2 = player2.getHand()
                .stream()
                .filter(hasTrumps)
                .max(betterTrump);

        if(bestTrumpHand1.isPresent() && bestTrumpHand2.isPresent()){
            return Optional.of((betterTrump.compare(bestTrumpHand1.get(), bestTrumpHand2.get()) > 0)?player1:player2);
        } else if(bestTrumpHand1.isPresent() || bestTrumpHand2.isPresent()){
            if(bestTrumpHand1.isPresent()){
                return Optional.of(player1);
            }else{
                return Optional.of(player2);
            }
        } else {
            return Optional.empty();
        }

    }

    static boolean leaderBeats(Card l, Card f, Suit trump){
        if(l.isTrump(trump) && f.isTrump(trump)){
            return (trumpPrecedence(l) == trumpPrecedence(f))? hrlb(l,f):trumpPrecedence(l) > trumpPrecedence(f);
        } else if(l.isTrump(trump) || f.isTrump(trump)){
            return l.isTrump(trump);
        } else if(l.getSuit() == f.getSuit()){
            return hrlb(l,f);
        } else {
            return true;
        }
    }

    /**
     *
     * @return and integer between -1 and 3 (inclusive) indicating the precedence of the trump (5 of trump -> 3)
     *
     * */
    static int trumpPrecedence(Card c) {
        return switch (c.getValue()) {
            case 1 -> (c.getSuit() == Suit.HEARTS) ? 1 : 0;
            case 11 -> 2;
            case 5 -> 3;
            default -> -1;
        };

    }

    /**
     * hrlb -> Highest in Red Lowest in Black
     * @return true if c1 beats c2 by the rules of Highest in Red, Lowest in Black
     *
     * @throws IllegalArgumentException when the suits of c1 and c2 do not match
     *
     * */

    private static boolean hrlb(Card c1, Card c2){
        if(c1.getSuit() != c2.getSuit()){
            throw new IllegalArgumentException();
        }
        if(c1.getSuit() == Suit.HEARTS || c1.getSuit() == Suit.DIAMONDS){
            return c1.getValue() > c2.getValue();
        } else if(c1.getValue() <= 10 && c2.getValue() <= 10){
            return  c1.getValue() < c2.getValue();
        } else{
            return  c1.getValue() > c2.getValue();
        }
    }

    Card getTopCard() {
        return topCard;
    }

    public String getOpponentName(Player player) {
        return ((player == p1) ? p2:p1).getName();
    }

    public void notifyPlayerDisconnect() {
        if(p1.isDisconnected() && p2.isDisconnected()){
            gameTerminated();
        }
    }

    public void registerTerminationListener(Alertable a){
        terminationListeners.add(a);
    }

    public void gameTerminated(){
        for (Alertable a :
                terminationListeners) {
            a.alert();
        }
    }

    public interface Alertable{
        void alert();
    }
}
