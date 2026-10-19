package Proyecto;

import Proyecto.View.Usuario.LoginView;
import Proyecto.util.conexionBD;
import javafx.application.Application;
import javafx.stage.Stage;

public class MainGUI extends Application {

    @Override
    public void start(Stage primaryStage) {
        new LoginView(primaryStage);
        Thread precalentamiento = new Thread(() -> conexionBD.precalentar(), "neon-pool-warmup");
        precalentamiento.setDaemon(true);
        precalentamiento.start();
    }

    public static void main(String[] args) {
        launch(args);
    }
}