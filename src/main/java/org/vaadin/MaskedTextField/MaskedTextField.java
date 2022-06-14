package org.vaadin.MaskedTextField;

import com.vaadin.flow.component.dependency.JsModule;
import com.vaadin.flow.component.dependency.NpmPackage;
import com.vaadin.flow.component.internal.PendingJavaScriptInvocation;
import com.vaadin.flow.component.internal.UIInternals;
import com.vaadin.flow.component.page.PendingJavaScriptResult;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.dom.DomEventListener;
import com.vaadin.flow.dom.DomListenerRegistration;
import com.vaadin.flow.internal.StateNode;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.stream.Collectors;

/**
 * @author Sergey.Tolstykh
 * @version 2.0
 * Date 14.06.2022
 */
@NpmPackage(value = "inputmask", version = "5.0.7")
@JsModule("inputmask/dist/inputmask.js")
public class MaskedTextField extends TextField {
    private MaskType maskType;
    private MaskFormat format;
    private String allowedChars;
    private String mask;
    private boolean containsLiteral;
    private String placeholder;
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

    public enum MaskType {LAZY, EAGER}

    public enum MaskFormat {SWING, INPUTMASK}

    /*
     * Default constructor will recognize InputMask.js style
     * */
    public MaskedTextField(String mask) {
        this(mask, null, true, "_", MaskType.LAZY, MaskFormat.INPUTMASK);
    }

    public MaskedTextField(String mask, String allowedChars, boolean containsLiteral, String placeholder, MaskType maskType, MaskFormat format) {
        this.allowedChars = allowedChars;
        this.mask = mask;
        this.containsLiteral = containsLiteral;
        this.placeholder = placeholder;
        this.maskType = maskType;
        this.format = format;
    }

    @Override
    public void setId(String id) {
        super.setId(id);
        build();
        if (inputTextChangeListener == null)
            addTextChangeListener(l -> valueUpdater(), maskType);
    }

    public void valueUpdater() {
        if (containsLiteral)
            getMaskedValuePromise().then(String.class, value -> setValue(value));
        else
            getUnmaskedValuePromise().then(String.class, value -> setValue(value));
    }

    public PendingJavaScriptResult getUnmaskedValuePromise() {
        return Util.getJavaScriptReturn(getElement().getNode(),"document.getElementById('" + this.getId().get() + "').shadowRoot.querySelector('input').inputmask.unmaskedvalue()");
    }

