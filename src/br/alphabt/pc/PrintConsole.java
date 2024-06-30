package br.alphabt.pc;

import java.io.IOException;
import java.util.*;

public final class PrintConsole {

    private final List<Text> texts;


    PrintConsole() {
        this.texts = new ArrayList<>();
    }

    public Text inline(String text) {
        if (text == null) throw new NullPointerException("This is not can't be null.");

        texts.add(new InlineText(this, text));

        return texts.getLast();
    }

    public Text inline(Object text) {
        if (text == null) throw new NullPointerException("This is not can't be null.");

        texts.add(new InlineText(this, text));

        return texts.getLast();
    }

    public Text inline(String... text) {
        if (text == null || text.length == 0) throw new NullPointerException("This is not can't be null.");

        this.texts.add(new InlineText(this, (Object[]) text));

        return texts.getLast();
    }

    public Text inline(Object... text) {
        if (text == null || text.length == 0) throw new NullPointerException("This is not can't be null.");

        this.texts.add(new InlineText(this, text));

        return texts.getLast();
    }

    public Text multiline(String text) {
        if (text == null) throw new NullPointerException("This is not can't be null.");

        this.texts.add(new MultilineText(this, text));

        return texts.getLast();
    }

    public Text multiline(Object text) {
        if (text == null) throw new NullPointerException("This is not can't be null.");
        this.texts.add(new MultilineText(this, text));

        return texts.getLast();
    }

    public Text multiline(String... text) {
        if (text == null || text.length == 0) throw new NullPointerException("This is not can't be null.");

        this.texts.add(new MultilineText(this, text));

        return texts.getLast();
    }

    public Text multiline(Object... text) {
        if (text == null || text.length == 0) throw new NullPointerException("This is not can't be null.");

        this.texts.add(new MultilineText(this, text));

        return texts.getLast();
    }

    public void printText(String text, int width, String align, String style, int breakLine) {
        this.inline(text).setStyle(style)
                .setAlign(align, width).setBreakLine(breakLine).done().print();
    }

    public void printMessage(String msg, PrintType pt) {
        this.printMessage(msg, "... <Press any key>", pt);
    }

    public void printMessage(String msg, String suffix, PrintType pt) {
        Text text = inline(msg).append(suffix);
        pt.colorText(text).breakLine();
        print();
        pause();
    }

    public Text getLastText() {
        return texts.getLast();
    }

    public void print() {

        for (Text text : texts) {
            System.out.print(text.getFormattedText());
        }

        texts.clear();
    }

    public void print(String... text) {
        this.inline(text);
        this.print();
    }

    public void print(Object... text) {
        this.inline(text);
        this.print();
    }

    public void printLine(String... text) {
        this.multiline(text).breakLine();
        this.print();
    }

    public void printLine(Object... text) {
        this.multiline(text).breakLine();
        this.print();
    }

    public void clear() {
        texts.clear();
    }

    public static abstract class Text {

        private Text(PrintConsole pc, Object... objects) {
            this.pc = pc;

            if (objects != null) {
                this.values = new ArrayList<>(List.of(objects));
            } else {
                this.values = new ArrayList<>();
            }
        }

