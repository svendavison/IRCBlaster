package ircblaster;
import java.io.*;
import java.net.*;
import java.util.ArrayList;

public class Bot {
    BufferedWriter writer;
    BufferedReader reader;
    ArrayList<String> channels;

    public void doWork() throws IOException, InterruptedException {
        // The server to connect to and our details.
        Boolean isDev = true;
        String server = "irc.freenode.net";
        String nick = "xenthbot4";
        String login = "xenthbot4";

        channels = new ArrayList<>();

        // Connect directly to the IRC server.
        Socket socket = new Socket(server, 6667);
        writer = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
        reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));

        // Log on to the server.
        writer.write("NICK " + nick + "\r\n");
        writer.write("USER " + login + " 8 * : Java IRC Bot\r\n");
        writer.flush();

        // Read lines from the server until it tells us we have connected.
        String line = null;
        while ((line = reader.readLine()) != null) {
            if (line.contains("004")) {
                // We are now logged in.
                //break;
            } else if (line.contains("433")) {
                //System.out.println("Nickname is already in use.");
                //return;
            } else if (line.contains("PING ")) {
                // We must respond to PINGs to avoid being disconnected.
                this.sendRAW(String.format("PONG %s\r\n", line.substring(5)));
                System.out.println(String.format("Saw a PING, gave a PONG."));
            } else if (line.contains(String.format(" 376 %s ", login))) {
                //end of MOTD
                Thread.sleep(500);
                //channels = this.getChannels(writer);
                this.sendRAW("LIST");
            } else if (line.contains(String.format(" 194 %s ", login))) {
                //i forget what this code is for...
                System.out.println(line);
            } else if (line.contains(String.format(" :!unicom"))) {
                //single bot control
                //:Xenthoxozet!~Xenthoxoz@rockinsupercool.host.online.net PRIVMSG #xenthoxozet :!unicom
                String[] parts = line.split(" ");
                String blastTarget = parts[2]; //we'll send to ch not user that typed the cmd
                
                this.multiLineBlast(blastTarget);
                System.out.println(String.format(" Channel %s [BLASTED] by request.", blastTarget));
                
            } else if (line.contains(String.format(" 322 %s ", login))) {
                //channel found for 'list'
                //:hitchcock.freenode.net 322 xenthbot3 #dc4420 42 :Next meeting: Apr 24t
                String[] parts = line.split(" ");
                String ch = parts[3];

                if(!isDev){
                    channels.add(ch);
                    System.out.println(String.format("Added: %s to list.", ch));
                }                
                //force a couple channels
                if(channels.isEmpty()){
                    channels.add("#xenthoxozet");
                    channels.add("#xenthbot");
                }                
            } else if (line.contains(String.format(" 323 %s ", login))) {
                //end of LIST
                System.out.println(String.format("[ OUR CHANNELS ]"));
                for (String ch : channels) {
                    System.out.print(String.format(" -- %s ", ch));
                    this.joinChannel(ch);
                    System.out.print("[Joined] ");
                    this.multiLineBlast(ch);
                    System.out.println("[BLASTED] ");
                }
                System.out.println(" ");
            } else {
                // Print the raw line received by the bot.
                System.out.println(String.format("[RAW] %s", line));
            }
        }
    }
    
    private void multiLineBlast(String ch) throws IOException{
        //We'll read this from a file. one line per entry into arrayList
        ArrayList<String> fullMsg = new ArrayList<>();
        fullMsg.add("This is one line.");
        fullMsg.add("");//this seems to give a blank line!
        fullMsg.add("This is the third line.");
        
        for(String str : fullMsg){
            this.postMessage(ch, str);
        }        
    }
    
    private void joinChannel(String ch) throws IOException {
        this.sendRAW(String.format("JOIN %s",ch));
    }
    
    private void postMessage(String ch, String msg) throws IOException{
        this.sendRAW(String.format("PRIVMSG %s : %s", ch, msg));
    }

    private void sendRAW(String msg) throws IOException {
        writer.write(String.format("%s\r\n", msg));
        writer.flush();
    }
}
