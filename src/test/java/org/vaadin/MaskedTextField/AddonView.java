package org.vaadin.MaskedTextField;

import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Input;
import com.vaadin.flow.router.Route;

@Route("")
public class AddonView extends Div {

    public AddonView() {
        MaskedTextField field = new MaskedTextField("####-###-###");
        field.setId("test");
        field.addValueChangeListener(l-> System.out.println(l.getValue()));
//        TextField textField = new TextField();
        add(field, new Input());
    }
}
