package smc.parser;

public interface Builder {
  void newHeaderWithName();
  void addHeaderWithValue();
  void setStateName();
  void done();
  void setSuperStateName();
  void setEvent();
  void setNullEvent();
  void setEntryAction();
  void setExitAction();
  void setStateBase();
  void setNextState();
  void setNullNextState();
  void transitionWithAction();
  void transitionNullAction();
  void addAction();
  void transitionWithActions();
  void headerError(ParserState state, ParserEvent event, int line, int pos);
  void stateSpecError(ParserState state, ParserEvent event, int line, int pos);
  void transitionError(ParserState state, ParserEvent event, int line, int pos);
  void transitionGroupError(ParserState state, ParserEvent event, int line, int pos);
  void endError(ParserState state, ParserEvent event, int line, int pos);
  void syntaxError(int line, int pos);
  void setName(String name);
}
