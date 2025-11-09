// =======================
// AUTHENTICATION HANDLING
// =======================
let currentUser = null;

// Reference DOM elements
const authPanel = document.getElementById("authPanel");
const connectionPanel = document.getElementById("connectionPanel");
const chatPanel = document.getElementById("chatPanel");
const currentUsernameDisplay = document.getElementById("currentUsername");

// Buttons
const loginBtn = document.getElementById("loginBtn");
const registerBtn = document.getElementById("registerBtn");

// Input fields
const authUsername = document.getElementById("authUsername");
const authPassword = document.getElementById("authPassword");

// Initialize WebSocket for Authentication
let currentAuthWs = new WebSocket("ws://localhost:8888/auth");

currentAuthWs.onopen = () => {
  console.log("[Auth] Connected to authentication service");
};

currentAuthWs.onmessage = (event) => {
  const data = JSON.parse(event.data);
  console.log("[Auth Response]", data);

  if (data.type === "AUTH_RESPONSE") {
    alert(data.content);

    // ✅ If authentication or registration is successful
    if (
      data.content.includes("LOGIN_SUCCESS") ||
      data.content.includes("REGISTER_SUCCESS")
    ) {
      currentUser = authUsername.value;
      authPanel.style.display = "none";
      connectToChatServer(currentUser);
    }
  }
};

currentAuthWs.onerror = (error) => {
  console.error("[Auth] WebSocket Error:", error);
  alert("Authentication service connection failed.");
};

// Handle Login and Register buttons
loginBtn.addEventListener("click", () => {
  const payload = {
    type: "LOGIN",
    username: authUsername.value.trim(),
    password: authPassword.value.trim(),
  };
  currentAuthWs.send(JSON.stringify(payload));
});

registerBtn.addEventListener("click", () => {
  const payload = {
    type: "REGISTER",
    username: authUsername.value.trim(),
    password: authPassword.value.trim(),
  };
  currentAuthWs.send(JSON.stringify(payload));
});

// =======================
// CHAT CONNECTION HANDLING
// =======================
let chatSocket;

function connectToChatServer(username) {
  const serverHost = "localhost";
  const serverPort = 8888;
  const wsUrl = `ws://${serverHost}:${serverPort}/chat?username=${username}`;

  chatSocket = new WebSocket(wsUrl);

  chatSocket.onopen = () => {
    console.log("[Chat] Connected to chat server");
    currentUsernameDisplay.textContent = username;
    chatPanel.style.display = "block";
  };

  chatSocket.onmessage = (event) => {
    const messageArea = document.getElementById("messageArea");
    const data = JSON.parse(event.data);

    if (data.type === "USER_LIST") {
      updateUserList(data.users);
    } else if (data.type === "CHAT_MESSAGE") {
      const msgDiv = document.createElement("div");
      msgDiv.className = "message";
      msgDiv.textContent = `${data.sender}: ${data.message}`;
      messageArea.appendChild(msgDiv);
      messageArea.scrollTop = messageArea.scrollHeight;
    }
  };

  chatSocket.onclose = () => {
    console.log("[Chat] Disconnected from chat server");
    alert("Disconnected from server.");
    chatPanel.style.display = "none";
    authPanel.style.display = "block";
  };
}

// =======================
// CHAT MESSAGE HANDLING
// =======================
const messageForm = document.getElementById("messageForm");
const messageInput = document.getElementById("messageInput");

messageForm.addEventListener("submit", (e) => {
  e.preventDefault();
  const message = messageInput.value.trim();
  if (message && chatSocket && chatSocket.readyState === WebSocket.OPEN) {
    const payload = { type: "CHAT_MESSAGE", message };
    chatSocket.send(JSON.stringify(payload));
    messageInput.value = "";
  }
});

// =======================
// USER LIST MANAGEMENT
// =======================
function updateUserList(users) {
  const userList = document.getElementById("userList");
  userList.innerHTML = "";
  users.forEach((u) => {
    const li = document.createElement("li");
    li.textContent = u;
    userList.appendChild(li);
  });
}

// =======================
// DISCONNECT HANDLING
// =======================
const disconnectBtn = document.getElementById("disconnectBtn");
disconnectBtn.addEventListener("click", () => {
  if (chatSocket) chatSocket.close();
  chatPanel.style.display = "none";
  authPanel.style.display = "block";
});

// =======================
// FILE UPLOAD HANDLING
// =======================
const fileBtn = document.getElementById("fileBtn");
const fileInput = document.getElementById("fileInput");

fileBtn.addEventListener("click", () => fileInput.click());

fileInput.addEventListener("change", (e) => {
  const file = e.target.files[0];
  if (file && chatSocket && chatSocket.readyState === WebSocket.OPEN) {
    const reader = new FileReader();
    reader.onload = () => {
      const payload = {
        type: "FILE_MESSAGE",
        fileName: file.name,
        fileData: reader.result,
      };
      chatSocket.send(JSON.stringify(payload));
    };
    reader.readAsDataURL(file);
  }
});
