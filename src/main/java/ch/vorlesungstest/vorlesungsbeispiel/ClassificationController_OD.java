package ch.vorlesungstest.vorlesungsbeispiel;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;

import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import ai.djl.modality.Classifications.Classification;
import ai.djl.modality.cv.output.DetectedObjects;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@RestController
public class ClassificationController_OD {

    //private Inference inference = new Inference();
    private static final String searchObject = "car";
    private static int fps = 5;

    @Autowired
    private ObjectDetection ObjectDetection;

    private static final String TEMP_DIR = "src\\main\\resources\\static\\tempVideos";
    private static final String HIGH_PROB_DIR = "src\\main\\resources\\static\\HighProb_Frames";
    private static final String Frames_DIR = "src\\main\\resources\\static\\Frames_Dir";
    private static final double YES_PROBABILITY_THRESHOLD = 0.5; // 98% probability
    private FrameExtractor extractor = new FrameExtractor();

    @GetMapping("/ping")
    public String ping() {
        return "Classification app is up and running!";
    }
    @PostMapping(path = "/analyze")
    public String predict(@RequestParam("image") MultipartFile image) throws Exception {
        System.out.println(image);
        // return inference.predict(image.getBytes()).toJson();
        return ObjectDetection.predict(image.getBytes(), searchObject ,YES_PROBABILITY_THRESHOLD ).toJson();
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

                List<Path> frames = extractor.extractFrames(tempFile.toString(), fps, Frames_DIR);
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
                    String jsonResult = ObjectDetection.predict(imageData, searchObject, YES_PROBABILITY_THRESHOLD).toJson();
                    
                    JSONArray results = new JSONArray(jsonResult);
                    for (int i = 0; i < results.length(); i++) {
                        JSONObject result = results.getJSONObject(i);
                        //System.out.println(result.getDouble("probability"));
                        if (searchObject.equals(result.getString("className"))) {
                            Path targetPath = highProbYesDir.resolve(framePath.getFileName());
                            Files.copy(framePath, targetPath, StandardCopyOption.REPLACE_EXISTING);
                            resultsForHighProbYes.put(result);
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

                System.out.println("Amount of relevant Frames: " + resultsForHighProbYes.length());
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


    private ArrayList<Classification> check_relevant(DetectedObjects detection) {

        ArrayList<Classification> x = new ArrayList<>();
        // Iterate through all detected objects
        for (Classification obj : detection.items()) { // Ensure this matches the list type returned by
                                                       // detection.items()
            String className = obj.getClassName();
            double probability = obj.getProbability();

            // Log or check the className
            // logger.info("Class: {}, Probability: {}", className, probability);

            // Example check
            if (className.equals(searchObject)) {
               // logger.info("Detected a person with probability: {}", probability);
                x.add(obj);
            }
        }
        return x;
    }

    @GetMapping("/getFrames")
    public ResponseEntity<List<String>> getFramePaths() {
        try {
            Path framesDir = Paths.get("src\\main\\resources\\static\\HighProb_Frames");
            List<String> fileNames = Files.walk(framesDir)
                                         .filter(Files::isRegularFile)
                                         .map(path -> path.getFileName().toString())
                                         .collect(Collectors.toList());
            return ResponseEntity.ok(fileNames);
        } catch (IOException e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

}
