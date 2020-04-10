
class Message{
    constructor(method, data){
        this.method = method;
        this.data = data;
    }

    toString(){
        return `{method:${this.method}, data:${this.data}}`;
    }
}
class Card{
    constructor(str){
        if(typeof(str) == "string"){

            if(str.length > 1){
                let val = "";
                if(str.length == 2){
                    val = str.substring(0, 1);
                    this.suit = str.substring(1);
                } else{
                    val = str.substring(0,2);
                    this.suit = str.substring(2);
                }
                switch(val){
                    case "K": 
                       { this.value = 13;
                        break;}

                    case "Q": 
                        {this.value = 12;
                        break;}

                    case "J": 
                        {this.value = 11;
                        break;}

                    case "A": 
                        {this.value = 1;
                        break;}
                    default: this.value = parseInt(val);


                }
                this.element = null;
            } else {
                this.value =null;
                this.suit = str;
            }
        }
    }
    getElement(){

        if(this.element != null){
            return this.element;
        }
        let e = document.createElement("img");

        let altText = "";
        let imgSrc = "";

        if(this.value != null){
                switch(this.value){
                case  13:
                    {altText += "king";
                    break;}
                case  12:
                    {altText += "queen";
                    break;}
                case  11:
                   { altText += "jack";
                    break;}
                case 2:
                   { altText += "deuce";
                    break;}
                case 1:
                    {altText += "ace";
                    break;}
                default:
                    {altText += this.value;
                    break;}
            } 

            imgSrc = "";
            switch(this.value){
                case 13 : 
                    {imgSrc += "K";
                    break;}
                case 12 : 
                    {imgSrc += "Q";
                    break;}
                case 11 : 
                    {imgSrc += "J";
                    break;}
                case 1 : 
                    {imgSrc += "A";
                    break;}
                default: imgSrc+= this.value ;
            }

            switch(this.suit){
                case  "S":
                    {imgSrc += "S";
                    altText += " of spades";
                    break;}
                case  "C":
                    {imgSrc += "C";
                    altText += " of clubs";
                    break;}
                case  "H":
                    {imgSrc += "H";
                    altText += " of hearts";
                    break;}
                case  "D":
                    {imgSrc += "D";
                    altText += " of diamonds";
                    break;}
            }

            
        } else {
            switch(this.suit){
                case  "S":
                    {imgSrc = "spade";
                    altText = " Spades";
                    break;}
                case  "C":
                    {imgSrc = "club";
                    altText = " Clubs";
                    break;}
                case  "H":
                    {imgSrc = "heart";
                    altText = " Hearts";
                    break;}
                case  "D":
                    {imgSrc = "diamond";
                    altText = " Diamonds";
                    break;}
            }
        }
        e.src = `./IMAGES/${imgSrc}.png`;
        e.alt = altText;
        e.id = this.value + this.suit;
        e.classList.add("inactive");


        this.element = e;

        return e;
    }
    toString(){
        let str= "";
        switch(this.value){
            case 13 : 
                {str += "K";
                break;}
            case 12 : 
                {str += "Q";
                break;}
            case 11 : 
                {str += "J";
                break;}
            case 1 : 
               { str += "A";
                break;}
            default: str += this.value ;
        }
        str += this.suit;
        return str;
    }
    equals(otherCard){
        if(otherCard instanceof Card){
            return this.value == otherCard.value && this.suit == otherCard.suit;
        } else {
            return false;
        }
    }
}

let game = {
    awaitingPlaysRefresh: false,
    hand: [],
    eligblePlays : [],
    playzone :[],
    opponent: {name: "",
        matchPoints: 0,
        handPoints: 0
    },
    player: {
        name : "",
        matchPoints : 0,
        handPoints : 0
    },
    trump : {},
    

};

let ws = new WebSocket("ws://24.224.183.243:9000");

