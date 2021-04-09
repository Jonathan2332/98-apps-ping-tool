package wei.mark.standout.ui;

import java.util.LinkedList;
import java.util.Queue;

import a98apps.pingnoads.R;
import a98apps.pingnoads.view.MainActivity;
import wei.mark.standout.StandOutWindow;
import wei.mark.standout.StandOutWindow.StandOutLayoutParams;
import wei.mark.standout.Utils;
import wei.mark.standout.constants.StandOutFlags;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

/**
 * Special view that represents a floating window.
 * 
 * @author Mark Wei <markwei@gmail.com>
 * 
 */
public class Window extends FrameLayout {
	public static final int VISIBILITY_GONE = 0;
	public static final int VISIBILITY_VISIBLE = 1;
	public static final int VISIBILITY_TRANSITION = 2;

	/**
	 * Id of the window.
	 */
	private int id;

	/**
	 * Whether the window is shown, hidden/closed, or in transition.
	 */
	public int visibility;

	/**
	 * Original params from {@link StandOutWindow#getParams(int)}.
	 */
	private StandOutLayoutParams originalParams;
	/**
	 * Original flags from {@link StandOutWindow#getFlags()}.
	 */
	private int flags;

	/**
	 * Touch information of the window.
	 */
	public TouchInfo touchInfo;

	/**
	 * Data attached to the window.
	 */
	private Bundle data;

	/**
	 * Width and height of the screen.
	 */
	private int displayWidth;
	private int displayHeight;

	/**
	 * Context of the window.
	 */
	private final StandOutWindow mContext;
	private LayoutInflater mLayoutInflater;

	public Window(Context context) {
		super(context);
		mContext = null;
	}

	@SuppressLint("ClickableViewAccessibility")
	public Window(final StandOutWindow context, final int id) {
		super(context);
		context.setTheme(context.getThemeStyle());

		mContext = context;
		mLayoutInflater = LayoutInflater.from(context);

		this.id = id;
		this.originalParams = context.getParams(id);
		this.flags = context.getFlags();
		this.touchInfo = new TouchInfo();
		touchInfo.ratio = (float) originalParams.width / originalParams.height;
		this.data = new Bundle();
		DisplayMetrics metrics = mContext.getResources()
				.getDisplayMetrics();
		displayWidth = metrics.widthPixels;
		displayHeight = (int) (metrics.heightPixels - 25 * metrics.density);

		// create the window contents
		View content;
		FrameLayout body;

		if (Utils.isSet(flags, StandOutFlags.FLAG_DECORATION_SYSTEM)) {
			// requested system window decorations
			content = getSystemDecorations();
			body = content.findViewById(R.id.body);
		} else {
			// did not request decorations. will provide own implementation
			content = new FrameLayout(context);
			content.setId(R.id.content);
			body = (FrameLayout) content;
		}

		addView(content);

		body.setOnTouchListener(new OnTouchListener() {

			@Override
			public boolean onTouch(View v, MotionEvent event) {
				// pass all touch events to the implementation
				boolean consumed;

				// handle move and bring to front
				consumed = context.onTouchHandleMove(Window.this, event);

				return consumed;
			}

		});

		// attach the view corresponding to the id from the
		// implementation
		context.createAndAttachView(body);

		// make sure the implementation attached the view
		if (body.getChildCount() == 0) {
			throw new RuntimeException(
					"You must attach your view to the given frame in createAndAttachView()");
		}

		// implement StandOut specific workarounds
		if (!Utils.isSet(flags,
				StandOutFlags.FLAG_FIX_COMPATIBILITY_ALL_DISABLE)) {
			fixCompatibility(body);
		}
		// implement StandOut specific additional functionality
		if (!Utils.isSet(flags,
				StandOutFlags.FLAG_ADD_FUNCTIONALITY_ALL_DISABLE)) {
			addFunctionality(body);
		}

		// attach the existing tag from the frame to the window
		setTag(body.getTag());
	}

	@Override
	public boolean onInterceptTouchEvent(MotionEvent event) {
		StandOutLayoutParams params = getLayoutParams();

		// multitouch
		if (event.getPointerCount() >= 2
				&& Utils.isSet(flags,
						StandOutFlags.FLAG_WINDOW_PINCH_RESIZE_ENABLE)
				&& (event.getAction() & MotionEvent.ACTION_MASK) == MotionEvent.ACTION_POINTER_DOWN) {
			touchInfo.scale = 1;
			touchInfo.dist = -1;
			touchInfo.firstWidth = params.width;
			touchInfo.firstHeight = params.height;
			return true;
		}

		return false;
	}

