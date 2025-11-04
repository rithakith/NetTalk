package com.chatapp.filetransfer;

import java.io.*;
import java.net.Socket;

public class FileTransferHandler {
    public static void sendFile(File file, Socket socket) throws IOException {
        try (BufferedInputStream bis = new BufferedInputStream(new FileInputStream(file));
             OutputStream os = socket.getOutputStream()) {
            byte[] buffer = new byte[4096];
            int count;
            while ((count = bis.read(buffer)) > 0) {
                os.write(buffer, 0, count);
            }
            os.flush();
        }
    }

    public static void receiveFile(String savePath, InputStream in) throws IOException {
        try (BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(savePath))) {
            byte[] buffer = new byte[4096];
            int count;
            while ((count = in.read(buffer)) > 0) {
                bos.write(buffer, 0, count);
            }
        }
    }
}
