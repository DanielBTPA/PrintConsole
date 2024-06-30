package br.alphabt.pc;

public enum PrintType {

    DEFAULT(""),
    INFO("color-text=blue; font-type=bold|italic"),
    ERR("color-text=red; font-type=bold|italic"),
    WARNING("color-text=yellow; font-type=bold|italic"),
    ACCEPT("color-text=green; font-type=bold|italic");


    private final String style;

    PrintType(String style) {
        this.style = style;
    }

    PrintConsole.Text colorText(PrintConsole.Text text) {
        return text.setStyle(style);
    }
}