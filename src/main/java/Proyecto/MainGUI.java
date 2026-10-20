package Proyecto;

import Proyecto.View.Usuario.LoginView;
import Proyecto.util.conexionBD;
import javafx.application.Application;
import javafx.stage.Stage;

public class MainGUI {
    public static void main(String[] args) {
        Application.launch(TechZoneApplication.class, args);
    }

    public static class TechZoneApplication extends Application {
        @Override
        public void start(Stage primaryStage) {
            new LoginView(primaryStage);
            Thread precalentamiento = new Thread(() -> conexionBD.precalentar(), "neon-pool-warmup");
            precalentamiento.setDaemon(true);
            precalentamiento.start();
        }
    }
}