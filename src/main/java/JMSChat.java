import javafx.application.Application;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import org.apache.activemq.ActiveMQConnectionFactory;

import javax.jms.*;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;

public class JMSChat extends Application {

    private MessageProducer producer;
    private Session session;

    public static void main(String[] args) {
        Application.launch(JMSChat.class,args);
    }

    @Override
    public void start(Stage primaryStage) throws Exception {
        primaryStage.setTitle("JMS Chat");

        BorderPane root = new BorderPane();

        HBox hBoxHeader = new HBox();

        Label labelCode = new Label("Code : ");
        TextField textFieldCode = new TextField("C1");
        textFieldCode.setPromptText("Code");

        Label labelHost = new Label("Host : ");
        TextField textFieldHost = new TextField("localhost");
        textFieldHost.setPromptText("Host");

        Label labelPort = new Label("Port : ");
        TextField textFieldPort = new TextField("61616");
        textFieldPort.setPromptText("Port");

        Button buttonConnecter = new Button("Connecter");

        // Set some padding and spacing CSS to Hbox :)
        hBoxHeader.setPadding(new Insets(10));
        hBoxHeader.setSpacing(10);
        hBoxHeader.setBackground(new Background(new BackgroundFill(Color.ORANGE, CornerRadii.EMPTY,Insets.EMPTY)));

        // Adding Components to Hbox
        hBoxHeader.getChildren().add(labelCode);
        hBoxHeader.getChildren().add(textFieldCode);
        hBoxHeader.getChildren().add(labelHost);
        hBoxHeader.getChildren().add(textFieldHost);
        hBoxHeader.getChildren().add(labelPort);
        hBoxHeader.getChildren().add(textFieldPort);
        hBoxHeader.getChildren().add(buttonConnecter);

        //Adding Hbox to BorderPanne
        root.setTop(hBoxHeader);

        VBox vBox = new VBox();

        GridPane gridPaneForm = new GridPane();

        Label labelTo = new Label("To : ");
        TextField textFieldTo = new TextField("C1");
        textFieldTo.setPromptText("To");
        textFieldTo.setPrefWidth(250);

        Label labelMessage = new Label("Message : ");
        TextArea textAreaMessage = new TextArea();
        textAreaMessage.setPrefRowCount(2);
        textAreaMessage.setPrefWidth(250);

        Label labelImage = new Label("Image :");

        File f = new File("images");
        ObservableList<String> observableListImages = FXCollections.observableArrayList(f.list());
        ComboBox<String> comboBoxImages = new ComboBox<String>(observableListImages);
        comboBoxImages.getSelectionModel().select(0);

        Button buttonEnvoyer = new Button("Envoyer");
        Button buttonEnvoyerImage = new Button("Envoyer Image");

        // Adding components to GridePanne
        gridPaneForm.setPadding(new Insets(10));
        gridPaneForm.setHgap(10);
        gridPaneForm.setVgap(10);
        gridPaneForm.add(labelTo,0,0);
        gridPaneForm.add(textFieldTo,1,0);
        gridPaneForm.add(labelMessage,0,1);
        gridPaneForm.add(textAreaMessage,1,1);
        gridPaneForm.add(buttonEnvoyer,2,1);
        gridPaneForm.add(labelImage,0,2);
        gridPaneForm.add(comboBoxImages,1,2);
        gridPaneForm.add(buttonEnvoyerImage,2,2);

        HBox hBoxChat = new HBox();

        ObservableList<String> observableListMessages = FXCollections.observableArrayList();
        ListView<String> listViewMessages = new ListView<>(observableListMessages);


        File f2 = new File("images/"+comboBoxImages.getSelectionModel().getSelectedItem());
        Image image = new Image(f2.toURI().toString());
        ImageView imageView = new ImageView(image);
        imageView.setFitWidth(320);imageView.setFitHeight(240);

        hBoxChat.setPadding(new Insets(10));
        hBoxChat.setSpacing(10);
        hBoxChat.getChildren().add(listViewMessages);
        hBoxChat.getChildren().add(imageView);

        vBox.setSpacing(10);
        vBox.getChildren().add(gridPaneForm);
        vBox.getChildren().add(hBoxChat);

        root.setCenter(vBox);


        Scene scene = new Scene(root,800,500);
        primaryStage.setScene(scene);
        primaryStage.show();


        // Events for buttons and Combobox

        comboBoxImages.getSelectionModel().selectedItemProperty().addListener(new ChangeListener<String>() {
            @Override
            public void changed(ObservableValue<? extends String> observable, String oldValue, String newValue) {
                 File file = new File("images/"+newValue);
                 Image image1 = new Image(file.toURI().toString());
                 imageView.setImage(image1);
            }
        });


        buttonEnvoyerImage.setOnAction(event -> {
            try {
                observableListMessages.add("Me : "+comboBoxImages.getSelectionModel().getSelectedItem());
                StreamMessage streamMessage = session.createStreamMessage();
                streamMessage.setStringProperty("code",textFieldTo.getText());
                streamMessage.setStringProperty("from",textFieldCode.getText());
                File f4 = new File("images/"+comboBoxImages.getSelectionModel().getSelectedItem());
                FileInputStream fileInputStream = new FileInputStream(f4);
                byte[] data = new byte[(int)f4.length()];
                fileInputStream.read(data);
                streamMessage.writeString(comboBoxImages.getSelectionModel().getSelectedItem());
                streamMessage.writeInt(data.length);
                streamMessage.writeBytes(data);
                producer.send(streamMessage);
            } catch (Exception e) {
                e.printStackTrace();
            }
        });

        buttonEnvoyer.setOnAction(event -> {
            try {
                observableListMessages.add("Me : "+textAreaMessage.getText());
                TextMessage textMessage = session.createTextMessage();
                textMessage.setText(textAreaMessage.getText());
                textMessage.setStringProperty("code",textFieldTo.getText());
                textMessage.setStringProperty("from",textFieldCode.getText());
                producer.send(textMessage);
            } catch (JMSException e) {
                e.printStackTrace();
            }
        });
        buttonConnecter.setOnAction( event -> {
            String codeUser = textFieldCode.getText();
            String host = textFieldHost.getText();
            int port = Integer.parseInt(textFieldPort.getText());

            // Establishing connection to JMS Broker
            ConnectionFactory connectionFactory = new ActiveMQConnectionFactory(
                    "tcp://"+host+":"+port
            );
            try {
                Connection connection = connectionFactory.createConnection();
                session = connection.createSession(false,Session.AUTO_ACKNOWLEDGE);
                Destination destination = session.createTopic("enset.chat");
                MessageConsumer consumer = session.createConsumer(destination,"code='"+codeUser+"'");
                connection.start();
                producer = session.createProducer(destination);
                producer.setDeliveryMode(DeliveryMode.NON_PERSISTENT);

                consumer.setMessageListener(message -> {
                    if (message instanceof TextMessage){
                        TextMessage textMessage = (TextMessage) message;
                        try {
                            observableListMessages.add("From "+textMessage.getStringProperty("from")+" : "+textMessage.getText());
                        } catch (JMSException e) {
                            e.printStackTrace();
                        }
                    }else if (message instanceof StreamMessage){
                        StreamMessage streamMessage = (StreamMessage) message;
                        try {
                            String nomPhoto = streamMessage.readString();
                            int size = streamMessage.readInt();
                            byte[] data = new byte[size];
                            streamMessage.readBytes(data);
                            ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(data);
                            Image image1 = new Image(byteArrayInputStream);
                            imageView.setImage(image1);
                            observableListMessages.add(streamMessage.getStringProperty("from")+" a envoyer une image : "+nomPhoto);
                        } catch (JMSException e) {
                            e.printStackTrace();
                        }
                    }
                });
                hBoxHeader.setDisable(true);
            } catch (JMSException e) {
                e.printStackTrace();
            }
        });
    }
}
