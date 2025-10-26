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
 * Connect to chat server
 * NOTE: This is a simulation. For real implementation, use WebSocket or implement a bridge
 */
function connectToServer(user, host, port) {
    console.log(`Connecting to ${host}:${port} as ${user}...`);
    
    // Simulate successful connection
    setTimeout(() => {
        connected = true;
        username = user;
        
        // Update UI
        connectionPanel.style.display = 'none';
        chatPanel.style.display = 'block';
        currentUsernameSpan.textContent = username;
        
        // Add system message
        addMessage('SYSTEM', 'Server', `Welcome to the chat, ${username}!`);
        addMessage('SYSTEM', 'Server', 'Connected successfully. This is a demo interface.');
        addMessage('SYSTEM', 'Info', 'To actually connect to the Java server, run the ChatClient.java console application or implement a WebSocket bridge.');
        
        // Simulate some users
        updateUserList(['Alice', 'Bob', username]);
        
        // Demo messages
        setTimeout(() => {
            addMessage('CHAT', 'Alice', 'Hello everyone!');
        }, 1000);
        
        setTimeout(() => {
            addMessage('CHAT', 'Bob', 'Welcome to the chat!');
        }, 2000);
        
    }, 500);
    
    /* 
     * REAL IMPLEMENTATION EXAMPLE (if you implement WebSocket bridge):
     * 
     * ws = new WebSocket(`ws://${host}:${port}/chat`);
     * 
     * ws.onopen = () => {
     *     console.log('Connected to server');
     *     ws.send(JSON.stringify({type: 'CONNECT', sender: username}));
     *     // Update UI...
     * };
     * 
     * ws.onmessage = (event) => {
     *     const message = JSON.parse(event.data);
     *     handleMessage(message);
     * };
     * 
     * ws.onerror = (error) => {
     *     console.error('WebSocket error:', error);
     *     alert('Connection error!');
     * };
     * 
     * ws.onclose = () => {
     *     connected = false;
     *     // Update UI...
     * };
     */
}

/**
 * Send message to server
 */
function sendMessage(message) {
    if (!connected) {
        alert('Not connected to server!');
        return;
    }
    
    console.log('Sending message:', message);
    
    // Handle different message types
    if (message.startsWith('@')) {
        // Private message
        const spaceIndex = message.indexOf(' ');
        if (spaceIndex > 0) {
            const receiver = message.substring(1, spaceIndex);
            const content = message.substring(spaceIndex + 1);
            addMessage('CHAT', username, `@${receiver}: ${content}`);
            addMessage('SYSTEM', 'Server', `Private message sent to ${receiver}`);
        }
    } else if (message.startsWith('/')) {
        // API command
        addMessage('CHAT', username, message);
        
        // Simulate API response
        setTimeout(() => {
            simulateApiResponse(message);
        }, 500);
    } else {
        // Regular chat message
        addMessage('CHAT', username, message);
    }
    
    /* 
     * REAL IMPLEMENTATION:
     * ws.send(JSON.stringify({
     *     type: message.startsWith('/') ? 'API_REQUEST' : 'CHAT',
     *     sender: username,
     *     content: message
     * }));
     */
}

/**
 * Disconnect from server
 */
function disconnect() {
    if (ws) {
        ws.close();
    }
    
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
    
    if (type !== 'SYSTEM') {
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

/**
 * Simulate API response (for demo purposes)
 */
function simulateApiResponse(command) {
    let response = '';
    
    if (command.startsWith('/weather')) {
        response = `🌤️ Weather for London:\nLondon: ⛅️  +15°C`;
    } else if (command === '/joke') {
        response = `😄 Random Joke:\nWhy do programmers prefer dark mode?\nBecause light attracts bugs!`;
    } else if (command === '/quote') {
        response = `💭 Quote of the moment:\n"The only way to do great work is to love what you do."\n- Steve Jobs`;
    } else if (command === '/help') {
        response = `🤖 Available API Commands:
/weather <city> - Get current weather for a city
/joke - Get a random joke
/quote - Get an inspirational quote
/help - Show this help message`;
    } else {
        response = 'Unknown command. Type /help for available commands.';
    }
    
    addMessage('API', 'API Bot', response);
}

// Initialize
console.log('Chat application loaded');
console.log('To actually connect to the Java server, use the ChatClient.java console application');
