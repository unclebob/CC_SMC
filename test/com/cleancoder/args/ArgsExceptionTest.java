package com.cleancoder.args;

import org.junit.jupiter.api.Test;

import static com.cleancoder.args.ArgsException.ErrorCode.*;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class ArgsExceptionTest {

  @Test
  public void testUnexpectedMessage() {
    ArgsException e = new ArgsException(UNEXPECTED_ARGUMENT, 'x', null);
    assertEquals("Argument -x unexpected.", e.errorMessage());
  }

  @Test
  public void testMissingStringMessage() {
    ArgsException e = new ArgsException(MISSING_STRING, 'x', null);
    assertEquals("Could not find string parameter for -x.", e.errorMessage());
  }

  @Test
  public void testInvalidIntegerMessage() {
    ArgsException e = new ArgsException(INVALID_INTEGER, 'x', "Forty two");
    assertEquals("Argument -x expects an integer but was 'Forty two'.", e.errorMessage());
  }

  @Test
  public void testMissingIntegerMessage() {
    ArgsException e = new ArgsException(MISSING_INTEGER, 'x', null);
    assertEquals("Could not find integer parameter for -x.", e.errorMessage());
  }

  @Test
  public void testInvalidDoubleMessage() {
    ArgsException e = new ArgsException(INVALID_DOUBLE, 'x', "Forty two");
    assertEquals("Argument -x expects a double but was 'Forty two'.", e.errorMessage());
  }

  @Test
  public void testMissingDoubleMessage() {
    ArgsException e = new ArgsException(MISSING_DOUBLE, 'x', null);
    assertEquals("Could not find double parameter for -x.", e.errorMessage());
  }

  @Test
  public void testInvalidArgumentName() {
    ArgsException e = new ArgsException(INVALID_ARGUMENT_NAME, '#', null);
    assertEquals("'#' is not a valid argument name.", e.errorMessage());
  }

  @Test
  public void testInvalidFormat() {
    ArgsException e = new ArgsException(INVALID_ARGUMENT_FORMAT, 'x', "$");
    assertEquals("'$' is not a valid argument format.", e.errorMessage());
  }
}

