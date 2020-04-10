document.getElementById("creationForm").addEventListener("submit", async (e) => {
    e.preventDefault();
    let form = e.target;
    let lobbycode = fetch(form.action, {
        credentials: "same-origin",
        method:form.method,
        body: new URLSearchParams(new FormData(form))
    }).then(async (res)=> await res.json()).then(obj => {
            lc = obj.lobbyCode
            outputElement = document.getElementById("lobbyIDOutput")
            let oldelems = outputElement.getElementsByTagName("span")

            for(e of oldelems){
                e.remove()
                
            }
            let txt = document.createElement("span")
            let label = document.createElement("p")
            label.textContent = "Game Lobby Code:"
            txt.textContent = lc
            outputElement.append(label)
            outputElement.append(txt)
            outputElement.classList.remove("hidden")
            window.open("/game")
            }
        )
    console.log(lobbycode)
    // console.log(JSON.parse(txt).lobbyCode)
})

document.getElementById("joiningForm").addEventListener("submit", async (e) => {
    e.preventDefault();
    let form = e.target;
    let lobbycode = fetch(form.action, {
        credentials: "same-origin",
        method:form.method,
        body: new URLSearchParams(new FormData(form))
    }).then(async (res)=> await res.json()).then(obj => {
            window.open("/game")
            }
        )
    
    console.log(lobbycode)
    // console.log(JSON.parse(txt).lobbyCode)
})