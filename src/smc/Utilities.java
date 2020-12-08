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

  public static String iotaList(String typeName, List<String> names) {
    String iotaList = "";
    boolean first = true;
    for (String name : names) {
      iotaList += "\t" + name + (first ? " " + typeName + " = iota" : "") + "\n";
      first = false;
    }
    return iotaList;
  }

  public static String capitalize(String s) {
    if (s.length() < 2) return s.toUpperCase();
    return s.substring(0, 1).toUpperCase() + s.substring(1);
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
