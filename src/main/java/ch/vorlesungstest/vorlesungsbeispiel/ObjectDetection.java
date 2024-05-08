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

    private static final Logger logger = LoggerFactory.getLogger(ObjectDetection.class);

    public ObjectDetection() {
    
    } // Constructor should not contain logic

    public DetectedObjects predict(byte[] imageData) throws IOException, ModelException, TranslateException {
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
            saveBoundingBoxImage(img, detection);
            logger.info("Object Detected and Bounding Box placed");
            System.out.println(detection);
            //ArrayList<Classification> dogs = check_relevant(detection);
           // System.out.println(dogs.size() + " dogs have been detected");
            return detection;
        }
    }

    private void saveBoundingBoxImage(Image img, DetectedObjects detection)
            throws IOException {
        Path outputDir = Paths.get("src\\main\\resources\\static\\predict_img");
        Files.createDirectories(outputDir);
        img.drawBoundingBoxes(detection);

        Path imagePath = outputDir.resolve("detected.png");

        //Code here 

        img.save(Files.newOutputStream(imagePath), "png"); // Saving as PNG
        //logger.info("Detected objects image has been saved in: {}", imagePath);
    }


}