ws.addEventListener("message", (e)=>{
let msg = JSON.parse(e.data);
console.log(msg);
switch(msg.method){
    case "setTrump": {
        let card = null;
        if(msg.data != null){
            card = new Card(msg.data);
        }
        setTrump(card);
        break;
    }
    case "getName" : {
        document
        .getElementById("you")
        .getElementsByTagName("span")[0]
        .innerHTML = msg.data;
        break;
    }
    case "getOpponentName" : {
        document
        .getElementById("opponent")
        .getElementsByTagName("span")[0]
        .innerHTML = msg.data;
        break;
    }
    case "getRob": {
        awaitRob();
        break;
    }
    case "getCard" : {
        awaitCard();
        break;
    }
    case "getHand" : {
        let newcards = [];
        msg.data.forEach((e) => newcards.push(new Card(e)));

        let cardsToBekept = game.hand.filter((card)=> newcards.some((newCard) => newCard.equals(card)));
        let cardsToBeAdded = newcards.filter((card)=> !game.hand.some((oldCard) => oldCard.equals(card)));
        game.hand= cardsToBekept.concat(cardsToBeAdded);



        syncHandState();
        break;
    }
    case "getEligiblePlays" : {

        let newcards = [];
        msg.data.forEach((e) => newcards.push(new Card(e)));
        let arr = game.hand.filter((cardInHand) => newcards.some((newCard) => newCard.equals(cardInHand)));
        console.log(arr);
        game.eligblePlays = arr;
        syncEligiblePlaysState();
        break;
    }
    case "notifyHandChange": {
        getHand();
        break;
    }
    case "setOpponentMatchPoints" : {
        updateOpponentMatchPoints(msg.data);
        break;
    }
    case "setOpponentHandPoints" : {
        updateOpponentHandPoints(msg.data);
        break;
    }
    case "setMatchPoints" : {
        updateMatchPoints(msg.data);
        break;
    }
    case "setHandPoints" : {
        updatetHandPoints(msg.data);
        break;
    }
    case "addToPlayzone" : {
        addToPlayzone(new Card(msg.data));
        break;
    }
    case "resetPlayzone" : {
        resetPlayzone();
        break;
    }
    case "promptRematch" : {

        askQuestion(`The match has concluded with ${msg.data} comming out victorious. would you like to rematch?`,
        ()=> ws.send(new Message("promptRematch", true), 
        ()=> ws.send(new Message("promptRematch", false))))
        break;  
    }

}
    console.log(e.data);
});


ws.addEventListener("open", (e)=>{
    getName();
    getOpponentName();
});




function getName(){
    ws.send(new Message("getName"));
}
function getOpponentName(){
    ws.send(new Message("getOpponentName"));
}
function getHand(){
    
    ws.send(JSON.stringify(new Message("getHand")));
}

function awaitCard(){
    console.log("starting waiting");
    ws.send(new Message("getEligiblePlays"));
    console.log("end waiting");
}

function awaitRob(){
            
            let yesFunc = ()=> {
                let cb = (event) => {
                sendRob(game.hand.filter((v)=> v.getElement() === event.target)[0]);
                game.hand.forEach(card => {
                    card.getElement().removeEventListener("click", cb);
                    card.getElement().classList.remove("clickable");
                    card.getElement().classList.add("inactive");
                });
                }
            
                for(const card of game.hand){
                    card.getElement().addEventListener("click", cb);
                    card.getElement().classList.add("clickable");
                    card.getElement().classList.remove("inactive");
                }
            };
            let noFunc = () => sendRob(null);
            askQuestion("would you like to rob?", yesFunc, noFunc);

    
}

function askQuestion(questionText, yesFunc, noFunc){
    let qt = document.getElementById("questionText");
    qt.innerHTML = questionText;

    let qb = document.getElementById("questionBox");
    qb.classList.remove("hidden");

    document.getElementById("yes").addEventListener("click", () => {
        yesFunc();
        qb.classList.add("hidden")
    });

    document.getElementById("no").addEventListener("click", () => {
        noFunc();
        qb.classList.add("hidden")}
        );


}

