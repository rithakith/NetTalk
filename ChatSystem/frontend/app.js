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

// Message tracking variables
let messageMap = new Map(); // Map of messageId to message element
let messageStatusMap = new Map(); // Map of messageId to status
let visibilityObserver = null; // Intersection Observer for message visibility
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
    
    // Show chat interface on first message
    showChatInterface();
    
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
        // Check if it's a quiz command or API command
        const command = message.split(' ')[0].toLowerCase();
        const quizCommands = ['/createquiz', '/addquestion', '/invitequiz', '/startquiz', 
                             '/joinquiz', '/answer', '/quizzes', '/help'];
        
        if (quizCommands.includes(command)) {
            // Quiz command - send as CHAT message for processing
            messageObj.type = 'CHAT';
        } else {
            // API command
            messageObj.type = 'API_REQUEST';
        }
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
            console.log('Received CHAT message:', message);
            console.log('MessageId:', message.messageId, 'Status:', message.status, 'Sender:', message.sender, 'Current user:', username);
            addMessage('CHAT', message.sender, message.content, message.messageId, message.status);
            
            // Send delivery confirmation if not from current user
            if (message.sender !== username && message.messageId) {
                sendMessageDelivered(message.messageId);
            }
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
            // Check for quiz-related system messages
            handleQuizMessage(message);
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
            
        case 'MESSAGE_SEEN':
            // Handle message status update
            try {
                const statusData = JSON.parse(message.content);
                updateMessageStatus(statusData.messageId, statusData.status);
            } catch (e) {
                console.error('Failed to parse message status:', e);
            }
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
            
        // Quiz message types
        case 'QUIZ_CREATED':
            addMessage('QUIZ', message.sender, message.content, null, null, 'quiz-invitation');
            handleQuizMessage(message);
            break;
            
        case 'QUIZ_INVITATION':
            addMessage('QUIZ', message.sender, message.content, null, null, 'quiz-invitation');
            handleQuizMessage(message);
            break;
            
        case 'QUIZ_START':
            addMessage('QUIZ', message.sender, message.content, null, null, 'quiz-start');
            // Show quiz interface or notification
            showQuizNotification('Quiz Started!', message.content);
            break;
            
        case 'QUIZ_QUESTION':
            addMessage('QUIZ', message.sender, message.content, null, null, 'quiz-question');
            // Highlight question in UI
            showQuizQuestion(message.content);
            break;
            
        case 'QUIZ_RESULTS':
            addMessage('QUIZ', message.sender, message.content, null, null, 'quiz-results');
            break;
            
        case 'QUIZ_ENDED':
            addMessage('QUIZ', message.sender, message.content, null, null, 'quiz-ended');
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
        
        // Create or update typing indicator
        if (!typingIndicator) {
            typingIndicator = document.createElement('div');
            typingIndicator.id = 'typingIndicator';
            typingIndicator.className = 'typing-indicator';
            messageArea.appendChild(typingIndicator);
        }
        
        typingIndicator.innerHTML = `<em class="typing-text">${text}</em>`;
        typingIndicator.style.display = 'block';
        
        // Ensure typing indicator is always at the bottom
        ensureTypingIndicatorAtBottom();
        
        // Scroll to bottom to show typing indicator
        messageArea.scrollTop = messageArea.scrollHeight;
    } else {
        if (typingIndicator) {
            typingIndicator.style.display = 'none';
        }
    }
}

/**
 * Ensure typing indicator is always the last element
 */
function ensureTypingIndicatorAtBottom() {
    const typingIndicator = document.getElementById('typingIndicator');
    if (typingIndicator && typingIndicator.parentNode) {
        // Remove and re-append to ensure it's last
        typingIndicator.remove();
        messageArea.appendChild(typingIndicator);
    }
}

/**
 * Show chat interface and hide welcome screen
 */
function showChatInterface() {
    const welcomeScreen = document.getElementById('welcomeScreen');
    const messageArea = document.getElementById('messageArea');
    
    if (welcomeScreen && messageArea) {
        welcomeScreen.classList.add('hidden');
        messageArea.style.display = 'flex';
        messageArea.classList.add('active');
    }
}

