package fr.frogdevelopment.pregnancycalendar.contraction;

import android.app.AlertDialog;
import android.app.Fragment;
import android.app.LoaderManager;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.CursorLoader;
import android.content.DialogInterface;
import android.content.Loader;
import android.database.Cursor;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.SystemClock;
import android.support.design.widget.Snackbar;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.DividerItemDecoration;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.helper.ItemTouchHelper;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.Chronometer;
import android.widget.LinearLayout;
import android.widget.TextView;

import org.threeten.bp.Instant;
import org.threeten.bp.LocalDateTime;
import org.threeten.bp.ZoneId;
import org.threeten.bp.format.DateTimeFormatter;
import org.threeten.bp.format.FormatStyle;
import org.threeten.bp.temporal.ChronoUnit;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import fr.frogdevelopment.pregnancycalendar.MyTabLayout;
import fr.frogdevelopment.pregnancycalendar.MyViewPager;
import fr.frogdevelopment.pregnancycalendar.R;
import fr.frogdevelopment.pregnancycalendar.contraction.ContractionContract.Contraction;

public class ContractionFragment extends Fragment implements LoaderManager.LoaderCallbacks<Cursor> {

	private ZoneId zoneId = ZoneId.systemDefault();

	private View        mRootView;
	private Chronometer mChronometer;
	private Button      mButton;

	private ContractionAdapter mAdapter;
	private Contraction        currentContraction;
	private ItemTouchHelper    mItemTouchHelper;
	private RecyclerView       mRecyclerView;
	private MyViewPager        mViewPager;
	private MyTabLayout        mTabLayout;

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		// Inflate the layout for this fragment
		mRootView = inflater.inflate(R.layout.fragment_contraction, container, false);

		mChronometer = (Chronometer) mRootView.findViewById(R.id.chronometer);
		mChronometer.setBase(SystemClock.elapsedRealtime());

		mButton = (Button) mRootView.findViewById(R.id.chrono_button);
		mButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				startOrStop();
			}
		});

		mRecyclerView = (RecyclerView) mRootView.findViewById(R.id.chrono_list);
		mRecyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));
		RecyclerView.ItemDecoration itemDecoration = new DividerItemDecoration(getActivity(), DividerItemDecoration.VERTICAL);
		mRecyclerView.addItemDecoration(itemDecoration);

		mAdapter = new ContractionAdapter();
		mRecyclerView.setAdapter(mAdapter);

		mItemTouchHelper = new ItemTouchHelper(new SwipeToDelete(ItemTouchHelper.ACTION_STATE_IDLE, ItemTouchHelper.LEFT, getActivity()));
		mItemTouchHelper.attachToRecyclerView(mRecyclerView);

		getLoaderManager().initLoader(666, null, this);

		mViewPager = (MyViewPager) getActivity().findViewById(R.id.container);
		mTabLayout = (MyTabLayout) getActivity().findViewById(R.id.tabs);

		setHasOptionsMenu(true);

		return mRootView;
	}

	private void startOrStop() {
		if (currentContraction == null) {
			start();
		} else {
			stop();
		}
	}

	private void start() {
		currentContraction = new Contraction();
		currentContraction.dateTime = LocalDateTime.now();

		mButton.setText(R.string.stop);
		mButton.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_stop, 0, 0, 0);

		mChronometer.setBase(SystemClock.elapsedRealtime());
		mChronometer.start();

		mAdapter.add(currentContraction);

		mRootView.setBackgroundResource(R.drawable.background_chrono_started);

		mItemTouchHelper.attachToRecyclerView(null);
		mViewPager.setEnabled(false);
		mTabLayout.setEnabled(false);
	}

	private void stop() {
		mChronometer.stop();
		LocalDateTime mStopDateTime = LocalDateTime.now();
		mButton.setText(R.string.start);
		mButton.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_start, 0, 0, 0);

		currentContraction.duration = ChronoUnit.MILLIS.between(currentContraction.dateTime, mStopDateTime);

		final ContentValues values = new ContentValues();
		values.put(ContractionContract.DATETIME, currentContraction.dateTime.atZone(zoneId).toEpochSecond());
		values.put(ContractionContract.DURATION, currentContraction.duration);

		Uri insertUri = getActivity().getContentResolver().insert(ContractionContentProvider.URI_CONTRACTION, values);

		long newId = ContentUris.parseId(insertUri);
		currentContraction.id = String.valueOf(newId);

		mAdapter.notifyDataSetChanged();

		mRootView.setBackgroundResource(R.drawable.background_chrono_stoped);
		mChronometer.setBase(SystemClock.elapsedRealtime());
		currentContraction = null;

		mItemTouchHelper.attachToRecyclerView(mRecyclerView);
		mViewPager.setEnabled(true);
		mTabLayout.setEnabled(true);
	}

	@Override
	public Loader<Cursor> onCreateLoader(int i, Bundle bundle) {
		return new CursorLoader(getActivity(), ContractionContentProvider.URI_CONTRACTION, ContractionContract.COLUMNS, null, null, null);
	}

	@Override
	public void onLoadFinished(Loader<Cursor> loader, Cursor cursor) {
		mAdapter.clear();

		if (cursor != null) {
			Contraction item;
			while (cursor.moveToNext()) {
				item = new Contraction();
				item.id = cursor.getString(ContractionContract.INDEX_ID);
				item.dateTime = LocalDateTime.ofInstant(Instant.ofEpochSecond(cursor.getLong(ContractionContract.INDEX_DATETIME)), zoneId);
				item.duration = cursor.getLong(ContractionContract.INDEX_DURATION);

				// Add the definition to the list
				mAdapter.add(item);
			}

			cursor.close();
		}

		getLoaderManager().destroyLoader(loader.getId());
	}

	@Override
	public void onLoaderReset(Loader<Cursor> loader) {
	}


	@Override
	public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
		menu.clear();
		inflater.inflate(R.menu.menu_contraction, menu);
		super.onCreateOptionsMenu(menu, inflater);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
			case R.id.action_delete:
				// Ask the user if they want to delete
				new AlertDialog.Builder(getActivity())