        protected static final Map<String, String[]> styleValues = Map.ofEntries(
                new AbstractMap.SimpleImmutableEntry<>("white", new String[]{"color", "\033[37m", "\033[47m"}),
                new AbstractMap.SimpleImmutableEntry<>("cyan", new String[]{"color", "\033[36m", "\033[46m"}),
                new AbstractMap.SimpleImmutableEntry<>("magenta", new String[]{"color", "\033[35m", "\033[45m"}),
                new AbstractMap.SimpleImmutableEntry<>("blue", new String[]{"color", "\033[34m", "\033[44m"}),
                new AbstractMap.SimpleImmutableEntry<>("yellow", new String[]{"color", "\033[33m", "\033[43m"}),
                new AbstractMap.SimpleImmutableEntry<>("green", new String[]{"color", "\033[32m", "\033[42m"}),
                new AbstractMap.SimpleImmutableEntry<>("red", new String[]{"color", "\033[31m", "\033[41m"}),
                new AbstractMap.SimpleImmutableEntry<>("black", new String[]{"color", "\033[30m", "\033[40m"}),
                new AbstractMap.SimpleImmutableEntry<>("normal", new String[]{"font", "\033[0m"}),
                new AbstractMap.SimpleImmutableEntry<>("bold", new String[]{"font", "\033[1m"}),
                new AbstractMap.SimpleImmutableEntry<>("italic", new String[]{"font", "\033[3m"}),
                new AbstractMap.SimpleImmutableEntry<>("underline", new String[]{"font", "\033[4m"}),
                new AbstractMap.SimpleImmutableEntry<>("rgb", new String[]{"format", "\033[38;2;%s;%s;%sm", "\033[48;2;%s;%s;%sm"}));

        protected final PrintConsole pc;
        protected final List<Object> values;
        protected int spacing = 0;
        protected int breakLine = 0;
        protected String style = "";
        protected int repeat = 1;
        protected int alignWidth = 1;
        protected String alignType = "left";

        protected boolean enumerate, mark;

        protected char charToMark = '•';
        protected int margin = 0;

        public Text setMargin(int margin) {
            this.margin = margin;
            return this;
        }

        public Text append(String text) {
            values.add(text);
            return this;
        }

        public Text append(Object obj) {
            values.add(obj);
            return this;
        }

        public Text append(String... text) {
            values.addAll(List.of(text));
            return this;
        }

        public Text append(Object... obj) {
            values.addAll(List.of(obj));
            return this;
        }

        public Text setRepeat(int repeat) {
            this.repeat = repeat;
            return this;
        }

        public Text setSpacing(int sz) {
            this.spacing = sz;
            return this;
        }

        public Text setStyle(String style) {
            this.style = style;
            return this;
        }

        public Text setAlign(String type, int width) {
            this.alignType = type;
            this.alignWidth = width;
            return this;
        }

        public Text breakLine() {
            breakLine += 1;
            return this;
        }

        public Text doubleBreakLine() {
            breakLine += 2;
            return this;
        }

        public Text enumerate() {
            this.enumerate = true;
            return this;
        }

        public Text mark() {
            this.mark = true;
            return this;
        }

        public Text mark(char c) {
            this.mark = true;
            this.charToMark = c;
            return this;
        }

        public Text spacing() {
            spacing += 1;
            return this;
        }

        public Text setBreakLine(int sz) {
            this.breakLine = sz;
            return this;
        }

        public PrintConsole done() {
            return pc;
        }

        public Text inlineAsNew(String... text) {
            return pc.inline(text);
        }

        public Text multilineAsNew(String... text) {
            return pc.multiline(text);
        }

        public Text inlineAsNew(Object... text) {
            return pc.inline(text);
        }

        public Text multilineAsNew(Object... text) {
            return pc.multiline(text);
        }

        public String getFormattedText() {
            return ("\n".repeat(breakLine));
        }

        protected String queryStyleCommand() {
            final StringBuilder formatted = new StringBuilder();

            String[] fs = style.replace(" ", "").split(";");

            for (String s : fs) {
                String[] ss = s.replace(" ", "").split("=");

                if (ss.length > 1) {
                    String k = ss[0], v = ss[1];

                    if (k.startsWith("font-type")) {
                        String[] nsv = v.replace(" ", "").split("\\|");

                        for (String font : nsv) {
                            String[] fonts = styleValues.get(font);
                            if (fonts != null && fonts[0].equals("font")) {
                                formatted.append(fonts[1]);
                            }
                        }
                    } else if (k.startsWith("color-")) {
                        String[] colors = styleValues.get(v);
                        if (colors != null && colors[0].equals("color")) {
                            formatted.append(k.endsWith("text") ? colors[1] : k.endsWith("bg") ? colors[2] : "");
                        } else {
                            String bte = k.endsWith("text") ? styleValues.get("rgb")[1] : k.endsWith("bg") ? styleValues.get("rgb")[2] : "";

                            if (v.matches("^\\(\\d+,\\d+,\\d+\\)$")) {
                                String[] rgb = v.replaceAll("[()]", "").
                                        replace(" ", "").split(",");
                                formatted.append(String.format(bte, rgb[0], rgb[1], rgb[2]));

                            } else if (v.matches("^#[A-Fa-f0-9]{6}$")) {
                                String hex = v.replace("#", "");
                                String[] rgb = new String[3];

                                for (int i = 0; i < rgb.length; i++) {
                                    String subHex = hex.substring(i * 2, (i + 1) * 2);
                                    rgb[i] = String.valueOf(Integer.parseInt(subHex, 16));
                                }

                                formatted.append(String.format(bte, rgb[0], rgb[1], rgb[2]));
                            }
                        }
                    }
                }
            }

            return formatted.toString();
        }

