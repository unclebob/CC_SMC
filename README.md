##The Care and Feeding of 
#SMC
##The State Machine Compiler

SMC is a Java application that translates a state transition table into a program that implements the described state machine.  Output languages include Java, Go, Dart, C, and C++.  Adding other languages is trivial.

###Command Line
`ant compile && ant jar`

`java -jar smc.jar -l <language> -o <directory> -f <flags>`

 * `<language>` is one of: `C`, `Cpp`, `Dart`, `Go`, or `Java`.
 * `<directory>` is the output directory.  Your new state machine will be written there.
 * `<flags>` currently for Java only.  `package:package_name` will put the appropriate `package` statement in the generated code.

###Syntax
The syntax for the state transition table is based on a simple state transition table.  Here is a straightforward example that describes the logic of a subway turnstile.  `turnstile.sm`:

    Initial: Locked
    FSM: Turnstile
    {
      Locked    Coin    Unlocked    unlock
      Locked    Pass    Locked      alarm
      Unlocked  Coin    Unlocked    thankyou
      Unlocked  Pass    Locked      lock
    }

When this is run through SMC it produces the source code for a state machine named `Turnstile`.  That machine starts in the `Locked` state, and follows the following logic:

* Given we are in the `Locked` state, when we get a `Coin` event, then we transition to the `Unlocked` state and invoke the `unlock` action.
* Given we are in the `Locked` state, when we get a `Pass` event, then we stay in the `Locked` state and invoke the `alarm` action.
* Given we are in the `Unlocked` state, when we get a `Coin` event, then we stay in the `Unlocked` state and invoke the `thankyou` action.
* GIven we are in the `Unlocked` state, when we get a `Pass` event, then we transition to the `Locked` state and invoke the `lock` action. 

###Opacity
One of the goals of SMC is to produce code that the programmer never needs to look at, and does not check in to source code control.  It is intended that SMC will generate the appropriate code during the pre-compile phase of your build.  

The output of SMC is two sets of functions: The _Event_ functions and the _Actions_ functions.  For most languages these functions will be arranged into an abstract class in which the _Event_ functions are public, and the _Action_ functions are protected and abstract.  

The programmer derives an implementation class from the generated class, and implements all the action functions.  The programmer then creates an instance of the implementation class and invokes the appropriate event functions as those events occur.  The generated code will make sure that the appropriate action functions are called in response to those events.

The state of the generated state machine is opaque to the programmer and kept private to the generated code.  From the programmer's point of view the generated code is a black box that translates events into actions.  

Here is a UML diagram that depicts the situation.  The programmer writes the `user` and the `Implementation`.  The generated `Turnstile` class is opaque.

                   +---------------+
    +------+       | <<generated>> |
    | user |------>|   Turnstile   |
    +------+       +---------------+
                   | + Coin()      |
                   | + Pass()      |
                   +---------------+
                           A
                           |
                           |
              +-------------------------+
              |     Implementation      |
              +-------------------------+
              | # lock()                |
              | # unlock()              |
              | # thankyou()            |
              | # alarm()               |
              | # unhandledTransition() |
              +-------------------------+

### Generated Java Code
Here is the Java code that SMC will generate for this state machine.  It is an abstract class that provides a simple nested switch/case implementation, a set of public event functions, and a set of protected abstract methods for the action functions.

    public abstract class Turnstile {
    	public abstract void unhandledTransition(String state, String event);
        private enum State {Locked,Unlocked}
        private enum Event {Coin,Pass}
    	private State state = State.Locked;
    	private void setState(State s) {state = s;}
    	public void Coin() {handleEvent(Event.Coin);}
    	public void Pass() {handleEvent(Event.Pass);}
    	private void handleEvent(Event event) {
    		switch(state) {
    			case Locked:
    			switch(event) {
    				case Coin:
    				setState(State.Unlocked);
    				unlock();
    				break;
    				case Pass:
    				setState(State.Locked);
    				alarm();
    				break;
    				default: unhandledTransition(state.name(), event.name()); break;
    			}
    			break;
    			case Unlocked:
    			switch(event) {
    				case Coin:
    				setState(State.Unlocked);
    				thankyou();
    				break;
    				case Pass:
    				setState(State.Locked);
    				lock();
    				break;
    				default: unhandledTransition(state.name(), event.name()); break;
    			}
    			break;
    		}
    	}
    	protected abstract void thankyou();
    	protected abstract void unlock();
    	protected abstract void alarm();
    	protected abstract void lock();
    }
    
