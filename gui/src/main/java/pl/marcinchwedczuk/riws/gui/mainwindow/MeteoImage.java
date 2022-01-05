package pl.marcinchwedczuk.riws.gui.mainwindow;

import javafx.scene.image.Image;
import javafx.scene.image.PixelReader;
import javafx.scene.image.WritableImage;

public class MeteoImage {
    public static Image cropToTemperatureRainPressureCharts(Image meteoImage) {
        PixelReader reader = meteoImage.getPixelReader();
        WritableImage newImage = new WritableImage(reader, 0, 30, 482, 280);
        return newImage;
    }
}
