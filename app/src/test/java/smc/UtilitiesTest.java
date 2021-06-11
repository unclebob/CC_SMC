package smc;

import de.bechte.junit.runners.context.HierarchicalContextRunner;
import org.junit.Test;
import org.junit.runner.RunWith;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static smc.Utilities.compressWhiteSpace;

@RunWith(HierarchicalContextRunner.class)
public class UtilitiesTest {

  public class CompressWhiteSpace {
    @Test
    public void emptyString() throws Exception {
      assertThat(compressWhiteSpace(""), is(""));
    }

    @Test
    public void NoWhiteSpace() throws Exception {
      assertThat(compressWhiteSpace("stringwithnowhitespace"), is("stringwithnowhitespace"));
    }

    @Test
    public void oneSpace() throws Exception {
      assertThat(compressWhiteSpace("one space"), is("one space"));
    }

    @Test
    public void manyWordsWithSingleSpaces() throws Exception {
      assertThat(compressWhiteSpace("many words with single spaces"), is("many words with single spaces"));
    }

    @Test
    public void oneTab() throws Exception {
      assertThat(compressWhiteSpace("one\ttab"), is("one tab"));
    }

    @Test
    public void twoTabs() throws Exception {
      assertThat(compressWhiteSpace("two\t\ttabs"), is("two tabs"));
    }

    @Test
    public void oneReturn() throws Exception {
      assertThat(compressWhiteSpace("one\nreturn"), is("one\nreturn"));
    }

    @Test
    public void returnsAndSpaces() throws Exception {
      assertThat(compressWhiteSpace("word \n word"), is("word\nword"));
    }

    @Test
    public void startingWhitespace() throws Exception {
      assertThat(compressWhiteSpace("\n  this"), is("\nthis"));
    }

    @Test
    public void acceptanceTest() throws Exception {
      assertThat(compressWhiteSpace("this  is\n\na\t\t  string     \n     \twith\n\nmany\n\n\n\t  whitespaces"),
        is("this is\na string\nwith\nmany\nwhitespaces"));
    }
  }
}
