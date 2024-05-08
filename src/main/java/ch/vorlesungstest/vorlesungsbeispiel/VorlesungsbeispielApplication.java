package ch.vorlesungstest.vorlesungsbeispiel;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.List;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class VorlesungsbeispielApplication {
    private static WebSocketNotifyClient client;

    public static void main(String[] args) {

        SpringApplication.run(VorlesungsbeispielApplication.class, args);
        startNodeServer();

        

     try {
            client = new WebSocketNotifyClient(new URI("ws://localhost:8081"));
            client.connect();
            System.out.println("Websocket connectet on "+ client );
        } catch (URISyntaxException e) {
            e.printStackTrace();
        }
    }

    public static void notifyWebSocketServer() {
        if (client != null && client.isOpen()) {
            client.notifyServer();
        } else {
            System.out.println("WebSocket is not connected.");
        }
    }
    private static void startNodeServer() {
        try {
            List<String> command = Arrays.asList("node", "src\\main\\resources\\static\\server.js");
            ProcessBuilder builder = new ProcessBuilder(command);
            builder.redirectErrorStream(true);
    
            Process process = builder.start();
            new Thread(() -> {
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        System.out.println("Node Output/Error: " + line);
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }).start();
    
            // Monitoring process to ensure it terminates correctly
            int exitCode = process.waitFor();
            if (exitCode == 0) {
                System.out.println("Node server started successfully.");
            } else {
                System.out.println("Node server failed to start with exit code: " + exitCode);
            }
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
            System.out.println("Failed to start node server.");
        }
    }
    
}
