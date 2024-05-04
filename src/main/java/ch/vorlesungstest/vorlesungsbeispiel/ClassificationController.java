package ch.vorlesungstest.vorlesungsbeispiel;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;

import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.Comparator;
import java.util.List;

@RestController
public class ClassificationController {

    private Inference inference = new Inference();
    private static final String TEMP_DIR = "tempVideos";
    private static final String HIGH_PROB_DIR = "HighProb_Frames";
    private static final String Frames_DIR = "Frames_Dir";
    private static final double YES_PROBABILITY_THRESHOLD = 0.98; // 98% probability
    private FrameExtractor extractor = new FrameExtractor();

    @GetMapping("/ping")
    public String ping() {
        return "Classification app is up and running!";
    }
    @PostMapping(path = "/analyze")
    public String predict(@RequestParam("image") MultipartFile image) throws Exception {
        System.out.println(image);
        return inference.predict(image.getBytes()).toJson();
    }


    @PostMapping("/upload_video")
    public String handleFileUpload(@RequestParam("video") MultipartFile file) {
        if (!file.isEmpty()) {
            Path tempFile = null;
            try {
                System.out.println("Video not empty " + file.getSize());
                Path tempDirPath = Paths.get(TEMP_DIR);
                Files.createDirectories(tempDirPath);

                tempFile = Files.createTempFile(tempDirPath, null, ".mp4");
                file.transferTo(tempFile);
                System.out.println("Tempfile saved at: " + tempFile);

                List<Path> frames = extractor.extractFrames(tempFile.toString(), 5, Frames_DIR);
                JSONArray resultsForHighProbYes = new JSONArray();
                if (Paths.get(HIGH_PROB_DIR) != null && Files.exists(Paths.get(HIGH_PROB_DIR))) {
                    deleteDirectoryRecursively(Paths.get(HIGH_PROB_DIR));
                }

                Path highProbYesDir = Paths.get(HIGH_PROB_DIR);
                Files.createDirectories(highProbYesDir);
                System.out.println("FrameCount: " + frames.size());
                int count = 0;
               
                for (Path framePath : frames) {

                    byte[] imageData = Files.readAllBytes(framePath);
                    String jsonResult = inference.predict(imageData).toJson();
                    JSONArray results = new JSONArray(jsonResult);
                    for (int i = 0; i < results.length(); i++) {
                        JSONObject result = results.getJSONObject(i);
                        if ("Yes".equals(result.getString("className"))
                                && result.getDouble("probability") > YES_PROBABILITY_THRESHOLD) {
                            Path targetPath = highProbYesDir.resolve(framePath.getFileName());
                            Files.copy(framePath, targetPath, StandardCopyOption.REPLACE_EXISTING);
                            resultsForHighProbYes.put(new JSONObject()
                                    .put("frame", framePath.getFileName().toString())
                                    .put("probability", result.getDouble("probability")));
                            count++;
                            System.out.println(count);
                        }
                    }
                }

                // After processing is complete, delete the temporary video file
                if (tempFile != null && Files.exists(tempFile)) {
                    Files.delete(tempFile);
                    System.out.println("Temporary video file deleted successfully at " + tempFile);
                }

                // Delete all files in the directories and the directories themselves
                deleteDirectoryRecursively(Paths.get(TEMP_DIR));
                // deleteDirectoryRecursively(highProbYesDir);
                deleteDirectoryRecursively(Paths.get(Frames_DIR));

                System.out.println(resultsForHighProbYes.length());
                return resultsForHighProbYes.toString();
            } catch (Exception e) {
                e.printStackTrace();
                return "{\"status\": \"error\", \"message\": \"" + e.getMessage() + "\"}";
            }
        } else {
            return "{\"status\": \"file is empty\"}";
        }
    }

    private void deleteDirectoryRecursively(Path directory) throws IOException {
        if (Files.exists(directory)) {
            Files.walk(directory)
                    .sorted(Comparator.reverseOrder())
                    .map(Path::toFile)
                    .forEach(File::delete);
            System.out.println("Deleted directory: " + directory);
        }
    }

}
