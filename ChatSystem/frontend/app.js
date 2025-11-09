let currentAuthWs = null;
let chatWs = null;
let currentUser = null;

// ----------------------
// STEP 1: AUTH (LOGIN + REGISTER)
// ----------------------
const loginBtn = document.getElementById("loginBtn");
const registerBtn = document.getElementById("registerBtn");

loginBtn.onclick = () => handleAuth("LOGIN");
registerBtn.onclick = () => handleAuth("REGISTER");

function handleAuth(authType) {
  const username = document.getElementById("authUsername").value.trim();
  const password = document.getElementById("authPassword").value.trim();

  if (!username || !password) {
    alert("Please enter both username and password");
    return;
  }

  // Connect to WebSocket Auth Server (port 8889)
  currentAuthWs = new WebSocket("ws://localhost:8889");

  currentAuthWs.onopen = () => {
    console.log(`[Auth] Sending ${authType} for user: ${username}`);
    currentAuthWs.send(
      JSON.stringify({
        type: authType, // 'REGISTER' or 'LOGIN'
        sender: username,
        content: password,
      })
    );
  };

  currentAuthWs.onmessage = (event) => {
    const data = JSON.parse(event.data);
    console.log("[Auth Response]", data);

    if (data.type === "AUTH_RESPONSE") {
      alert(data.content);

      if (data.content.includes("SUCCESS")) {
        // Save current user and move to chat panel
        currentUser = username;
        document.getElementById("authPanel").style.display = "none";
        document.getElementById("chatPanel").style.display = "block";
        document.getElementById("currentUsername").innerText = username;

        // Now connect to chat server via WebSocket bridge
        connectToChatServer(username);
      }
    }
  };

  currentAuthWs.onerror = (err) => {
    console.error("[Auth] WebSocket error:", err);
    alert("Connection error during authentication");
  };
}

// ----------------------
// STEP 2: CONNECT TO CHAT
// ----------------------
function connectToChatServer(username) {
  chatWs = new WebSocket("ws://localhost:8889");

  chatWs.onopen = () => {
    console.log(`[Chat] Connected as ${username}`);
    chatWs.send(
      JSON.stringify({
        type: "JOIN",
        sender: username,
        content: "Joined the chat",
      })
    );
  };

  chatWs.onmessage = (event) => {
    const data = JSON.parse(event.data);
    console.log("[Chat Message]", data);

    const messageArea = document.getElementById("messageArea");

    if (data.type === "USER_LIST") {
      updateUserList(data.content.split(","));
    } else if (data.type === "MESSAGE" || data.type === "PRIVATE_MESSAGE") {
      const messageDiv = document.createElement("div");
      messageDiv.classList.add(
        "message",
        data.sender === currentUser ? "own-message" : "other-message"
      );
      messageDiv.innerHTML = `<strong>${data.sender}:</strong> ${data.content}`;
      messageArea.appendChild(messageDiv);
      messageArea.scrollTop = messageArea.scrollHeight;
    } else if (data.type === "FILE") {
      const fileDiv = document.createElement("div");
      fileDiv.classList.add("file-message");
      fileDiv.innerHTML = `<strong>${data.sender}</strong> sent a file: <a href="${data.content}" target="_blank">Download</a>`;
      messageArea.appendChild(fileDiv);
    } else if (data.type === "SERVER_INFO") {
      const infoDiv = document.createElement("div");
      infoDiv.classList.add("server-message");
      infoDiv.textContent = data.content;
      messageArea.appendChild(infoDiv);
    }
  };

  chatWs.onclose = () => {
    console.log("[Chat] Disconnected from chat server");
  };

  chatWs.onerror = (err) => {
    console.error("[Chat] WebSocket error:", err);
  };
}

// ----------------------
// STEP 3: MESSAGE SENDING
// ----------------------
const messageForm = document.getElementById("messageForm");
const messageInput = document.getElementById("messageInput");

messageForm.addEventListener("submit", (e) => {
  e.preventDefault();
  const message = messageInput.value.trim();
  if (!message) return;

  chatWs.send(
    JSON.stringify({
      type: "MESSAGE",
      sender: currentUser,
      content: message,
    })
  );
  messageInput.value = "";
});

// ----------------------
// STEP 4: FILE UPLOAD
// ----------------------
const fileBtn = document.getElementById("fileBtn");
const fileInput = document.getElementById("fileInput");

fileBtn.onclick = () => fileInput.click();

fileInput.onchange = () => {
  const file = fileInput.files[0];
  if (!file) return;

  const reader = new FileReader();
  reader.onload = () => {
    chatWs.send(
      JSON.stringify({
        type: "FILE",
        sender: currentUser,
        content: reader.result,
        filename: file.name,
      })
    );
  };
  reader.readAsDataURL(file);
};

// ----------------------
// STEP 5: DISCONNECT
// ----------------------
document.getElementById("disconnectBtn").onclick = () => {
  if (chatWs && chatWs.readyState === WebSocket.OPEN) {
    chatWs.close();
    alert("You have disconnected from the chat.");
    document.getElementById("chatPanel").style.display = "none";
    document.getElementById("authPanel").style.display = "block";
  }
};

// ----------------------
// STEP 6: USER LIST UPDATE
// ----------------------
function updateUserList(users) {
  const userList = document.getElementById("userList");
  userList.innerHTML = "";
  users.forEach((user) => {
    const li = document.createElement("li");
    li.textContent = user;
    userList.appendChild(li);
  });
}

// ----------------------
// STEP 7: API BUTTONS
// ----------------------
document.querySelectorAll(".btn-api").forEach((btn) => {
  btn.onclick = () => {
    const cmd = btn.getAttribute("data-cmd");
    chatWs.send(
      JSON.stringify({
        type: "MESSAGE",
        sender: currentUser,
        content: cmd,
      })
    );
  };
});
