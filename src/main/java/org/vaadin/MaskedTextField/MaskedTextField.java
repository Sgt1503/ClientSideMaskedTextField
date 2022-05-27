package org.vaadin.MaskedTextField;

import com.vaadin.flow.component.customfield.CustomField;
import com.vaadin.flow.component.dependency.CssImport;
import com.vaadin.flow.component.dependency.JsModule;
import com.vaadin.flow.component.dependency.NpmPackage;
import com.vaadin.flow.component.html.Input;
import com.vaadin.flow.component.internal.PendingJavaScriptInvocation;
import com.vaadin.flow.component.internal.UIInternals;
import com.vaadin.flow.dom.DomEventListener;
import com.vaadin.flow.dom.DomListenerRegistration;
import com.vaadin.flow.internal.StateNode;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.stream.Collectors;

/**
 * @author Sergey.Tolstykh
 * @version 2.0
 *          Date 23.05.2022
 */
@NpmPackage(value = "jquery", version = "3.6.0")
@NpmPackage(value = "inputmask", version = "5.0.7")
@JsModule("inputmask/dist/inputmask.js")
@JsModule("inputmask/dist/jquery.inputmask.js")
@JsModule("./src/jquery-loader.js")
@CssImport(value = "./styles/ncore/components/custom-textfield.css")
public class MaskedTextField extends CustomField<String> {
    private MaskType maskType;
    private MaskFormat format;
    private String allowedChars;
    private String mask;
    private boolean containsLiteral;
    private String placeholder;
    private Input input;
    private final char DIGIT = '#';
    private final char ESCAPE_CHAR = '\'';
    private final char UPPERCASE = 'U';
    private final char LOWERCASE = 'L';
    private final char ANY_CHAR_AND_NUM = 'A';
    private final char ANY_CHAR = '?';
    private final char ANY_HEX = 'H';
    private final char DELIMITER = '-';
    private final char ANYTHING = '*';
    private DomListenerRegistration inputTextChangeListener;
    String INPUTMASK = "inputmask";
    public enum MaskType {LAZY, EAGER}
    public enum MaskFormat {SWING, INPUTMASK}

    /*
    * Default constructor will recognize InputMask.js style
    * */
    public MaskedTextField(String mask) {
        this(mask, null, false, "_", MaskType.LAZY, MaskFormat.INPUTMASK);
    }

    public MaskedTextField(String mask, String allowedChars, boolean containsLiteral, String placeholder, MaskType maskType, MaskFormat format) {
        this.allowedChars = allowedChars;
        this.mask = mask;
        this.containsLiteral = containsLiteral;
        this.placeholder = placeholder;
        this.maskType = maskType;
        this.format = format;
        createInput();
    }

    @Override
    public void setId(String id) {
        super.setId(id);
        if (input == null)
            createInput();
        input.setId(id + "-input");
        if (getValue() == null) {
            build();
        }
        getElement().setChild(0, input.getElement());
        if (inputTextChangeListener == null)
            addTextChangeListener(l -> {
                valueUpdater();
            }, maskType);
    }

    public void valueUpdater() {
        if (containsLiteral)
            getMaskedValuePromise().then(String.class, value -> setValue(value));
        else
            getUnmaskedValuePromise().then(String.class, value -> setValue(value));
    }

    public PendingJavaScriptInvocation getUnmaskedValuePromise() {
        return getJavaScriptReturn(input.getElement().getNode(), getJqueryExpression(input.getId().get(), "unmaskedvalue"));
    }

    public PendingJavaScriptInvocation getMaskedValuePromise() {
        return getJavaScriptReturn(input.getElement().getNode(), getJsExpression(input.getId().get(), INPUTMASK + "._valueGet", null));
    }

    @Override
    public String getValue() {
        return super.getValue();
    }

    private PendingJavaScriptInvocation getJavaScriptReturn(StateNode node, String expression) {
        UIInternals.JavaScriptInvocation invocation = new UIInternals.JavaScriptInvocation("return " + expression);
        PendingJavaScriptInvocation pending = new PendingJavaScriptInvocation(node, invocation);
        node.runWhenAttached((ui) -> {
            ui.getInternals().getStateTree().beforeClientResponse(node, (context) -> {
                if (!pending.isCanceled()) {
                    context.getUI().getInternals().addJavaScriptInvocation(pending);
                }
            });
        });
        return pending;
    }