//                .setIcon(R.drawable.ic_warning_black)
						.setTitle(R.string.delete_title)
						.setMessage(R.string.delete_confirmation)
						.setPositiveButton(R.string.delete_positive_button_continue, new DialogInterface.OnClickListener() {
							@Override
							public void onClick(DialogInterface dialog, int which) {
								Snackbar.make(mRootView, R.string.delete_deleted, Snackbar.LENGTH_LONG)
										.setAction(R.string.undo, new View.OnClickListener() {
											@Override
											public void onClick(View v) {
												// reload data
												getLoaderManager().restartLoader(666, null,ContractionFragment.this);
											}
										})
										.setActionTextColor(Color.RED)
										.addCallback(new Snackbar.Callback() {

											@Override
											public void onShown(Snackbar sb) {
												// clear view
												mAdapter.clear();
											}

											@Override
											public void onDismissed(Snackbar transientBottomBar, int event) {
												if (event == DISMISS_EVENT_TIMEOUT) {
													// clear dataBase
													 getActivity().getContentResolver().delete(ContractionContentProvider.URI_CONTRACTION, null, null);
												}
											}
										})
										.show();
							}
						})
						.setNegativeButton(android.R.string.no, null)
						.show();

				return true;

			default:
				return super.onOptionsItemSelected(item);
		}
	}

	@Override
	public void setUserVisibleHint(boolean isVisibleToUser) {
		super.setUserVisibleHint(isVisibleToUser);
		if (isVisibleToUser) {
			// reload data when showing view
			getLoaderManager().restartLoader(666, null,ContractionFragment.this);
		}
	}

	private class SwipeToDelete extends ItemTouchHelper.SimpleCallback {

		// we want to cache these and not allocate anything repeatedly in the onChildDraw method
		private Drawable background;
		private Drawable xMark;

		private int xMarkMargin;

		private boolean initiated;
		private Context context;

		SwipeToDelete(int dragDirs, int swipeDirs, Context context) {
			super(dragDirs, swipeDirs);
			this.context = context;
		}

		private void init() {
			background = new ColorDrawable(Color.RED);
			xMark = ContextCompat.getDrawable(context, R.drawable.ic_delete);
			xMark.setColorFilter(Color.WHITE, PorterDuff.Mode.SRC_ATOP);
			xMarkMargin = (int) context.getResources().getDimension(R.dimen.ic_clear_margin);
			initiated = true;
		}

		@Override
		public boolean onMove(RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder, RecyclerView.ViewHolder target) {
			// not important, we don't want drag & drop
			return false;
		}

		@Override
		public void onChildDraw(Canvas c, RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder, float dX, float dY, int actionState, boolean isCurrentlyActive) {
			View itemView = viewHolder.itemView;

			// not sure why, but this method get's called for viewholder that are already swiped away
			if (viewHolder.getAdapterPosition() == -1) {
				// not interested in those
				return;
			}

			if (!initiated) {
				init();
			}

			// draw red background
			background.setBounds(itemView.getRight() + (int) dX, itemView.getTop(), itemView.getRight(), itemView.getBottom());
			background.draw(c);

			if (dX <= -70) {
				// draw x mark
				int itemHeight = itemView.getBottom() - itemView.getTop();
				int intrinsicWidth = xMark.getIntrinsicWidth();
				int intrinsicHeight = xMark.getIntrinsicWidth();

				int xMarkLeft = itemView.getRight() - xMarkMargin - intrinsicWidth;
				int xMarkRight = itemView.getRight() - xMarkMargin;
				int xMarkTop = itemView.getTop() + (itemHeight - intrinsicHeight) / 2;
				int xMarkBottom = xMarkTop + intrinsicHeight;
				xMark.setBounds(xMarkLeft, xMarkTop, xMarkRight, xMarkBottom);

				xMark.draw(c);

			}

			super.onChildDraw(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive);
		}

		@Override
		public void onSwiped(RecyclerView.ViewHolder viewHolder, int direction) {
			mAdapter.pendingRemoval(viewHolder);
		}

		@Override
		public int getSwipeDirs(RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder) {
			if (mAdapter.isPendingRemoval(viewHolder)) {
				return 0;
			}

			return super.getSwipeDirs(recyclerView, viewHolder);
		}
	}

	private static final int               PENDING_REMOVAL_TIMEOUT = 2000;
	private static final DateTimeFormatter dateFormatter           = DateTimeFormatter.ofLocalizedDate(FormatStyle.SHORT);
	private static final DateTimeFormatter timeFormatter           = DateTimeFormatter.ofLocalizedTime(FormatStyle.MEDIUM);

	private class ContractionAdapter extends RecyclerView.Adapter<ContractionViewHolder> {

		private final LayoutInflater mInflater;

		private final Locale            locale              = Locale.getDefault();
		private final List<Contraction> mRows               = new ArrayList<>();
		private final List<String>      mPendingRemovalRows = new ArrayList<>();

		private final Handler               mRemoveHandler  = new Handler();
		private final Map<String, Runnable> mPendingRemoves = new HashMap<>();

		ContractionAdapter() {
			mInflater = getActivity().getLayoutInflater();
		}

		void add(Contraction contraction) {
			mRows.add(contraction);
			notifyDataSetChanged();
		}

		void addAll(List<Contraction> contractions) {
			mRows.addAll(contractions);
			notifyDataSetChanged();
		}

		void clear() {
			mRows.clear();
			notifyDataSetChanged();
		}

		@Override
		public int getItemCount() {
			return mRows.size();
		}

		private Contraction getItem(int position) {
			try {
				return mRows.get(getItemCount() - position - 1); // reverse order
			} catch (IndexOutOfBoundsException e) {
				return null;
			}
		}

		@Override
		public ContractionViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
			// Inflate the custom layout
			View contactView = mInflater.inflate(R.layout.row_contraction, parent, false);

			// Return a new holder instance
			return new ContractionViewHolder(contactView);
		}

		// Involves populating data into the item through holder
		@Override
		public void onBindViewHolder(final ContractionViewHolder viewHolder, int position) {
			// Get the data model based on position
			final Contraction item = getItem(position);

			if (item == null) {
				return;
			}

			if (isPendingRemoval(viewHolder)) {
				/** {show undo layout} and {hide regular layout} */
				viewHolder.regularLayout.setVisibility(View.GONE);
				viewHolder.undoLayout.setVisibility(View.VISIBLE);

				viewHolder.undo.setOnClickListener(new View.OnClickListener() {
					@Override
					public void onClick(View v) {
						undo(item, viewHolder.getAdapterPosition());
					}
				});
			} else {
				/** {show regular layout} and {hide undo layout} */
				viewHolder.regularLayout.setVisibility(View.VISIBLE);
				viewHolder.undoLayout.setVisibility(View.GONE);

				viewHolder.date.setText(item.dateTime.format(dateFormatter));
				viewHolder.time.setText(item.dateTime.format(timeFormatter));

				if (item.duration != null) {
					viewHolder.duration.setText(durationToLabel(item.duration));
				} else {
					viewHolder.duration.setText("--:--");
				}

				Contraction previous = getItem(position + 1); // +1 as reverse order ...
				if (previous != null) {
					long durationSincePrevious = ChronoUnit.MILLIS.between(previous.dateTime.plus(previous.duration, ChronoUnit.MILLIS), item.dateTime);
					viewHolder.last.setText(durationToLabel(durationSincePrevious));
				} else {
					viewHolder.last.setText("--:--");
				}
			}
		}

		private String durationToLabel(long duration) {
			String label;
			long seconds = TimeUnit.MILLISECONDS.toSeconds(duration);
			if (seconds < 60) { // less than 1 minute
				label = String.format(locale, "%02dsec", seconds);
			} else { // more than 1 minute
				long minutes = TimeUnit.MILLISECONDS.toMinutes(duration);
				if (minutes < 60) { // less than 1 hour
					label = String.format(locale, "%02dmin%02d", minutes, seconds - TimeUnit.MINUTES.toSeconds(minutes));
				} else { //more than 1 hour
					long hour = TimeUnit.MINUTES.toHours(minutes);
					if (hour < 24) { // less than 1 day
						label = String.format(locale, "%02dh%02d", hour, minutes - TimeUnit.HOURS.toMinutes(hour));
					} else { // more than 1 day
						long days = TimeUnit.HOURS.toDays(hour);
						label = String.format(locale, "%02dj%02dh", days, TimeUnit.MILLISECONDS.toHours(duration) - TimeUnit.DAYS.toHours(days));
					}
				}
			}

			return label;
		}

		boolean isPendingRemoval(RecyclerView.ViewHolder viewHolder) {
			Contraction item = getItem(viewHolder.getAdapterPosition());
			return item != null && mPendingRemovalRows.contains(item.id);
		}

		void pendingRemoval(final RecyclerView.ViewHolder viewHolder) {
			final int position = viewHolder.getAdapterPosition();

			final Contraction item = getItem(position);

			if (item != null && !mPendingRemovalRows.contains(item.id)) {
				mPendingRemovalRows.add(item.id);
				// this will redraw row in "undo" state
				notifyItemChanged(position);
				// let's create, store and post a runnable to remove the data
				Runnable pendingRemovalRunnable = new Runnable() {
					@Override
					public void run() {
						remove(item, position);
					}
				};
				mRemoveHandler.postDelayed(pendingRemovalRunnable, PENDING_REMOVAL_TIMEOUT);
				mPendingRemoves.put(item.id, pendingRemovalRunnable);
			}
		}

		private void remove(Contraction item, int adapterPosition) {
			if (mPendingRemovalRows.contains(item.id)) {
				mPendingRemovalRows.remove(item.id);
			}

			if (mRows.contains(item)) {

				Uri uri = Uri.parse(ContractionContentProvider.URI_CONTRACTION + "/" + item.id);
				getActivity().getContentResolver().delete(uri, null, null);

				mRows.remove(item);
				notifyItemRemoved(adapterPosition);

				Snackbar.make(mRootView, R.string.delete_deleted, Snackbar.LENGTH_LONG).show();
			}
		}

		private void undo(Contraction item, int adapterPosition) {
			// user wants to undo the removal, let's cancel the pending task
			Runnable pendingRemovalRunnable = mPendingRemoves.remove(item.id);
			if (pendingRemovalRunnable != null) {
				mRemoveHandler.removeCallbacks(pendingRemovalRunnable);
			}
			mPendingRemovalRows.remove(item.id);
			// this will rebind the row in "normal" state
			notifyItemChanged(adapterPosition);
		}
	}

	class ContractionViewHolder extends RecyclerView.ViewHolder {

		final LinearLayout regularLayout;
		final TextView     date;
		final TextView     time;
		final TextView     duration;
		final TextView     last;

		final LinearLayout undoLayout;
		final TextView     undo;

		ContractionViewHolder(View view) {
			super(view);

			regularLayout = (LinearLayout) view.findViewById(R.id.row_regular_layout);

			date = (TextView) view.findViewById(R.id.row_contraction_date);
			time = (TextView) view.findViewById(R.id.row_contraction_time);
			duration = (TextView) view.findViewById(R.id.row_contraction_duration);
			last = (TextView) view.findViewById(R.id.row_contraction_last);

			undoLayout = (LinearLayout) view.findViewById(R.id.row_undo_layout);
			undo = (TextView) view.findViewById(R.id.undo);
		}
	}

}
