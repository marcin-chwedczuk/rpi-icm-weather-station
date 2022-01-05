package pl.marcinchwedczuk.riws.gui.mainwindow;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import pl.marcinchwedczuk.riws.icm.IcmMeteo;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.URL;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ResourceBundle;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicBoolean;

public class MainWindow implements Initializable {
    public static MainWindow showOn(Stage window) {
        try {
            FXMLLoader loader = new FXMLLoader(MainWindow.class.getResource("MainWindow.fxml"));

            Scene scene = new Scene(loader.load());
            MainWindow controller = (MainWindow) loader.getController();

            window.initStyle(StageStyle.UNDECORATED);
            window.setTitle("ICM Meteo");
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

    @FXML
    private Label timeLabel;

    @FXML
    private Label updateTimeLabel;

    private final ExecutorService executorService = Executors.newCachedThreadPool(r -> {
        Thread t = Executors.defaultThreadFactory().newThread(r);
        t.setDaemon(true);
        return t;
    });

    private final AtomicBoolean updateRunning = new AtomicBoolean(false);

    private final Timer timeTimer = new Timer("time-timer", true);
    private final Timer refreshForecastTimer = new Timer("refresh-forecast-timer", true);

    private final DateTimeFormatter HHmm = DateTimeFormatter.ofPattern("HH:mm");

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        timeTimer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                Platform.runLater(() -> {
                    timeLabel.setText(LocalTime.now().format(HHmm));
                });
            }
        }, 0L, 1000L);

        refreshForecastTimer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                scheduleForecastUpdate();
            }
        }, 15 * 1000L, 3 * 60 * 60 * 1000L); // Every 3h, 15s for splash
    }

    private void scheduleForecastUpdate() {
        executorService.submit(() -> {
            updateRunning.set(true);
            try {
                byte[] meteoImage = new IcmMeteo().getMeteoImage();
                if (meteoImage == null) return;

                Image img = new Image(new ByteArrayInputStream(meteoImage));
                Image cropped = MeteoImage.cropToTemperatureRainPressureCharts(img);

                Platform.runLater(() -> {
                    imgView.setImage(cropped);
                    updateTimeLabel.setText(LocalTime.now().format(HHmm));
                });
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                updateRunning.set(false);
            }
        });
    }

    @FXML
    private void loadRIWS() {
        if (updateRunning.get())
            return;

        scheduleForecastUpdate();
    }
}
