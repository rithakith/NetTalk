// Chat Client Frontend Application
// Note: This is a demonstration frontend. For actual connection to the Java server,
// you would need a WebSocket bridge or REST API layer.

let connected = false;
let username = '';
let ws = null; // WebSocket connection (if implemented)

// Typing indicator variables
let typingUsers = new Set(); // Users currently typing
let typingTimeout = null; // Timeout for stopping typing indicator
let isTyping = false; // Whether current user is typing

// Local Storage keys
const STORAGE_KEYS = {
    MESSAGES: 'chatMessages',
    USERNAME: 'lastUsername',
    SESSION: 'chatSession',
    SERVER_HOST: 'serverHost',
    SERVER_PORT: 'serverPort',
    MAX_MESSAGES: 100 // Maximum messages to store
};

// DOM Elements
const connectionPanel = document.getElementById('connectionPanel');
const chatPanel = document.getElementById('chatPanel');
const connectForm = document.getElementById('connectForm');
const messageForm = document.getElementById('messageForm');
const messageInput = document.getElementById('messageInput');
const messageArea = document.getElementById('messageArea');
const currentUsernameSpan = document.getElementById('currentUsername');
const disconnectBtn = document.getElementById('disconnectBtn');
const userList = document.getElementById('userList');
const apiButtons = document.querySelectorAll('.btn-api');
const statusIndicator = document.getElementById('statusIndicator');

// Connect Form Handler
connectForm.addEventListener('submit', (e) => {
    e.preventDefault();
    
    username = document.getElementById('username').value.trim();
    const serverHost = document.getElementById('serverHost').value;
    const serverPort = document.getElementById('serverPort').value;
    
    if (!username) {
        alert('Please enter a username');
        return;
    }
    
    // Save username to local storage
    saveUsername(username);
    
    // Save server connection details
    saveConnectionDetails(serverHost, serverPort);
    
    // Simulate connection (in real implementation, establish WebSocket connection)
    connectToServer(username, serverHost, serverPort);
});

// Message Form Handler
messageForm.addEventListener('submit', (e) => {
    e.preventDefault();
    
    const message = messageInput.value.trim();
    if (!message) return;
    
    // Stop typing indicator when sending message
    stopTyping();
    
    sendMessage(message);
    messageInput.value = '';
});

// Typing detection on message input
messageInput.addEventListener('input', () => {
    if (!connected) return;
    
    const value = messageInput.value.trim();
    
    if (value.length > 0 && !isTyping) {
        // User started typing
        startTyping();
    } else if (value.length === 0 && isTyping) {
        // User cleared input
        stopTyping();
    }
    
    // Reset typing timeout
    if (isTyping) {
        clearTimeout(typingTimeout);
        typingTimeout = setTimeout(() => {
            stopTyping();
        }, 3000); // Stop typing after 3 seconds of inactivity
    }
});

// Stop typing when user stops for a while or presses Enter
messageInput.addEventListener('keydown', (e) => {
    if (e.key === 'Enter' && isTyping) {
        stopTyping();
    }
});

// Also stop typing when user leaves the input field
messageInput.addEventListener('blur', () => {
    if (isTyping) {
        stopTyping();
    }
});

// Disconnect Button Handler
disconnectBtn.addEventListener('click', () => {
    disconnect();
});

// API Command Buttons
apiButtons.forEach(btn => {
    btn.addEventListener('click', () => {
        const command = btn.getAttribute('data-cmd');
        sendMessage(command);
    });
});

/**
 * Connect to chat server via WebSocket bridge
 */
