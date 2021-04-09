package wei.mark.standout;

import java.util.LinkedList;
import java.util.Set;

import a98apps.pingnoads.R;
import a98apps.pingnoads.constants.PingConstants;
import wei.mark.standout.constants.StandOutFlags;
import wei.mark.standout.ui.Window;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.graphics.BitmapFactory;
import android.graphics.PixelFormat;
import android.net.Uri;
import android.os.Build;
import android.os.IBinder;
import android.support.v4.app.NotificationCompat;
import android.util.Log;
import android.util.TypedValue;
import android.view.Display;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.animation.Animation;
import android.view.animation.Animation.AnimationListener;
import android.view.animation.AnimationUtils;
import android.widget.FrameLayout;
import android.widget.TextView;

/**
 * Extend this class to easily create and manage floating StandOut windows.
 * 
 * @author Mark Wei <markwei@gmail.com>
 * 
 *         Contributors: Jason <github.com/jasonconnery>
 * 
 */
public abstract class StandOutWindow extends Service {
	private static final String TAG = "StandOutWindow";

	/**
	 * StandOut window id: You may use this sample id for your first window.
	 */
	public static final int DEFAULT_ID = 0;

	/**
	 * Special StandOut window id: You may NOT use this id for any windows.
	 */
	private static final int ONGOING_NOTIFICATION_ID = -1;

	/**
	 * Intent action: Show a new window corresponding to the id.
	 */
	private static final String ACTION_SHOW = "SHOW";

	private static final String ACTION_RESTORE = "RESTORE";

	/**
	 * Intent action: Close an existing window with an existing id.
	 */
	private static final String ACTION_CLOSE = "CLOSE";

	/**
	 * Intent action: Close all existing windows.
	 */
	private static final String ACTION_CLOSE_ALL = "CLOSE_ALL";


	/**
	 * Show a new window corresponding to the id, or restore a previously hidden
	 * window.
	 * 
	 * @param context
	 *            A Context of the application package implementing this class.
	 * @param cls
	 *            The Service extending {@link StandOutWindow} that will be used
	 *            to create and manage the window.
	 * @param id
	 *            The id representing this window. If the id exists, and the
	 *            corresponding window was previously hidden, then that window
	 *            will be restored.
	 * 
	 * @see #show(int)
	 */
	public static void show(Context context,
			Class<? extends StandOutWindow> cls, int id, String url) {
		context.startService(getShowIntent(context, cls, id, url));
	}

	/**
	 * Close an existing window with an existing id.
	 * 
	 * @param context
	 *            A Context of the application package implementing this class.
	 * @param cls
	 *            The Service extending {@link StandOutWindow} that is managing
	 *            the window.
	 * @param id
	 *            The id representing this window. The window must previously be
	 *            shown.
	 * @see #close(int)
	 */
	public static void close(Context context,
			Class<? extends StandOutWindow> cls, int id) {
		context.startService(getCloseIntent(context, cls, id));
	}

	/**
	 * Close all existing windows.
	 * 
	 * @param context
	 *            A Context of the application package implementing this class.
	 * @param cls
	 *            The Service extending {@link StandOutWindow} that is managing
	 *            the window.
	 * @see #closeAll()
	 */
	public static void closeAll(Context context,
			Class<? extends StandOutWindow> cls) {
		context.startService(getCloseAllIntent(context, cls));
	}

	private static Intent getShowIntent(Context context,
										Class<? extends StandOutWindow> cls, int id, String url) {
		boolean cached = sWindowCache.isCached(id, cls);
		String action = cached ? ACTION_RESTORE : ACTION_SHOW;
		Uri uri = cached ? Uri.parse("standout://" + cls + '/' + id) : null;
		return new Intent(context, cls).putExtra("id", id).putExtra("url", url).setAction(action)
				.setData(uri);
	}

	/**
	 * See {@link #close(Context, Class, int)}.
	 * 
	 * @param context
	 *            A Context of the application package implementing this class.
	 * @param cls
	 *            The Service extending {@link StandOutWindow} that is managing
	 *            the window.
	 * @param id
	 *            The id representing this window. If the id exists, and the
	 *            corresponding window was previously hidden, then that window
	 *            will be restored.
	 * @return An {@link Intent} to use with
	 *         {@link Context#startService(Intent)}.
	 */
	public static Intent getCloseIntent(Context context,
			Class<? extends StandOutWindow> cls, int id) {
		return new Intent(context, cls).putExtra("id", id).setAction(
				ACTION_CLOSE);
	}


