package smc.lexer;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Lexer {
  private TokenCollector collector;
  private int lineNumber;
  private int position;

  public Lexer(TokenCollector collector) {
    this.collector = collector;
  }

  public void lex(String s) {
    lineNumber = 1;
    String lines[] = s.split("\n");
    for (String line : lines) {
      lexLine(line);
      lineNumber++;
    }
  }

  private void lexLine(String line) {
    for (position = 0; position < line.length(); )
      lexToken(line);
  }

  private void lexToken(String line) {
    if (!findToken(line)) {
      collector.error(lineNumber, position + 1);
      position += 1;
    }
  }

  private boolean findToken(String line) {
    return
      findWhiteSpace(line) ||
        findSingleCharacterToken(line) ||
        findName(line);
  }

  private static Pattern whitePattern = Pattern.compile("^\\s+");
  private static Pattern commentPattern = Pattern.compile("^//.*$");
  private static Pattern[] whitePatterns = new Pattern[] {whitePattern, commentPattern};

  private boolean findWhiteSpace(String line) {
    for (Pattern pattern : whitePatterns) {
      Matcher matcher = pattern.matcher(line.substring(position));
      if (matcher.find()) {
        position += matcher.end();
        return true;
      }
    }

    return false;
  }

  private boolean findSingleCharacterToken(String line) {
    String c = line.substring(position, position + 1);
    switch (c) {
      case "{":
        collector.openBrace(lineNumber, position);
        break;
      case "}":
        collector.closedBrace(lineNumber, position);
        break;
      case "(":
        collector.openParen(lineNumber, position);
        break;
      case ")":
        collector.closedParen(lineNumber, position);
        break;
      case "<":
        collector.openAngle(lineNumber, position);
        break;
      case ">":
        collector.closedAngle(lineNumber, position);
        break;
      case "-":
        collector.dash(lineNumber, position);
        break;
      case "*":
        collector.dash(lineNumber, position);
        break;
      case ":":
        collector.colon(lineNumber, position);
        break;
      default:
        return false;
    }
    position++;
    return true;
  }

  private static Pattern namePattern = Pattern.compile("^\\w+");

  private boolean findName(String line) {
    Matcher nameMatcher = namePattern.matcher(line.substring(position));
    if (nameMatcher.find()) {
      collector.name(nameMatcher.group(0), lineNumber, position);
      position += nameMatcher.end();
      return true;
    }
    return false;
  }
}
