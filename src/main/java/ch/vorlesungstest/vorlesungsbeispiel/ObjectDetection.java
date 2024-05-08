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
import java.io.IOException;
import java.io.InputStream;

import javax.imageio.ImageIO;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
            return new DetectionResult(detection, imagePath);
        }
    }

    private String saveBoundingBoxImage(Image img, DetectedObjects detection, String[] targetClass, double probabilityThreshold)
        throws IOException {
    List<String> targetClassList = Arrays.asList(targetClass);
    Path outputDir = Paths.get("src/main/resources/static/predict_img");
    Files.createDirectories(outputDir);
    img.drawBoundingBoxes(detection);

    boolean shouldSave = detection.items().stream()
        .anyMatch(item -> targetClassList.contains(item.getClassName()) && item.getProbability() > probabilityThreshold);

    if (shouldSave) {
        count++;
        Path imagePath = outputDir.resolve("detected-" + count + ".png");
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


}
