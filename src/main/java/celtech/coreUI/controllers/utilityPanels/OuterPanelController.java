package celtech.coreUI.controllers.utilityPanels;

import java.net.URL;
import java.util.ResourceBundle;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;

/**
 * FXML Controller class
 *
 * @author Ian
 */
public class OuterPanelController implements Initializable
{

    @FXML
    private VBox outerPanel;
    
    @FXML
    private AnchorPane rootPane;

    @FXML
    private Label title;

    @FXML
    private Pane crossButton;
    
    /**
     * Initialises the controller class.
     */
    @Override
    public void initialize(URL url, ResourceBundle rb)
    {
        crossButton.setOnMouseClicked((MouseEvent event) ->
        {
            rootPane.setVisible(false);
        });
    }
    
    public void setInnerPanel(Node insetPanel)
    {
        outerPanel.getChildren().add(insetPanel);
    }

    public void setTitle(String title)
    {
        this.title.setText(title);
    }

}
