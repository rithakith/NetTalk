import java.io.*;
import java.net.*;

public class QuizTestClient {
    public static void main(String[] args) throws Exception {
        Socket socket = new Socket("localhost", 8888);
        ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream());
        ObjectInputStream in = new ObjectInputStream(socket.getInputStream());
        
        // Connect as admin
        com.chatapp.common.Message connectMsg = new com.chatapp.common.Message(
            com.chatapp.common.Message.MessageType.CONNECT, "TestAdmin", "");
        out.writeObject(connectMsg);
        
        Thread.sleep(1000);
        
        // List quizzes
        com.chatapp.common.Message listMsg = new com.chatapp.common.Message(
            com.chatapp.common.Message.MessageType.CHAT, "TestAdmin", "/quizzes");
        out.writeObject(listMsg);
        
        Thread.sleep(1000);
        
        // Try to start quiz with ID 4192feb0
        com.chatapp.common.Message startMsg = new com.chatapp.common.Message(
            com.chatapp.common.Message.MessageType.CHAT, "TestAdmin", "/startquiz 4192feb0");
        out.writeObject(startMsg);
        
        // Read responses
        for (int i = 0; i < 10; i++) {
            try {
                com.chatapp.common.Message response = (com.chatapp.common.Message) in.readObject();
                System.out.println("Response " + i + ": " + response.getSender() + ": " + response.getContent());
                Thread.sleep(500);
            } catch (Exception e) {
                System.out.println("Exception reading response " + i + ": " + e.getMessage());
                break;
            }
        }
        
        socket.close();
    }
}