package org.vaadin.MaskedTextField;

import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.Route;

@Route("")
public class AddonView extends Div {

    public AddonView() {
        MaskedTextField field = new MaskedTextField("####-###-###");
        field.setId("test");
        TextField textField = new TextField();
        add(field, textField);
    }
}