/**
 * Add message to chat area
 */
function addMessage(type, sender, content, messageId = null, status = null, quizStyle = null) {
    // Show chat interface when first message arrives
    showChatInterface();
    
    const messageDiv = document.createElement('div');
    
    // Determine message alignment and styling based on sender and type
    let messageClass = 'message ';
    if (type === 'CHAT') {
        messageClass += sender === username ? 'message-sent' : 'message-received';
    } else if (type === 'QUIZ' && quizStyle) {
        messageClass += `message-quiz message-${quizStyle}`;
    } else {
        messageClass += `message-${type.toLowerCase()}`;
    }
    messageDiv.className = messageClass;
    
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
    
    // Add message status indicators for own messages
    if (type === 'CHAT' && sender === username && messageId) {
        console.log('Adding status indicator for message:', messageId, 'status:', status);
        const statusDiv = document.createElement('span');
        statusDiv.className = 'message-status';
        statusDiv.setAttribute('data-message-id', messageId);
        updateStatusIcon(statusDiv, status || 'SENT');
        timeDiv.appendChild(statusDiv);
        
        // Store message reference
        messageMap.set(messageId, messageDiv);
        messageStatusMap.set(messageId, status || 'SENT');
        console.log('Status indicator added successfully');
    }
    
    if (type !== 'SYSTEM') {
        senderDiv.appendChild(timeDiv);
        messageDiv.appendChild(senderDiv);
    }
    messageDiv.appendChild(contentDiv);
    
    // Add the message
    messageArea.appendChild(messageDiv);
    
    // Ensure typing indicator stays at bottom if it exists
    ensureTypingIndicatorAtBottom();
    
    messageArea.scrollTop = messageArea.scrollHeight;
    
    // Set up visibility observer for seen messages (for received messages)
    if (type === 'CHAT' && sender !== username && messageId) {
        setupMessageVisibility(messageDiv, messageId);
    }
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
        } else {
            // Add click handler to start chatting with user
            li.style.cursor = 'pointer';
            li.addEventListener('click', () => {
                showChatInterface();
                // Pre-fill message input with @username
                messageInput.value = `@${user} `;
                messageInput.focus();
            });
            li.title = `Click to send private message to ${user}`;
        }
        userList.appendChild(li);
    });
}

/**
 * Update message status icon
 */
function updateStatusIcon(statusElement, status) {
    statusElement.innerHTML = '';
    
    switch (status) {
        case 'SENT':
            // Single checkmark
            statusElement.innerHTML = ' <span class="status-icon status-sent">✓</span>';
            statusElement.title = 'Sent';
            break;
        case 'DELIVERED':
            // Double checkmark (gray)
            statusElement.innerHTML = ' <span class="status-icon status-delivered">✓✓</span>';
            statusElement.title = 'Delivered';
            break;
        case 'SEEN':
            // Double checkmark (blue)
            statusElement.innerHTML = ' <span class="status-icon status-seen">✓✓</span>';
            statusElement.title = 'Seen';
            break;
    }
}

/**
 * Update message status
 */
function updateMessageStatus(messageId, status) {
    const statusElement = document.querySelector(`[data-message-id="${messageId}"]`);
    if (statusElement) {
        updateStatusIcon(statusElement, status);
        messageStatusMap.set(messageId, status);
        console.log(`Message ${messageId} status updated to ${status}`);
    }
}

/**
 * Send message delivery confirmation
 */
function sendMessageDelivered(messageId) {
    if (ws && ws.readyState === WebSocket.OPEN) {
        try {
            ws.send(JSON.stringify({
                type: 'MESSAGE_DELIVERED',
                sender: username,
                messageId: messageId
            }));
        } catch (error) {
            console.error('Failed to send delivery confirmation:', error);
        }
    }
}

/**
 * Send message seen confirmation
 */
