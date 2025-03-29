
let mouseX = 0;
let mouseY = 0;
let isMoving = false;

// Select all cubes
const cubes = document.querySelectorAll(".cube");

// Update mouse coordinates
document.addEventListener("mousemove", (event) => {
    mouseX = (event.clientX / window.innerWidth - 0.5) * 100;
    mouseY = (event.clientY / window.innerHeight - 0.5) * 100;
    isMoving = true;
});

// Function to move cubes smoothly
function moveCubes() {
    if (isMoving) {
        cubes.forEach((cube, index) => {
            let rotationX = 45 + mouseY * (index + 1) * 0.1;
            let rotationY = 45 + mouseX * (index + 1) * 0.1;

            cube.style.transform = `rotateX(${rotationX}deg) rotateY(${rotationY}deg) translateY(-10px)`;
        });
    }

    requestAnimationFrame(moveCubes);
}

// Start animation loop
moveCubes();

const userEmail = sessionStorage.getItem("email");
if (!userEmail) {
 window.location.href = "index.html"; // Redirect to login if not logged in
}

const API_KEYS = [
   "search_api"
];

let currentKeyIndex = 0;


function getNextApiKey() {
    currentKeyIndex = (currentKeyIndex + 1) % API_KEYS.length;
    return API_KEYS[currentKeyIndex];
}



async function fetchSearchResults() {
    const query = "Latest News India";  
    let apiKey = API_KEYS[currentKeyIndex];
    let apiUrl = `https://www.googleapis.com/customsearch/v1?key=${apiKey}&cx=d1c669fd4b5a441fd&q=${encodeURIComponent(query)}`;
    
    try {
        const response = await fetch(apiUrl);
        const jsonResponse = await response.json();

        if (jsonResponse.error && jsonResponse.error.code === 429) {
            console.warn("🔄 Quota exceeded, switching API key...");
            apiKey = getNextApiKey();
            apiUrl = `https://www.googleapis.com/customsearch/v1?key=${apiKey}&cx=d1c669fd4b5a441fd&q=${encodeURIComponent(query)}`;
            return fetchSearchResults(); 
        }

        console.log("API Response:", jsonResponse);

        if (!jsonResponse.items || jsonResponse.items.length === 0) {
            document.getElementById("results-container").innerHTML = `<p>No results found.</p>`;
            return fetchSearchResults();
        }

        const resultsContainer = document.getElementById("results-container");
        resultsContainer.innerHTML = "";
        jsonResponse.items.slice(0, 4).forEach(item => {
            resultsContainer.innerHTML += `
                <div class="result">
                    <h2><a href="${item.link}" target="_blank">${item.title}</a></h2>
                    <p>${item.snippet}</p>
                </div>
            `;
        });
          
             const showMoreButton = document.createElement("button");
             showMoreButton.innerText = "Show More";
             showMoreButton.classList.add("show-more-btn");
             showMoreButton.onclick = function() {
                 window.location.href = "morenews.html"; 
             };
     
             
             const buttonContainer = document.createElement("div");
             buttonContainer.classList.add("button-container");
             buttonContainer.appendChild(showMoreButton);
             resultsContainer.appendChild(buttonContainer);
     

    } catch (error) {
        console.error("❌ Error fetching search results:", error);
    }
}




async function fetchScamResults() {
    const query = "Latest Top Scams";  
    let apiKey = API_KEYS[currentKeyIndex];
    let apiUrl = `https://www.googleapis.com/customsearch/v1?key=${apiKey}&cx=d1c669fd4b5a441fd&q=${encodeURIComponent(query)}`;
    
    try {
        const response = await fetch(apiUrl);
        const jsonResponse = await response.json();

        if (jsonResponse.error && jsonResponse.error.code === 429) {
            console.warn("🔄 Quota exceeded, switching API key...");
            apiKey = getNextApiKey();
            apiUrl = `https://www.googleapis.com/customsearch/v1?key=${apiKey}&cx=d1c669fd4b5a441fd&q=${encodeURIComponent(query)}`;
            return fetchScamResults(); // Retry with new key
        }

        console.log("API Response:", jsonResponse);

        if (!jsonResponse.items || jsonResponse.items.length === 0) {
            document.getElementById("resultscontainers").innerHTML = `<p>No results found.</p>`;
            return;
        }

        const resultsContainer = document.getElementById("resultscontainers");
        resultsContainer.innerHTML = "";
        jsonResponse.items.slice(0, 4).forEach(item => {
            resultsContainer.innerHTML += `
                <div class="result">
                    <h2><a href="${item.link}" target="_blank">${item.title}</a></h2>
                    <p>${item.snippet}</p>
                </div>
            `;
        });
           
             const showMoreButton = document.createElement("button");
             showMoreButton.innerText = "Show More";
             showMoreButton.classList.add("show-more-btn");
             showMoreButton.onclick = function() {
                 window.location.href = "morescam.html";
             };
     
             const buttonContainer = document.createElement("div");
             buttonContainer.classList.add("button-container");
             buttonContainer.appendChild(showMoreButton);
             resultsContainer.appendChild(buttonContainer);
     

    } catch (error) {
        console.error("❌ Error fetching search results:", error);
    }
}


window.onload = function () {
    fetchSearchResults();
    fetchScamResults();
};


const mainBtn = document.getElementById('mainBtn');
const menuContainer = document.getElementById('menuContainer');
const menuIcon = document.getElementById('menuIcon');

mainBtn.addEventListener('click', () => {
  menuContainer.classList.toggle('open');
  
  // Toggle icon between menu (closed) and menu_open (open)
  if (menuContainer.classList.contains('open')) {
    menuIcon.innerHTML = "&#xE9BD;";
  } else {
    menuIcon.innerHTML = "&#xE5D2;";
  }
});


function logout() {
    if (confirm("Are you sure you want to log out?")) {
        sessionStorage.clear();
        localStorage.clear();
        window.location.href = "index.html";
    }
}

document.getElementById("logoutBtn").addEventListener("click", logout);
document.getElementById("collapsible-btn btn5").addEventListener("click", logout);