function connectToServer(user, host, port) {
    console.log(`Connecting to WebSocket bridge at ${host}:8889 as ${user}...`);
    
    try {
        // Connect to WebSocket bridge server (running on port 8889)
        ws = new WebSocket(`ws://${host}:8889`);
        
        ws.onopen = () => {
            console.log('Connected to WebSocket bridge');
            connected = true;
            username = user;
            
            // Send CONNECT message
            ws.send(JSON.stringify({type: 'CONNECT', sender: username}));
            
            // Save session state
            saveSession(user, host, port);
            
            // Update UI
            connectionPanel.style.display = 'none';
            chatPanel.style.display = 'block';
            currentUsernameSpan.textContent = username;
            updateStatusIndicator('connected');
            
            // Load previous messages from local storage
            loadMessagesFromStorage();
            
            // Add system message
            addMessage('SYSTEM', 'Server', `Connecting as ${username}...`);
        };
        
        ws.onmessage = (event) => {
            try {
                const message = JSON.parse(event.data);
                handleMessage(message);
            } catch (error) {
                console.error('Error parsing message:', error);
            }
        };
        
        ws.onerror = (error) => {
            console.error('WebSocket error:', error);
            addMessage('SYSTEM', 'Error', 'Failed to connect to WebSocket bridge server on port 8889');
            addMessage('SYSTEM', 'Info', 'Please ensure: 1) ChatServer is running on port 8888, 2) WebSocketServer is running on port 8889');
            addMessage('SYSTEM', 'Info', 'Run: java -cp ".:lib/*" com.chatapp.server.WebSocketServer');
            alert('Connection error!\n\nMake sure:\n1. ChatServer is running (port 8888)\n2. WebSocketServer is running (port 8889)\n3. Required libraries (Java-WebSocket and Gson) are in lib/ folder');
            disconnect();
        };
        
        ws.onclose = () => {
            console.log('WebSocket connection closed');
            
            if (connected) {
                // Unexpected disconnect - try to reconnect
                connected = false;
                updateStatusIndicator('disconnected');
                addMessage('SYSTEM', 'Server', 'Connection lost. Attempting to reconnect...');
                
                // Wait 2 seconds and try to reconnect
                setTimeout(() => {
                    const sessionData = localStorage.getItem(STORAGE_KEYS.SESSION);
                    if (sessionData) {
                        const session = JSON.parse(sessionData);
                        console.log('Attempting to reconnect...');
                        updateStatusIndicator('connecting');
                        connectToServer(session.username, session.host, session.port);
                    }
                }, 2000);
            } else {
                // Intentional disconnect
                disconnect();
            }
        };
        
    } catch (error) {
        console.error('Failed to create WebSocket connection:', error);
        alert('Failed to connect to WebSocket bridge server!');
    }
}

/**
 * Send message to server
 */
function sendMessage(message) {
    if (!connected || !ws || ws.readyState !== WebSocket.OPEN) {
        alert('Not connected to server!');
        return;
    }
    
    console.log('Sending message:', message);
    
    let messageObj = {
        sender: username,
        content: message
    };
    
    // Handle different message types
    if (message.startsWith('@')) {
        // Private message
        const spaceIndex = message.indexOf(' ');
        if (spaceIndex > 0) {
            const receiver = message.substring(1, spaceIndex);
            const content = message.substring(spaceIndex + 1);
            messageObj = {
                type: 'PRIVATE_MSG',
                sender: username,
                receiver: receiver,
                content: content
            };
        } else {
            alert('Invalid private message format. Use: @username message');
            return;
        }
    } else if (message.startsWith('/')) {
        // API command
        messageObj.type = 'API_REQUEST';
    } else {
        // Regular chat message
        messageObj.type = 'CHAT';
    }
    
    // Send message through WebSocket
    try {
        ws.send(JSON.stringify(messageObj));
    } catch (error) {
        console.error('Failed to send message:', error);
        alert('Failed to send message!');
    }
}

/**
 * Handle incoming messages from server
 */
function handleMessage(message) {
    console.log('Received message:', message);
    
    switch (message.type) {
        case 'CONNECT':
            addMessage('SYSTEM', 'Server', `${message.sender} joined the chat`);
            break;
            
        case 'DISCONNECT':
            addMessage('SYSTEM', 'Server', `${message.sender} left the chat`);
            break;
            
        case 'CHAT':
            addMessage('CHAT', message.sender, message.content);
            break;
            
        case 'PRIVATE_MSG':
            if (message.receiver === username || message.sender === username) {
                addMessage('PRIVATE', message.sender, 
                    `@${message.receiver}: ${message.content}`);
            }
            break;
            
        case 'API_RESPONSE':
            addMessage('API', 'API Bot', message.content);
            break;
            
        case 'SYSTEM':
            addMessage('SYSTEM', 'Server', message.content);
            break;
            
        case 'TYPING_START':
            if (message.sender !== username) {
                addTypingUser(message.sender);
            }
            break;
            
        case 'TYPING_STOP':
            if (message.sender !== username) {
                removeTypingUser(message.sender);
            }
            break;
            break;
            
        case 'USER_LIST':
            if (message.content) {
                try {
                    const users = JSON.parse(message.content);
                    updateUserList(users);
                } catch (e) {
                    console.error('Failed to parse user list:', e);
                }
            }
            break;
            
        default:
            console.warn('Unknown message type:', message.type);
    }
}