    public void createInput() {
        this.input = new Input();
        input.getElement().setAttribute("required", true);
        input.getElement().setAttribute("autocomplete", "off");
        input.setType("text");
    }

    public String generateInputmaskJsCode(String placeholder, String jsMask, boolean greedy,
                                          Definition... definitions) {
        String jsDefinitions = "{\n";
        if (definitions != null) {
            for (int i = 0; i < definitions.length; i++) {
                jsDefinitions += definitions[i].getDefinitionJs();
                if (i < definitions.length - 1)
                    jsDefinitions += ",\n";
            }
        }
        jsDefinitions += "  \n}";
        String jsCode = "Inputmask(" +
                "{\n" +
                ("    mask: \"" + jsMask + "\"") + ",\n" +
                ("    greedy: " + greedy + ",\n") +
                (definitions != null ? "    definitions: " + jsDefinitions + (placeholder != null ? ",\n" : "") : "") +
                (placeholder != null ? "    placeholder: " + "\"" + placeholder + "\"" + ",\n" : "") +
                "})";

        return jsCode;
    }


    protected Definition[] genDefinitionsFromSwingMask(String mask, String allowedChars) {
        ArrayList<String> regexp = new ArrayList<>();
        if (StringUtils.isEmpty(allowedChars)) {
            for (int i = 0; i < mask.length(); i++) {
                switch (mask.charAt(i)) {
                    case DIGIT: {
                        regexp.add("\"[0-9]\"");
                        break;
                    }
                    case ESCAPE_CHAR: {
                        i += 1;
                        break;
                    }
                    case UPPERCASE: {
                        regexp.add("\"[A-Z А-Я Ё]\"");
                        break;
                    }
                    case LOWERCASE: {
                        regexp.add("\"[a-z а-я ё]\"");
                        break;
                    }
                    case ANY_CHAR_AND_NUM: {
                        regexp.add("\"[a-z A-Z а-я А-Я Ё ё 0-9]\"");
                        break;
                    }
                    case ANYTHING: {
                        regexp.add(
                                "\"[a-z A-Z Ёё а-я А-Я 0-9 \\. \\* \\\\ \\! \\@ \\# \\$ \\% \\^ \\& \\( \\) \\- \\+ \\~ \\| \\/ \\_ \\? \\< \\> \\{ \\} \\` \\' \\[ \\] ]\"");
                        break;
                    }
                    case ANY_CHAR: {
                        regexp.add("\"[a-z A-Z Ёё а-я А-Я]\"");
                        break;
                    }
                    case ANY_HEX: {
                        regexp.add("\"[0-9 A-E]\"");
                        break;
                    }
                    case DELIMITER: {
                        regexp.add("\"-\"");
                        break;
                    }
                    default: {
                        regexp.add("\"[" + mask.charAt(i) + "]\"");
                    }
                }
            }
        } else {
            char[] allowedCharsArray = allowedChars.toCharArray();
            ArrayList<Character> upperChars = new ArrayList();
            ArrayList<Character> lowerChars = new ArrayList();
            ArrayList<Character> digits = new ArrayList();
            ArrayList<Character> others = new ArrayList();
            ArrayList<Character> hexChars = new ArrayList();
            for (char c : allowedCharsArray) {
                if (Character.isLetter(c)) {
                    if (Character.isUpperCase(c))
                        upperChars.add(c);
                    else
                        lowerChars.add(c);
                    if (c == 'a' ||
                            c == 'A' ||
                            c == 'B' ||
                            c == 'b' ||
                            c == 'C' ||
                            c == 'c' ||
                            c == 'D' ||
                            c == 'd' ||
                            c == 'E' ||
                            c == 'e' ||
                            c == 'F' ||
                            c == 'f')
                        hexChars.add(c);
                } else if (Character.isDigit(c))
                    digits.add(c);
                else if (!Character.isDigit(c) && !Character.isLetter(c))
                    others.add(c);
            }
            for (int i = 0; i < mask.length(); i++) {
                switch (mask.charAt(i)) {
                    case DIGIT: {
                        regexp.add("\"[" + regexpCharMixer(digits) + "]\"");
                        break;
                    }
                    case ESCAPE_CHAR: {
                        i += 1;
                        break;
                    }
                    case UPPERCASE: {
                        regexp.add("\"[" + regexpCharMixer(upperChars) + "]\"");
                        break;
                    }
                    case LOWERCASE: {
                        regexp.add("\"[" + regexpCharMixer(lowerChars) + "]\"");
                        break;
                    }
                    case ANY_CHAR_AND_NUM: {
                        regexp.add("\"[" + regexpCharMixer(upperChars, lowerChars, digits) + "]\"");
                        break;
                    }
                    case ANY_CHAR: {
                        regexp.add("\"[" + regexpCharMixer(upperChars, lowerChars) + "]\"");
                        break;
                    }
                    case ANY_HEX: {
                        regexp.add("\"[" + regexpCharMixer(digits, hexChars) + "]\"");
                        break;
                    }
                    case ANYTHING: {
                        regexp.add("\"[" + regexpCharMixer(upperChars, lowerChars, digits, others) + "]\"");
                        break;
                    }
                    case DELIMITER: {
                        regexp.add("\"" + regexpCharMixer(
                                (ArrayList) others.stream().filter(c -> c == '-').collect(Collectors.toList())) + "\"");
                        break;
                    }
                    default: {
                        regexp.add("\"[" + mask.charAt(i) + "]\"");
                    }
                }
            }
        }
        ArrayList<Definition> definitions = new ArrayList<>();
        mask = StringUtils.remove(mask, '-');
        int j = 0;
        for (int i = 0; i < mask.length(); i++) {
            if (mask.charAt(i) == ESCAPE_CHAR) {
                if (i == mask.length() - 2)
                    break;
                i += 2;
            }
            definitions.add(new Definition(mask.charAt(i), regexp.get(j), (mask.charAt(i) == UPPERCASE ? "upper" : mask.charAt(i) == LOWERCASE ? "lower" : null), null));
            j++;
        }
        Definition[] defsArray = new Definition[definitions.size()];
        for (int i = 0; i < definitions.size(); i++) {
            defsArray[i] = definitions.get(i);
        }
        return defsArray;
    }


