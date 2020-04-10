package gui;
import gameLogic.*;
import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.util.Arrays;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.function.Consumer;

public class UserView extends JFrame implements MouseListener, PlayerView {
    private final int CARD_HEIGHT = 180;
    private Player player;
    private JPanel hand;
    private Set<CardPanel> cards;
    private JPanel playZone;
    private JPanel deck;
    private SideBar rightSidebar;
    private SideBar leftSideBar;
    private CardPanel trump;
    private JLabel opponentMatchPoints;
    private JLabel opponentHandPoints;
    private JLabel matchPoints;
    private JLabel handPoints;

    private CompletableFuture<Card> cardSelection;
    private CompletableFuture<Optional<Card>> rob;
    private boolean robRequested;


    public UserView(Player p){
        player = p;
        player.registerPlayerView(this);
        cards = ConcurrentHashMap.newKeySet();
//        cards = new HashSet<>();
        initHand();
        initRightSideBar();
        initLeftSideBar();
        initPlayZone();
        this.setLayout(new BorderLayout());
        this.add(playZone);
        this.add(rightSidebar, BorderLayout.EAST);
        this.add(leftSideBar, BorderLayout.WEST);
        this.add(hand,BorderLayout.SOUTH);
        this.setSize(new Dimension(900, 500));
        this.setDefaultCloseOperation(EXIT_ON_CLOSE);

        updateCardList();
        this.setVisible(true);
    }