	private static Intent getCloseAllIntent(Context context,
											Class<? extends StandOutWindow> cls) {
		return new Intent(context, cls).setAction(ACTION_CLOSE_ALL);
	}


	// internal map of ids to shown/hidden views
	private static final WindowCache sWindowCache;

	// static constructors
	static {
		sWindowCache = new WindowCache();
	}

	// internal system services
	private WindowManager mWindowManager;
	private String url;

	// internal state variables
	private boolean startedForeground;

	@Override
	public IBinder onBind(Intent intent) {
		return null;
	}

	@Override
	public void onCreate() {
		super.onCreate();

		mWindowManager = (WindowManager) getSystemService(Context.WINDOW_SERVICE);

		startedForeground = false;
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		super.onStartCommand(intent, flags, startId);

		// intent should be created with
		// getShowIntent(), getHideIntent(), getCloseIntent()
		if (intent != null) {
			String action = intent.getAction();
			int id = intent.getIntExtra("id", DEFAULT_ID);
            setUrl(intent.getStringExtra("url"));
			// this will interfere with getPersistentNotification()
			if (id == ONGOING_NOTIFICATION_ID) {
				throw new RuntimeException(
						"ID cannot equals StandOutWindow.ONGOING_NOTIFICATION_ID");
			}

			if (ACTION_SHOW.equals(action) || ACTION_RESTORE.equals(action)) {
				show(id);
			} else if (ACTION_CLOSE.equals(action)) {
				close(id);
			} else if (ACTION_CLOSE_ALL.equals(action)) {
				closeAll();
			}
		} else {
			Log.w(TAG, "Tried to onStartCommand() with a null intent.");
		}

		// the service is started in foreground in show()
		// so we don't expect Android to kill this service
		return START_NOT_STICKY;
	}

	@Override
	public void onDestroy() {
		super.onDestroy();

		// closes all windows
		closeAll();
	}

	/**
	 * Return the name of every window in this implementation. The name will
	 * appear in the default implementations of the system window decoration
	 * title and notification titles.
	 * 
	 * @return The name.
	 */
	protected abstract String getAppName();

	/**
	 * Return the icon resource for every window in this implementation. The
	 * icon will appear in the default implementations of the system window
	 * decoration and notifications.
	 * 
	 * @return The icon.
	 */
	public abstract int getAppIcon();

	/**
	 * Create a new {@link View} corresponding to the id, and add it as a child
	 * to the frame. The view will become the contents of this StandOut window.
	 * The view MUST be newly created, and you MUST attach it to the frame.
	 * 
	 * <p>
	 * If you are inflating your view from XML, make sure you use
	 * {@link LayoutInflater#inflate(int, ViewGroup, boolean)} to attach your
	 * view to frame. Set the ViewGroup to be frame, and the boolean to true.
	 * 
	 * <p>
	 * If you are creating your view programmatically, make sure you use
	 * {@link FrameLayout#addView(View)} to add your view to the frame.
	 *
	 * @param frame
	 *            The {@link FrameLayout} to attach your view as a child to.
	 */
	public abstract void createAndAttachView(FrameLayout frame);

	protected abstract void onFinishWindow();


	public abstract StandOutLayoutParams getParams(int id);

	/**
	 * Implement this method to change modify the behavior and appearance of the
	 * window corresponding to the id.
	 * 
	 * <p>
	 * You may use any of the flags defined in {@link StandOutFlags}. This
	 * method will be called many times, so keep it fast.
	 * 
	 * <p>
	 * Use bitwise OR (|) to set flags, and bitwise XOR (^) to unset flags. To
	 * test if a flag is set, use {@link Utils#isSet(int, int)}.
	 * 
	 * @return A combination of flags.
	 */
	public int getFlags() {
		return 0;
	}

	/**
	 * Implement this method to set a custom title for the window corresponding
	 * to the id.
	 * 
	 * @return The title of the window.
	 */
	public String getTitle() {
		return getAppName();
	}

	/**
	 * Return the title for the persistent notification. This is called every
	 * time {@link #show(int)} is called.
	 * 
	 * @return The title for the persistent notification.
	 */
	private String getPersistentNotificationTitle() {
		return getAppName() + " Running";
	}

	/**
	 * Return the message for the persistent notification. This is called every
	 * time {@link #show(int)} is called.
	 * 
	 * @return The message for the persistent notification.
	 */
	protected String getPersistentNotificationMessage() {
		return "";
	}