### Actions Interface
It is often more convenient to express the abstract _Action_ functions as an interface, or an abstract class.    We can accomplish this by adding the `Actions:` header to the state machine description.  

    Initial: Locked
    FSM: Turnstile
    Actions: TurnstileActions
    {
      ...
    }

The programer will write the `TurnstileActions` interface to declare all the _Action_ functions.  SMC will generate code that implements that interface.   Here how this looks in UML:

              +-------------------------+
              |      <<interface>>      |
              |     Implementation      |
              +-------------------------+
              | + lock()                |
              | + unlock()              |
              | + thankyou()            |
              | + alarm()               |
              +-------------------------+
                           A
                           |
                   +---------------+
    +------+       | <<generated>> |
    | user |------>|   Turnstile   |
    +------+       +---------------+
                   | + Coin()      |
                   | + Pass()      |
                   +---------------+
                           A
                           |
                           |
              +-------------------------+
              |     Implementation      |
              +-------------------------+
              | # lock()                |
              | # unlock()              |
              | # thankyou()            |
              | # alarm()               |
              | # unhandledTransition() |
              +-------------------------+

NOTE: This is optional for Java, but necessary in C and C++.  In C, the `TurnstileActions` interface is implemented as a `struct` holding pointers to functions to the _Action_ functions.  (See the test_cases directory).  

### Syntactic Sugar.

While the syntax described so far is sufficient for any state machine, there are things we can do to make it more convenient.  For example, the Turnstile can be expressed more simply by combining the transitions that share the same starting state:

    Initial: Locked
    FSM: Turnstile
    {
      Locked    {
        Coin    Unlocked    unlock
        Pass    Locked      alarm
      }
      Unlocked  {
        Coin    Unlocked    thankyou
        Pass    Locked      lock
      }
    }

Now let's add an `Alarming` state that must be reset by a repairman:  

    Initial: Locked
    FSM: Turnstile
    {
      Locked    {
        Coin    Unlocked    unlock
        Pass    Alarming    alarmOn
        Reset   -           {alarmOff lock}
      }
      Unlocked  {
        Reset   Locked      {alarmOff lock}
        Coin    Unlocked    thankyou
        Pass    Locked      lock
      }
      Alarming {
        Coin    -          -
        Pass    -          -  
        Reset   Locked     {alarmOff lock}
      }
    }
    
We use the _dash_ (`-`) character for two purposes.  When used as an action it means that there are no actions to perform.  When used as the _next-state_ it means that the state does not change.  Note: the _star_ (`*`) character can be used as a synonym for _dash_.  

When more than one action should be performed, they can be grouped together in braces (`{}`).

###Super States
Notice the duplication of the `Reset` transition.  In all three states the `Reset` event does the same thing.  It transitions to the `Locked` state and it invokes the `lock` and `alarmOff` actions.  This duplication can be eliminated by using a _Super State_ as follows:

    Initial: Locked
    FSM: Turnstile
    {
      // This is an abstract super state.
      (Resetable)  {
        Reset       Locked       {alarmOff lock}
      }
      Locked : Resetable    { 
        Coin    Unlocked    unlock
        Pass    Alarming    alarmOn
      }
      Unlocked : Resetable {
        Coin    Unlocked    thankyou
        Pass    Locked      lock
      }
      Alarming : Resetable { // inherits all it's transitions from Resetable.
      }
    }

The parenthesis around `Resetable` indicate that it is an _abstract state_.  The State machine will never actually be in this state.  Rather, it exists to be used as a _super-state_ by `Locked`, `Unlocked`, and `Alarming`.  Note the use of the colon (`:`) character to denote that those three states derive from `Resetable`.  