/**
 * Disconnect from server
 */
function disconnect() {
    if (ws && ws.readyState === WebSocket.OPEN) {
        // Send disconnect message before closing
        try {
            ws.send(JSON.stringify({
                type: 'DISCONNECT',
                sender: username
            }));
        } catch (e) {
            console.error('Failed to send disconnect message:', e);
        }
        ws.close();
    }
    
    ws = null;
    connected = false;
    
    // Clear session state
    clearSession();
    
    // Clean up typing state
    isTyping = false;
    typingUsers.clear();
    clearTimeout(typingTimeout);
    
    chatPanel.style.display = 'none';
    connectionPanel.style.display = 'block';
    messageArea.innerHTML = '';
    
    console.log('Disconnected from server');
}

/**
 * Start typing indicator
 */
function startTyping() {
    if (!isTyping && connected && ws) {
        isTyping = true;
        try {
            ws.send(JSON.stringify({
                type: 'TYPING_START',
                sender: username
            }));
        } catch (error) {
            console.error('Failed to send typing start:', error);
        }
    }
}

/**
 * Stop typing indicator
 */
function stopTyping() {
    if (isTyping && connected && ws) {
        isTyping = false;
        clearTimeout(typingTimeout);
        try {
            ws.send(JSON.stringify({
                type: 'TYPING_STOP',
                sender: username
            }));
        } catch (error) {
            console.error('Failed to send typing stop:', error);
        }
    }
}

/**
 * Add user to typing indicator
 */
function addTypingUser(user) {
    typingUsers.add(user);
    updateTypingIndicator();
}

/**
 * Remove user from typing indicator
 */
function removeTypingUser(user) {
    typingUsers.delete(user);
    updateTypingIndicator();
}

/**
 * Update typing indicator display
 */
function updateTypingIndicator() {
    let typingIndicator = document.getElementById('typingIndicator');
    
    // Create typing indicator if it doesn't exist
    if (!typingIndicator) {
        typingIndicator = document.createElement('div');
        typingIndicator.id = 'typingIndicator';
        typingIndicator.className = 'typing-indicator';
        messageArea.appendChild(typingIndicator);
    }
    
    if (typingUsers.size > 0) {
        const users = Array.from(typingUsers);
        let text = '';
        
        if (users.length === 1) {
            text = `${users[0]} is typing...`;
        } else if (users.length === 2) {
            text = `${users[0]} and ${users[1]} are typing...`;
        } else {
            text = `${users.slice(0, -1).join(', ')}, and ${users[users.length - 1]} are typing...`;
        }
        
        typingIndicator.innerHTML = `<em class="typing-text">${text}</em>`;
        typingIndicator.style.display = 'block';
        
        // Scroll to bottom to show typing indicator
        messageArea.scrollTop = messageArea.scrollHeight;
    } else {
        typingIndicator.style.display = 'none';
    }
}

/**
 * Add message to chat area
 */
