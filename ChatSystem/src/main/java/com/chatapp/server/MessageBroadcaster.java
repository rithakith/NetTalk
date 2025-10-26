package com.chatapp.server;

import com.chatapp.common.Message;
import java.io.*;
import java.nio.ByteBuffer;
import java.nio.channels.*;
import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * MEMBER 3 CONTRIBUTION: NIO Message Broadcaster
 * 
 * Network Programming Concept: Java NIO (Channels, Buffers, Selectors)
 * 
 * This class demonstrates high-performance, non-blocking I/O using Java NIO.
 * A single thread can efficiently monitor thousands of client channels using
 * a Selector, checking which channels are ready for read/write operations.
 * 
 * Key Concepts Demonstrated:
 * - Non-blocking I/O with Channels
 * - Selector for multiplexing multiple channels in single thread
 * - ByteBuffer for efficient data handling
 * - Scalability: one thread handles many connections
 */
public class MessageBroadcaster implements Runnable {
    private final Selector selector;
    private final ConcurrentLinkedQueue<BroadcastTask> broadcastQueue;
    private volatile boolean running;
    private final Map<SocketChannel, ByteBuffer> clientBuffers;
    
    /**
     * Represents a broadcast task
     */
    private static class BroadcastTask {
        final Message message;
        final Set<SocketChannel> targetChannels;
        
        BroadcastTask(Message message, Set<SocketChannel> targets) {
            this.message = message;
            this.targetChannels = targets;
        }
    }
    
    public MessageBroadcaster() throws IOException {
        this.selector = Selector.open();
        this.broadcastQueue = new ConcurrentLinkedQueue<>();
        this.clientBuffers = new HashMap<>();
        this.running = false;
    }
    
    @Override
    public void run() {
        running = true;
        System.out.println("[MessageBroadcaster] NIO Broadcaster started");
        
        try {
            while (running) {
                // Non-blocking select - check which channels are ready
                // Timeout allows checking broadcast queue periodically
                int readyChannels = selector.select(100);
                
                // Process any pending broadcast tasks
                processBroadcastQueue();
                
                if (readyChannels > 0) {
                    Set<SelectionKey> selectedKeys = selector.selectedKeys();
                    Iterator<SelectionKey> iterator = selectedKeys.iterator();
                    
                    while (iterator.hasNext()) {
                        SelectionKey key = iterator.next();
                        iterator.remove();
                        
                        if (key.isValid() && key.isWritable()) {
                            handleWrite(key);
                        }
                    }
                }
            }
        } catch (IOException e) {
            System.err.println("[MessageBroadcaster] Error in broadcaster: " + e.getMessage());
        } finally {
            cleanup();
        }
    }
    
    /**
     * Register a client channel with the selector for NIO operations
     */
    public void registerChannel(SocketChannel channel) {
        try {
            channel.configureBlocking(false);
            channel.register(selector, SelectionKey.OP_WRITE);
            clientBuffers.put(channel, ByteBuffer.allocate(8192));
            System.out.println("[MessageBroadcaster] Registered channel: " + channel.getRemoteAddress());
        } catch (IOException e) {
            System.err.println("[MessageBroadcaster] Error registering channel: " + e.getMessage());
        }
    }
    
    /**
     * Unregister a client channel
     */
    public void unregisterChannel(SocketChannel channel) {
        clientBuffers.remove(channel);
        try {
            SelectionKey key = channel.keyFor(selector);
            if (key != null) {
                key.cancel();
            }
            System.out.println("[MessageBroadcaster] Unregistered channel");
        } catch (Exception e) {
            System.err.println("[MessageBroadcaster] Error unregistering channel: " + e.getMessage());
        }
    }
    
    /**
     * Queue a message for broadcast to all registered channels
     */
    public void queueBroadcast(Message message, Set<SocketChannel> targetChannels) {
        broadcastQueue.offer(new BroadcastTask(message, targetChannels));
        selector.wakeup(); // Wake up selector to process immediately
    }
    
    /**
     * Process pending broadcast tasks from queue
     */
    private void processBroadcastQueue() {
        BroadcastTask task;
        while ((task = broadcastQueue.poll()) != null) {
            try {
                // Serialize message to bytes
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                ObjectOutputStream oos = new ObjectOutputStream(baos);
                oos.writeObject(task.message);
                oos.flush();
                byte[] messageBytes = baos.toByteArray();
                
                // Write to all target channels
                for (SocketChannel channel : task.targetChannels) {
                    if (channel.isOpen() && channel.isConnected()) {
                        ByteBuffer buffer = clientBuffers.get(channel);
                        if (buffer != null) {
                            buffer.clear();
                            buffer.putInt(messageBytes.length); // Message length prefix
                            buffer.put(messageBytes);
                            buffer.flip();
                            
                            // Non-blocking write
                            channel.write(buffer);
                        }
                    }
                }
                
                System.out.println("[MessageBroadcaster] Broadcasted message to " + 
                                 task.targetChannels.size() + " channels");
                
            } catch (IOException e) {
                System.err.println("[MessageBroadcaster] Error broadcasting: " + e.getMessage());
            }
        }
    }
    
    /**
     * Handle channel ready for write
     */
    private void handleWrite(SelectionKey key) {
        SocketChannel channel = (SocketChannel) key.channel();
        ByteBuffer buffer = clientBuffers.get(channel);
        
        if (buffer != null && buffer.hasRemaining()) {
            try {
                channel.write(buffer);
            } catch (IOException e) {
                System.err.println("[MessageBroadcaster] Error writing to channel: " + e.getMessage());
                unregisterChannel(channel);
            }
        }
    }
    
    /**
     * Cleanup resources
     */
    private void cleanup() {
        try {
            for (SelectionKey key : selector.keys()) {
                key.cancel();
            }
            selector.close();
            System.out.println("[MessageBroadcaster] Selector closed");
        } catch (IOException e) {
            System.err.println("[MessageBroadcaster] Error closing selector: " + e.getMessage());
        }
    }
    
    public void shutdown() {
        running = false;
        selector.wakeup();
    }
    
    public boolean isRunning() {
        return running;
    }
}
