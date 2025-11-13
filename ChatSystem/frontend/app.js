// ----------------------
// GLOBAL VARIABLES
// ----------------------
let currentAuthWs = null;
let chatWs = null;
let currentUser = null;
let typingUsers = new Set();
let typingTimeout = null;
let reconnectTimer = null;

const STORAGE_KEYS = {
  username: "chat_username",
  session: "chat_session",
  messages: "chat_messages",
};
const MAX_MESSAGES = 100;

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

  currentAuthWs = new WebSocket("ws://localhost:8889");

  currentAuthWs.onopen = () => {
    console.log(`[Auth] ${authType} as ${username}`);
    currentAuthWs.send(
      JSON.stringify({
        type: authType,
        sender: username,
        content: password,
      })
    );
  };

  currentAuthWs.onmessage = (event) => {
    const data = JSON.parse(event.data);
    if (data.type === "AUTH_RESPONSE") {
      alert(data.content);
      if (data.content.includes("SUCCESS")) {
        currentUser = username;
        localStorage.setItem(STORAGE_KEYS.username, username);
        document.getElementById("authPanel").style.display = "none";
        document.getElementById("chatPanel").style.display = "block";
        document.getElementById("currentUsername").innerText = username;
        connectToChatServer(username);
      }
    }
  };

  currentAuthWs.onerror = () => alert("Auth server not reachable");
}

// ----------------------
// STEP 2: CONNECT TO CHAT SERVER
// ----------------------
function connectToChatServer(username) {
  chatWs = new WebSocket("ws://localhost:8889");

  chatWs.onopen = () => {
    console.log(`[Chat] Connected as ${username}`);
    // Send CONNECT instead of JOIN
    sendJson({ type: "CONNECT", sender: username, content: "Joined chat" });
    restoreMessages();
  };

  chatWs.onmessage = (event) => {
    const data = JSON.parse(event.data);
    handleChatMessage(data);
  };

  chatWs.onclose = () => {
    console.log("[Chat] Disconnected");
    tryReconnect(username);
  };

  chatWs.onerror = (err) => {
    console.error("[Chat] Error", err);
  };
}

// ----------------------
// STEP 3: MESSAGE HANDLING
// ----------------------
function handleChatMessage(data) {
  const messageArea = document.getElementById("messageArea");

  switch (data.type) {
    case "USER_LIST":
      updateUserList(data.content.split(","));
      break;

    case "CHAT":
    case "PRIVATE_MSG":
      displayMessage(data.sender, data.content, data.type);
      saveMessage(data);
      break;

    case "FILE_SEND":
      displayFileMessage(data.sender, data.fileName, data.content);
      saveMessage(data);
      break;

    case "TYPING_START":
      showTyping(data.sender);
      break;

    case "TYPING_STOP":
      hideTyping(data.sender);
      break;

    case "SERVER_INFO":
      displayServerInfo(data.content);
      break;

    default:
      console.log("[Unknown message]", data);
  }

  messageArea.scrollTop = messageArea.scrollHeight;
}

// ----------------------
// STEP 4: MESSAGE SENDING
// ----------------------
const messageForm = document.getElementById("messageForm");
const messageInput = document.getElementById("messageInput");

messageForm.addEventListener("submit", (e) => {
  e.preventDefault();
  const message = messageInput.value.trim();
  if (!message) return;

  // Send CHAT instead of MESSAGE
  sendJson({
    type: "CHAT",
    sender: currentUser,
    content: message,
  });
  messageInput.value = "";
  sendTypingStop();
});

// ----------------------
// STEP 5: FILE UPLOAD
// ----------------------
const fileBtn = document.getElementById("fileBtn");
const fileInput = document.getElementById("fileInput");

fileBtn.onclick = () => fileInput.click();

fileInput.onchange = () => {
  const file = fileInput.files[0];
  if (!file) return;
  const reader = new FileReader();
  reader.onload = () => {
    sendJson({
      type: "FILE_SEND",
      sender: currentUser,
      fileName: file.name,
      fileSize: file.size,
      content: reader.result,
    });
  };
  reader.readAsDataURL(file);
};

