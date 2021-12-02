/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package server;

import java.awt.Graphics;
import java.awt.image.BufferedImage;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.Socket;
import javax.imageio.ImageIO;
import javax.swing.ImageIcon;

/**
 *
 * @author Admin
 */
public class ServerThread implements Runnable {

    private Socket socketOfServer;
    private int clientNumber;
    private BufferedReader is;
    private BufferedWriter os;
    private boolean isClosed;

    public BufferedReader getIs() {
        return is;
    }

    public BufferedWriter getOs() {
        return os;
    }

    public int getClientNumber() {
        return clientNumber;
    }

    public ServerThread(Socket socketOfServer, int clientNumber) {
        this.socketOfServer = socketOfServer;
        this.clientNumber = clientNumber;
        System.out.println("Server thread number " + clientNumber + " Started");
        isClosed = false;
    }

    @Override
    public void run() {
        try {
            // Mở luồng vào ra trên Socket tại Server.
            is = new BufferedReader(new InputStreamReader(socketOfServer.getInputStream()));
            os = new BufferedWriter(new OutputStreamWriter(socketOfServer.getOutputStream()));
            System.out.println("Successfully started new thread, ID: " + clientNumber);
            write("get-id" + "," + this.clientNumber);
            Server.serverThreadBus.sendOnlineList();
            Server.serverThreadBus.mutilCastSend("global-message" + "," + "---Client " + this.clientNumber + " login---");
            String message;
            while (!isClosed) {
                message = is.readLine();
                if (message == null) {
                    break;
                }
                String[] messageSplit = message.split(",");
                String cmd = messageSplit[0];
                System.out.println(message);
                if (cmd.equals("msg")) {
                    if (messageSplit[1].equals("sendMess-to-global")) {
                        Server.serverThreadBus.broadCastMess(this.getClientNumber(), "global-message" + "," + "Client " + messageSplit[3] + ": " + messageSplit[2]);
                    }
                    if (messageSplit[1].equals("sendMess-to-person")) {
                        Server.serverThreadBus.sendMess(Integer.parseInt(messageSplit[4]), "Client " + messageSplit[3] + " (to you): " + messageSplit[2]);
                    }
                }

                if (cmd.equals("msgImage")) {
                    BufferedInputStream bis = new BufferedInputStream(socketOfServer.getInputStream());
                    BufferedImage bufferedImage = ImageIO.read(bis);
                    if (messageSplit[1].equals("sendImage-to-global")) {
                        Server.serverThreadBus.broadCastPicture(this.getClientNumber(), "Client " + messageSplit[3] + ": ", bufferedImage);
                    }
                    if (messageSplit[1].equals("sendImage-to-person")) {
                        Server.serverThreadBus.sendPicture(Integer.parseInt(messageSplit[4]), "Client " + messageSplit[3] + " (to you): ", bufferedImage);
                    }
                }

                if (cmd.equals("msgFile")) {
                    InputStream is = socketOfServer.getInputStream();
                    byte[] buffer = new byte[2002];
                    is.read(buffer, 0, buffer.length);
                    if (messageSplit[1].equals("sendFile-to-global")) {
                        Server.serverThreadBus.broadCastFile(this.getClientNumber(), "Client " + messageSplit[3] + ": ,", messageSplit[2], buffer);
                    }
                    if (messageSplit[1].equals("sendFile-to-person")) {
                        Server.serverThreadBus.sendFile(Integer.parseInt(messageSplit[4]), "Client " + messageSplit[3] + " (to you): ,", messageSplit[2], buffer);
                    }
                }

            }
        } catch (IOException e) {
            isClosed = true;
            Server.serverThreadBus.remove(clientNumber);
            System.out.println(this.clientNumber + " log out");
            Server.serverThreadBus.sendOnlineList();
            Server.serverThreadBus.mutilCastSend("global-message" + "," + "---Client " + this.clientNumber + " log out---");
        }
    }

    public void write(String message) throws IOException {
        os.write(message);
        os.newLine();
        os.flush();
    }

    public void writeFile(byte[] b) throws IOException {
        OutputStream oos = socketOfServer.getOutputStream();
        oos.write(b, 0, b.length);
        oos.flush();
    }

    public void writePicture(BufferedImage image) throws IOException {
        BufferedOutputStream bos = new BufferedOutputStream(socketOfServer.getOutputStream());
        BufferedImage bufferImage = new BufferedImage(image.getWidth(null), image.getHeight(null), BufferedImage.TYPE_INT_RGB);
        Graphics graphics = bufferImage.createGraphics();
        graphics.drawImage(image, 0, 0, null);
        graphics.dispose();
        ImageIO.write(bufferImage, "png", bos);
        bos.flush();
    }
}