A state with a super-state _inherits_ all the transitions of that super-state.  Super-state transitions can be overridden if necessary.  What's more, a state can derive from more than one super-state by using additional colon operators as follows:

    state : superstate1 : superstate2 {...

Super-states do not have to be abstract.  A state can derive from any other state, whether abstract or not.  However, if we mark a state as abstract, then SMC will ensure that it is never used as the target of a transition.  The state machine will never be in that state.  

### Comments
A comment is any string beginning with two slashes, and ending with a line-end.  They can be placed at the start of a line as in the example above; or they can be placed at the end of a line.

### Entry and Exit actions
In the previous example, the fact that the alarm is turned on every time the `Alarming` state is entered and is turned off every time the `Alarming` state is exited, is hidden within the logic of several different transitions.  We can make it explicit by using _entry actions_ and _exit actions_.  


    Initial: Locked
    FSM: Turnstile
    {
      (Resetable) {
        Reset       Locked       -
      }
      Locked : Resetable <lock     {
        Coin    Unlocked    -
        Pass    Alarming    -
      }
      Unlocked : Resetable <unlock  {
        Coin    Unlocked    thankyou
        Pass    Locked      -
      }
      Alarming : Resetable <alarmOn >alarmOff   -    -    -
    }

The _less-than_ (`<`) character denote an _entry-action_.  It is invoked whenever the state is entered.  Likewise the _greater-than_ (`>`) character denotes an _exit-action_ which is invoked whenever the state is exited.  

In the above example, notice that nearly all the actions have been restated as _entry-_ and _exit-actions_.  You may find that this makes the state machine more readable.  

The _Entry-_ and _Exit-actions_ of superstates are inherited by their derivative states.

### Semantic Differences with _Entry-_ and _Exit-actions_.
Note also that there is a slight semantic difference between the last two examples.  If we are in the `Locked` state, and we get a `Reset` event, then the `lock` action will be invoked even though we are already in the locked state.  This is because _every_ transition invokes all the _exit-_ and _entry-actions_, regardless of whether the state is actually changing.  Thus, when we are in the `Unlocked` state, and we get a `Coin` event, even though we stay in the `Unlocked` state, the `unlock` action will be invoked.

### Internal Structure.
The internal structure of SMC is a simple traditional compiler.  Here is a picture:

    +-------+    +--------+    +----------+    +-----------+    +-----------+    +--------------+
    | Lexer |--->| Parser |--->| Semantic |--->| Optimizer |--->| Generator |--->| Implementing |
    +-------+    +--------+    | Analyzer |    +-----------+    +-----------+    |   Visitor    |
                               +----------+                                      +--------------+

This closely reflects the package structure of the java code.

* The _Lexer_ translates the source code into a stream of lexical tokens which act as events going into the Parser.
* The _Parser_ is a simple finite state machine that implements the Backus-Naur description of the source code (See below).  That state machine is implemented as a simple state transition table held within a Java array of `Transition` objects.  The actions of that parser state machine use the _Builder_ pattern to create a _Syntax Data Structure_.
* The _Semantic Analyzer_ ensures that the _Syntax Data Structure_ describes a true finite state machine, and if so, translates it into a _Semantic Data Structure_ that can only hold true finite state machines.
* The Optimizer then translates the _Semantic Data Structure_ into a simple state transition table.  It reduces all the super state inheritance, and the _entry-_ and _exit-actions_ back into vanilla states, events, and actions.
* The _Generator_ converts the optimized state transition table into a set of code-generation-nodes that represent a _Nested Switch Case_ statement in a language agnostic way.
* Finally, the _Implementing Visitors_ translate the code-generation-nodes into a true programming language, like Java.

The upshot of all this is that you can generate a new language, like C#, by simply writing a new _Implementing Visitor_, which is a relatively trivial task. (See Below)

### Writing a Code Generator

The value of the `-l` command line argument is used to find a class whose name is:
 
`smc.generators.<LANGUAGE>CodeGenerator`.  
	
This class decides which _Implementing Visitor_ to create, and how the output files should be written.
	
You can create a generator for a new language by deriving that class from `smc.generators.CodeGenerator` and putting it in the classpath.  Check out the source code for the Java code generator.  It's pretty straightforward.



### BNF

The Backus-Naur form (BNF) of the SMC source code is: 

    <FSM> ::= <header>* <logic>
    <header> ::= <name> ":" <name>
        
    <logic> ::= "{" <transition>* "}"
    <transition> ::= <state-spec> <subtransition>
                 |   <state-spec> "{" <subtransition>* "}"
    <state-spec> :== <state> <state-modifier>*	
    <state> ::= <name> | "(" <name> ")"	
    <state-modifier> :== ":" <name>
                     |   "<" <name>
                     |   ">" <name>
    <subtransition> :: <event> <next-state> <action>
    <action> ::= <name> | "{" <name>* "}" | "-"
    <next-state> ::= <state> | "-"
    <event> :: <name> | "-"

### License
You may use this program and source code for any purpose at all at your own risk.  It is not copyrighted.  Have fun!






