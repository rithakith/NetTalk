let ws;
let connected = false;
let username = '';
let currentAuthWs = null;

// DOM Elements
const authPanel = document.getElementById('authPanel');
const connectionPanel = document.getElementById('connectionPanel');
const chatPanel = document.getElementById('chatPanel');
const currentUsernameSpan = document.getElementById('currentUsername');
const messageInput = document.getElementById('messageInput');
const messageArea = document.getElementById('messageArea');
const userList = document.getElementById('userList');

// Auth buttons
document.getElementById('loginBtn').addEventListener('click', () => handleAuth('LOGIN'));
document.getElementById('registerBtn').addEventListener('click', () => handleAuth('REGISTER'));

// File handling
document.getElementById('fileBtn').addEventListener('click', () => document.getElementById('fileInput').click());
document.getElementById('fileInput').addEventListener('change', handleFile);

// Connect form
document.getElementById('connectForm').addEventListener('submit', e => {
    e.preventDefault();
    connectToServer();
});

// Disconnect
document.getElementById('disconnectBtn').addEventListener('click', disconnect);

// Message send
document.getElementById('messageForm').addEventListener('submit', e => {
    e.preventDefault();
    sendMessage(messageInput.value.trim());
    messageInput.value = '';
});

// API commands
document.querySelectorAll('.btn-api').forEach(btn => {
    btn.addEventListener('click', () => sendMessage(btn.getAttribute('data-cmd')));
});

/* 🔐 Secure Authentication */
function handleAuth(authType) {
    const user = document.getElementById('authUsername').value.trim();
    const pass = document.getElementById('authPassword').value.trim();

    if (!user || !pass) {
        alert('Please enter username and password');
        return;
    }

    // Close any existing auth connection
    if (currentAuthWs) {
        currentAuthWs.close();
    }

    // Create WebSocket connection for authentication
    currentAuthWs = new WebSocket('ws://localhost:8889');

    currentAuthWs.onopen = () => {
        console.log(`[Auth] Sending ${authType} for user: ${user}`);
        currentAuthWs.send(JSON.stringify({
            type: authType,
            sender: user,
            content: pass
        }));
    };

    currentAuthWs.onmessage = (e) => {
        try {
            const msg = JSON.parse(e.data);
            console.log('[Auth] Received:', msg);

            if (msg.type === 'AUTH_RESPONSE') {
                if (msg.content.includes('SUCCESS')) {
                    // Authentication successful
                    username = user;
                    authPanel.style.display = 'none';
                    connectionPanel.style.display = 'block';
                    document.getElementById('username').value = user;
                    alert(`✅ ${authType} successful! ${msg.content}`);
                    currentAuthWs.close();
                    currentAuthWs = null;
                } else {
                    // Authentication failed
                    alert(`❌ ${authType} failed: ${msg.content}`);
                    currentAuthWs.close();
                    currentAuthWs = null;
                }
            }
        } catch (error) {
            console.error('[Auth] Error parsing message:', error);
        }
    };

    currentAuthWs.onerror = (error) => {
        console.error('[Auth] WebSocket error:', error);
        alert('Connection error during authentication');
        currentAuthWs = null;
    };

    currentAuthWs.onclose = () => {
        console.log('[Auth] Authentication connection closed');
        currentAuthWs = null;
    };

    // Set timeout to prevent hanging
    setTimeout(() => {
        if (currentAuthWs && currentAuthWs.readyState === WebSocket.OPEN) {
            currentAuthWs.close();
            currentAuthWs = null;
            alert('Authentication timeout. Please try again.');
        }
    }, 10000);
}

/* 🌐 Connection logic */
function connectToServer() {
    const serverHost = document.getElementById('serverHost').value || 'localhost';
    const serverPort = document.getElementById('serverPort').value || '8888';
    const connectUsername = document.getElementById('username').value.trim();

    if (!connectUsername) {
        alert('Please enter a username');
        return;
    }

    // Use the authenticated username or the one from connect form
    const finalUsername = username || connectUsername;

    try {
        ws = new WebSocket(`ws://${serverHost}:8889`);

        ws.onopen = () => {
            console.log('[Connect] WebSocket connected, sending CONNECT for:', finalUsername);
            // Send connect message to join chat
            ws.send(JSON.stringify({
                type: 'CONNECT',
                sender: finalUsername
            }));
            connected = true;
            username = finalUsername;
            connectionPanel.style.display = 'none';
            chatPanel.style.display = 'block';
            currentUsernameSpan.textContent = `${username}`;
            addMessage('SYSTEM', 'System', 'Connected to chat server');
        };

        ws.onmessage = (event) => {
            try {
                const message = JSON.parse(event.data);
                handleMessage(message);
            } catch (error) {
                console.error('[Connect] Error parsing message:', error);
            }
        };

        ws.onclose = (event) => {
            console.log('[Connect] WebSocket closed:', event.code, event.reason);
            if (connected) {
                addMessage('SYSTEM', 'System', 'Disconnected from server');
                disconnect();
            }
        };

        ws.onerror = (error) => {
            console.error('[Connect] WebSocket error:', error);
            alert('Failed to connect to server');
        };

    } catch (error) {
        console.error('[Connect] Connection error:', error);
        alert('Connection failed: ' + error.message);
    }
}

