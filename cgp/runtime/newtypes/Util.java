/** 
 * Miscellaneous utility operations.
 *
 * David Benn, October 2000
 */

package cgp.runtime.newtypes;

import cgp.runtime.NumberType;
import cgp.runtime.Type;

import java.util.Random;

public class Util extends Type {
    // Instance fields.
    private Random rng = new Random();

    // Constructors.
    public Util() {
	setType("utility");
    }
    
    /**
     * Sleep for the specified number of seconds
     * or fraction thereof.
     */
    public void sleep(NumberType secs) {
	try {
	    Thread.sleep((int)(secs.getValue()*1000));
	} catch(InterruptedException e) {}
    }

    /**
     * Return a random integer in the range -N-1 <= 0 <= N-1.
     * This uses a RNG which is seeded by default from the time.
     */
    public NumberType random(NumberType n) {
	return new NumberType(rng.nextInt() % (int)n.getValue());
    }
}