    /* gui set-up*/
    private void initPlayZone() {
        this.playZone = new JPanel(new GridBagLayout());
        playZone.setBackground(Color.cyan);
    }
    private void initLeftSideBar() {
        this.leftSideBar = new SideBar();
        leftSideBar.setLayout(new GridLayout(2, 1));
        leftSideBar.setBackground(Color.ORANGE);
        leftSideBar.setPreferredSize(new Dimension(100, 400));

        var opponentPoints = new JPanel(new GridLayout(3, 1));
        var name = new JLabel("Opponent");
        name.setFont(new Font(name.getFont().getName(), Font.BOLD, 27));
        opponentMatchPoints = new JLabel("Match Points: 0");
        opponentHandPoints = new JLabel("lift Points: 0");
        opponentPoints.add(name);
        opponentPoints.add(opponentMatchPoints);
        opponentPoints.add(opponentHandPoints);
        opponentPoints.setBorder(BorderFactory.createLineBorder(Color.black, 3));
        Arrays
                .stream(opponentPoints.getComponents())
                .forEach(component ->
                        ((JComponent)component).setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 0)));


        var points = new JPanel(new GridLayout(3, 1));
        name = new JLabel("You");
        name.setFont(new Font(name.getFont().getName(), Font.BOLD, 27));
        matchPoints = new JLabel("Match Points: 0");
        handPoints = new JLabel("lift Points: 0");
        points.add(name);
        points.add(matchPoints);
        points.add(handPoints);
        points.setBorder(BorderFactory.createLineBorder(Color.black, 3));

        Arrays
                .stream(points.getComponents())
                .forEach(component ->
                        ((JComponent)component).setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 0)));

        leftSideBar.add(opponentPoints);
        leftSideBar.add(points);
    }
    private void initRightSideBar() {
        this.rightSidebar = new SideBar();
        rightSidebar.setLayout(new BorderLayout());
        rightSidebar.setBackground(Color.BLUE);
        rightSidebar.setPreferredSize(new Dimension(CARD_HEIGHT, 400));

        this.deck = new JPanel();
        deck.setOpaque(false);
        deck.setLayout( new GridBagLayout());
        rightSidebar.add(deck);
    }
    private void initHand(){
        this.hand = new JPanel();
        hand.setBackground(Color.RED);
        hand.setPreferredSize(new Dimension(400, CARD_HEIGHT));
    }

    /**
     * updateCardList syncs this object's hand state with the gui representation of its hand state
     *
     * this method feels very convoluted,
     * but it is mutating the set in place to avoid race conditions in the gui
     */
    private void updateCardList(){
        var cardsInHand = player.getHand();
        //add new Cards
        cardsInHand.stream()
                .filter(c -> cards.stream()
                                .map(CardPanel::getCard)
                                .noneMatch(r-> r.equals(c)))
                .forEach(c-> cards.add(new CardPanel(c, CARD_HEIGHT, false, this)));

        //remove old cards
        cards.removeIf(c-> !cardsInHand.contains(c.getCard()));

        //sync gameLogic.Card list with hand panel
        var components = hand.getComponents();
        for(int i = 0 ; i < hand.getComponentCount(); i++){
            if((!cards.contains((components[i])))){
                hand.remove(components[i]);
            }
        }
        cards.forEach(c-> {
            if(!Arrays.asList(hand.getComponents()).contains(c)){
                hand.add(c);
            }
        });
    hand.revalidate();
    }
    /* Public Interface*/

    @Override
    public void setTrump(Card c){
        if (c == null && trump != null) {
            trump = new SuitPanel(trump.getCard(), this);
        }else if(c != null) {
            trump = new CardPanel(c, CARD_HEIGHT, false, this);
        } else {
            throw new IllegalStateException("Inorder to set the trump to null, there must be a previous trump to infer trump suit");
        }
        Arrays.stream(deck.getComponents())
                .filter(comp -> comp instanceof CardPanel)
                .findAny().ifPresent(comp-> deck.remove(comp));
        deck.add(trump);
        deck.revalidate();
        deck.repaint();
    }
    @Override
    public CompletableFuture<Card> getCard(){
        //visual code too
        highlightCardsInContainer(hand);
        cardSelection = new CompletableFuture<>();
        return cardSelection;

    }
    @Override
    public CompletableFuture<Optional<Card>> getRob() throws ExecutionException, InterruptedException {
        this.robRequested = true;
        rob = new CompletableFuture<>();
        if(askForRob().get()){
            highlightCardsInContainer(hand);
        } else{
            robChosen(null);
        }


        return rob;

    }
    @Override
    public void addToPlayZone(Card c){
        playZone.add(new CardPanel(c, CARD_HEIGHT, false, null));
        playZone.revalidate();
        playZone.repaint();
    }
    @Override
    public void resetPlayZone(){
        playZone.removeAll();
        playZone.revalidate();
        playZone.repaint();
    }
    @Override
    public void setOpponentMatchPoints(int points){
        setPoints(points, opponentMatchPoints, false);
    }
    @Override
    public void setOpponentHandPoints(int points){
        setPoints(points, opponentHandPoints, true);
    }
    @Override
    public void setMatchPoints(int points){
        setPoints(points, matchPoints, false);
    }
    @Override
    public void setHandPoints(int points){
        setPoints(points, handPoints, true);
    }

    private void setPoints(int points, JLabel label, boolean lift){
        String pointType = (lift) ? "Lift": "Match";
        label.setText(String.format("%s Points: %d", pointType, points));
    }
    private CompletableFuture<Boolean> askForRob() {
        //ask user if they want to rob
        highlightCard(trump);
        var retval = new CompletableFuture<Boolean>();
        JPanel questionBox = new JPanel(new GridLayout(2,1));
        JPanel yesNoBox = new JPanel(new GridLayout(1,2));
        JButton yes = new JButton("Yes");
        JButton no = new JButton(("No"));
        yesNoBox.add(yes);
        yesNoBox.add(no);
        JTextArea question = new JTextArea("Would you like to rob trump?");
        question.setLineWrap(true);
        question.setEditable(false);
        questionBox.add(question);
        questionBox.add(yesNoBox);
        questionBox.setPreferredSize(new Dimension(150, 80));
        rightSidebar.add(questionBox, BorderLayout.SOUTH);
        rightSidebar.revalidate();

        yes.addActionListener(e-> {
            retval.complete((true));
            rightSidebar.remove(questionBox);
            unhighlightCard(trump);
        });
        no.addActionListener(e ->  {
            retval.complete((false));
            rightSidebar.remove(questionBox);
            unhighlightCard(trump);
        });
        return retval;
    }

    /* gui-related helper methods*/
    private void changeHighlightStatusInContainer(Container c ,Consumer<CardPanel> consumer){
        Arrays.stream(c.getComponents())
                .filter(comp-> comp instanceof CardPanel)
                .map(card-> (CardPanel)card)
                .forEach(consumer);
    }
    private void highlightCardsInContainer(Container c) {
        changeHighlightStatusInContainer(c, this::highlightCard);
    }
    private void unhighlightCardsInContainer(Container c) {
        changeHighlightStatusInContainer(c, this::unhighlightCard);
    }
    private void highlightCard(CardPanel cardPanel){
        if(robRequested || player.getEligiblePlays().contains(cardPanel.getCard())){
            cardPanel.highllight();
        }
    }
    private void unhighlightCard(CardPanel cardPanel){
        cardPanel.unHighlight();
    }


    /** this method is called bt the player to let the userview know that it's internal state has changed*/
    @Override
    public void notifyHandChange(){
        updateCardList();
        repaint();
    }

    @Override
    public CompletableFuture<Boolean> promptRematch(String winner) {
        return CompletableFuture.completedFuture(false);
    }

    @Override
    public void reset() {
        resetPlayZone();
    }

    /*Completing futures*/
    private void CardSelected(Card c){
        if (cardSelection != null) {
            addToPlayZone(c);
            cardSelection.complete(c);
            cardSelection = null;
            unhighlightCardsInContainer(hand);
            updateCardList();
        }
    }
    private void robChosen(Card c){
        if (c == null) {
                rob.complete(Optional.empty());

        } else {
            rob.complete(Optional.of(c));
        unhighlightCardsInContainer(hand);
            updateCardList();
            ;
        setTrump(null);
        }
         this.robRequested = false;
    }

    @Override
    public void mouseClicked(MouseEvent e) {
        if(e.getClickCount() == 2 && e.getSource() instanceof CardPanel && ((CardPanel) e.getSource()).isHighlighted()){
            var source = (CardPanel) e.getSource();
            if(robRequested){
                if(source.getParent().equals(hand)){
                    robChosen(source.getCard());
                } else {

                }
            }else {

            var card = source.getCard();
            CardSelected(card);

            }
        }
    }

    @Override
    public void mousePressed(MouseEvent e) {

    }

    @Override
    public void mouseReleased(MouseEvent e) {

    }

    @Override
    public void mouseEntered(MouseEvent e) {

    }

    @Override
    public void mouseExited(MouseEvent e) {

    }
}
