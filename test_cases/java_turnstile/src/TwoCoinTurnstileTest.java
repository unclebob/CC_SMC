import org.junit.Before;
import org.junit.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

public class TwoCoinTurnstileTest {
  private String output = "";
  private TwoCoinTurnstile sm;

  class TwoCoinTurnstileImp extends TwoCoinTurnstile {
    public void unhandledTransition(String state, String event) {
      output += String.format("X(%s,%s) ", state, event);
    }

    public void thankyou() {
      output += "T";
    }

    public void unlock() {
      output += "U";
    }

    public void alarmOn() {
      output += "A";
    }

    public void lock() {
      output += "L";
    }

    public void alarmOff() {
      output += "O";
    }
  }

  @Before
  public void setup() {
    output = "";
    sm = new TwoCoinTurnstileImp();
  }

  @Test
  public void normal() throws Exception {
    sm.Coin();
    sm.Coin();
    sm.Pass();
    assertThat(output, is("UL"));
  }

  @Test
  public void oneCoinAttempt() throws Exception {
    sm.Coin();
    sm.Pass();
    assertThat(output, is("A"));
  }

  @Test
  public void alarmReset() throws Exception {
    sm.Pass();
    sm.Reset();
    assertThat(output, is("AOL"));
  }

  @Test
  public void extraCoins() throws Exception {
    sm.Coin();
    sm.Coin();
    sm.Coin();
    sm.Coin();
    sm.Pass();
    assertThat(output, is("UTTL"));
  }

  @Test
  public void pass() throws Exception {
    
  }


}