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

document.addEventListener("DOMContentLoaded", function() {
  let dateTimelineShown = false;
  
  // Add logout button event listener here
  const logoutBtn = document.getElementById("btn5");
  if (logoutBtn) {
    logoutBtn.addEventListener("click", logout);
  }

  const firebaseConfig = {
   //firebase config
  };

  // 🔥 Initialize Firebase
  firebase.initializeApp(firebaseConfig);
  const db = firebase.database();

  // Get user email from session storage
  const userEmail = sessionStorage.getItem("email");
  if (!userEmail) {
   window.location.href = "index.html"; // Redirect to login if not logged in
  }

  // Create a userId by replacing non-alphanumeric characters
  const userId = userEmail.replace(/[^a-zA-Z0-9]/g, "_");
  // Update the reference to store data under "fake/" instead of "users/"
  const userRef = db.ref("fake_news/" + userId);

  // Fetch and display user details
  userRef.get().then(snapshot => {
    if (snapshot.exists()) {
      const userData = snapshot.val();
      const usernameEl = document.getElementById("username");
      const emailEl = document.getElementById("email");
      const nameEl = document.getElementById("name");

      // Only set values if elements exist
      if (usernameEl) usernameEl.value = userData.username || "";
      if (emailEl) emailEl.value = userData.email || "";
      if (nameEl) nameEl.value = userData.name || "";
    } else {
      const errorMessage = document.getElementById("error-message");
      if (errorMessage) {
        errorMessage.textContent = "User not found!";
        errorMessage.style.display = "block";
      }
    }
  });

  // Fetch previous messages on page load
  const messagesRef = userRef.child('messages');
  messagesRef.once('value').then(snapshot => {
    if (snapshot.exists()) {
      const messages = snapshot.val();
      const chatContainer = document.getElementById('chat-container');
      for (const messageId in messages) {
        const messageData = messages[messageId];
        displayMessage(messageData.text, messageData.isBot, messageData.status);
      }
    }
  });

  function handleKeyDown(e) {
    if (e.key === 'Enter' && !e.shiftKey) {
      e.preventDefault();
      sendMessage();
    }
  }

  function handleInput(e) {
    const arrowBtn = document.getElementById("arrowBtn");
    if (e.target.value.length > 0) {
      arrowBtn.style.display = "flex";
    } else {
      arrowBtn.style.display = "none";
    }
  }

  async function sendMessage() {
    const queryEl = document.getElementById('query');
    const messageText = queryEl.value.trim();
    if (!messageText) {
      alert('Please enter a prompt.');
      return;
    }

    if (!isValidInput(messageText)) {
      alert("Input appears to be gibberish. Please enter a valid prompt.");
      return;
    }

    const chatContainer = document.getElementById('chat-container');
    
    // Insert date timeline if not shown
    if (!dateTimelineShown) {
      addDateTimeline();
      dateTimelineShown = true;
    }

    // Append user's message (right aligned)
    displayMessage(messageText, false);
    storeMessage(messageText, false);  // Store user message

    // Clear the input and hide extra elements
    queryEl.value = '';
    document.getElementById("color-box").style.display = "none";
    document.getElementById("arrowBtn").style.display = "none";

    // Show loading animation
    const loadingDiv = document.createElement('div');
    loadingDiv.className = 'loading';
    loadingDiv.innerHTML = '<span></span><span></span><span></span>';
    chatContainer.appendChild(loadingDiv);
    chatContainer.scrollTop = chatContainer.scrollHeight;

    // Build the Gemini URL
    const geminiUrl = 'https://www.fakedetector.run.place/service1/gemini?messageText=' + encodeURIComponent(messageText);

    try {
      const response = await fetch(geminiUrl);
      if (!response.ok) {
        throw new Error('HTTP error! Status: ' + response.status);
      }
      const data = await response.json(); // Expecting JSON with a "result" field

      chatContainer.removeChild(loadingDiv);

      const output = data.result.trim();
      const words = output.split(/\s+/);
      const firstWord = words[0].toLowerCase();
      const remainingText = words.slice(1).join(' ');

      // Display bot response with status if applicable
      displayMessage(remainingText, true, firstWord);
      storeMessage(remainingText, true, firstWord); // Store bot response along with status
    } catch (error) {
      chatContainer.removeChild(loadingDiv);
      const errorMsg = document.createElement('div');
      errorMsg.className = 'message received';
      errorMsg.textContent = 'Error: ' + error.message;
      chatContainer.appendChild(errorMsg);
      chatContainer.scrollTop = chatContainer.scrollHeight;
    }
  }

  // Display message in the chat container
  function displayMessage(text, isBot, status = '') {
    const chatContainer = document.getElementById('chat-container');
    const message = document.createElement('div');
    message.className = `message ${isBot ? 'received' : 'sent'}`;
    
    if (isBot && status) {
      const statusDiv = document.createElement('div');
      statusDiv.className = 'status';
      let customStatusText = status;
      if (status === 'fake') {
        customStatusText = "FAKE";
        statusDiv.classList.add('fake');
      } else if (status === 'real') {
        customStatusText = "REAL";
        statusDiv.classList.add('real');
      } else if (status === 'fake.') {
        customStatusText = "FAKE";
        statusDiv.classList.add('fake');
      }
      statusDiv.textContent = customStatusText;
      message.appendChild(statusDiv);
    }
    
    const textDiv = document.createElement('div');
    textDiv.className = 'bot-text';
    textDiv.textContent = text;
    message.appendChild(textDiv);
    
    // Append a report button if scam status is detected
    if (isBot && status === 'scam') {
      const reportBtn = document.createElement('button');
      reportBtn.className = 'report-button';
      reportBtn.textContent = 'Report';
      reportBtn.onclick = function() {
        window.location.href = 'https://cybercrime.gov.in/Webform/Accept.aspx';
      };
      message.appendChild(document.createElement('br'));
      message.appendChild(reportBtn);
    }
    
    chatContainer.appendChild(message);
    chatContainer.scrollTop = chatContainer.scrollHeight;
  }

  // Store message in Firebase
  function storeMessage(text, isBot, status = '') {
    const messageData = {
      text: text,
      isBot: isBot,
      status: status,
      timestamp: Date.now() // Adding timestamp for message order
    };

    const messagesRef = userRef.child('messages');
    messagesRef.push(messageData); // Store message
  }

  // Add date timeline
  function addDateTimeline() {
    const chatContainer = document.getElementById("chat-container");
    const dateLabel = document.createElement("div");
    dateLabel.classList.add("date-timeline");
    dateLabel.textContent = "Today";
    chatContainer.appendChild(dateLabel);
  }

  // Expose functions globally if needed
  window.handleKeyDown = handleKeyDown;
  window.handleInput = handleInput;
  window.sendMessage = sendMessage;
});

function isValidInput(input) {
  return /^[a-zA-Z0-9\s,.;:!?]+$/.test(input) && /[\s,.;:!?]/.test(input);
}

function toggleDarkMode() {
  const body = document.body;
  const toggleIcon = document.getElementById('toggleIcon');

  // Toggle dark mode class on body
  if(body.classList.contains('dark-mode')) {
    body.classList.remove('dark-mode');
    toggleIcon.innerHTML = "&#xE51C;";
  } else {
    body.classList.add('dark-mode');
    toggleIcon.innerHTML = "&#xE518;";
  }
}

function logout() {
  if (confirm("Are you sure you want to log out?")) {
    sessionStorage.clear();
    localStorage.clear();
    window.location.href = "index.html";
  }
}
document.getElementById("logoutBtn").addEventListener("click", logout);
document.getElementById("collapsible-btn btn5").addEventListener("click", logout);