function addMessage(type, sender, content) {
    const messageDiv = document.createElement('div');
    messageDiv.className = `message message-${type.toLowerCase()}`;
    
    const senderDiv = document.createElement('div');
    senderDiv.className = 'message-sender';
    senderDiv.textContent = sender;
    
    const contentDiv = document.createElement('div');
    contentDiv.className = 'message-content';
    contentDiv.textContent = content;
    
    // Add timestamp
    const timestamp = new Date().toLocaleTimeString();
    const timeDiv = document.createElement('span');
    timeDiv.className = 'message-time';
    timeDiv.textContent = ` (${timestamp})`;
    
    if (type !== 'SYSTEM') {
        senderDiv.appendChild(timeDiv);
        messageDiv.appendChild(senderDiv);
    }
    messageDiv.appendChild(contentDiv);
    
    messageArea.appendChild(messageDiv);
    messageArea.scrollTop = messageArea.scrollHeight;
    
    // Save message to local storage
    saveMessageToStorage({
        type: type,
        sender: sender,
        content: content,
        timestamp: new Date().toISOString()
    });
}

/**
 * Update user list
 */
function updateUserList(users) {
    userList.innerHTML = '';
    users.forEach(user => {
        const li = document.createElement('li');
        li.textContent = user;
        if (user === username) {
            li.style.fontWeight = 'bold';
            li.style.color = '#667eea';
        }
        userList.appendChild(li);
    });
}

/**
 * Update connection status indicator
 */
function updateStatusIndicator(status) {
    if (!statusIndicator) return;
    
    switch (status) {
        case 'connected':
            statusIndicator.textContent = '🟢';
            statusIndicator.title = 'Connected';
            break;
        case 'connecting':
            statusIndicator.textContent = '🟡';
            statusIndicator.title = 'Connecting...';
            break;
        case 'disconnected':
            statusIndicator.textContent = '🔴';
            statusIndicator.title = 'Disconnected';
            break;
        default:
            statusIndicator.textContent = '⚪';
            statusIndicator.title = 'Unknown';
    }
}

// Initialize
console.log('Chat application loaded');
console.log('Make sure the WebSocket bridge server is running on port 8889');

// Try to restore previous session first
const sessionRestored = restoreSession();

// If no session restored, load last username if available
if (!sessionRestored) {
    loadLastUsername();
}

/**
 * Local Storage Functions
 */

/**
 * Save message to local storage
 */
function saveMessageToStorage(message) {
    try {
        let messages = JSON.parse(localStorage.getItem(STORAGE_KEYS.MESSAGES)) || [];
        
        // Add new message
        messages.push(message);
        
        // Keep only last MAX_MESSAGES
        if (messages.length > STORAGE_KEYS.MAX_MESSAGES) {
            messages = messages.slice(-STORAGE_KEYS.MAX_MESSAGES);
        }
        
        localStorage.setItem(STORAGE_KEYS.MESSAGES, JSON.stringify(messages));
    } catch (error) {
        console.error('Failed to save message to local storage:', error);
    }
}

/**
 * Load messages from local storage
 */
function loadMessagesFromStorage() {
    try {
        const messages = JSON.parse(localStorage.getItem(STORAGE_KEYS.MESSAGES)) || [];
        
        if (messages.length > 0) {
            // Add separator to show previous messages
            const separatorDiv = document.createElement('div');
            separatorDiv.className = 'message message-system';
            separatorDiv.innerHTML = '<div class="message-content"><em>--- Previous Messages ---</em></div>';
            messageArea.appendChild(separatorDiv);
            
            // Display stored messages
            messages.forEach(msg => {
                displayStoredMessage(msg);
            });
            
            // Add another separator
            const separator2Div = document.createElement('div');
            separator2Div.className = 'message message-system';
            separator2Div.innerHTML = '<div class="message-content"><em>--- New Session ---</em></div>';
            messageArea.appendChild(separator2Div);
            
            messageArea.scrollTop = messageArea.scrollHeight;
        }
    } catch (error) {
        console.error('Failed to load messages from local storage:', error);
    }
}

/**
 * Display a stored message from local storage
 */
function displayStoredMessage(message) {
    const messageDiv = document.createElement('div');
    messageDiv.className = `message message-${message.type.toLowerCase()} message-stored`;
    
    const senderDiv = document.createElement('div');
    senderDiv.className = 'message-sender';
    senderDiv.textContent = message.sender;
    
    const contentDiv = document.createElement('div');
    contentDiv.className = 'message-content';
    contentDiv.textContent = message.content;
    
    // Format stored timestamp
    let timestamp = 'Unknown time';
    try {
        const date = new Date(message.timestamp);
        timestamp = date.toLocaleString();
    } catch (e) {
        console.error('Failed to parse timestamp:', e);
    }
    
    const timeDiv = document.createElement('span');
    timeDiv.className = 'message-time';
    timeDiv.textContent = ` (${timestamp})`;
    
    if (message.type !== 'SYSTEM') {
        senderDiv.appendChild(timeDiv);
        messageDiv.appendChild(senderDiv);
    }
    messageDiv.appendChild(contentDiv);
    
    messageArea.appendChild(messageDiv);
}