	@SuppressLint("ClickableViewAccessibility")
	@Override
	public boolean onTouchEvent(MotionEvent event) {
		// handle touching outside

		// handle multitouch
		if (event.getPointerCount() >= 2
				&& Utils.isSet(flags,
						StandOutFlags.FLAG_WINDOW_PINCH_RESIZE_ENABLE)) {
			// 2 fingers or more

			float x0 = event.getX(0);
			float y0 = event.getY(0);
			float x1 = event.getX(1);
			float y1 = event.getY(1);

			double dist = Math
					.sqrt(Math.pow(x0 - x1, 2) + Math.pow(y0 - y1, 2));

			if ((event.getAction() & MotionEvent.ACTION_MASK) == MotionEvent.ACTION_MOVE) {
				if (touchInfo.dist == -1) {
					touchInfo.dist = dist;
				}
				touchInfo.scale *= dist / touchInfo.dist;
				touchInfo.dist = dist;

				// scale the window with anchor point set to middle
				edit().setAnchorPoint()
						.setSize(
								(int) (touchInfo.firstWidth * touchInfo.scale),
								(int) (touchInfo.firstHeight * touchInfo.scale))
						.commit();
			}
		}

		return true;
	}

	@Override
	public void setLayoutParams(ViewGroup.LayoutParams params) {
		if (params instanceof StandOutLayoutParams) {
			super.setLayoutParams(params);
		} else {
			throw new IllegalArgumentException(
					"Window"
							+ id
							+ ": LayoutParams must be an instance of StandOutLayoutParams.");
		}
	}

	/**
	 * Convenience method to start editting the size and position of this
	 * window. Make sure you call {@link Editor#commit()} when you are done to
	 * update the window.
	 * 
	 * @return The Editor associated with this window.
	 */
	public Editor edit() {
		return new Editor();
	}

	@Override
	public StandOutLayoutParams getLayoutParams() {
		StandOutLayoutParams params = (StandOutLayoutParams) super
				.getLayoutParams();
		if (params == null) {
			params = originalParams;
		}
		return params;
	}