    public PendingJavaScriptResult getMaskedValuePromise() {
        return Util.getJavaScriptReturn(getElement().getNode(),"document.getElementById('" + this.getId().get() + "').shadowRoot.querySelector('input').inputmask._valueGet()");
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
    public void setValue(String value) {
        super.setValue(value);
    }


    public void applyNewSwingMask(String mask, String placeholder, String allowedChars) {
        applyMask(generateInputmaskJsCode(placeholder, StringUtils.remove(mask, '\''), false,
                genDefinitionsFromSwingMask(mask, allowedChars)));
    }

    public void applyNewMask(String mask) {
        applyMask(generateInputmaskJsCode("_", mask, false));
    }

    protected void build() {
        String inputmask = null;
        switch (format) {
            case SWING:
                inputmask = generateInputmaskJsCode("_", StringUtils.remove(mask, '\''), false,
                        genDefinitionsFromSwingMask(mask, allowedChars));
                break;
            default:
                inputmask = generateInputmaskJsCode("_", mask, false);
                break;
        }
        ;
        applyMask(inputmask);
    }

    private void applyMask(String inputmask) {
        Util.getJavaScriptInvoke(getElement().getNode(),
                "let textfield = document.getElementById('" + this.getId().get() + "')\n" +
                        "let mask = " + inputmask + ";\n" +
"function setmask(element, mask) {\n" +
                        "\treturn new Promise((result)=> {\n" +
                        "\t\tmask.mask(element);\n" +
                        "\t} )\t\n" +
                        "}\n" +
                        "function waitForInput() {\n" +
                        "\treturn new Promise((result)=> {\n" +
                        "\t\twhile (!textfield) {\n" +
                        "\t\t\tsleep(100);\n" +
                        "\t\t}\n" +
                        "\t\twhile (!textfield.shadowRoot.querySelector('input')) {\n" +
                        "\t\t\tsleep(100);\n" +
                        "\t\t}\n" +
                        "\t\t\treturn result(textfield.shadowRoot.querySelector('input'));\n" +
                        "\t} )\t\n" +
                        "}\n" +
                        "\n" +
                        "function sleep(ms) {\n" +
                        "  return new Promise(resolve => setTimeout(resolve, ms));\n" +
                        "}\n" +
                        "\n" +
                        "function setCaretPosition(elem, caretPos) {\n" +
                        "    \n" +
                        "    if(elem != null) {\n" +
                        "        if(elem.createTextRange) {\n" +
                        "            var range = elem.createTextRange();\n" +
                        "            range.move('character', caretPos);\n" +
                        "            range.select();\n" +
                        "        }\n" +
                        "        else {\n" +
                        "            if(elem.selectionStart) {\n" +
                        "                elem.focus();\n" +
                        "                elem.setSelectionRange(caretPos, caretPos);\n" +
                        "            }\n" +
                        "            else\n" +
                        "                elem.focus();\n" +
                        "        }\n" +
                        "    }\n" +
                        "}\n" +
                        "waitForInput().then((result)=>{\n" +
                        "\tlet oldValue;\n" +
                        "\tsetmask(result, mask);\n" +
                        "\tresult.oninput = (e)=> {\n" +
                        "\t\tif (typeof result.inputmask.caretPos === 'undefined') {\n" +
                        "\t\t\tsetCaretPosition(result, 0);\n" +
                        "\t\t}\n" +
                        "\t\telse{\n" +
                        "\t\t\tsetCaretPosition(result, result.inputmask.caretPos.begin);\n" +
                        "\t\t}\n" +
                        "\t\tif (result.inputmask.unmaskedvalue() !== oldValue) {\n" +
                        "\t\t\toldValue = result.inputmask.unmaskedvalue();\n" +
                        "\t\t\ttextfield.dispatchEvent(new Event('input1'));\n" +
                        "\t\t}\n" +
                        "\t}\n" +
                        "\tresult.onchange = (e)=> {\n" +
                        "\t\tif (typeof result.inputmask.caretPos === 'undefined') {\n" +
                        "\t\t\tsetCaretPosition(result, 0);\n" +
                        "\t\t}\n" +
                        "\t\telse{\n" +
                        "\t\t\tsetCaretPosition(result, result.inputmask.caretPos.begin);\n" +
                        "\t\t}\n" +
                        "\t\tif (result.inputmask.unmaskedvalue() !== oldValue) {\n" +
                        "\t\t\toldValue = result.inputmask.unmaskedvalue();\n" +
                        "\t\t\ttextfield.dispatchEvent(new Event('change1'));\n" +
                        "\t\t}\n" +
                        "\t}\n" +
                        "\tresult.onfocus = (e)=> {\n" +
                        "\t\tif (typeof result.inputmask.caretPos === 'undefined') {\n" +
                        "\t\t\tsetCaretPosition(result, 0);\n" +
                        "\t\t}\n" +
                        "\t\telse{\n" +
                        "\t\t\tsetCaretPosition(result, result.inputmask.caretPos.begin);\n" +
                        "\t\t}\n" +
                        "\t}\n" +
                        "\tresult.onkeypress = (e)=> {\n" +
                        "\t\tif (typeof result.inputmask.caretPos === 'undefined') {\n" +
                        "\t\t\tsetCaretPosition(result, 0);\n" +
                        "\t\t}\n" +
                        "\t\telse{\n" +
                        "\t\t\tsetCaretPosition(result, result.inputmask.caretPos.begin);\n" +
                        "\t\t}\n" +
                        "\t}\n" +
                        "\tresult.onkeydown = (e)=> {\n" +
                        "\t \tlet key = e;\n" +
                        "    \tif(key.code == 'Backspace'){\n" +
                        "\t\t\tif (typeof result.inputmask.caretPos === 'undefined') {\n" +
                        "\t\t\t\tsetCaretPosition(result, 0);\n" +
                        "\t\t\t}\n" +
                        "\t\t\telse{\n" +
                        "\t\t\t\tsetCaretPosition(result, result.inputmask.caretPos.begin);\n" +
                        "\t\t\t}\n" +
                        "\t\t}\n" +
                        "\t}\n" +
                        "})\n"
                    );
    }

    /**
     * Listener triggers on lost focus
     */
    public void addTextChangeListener(DomEventListener listener, MaskType maskType) {
        getElement().removeSynchronizedPropertyEvent("input");
        getElement().removeSynchronizedPropertyEvent("change");
        if (inputTextChangeListener != null)
            inputTextChangeListener.remove();
        inputTextChangeListener = getElement().addEventListener(maskType.equals(MaskType.EAGER) ? "input1" : "change1", listener);
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