function sendMessageSeen(messageId) {
    if (ws && ws.readyState === WebSocket.OPEN) {
        try {
            ws.send(JSON.stringify({
                type: 'MESSAGE_SEEN',
                sender: username,
                messageId: messageId
            }));
        } catch (error) {
            console.error('Failed to send seen confirmation:', error);
        }
    }
}

/**
 * Setup message visibility observer for "seen" functionality
 */
function setupMessageVisibility(messageElement, messageId) {
    if (!visibilityObserver) {
        visibilityObserver = new IntersectionObserver((entries) => {
            entries.forEach(entry => {
                if (entry.isIntersecting) {
                    const msgId = entry.target.getAttribute('data-visibility-id');
                    if (msgId) {
                        // Message is visible, mark as seen
                        setTimeout(() => {
                            sendMessageSeen(msgId);
                        }, 1000); // Wait 1 second before marking as seen
                        
                        // Stop observing this message
                        visibilityObserver.unobserve(entry.target);
                    }
                }
            });
        }, {
            threshold: 0.5, // 50% of message must be visible
            rootMargin: '0px 0px -50px 0px' // Account for bottom margin
        });
    }
    
    messageElement.setAttribute('data-visibility-id', messageId);
    visibilityObserver.observe(messageElement);
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
}

// ============== Quiz Functions ==============

/**
 * Show quiz notification with special styling
 */
function showQuizNotification(title, content) {
    // Create temporary notification element
    const notification = document.createElement('div');
    notification.className = 'quiz-notification';
    notification.innerHTML = `<strong>${title}</strong><br>${content}`;
    notification.style.cssText = `
        position: fixed;
        top: 20px;
        right: 20px;
        background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
        color: white;
        padding: 15px;
        border-radius: 8px;
        z-index: 1000;
        box-shadow: 0 4px 12px rgba(0,0,0,0.3);
        max-width: 300px;
        animation: slideIn 0.3s ease-out;
    `;
    
    document.body.appendChild(notification);
    
    // Auto remove after 5 seconds
    setTimeout(() => {
        if (notification.parentNode) {
            notification.remove();
        }
    }, 5000);
}

/**
 * Show quiz question with special highlighting
 */
function showQuizQuestion(questionContent) {
    // Scroll to the question message
    const messages = document.querySelectorAll('.message-quiz-question');
    if (messages.length > 0) {
        const lastQuestion = messages[messages.length - 1];
        lastQuestion.scrollIntoView({ behavior: 'smooth', block: 'center' });
        
        // Add pulse effect to highlight the question
        lastQuestion.style.animation = 'pulse 1s ease-in-out 3';
    }
    
    // Show desktop notification if supported
    if ('Notification' in window && Notification.permission === 'granted') {
        new Notification('Quiz Question Available', {
            body: 'A new quiz question is ready!',
            icon: '/favicon.ico'
        });
    }
}

// ==================== QUIZ MANAGEMENT ====================

let activeQuizzes = new Map(); // Store active quizzes
let quizInvitations = new Map(); // Store quiz invitations

/**
 * Toggle quiz notification panel visibility
 */
function toggleQuizPanel() {
    const panel = document.getElementById('quizNotificationPanel');
    if (panel.style.display === 'none') {
        panel.style.display = 'block';
    } else {
        panel.style.display = 'none';
    }
}

/**
 * Refresh quiz list from server
 */
function refreshQuizList() {
    if (connected && ws && ws.readyState === WebSocket.OPEN) {
        sendMessage('/quizzes');
        console.log('Requested quiz list from server');
    } else {
        alert('Not connected to server');
    }
}

/**
 * Handle quiz invitation
 */
function handleQuizInvitation(quizId, quizName, inviter) {
    // Store invitation
    quizInvitations.set(quizId, {
        id: quizId,
        name: quizName,
        inviter: inviter,
        timestamp: Date.now()
    });
    
    // Show notification panel
    showQuizNotification(quizId, quizName, inviter, 'invitation');
    
    // Add to available quizzes
    updateAvailableQuizzes();
}

/**
 * Show quiz notification
 */