function sendCard(card){
    let msg = new Message("getCard",card.toString());
    ws.send(JSON.stringify(msg));
    ws.send(JSON.stringify(new Message("getHand")));
    game.eligblePlays = [];
    syncEligiblePlaysState();
    game.hand.forEach(c => markInactive(c))
    
}

function sendRob(card){
    let msg = new Message("getRob",(card != null) ? card.toString() : null);
    ws.send(JSON.stringify(msg));
    ws.send(JSON.stringify(new Message("getHand")));
}

function addToPlayzone(card){
    if(card instanceof Card){
        game.playzone.push(card);
        document.getElementById("playzone").appendChild(card.getElement());
    
    }
}

function resetPlayzone(){
    setTimeout( () => {
        for (const card of game.playzone) {
            document.getElementById("playzone").removeChild(card.getElement());
        }
        game.playzone = [];
    }, 1000 * parseFloat(document.getElementById("rspzTime").value))
        
    
}
function setTrump(card){
    if(card instanceof Card){
        if(game.trump instanceof Card){
            document.getElementById("deck").removeChild(game.trump.getElement());
        }
        document.getElementById("deck").appendChild(card.getElement());
        game.trump = card;
    } else if(card == null) {
        if(game.trump instanceof Card){
            document.getElementById("deck").removeChild(game.trump.getElement());
        }
        card = new Card(game.trump.suit);
        document.getElementById("deck").appendChild(card.getElement());
        game.trump = card;
        game.trump.getElement().classList.remove("inactive")
    }
}

function updateOpponentMatchPoints(points){
    document.getElementById("opponent").getElementsByClassName("match")[0].innerHTML = `Match: ${points}`;
}

function updateOpponentHandPoints(points){
    document.getElementById("opponent").getElementsByClassName("handPoints")[0].innerHTML = `Hand: ${points}`;
}

function updateMatchPoints(points){

    document.getElementById("you").getElementsByClassName("match")[0].innerHTML = `Match: ${points}`;
}

function updatetHandPoints(points){
    document.getElementById("you").getElementsByClassName("handPoints")[0].innerHTML  = `Hand: ${points}`;
}

function syncEligiblePlaysState(){
    let htmlCards = document.getElementById("hand").getElementsByTagName("img");
    for(const card of htmlCards){
        if(game.eligblePlays.filter(c=> c.getElement() === card).length === 0){
            card.classList.add("inactive");
            card.classList.remove("clickable");
        } else {
            card.classList.remove("inactive");
            card.classList.add("clickable");
        }
    }
    let cb = (event) => {
        if(event.target.classList.contains("clickable")){
            sendCard(game.hand.filter((v)=> v.getElement() === event.target)[0]);
            game.hand.forEach(card => card.getElement().removeEventListener("click", cb));
        }
    };
    for(const card of game.hand){
        card.getElement().addEventListener("click", cb);
    }
}

function markInactive(card){
    card.getElement().classList.add("inactive");
    card.getElement().remove("clickable");
}

function syncHandState(){
    let htmlHand = document.getElementById("hand");
    let htmlHandCards = htmlHand.getElementsByTagName("img");
    for (const htmlCard of htmlHandCards) {
        if(game.hand.filter(c=> c.getElement() === htmlCard).length === 0){
            htmlHand.removeChild(htmlCard);
        }
        
    }
    for (const card of game.hand) {
        let htmlHand = document.getElementById("hand");
        let htmlHandCards = htmlHand.getElementsByTagName("img");
        let append = true;
        for (const htmlCard of htmlHandCards) {
            if(card.getElement() == htmlCard){
                append = false;
            }
            
        }
        if(append){
            htmlHand.appendChild(card.getElement());
        }
        
    }
}
