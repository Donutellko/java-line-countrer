package org.donutellko;


public class LineCounter {

    private char NEWLINE = '\n';
    private char BACKSLASH = '\\';
    private char STAR = '*';
    private char SLASH = '/';
    private char SINGLE_QUOTE = '\'';
    private char DOUBLE_QUOTE = '"';

    private final char[] code;
    private int count = 0;
    private int pos = 0;
    private Character prev = null;
    private Character cur = null;
    private State state = State.LINE_BEGIN;
    private boolean verboseLogging = false;
    private Integer multilineCommentStartPos = null;
    private Integer multilineCommentEndPos = null;

    public LineCounter(String code) {
        this.code = code.toCharArray();
    }

    public void enableLogging() {
        verboseLogging = true;
    }

    public int count() {
        if (code.length == 0) {
            return 0;
        }
        if (pos > 0) {
            throw new IllegalStateException("callable once");
        }
        cur = code[pos];
        do {
            switch (state) {
                case LINE_BEGIN -> {
                    if (isNewline()) {
                        break;
                    } else if (isWhitespace()) {
                        break;
                    } else if (was(SLASH) && is(SLASH)) {
                        if (canStartSingleLineComment()) {
                            setState(State.INSIDE_SINGLE_LINE_COMMENT);
                        }
                    } else if (was(SLASH) && is(STAR)) {
                        if (canStartMultilineComment()) {
                            setState(State.INSIDE_MULTI_LINE_COMMENT_NO_CODE_BEFORE);
                        }
                    } else if (is(SLASH)) {
                        setState(State.LINE_STARTS_WITH_SLASH);
                    } else if (is(DOUBLE_QUOTE)) {
                        setState(State.INSIDE_STRING_LITERAL);
                        count++;
                    } else {
                        setState(State.LINE_WITH_CODE);
                        count++;
                    }
                }
                case INSIDE_STRING_LITERAL -> {
                    if (isNewline()) {
                        throw new IllegalStateException();
                    } else if (was(BACKSLASH) && is(DOUBLE_QUOTE)) {
                        break;
                    } else if (is(DOUBLE_QUOTE)) {
                        setState(State.LINE_WITH_CODE);
                    }
                }
                case LINE_WITH_CODE -> {
                    if (isNewline()) {
                        setState(State.LINE_BEGIN);
                    } else if (was(SLASH) && is(SLASH)) {
                        if (canStartSingleLineComment()) {
                            setState(State.INSIDE_SINGLE_LINE_COMMENT);
                        }
                    } else if (was(SLASH) && is(STAR)) {
                        if (canStartMultilineComment()) {
                            setState(State.INSIDE_MULTI_LINE_COMMENT_WITH_CODE_BEFORE);
                        }
                    } else if (was(SINGLE_QUOTE) && is(DOUBLE_QUOTE)) {
                        break;
                    } else if (is(DOUBLE_QUOTE)) {
                        setState(State.INSIDE_STRING_LITERAL);
                    }
                }
                case INSIDE_SINGLE_LINE_COMMENT -> {
                    if (isNewline()) {
                        setState(State.LINE_BEGIN);
                    }
                }
                case INSIDE_MULTI_LINE_COMMENT_NO_CODE_BEFORE -> {
                    if (was(STAR) && is(SLASH)) {
                        if (canEndMultilineComment()) {
                            setState(State.LINE_BEGIN);
                        }
                    }
                }
                case INSIDE_MULTI_LINE_COMMENT_WITH_CODE_BEFORE -> {
                    if (isNewline()) {
                        if (canStartMultilineComment()) {
                            setState(State.INSIDE_MULTI_LINE_COMMENT_NO_CODE_BEFORE);
                        }
                    } else if (was(STAR) && is(SLASH)) {
                        if (canEndMultilineComment()) { // for situation with /*/
                            setState(State.LINE_WITH_CODE);
                        }
                    }
                }
                case LINE_STARTS_WITH_SLASH -> {
                    if (isNewline()) {
                        count++;
                        setState(State.LINE_BEGIN);
                    } else if (is(STAR)) {
                        if (canStartMultilineComment()) {
                            setState(State.INSIDE_MULTI_LINE_COMMENT_NO_CODE_BEFORE);
                        }
                    } else if (is(SLASH)) {
                        if (canStartSingleLineComment()) {
                            setState(State.INSIDE_SINGLE_LINE_COMMENT);
                        }
                    } else {
                        setState(State.LINE_WITH_CODE);
                    }
                }
                default -> throw new IllegalStateException("Forgot to define " + this.state);
            }
            if (verboseLogging) {
                print();
            }
        } while (next());
        return count;
    }

    private void setState(State state) {
        if (isMultilineComment(state)) {
            multilineCommentStartPos = pos;
        } else if (isMultilineComment(this.state)) {
            multilineCommentEndPos = pos;
        }
        this.state = state;
    }

    private static boolean isMultilineComment(State state) {
        return state == State.INSIDE_MULTI_LINE_COMMENT_NO_CODE_BEFORE
            || state == State.INSIDE_MULTI_LINE_COMMENT_WITH_CODE_BEFORE;
    }
    private boolean canEndMultilineComment() {
        // for situation with /*/
        return multilineCommentStartPos == null
            || multilineCommentStartPos != pos - 1;
    }

    private boolean canStartSingleLineComment() {
        // for situation with i = 4 /* *// 2;
        System.out.printf("pos = %s, multilineCommentEndPos = %s %n", pos, multilineCommentEndPos);
        return multilineCommentEndPos == null
            || multilineCommentEndPos != pos - 1;
    }

    private boolean canStartMultilineComment() {
        // for situation with */*
        return multilineCommentEndPos == null
            || multilineCommentEndPos != pos - 1;
    }

    private void print() {
        System.out.printf("count=%s. %s%s : %s %n", count, toStr(prev), toStr(cur), state);
    }

    private String toStr(Character c) {
        if (c == null) {
            return "null";
        }
        if (c == '\n') {
            return "\\n  ";
        }
        return "'" + c + "' ";
    }

    boolean is(char c) {
        return c == cur;
    }

    boolean was(char c) {
        return prev != null && c == prev;
    }

    boolean isNewline() {
        return cur == NEWLINE;
    }

    boolean isWhitespace() {
        return cur != NEWLINE && Character.isWhitespace(cur);
    }

    private boolean next() {
        prev = cur;
        pos++;
        if (pos == code.length) {
            return false;
        }
        cur = code[pos];
        return true;
    }

    /*

    WHITELINE and starts SINGLE LINE COMMENT -> skip to \n,
    WHITELINE and starts MULTI LINE COMMENT -> skip to * /
    CODE and starts SINGLE LINE COMMENT -> count++, skip to \n
    CODE and starts STRING_LITERAL -> count++, skip to " without \ before it
    CODE and starts MULTI_LINE_COMMENT -> set CODE_WITH_MULTI_LINE_COMMENT, skip to / *
    CODE_WITH_MULTI_LINE_COMMENT then ends * / -> skip whitespaces,
     */

    private enum State {
        LINE_BEGIN,
        LINE_WITH_CODE,
        LINE_STARTS_WITH_SLASH,
        INSIDE_SINGLE_LINE_COMMENT,
        INSIDE_STRING_LITERAL,
        INSIDE_MULTI_LINE_COMMENT_NO_CODE_BEFORE, // should increment, if there is code after * /
        INSIDE_MULTI_LINE_COMMENT_WITH_CODE_BEFORE; // should not increment, if there is code after * /
    }
}
