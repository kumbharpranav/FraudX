document.addEventListener("DOMContentLoaded", function() {
  let dateTimelineShown = false;
  
 const firebaseConfig = {
   //firebase credentials
};


  firebase.initializeApp(firebaseConfig);
  const db = firebase.database();


  const userEmail = sessionStorage.getItem("email");
  if (!userEmail) {
    window.location.href = "index.html"; 
  }
  const userId = userEmail.replace(/[^a-zA-Z0-9]/g, "_");

  const userRef = db.ref("scam/" + userId);

 
  userRef.get().then(snapshot => {
    if (snapshot.exists()) {
      const userData = snapshot.val();
    
      const usernameEl = document.getElementById("username");
      const emailEl = document.getElementById("email");
      const nameEl = document.getElementById("name");
      const errorEl = document.getElementById("error-message");

     
      if (usernameEl) usernameEl.value = userData.username || "";
      if (emailEl) emailEl.value = userData.email || "";
      if (nameEl) nameEl.value = userData.name || "";
    } else {
      const errorEl = document.getElementById("error-message");
      if (errorEl) {
        errorEl.textContent = "User not found!";
        errorEl.style.display = "block";
      }
    }
  }).catch(error => {
    console.error("Error fetching user data:", error);
  });

  
  const messagesRef = userRef.child('messages');
messagesRef.orderByChild("timestamp").once('value').then(snapshot => {
    const dateTimelineShown = false; 

    if (snapshot.exists()) {
      snapshot.forEach(childSnap => {
        const messageData = childSnap.val();
        displayMessage(messageData.text, messageData.isBot, messageData.status);

      });
    }
  });

  function handleKeyDown(e) {
    if (e.key === 'Enter' && !e.shiftKey) {
      e.preventDefault();
      sendMessage();
    }
  }
  window.handleKeyDown = handleKeyDown;  
  function handleInput(e) {
    const arrowBtn = document.getElementById("arrowBtn");
    if (e.target.value.length > 0) {
      arrowBtn.style.display = "flex";
    } else {
      arrowBtn.style.display = "none";
    }
  }
  window.handleInput = handleInput;
  
  async function sendMessage() {
    const queryEl = document.getElementById('query');
    const messageText = queryEl.value.trim();
    if (!messageText) {
      alert('Please enter a prompt.');
      return;
    }
    
    const chatContainer = document.getElementById('chat-container');
    if (!dateTimelineShown) {
      const chatContainer = document.getElementById("chat-container");
      const dateLabel = document.createElement("div");
      dateLabel.classList.add("date-timeline");
      dateLabel.textContent = "Today"; 
      dateTimelineShown = true;
    }

    displayMessage(messageText, false);
    storeMessage(messageText, false);
    
    queryEl.value = '';
    if (document.getElementById("color-box")) {
      document.getElementById("color-box").style.display = "none";
    }
    if (document.getElementById("arrowBtn")) {
      document.getElementById("arrowBtn").style.display = "none";
    }
    
    const loadingDiv = document.createElement('div');
    loadingDiv.className = 'message received loading';
    loadingDiv.innerHTML = '<span></span><span></span><span></span>';
    chatContainer.appendChild(loadingDiv);
    chatContainer.scrollTop = chatContainer.scrollHeight;
    
    const geminiUrl = 'https://www.fakedetector.run.place/gemini?messageText=' + encodeURIComponent(messageText);
    try {
      const response = await fetch(geminiUrl);
      if (!response.ok) {
        throw new Error('HTTP error! Status: ' + response.status);
      }
      const data = await response.json(); 
      chatContainer.removeChild(loadingDiv);
      
      const output = data.result.trim();
      const words = output.split(/\s+/);
      const firstWord = words[0].toLowerCase();
      const remainingText = words.slice(1).join(' ');
      
      let status = '';
      if (firstWord === 'scam' || firstWord === 'scam,' || firstWord === 'scam.') {
        status = 'scam';
      } else if (firstWord === 'safe' || firstWord === 'safe,' || firstWord === 'safe:') {
        status = 'safe';
      }
      displayMessage(remainingText, true, status);
      storeMessage(remainingText, true, status);
    } catch (error) {
      chatContainer.removeChild(loadingDiv);
      displayMessage("Error: " + error.message, true);
    }
  }
  
function displayMessage(text, isBot, status = '', timestamp = '') {

    const chatContainer = document.getElementById('chat-container');
    const messageDiv = document.createElement('div');
    messageDiv.className = `message ${isBot ? 'received' : 'sent'}`;
    
    if (isBot && status) {
      const statusDiv = document.createElement('div');
      statusDiv.className = 'status';
      let customStatusText = status.toUpperCase();
      if (status === 'scam') {
        statusDiv.classList.add('scam');
      } else if (status === 'safe') {
        statusDiv.classList.add('safe');
      }
      statusDiv.textContent = customStatusText;
      messageDiv.appendChild(statusDiv);
    }
    
    const textDiv = document.createElement('div');

    textDiv.className = 'bot-text';
    textDiv.textContent = text;
    messageDiv.appendChild(textDiv);
    
    if (isBot && status === 'scam') {
      const reportBtn = document.createElement('button');
      reportBtn.className = 'report-button';
      reportBtn.textContent = 'Report';
      reportBtn.onclick = function() {
        window.location.href = 'https://cybercrime.gov.in/Webform/Accept.aspx';
      };
      messageDiv.appendChild(document.createElement('br'));
      messageDiv.appendChild(reportBtn);
    }
    
    chatContainer.appendChild(messageDiv);
    chatContainer.scrollTop = chatContainer.scrollHeight;
  }
  
  function storeMessage(text, isBot, status = '') {
    const messageData = {
      text: text,
      isBot: isBot,
      status: status,
      timestamp: Date.now()
    };
    userRef.child('messages').push(messageData);
  }
  
  function addDateTimeline() {
    const chatContainer = document.getElementById("chat-container");
    const dateLabel = document.createElement("div");
    dateLabel.classList.add("date-timeline");
    dateLabel.textContent = "Today";
    chatContainer.appendChild(dateLabel);
  }
  
  window.handleKeyDown = handleKeyDown;
  window.handleInput = handleInput;
  window.sendMessage = sendMessage;
});

function toggleDarkMode() {
  const body = document.body;
  const toggleIcon = document.getElementById('toggleIcon');
  if (body.classList.contains('dark-mode')) {
    body.classList.remove('dark-mode');
    toggleIcon.innerHTML = "&#xE51C;";
  } else {
    body.classList.add('dark-mode');
    toggleIcon.innerHTML = "&#xE518;";
  }
}

const mainBtn = document.getElementById('mainBtn');
const menuContainer = document.getElementById('menuContainer');
const menuIcon = document.getElementById('menuIcon');
mainBtn.addEventListener('click', () => {
  menuContainer.classList.toggle('open');
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
