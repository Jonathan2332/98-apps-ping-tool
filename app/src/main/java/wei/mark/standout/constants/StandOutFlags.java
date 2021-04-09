package wei.mark.standout.constants;

import wei.mark.standout.StandOutWindow;
import wei.mark.standout.ui.Window;
import android.view.Gravity;
import android.view.MotionEvent;

/**
 * Flags to be returned from {@link StandOutWindow#getFlags()}.
 * 
 * @author Mark Wei <markwei@gmail.com>
 * 
 */
public class StandOutFlags {
	// This counter keeps track of which primary bit to set for each flag
	private static int flag_bit = 0;

	/**
	 * Setting this flag indicates that the window wants the system provided
	 * window decorations (titlebar, hide/close buttons, resize handle, etc).
	 */
	public static final int FLAG_DECORATION_SYSTEM = 1 << flag_bit++;

	/**
	 * Setting this flag indicates that the window decorator should NOT provide
	 * a close button.
	 * 
	 * <p>
	 * This flag also sets {@link #FLAG_DECORATION_SYSTEM}.
	 */
	public static final int FLAG_DECORATION_CLOSE_DISABLE = FLAG_DECORATION_SYSTEM
			| 1 << flag_bit++;

	/**
	 * Setting this flag indicates that the window decorator should NOT provide
	 * a resize handle.
	 * 
	 * <p>
	 * This flag also sets {@link #FLAG_DECORATION_SYSTEM}.
	 */
	public static final int FLAG_DECORATION_RESIZE_DISABLE = FLAG_DECORATION_SYSTEM
			| 1 << flag_bit++;

	/**
	 * Setting this flag indicates that the window decorator should NOT provide
	 * a resize handle.
	 * 
	 * <p>
	 * This flag also sets {@link #FLAG_DECORATION_SYSTEM}.
	 */
	public static final int FLAG_DECORATION_MAXIMIZE_DISABLE = FLAG_DECORATION_SYSTEM
			| 1 << flag_bit++;

	/**
	 * Setting this flag indicates that the window decorator should NOT provide
	 * a resize handle.
	 * 
	 * <p>
	 * This flag also sets {@link #FLAG_DECORATION_SYSTEM}.
	 */
	public static final int FLAG_DECORATION_MOVE_DISABLE = FLAG_DECORATION_SYSTEM
			| 1 << flag_bit++;

	/**
	 * Setting this flag indicates that the system should keep the window's
	 * position within the edges of the screen. If this flag is not set, the
	 * window will be able to be dragged off of the screen.
	 * 
	 * <p>
	 * If this flag is set, the window's {@link Gravity} is recommended to be
	 * {@link Gravity#TOP} | {@link Gravity#LEFT}. If the gravity is anything
	 * other than TOP|LEFT, then even though the window will be displayed within
	 * the edges, it will behave as if the user can drag it off the screen.
	 * 
	 */
	public static final int FLAG_WINDOW_EDGE_LIMITS_ENABLE = 1 << flag_bit++;


	public static final int FLAG_WINDOW_ASPECT_RATIO_ENABLE = 1 << flag_bit++;

	/**
	 * Setting this flag indicates that the system should resize the window when
	 * it detects a pinch-to-zoom gesture.
	 * 
	 * @see Window#onInterceptTouchEvent(MotionEvent)
	 */
	public static final int FLAG_WINDOW_PINCH_RESIZE_ENABLE = 1 << flag_bit++;

	public static final int FLAG_WINDOW_FOCUSABLE_DISABLE = 1 << flag_bit++;

	public static final int FLAG_FIX_COMPATIBILITY_ALL_DISABLE = 1 << flag_bit++;

	public static final int FLAG_ADD_FUNCTIONALITY_ALL_DISABLE = 1 << flag_bit++;

	public static final int FLAG_ADD_FUNCTIONALITY_RESIZE_DISABLE = 1 << flag_bit++;

}