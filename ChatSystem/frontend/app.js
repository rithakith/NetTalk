// Chat Client Frontend Application
// Note: This is a demonstration frontend. For actual connection to the Java server,
// you would need a WebSocket bridge or REST API layer.

let connected = false;
let username = '';
let ws = null; // WebSocket connection (if implemented)

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
    
    // Simulate connection (in real implementation, establish WebSocket connection)
    connectToServer(username, serverHost, serverPort);
});

// Message Form Handler
messageForm.addEventListener('submit', (e) => {
    e.preventDefault();
    
    const message = messageInput.value.trim();
    if (!message) return;
    
    sendMessage(message);
    messageInput.value = '';
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
            
            // Update UI
            connectionPanel.style.display = 'none';
            chatPanel.style.display = 'block';
            currentUsernameSpan.textContent = username;
            
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
            connected = false;
            disconnect();
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
    chatPanel.style.display = 'none';
    connectionPanel.style.display = 'block';
    messageArea.innerHTML = '';
    
    console.log('Disconnected from server');
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

// Initialize
console.log('Chat application loaded');
console.log('Make sure the WebSocket bridge server is running on port 8889');