        protected String alignText(String text) {
            //text = (text.length() % 2 != 0 && !alignType.equals("right")) ? text.concat(" ") : text;
            int padding = (alignWidth - text.length()) / 2;

            return switch (alignType) {
                case "center" -> String.format("%" + padding + "s%s%" + padding + "s", "", text, "");
                case "left" -> String.format("%-" + alignWidth + "s", " ".repeat(margin) + text);
                case "right" -> String.format("%" + alignWidth + "s", text + " ".repeat(margin));
                default -> throw new IllegalStateException("Unexpected value: " + alignType);
            };
        }

        private Text copyFormat(Text text) {
            this.style = text.style;
            this.alignWidth = text.alignWidth;
            this.breakLine = text.breakLine;
            this.spacing = text.spacing;
            this.alignType = text.alignType;
            this.repeat = text.repeat;
            this.margin = text.margin;
            return this;
        }

    }

    private static class InlineText extends Text {
        private InlineText(PrintConsole pc, Object... objects) {
            super(pc, objects);
        }

        private InlineText(PrintConsole pc, String... objects) {
            super(pc, (Object[]) objects);
        }

        @Override
        public String getFormattedText() {
            return formattedText() + super.getFormattedText();
        }

        private String formattedText() {
            StringBuilder sb = new StringBuilder();

            for (int i = 0; i < this.values.size(); i++) {
                String text = (enumerate ? String.valueOf(i + 1).concat(". ") : mark ? (charToMark + " ") : "")
                        .concat(String.valueOf(this.values.get(i)).repeat(repeat));
                text = text.concat(i < this.values.size() - 1 ? " ".repeat(spacing) : "");

                sb.append(text);
            }
            String format = sb.toString();
            sb.delete(0, sb.length());
            sb.append(queryStyleCommand())
                    .append(alignText(format))
                    .append(styleValues.get("normal")[1]);

            return sb.toString();

        }
    }

    private static class MultilineText extends Text {

        private MultilineText(PrintConsole pc, Object... objects) {
            super(pc, objects);
        }

        private MultilineText(PrintConsole pc, String... objects) {
            super(pc, (Object[]) objects);
        }

        @Override
        public String getFormattedText() {
            return formattedText() + super.getFormattedText();
        }

        private String formattedText() {
            StringBuilder sb = new StringBuilder();

            for (int i = 0; i < this.values.size(); i++) {
                String text = String.valueOf(this.values.get(i));

                sb.append(queryStyleCommand())
                        .append(alignText((enumerate ? String.valueOf(i + 1).concat(". ") : mark ? (charToMark + " ") : "").concat(text.repeat(repeat))))
                        .append(styleValues.get("normal")[1])
                        .append(i < values.size() - 1 ? "\n".repeat(spacing + 1) : "");
            }

            return sb.toString();
        }

    }

    public static PrintConsole getPrintConsole(PartConsole partConsole) {
        return partConsole.getPrintConsole();
    }

    public static void pause() {
        if (Thread.currentThread() == ConsoleManager.cmThread) {
            try {
                System.in.read();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }


}
