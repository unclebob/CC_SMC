package smc;


import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static smc.Utilities.compressWhiteSpace;


public class UtilitiesTest {

  @Nested
  public class CompressWhiteSpace {
    @Test
    public void emptyString() {
      assertThat(compressWhiteSpace(""), is(""));
    }

    @Test
    public void NoWhiteSpace() {
      assertThat(compressWhiteSpace("stringwithnowhitespace"), is("stringwithnowhitespace"));
    }

    @Test
    public void oneSpace() {
      assertThat(compressWhiteSpace("one space"), is("one space"));
    }

    @Test
    public void manyWordsWithSingleSpaces() {
      assertThat(compressWhiteSpace("many words with single spaces"), is("many words with single spaces"));
    }

    @Test
    public void oneTab() {
      assertThat(compressWhiteSpace("one\ttab"), is("one tab"));
    }

    @Test
    public void twoTabs() {
      assertThat(compressWhiteSpace("two\t\ttabs"), is("two tabs"));
    }

    @Test
    public void oneReturn() {
      assertThat(compressWhiteSpace("one\nreturn"), is("one\nreturn"));
    }

    @Test
    public void returnsAndSpaces() {
      assertThat(compressWhiteSpace("word \n word"), is("word\nword"));
    }

    @Test
    public void startingWhitespace() {
      assertThat(compressWhiteSpace("\n  this"), is("\nthis"));
    }

    @Test
    public void acceptanceTest() {
      assertThat(compressWhiteSpace("this  is\n\na\t\t  string     \n     \twith\n\nmany\n\n\n\t  whitespaces"),
        is("this is\na string\nwith\nmany\nwhitespaces"));
    }
  }
}