function showQuizNotification(quizId, quizName, admin, type = 'active') {
    const notificationPanel = document.getElementById('quizNotificationPanel');
    const notificationList = document.getElementById('quizNotificationList');
    
    // Show panel
    notificationPanel.style.display = 'block';
    
    // Check if notification already exists
    let existingNotification = document.getElementById(`quiz-notif-${quizId}`);
    if (existingNotification) {
        return; // Don't add duplicate
    }
    
    // Create notification item
    const notificationItem = document.createElement('div');
    notificationItem.id = `quiz-notif-${quizId}`;
    notificationItem.className = `quiz-notification-item ${type}`;
    
    const badgeText = type === 'invitation' ? 'INVITATION' : 
                      type === 'running' ? 'IN PROGRESS' : 'AVAILABLE';
    const badgeClass = type === 'invitation' ? 'invitation' : 
                       type === 'running' ? 'running' : 'active';
    
    notificationItem.innerHTML = `
        <div class="quiz-item-header">
            <div class="quiz-item-title">🎯 ${quizName}</div>
            <span class="quiz-item-badge ${badgeClass}">${badgeText}</span>
        </div>
        <div class="quiz-item-info">
            ${type === 'invitation' ? `Invited by: ${admin}` : `Created by: ${admin}`}
            <br>Quiz ID: ${quizId}
        </div>
        <div class="quiz-item-actions">
            <button class="btn-join-quiz" onclick="joinQuiz('${quizId}')">
                Join Quiz
            </button>
            <button class="btn-view-quiz" onclick="viewQuizDetails('${quizId}')">
                View Details
            </button>
        </div>
    `;
    
    if (type === 'invitation') {
        notificationItem.classList.add('quiz-invitation-pulse');
    }
    
    notificationList.appendChild(notificationItem);
}

/**
 * Join a quiz
 */
function joinQuiz(quizId) {
    if (connected && ws && ws.readyState === WebSocket.OPEN) {
        // Show chat interface when joining a quiz
        showChatInterface();
        
        sendMessage(`/joinquiz ${quizId}`);
        
        // Remove notification after joining
        const notification = document.getElementById(`quiz-notif-${quizId}`);
        if (notification) {
            notification.style.animation = 'fadeOut 0.3s';
            setTimeout(() => {
                if (notification.parentNode) {
                    notification.remove();
                }
            }, 300);
        }
        
        addMessage('SYSTEM', 'You', `Joined quiz ${quizId}`);
    } else {
        alert('Not connected to server');
    }
}

/**
 * View quiz details
 */
function viewQuizDetails(quizId) {
    // Request quiz details from server
    if (connected && ws && ws.readyState === WebSocket.OPEN) {
        sendMessage(`/quizinfo ${quizId}`);
    } else {
        alert('Not connected to server');
    }
}

/**
 * Update available quizzes list in sidebar
 */
function updateAvailableQuizzes() {
    const container = document.getElementById('availableQuizzes');
    
    if (activeQuizzes.size === 0 && quizInvitations.size === 0) {
        container.innerHTML = '<p class="quiz-placeholder">No active quizzes</p>';
        return;
    }
    
    container.innerHTML = '';
    
    // Add invitations first
    quizInvitations.forEach((quiz, quizId) => {
        const quizItem = createQuizListItem(quiz, 'invitation');
        container.appendChild(quizItem);
    });
    
    // Add active quizzes
    activeQuizzes.forEach((quiz, quizId) => {
        // Don't duplicate if already in invitations
        if (!quizInvitations.has(quizId)) {
            const quizItem = createQuizListItem(quiz, quiz.status || 'created');
            container.appendChild(quizItem);
        }
    });
}

/**
 * Create quiz list item element
 */
