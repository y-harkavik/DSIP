package main;

import javafx.embed.swing.SwingFXUtils;
import javafx.event.ActionEvent;
import javafx.scene.control.Alert;
import javafx.scene.control.TextField;
import javafx.scene.image.ImageView;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import main.algorithm.OtsuMethod;
import main.filter.FilterEngine;
import main.filter.FilterStrategy;
import main.filter.impl.DilationFilter;
import main.filter.impl.ErosionFilter;
import main.filter.impl.FilterEngineImpl;
import main.filter.impl.MedianFilter;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

public class Controller {
    public ImageView sourceImageView;
    public ImageView grayscaleImageView;
    public ImageView filteredImageView;
    public ImageView binaryImageView;
    public ImageView erosionImageView;
    public ImageView dilationImageView;
    public TextField filterTimesTextField;
    public TextField erosionTimesTextField;
    public TextField dilationTimesTextField;

    private static final String[] IMAGES_EXTENSIONS = {"*.png", "*.jpg", "*.jpeg", "*.bmp"};

    private BufferedImage sourceBufferedImage;
    private BufferedImage grayscaleBufferedImage;
    private BufferedImage filteredBufferedImage;
    private BufferedImage binarizedBufferedImage;
    private BufferedImage erosionBufferedImage;
    private BufferedImage dilationBufferedImage;

    private FilterEngine filterEngine = new FilterEngineImpl();

    public void openImageAction(ActionEvent actionEvent) {
        try {
            File imageFile = getFileByMask("Image", IMAGES_EXTENSIONS);

            if (imageFile == null) {
                createAlertError("Cannot open file");
                return;
            }

            sourceBufferedImage = ImageIO.read(imageFile);
            if (sourceBufferedImage.getType() != BufferedImage.TYPE_BYTE_GRAY && sourceBufferedImage.getType() != BufferedImage.TYPE_3BYTE_BGR) {
                sourceBufferedImage = null;
                createAlertError("Image must be grayscale");
                return;
            }
            setImage(sourceImageView, sourceBufferedImage);
        } catch (IOException e) {
            createAlertError("Cannot open image");
        }
    }

    public void convertImageAction(ActionEvent actionEvent) {
        if (sourceBufferedImage != null) {
            grayscaleBufferedImage = filterEngine.rgbToGrayscale(sourceBufferedImage);
            setImage(grayscaleImageView, grayscaleBufferedImage);
        }
    }

    public void filterImageAction(ActionEvent actionEvent) {
        if (grayscaleBufferedImage != null) {
            FilterStrategy filterStrategy = new MedianFilter();
            BufferedImage temp = grayscaleBufferedImage;

            for (int i = 0; i < Integer.parseInt(filterTimesTextField.getText()); i++) {
                temp = filterEngine.doFilter(temp,
                        filterStrategy);
            }
            filteredBufferedImage = temp;

            setImage(filteredImageView, filteredBufferedImage);
        }
    }

    public void binaryImageAction(ActionEvent actionEvent) {
        if (filteredBufferedImage != null) {
            binarizedBufferedImage = filterEngine.doFilter(filteredBufferedImage, new OtsuMethod());
            setImage(binaryImageView, binarizedBufferedImage);
        }
    }

    public void erosionImageAction(ActionEvent actionEvent) {
        if (binarizedBufferedImage != null) {
            FilterStrategy filterStrategy = new ErosionFilter();
            BufferedImage temp = binarizedBufferedImage;

            for (int i = 0; i < Integer.parseInt(erosionTimesTextField.getText()); i++) {
                temp = filterEngine.doFilter(temp,
                        filterStrategy);
            }
            erosionBufferedImage = temp;

            setImage(erosionImageView, erosionBufferedImage);
        }
    }

    public void dilationImageAction(ActionEvent actionEvent) {
        if (erosionBufferedImage != null) {
            FilterStrategy filterStrategy = new DilationFilter();
            BufferedImage temp = erosionBufferedImage;

            for (int i = 0; i < Integer.parseInt(dilationTimesTextField.getText()); i++) {
                temp = filterEngine.doFilter(temp,
                        filterStrategy);
            }
            dilationBufferedImage = temp;

            setImage(dilationImageView, dilationBufferedImage);
        }
    }

    private File getFileByMask(String neededFile, String[] mask) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.getExtensionFilters().addAll(new FileChooser.ExtensionFilter(neededFile, mask));
        return fileChooser.showOpenDialog(new Stage());
    }

    public static void createAlertError(String alertMessage) {
        Alert alertFileNotFound = new Alert(Alert.AlertType.ERROR);
        alertFileNotFound.setTitle("Hello amigo");
        alertFileNotFound.setContentText(alertMessage);
        alertFileNotFound.showAndWait();
    }

    private void setImage(ImageView imageView, BufferedImage bufferedImage) {
        imageView.setImage(SwingFXUtils.toFXImage(bufferedImage, null));
    }
}
