package ch.vorlesungstest.vorlesungsbeispiel;

import ai.djl.Application;
import ai.djl.ModelException;
import ai.djl.inference.Predictor;
import ai.djl.modality.Classifications.Classification;
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
import java.io.IOException;
import java.io.InputStream;

import javax.imageio.ImageIO;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

@Component
public final class ObjectDetection {

    private final SimpMessagingTemplate template;

    private static final Logger logger = LoggerFactory.getLogger(ObjectDetection.class);
    private static int count = 0;

    public ObjectDetection(SimpMessagingTemplate template) {
        this.template = template;
    } // Constructor should not contain logic

    public DetectedObjects predict(byte[] imageData, String targetClass, double probabilityThreshold) throws IOException, ModelException, TranslateException {
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
            saveBoundingBoxImage(img, detection, targetClass, probabilityThreshold);
            logger.info("Object Detection processing completed.");
            return detection;
        }
    }
    

    private void saveBoundingBoxImage(Image img, DetectedObjects detection, String targetClass, double probabilityThreshold)
        throws IOException {
    Path outputDir = Paths.get("src\\main\\resources\\static\\predict_img");
    Files.createDirectories(outputDir);
    img.drawBoundingBoxes(detection);

    // Check each detected object to see if it matches the criteria
    boolean shouldSave = detection.items().stream()
        .anyMatch(item -> item.getClassName().equals(targetClass) && item.getProbability() > probabilityThreshold);

    if (shouldSave) {
        count++;
        Path imagePath = outputDir.resolve("detected-" + targetClass +" "+ count +".png");
        img.save(Files.newOutputStream(imagePath), "png"); // Save the image if conditions are met
        logger.info("Detected objects image has been saved in: {}", imagePath);
        template.convertAndSend("/topic/imageUpdate", imagePath.toString());
    } else {
        logger.info("No objects meeting the criteria were detected.");
    }
}



}
