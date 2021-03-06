package wei.mark.standout.ui;

import java.util.Locale;

/**
 * This class holds temporal touch and gesture information. Mainly used to hold
 * temporary data for onTouchEvent(MotionEvent).
 * 
 * @author Mark Wei <markwei@gmail.com>
 * 
 */
public class TouchInfo {
	/**
	 * The state of the window.
	 */
	public int firstX, firstY, lastX, lastY;
	double dist, scale, firstWidth, firstHeight;
	float ratio;

	/**
	 * Whether we're past the move threshold already.
	 */
	public boolean moving;

	@Override
	public String toString() {
		return String
				.format(Locale.US,
						"WindowTouchInfo { firstX=%d, firstY=%d,lastX=%d, lastY=%d, firstWidth=%f, firstHeight=%f }",
						firstX, firstY, lastX, lastY, firstWidth, firstHeight);
	}
}
