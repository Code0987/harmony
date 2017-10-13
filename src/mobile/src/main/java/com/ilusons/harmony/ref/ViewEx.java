package com.ilusons.harmony.ref;

import android.content.Context;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.TransitionDrawable;
import android.os.Bundle;
import android.support.annotation.ColorRes;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.graphics.drawable.DrawableCompat;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.util.AttributeSet;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import java.util.ArrayList;
import java.util.List;

public class ViewEx {

	/**
	 * Fragment adapter for view pager
	 */
	public static class DynamicFragmentViewPagerAdapter extends FragmentPagerAdapter {

		public static final String KEY_TITLE = "_title";

		private final List<Fragment> fragments = new ArrayList<>();

		public DynamicFragmentViewPagerAdapter(FragmentManager manager) {
			super(manager);
		}

		@Override
		public Fragment getItem(int position) {
			return fragments.get(position);
		}

		@Override
		public int getCount() {
			return fragments.size();
		}

		@Override
		public CharSequence getPageTitle(int position) {
			return fragments.get(position).getArguments().getString(KEY_TITLE);
		}

		public void add(Fragment fragment, String title) {
			Bundle bundle = fragment.getArguments();
			if (bundle == null)
				bundle = new Bundle();
			if (!bundle.containsKey(KEY_TITLE))
				bundle.putString(KEY_TITLE, title);
			fragment.setArguments(bundle);

			fragments.add(fragment);

			notifyDataSetChanged();
		}

		public void remove(Fragment fragment) {
			fragments.remove(fragment);

			notifyDataSetChanged();
		}

		public void clear() {
			fragments.clear();

			notifyDataSetChanged();
		}

	}

	/**
	 * View pager used for a finite, low number of pages, where there is no need for
	 * optimization.
	 */
	public static class DynamicViewPagerAdapter extends PagerAdapter {
		// This holds all the currently displayable views, in order from left to right.
		private ArrayList<View> views = new ArrayList<View>();

		//-----------------------------------------------------------------------------
		// Used by ViewPager.  "Object" represents the page; tell the ViewPager where the
		// page should be displayed, from left-to-right.  If the page no longer exists,
		// return POSITION_NONE.
		@Override
		public int getItemPosition(Object object) {
			int index = views.indexOf(object);
			if (index == -1)
				return POSITION_NONE;
			else
				return index;
		}

		//-----------------------------------------------------------------------------
		// Used by ViewPager.  Called when ViewPager needs a page to display; it is our job
		// to add the page to the container, which is normally the ViewPager itself.  Since
		// all our pages are persistent, we simply retrieve it from our "views" ArrayList.
		@Override
		public Object instantiateItem(ViewGroup container, int position) {
			View v = views.get(position);
			container.addView(v);
			return v;
		}

		//-----------------------------------------------------------------------------
		// Used by ViewPager.  Called when ViewPager no longer needs a page to display; it
		// is our job to remove the page from the container, which is normally the
		// ViewPager itself.  Since all our pages are persistent, we do nothing to the
		// contents of our "views" ArrayList.
		@Override
		public void destroyItem(ViewGroup container, int position, Object object) {
			container.removeView(views.get(position));
		}

		//-----------------------------------------------------------------------------
		// Used by ViewPager; can be used by app as well.
		// Returns the total number of pages that the ViewPage can display.  This must
		// never be 0.
		@Override
		public int getCount() {
			return views.size();
		}

		//-----------------------------------------------------------------------------
		// Used by ViewPager.
		@Override
		public boolean isViewFromObject(View view, Object object) {
			return view == object;
		}

		//-----------------------------------------------------------------------------
		// Add "view" to right end of "views".
		// Returns the position of the new view.
		// The app should call this to add pages; not used by ViewPager.
		public int addView(View v) {
			return addView(v, views.size());
		}

		//-----------------------------------------------------------------------------
		// Add "view" at "position" to "views".
		// Returns position of new view.
		// The app should call this to add pages; not used by ViewPager.
		public int addView(View v, int position) {
			views.add(position, v);
			return position;
		}

		//-----------------------------------------------------------------------------
		// Removes "view" from "views".
		// Retuns position of removed view.
		// The app should call this to remove pages; not used by ViewPager.
		public int removeView(ViewPager pager, View v) {
			return removeView(pager, views.indexOf(v));
		}

