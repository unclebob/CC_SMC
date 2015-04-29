package smc.lexer;

public interface TokenCollector {//@formatter:off
  void openBrace(int line, int pos);
  void closedBrace(int line, int pos);
  void openParen(int line, int pos);
  void closedParen(int line, int pos);
  void openAngle(int line, int pos);
  void closedAngle(int line, int pos);
  void dash(int line, int pos);
  void colon(int line, int pos);
  void name(String name, int line, int pos);
  void error(int line, int pos);
}//@formatter:on