// ----------------------
// STEP 6: DISCONNECT
// ----------------------
document.getElementById("disconnectBtn").onclick = () => {
  if (chatWs && chatWs.readyState === WebSocket.OPEN) chatWs.close();
  alert("You disconnected.");
  document.getElementById("chatPanel").style.display = "none";
  document.getElementById("authPanel").style.display = "block";
};

// ----------------------
// STEP 7: USER LIST
// ----------------------
function updateUserList(users) {
  const userList = document.getElementById("userList");
  userList.innerHTML = "";
  users.forEach((u) => {
    const li = document.createElement("li");
    li.textContent = u;
    userList.appendChild(li);
  });
}

// ----------------------
// STEP 8: API COMMAND BUTTONS
// ----------------------
document.querySelectorAll(".btn-api").forEach((btn) => {
  btn.onclick = () =>
    sendJson({
      type: "CHAT",
      sender: currentUser,
      content: btn.dataset.cmd,
    });
});

// ----------------------
// STEP 9: TYPING EVENTS
// ----------------------
messageInput.addEventListener("input", () => {
  sendTypingStart();
  clearTimeout(typingTimeout);
  typingTimeout = setTimeout(sendTypingStop, 1500);
});

function sendTypingStart() {
  sendJson({ type: "TYPING_START", sender: currentUser });
}
function sendTypingStop() {
  sendJson({ type: "TYPING_STOP", sender: currentUser });
}

function showTyping(user) {
  if (user === currentUser) return;
  typingUsers.add(user);
  updateTypingDisplay();
}

function hideTyping(user) {
  typingUsers.delete(user);
  updateTypingDisplay();
}

function updateTypingDisplay() {
  const typingDiv = document.getElementById("typingIndicator") || createTypingIndicator();
  typingDiv.textContent =
    typingUsers.size > 0
      ? `${Array.from(typingUsers).join(", ")} typing...`
      : "";
}

function createTypingIndicator() {
  const div = document.createElement("div");
  div.id = "typingIndicator";
  div.className = "typing-indicator";
  document.getElementById("messageArea").appendChild(div);
  return div;
}

// ----------------------
// STEP 10: LOCAL STORAGE
// ----------------------
function saveMessage(data) {
  const messages = JSON.parse(localStorage.getItem(STORAGE_KEYS.messages) || "[]");
  messages.push(data);
  if (messages.length > MAX_MESSAGES) messages.shift();
  localStorage.setItem(STORAGE_KEYS.messages, JSON.stringify(messages));
}

function restoreMessages() {
  const saved = JSON.parse(localStorage.getItem(STORAGE_KEYS.messages) || "[]");
  saved.forEach((m) => {
    if (m.type === "CHAT" || m.type === "PRIVATE_MSG") {
      displayMessage(m.sender, m.content, m.type);
    } else if (m.type === "FILE_SEND") {
      displayFileMessage(m.sender, m.fileName, m.content);
    }
  });
}

function displayMessage(sender, content, type) {
  const div = document.createElement("div");
  div.className = sender === currentUser ? "own-message" : "other-message";
  div.innerHTML = `<strong>${sender}:</strong> ${content}`;
  document.getElementById("messageArea").appendChild(div);
}

function displayFileMessage(sender, filename, url) {
  const div = document.createElement("div");
  div.className = "file-message";
  div.innerHTML = `<strong>${sender}</strong> sent a file: <a href="${url}" target="_blank">${filename}</a>`;
  document.getElementById("messageArea").appendChild(div);
}

function displayServerInfo(msg) {
  const div = document.createElement("div");
  div.className = "server-message";
  div.textContent = msg;
  document.getElementById("messageArea").appendChild(div);
}

// ----------------------
// STEP 11: RECONNECT SUPPORT
// ----------------------
function tryReconnect(username) {
  if (reconnectTimer) return;
  reconnectTimer = setTimeout(() => {
    console.log("[Reconnect] Attempting...");
    connectToChatServer(username);
    reconnectTimer = null;
  }, 2000);
}

// ----------------------
// UTIL: SEND JSON
// ----------------------
function sendJson(obj) {
  if (chatWs && chatWs.readyState === WebSocket.OPEN) {
    chatWs.send(JSON.stringify(obj));
  } else {
    console.warn("Chat socket not ready.");
  }
}
