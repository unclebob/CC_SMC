import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;

public class TwoCoinTurnstileTest {
  private String output = "";
  private TwoCoinTurnstile sm;

  @Nested
  class TwoCoinTurnstileImp  extends TwoCoinTurnstile {
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

  @BeforeEach
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