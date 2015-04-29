package com.cleancoder.args;

import static com.cleancoder.args.ArgsException.ErrorCode.INVALID_ARGUMENT_FORMAT;
import static com.cleancoder.args.ArgsException.ErrorCode.INVALID_ARGUMENT_NAME;
import static com.cleancoder.args.ArgsException.ErrorCode.INVALID_DOUBLE;
import static com.cleancoder.args.ArgsException.ErrorCode.INVALID_INTEGER;
import static com.cleancoder.args.ArgsException.ErrorCode.MISSING_DOUBLE;
import static com.cleancoder.args.ArgsException.ErrorCode.MISSING_INTEGER;
import static com.cleancoder.args.ArgsException.ErrorCode.MISSING_STRING;
import static com.cleancoder.args.ArgsException.ErrorCode.UNEXPECTED_ARGUMENT;
import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class ArgsExceptionTest {
  @Test
  public void testUnexpectedMessage() throws Exception {
    ArgsException e = new ArgsException(UNEXPECTED_ARGUMENT, 'x', null);
    assertEquals("Argument -x unexpected.", e.errorMessage());
  }

  @Test
  public void testMissingStringMessage() throws Exception {
    ArgsException e = new ArgsException(MISSING_STRING, 'x', null);
    assertEquals("Could not find string parameter for -x.", e.errorMessage());
  }

  @Test
  public void testInvalidIntegerMessage() throws Exception {
    ArgsException e = new ArgsException(INVALID_INTEGER, 'x', "Forty two");
    assertEquals("Argument -x expects an integer but was 'Forty two'.", e.errorMessage());
  }

  @Test
  public void testMissingIntegerMessage() throws Exception {
    ArgsException e = new ArgsException(MISSING_INTEGER, 'x', null);
    assertEquals("Could not find integer parameter for -x.", e.errorMessage());
  }

  @Test
  public void testInvalidDoubleMessage() throws Exception {
    ArgsException e = new ArgsException(INVALID_DOUBLE, 'x', "Forty two");
    assertEquals("Argument -x expects a double but was 'Forty two'.", e.errorMessage());
  }

  @Test
  public void testMissingDoubleMessage() throws Exception {
    ArgsException e = new ArgsException(MISSING_DOUBLE, 'x', null);
    assertEquals("Could not find double parameter for -x.", e.errorMessage());
  }

  @Test
  public void testInvalidArgumentName() throws Exception {
    ArgsException e = new ArgsException(INVALID_ARGUMENT_NAME, '#', null);
    assertEquals("'#' is not a valid argument name.", e.errorMessage());
  }

  @Test
  public void testInvalidFormat() throws Exception {
    ArgsException e = new ArgsException(INVALID_ARGUMENT_FORMAT, 'x', "$");
    assertEquals("'$' is not a valid argument format.", e.errorMessage());
  }
}