/* 💬 Send message */
function sendMessage(text) {
    if (!connected || !text || !ws) {
        console.warn('[Send] Not connected or empty message');
        return;
    }

    let message = {
        sender: username,
        content: text
    };

    // Determine message type
    if (text.startsWith('@')) {
        // Private message: @username message
        const spaceIndex = text.indexOf(' ');
        if (spaceIndex > 0) {
            message.type = 'PRIVATE_MSG';
            message.receiver = text.substring(1, spaceIndex);
            message.content = text.substring(spaceIndex + 1);
        } else {
            alert('Invalid private message format. Use: @username message');
            return;
        }
    } else if (text.startsWith('/')) {
        // Command message
        if (text === '/quit') {
            disconnect();
            return;
        }
        message.type = 'API_REQUEST';
    } else {
        // Regular chat message
        message.type = 'CHAT';
    }

    try {
        ws.send(JSON.stringify(message));
        console.log('[Send] Message sent:', message.type);

        // Echo our own message for immediate feedback
        if (message.type === 'CHAT') {
            addMessage('CHAT', username, text);
        }
    } catch (error) {
        console.error('[Send] Error sending message:', error);
    }
}

/* 📁 File sending */
function handleFile(e) {
    const file = e.target.files[0];
    if (!file) return;

    // Reset file input
    e.target.value = '';

    if (file.size > 10 * 1024 * 1024) { // 10MB limit
        alert('File too large. Maximum size is 10MB.');
        return;
    }

    const reader = new FileReader();
    reader.onload = () => {
        // Convert to base64 for text-based transfer
        const base64Data = reader.result.split(',')[1];

        // Send file via WebSocket
        ws.send(JSON.stringify({
            type: 'FILE_SEND',
            sender: username,
            fileName: file.name,
            fileSize: file.size,
            content: base64Data
        }));

        addMessage('SYSTEM', 'You', `📤 Uploading file: ${file.name} (${formatFileSize(file.size)})`);
    };

    reader.onerror = () => {
        alert('Error reading file');
    };

    reader.readAsDataURL(file);
}

function formatFileSize(bytes) {
    if (bytes === 0) return '0 Bytes';
    const k = 1024;
    const sizes = ['Bytes', 'KB', 'MB', 'GB'];
    const i = Math.floor(Math.log(bytes) / Math.log(k));
    return parseFloat((bytes / Math.pow(k, i)).toFixed(2)) + ' ' + sizes[i];
}

/* 📥 Handle incoming messages */
function handleMessage(msg) {
    console.log('[Receive] Message received:', msg);

    switch (msg.type) {
        case 'CHAT':
            addMessage('CHAT', msg.sender, msg.content);
            break;
        case 'PRIVATE_MSG':
            addMessage('PRIVATE', msg.sender, `(Private) ${msg.content}`);
            break;
        case 'API_RESPONSE':
            addMessage('API', 'API Bot', msg.content);
            break;
        case 'USER_LIST':
            const users = msg.content.replace('Online users: ', '').split(', ');
            updateUserList(users);
            break;
        case 'SYSTEM':
            addMessage('SYSTEM', 'Server', msg.content);
            break;
        case 'FILE_RESPONSE':
            addMessage('SYSTEM', 'Server', msg.content);
            break;
        case 'AUTH_RESPONSE':
            console.log('[Receive] Auth response:', msg.content);
            break;
        default:
            console.log('[Receive] Unknown message type:', msg.type);
            addMessage('SYSTEM', 'Unknown', JSON.stringify(msg));
    }
}

/* 🧹 Disconnect */
function disconnect() {
    if (ws) {
        try {
            ws.send(JSON.stringify({
                type: 'DISCONNECT',
                sender: username
            }));
        } catch (error) {
            console.error('[Disconnect] Error sending disconnect message:', error);
        }
        ws.close();
    }

    connected = false;
    chatPanel.style.display = 'none';
    connectionPanel.style.display = 'block';

    // Clear chat area
    messageArea.innerHTML = '';
    userList.innerHTML = '';

    console.log('[Disconnect] Disconnected from server');
}

/* 🪶 Message rendering */
function addMessage(type, sender, content) {
    const div = document.createElement('div');
    div.className = `message message-${type.toLowerCase()}`;

    const time = new Date().toLocaleTimeString();
    div.innerHTML = `
        <div class="message-sender">
            ${sender} <span class="message-time">${time}</span>
        </div>
        <div class="message-content">${content}</div>
    `;

    messageArea.appendChild(div);
    messageArea.scrollTop = messageArea.scrollHeight;
}

/* 👥 Update user list */
function updateUserList(users) {
    userList.innerHTML = '';
    if (users && users.length > 0) {
        users.forEach(user => {
            if (user.trim()) {
                const li = document.createElement('li');
                li.textContent = user;
                if (user === username) {
                    li.style.fontWeight = 'bold';
                    li.style.color = '#667eea';
                }
                userList.appendChild(li);
            }
        });
    }
}

// Add some helpful debug info
console.log('ChatSystem Frontend Loaded');
console.log('Available commands: /help, /joke, /quote, /weather <city>');
console.log('Private messages: @username message');
console.log('File upload: Click "Send File" button (up to 10MB)');