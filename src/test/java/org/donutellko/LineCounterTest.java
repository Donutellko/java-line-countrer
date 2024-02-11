package org.donutellko;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.stream.Stream;

class LineCounterTest {

    @ParameterizedTest
    @MethodSource("testSource")
    @DisplayName("$testName")
    void test(int expected, String testName, String code) {
        System.out.println(testName);
        System.out.println("\"\"\"\n" + code + "\n\"\"\"");
        System.out.println();
        LineCounter lineCounter = new LineCounter(code);
        var actual = lineCounter.count();
        Assertions.assertEquals(expected, actual);
    }

    private static Stream<Arguments> testSource() {
        return Stream.of(
            Arguments.arguments(0, "empty",
                ""),
            Arguments.arguments(0, "whitespaces",
                "        "),
            Arguments.arguments(0, "whitespaces, newline",
                "        \n     "),
            Arguments.arguments(1, "1 line code",
                "i++;"),
            Arguments.arguments(2, "2 line code",
                "i++;\ni--;"),
            Arguments.arguments(1, "1 line code, whitelines",
                "\n\ni++;\n\n"),
            Arguments.arguments(0, "only comment",
                "//hello"),
            Arguments.arguments(0, "only comment and empty lines",
                "    \n   //hello"),
            Arguments.arguments(2, "who lines",
                """
                i++;
                i--;
                """),
            Arguments.arguments(2, "two lines, comments", """
                i++; // hello
                //
                i--;
                """),
            Arguments.arguments(2, "two lines, comments", """
                i++;
                //
                i--;
                """),
            Arguments.arguments(2, "two lines, multi-line comment", """
                i++;
                /* */
                i--;
                """),
            Arguments.arguments(2, "two lines, multi line comments", """
                i++;
                /* 
                i==;
                */
                i--;
                """),
            Arguments.arguments(2, "two lines, string literal", """
                i = "helo";
                i--;
                """),
            Arguments.arguments(2, "two lines, comment starts on line with code", """
                i++; /* 
                i==;
                */
                i--;
                """),
            Arguments.arguments(2, "two lines, code starts when multi-line comment ends", """
                i++;
                /* 
                i==;
                */ j++;
                """),
            Arguments.arguments(2, "two lines, comments /* in string literal", """
                i = "hello /* hello";
                i--;
                """),
            Arguments.arguments(2, "two lines, several multiling comments", """
                i = "hello"; /* hello */ i--; /* hello */
                i--;
                """),
            Arguments.arguments(4, "two lines, divison on separate line", """
                i = 4
                / 
                2;
                i--;
                """)
        );
    }
}