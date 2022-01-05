package pl.marcinchwedczuk.riws.gui.mainwindow;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.stage.Stage;
import pl.marcinchwedczuk.riws.icm.IcmMeteo;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.URL;
import java.util.ResourceBundle;

public class MainWindow implements Initializable {
    public static MainWindow showOn(Stage window) {
        try {
            FXMLLoader loader = new FXMLLoader(MainWindow.class.getResource("MainWindow.fxml"));

            Scene scene = new Scene(loader.load());
            MainWindow controller = (MainWindow) loader.getController();

            window.setTitle("Main Window");
            window.setScene(scene);
            window.setResizable(false);

            window.show();

            return controller;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @FXML
    private ImageView imgView;

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
    }

    @FXML
    private void loadRIWS() {
        try {
            byte[] imageBytes = new IcmMeteo().getMeteoImage();
            if (imageBytes == null) return;

            Image img = new Image(new ByteArrayInputStream(imageBytes));
            Image cropped = MeteoImage.cropToTemperatureAndRainCharts(img);
            imgView.setImage(cropped);
        } catch (Exception e) {
            e.printStackTrace();
            UiService.infoDialog("ERROR: " + e.getMessage());
        }
    }
}
