package smc;

import java.util.ArrayList;
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

  public static List<String> addPrefix(String prefix, List<String> list) {
    List<String> result = new ArrayList<>();
    for (String element : list)
      result.add(prefix + element);
    return result;
  }

  public static String compressWhiteSpace(String s) {
    return s.replaceAll("\\n+", "\n").replaceAll("[\t ]+", " ").replaceAll(" *\n *", "\n");
  }
}