/**
 * Clear all messages from local storage
 */
function clearMessagesFromStorage() {
    try {
        localStorage.removeItem(STORAGE_KEYS.MESSAGES);
        console.log('Messages cleared from local storage');
    } catch (error) {
        console.error('Failed to clear messages from local storage:', error);
    }
}

/**
 * Save username to local storage
 */
function saveUsername(user) {
    try {
        localStorage.setItem(STORAGE_KEYS.USERNAME, user);
    } catch (error) {
        console.error('Failed to save username to local storage:', error);
    }
}

/**
 * Load last username from local storage
 */
function loadLastUsername() {
    try {
        const lastUser = localStorage.getItem(STORAGE_KEYS.USERNAME);
        if (lastUser) {
            document.getElementById('username').value = lastUser;
        }
        
        // Load saved server details
        const savedHost = localStorage.getItem(STORAGE_KEYS.SERVER_HOST);
        const savedPort = localStorage.getItem(STORAGE_KEYS.SERVER_PORT);
        if (savedHost) {
            document.getElementById('serverHost').value = savedHost;
        }
        if (savedPort) {
            document.getElementById('serverPort').value = savedPort;
        }
    } catch (error) {
        console.error('Failed to load username from local storage:', error);
    }
}

/**
 * Save session state to local storage
 */
function saveSession(user, host, port) {
    try {
        const sessionData = {
            username: user,
            host: host,
            port: port,
            timestamp: new Date().toISOString(),
            connected: true
        };
        localStorage.setItem(STORAGE_KEYS.SESSION, JSON.stringify(sessionData));
        console.log('Session saved to local storage');
    } catch (error) {
        console.error('Failed to save session to local storage:', error);
    }
}

/**
 * Clear session state from local storage
 */
function clearSession() {
    try {
        localStorage.removeItem(STORAGE_KEYS.SESSION);
        console.log('Session cleared from local storage');
    } catch (error) {
        console.error('Failed to clear session from local storage:', error);
    }
}

/**
 * Save connection details to local storage
 */
function saveConnectionDetails(host, port) {
    try {
        localStorage.setItem(STORAGE_KEYS.SERVER_HOST, host);
        localStorage.setItem(STORAGE_KEYS.SERVER_PORT, port);
    } catch (error) {
        console.error('Failed to save connection details:', error);
    }
}

/**
 * Restore previous session if exists
 */
function restoreSession() {
    try {
        const sessionData = localStorage.getItem(STORAGE_KEYS.SESSION);
        if (sessionData) {
            const session = JSON.parse(sessionData);
            
            // Check if session is recent (within last 24 hours)
            const sessionTime = new Date(session.timestamp);
            const now = new Date();
            const hoursDiff = (now - sessionTime) / (1000 * 60 * 60);
            
            if (hoursDiff < 24 && session.connected) {
                console.log('Restoring previous session for:', session.username);
                
                // Auto-reconnect
                username = session.username;
                document.getElementById('username').value = session.username;
                document.getElementById('serverHost').value = session.host;
                document.getElementById('serverPort').value = session.port;
                
                // Attempt to reconnect
                connectToServer(session.username, session.host, session.port);
                
                return true;
            } else {
                console.log('Session expired or disconnected');
                clearSession();
            }
        }
    } catch (error) {
        console.error('Failed to restore session:', error);
    }
    return false;
}

// Add clear messages button functionality (optional - can be added to HTML)
window.clearChatHistory = function() {
    if (confirm('Are you sure you want to clear all chat history from local storage?')) {
        clearMessagesFromStorage();
        alert('Chat history cleared!');
    }
};