	/**
	 * Return the intent for the persistent notification. This is called every
	 * time {@link #show(int)} is called.
	 * 
	 * <p>
	 * The returned intent will be packaged into a {@link PendingIntent} to be
	 * invoked when the user clicks the notification.
	 * 
	 * @param id
	 *            The id of the window shown.
	 * @return The intent for the persistent notification.
	 */
	protected Intent getPersistentNotificationIntent(int id) {
		return null;
	}


	@SuppressWarnings("deprecation")
	private Notification getPersistentNotification(int id) {
		// basic notification stuff
		// http://developer.android.com/guide/topics/ui/notifiers/notifications.html
		int icon = getAppIcon();
		long when = System.currentTimeMillis();
		Context c = getApplicationContext();
		String contentTitle = getPersistentNotificationTitle();
		String contentText = getPersistentNotificationMessage();

		// getPersistentNotification() is called for every new window
		// so we replace the old notification with a new one that has
		// a bigger id
		Intent notificationIntent = getPersistentNotificationIntent(id);

		PendingIntent contentIntent = null;

		if (notificationIntent != null) {
			contentIntent = PendingIntent.getService(this, 0,
					notificationIntent,
					// flag updates existing persistent notification
					PendingIntent.FLAG_UPDATE_CURRENT);
		}

		NotificationCompat.Builder nBuilder;
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
		{
			String NOTIFICATION_CHANNEL_ID = "com.a98apps.pingnoads";
			NotificationChannel chan = new NotificationChannel(NOTIFICATION_CHANNEL_ID, "Window Float Service", NotificationManager.IMPORTANCE_NONE);
			NotificationManager manager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
			assert manager != null;
			manager.createNotificationChannel(chan);

			nBuilder = new NotificationCompat.Builder(c, NOTIFICATION_CHANNEL_ID);
			return nBuilder.setContentIntent(contentIntent)
					.setOngoing(true)
					.setSmallIcon(R.drawable.ic_ping_notification).setWhen(when)
					.setLargeIcon(BitmapFactory.decodeResource(getResources(), R.mipmap.ic_launcher))
					.setColor(getColor(R.color.colorAccent2))
					.setContentTitle(contentTitle)
					.setContentIntent(contentIntent)
					.setContentText(contentText).build();
		}
		else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP && Build.VERSION.SDK_INT <= Build.VERSION_CODES.M)
		{
			nBuilder = new NotificationCompat.Builder(c);
			return nBuilder.setContentIntent(contentIntent)
					.setOngoing(true)
					.setSmallIcon(R.drawable.ic_ping_notification).setWhen(when)
					.setColor(getResources().getColor(R.color.colorAccent2))
					.setLargeIcon(BitmapFactory.decodeResource(getResources(), icon))
					.setContentTitle(contentTitle)
					.setContentIntent(contentIntent)
					.setContentText(contentText).build();
		}
		else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N)
		{
			nBuilder = new NotificationCompat.Builder(c);
			return nBuilder.setContentIntent(contentIntent)
					.setOngoing(true)
					.setSmallIcon(R.drawable.ic_ping_notification).setWhen(when)
					.setLargeIcon(BitmapFactory.decodeResource(getResources(), icon))
					.setColor(getColor(R.color.colorAccent2))
					.setContentTitle(contentTitle)
					.setContentIntent(contentIntent)
					.setContentText(contentText).build();
		}
		else
		{
			nBuilder = new NotificationCompat.Builder(c);
			return nBuilder.setContentIntent(contentIntent)
					.setSmallIcon(icon).setWhen(when)
					.setContentTitle(contentTitle)
					.setContentText(contentText).build();
		}
	}


	/**
	 * Return the animation to play when the window corresponding to the id is
	 * shown.
	 * 
	 * @return The animation to play or null.
	 */
	private Animation getShowAnimation() {
		return AnimationUtils.loadAnimation(this, android.R.anim.fade_in);
	}

	/**
	 * Return the animation to play when the window corresponding to the id is
	 * closed.
	 * 
	 * @return The animation to play or null.
	 */
	private Animation getCloseAnimation() {
		return AnimationUtils.loadAnimation(this, android.R.anim.fade_out);
	}

	/**
	 * Implement this method to set a custom theme for all windows in this
	 * implementation.
	 * 
	 * @return The theme to set on the window, or 0 for device default.
	 */
	public int getThemeStyle() {
		return 0;
	}

	/**
	 * Implement this callback to be alerted when all windows are about to be
	 * closed. This callback will occur before any views are removed from the
	 * window manager.
	 * 
	 * @return Return true to cancel the views from being closed, or false to
	 *         continue.
	 * @see #closeAll()
	 */
	private boolean onCloseAll() {
		return false;
	}

	/**
	 * Show or restore a window corresponding to the id. Return the window that
	 * was shown/restored.
	 * 
	 * @param id
	 *            The id of the window.
	 * @return The window shown.
	 */
	private synchronized Window show(int id) {
		// get the window corresponding to the id
		Window cachedWindow = getWindow(id);
		final Window window;

		// check cache first
		if (cachedWindow != null) {
			window = cachedWindow;
		} else {
			window = new Window(this, id);
		}

		// focus an already shown window
		if (window.visibility == Window.VISIBILITY_VISIBLE) {
			Log.d(TAG, "Window " + id + " is already shown.");
			return window;
		}

		window.visibility = Window.VISIBILITY_VISIBLE;

		// get animation
		Animation animation = getShowAnimation();

		// get the params corresponding to the id
		StandOutLayoutParams params = window.getLayoutParams();

		try {
			// add the view to the window manager
			mWindowManager.addView(window, params);

			// animate
			if (animation != null) {
				window.getChildAt(0).startAnimation(animation);
			}
		} catch (Exception ex) {
			ex.printStackTrace();
		}

		// add view to internal map
		sWindowCache.putCache(id, getClass(), window);

		// get the persistent notification
		Notification notification = getPersistentNotification(id);

		// show the notification
		if (notification != null) {
			notification.flags = notification.flags
					| Notification.FLAG_NO_CLEAR;

			// only show notification if not shown before
			if (!startedForeground) {
				// tell Android system to show notification
				startForeground(
						getClass().hashCode() + ONGOING_NOTIFICATION_ID,
						notification);
				startedForeground = true;
			}
		} else {
			// notification can only be null if it was provided before
			if (!startedForeground) {
				throw new RuntimeException("Your StandOutWindow service must"
						+ "provide a persistent notification."
						+ "The notification prevents Android"
						+ "from killing your service in low"
						+ "memory situations.");
			}
		}

		return window;
	}

	/**
	 * Close a window corresponding to the id.
	 * 
	 * @param id
	 *            The id of the window.
	 */
	public final synchronized void close(final int id) {
		// get the view corresponding to the id
		final Window window = getWindow(id);

		if (window == null) {
			throw new IllegalArgumentException("Tried to close(" + id
					+ ") a null window.");
		}

		if (window.visibility == Window.VISIBILITY_TRANSITION) {
			return;
		}

		window.visibility = Window.VISIBILITY_TRANSITION;

		// get animation
		Animation animation = getCloseAnimation();

		onFinishWindow();

		// remove window
		try {
			// animate
			if (animation != null) {
				animation.setAnimationListener(new AnimationListener() {

					@Override
					public void onAnimationStart(Animation animation) {
					}

					@Override
					public void onAnimationRepeat(Animation animation) {
					}

					@Override
					public void onAnimationEnd(Animation animation) {
						// remove the window from the window manager
						mWindowManager.removeView(window);
						window.visibility = Window.VISIBILITY_GONE;

						// remove view from internal map
						sWindowCache.removeCache(id,
								StandOutWindow.this.getClass());

						// if we just released the last window, quit
						if (getExistingIds().size() == 0) {
							// tell Android to remove the persistent
							// notification
							// the Service will be shutdown by the system on low
							// memory
							startedForeground = false;
							stopForeground(true);
						}
					}
				});
				window.getChildAt(0).startAnimation(animation);
			} else {
				// remove the window from the window manager
				mWindowManager.removeView(window);

				// remove view from internal map
				sWindowCache.removeCache(id, getClass());

				// if we just released the last window, quit
				if (sWindowCache.getCacheSize(getClass()) == 0) {
					// tell Android to remove the persistent notification
					// the Service will be shutdown by the system on low memory
					startedForeground = false;
					stopForeground(true);
				}
			}
		} catch (Exception ex) {
			ex.printStackTrace();
		}
	}

	/**
	 * Close all existing windows.
	 */
	private synchronized void closeAll() {
		// alert callbacks and cancel if instructed
		if (onCloseAll()) {
			Log.w(TAG, "Windows close all cancelled by implementation.");
			return;
		}

		// add ids to temporary set to avoid concurrent modification
		LinkedList<Integer> ids = new LinkedList<>(getExistingIds());

		// close each window
		for (int id : ids) {
			close(id);
		}
	}

	/**
	 * Return the ids of all shown or hidden windows.
	 * 
	 * @return A set of ids, or an empty set.
	 */
	private Set<Integer> getExistingIds() {
		return sWindowCache.getCacheIds(getClass());
	}

	private Window getWindow(int id) {
		return sWindowCache.getCache(id, getClass());
	}


    private void setUrl(String text) {
        this.url = text;
    }
    protected final String getUrl() {
        return url;
    }

	protected final void setFontSize(String size, ViewGroup contentLayout)
	{
		for (int i=0; i < contentLayout.getChildCount(); i++) {
			View view = contentLayout.getChildAt(i);
			if (view instanceof TextView) {
				if(size.equals(PingConstants.TEXT_SMALL)) {
					((TextView) view).setTextSize(TypedValue.COMPLEX_UNIT_PX, getResources().getDimension(R.dimen.text_size_small));
				}
				else if(size.equals(PingConstants.TEXT_MID)) {
					((TextView) view).setTextSize(TypedValue.COMPLEX_UNIT_PX, getResources().getDimension(R.dimen.text_size_mid));
				}
				else {
					((TextView) view).setTextSize(TypedValue.COMPLEX_UNIT_PX, getResources().getDimension(R.dimen.text_size_big));
				}
			}
			else if (view instanceof ViewGroup)
				setFontSize(size, (ViewGroup) view);
		}
	}

	public boolean onTouchHandleMove(Window window, MotionEvent event) {
		StandOutLayoutParams params = window.getLayoutParams();

		// how much you have to move in either direction in order for the
		// gesture to be a move and not tap

		int totalDeltaX = window.touchInfo.lastX - window.touchInfo.firstX;
		int totalDeltaY = window.touchInfo.lastY - window.touchInfo.firstY;

		switch (event.getAction()) {
			case MotionEvent.ACTION_DOWN:
				window.touchInfo.lastX = (int) event.getRawX();
				window.touchInfo.lastY = (int) event.getRawY();

				window.touchInfo.firstX = window.touchInfo.lastX;
				window.touchInfo.firstY = window.touchInfo.lastY;
				break;
			case MotionEvent.ACTION_MOVE:
				int deltaX = (int) event.getRawX() - window.touchInfo.lastX;
				int deltaY = (int) event.getRawY() - window.touchInfo.lastY;

				window.touchInfo.lastX = (int) event.getRawX();
				window.touchInfo.lastY = (int) event.getRawY();

				if (window.touchInfo.moving
						|| Math.abs(totalDeltaX) >= params.threshold
						|| Math.abs(totalDeltaY) >= params.threshold) {
					window.touchInfo.moving = true;

					// update the position of the window
					if (event.getPointerCount() == 1) {
						params.x += deltaX;
						params.y += deltaY;
					}

					window.edit().setPosition(params.x, params.y).commit();
				}
				break;
			case MotionEvent.ACTION_UP:
				window.touchInfo.moving = false;
				break;
		}
		return true;
	}


	public boolean onTouchHandleResize(Window window,
									   MotionEvent event) {
		StandOutLayoutParams params = window
				.getLayoutParams();

		switch (event.getAction()) {
			case MotionEvent.ACTION_DOWN:
				window.touchInfo.lastX = (int) event.getRawX();
				window.touchInfo.lastY = (int) event.getRawY();

				window.touchInfo.firstX = window.touchInfo.lastX;
				window.touchInfo.firstY = window.touchInfo.lastY;
				break;
			case MotionEvent.ACTION_MOVE:
				int deltaX = (int) event.getRawX() - window.touchInfo.lastX;
				int deltaY = (int) event.getRawY() - window.touchInfo.lastY;

				// update the size of the window
				params.width += deltaX;
				params.height += deltaY;

				// keep window between min/max width/height
				if (params.width >= params.minWidth
						&& params.width <= params.maxWidth) {
					window.touchInfo.lastX = (int) event.getRawX();
				}

				if (params.height >= params.minHeight
						&& params.height <= params.maxHeight) {
					window.touchInfo.lastY = (int) event.getRawY();
				}

				window.edit().setSize(params.width, params.height).commit();
				break;
			case MotionEvent.ACTION_UP:
				break;
		}

		return true;
	}

	/**
	 * Update the window corresponding to this id with the given params.
	 * 
	 * @param id
	 *            The id of the window.
	 * @param params
	 *            The updated layout params to apply.
	 */
	public void updateViewLayout(int id, StandOutLayoutParams params) {
		Window window = getWindow(id);

		if (window == null) {
			throw new IllegalArgumentException("Tried to updateViewLayout("
					+ id + ") a null window.");
		}

		if (window.visibility == Window.VISIBILITY_GONE) {
			return;
		}

		if (window.visibility == Window.VISIBILITY_TRANSITION) {
			return;
		}

		try {
			window.setLayoutParams(params);
			mWindowManager.updateViewLayout(window, params);
		} catch (Exception ex) {
			ex.printStackTrace();
		}
	}

	/**
	 * LayoutParams specific to floating StandOut windows.
	 * 
	 * @author Mark Wei <markwei@gmail.com>
	 * 
	 */
	public class StandOutLayoutParams extends WindowManager.LayoutParams {

		public static final int TOP = 0;
		/**
		 * Special value for x position that represents the right of the screen.
		 */
		public static final int RIGHT = Integer.MAX_VALUE;
		/**
		 * Special value for y position that represents the bottom of the
		 * screen.
		 */
		private final int BOTTOM = Integer.MAX_VALUE;
		/**
		 * Special value for x or y position that represents the center of the
		 * screen.
		 */
		private final int CENTER = Integer.MIN_VALUE;
		/**
		 * Special value for x or y position which requests that the system
		 * determine the position.
		 */
		private final int AUTO_POSITION = Integer.MIN_VALUE + 1;

		/**
		 * The distance that distinguishes a tap from a drag.
		 */
		private final int threshold;

		/**
		 * Optional constraints of the window.
		 */
		public final int minWidth;
		public final int minHeight;
		public final int maxWidth;
		public final int maxHeight;

		/**
		 * @param id
		 *            The id of the window.
		 */
		StandOutLayoutParams(int id) {
			super(200, 200, Build.VERSION.SDK_INT >= Build.VERSION_CODES.O ? TYPE_APPLICATION_OVERLAY : TYPE_PHONE,
					StandOutLayoutParams.FLAG_NOT_TOUCH_MODAL
							| StandOutLayoutParams.FLAG_WATCH_OUTSIDE_TOUCH,
					PixelFormat.TRANSLUCENT);

			int windowFlags = getFlags();

			setFocusFlag();

			if (!Utils.isSet(windowFlags,
					StandOutFlags.FLAG_WINDOW_EDGE_LIMITS_ENABLE)) {
				// windows may be moved beyond edges
				flags |= FLAG_LAYOUT_NO_LIMITS;
			}

			x = getX(id, width);
			y = getY(id, height);

			gravity = Gravity.TOP | Gravity.START;

			threshold = 10;
			minWidth = minHeight = 0;
			maxWidth = maxHeight = Integer.MAX_VALUE;
		}

		/**
		 * @param id
		 *            The id of the window.
		 * @param w
		 *            The width of the window.
		 * @param h
		 *            The height of the window.
		 */
		StandOutLayoutParams(int id, int w, int h) {
			this(id);
			width = w;
			height = h;
		}
		public StandOutLayoutParams(int id, int w, int h, int xpos, int ypos) {
			this(id, w, h);

			if (xpos != AUTO_POSITION) {
				x = xpos;
			}
			if (ypos != AUTO_POSITION) {
				y = ypos;
			}

			Display display = mWindowManager.getDefaultDisplay();
			int width = display.getWidth();
			int height = display.getHeight();

			if (x == RIGHT) {
				x = width - w;
			} else if (x == CENTER) {
				x = (width - w) / 2;
			}

			if (y == BOTTOM) {
				y = height - h;
			} else if (y == CENTER) {
				y = (height - h) / 2;
			}
		}


		// helper to create cascading windows
		private int getX(int id, int width) {
			Display display = mWindowManager.getDefaultDisplay();
			int displayWidth = display.getWidth();

			int types = sWindowCache.size();

			int initialX = 100 * types;
			int variableX = 100 * id;
			int rawX = initialX + variableX;

			return rawX % (displayWidth - width);
		}

		// helper to create cascading windows
		private int getY(int id, int height) {
			Display display = mWindowManager.getDefaultDisplay();
			int displayWidth = display.getWidth();
			int displayHeight = display.getHeight();

			int types = sWindowCache.size();

			int initialY = 100 * types;
			int variableY = x + 200 * (100 * id) / (displayWidth - width);

			int rawY = initialY + variableY;

			return rawY % (displayHeight - height);
		}

		void setFocusFlag() {
			flags = flags | StandOutLayoutParams.FLAG_NOT_FOCUSABLE;
		}
	}
}