    protected String regexpCharMixer(ArrayList<Character>... chars) {
        final String[] s = {""};
        if (chars != null) {
            for (ArrayList<Character> a : chars) {
                a.forEach(c ->
                {
                    if (c == '.' ||
                            c == '*' ||
                            c == '\\' ||
                            c == '!' ||
                            c == '@' ||
                            c == '#' ||
                            c == '$' ||
                            c == '%' ||
                            c == '^' ||
                            c == '&' ||
                            c == '(' ||
                            c == ')' ||
                            c == '-' ||
                            c == '+' ||
                            c == '~' ||
                            c == '|' ||
                            c == '/' ||
                            c == '_' ||
                            c == '?' ||
                            c == '<' ||
                            c == '>' ||
                            c == '{' ||
                            c == '}' ||
                            c == '`' ||
                            c == ':' ||
                            c == '\'' ||
                            c == '[' ||
                            c == ']')
                        s[0] += "\\";
                    s[0] += c;
                });

            }
        }
        return s[0];
    }

    /**
     * Swing formatted mask
     *
     * @param mask маска
     * @see javax.swing.text.MaskFormatter
     */
    public void setMask(String mask) {
        this.mask = mask;
        build();
    }

    public void setSwingMask(String mask) {
        this.mask = mask;
        applyNewSwingMask(mask, placeholder, allowedChars);
    }

    /**
     * Inputmask.js formatted mask
     * https://github.com/RobinHerbots/Inputmask/
     *
     * @param mask маска
     */
    public void setInputMask(String mask, MaskType maskType) {
        this.mask = mask;
        this.maskType = maskType;
        applyNewMask(mask);
    }


