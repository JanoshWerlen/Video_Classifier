package ch.vorlesungstest.vorlesungsbeispiel;

import ai.djl.Application;
import ai.djl.ModelException;
import ai.djl.inference.Predictor;
import ai.djl.modality.cv.Image;
import ai.djl.modality.cv.ImageFactory;
import ai.djl.modality.cv.output.DetectedObjects;
import ai.djl.repository.zoo.Criteria;
import ai.djl.repository.zoo.ModelZoo;
import ai.djl.repository.zoo.ZooModel;
import ai.djl.training.util.ProgressBar;
import ai.djl.translate.TranslateException;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;

import javax.imageio.ImageIO;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.apache.http.entity.StringEntity;
import org.apache.http.HttpResponse;

@Component
public final class ObjectDetection {

    private static final Logger logger = LoggerFactory.getLogger(ObjectDetection.class);
/* 
    private final SimpMessagingTemplate template;

    

    public ObjectDetection(SimpMessagingTemplate template) {
        this.template = template;
    }*/

    public DetectionResult predict(byte[] imageData, String[] targetClass, double probabilityThreshold)
            throws IOException, ModelException, TranslateException {

        for (int i = 0; i < targetClass.length; i++) {
            System.out.println("looking for " + targetClass[i]);
        }

        InputStream is = new ByteArrayInputStream(imageData);
        BufferedImage bi = ImageIO.read(is);
        Image img = ImageFactory.getInstance().fromImage(bi);

        Criteria<Image, DetectedObjects> criteria = Criteria.builder()
                .optApplication(Application.CV.OBJECT_DETECTION)
                .setTypes(Image.class, DetectedObjects.class)
                .optFilter("backbone", "resnet50")
                .optProgress(new ProgressBar())
                .build();

        try (ZooModel<Image, DetectedObjects> model = ModelZoo.loadModel(criteria);
                Predictor<Image, DetectedObjects> predictor = model.newPredictor()) {
            DetectedObjects detection = predictor.predict(img);
            String imagePath = saveBoundingBoxImage(img, detection, targetClass, probabilityThreshold);
            logger.info("Object Detection processing completed.");
            VorlesungsbeispielApplication.notifyWebSocketServer();
            return new DetectionResult(detection, imagePath);
        }
    }

    synchronized private String saveBoundingBoxImage(Image img, DetectedObjects detection, String[] targetClass,
            double probabilityThreshold)
            throws IOException {
        List<String> targetClassList = Arrays.asList(targetClass);
        Path outputDir = Paths.get("src/main/resources/static/predict_img");
        Path displayDir = Paths.get("src/main/resources/static/display");
        // Ensure directory for output images exists
        Files.createDirectories(outputDir);
        // Ensure directory for displayed images exists
        Files.createDirectories(displayDir);
        img.drawBoundingBoxes(detection);

        boolean shouldSave = detection.items().stream()
                .anyMatch(item -> targetClassList.contains(item.getClassName())
                        && item.getProbability() > probabilityThreshold);

        Path displayPath = displayDir.resolve("display.png"); // Always the same name for overwrite
        img.save(Files.newOutputStream(displayPath), "png");

        if (shouldSave) {
            String uniqueID = UUID.randomUUID().toString();
            Path imagePath = outputDir.resolve("detected-" + uniqueID + ".png");

            img.save(Files.newOutputStream(imagePath), "png");

            // Correct the image path to be web accessible
            String webPath = "/predict_img/" + imagePath.getFileName().toString();
            logger.info("Detected objects image has been saved in: {}", imagePath);
            return webPath;
        } else {
            logger.info("No objects meeting the criteria {} were detected.");
            return null;
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
