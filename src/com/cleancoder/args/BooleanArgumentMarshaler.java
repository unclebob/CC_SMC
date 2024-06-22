package com.cleancoder.args;

import java.util.Iterator;

public class BooleanArgumentMarshaler implements ArgumentMarshaler {
  private boolean booleanValue = false;

  public void set(Iterator<String> currentArgument) {
    booleanValue = true;
  }

   public static boolean getValue(ArgumentMarshaler am) {
    boolean ret = false;
    if (am instanceof BooleanArgumentMarshaler) {
        ret = ((BooleanArgumentMarshaler) am).booleanValue;
    }
    return ret;
  }
}
