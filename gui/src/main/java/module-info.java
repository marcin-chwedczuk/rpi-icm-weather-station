module pl.marcinchwedczuk.riws.gui {
    requires pl.marcinchwedczuk.riws.icm;

    requires javafx.controls;
    requires javafx.fxml;

    exports pl.marcinchwedczuk.riws.gui;
    exports pl.marcinchwedczuk.riws.gui.mainwindow;

    // Allow @FXML injection to private fields.
    opens pl.marcinchwedczuk.riws.gui.mainwindow;
}