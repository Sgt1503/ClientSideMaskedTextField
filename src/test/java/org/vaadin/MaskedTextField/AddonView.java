package org.vaadin.MaskedTextField;

import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Input;
import com.vaadin.flow.router.Route;

@Route("")
public class AddonView extends Div {

    public AddonView() {
        MaskedTextField field = new MaskedTextField("####-###-###");
        field.setId("test");
        field.addTextChangeListener(l-> System.out.println(field.getValue()), MaskedTextField.MaskType.LAZY);
        add(field, new Input());
    }
}
