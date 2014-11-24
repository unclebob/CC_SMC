package com.cleancoder.args;

import java.util.Iterator;

public interface ArgumentMarshaler {
  void set(Iterator<String> currentArgument) throws ArgsException;
}