    /**
     * A list of acceptable characters. Additional restriction of the mask.
     *
     * @param allowedChars A list of acceptable characters.
     */
    public final void setAllowedChars(String allowedChars) {
        this.allowedChars = allowedChars;
        applyNewSwingMask(mask, placeholder, allowedChars);
    }


    @Override
    protected String generateModelValue() {
        return input.getValue();
    }


    @Override
    protected void setPresentationValue(String s) {
        setValue(s);
    }


    @Override
    public void setValue(String value) {
        super.setValue(value);
        if (input != null && value != null)
            input.setValue(value);
    }

    /*
     * Example $('#TEST').mask("test")
     *
     */
    public String getJsExpression(String id, String functionName, String funcParam) {
        return "document.getElementById('" + id + "')." + functionName + "(" + (funcParam != null ? "\"" + funcParam + "\"" : "") + ")";
    }

    /*
     * Example $('#TEST').inputmask('remove')
     *
     */
    public String getJqueryExpression(String id, String funcParam) {
        return "$('#" + id + "')." + INPUTMASK + (funcParam != null ? "(\'" + funcParam + "\');" : "");
    }

    public void applyNewSwingMask(String mask, String placeholder, String allowedChars) {
        getElement().executeJs(generateInputmaskJsCode(placeholder, StringUtils.remove(mask, '\''), false, genDefinitionsFromSwingMask(mask, allowedChars)) + ".mask($('#" + input.getId().get() + "'));");
    }

    public void applyNewMask(String mask) {
        getElement().executeJs(getJqueryExpression(input.getId().get(), mask));
    }

    protected void build(){
        if (format.equals(MaskFormat.SWING))
            getElement().executeJs(generateInputmaskJsCode("_", StringUtils.remove(mask, '\''), false,
                    genDefinitionsFromSwingMask(mask, allowedChars)) + ".mask($('#" + this.input.getId().get() + "'));");
        else if (format.equals(MaskFormat.INPUTMASK))
            getElement().executeJs(generateInputmaskJsCode("_", mask, false) + ".mask($('#" + this.input.getId().get() + "'));");

    }

    /**
     * Listener triggers on lost focus
     */
    public void addTextChangeListener(DomEventListener listener, MaskType maskType) {
        if (inputTextChangeListener != null)
            inputTextChangeListener.remove();
        inputTextChangeListener = getElement().addEventListener(maskType.equals(MaskType.EAGER) ? "keypress" : "focusout", listener);
    }

    public Input getInput() {
        return input;
    }

    public MaskType getMaskType() {
        return maskType;
    }

    public void setMaskType(MaskType maskType) {
        this.maskType = maskType;
        build();
        addTextChangeListener(l -> {
            valueUpdater();
        }, maskType);
    }

    public String getAllowedChars() {
        return allowedChars;

    }

    public String getMask() {
        return mask;
    }

    public boolean isContainsLiteral() {
        return containsLiteral;
    }

    public void setContainsLiteral(boolean containsLiteral) {
        this.containsLiteral = containsLiteral;
        build();
    }

    public String getPlaceholder() {
        return placeholder;
    }

    public void setPlaceholder(String placeholder) {
        this.placeholder = placeholder;
        build();
    }

    private static class Definition {
        char letter;
        String validator;
        String casing;
        String definitionSymbol;

        public Definition(char letter, String validator, String casing, String definitionSymbol) {
            if (letter == ' ' || validator == null)
                throw new NullPointerException("Definition should have at least letter and validator");
            this.letter = letter;
            this.validator = validator;
            this.casing = casing;
            this.definitionSymbol = definitionSymbol;
        }

        String getDefinitionJs() {
            return
                    "     \"" + letter + "\": {\n" +
                            "       validator: " + validator + (casing != null || definitionSymbol != null ? ",\n" : "\n") +
                            (casing != null ? "       casing: " + "\"" + casing + "\"" + (definitionSymbol != null ? "," : "") + " \n" : "") +
                            (definitionSymbol != null ? "       definitionSymbol: " + definitionSymbol : "") +
                            "     }";
        }
    }
}