	private View getSystemDecorations() {
		final View decorations = mLayoutInflater.inflate(
				R.layout.system_window_decorators, null);

		// icon
		final ImageView icon = decorations
				.findViewById(R.id.window_icon);
		icon.setImageResource(mContext.getAppIcon());

		// title
		TextView title = decorations.findViewById(R.id.title);
		title.setText(mContext.getTitle());

		// open
		View open = decorations.findViewById(R.id.open);
		open.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				mContext.startActivity(new Intent(mContext.getApplicationContext(), MainActivity.class)
						.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK)
						.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
						.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
			}
		});

		// maximize
		View maximize = decorations.findViewById(R.id.maximize);
		maximize.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				StandOutLayoutParams params = getLayoutParams();
				boolean isMaximized = data
						.getBoolean(WindowDataKeys.IS_MAXIMIZED);
				if (isMaximized && params.width == displayWidth
						&& params.height == displayHeight && params.x == 0
						&& params.y == 0) {
					data.putBoolean(WindowDataKeys.IS_MAXIMIZED, false);
					int oldWidth = data.getInt(
							WindowDataKeys.WIDTH_BEFORE_MAXIMIZE, -1);
					int oldHeight = data.getInt(
							WindowDataKeys.HEIGHT_BEFORE_MAXIMIZE, -1);
					int oldX = data
							.getInt(WindowDataKeys.X_BEFORE_MAXIMIZE, -1);
					int oldY = data
							.getInt(WindowDataKeys.Y_BEFORE_MAXIMIZE, -1);
					edit().setSize(oldWidth, oldHeight).setPosition(oldX, oldY)
							.commit();
				} else {
					data.putBoolean(WindowDataKeys.IS_MAXIMIZED, true);
					data.putInt(WindowDataKeys.WIDTH_BEFORE_MAXIMIZE,
							params.width);
					data.putInt(WindowDataKeys.HEIGHT_BEFORE_MAXIMIZE,
							params.height);
					data.putInt(WindowDataKeys.X_BEFORE_MAXIMIZE, params.x);
					data.putInt(WindowDataKeys.Y_BEFORE_MAXIMIZE, params.y);
					edit().setSize(1f, 1f).setPosition(0, 0).commit();
				}
			}
		});

		// close
		View close = decorations.findViewById(R.id.close);
		close.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				mContext.close(id);
			}
		});

		// move
		View titlebar = decorations.findViewById(R.id.titlebar);
		titlebar.setOnTouchListener(new OnTouchListener() {

			@SuppressLint("ClickableViewAccessibility")
			@Override
			public boolean onTouch(View v, MotionEvent event) {
				// handle dragging to move
				return mContext.onTouchHandleMove(Window.this, event);
			}
		});

		// resize
		View corner = decorations.findViewById(R.id.corner);
		corner.setOnTouchListener(new OnTouchListener() {

			@SuppressLint("ClickableViewAccessibility")
			@Override
			public boolean onTouch(View v, MotionEvent event) {
				// handle dragging to move

				return mContext.onTouchHandleResize(
						Window.this, event);
			}
		});

		if (Utils.isSet(flags, StandOutFlags.FLAG_DECORATION_MAXIMIZE_DISABLE)) {
			maximize.setVisibility(View.GONE);
		}
		if (Utils.isSet(flags, StandOutFlags.FLAG_DECORATION_CLOSE_DISABLE)) {
			close.setVisibility(View.GONE);
		}
		if (Utils.isSet(flags, StandOutFlags.FLAG_DECORATION_MOVE_DISABLE)) {
			titlebar.setOnTouchListener(null);
		}
		if (Utils.isSet(flags, StandOutFlags.FLAG_DECORATION_RESIZE_DISABLE)) {
			corner.setVisibility(View.GONE);
		}

		return decorations;
	}

	/**
	 * Implement StandOut specific additional functionalities.
	 * 
	 * <p>
	 * Currently, this method does the following:
	 * 
	 * <p>
	 * Attach resize handles: For every View found to have id R.id.corner,
	 * attach an OnTouchListener that implements resizing the window.
	 * 
	 * @param root
	 *            The view hierarchy that is part of the window.
	 */
	private void addFunctionality(View root) {
		// corner for resize
		if (!Utils.isSet(flags,
				StandOutFlags.FLAG_ADD_FUNCTIONALITY_RESIZE_DISABLE)) {
			View corner = root.findViewById(R.id.corner);
			if (corner != null) {
				corner.setOnTouchListener(new OnTouchListener() {

					@SuppressLint("ClickableViewAccessibility")
					@Override
					public boolean onTouch(View v, MotionEvent event) {
						// handle dragging to move

						return mContext.onTouchHandleResize(
								Window.this, event);
					}
				});
			}
		}
	}

	/**
	 * Iterate through each View in the view hiearchy and implement StandOut
	 * specific compatibility workarounds.
	 * 
	 * <p>
	 * Currently, this method does the following:
	 * 
	 * <p>
	 * Nothing yet.
	 * 
	 * @param root
	 *            The root view hierarchy to iterate through and check.
	 */
	private void fixCompatibility(View root) {
		Queue<View> queue = new LinkedList<>();
		queue.add(root);

		View view;
		while ((view = queue.poll()) != null) {
			// do nothing yet

			// iterate through children
			if (view instanceof ViewGroup) {
				ViewGroup group = (ViewGroup) view;
				for (int i = 0; i < group.getChildCount(); i++) {
					queue.add(group.getChildAt(i));
				}
			}
		}
	}

	/**
	 * Convenient way to resize or reposition a Window. The Editor allows you to
	 * easily resize and reposition the window around anchor points.
	 * 
	 * @author Mark Wei <markwei@gmail.com>
	 * 
	 */
	public class Editor {
		/**
		 * Special value for width, height, x, or y positions that represents
		 * that the value should not be changed.
		 */
		static final int UNCHANGED = Integer.MIN_VALUE;

		/**
		 * Layout params of the window associated with this Editor.
		 */
		StandOutLayoutParams mParams;

		/**
		 * The position of the anchor point as a percentage of the window's
		 * width/height. The anchor point is only used by the {@link Editor}.
		 *
		 * <p>
		 * The anchor point effects the following methods:
		 *
		 * <p>
		 * {@link #setSize(float, float)}, {@link #setSize(int, int)},
		 * {@link #setPosition(int, int)}, {@link #setPosition(int, int)}.
		 *
		 * The window will move, expand, or shrink around the anchor point.
		 *
		 * <p>
		 * Values must be between 0 and 1, inclusive. 0 means the left/top, 0.5
		 * is the center, 1 is the right/bottom.
		 */
		float anchorX, anchorY;

		Editor() {
			mParams = getLayoutParams();
			anchorX = anchorY = 0;
		}

		Editor setAnchorPoint() {

			anchorX = (float) 0.5;
			anchorY = (float) 0.5;

			return this;
		}

		Editor setSize(float percentWidth, float percentHeight) {
			return setSize((int) (displayWidth * percentWidth),
					(int) (displayHeight * percentHeight));
		}

		public Editor setSize(int width, int height) {
			return setSize(width, height, false);
		}

		private Editor setSize(int width, int height, boolean skip) {
			if (mParams != null) {
				if (anchorX < 0 || anchorX > 1 || anchorY < 0 || anchorY > 1) {
					throw new IllegalStateException(
							"Anchor point must be between 0 and 1, inclusive.");
				}

				int lastWidth = mParams.width;
				int lastHeight = mParams.height;

				if (width != UNCHANGED) {
					mParams.width = width;
				}
				if (height != UNCHANGED) {
					mParams.height = height;
				}

				// set max width/height
				int maxWidth = mParams.maxWidth;
				int maxHeight = mParams.maxHeight;

				if (Utils.isSet(flags,
						StandOutFlags.FLAG_WINDOW_EDGE_LIMITS_ENABLE)) {
					maxWidth = Math.min(maxWidth, displayWidth);
					maxHeight = Math.min(maxHeight, displayHeight);
				}

				// keep window between min and max
				mParams.width = Math.min(
						Math.max(mParams.width, mParams.minWidth), maxWidth);
				mParams.height = Math.min(
						Math.max(mParams.height, mParams.minHeight), maxHeight);

				// keep window in aspect ratio
				if (Utils.isSet(flags,
						StandOutFlags.FLAG_WINDOW_ASPECT_RATIO_ENABLE)) {
					int ratioWidth = (int) (mParams.height * touchInfo.ratio);
					int ratioHeight = (int) (mParams.width / touchInfo.ratio);
					if (ratioHeight >= mParams.minHeight
							&& ratioHeight <= mParams.maxHeight) {
						// width good adjust height
						mParams.height = ratioHeight;
					} else {
						// height good adjust width
						mParams.width = ratioWidth;
					}
				}

				if (!skip) {
					// set position based on anchor point
					setPosition((int) (mParams.x + lastWidth * anchorX),
							(int) (mParams.y + lastHeight * anchorY));
				}
			}

			return this;
		}

		public Editor setPosition(int x, int y) {
			return setPosition(x, y, false);
		}


		private Editor setPosition(int x, int y, boolean skip) {
			if (mParams != null) {
				if (anchorX < 0 || anchorX > 1 || anchorY < 0 || anchorY > 1) {
					throw new IllegalStateException(
							"Anchor point must be between 0 and 1, inclusive.");
				}

				// sets the x and y correctly according to anchorX and
				// anchorY
				if (x != UNCHANGED) {
					mParams.x = (int) (x - mParams.width * anchorX);
				}
				if (y != UNCHANGED) {
					mParams.y = (int) (y - mParams.height * anchorY);
				}

				if (Utils.isSet(flags,
						StandOutFlags.FLAG_WINDOW_EDGE_LIMITS_ENABLE)) {
					// if gravity is not TOP|LEFT throw exception
					if (mParams.gravity != (Gravity.TOP | Gravity.START)) {
						throw new IllegalStateException(
								"The window "
										+ id
										+ " gravity must be TOP|LEFT if FLAG_WINDOW_EDGE_LIMITS_ENABLE or FLAG_WINDOW_EDGE_TILE_ENABLE is set.");
					}

					// keep window inside edges
					mParams.x = Math.min(Math.max(mParams.x, 0), displayWidth
							- mParams.width);
					mParams.y = Math.min(Math.max(mParams.y, 0), displayHeight
							- mParams.height);
				}
			}

			return this;
		}

		/**
		 * Commit the changes to this window. Updates the layout. This Editor
		 * cannot be used after you commit.
		 */
		public void commit() {
			if (mParams != null) {
				mContext.updateViewLayout(id, mParams);
				mParams = null;
			}
		}
	}

	@Override
	public void onConfigurationChanged(Configuration newConfig) {
		super.onConfigurationChanged(newConfig);
		// Checks the orientation of the screen
		if (newConfig.orientation == Configuration.ORIENTATION_LANDSCAPE) {
			DisplayMetrics metrics = mContext.getResources()
					.getDisplayMetrics();
			displayWidth = metrics.widthPixels;
			displayHeight = (int) (metrics.heightPixels - 25 * metrics.density);
			mContext.updateViewLayout(id, mContext.getParams(id));
		}
		else if (newConfig.orientation == Configuration.ORIENTATION_PORTRAIT)
		{
			DisplayMetrics metrics = mContext.getResources()
					.getDisplayMetrics();
			displayWidth = metrics.widthPixels;
			displayHeight = (int) (metrics.heightPixels - 25 * metrics.density);
			mContext.updateViewLayout(id, mContext.getParams(id));
		}
	}

	static class WindowDataKeys {
		static final String IS_MAXIMIZED = "isMaximized";
		static final String WIDTH_BEFORE_MAXIMIZE = "widthBeforeMaximize";
		static final String HEIGHT_BEFORE_MAXIMIZE = "heightBeforeMaximize";
		static final String X_BEFORE_MAXIMIZE = "xBeforeMaximize";
		static final String Y_BEFORE_MAXIMIZE = "yBeforeMaximize";
	}
}