function createQuizListItem(quiz, status) {
    const item = document.createElement('div');
    item.className = 'quiz-list-item';
    item.id = `quiz-list-${quiz.id}`;
    
    const statusText = status === 'invitation' ? 'INVITED' :
                       status === 'running' ? 'IN PROGRESS' :
                       status === 'completed' ? 'COMPLETED' : 'AVAILABLE';
    const statusClass = status === 'invitation' ? 'created' :
                        status === 'running' ? 'running' : 
                        status === 'completed' ? 'completed' : 'created';
    
    item.innerHTML = `
        <div class="quiz-list-item-header">
            <div class="quiz-list-item-title">${quiz.name}</div>
            <span class="quiz-list-item-status ${statusClass}">${statusText}</span>
        </div>
        <div class="quiz-list-item-info">
            ${quiz.inviter ? `Invited by: ${quiz.inviter}` : 
              quiz.admin ? `By: ${quiz.admin}` : `Quiz ID: ${quiz.id}`}
            ${quiz.questions ? ` • ${quiz.questions} questions` : ''}
        </div>
        <div class="quiz-list-item-actions">
            <button class="btn-quiz-action btn-quiz-join" onclick="joinQuiz('${quiz.id}')">
                Join
            </button>
            <button class="btn-quiz-action btn-quiz-view" onclick="viewQuizDetails('${quiz.id}')">
                Details
            </button>
        </div>
    `;
    
    return item;
}

/**
 * Parse quiz list from server response
 */
function parseQuizList(content) {
    // Clear existing quizzes
    activeQuizzes.clear();
    
    // Parse lines like "- Quiz Name (ID: xyz) - Admin: username - State: CREATED - Questions: 0"
    const lines = content.split('\n');
    for (let line of lines) {
        const match = line.match(/- (.+) \(ID: ([a-zA-Z0-9]+)\) - Admin: (.+) - State: ([A-Z]+) - Questions: (\d+)/);
        if (match) {
            const quizName = match[1];
            const quizId = match[2];
            const adminName = match[3];
            const state = match[4].toLowerCase();
            const questionCount = parseInt(match[5]);
            
            activeQuizzes.set(quizId, {
                id: quizId,
                name: quizName,
                admin: adminName,
                status: state,
                questions: questionCount
            });
        }
    }
    
    updateAvailableQuizzes();
    console.log(`Loaded ${activeQuizzes.size} active quizzes`);
}

/**
 * Enhanced message handler for quiz-related messages
 */
function handleQuizMessage(message) {
    const content = message.content;
    
    // Check for quiz creation
    if (content.includes('Quiz') && content.includes('created successfully')) {
        const match = content.match(/Quiz '(.+)' created successfully! Quiz ID: ([a-zA-Z0-9]+)/);
        if (match) {
            const quizName = match[1];
            const quizId = match[2];
            
            activeQuizzes.set(quizId, {
                id: quizId,
                name: quizName,
                admin: message.sender,
                status: 'created',
                questions: 0
            });
            
            updateAvailableQuizzes();
        }
    }
    
    // Check for quiz invitation
    else if (content.includes('invited') && content.includes('quiz')) {
        const match = content.match(/invited to quiz '(.+)' \(ID: ([a-zA-Z0-9]+)\)/);
        if (match) {
            const quizName = match[1];
            const quizId = match[2];
            
            handleQuizInvitation(quizId, quizName, message.sender);
        }
    }
    
    // Check for quiz list response
    else if (content.includes('Active Quizzes:')) {
        parseQuizList(content);
    }
    
    // Check for quiz started
    else if (content.includes('Quiz') && content.includes('has started')) {
        const match = content.match(/Quiz '(.+)' \(ID: ([a-zA-Z0-9]+)\) has started/);
        if (match) {
            const quizName = match[1];
            const quizId = match[2];
            
            // Update quiz status
            const quiz = activeQuizzes.get(quizId);
            if (quiz) {
                quiz.status = 'running';
                updateAvailableQuizzes();
                showQuizNotification(quizId, quizName, quiz.admin, 'running');
            }
        }
    }
}

// Auto-refresh quiz list on connection
const originalConnectFormSubmit = connectForm.onsubmit;
connectForm.addEventListener('submit', (e) => {
    // After connection is established, refresh quiz list
    setTimeout(() => {
        if (connected) {
            refreshQuizList();
        }
    }, 2000);
});

