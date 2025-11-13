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

// Quiz tracking variables
let currentQuiz = null; // Currently active quiz
let quizAnswers = new Map(); // Map of questionNumber to selectedOption (0-3)
let quizQuestions = []; // Array of quiz questions
let joinedQuizId = null; // ID of quiz user has joined

// Private chat tracking variables
let privateChats = new Map(); // Map of username to array of messages
let currentPrivateChat = null; // Currently active private chat username
let chatView = 'public'; // 'public' or 'private'
let onlineUsers = []; // List of currently online users

// Local Storage keys
const STORAGE_KEYS = {
    MESSAGES: 'chatMessages',
    PRIVATE_CHATS: 'privateChats',
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
            // Load private chats for this user (persisted)
            loadPrivateChatsFromStorage();
            // Refresh user list so unread badges appear
            updateUserList(onlineUsers);
            
            // Add system message
            addMessage('SYSTEM', 'Server', `Connecting as ${username}...`);
        };
        
        ws.onmessage = (event) => {
            try {
                const message = JSON.parse(event.data);
                console.log('[WebSocket] Received message:', message.type, message);
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
    
    // If in private chat view, automatically send as private message
    if (chatView === 'private' && currentPrivateChat) {
        const messageId = `msg_${Date.now()}_${Math.random()}`;
        messageObj = {
            type: 'PRIVATE_MSG',
            sender: username,
            receiver: currentPrivateChat,
            content: message,
            messageId: messageId
        };
        
        // Add to local history immediately with messageId
        addPrivateMessageToHistory(currentPrivateChat, message, username, messageId);
    }
    // Handle different message types for public chat
    else if (message.startsWith('@')) {
        // Private message in public chat
        const spaceIndex = message.indexOf(' ');
        if (spaceIndex > 0) {
            const receiver = message.substring(1, spaceIndex);
            const content = message.substring(spaceIndex + 1);
            const messageId = `msg_${Date.now()}_${Math.random()}`;
            messageObj = {
                type: 'PRIVATE_MSG',
                sender: username,
                receiver: receiver,
                content: content,
                messageId: messageId
            };
            
            // Add to local history with messageId
            addPrivateMessageToHistory(receiver, content, username, messageId);
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
                // Determine the other user in the conversation
                const otherUser = message.sender === username ? message.receiver : message.sender;
                
                // Add to private chat history if sent by the other user
                if (message.sender !== username) {
                    addPrivateMessageToHistory(otherUser, message.content, message.sender, message.messageId);
                    
                    // Send delivery confirmation
                    if (message.messageId) {
                        sendPrivateMessageDelivered(message.messageId, otherUser);
                    }
                }
                
                // Only show in public chat if not in private chat view
                if (chatView !== 'private') {
                    addMessage('PRIVATE', message.sender, 
                        `@${message.receiver}: ${message.content}`);
                }
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
            
        case 'MESSAGE_SEEN':
            // Handle message status update
            try {
                const statusData = JSON.parse(message.content);
                updateMessageStatus(statusData.messageId, statusData.status);
                // Also update private message status
                updatePrivateMessageStatus(statusData.messageId, statusData.status);
            } catch (e) {
                console.error('Failed to parse message status:', e);
            }
            break;
            
        case 'USER_LIST':
            if (message.content) {
                try {
                    // Parse "Online users: user1, user2, user3" format
                    let users = [];
                    if (message.content.startsWith('Online users: ')) {
                        const userListStr = message.content.replace('Online users: ', '');
                        users = userListStr.split(', ').filter(u => u.trim().length > 0);
                    } else {
                        // Try parsing as JSON (for future compatibility)
                        users = JSON.parse(message.content);
                    }
                    updateUserList(users);
                } catch (e) {
                    console.error('Failed to parse user list:', e);
                    console.log('Raw content:', message.content);
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
            console.log('[WebSocket] Received QUIZ_START message:', message.content);
            // Don't add to chat - quiz interface will be displayed
            // addMessage('QUIZ', message.sender, message.content, null, null, 'quiz-start');
            // Parse and display quiz interface
            parseAndDisplayQuiz(message.content);
            break;
            
        case 'QUIZ_QUESTION':
            console.log('[WebSocket] Received QUIZ_QUESTION message:', message.content);
            // Don't add to chat - questions should only appear in quiz interface
            // addMessage('QUIZ', message.sender, message.content, null, null, 'quiz-question');
            // Add question to current quiz
            addQuestionToQuiz(message.content);
            break;
            
        case 'QUIZ_RESULTS':
            addMessage('QUIZ', message.sender, message.content, null, null, 'quiz-results');
            // Display results in quiz container if still active
            displayQuizResults(message.content);
            break;
            
        case 'QUIZ_ENDED':
            addMessage('QUIZ', message.sender, message.content, null, null, 'quiz-ended');
            break;
            
        case 'QUIZ_DELETED':
            addMessage('QUIZ', message.sender, message.content, null, null, 'quiz-deleted');
            handleQuizDeleted(message);
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
    
    // Hide admin panel
    hideAdminPanel();
    
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
    
    // Update private chat typing indicator if in private chat with this user
    if (chatView === 'private' && currentPrivateChat === user) {
        updatePrivateTypingIndicator(user);
    }
}

/**
 * Remove user from typing indicator
 */
function removeTypingUser(user) {
    typingUsers.delete(user);
    updateTypingIndicator();
    
    // Update private chat typing indicator if in private chat with this user
    if (chatView === 'private' && currentPrivateChat === user) {
        updatePrivateTypingIndicator(user);
    }
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
    onlineUsers = users; // Store globally
    userList.innerHTML = '';
    users.forEach(user => {
        const li = document.createElement('li');
        
        // Create user item with unread indicator
        const userItem = document.createElement('div');
        userItem.style.display = 'flex';
        userItem.style.alignItems = 'center';
        userItem.style.justifyContent = 'space-between';
        userItem.style.width = '100%';
        
        const userName = document.createElement('span');
        userName.textContent = user;
        userItem.appendChild(userName);
        
        // Add unread badge if there are unread messages
        const unreadCount = getUnreadPrivateMessageCount(user);
        if (unreadCount > 0 && user !== username) {
            const badge = document.createElement('span');
            badge.className = 'unread-badge';
            badge.textContent = unreadCount;
            userItem.appendChild(badge);
        }
        
        li.appendChild(userItem);
        
        if (user === username) {
            li.style.fontWeight = 'bold';
            li.style.color = '#667eea';
        } else {
            // Add click handler to open private chat
            li.style.cursor = 'pointer';
            li.className = currentPrivateChat === user ? 'user-item-active' : 'user-item';
            li.addEventListener('click', () => {
                openPrivateChat(user);
            });
            li.title = `Click to chat privately with ${user}`;
        }
        userList.appendChild(li);
    });
}

/**
 * Get unread private message count for a user
 */
function getUnreadPrivateMessageCount(user) {
    const chatMessages = privateChats.get(user);
    if (!chatMessages) return 0;
    return chatMessages.filter(msg => !msg.read && msg.sender === user).length;
}

/**
 * Open private chat with a specific user
 */
function openPrivateChat(targetUser) {
    console.log(`Opening private chat with ${targetUser}`);
    
    currentPrivateChat = targetUser;
    chatView = 'private';
    
    // Initialize private chat history if not exists
    if (!privateChats.has(targetUser)) {
        privateChats.set(targetUser, []);
    }
    
    // Mark all messages from this user as read
    markPrivateChatAsRead(targetUser);
    
    // Show chat interface and update display
    showChatInterface();
    displayPrivateChatInterface(targetUser);
    
    // Update user list to highlight active chat
    updateUserList(onlineUsers);
    
    // Focus message input
    messageInput.focus();
}

/**
 * Close private chat and return to public chat
 */
function closePrivateChat() {
    console.log('Closing private chat');
    
    currentPrivateChat = null;
    chatView = 'public';
    
    // Restore public message area
    displayPublicChatInterface();
    
    // Update user list
    updateUserList(onlineUsers);
    
    messageInput.focus();
}

/**
 * Display private chat interface
 */
function displayPrivateChatInterface(targetUser) {
    // Hide welcome screen if visible
    const welcomeScreen = document.getElementById('welcomeScreen');
    if (welcomeScreen) {
        welcomeScreen.style.display = 'none';
    }
    
    // Clear and show message area
    messageArea.innerHTML = '';
    messageArea.style.display = 'block';
    
    // Add private chat header
    const chatHeader = document.createElement('div');
    chatHeader.className = 'private-chat-header';
    chatHeader.innerHTML = `
        <button class="btn-back-to-public" onclick="closePrivateChat()">
            ← Back to Public Chat
        </button>
        <div class="private-chat-title">
            <span class="private-chat-icon">💬</span>
            <strong>Private Chat with ${targetUser}</strong>
        </div>
    `;
    messageArea.appendChild(chatHeader);
    
    // Add private messages container
    const messagesContainer = document.createElement('div');
    messagesContainer.className = 'private-messages-container';
    messagesContainer.id = 'privateMessagesContainer';
    messageArea.appendChild(messagesContainer);
    
    // Load and display existing messages
    updatePrivateMessageArea(targetUser);
    
    // Update placeholder
    messageInput.placeholder = `Message ${targetUser}...`;
}

/**
 * Display public chat interface
 */
function displayPublicChatInterface() {
    // Clear message area
    messageArea.innerHTML = '';
    
    // Reload public messages (you might want to store these separately)
    // For now, just show welcome screen
    const welcomeScreen = document.getElementById('welcomeScreen');
    if (welcomeScreen) {
        welcomeScreen.style.display = 'flex';
    }
    messageArea.style.display = 'none';
    
    // Update placeholder
    messageInput.placeholder = 'Type a message to start chatting...';
}

/**
 * Update private message area with messages
 */
function updatePrivateMessageArea(targetUser) {
    const container = document.getElementById('privateMessagesContainer');
    if (!container) return;
    
    const messages = privateChats.get(targetUser) || [];
    
    if (messages.length === 0) {
        container.innerHTML = `
            <div class="private-chat-empty">
                <div class="empty-icon">💭</div>
                <p>No messages yet</p>
                <p class="empty-hint">Start the conversation by typing a message below</p>
            </div>
        `;
        return;
    }
    
    container.innerHTML = '';
    messages.forEach(msg => {
        const messageDiv = document.createElement('div');
        // Use same class structure as public chat
        messageDiv.className = `message ${msg.sender === username ? 'message-sent' : 'message-received'}`;
        
        const senderDiv = document.createElement('div');
        senderDiv.className = 'message-sender';
        senderDiv.textContent = msg.sender;
        
        const contentDiv = document.createElement('div');
        contentDiv.className = 'message-content';
        contentDiv.textContent = msg.content;
        
        // Add timestamp
        const timeDiv = document.createElement('span');
        timeDiv.className = 'message-time';
        const timestamp = new Date(msg.timestamp).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' });
        timeDiv.textContent = ` (${timestamp})`;
        
        // Add status indicators for sent messages (like public chat)
        if (msg.sender === username && msg.messageId) {
            const statusDiv = document.createElement('span');
            statusDiv.className = 'message-status';
            statusDiv.setAttribute('data-message-id', msg.messageId);
            updateStatusIcon(statusDiv, msg.status || 'SENT');
            timeDiv.appendChild(statusDiv);
        }
        
        senderDiv.appendChild(timeDiv);
        messageDiv.appendChild(senderDiv);
        messageDiv.appendChild(contentDiv);
        
        container.appendChild(messageDiv);
    });
    
    // Add typing indicator if other user is typing
    updatePrivateTypingIndicator(targetUser);
    
    // Scroll to bottom
    container.scrollTop = container.scrollHeight;
}

/**
 * Update typing indicator for private chat
 */
function updatePrivateTypingIndicator(targetUser) {
    const container = document.getElementById('privateMessagesContainer');
    if (!container) return;
    
    let typingIndicator = document.getElementById('privateTypingIndicator');
    
    // Check if the target user is typing
    if (typingUsers.has(targetUser)) {
        if (!typingIndicator) {
            typingIndicator = document.createElement('div');
            typingIndicator.id = 'privateTypingIndicator';
            typingIndicator.className = 'typing-indicator';
        }
        
        typingIndicator.innerHTML = `<em class="typing-text">${targetUser} is typing...</em>`;
        typingIndicator.style.display = 'block';
        
        // Ensure it's at the bottom
        if (typingIndicator.parentNode !== container) {
            container.appendChild(typingIndicator);
        }
        
        container.scrollTop = container.scrollHeight;
    } else {
        if (typingIndicator && typingIndicator.parentNode === container) {
            typingIndicator.remove();
        }
    }
}

/**
 * Mark private chat as read
 */
function markPrivateChatAsRead(targetUser) {
    const messages = privateChats.get(targetUser);
    if (messages) {
        messages.forEach(msg => {
            if (msg.sender === targetUser) {
                msg.read = true;
                // Send seen confirmation for unread messages
                if (msg.messageId && msg.status !== 'SEEN') {
                    sendPrivateMessageSeen(msg.messageId, targetUser);
                    msg.status = 'SEEN';
                }
            }
        });
        // Persist change
        savePrivateChatsToStorage();
    }
}

/**
 * Add private message to chat history
 */
function addPrivateMessageToHistory(targetUser, message, sender, messageId = null) {
    if (!privateChats.has(targetUser)) {
        privateChats.set(targetUser, []);
    }
    
    const chatMessages = privateChats.get(targetUser);
    const msgObj = {
        sender: sender,
        content: message,
        timestamp: new Date().toISOString(),
        read: sender === username || (chatView === 'private' && currentPrivateChat === targetUser),
        messageId: messageId || `private_${Date.now()}_${Math.random()}`,
        status: sender === username ? 'SENT' : null,
        deliveredTo: new Set(),
        seenBy: new Set()
    };
    
    chatMessages.push(msgObj);
    
    // Update display if this is the active chat
    if (chatView === 'private' && currentPrivateChat === targetUser) {
        updatePrivateMessageArea(targetUser);
    }
    
    // Update user list to show unread badge
    updateUserList(onlineUsers);

    // Persist private chats
    savePrivateChatsToStorage();
    
    return msgObj;
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
 * Update private message status
 */
function updatePrivateMessageStatus(messageId, status) {
    // Find message in private chats and update status
    let updated = false;
    privateChats.forEach((messages, user) => {
        messages.forEach(msg => {
            if (msg.messageId === messageId) {
                msg.status = status;
                updated = true;
            }
        });
    });
    
    if (updated) {
        // Update display if in active private chat
        if (chatView === 'private' && currentPrivateChat) {
            updatePrivateMessageArea(currentPrivateChat);
        }
        // Persist changes
        savePrivateChatsToStorage();
    }
    
    // Also update DOM element if visible
    const statusElement = document.querySelector(`[data-message-id="${messageId}"]`);
    if (statusElement) {
        updateStatusIcon(statusElement, status);
    }
}

/**
 * Send private message delivery confirmation
 */
function sendPrivateMessageDelivered(messageId, fromUser) {
    if (ws && ws.readyState === WebSocket.OPEN) {
        try {
            ws.send(JSON.stringify({
                type: 'MESSAGE_DELIVERED',
                sender: username,
                receiver: fromUser,
                messageId: messageId
            }));
            console.log(`Sent delivery confirmation for private message ${messageId}`);
        } catch (error) {
            console.error('Failed to send private message delivery confirmation:', error);
        }
    }
}

/**
 * Send private message seen confirmation
 */
function sendPrivateMessageSeen(messageId, fromUser) {
    if (ws && ws.readyState === WebSocket.OPEN) {
        try {
            ws.send(JSON.stringify({
                type: 'MESSAGE_SEEN',
                sender: username,
                receiver: fromUser,
                messageId: messageId
            }));
            console.log(`Sent seen confirmation for private message ${messageId}`);
        } catch (error) {
            console.error('Failed to send private message seen confirmation:', error);
        }
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
function savePrivateChatsToStorage() {
    try {
        if (!username) return;
        const data = {};
        privateChats.forEach((msgs, user) => {
            data[user] = msgs;
        });
        const key = `${STORAGE_KEYS.PRIVATE_CHATS}_${username}`;
        localStorage.setItem(key, JSON.stringify(data));
        console.log('Private chats saved to local storage');
    } catch (error) {
        console.error('Failed to save private chats to local storage:', error);
    }
}

function loadPrivateChatsFromStorage() {
    try {
        if (!username) return;
        const key = `${STORAGE_KEYS.PRIVATE_CHATS}_${username}`;
        const raw = localStorage.getItem(key);
        if (!raw) return;
        const obj = JSON.parse(raw);
        privateChats.clear();
        Object.keys(obj).forEach(user => {
            // Ensure message array exists
            const arr = Array.isArray(obj[user]) ? obj[user] : [];
            privateChats.set(user, arr);
        });
        console.log('Private chats loaded from local storage');
    } catch (error) {
        console.error('Failed to load private chats from local storage:', error);
    }
}

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

/**
 * Make private chat functions globally accessible
 */
window.openPrivateChat = openPrivateChat;
window.closePrivateChat = closePrivateChat;

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

// Quiz localStorage keys
const QUIZ_STORAGE_KEY = 'chatActiveQuizzes';

/**
 * Load active quizzes from localStorage
 */
function loadQuizzesFromStorage() {
    try {
        const savedQuizzes = localStorage.getItem(QUIZ_STORAGE_KEY);
        if (savedQuizzes) {
            const quizArray = JSON.parse(savedQuizzes);
            activeQuizzes.clear();
            quizArray.forEach(quiz => {
                activeQuizzes.set(quiz.id, quiz);
            });
            console.log(`Loaded ${activeQuizzes.size} quizzes from storage`);
            updateAvailableQuizzes();
        }
    } catch (error) {
        console.error('Failed to load quizzes from storage:', error);
    }
}

/**
 * Save active quizzes to localStorage
 */
function saveQuizzesToStorage() {
    try {
        const quizArray = Array.from(activeQuizzes.values());
        localStorage.setItem(QUIZ_STORAGE_KEY, JSON.stringify(quizArray));
    } catch (error) {
        console.error('Failed to save quizzes to storage:', error);
    }
}

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
        
        // Store joined quiz ID
        joinedQuizId = quizId;
        
        // Get quiz info from activeQuizzes
        const quiz = activeQuizzes.get(quizId) || quizInvitations.get(quizId);
        const quizName = quiz ? quiz.name : 'Quiz';
        
        // Show waiting screen
        displayWaitingScreen(quizName, quizId);
        
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
        
        addMessage('SYSTEM', 'You', `Joining quiz ${quizName}...`);
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
    // Don't clear existing quizzes, merge with server data
    const serverQuizzes = new Map();
    
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
            
            serverQuizzes.set(quizId, {
                id: quizId,
                name: quizName,
                admin: adminName,
                status: state,
                questions: questionCount
            });
        }
    }
    
    // Merge server quizzes with local quizzes
    serverQuizzes.forEach((quiz, id) => {
        activeQuizzes.set(id, quiz);
    });
    
    saveQuizzesToStorage();
    updateAvailableQuizzes();
    console.log(`Loaded ${serverQuizzes.size} quizzes from server, total: ${activeQuizzes.size}`);
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
            
            saveQuizzesToStorage();
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
                saveQuizzesToStorage();
                updateAvailableQuizzes();
                showQuizNotification(quizId, quizName, quiz.admin, 'running');
            }
        }
    }
}

/**
 * Handle quiz deleted notification
 */
function handleQuizDeleted(message) {
    const content = message.content;
    console.log('🗑️ Received quiz deletion notification:', content);
    
    // Extract quiz ID from message: "Quiz 'name' (ID: quiz_id) has been deleted"
    const match = content.match(/\(ID: ([a-zA-Z0-9_]+)\)/);
    if (match) {
        const quizId = match[1];
        console.log('📝 Extracted quiz ID:', quizId);
        console.log('📊 Active quizzes before deletion:', Array.from(activeQuizzes.keys()));
        
        // Remove from active quizzes
        const wasInActive = activeQuizzes.delete(quizId);
        console.log('🔴 Removed from activeQuizzes:', wasInActive);
        
        // Remove from invitations
        const wasInInvitations = quizInvitations.delete(quizId);
        console.log('🔴 Removed from invitations:', wasInInvitations);
        
        // Save to storage
        saveQuizzesToStorage();
        console.log('💾 Saved to storage. Remaining quizzes:', activeQuizzes.size);
        
        // Update UI - remove from quiz list
        updateAvailableQuizzes();
        console.log('🔄 Updated available quizzes UI');
        
        // Remove quiz list item from DOM
        const quizListItem = document.getElementById('quiz-list-' + quizId);
        if (quizListItem) {
            quizListItem.remove();
            console.log('🗑️ Removed quiz list item from DOM');
        } else {
            console.log('⚠️ Quiz list item not found in DOM');
        }
        
        // Remove notification if exists
        const notification = document.getElementById('quiz-notif-' + quizId);
        if (notification) {
            notification.remove();
            console.log('🗑️ Removed notification from DOM');
        }
        
        console.log('✅ Quiz deleted and removed from UI:', quizId);
        console.log('📊 Active quizzes after deletion:', Array.from(activeQuizzes.keys()));
    } else {
        console.error('❌ Failed to extract quiz ID from deletion message:', content);
    }
}

// Auto-refresh quiz list on connection
const originalConnectFormSubmit = connectForm.onsubmit;
connectForm.addEventListener('submit', (e) => {
    // Load quizzes from storage first
    loadQuizzesFromStorage();
    
    // After connection is established, refresh quiz list from server
    setTimeout(() => {
        if (connected) {
            refreshQuizList();
        }
    }, 2000);
});

/**
 * Display waiting screen after joining quiz
 */
function displayWaitingScreen(quizName, quizId) {
    const messageArea = document.getElementById('messageArea');
    
    // Create waiting container
    const waitingContainer = document.createElement('div');
    waitingContainer.id = 'activeQuizContainer';
    waitingContainer.className = 'active-quiz-container waiting';
    
    waitingContainer.innerHTML = `
        <div class="quiz-waiting-screen">
            <div class="waiting-icon">⏳</div>
            <h2>Joined: ${quizName}</h2>
            <p class="waiting-message">Waiting for admin to start the quiz...</p>
            <div class="waiting-info">
                <p>📝 Quiz ID: <code>${quizId}</code></p>
                <p>👥 You're in! The quiz will begin shortly.</p>
            </div>
            <button class="btn-cancel-quiz" onclick="leaveQuiz()">
                <span>🚪</span> Leave Quiz
            </button>
        </div>
    `;
    
    messageArea.innerHTML = '';
    messageArea.appendChild(waitingContainer);
}

/**
 * Leave quiz before it starts
 */
function leaveQuiz() {
    if (confirm('Are you sure you want to leave this quiz?')) {
        joinedQuizId = null;
        currentQuiz = null;
        backToChat();
    }
}

/**
 * Parse quiz start message and display quiz interface
 */
function parseAndDisplayQuiz(content) {
    console.log('[Quiz] parseAndDisplayQuiz called with content:', content);
    console.log('[Quiz] Content type:', typeof content);
    console.log('[Quiz] Content length:', content ? content.length : 'null/undefined');
    
    // Example format: "Quiz 'Quiz Name' has started! Get ready!"
    const match = content.match(/Quiz '(.+?)' has started/);
    
    console.log('[Quiz] Regex match result:', match);
    
    if (match) {
        const quizName = match[1];
        
        console.log('[Quiz] ✓ Successfully parsed quiz name:', quizName);
        console.log('[Quiz] Current joinedQuizId:', joinedQuizId);
        
        // If we don't have a joinedQuizId, try to find it from activeQuizzes
        if (!joinedQuizId) {
            console.log('[Quiz] No joinedQuizId, searching in activeQuizzes...');
            for (const [id, quiz] of activeQuizzes.entries()) {
                if (quiz.name === quizName) {
                    joinedQuizId = id;
                    console.log('[Quiz] Found quiz ID from active quizzes:', joinedQuizId);
                    break;
                }
            }
            if (!joinedQuizId) {
                console.error('[Quiz] ERROR: Could not find quiz ID for quiz:', quizName);
            }
        }
        
        // Get quiz info including total question count
        const quizInfo = activeQuizzes.get(joinedQuizId);
        const totalQuestions = quizInfo ? quizInfo.questions : 0;
        
        console.log('[Quiz] Total questions in quiz:', totalQuestions);
        
        currentQuiz = {
            id: joinedQuizId,
            name: quizName,
            questions: [],
            totalQuestions: totalQuestions,  // Store total question count
            startTime: new Date()
        };
        quizAnswers.clear();
        quizQuestions = [];
        
        console.log('[Quiz] ✓ Quiz state initialized');
        console.log('[Quiz] currentQuiz:', currentQuiz);
        
        // Show chat interface
        console.log('[Quiz] Calling showChatInterface()...');
        showChatInterface();
        
        // Display quiz header
        console.log('[Quiz] Calling displayQuizInterface(' + quizName + ')...');
        displayQuizInterface(quizName);
        
        console.log('[Quiz] ✓ parseAndDisplayQuiz completed successfully');
    } else {
        console.error('[Quiz] ✗ FAILED to parse quiz start message!');
        console.error('[Quiz] Message content:', content);
        console.error('[Quiz] Expected format: "Quiz \'Name\' has started"');
        console.error('[Quiz] Trying to extract quiz name anyway...');
        
        // Try alternative parsing
        const altMatch = content.match(/Quiz ["'](.+?)["']/);
        if (altMatch) {
            console.warn('[Quiz] Found quiz name using alternative regex:', altMatch[1]);
        }
    }
}

/**
 * Add a question to the current quiz
 */
function addQuestionToQuiz(content) {
    console.log('[Quiz] Received question:', content);
    
    // Parse question format: "Question 1: What is...? Options: A) ..., B) ..., C) ..., D) ..."
    const questionMatch = content.match(/Question (\d+): (.+?)\? Options: (.+)/);
    
    if (!questionMatch) {
        console.error('[Quiz] Failed to parse question format:', content);
        return;
    }
    
    if (!currentQuiz) {
        console.error('[Quiz] No current quiz to add question to');
        return;
    }
    
    const questionNum = parseInt(questionMatch[1]);
    const questionText = questionMatch[2];
    const optionsText = questionMatch[3];
    
    console.log('[Quiz] Parsed - Number:', questionNum, 'Text:', questionText, 'Options:', optionsText);
    
    // Parse options (format: "A) option1, B) option2, C) option3, D) option4")
    const options = [];
    const optionMatches = optionsText.matchAll(/([A-D])\)\s*([^,]+)/g);
    
    for (const match of optionMatches) {
        options.push({
            letter: match[1],
            text: match[2].trim()
        });
    }
    
    console.log('[Quiz] Parsed options:', options);
    
    if (options.length === 0) {
        console.error('[Quiz] No options parsed from:', optionsText);
        return;
    }
    
    const question = {
        number: questionNum,
        text: questionText,
        options: options
    };
    
    quizQuestions.push(question);
    currentQuiz.questions.push(question);
    
    console.log('[Quiz] Question added. Total questions:', quizQuestions.length);
    
    // Update quiz interface
    updateQuizInterface();
}

/**
 * Display quiz interface in the message area
 */
function displayQuizInterface(quizName) {
    const messageArea = document.getElementById('messageArea');
    
    console.log('[Quiz] displayQuizInterface called for:', quizName);
    console.log('[Quiz] messageArea found:', !!messageArea);
    console.log('[Quiz] messageArea display:', messageArea ? messageArea.style.display : 'N/A');
    
    if (!messageArea) {
        console.error('[Quiz] messageArea not found in DOM!');
        return;
    }
    
    // Ensure message area is visible
    messageArea.style.display = 'flex';
    messageArea.classList.add('active');
    
    // Hide welcome screen if visible
    const welcomeScreen = document.getElementById('welcomeScreen');
    if (welcomeScreen) {
        welcomeScreen.style.display = 'none';
        welcomeScreen.classList.add('hidden');
    }
    
    // Remove any existing quiz container (including waiting screen)
    const existingContainer = document.getElementById('activeQuizContainer');
    if (existingContainer) {
        console.log('[Quiz] Removing existing quiz container');
        existingContainer.remove();
    }
    
    console.log('[Quiz] Creating quiz container...');
    
    // Get total questions if available
    const totalQs = currentQuiz && currentQuiz.totalQuestions > 0 
        ? currentQuiz.totalQuestions 
        : '?';
    
    // Create quiz container
    const quizContainer = document.createElement('div');
    quizContainer.id = 'activeQuizContainer';
    quizContainer.className = 'active-quiz-container';
    
    quizContainer.innerHTML = `
        <div class="quiz-header-display">
            <div class="quiz-icon">📝</div>
            <div class="quiz-title-display">
                <h2>${quizName}</h2>
                <p class="quiz-subtitle">Select your answers and submit when ready • ${totalQs} question${totalQs !== 1 && totalQs !== '?' ? 's' : ''}</p>
            </div>
        </div>
        <div id="quizQuestionsContainer" class="quiz-questions-container">
            <div class="quiz-loading">
                <span class="loading-spinner">⏳</span>
                <p>Loading questions...</p>
            </div>
        </div>
        <div class="quiz-footer">
            <button id="submitQuizBtn" class="btn-submit-quiz" onclick="submitQuiz()" disabled>
                <span>📤</span> Submit Quiz
            </button>
            <button class="btn-cancel-quiz" onclick="cancelQuiz()">
                <span>❌</span> Cancel
            </button>
        </div>
    `;
    
    console.log('[Quiz] Clearing messageArea and appending quiz container...');
    messageArea.innerHTML = '';
    messageArea.appendChild(quizContainer);
    
    console.log('[Quiz] Quiz container appended to DOM');
    console.log('[Quiz] messageArea children count:', messageArea.children.length);
    console.log('[Quiz] Quiz container visible:', quizContainer.offsetHeight > 0);
}

/**
 * Update quiz interface with questions
 */
function updateQuizInterface() {
    const container = document.getElementById('quizQuestionsContainer');
    
    console.log('[Quiz] updateQuizInterface called');
    console.log('[Quiz] Container found:', !!container);
    console.log('[Quiz] Container display:', container ? getComputedStyle(container).display : 'N/A');
    console.log('[Quiz] Container dimensions:', container ? `${container.offsetWidth}x${container.offsetHeight}` : 'N/A');
    
    if (!container) {
        console.error('[Quiz] Quiz questions container not found');
        return;
    }
    
    if (quizQuestions.length === 0) {
        console.log('[Quiz] No questions to display yet');
        return;
    }
    
    console.log('[Quiz] Updating interface with', quizQuestions.length, 'questions');
    
    container.innerHTML = '';
    
    quizQuestions.forEach((question, index) => {
        console.log('[Quiz] Rendering question', question.number, ':', question.text);
        
        const questionDiv = document.createElement('div');
        questionDiv.className = 'quiz-question-card';
        questionDiv.id = `question-${question.number}`;
        
        console.log('[Quiz] Question', question.number, 'options:', question.options);
        
        let optionsHTML = '';
        question.options.forEach(option => {
            console.log('[Quiz] Adding option:', option.letter, '-', option.text);
            const isSelected = quizAnswers.get(question.number) === (option.letter.charCodeAt(0) - 65); // A=0, B=1, C=2, D=3
            optionsHTML += `
                <label class="quiz-option ${isSelected ? 'selected' : ''}">
                    <input type="radio" 
                           name="question${question.number}" 
                           value="${option.letter}"
                           ${isSelected ? 'checked' : ''}
                           onchange="selectQuizOption(${question.number}, '${option.letter}')">
                    <span class="option-marker">${option.letter}</span>
                    <span class="option-text">${option.text}</span>
                    <span class="check-icon">✓</span>
                </label>
            `;
        });
        
        console.log('[Quiz] Options HTML for question', question.number, ':', optionsHTML.length, 'chars');
        
        questionDiv.innerHTML = `
            <div class="question-header">
                <span class="question-number">Question ${question.number}</span>
                <span class="question-status ${quizAnswers.has(question.number) ? 'answered' : 'unanswered'}">
                    ${quizAnswers.has(question.number) ? '✓ Answered' : '⚪ Not Answered'}
                </span>
            </div>
            <div class="question-text">${question.text}?</div>
            <div class="question-options">
                ${optionsHTML}
            </div>
        `;
        
        container.appendChild(questionDiv);
        
        console.log('[Quiz] Question div appended, height:', questionDiv.offsetHeight);
    });
    
    console.log('[Quiz] Questions rendered successfully');
    console.log('[Quiz] Container height after render:', container.offsetHeight);
    console.log('[Quiz] Container children count:', container.children.length);
    
    // Check parent containers
    const quizContainer = document.getElementById('activeQuizContainer');
    const messageArea = document.getElementById('messageArea');
    console.log('[Quiz] Quiz container height:', quizContainer ? quizContainer.offsetHeight : 'not found');
    console.log('[Quiz] Message area height:', messageArea ? messageArea.offsetHeight : 'not found');
    console.log('[Quiz] Message area display:', messageArea ? getComputedStyle(messageArea).display : 'N/A');
    
    // Update submit button state
    updateSubmitButton();
}

/**
 * Select an option for a question
 */
function selectQuizOption(questionNumber, optionLetter) {
    console.log('[Quiz] Selected option', optionLetter, 'for question', questionNumber);
    
    // Convert letter (A-D) to index (0-3)
    const answerIndex = optionLetter.charCodeAt(0) - 'A'.charCodeAt(0);
    quizAnswers.set(questionNumber, answerIndex);
    
    console.log('[Quiz] Stored answer index:', answerIndex, 'Total answers:', quizAnswers.size);
    
    updateQuizInterface();
}

/**
 * Update submit button state
 */
function updateSubmitButton() {
    const submitBtn = document.getElementById('submitQuizBtn');
    if (!submitBtn) return;
    
    // Use total questions if available, otherwise use received questions
    const expectedQuestions = currentQuiz && currentQuiz.totalQuestions > 0 
        ? currentQuiz.totalQuestions 
        : quizQuestions.length;
    
    const allAnswered = quizQuestions.length > 0 && 
                       quizQuestions.length >= expectedQuestions &&
                       quizQuestions.every(q => quizAnswers.has(q.number));
    
    submitBtn.disabled = !allAnswered;
    
    if (allAnswered) {
        submitBtn.classList.add('ready');
        submitBtn.innerHTML = `<span>✅</span> Submit Quiz (${quizAnswers.size}/${expectedQuestions})`;
    } else {
        submitBtn.classList.remove('ready');
        submitBtn.innerHTML = `<span>📤</span> Submit Quiz (${quizAnswers.size}/${expectedQuestions})`;
    }
    
    console.log('[Quiz] Submit button updated - Answered:', quizAnswers.size, 'Expected:', expectedQuestions, 'Enabled:', allAnswered);
}

/**
 * Submit quiz answers
 */
function submitQuiz() {
    if (!currentQuiz || !currentQuiz.id || quizAnswers.size === 0) {
        alert('Please answer all questions before submitting.');
        return;
    }
    
    // Check if all questions are answered
    const allAnswered = quizQuestions.every(q => quizAnswers.has(q.number));
    if (!allAnswered) {
        const unanswered = quizQuestions.filter(q => !quizAnswers.has(q.number)).length;
        alert(`Please answer all questions before submitting.\n${unanswered} question(s) remaining.`);
        return;
    }
    
    // Send answers one by one for each question
    // Format: /answer <quiz_id> <answer_index>
    quizQuestions.forEach(question => {
        const answerIndex = quizAnswers.get(question.number);
        const answerCmd = `/answer ${currentQuiz.id} ${answerIndex}`;
        console.log(`Submitting answer for question ${question.number}: ${answerCmd}`);
        sendMessage(answerCmd);
    });
    
    // Show confirmation
    const container = document.getElementById('activeQuizContainer');
    if (container) {
        container.innerHTML = `
            <div class="quiz-submitted">
                <div class="success-icon">✅</div>
                <h2>Quiz Submitted!</h2>
                <p>Your answers have been submitted successfully.</p>
                <p class="submit-details">
                    <strong>Quiz:</strong> ${currentQuiz.name}<br>
                    <strong>Total Questions:</strong> ${quizAnswers.size}<br>
                    <strong>Time:</strong> ${new Date().toLocaleTimeString()}
                </p>
                <p class="waiting-results">⏳ Waiting for results...</p>
                <button class="btn-back-to-chat" onclick="backToChat()">
                    <span>💬</span> Back to Chat
                </button>
            </div>
        `;
    }
    
    // Don't clear quiz data immediately - wait for results
    console.log(`✅ Quiz submitted: ${quizAnswers.size} answers for quiz ${currentQuiz.id}`);
}

/**
 * Cancel quiz
 */
function cancelQuiz() {
    if (confirm('Are you sure you want to cancel this quiz? Your answers will be lost.')) {
        currentQuiz = null;
        quizAnswers.clear();
        quizQuestions = [];
        
        backToChat();
    }
}

/**
 * Return to chat interface
 */
function backToChat() {
    const messageArea = document.getElementById('messageArea');
    const container = document.getElementById('activeQuizContainer');
    
    if (container) {
        container.remove();
    }
    
    // Clear quiz state
    joinedQuizId = null;
    currentQuiz = null;
    quizAnswers.clear();
    quizQuestions = [];
    
    // Restore message area
    messageArea.innerHTML = '';
    loadMessagesFromStorage();
}

/**
 * Display quiz results
 */
function displayQuizResults(resultsMessage) {
    const container = document.getElementById('activeQuizContainer');
    if (!container) return;
    
    // Parse results from message
    // Format: "Quiz Results for 'QuizName': \n username - Score: X/Y - Rank: #Z"
    const lines = resultsMessage.split('\n');
    let resultsHTML = '<div class="quiz-results-list">';
    
    lines.forEach((line, index) => {
        if (line.includes('Score:')) {
            const isCurrentUser = line.includes(username);
            const rankClass = index === 1 ? 'rank-gold' : index === 2 ? 'rank-silver' : index === 3 ? 'rank-bronze' : '';
            resultsHTML += `
                <div class="result-item ${isCurrentUser ? 'current-user' : ''} ${rankClass}">
                    <span class="result-rank">#${index}</span>
                    <span class="result-content">${line}</span>
                    ${isCurrentUser ? '<span class="you-badge">You</span>' : ''}
                </div>
            `;
        }
    });
    
    resultsHTML += '</div>';
    
    container.innerHTML = `
        <div class="quiz-results">
            <div class="results-icon">🏆</div>
            <h2>Quiz Results</h2>
            ${resultsHTML}
            <button class="btn-back-to-chat" onclick="backToChat()">
                <span>💬</span> Back to Chat
            </button>
        </div>
    `;
    
    // Clear quiz data
    setTimeout(() => {
        joinedQuizId = null;
        currentQuiz = null;
        quizAnswers.clear();
        quizQuestions = [];
    }, 1000);
}

// Initialize - Load quizzes from storage on page load
window.addEventListener('DOMContentLoaded', () => {
    console.log('Chat client loaded');
    loadQuizzesFromStorage();
});

/**
 * Debug function - Check quiz display state
 * Call this from console: checkQuizDisplay()
 */
window.checkQuizDisplay = function() {
    console.log('=== QUIZ DISPLAY DEBUG ===');
    console.log('Current quiz:', currentQuiz);
    console.log('Joined quiz ID:', joinedQuizId);
    console.log('Quiz questions count:', quizQuestions.length);
    console.log('Quiz questions:', quizQuestions);
    
    const messageArea = document.getElementById('messageArea');
    const quizContainer = document.getElementById('activeQuizContainer');
    const questionsContainer = document.getElementById('quizQuestionsContainer');
    
    console.log('Message area:', messageArea);
    console.log('  - display:', messageArea ? getComputedStyle(messageArea).display : 'N/A');
    console.log('  - dimensions:', messageArea ? `${messageArea.offsetWidth}x${messageArea.offsetHeight}` : 'N/A');
    console.log('  - children:', messageArea ? messageArea.children.length : 'N/A');
    
    console.log('Quiz container:', quizContainer);
    console.log('  - display:', quizContainer ? getComputedStyle(quizContainer).display : 'N/A');
    console.log('  - dimensions:', quizContainer ? `${quizContainer.offsetWidth}x${quizContainer.offsetHeight}` : 'N/A');
    console.log('  - visibility:', quizContainer ? getComputedStyle(quizContainer).visibility : 'N/A');
    console.log('  - children:', quizContainer ? quizContainer.children.length : 'N/A');
    console.log('  - className:', quizContainer ? quizContainer.className : 'N/A');
    
    console.log('Questions container:', questionsContainer);
    console.log('  - display:', questionsContainer ? getComputedStyle(questionsContainer).display : 'N/A');
    console.log('  - dimensions:', questionsContainer ? `${questionsContainer.offsetWidth}x${questionsContainer.offsetHeight}` : 'N/A');
    console.log('  - children:', questionsContainer ? questionsContainer.children.length : 'N/A');
    
    if (questionsContainer && questionsContainer.children.length > 0) {
        console.log('First question element:', questionsContainer.children[0]);
        console.log('  - display:', getComputedStyle(questionsContainer.children[0]).display);
        console.log('  - dimensions:', `${questionsContainer.children[0].offsetWidth}x${questionsContainer.children[0].offsetHeight}`);
    }
    
    console.log('=========================');
};

/**
 * Debug function - Manually trigger quiz display
 * Call this from console: testQuizDisplay('TestQuiz')
 */
window.testQuizDisplay = function(quizName) {
    console.log('=== MANUAL QUIZ DISPLAY TEST ===');
    console.log('Calling displayQuizInterface with name:', quizName || 'TestQuiz');
    displayQuizInterface(quizName || 'TestQuiz');
    console.log('Display function completed');
    console.log('Check if quiz interface appeared above');
    console.log('================================');
};

// ========== ADMIN FUNCTIONALITY ==========

let isAdmin = false;
let currentAdminCommand = '';

/**
 * Check if user is admin and show admin panel
 */
function checkAdminStatus(msg) {
    // Check if server message indicates admin status
    if (msg.includes('ADMIN') || msg.includes('admin')) {
        isAdmin = true;
        showAdminPanel();
    }
}

/**
 * Show admin panel
 */
function showAdminPanel() {
    const adminSection = document.getElementById('adminSection');
    const adminCommands = document.getElementById('adminCommands');
    
    if (adminSection && adminCommands) {
        adminSection.style.display = 'block';
        adminCommands.style.display = 'block';
    }
}

/**
 * Hide admin panel
 */
function hideAdminPanel() {
    const adminSection = document.getElementById('adminSection');
    const adminCommands = document.getElementById('adminCommands');
    
    if (adminSection && adminCommands) {
        adminSection.style.display = 'none';
        adminCommands.style.display = 'none';
    }
    isAdmin = false;
}

/**
 * Show admin command dialog
 */
window.showAdminDialog = function(command) {
    currentAdminCommand = command;
    const dialog = document.getElementById('adminDialog');
    const title = document.getElementById('adminDialogTitle');
    const input = document.getElementById('adminInput');
    
    // Set title and placeholder based on command
    switch(command) {
        case 'kick':
            title.textContent = '🚫 Kick User';
            input.placeholder = 'Enter username to kick';
            break;
        case 'ban':
            title.textContent = '🔨 Ban User';
            input.placeholder = 'Enter username to ban';
            break;
        case 'mute':
            title.textContent = '🔇 Mute User';
            input.placeholder = 'Enter username to mute';
            break;
        case 'unmute':
            title.textContent = '🔊 Unmute User';
            input.placeholder = 'Enter username to unmute';
            break;
        case 'broadcast':
            title.textContent = '📢 Admin Broadcast';
            input.placeholder = 'Enter announcement message';
            break;
    }
    
    input.value = '';
    dialog.style.display = 'flex';
    input.focus();
};

/**
 * Close admin dialog
 */
window.closeAdminDialog = function() {
    const dialog = document.getElementById('adminDialog');
    dialog.style.display = 'none';
    currentAdminCommand = '';
};

/**
 * Execute admin command
 */
window.executeAdminCommand = function() {
    const input = document.getElementById('adminInput');
    const value = input.value.trim();
    
    if (!value) {
        alert('Please enter a value');
        return;
    }
    
    if (!ws || ws.readyState !== WebSocket.OPEN) {
        alert('Not connected to server');
        return;
    }
    
    let msgType = '';
    let payload = {};
    
    switch(currentAdminCommand) {
        case 'kick':
            msgType = 'ADMIN_KICK';
            payload = {
                type: msgType,
                sender: username,
                target: value
            };
            break;
        case 'ban':
            msgType = 'ADMIN_BAN';
            payload = {
                type: msgType,
                sender: username,
                target: value
            };
            break;
        case 'mute':
            msgType = 'ADMIN_MUTE';
            payload = {
                type: msgType,
                sender: username,
                target: value
            };
            break;
        case 'unmute':
            msgType = 'ADMIN_UNMUTE';
            payload = {
                type: msgType,
                sender: username,
                target: value
            };
            break;
        case 'broadcast':
            msgType = 'ADMIN_BROADCAST';
            payload = {
                type: msgType,
                sender: username,
                content: value
            };
            break;
    }
    
    // Send admin command
    ws.send(JSON.stringify(payload));
    console.log('Admin command sent:', payload);
    
    // Close dialog
    closeAdminDialog();
};

/**
 * Send admin stats request
 */
window.sendAdminStats = function() {
    if (!ws || ws.readyState !== WebSocket.OPEN) {
        alert('Not connected to server');
        return;
    }
    
    const payload = {
        type: 'ADMIN_STATS',
        sender: username
    };
    
    ws.send(JSON.stringify(payload));
    console.log('Admin stats requested');
};

