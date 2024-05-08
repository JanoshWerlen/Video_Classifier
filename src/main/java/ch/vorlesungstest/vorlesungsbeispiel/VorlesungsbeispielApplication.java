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
        startNodeServer();

        SpringApplication.run(VorlesungsbeispielApplication.class, args);

        try {
            client = new WebSocketNotifyClient(new URI("ws://localhost:8000"));
            client.connect();
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
        // Adjust the path to where your server.js file is located
        // It's good practice to use absolute path or ensure the relative path is correct from the execution context
        List<String> command = Arrays.asList("node", "E:\\VS_Code\\ZHAW\\Model_Deployment\\JavaSpringboot\\New folder\\vorlesungsbeispiel\\src\\main\\resources\\static\\server.js");
        ProcessBuilder builder = new ProcessBuilder(command);
        builder.redirectErrorStream(true); // Redirects error stream to the output stream

        // Set up the working directory if necessary (uncomment below line if needed)
        // builder.directory(new File("/path/to/working/directory"));

        Process process = builder.start();

        // Output and error stream handling in separate threads
        new Thread(() -> {
            try (InputStream inputStream = process.getInputStream();
                 BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    System.out.println("Node Output/Error: " + line);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }).start();

        // Optional: Wait for the process to terminate with an exit value
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
