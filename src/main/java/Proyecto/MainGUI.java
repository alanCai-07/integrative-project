package Proyecto;

import Proyecto.View.Usuario.LoginView;
import javafx.application.Application;
import javafx.stage.Stage;

public class MainGUI extends Application {

    @Override
    public void start(Stage primaryStage) {
        new LoginView(primaryStage);
    }

    public static void main(String[] args) {
        launch(args);
    }
}