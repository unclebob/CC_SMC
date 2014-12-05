package smc;

import java.util.List;

public class Utilities {
  public static String commaList(List<String> names) {
    String commaList = "";
    boolean first = true;
    for (String name : names) {
      commaList += (first ? "" : ",") + name;
      first = false;
    }
    return commaList;
  }
}