		//-----------------------------------------------------------------------------
		// Removes the "view" at "position" from "views".
		// Retuns position of removed view.
		// The app should call this to remove pages; not used by ViewPager.
		public int removeView(ViewPager pager, int position) {
			// ViewPager doesn't have a delete method; the closest is to set the adapter
			// again.  When doing so, it deletes all its views.  Then we can delete the view
			// from from the adapter and finally set the adapter to the pager again.  Note
			// that we set the adapter to null before removing the view from "views" - that's
			// because while ViewPager deletes all its views, it will call destroyItem which
			// will in turn cause a null pointer ref.
			pager.setAdapter(null);
			views.remove(position);
			pager.setAdapter(this);

			return position;
		}

		//-----------------------------------------------------------------------------
		// Returns the "view" at "position".
		// The app should call this to retrieve a view; not used by ViewPager.
		public View getView(int position) {
			return views.get(position);
		}

		// Other relevant methods:

		// finishUpdate - called by the ViewPager - we don't care about what pages the
		// pager is displaying so we don't use this method.

		@Override
		public CharSequence getPageTitle(int position) {
			return super.getPageTitle(position);
		}
	}

	public static void tintMenuIcon(Context context, MenuItem item, @ColorRes int color) {
		Drawable normalDrawable = item.getIcon();
		Drawable wrapDrawable = DrawableCompat.wrap(normalDrawable);
		DrawableCompat.setTint(wrapDrawable, context.getResources().getColor(color));

		item.setIcon(wrapDrawable);
	}

	/**
	 * Fragment adapter for view pager
	 */
	public static class FragmentViewPagerAdapter extends FragmentPagerAdapter {

		private final List<Fragment> fragmentList = new ArrayList<>();
		private final List<String> titleList = new ArrayList<>();

		public FragmentViewPagerAdapter(FragmentManager manager) {
			super(manager);
		}

		@Override
		public Fragment getItem(int position) {
			return fragmentList.get(position);
		}

		@Override
		public int getCount() {
			return fragmentList.size();
		}

		public void add(Fragment fragment, String title) {
			fragmentList.add(fragment);
			titleList.add(title);

			notifyDataSetChanged();
		}

		@Override
		public CharSequence getPageTitle(int position) {
			return titleList.get(position);
		}
	}

	/**
	 * View pager used for a finite, low number of pages, where there is no need for
	 * optimization.
	 */
	public static class StaticViewPager extends ViewPager {

		/**
		 * Initialize the view.
		 *
		 * @param context The application context.
		 */
		public StaticViewPager(final Context context) {
			super(context);
		}

		/**
		 * Initialize the view.
		 *
		 * @param context The application context.
		 * @param attrs   The requested attributes.
		 */
		public StaticViewPager(final Context context, final AttributeSet attrs) {
			super(context, attrs);
		}

		@Override
		protected void onAttachedToWindow() {
			super.onAttachedToWindow();

			// Make sure all are loaded at once
			final int childrenCount = getChildCount();
			setOffscreenPageLimit(childrenCount - 1);

			// Attach the adapter
			setAdapter(new PagerAdapter() {

				@Override
				public Object instantiateItem(final ViewGroup container, final int position) {
					return container.getChildAt(position);
				}

				@Override
				public boolean isViewFromObject(final View arg0, final Object arg1) {
					return arg0 == arg1;

				}

				@Override
				public int getCount() {
					return childrenCount;
				}

				@Override
				public void destroyItem(final View container, final int position, final Object object) {
				}

				@Override
				public CharSequence getPageTitle(int position) {
					return getChildAt(position).getTag().toString();
				}
			});
		}

	}

	public static boolean hasBitmap(ImageView iv) {
		Drawable drawable = iv.getDrawable();
		BitmapDrawable bitmapDrawable = drawable instanceof BitmapDrawable ? (BitmapDrawable) drawable : null;

		return bitmapDrawable != null && bitmapDrawable.getBitmap() != null;
	}

	public static boolean hasTransitionDrawable(ImageView iv) {
		Drawable drawable = iv.getDrawable();
		TransitionDrawable transitionDrawable = drawable instanceof TransitionDrawable ? (TransitionDrawable) drawable : null;

		return transitionDrawable != null;
	}

}
