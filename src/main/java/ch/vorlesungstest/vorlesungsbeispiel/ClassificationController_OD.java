package ch.vorlesungstest.vorlesungsbeispiel;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

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
import org.springframework.web.bind.annotation.SessionAttribute;

import ai.djl.modality.Classifications.Classification;
import ai.djl.modality.cv.output.DetectedObjects;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
// @SessionAttributes("searchObject")
public class ClassificationController_OD {

    

    // private Inference inference = new Inference();
    private static int fps = 5;
    private static final double YES_PROBABILITY_THRESHOLD = 0.5; // 0.5 = 50% probability
    private static String[] searchObject = { "dog", "person", "cat" };

    @Autowired
    private ObjectDetection ObjectDetection;

    private static final String TEMP_DIR = "src\\main\\resources\\static\\tempVideos";
    private static final String HIGH_PROB_DIR = "src\\main\\resources\\static\\HighProb_Frames";
    private static final String Frames_DIR = "src\\main\\resources\\static\\Frames_Dir";

    private FrameExtractor extractor = new FrameExtractor();

    /*
     * @PostMapping("/setSearchObject")
     * public ResponseEntity<String> setSearchObject(@RequestParam("searchObject")
     * HttpServletRequest request) {
     * HttpSession session = request.getSession();
     * session.removeAttribute("searchObject"); // Remove the current setting
     * session.setAttribute("searchObject", searchObject); // Set the new value
     * 
     * session.setAttribute("searchObject", searchObject); // Manually setting the
     * session attribute
     * return ResponseEntity.ok("Search object set to: " + searchObject);
     * }
     */
    @GetMapping("/ping")
    public String ping() {
        return "Classification app is up and running!";
    }

    @PostMapping(path = "/analyze")
    public ResponseEntity<?> predict(@RequestParam("image") MultipartFile image) {

        // System.out.println("Session ID: " + request.getSession().getId() + " -
        // Current searchObject: " + searchObject);
        if (image.isEmpty()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("{\"error\": \"Empty file.\"}");
        }

        try {
            DetectionResult result = ObjectDetection.predict(image.getBytes(), searchObject, YES_PROBABILITY_THRESHOLD);
            if (result == null || result.getDetectedObjects() == null || result.getImagePath() == null) {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body("{\"error\": \"Detection failed.\"}");
            }

            // Since result.getDetectedObjects().toJson() is an array, parse it as JSONArray
            JSONArray detections = new JSONArray(result.getDetectedObjects().toJson());
            JSONObject json = new JSONObject();
            json.put("detections", detections);
            json.put("imagePath", result.getImagePath());
            return ResponseEntity.ok(json.toString());
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("{\"error\": \"" + e.getMessage() + "\"}");
        }
    }

    @PostMapping("/upload_video")
public Map<String, Integer> handleFileUpload(@RequestParam("video") MultipartFile file) throws IOException {

    if (file.isEmpty()) {
        throw new IllegalArgumentException("Empty file provided.");
    }

    Path tempFile = null;
    try {
        System.out.println("Received video file " + file.getSize());
        Path tempDirPath = Paths.get(TEMP_DIR);
        Files.createDirectories(tempDirPath);
        tempFile = Files.createTempFile(tempDirPath, null, ".mp4");
        file.transferTo(tempFile);

        List<Path> frames = extractor.extractFrames(tempFile.toString(), fps, Frames_DIR);
        JSONArray resultsForHighProb = new JSONArray();

        for (Path framePath : frames) {
            byte[] imageData = Files.readAllBytes(framePath);
            try {

                DetectionResult detectionResult = ObjectDetection.predict(imageData, searchObject,
                        YES_PROBABILITY_THRESHOLD);
                if (detectionResult == null || detectionResult.getDetectedObjects() == null
                        || detectionResult.getImagePath() == null) {
                    continue; // Skip this frame and continue with next
                }

                JSONArray detections = new JSONArray(detectionResult.getDetectedObjects().toJson());
                JSONObject frameResult = new JSONObject();
                frameResult.put("detections", detections);
                frameResult.put("imagePath", detectionResult.getImagePath());

                resultsForHighProb.put(frameResult);
            } catch (Exception e) {
                e.printStackTrace();
                continue; // Log the error and continue processing other frames
            }
        }

        System.out.println("Amount of detections: " + resultsForHighProb.length());

        Map<String, Integer> classNameCounts = new HashMap<>();

        // Iterate through each detection result
        for (int i = 0; i < resultsForHighProb.length(); i++) {
            JSONObject detectionResult = resultsForHighProb.getJSONObject(i);
            JSONArray detections = detectionResult.getJSONArray("detections");

            // Iterate through each detection item
            for (int j = 0; j < detections.length(); j++) {
                JSONObject detectionItem = detections.getJSONObject(j);
                String className = detectionItem.getString("className");

                // Update the count for the class name
                classNameCounts.put(className, classNameCounts.getOrDefault(className, 0) + 1);
            }
        }

        // Print the class name counts
        for (Map.Entry<String, Integer> entry : classNameCounts.entrySet()) {
            System.out.println("ClassName: " + entry.getKey() + ", Amount: " + entry.getValue());
        }

        return classNameCounts;
    } catch (Exception e) {
        e.printStackTrace();
        throw new RuntimeException("Failed to process video file.", e);
    }
}


    private void cleanUpResources(Path tempFile) throws IOException {
        if (tempFile != null && Files.exists(tempFile)) {
            Files.delete(tempFile);
        }
        deleteDirectoryRecursively(Paths.get(TEMP_DIR));
        deleteDirectoryRecursively(Paths.get(Frames_DIR));
        System.out.println("Clean-up done.");
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

    private ArrayList<Classification> check_relevant(DetectedObjects detection,
            @SessionAttribute("searchObject") String searchObject) {

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
    /*
     * @GetMapping("/getFrames")
     * public ResponseEntity<List<String>> getFramePaths() {
     * try {
     * Path framesDir = Paths.get("src\\main\\resources\\static\\HighProb_Frames");
     * List<String> fileNames = Files.walk(framesDir)
     * .filter(Files::isRegularFile)
     * .map(path -> path.getFileName().toString())
     * .collect(Collectors.toList());
     * return ResponseEntity.ok(fileNames);
     * } catch (IOException e) {
     * e.printStackTrace();
     * return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
     * }
     * }
     */
}
