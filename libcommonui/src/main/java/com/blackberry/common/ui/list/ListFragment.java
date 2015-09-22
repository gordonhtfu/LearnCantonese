
package com.blackberry.common.ui.list;

import com.blackberry.common.ui.R;
import com.blackberry.common.ui.fragment.ContractFragment;
import com.blackberry.common.ui.list.ListUIDelegate.ListLayoutType;
import com.blackberry.common.ui.swipemenulistview.SwipeMenuListView;
import com.blackberry.common.ui.swipemenulistview.SwipeMenuListView.OnSwipeListener;

import android.database.Cursor;
import android.os.Bundle;
import android.os.Handler;
import android.view.ActionMode;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.ListView;

import java.util.UUID;

public final class ListFragment extends ContractFragment<ListFragment.DelegateProvider> {

    public interface DelegateProvider {
        ListUIDelegate getDelegate(int id);
    }

    private AbsListView mListView;
    private int mUniqueId = -1;
    private static final String KEY_UUID_STATE = "uuid";

    private ActionMode mActionMode;
    private ActionModeHandler mActionModeHandler;
    private Handler mHandler = new Handler();
    private Runnable cabClearRunnable;

    public ListFragment() {
        super(DelegateProvider.class);
        mUniqueId = UUID.randomUUID().hashCode();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (savedInstanceState != null) {
            mUniqueId = savedInstanceState.getInt(KEY_UUID_STATE);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {

        ListLayoutType type = getDelegate().getListLayoutType();
        View rootView = initListView(inflater, container, getLayout(type), type);

        mListView.setOnItemClickListener(new ListItemClickListener());
        mListView.setOnItemLongClickListener(new ListItemLongClickListener());
        setSwipeListener();

        return rootView;
    }

    /*
     * TODO: Find out if this can be triggered per item rather than handled by the ListView itself.
     */
    private void setSwipeListener() {
        if (mListView instanceof SwipeMenuListView) {
            SwipeMenuListView swipeListView = (SwipeMenuListView) mListView;
            swipeListView.setOnSwipeListener(new OnSwipeListener() {
                @Override
                public void onSwipeStart(int position) {
                    // swipe start
                }

                @Override
                public void onSwipeEnd(int position) {

                    Object item = mListView.getItemAtPosition(position);
                    if (item != null) {
                        if (item instanceof Cursor) {
                            getDelegate().listItemSwipe(item);
                        }
                    }
                }
            });
        }
    }

    private int getLayout(ListLayoutType type) {
        int layoutId = -1;
        switch (type) {
            case LIST:
                layoutId = R.layout.fragment_list;
                break;
            case GRID:
                layoutId = R.layout.fragment_grid;
                break;
            case STICKY_LIST:
                layoutId = R.layout.fragment_sticky_list;
                break;
            case TREE:
                layoutId = R.layout.fragment_tree;
                break;

            default:
                layoutId = R.layout.fragment_list;
        }
        return layoutId;
    }

    private View initListView(LayoutInflater inflater, ViewGroup container, int layoutId,
            ListLayoutType layoutType) {
        View rootView = inflater.inflate(layoutId, container, false);
        mListView = (AbsListView) rootView.findViewById(R.id.list_entities);
        mListView.setAdapter(getDelegate().getAdapter());
        return rootView;
    }

    public ListUIDelegate getDelegate() {
        ListUIDelegate delegate = getContract().getDelegate(this.mUniqueId);
        if (delegate == null) {
            throw new NullPointerException("DelegateProvider fails to return a valid Delegate!");
        } else {
            return delegate;
        }
    }

    private class ListItemClickListener implements ListView.OnItemClickListener {
        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
            Object item = parent.getItemAtPosition(position);
            if (item != null) {
                getDelegate().listItemClick(item);
                ListFragment.this.delayedClearListChoices();
            }
        }
    }

    private class ListItemLongClickListener implements ListView.OnItemLongClickListener {
        @Override
        public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
            Object item = parent.getItemAtPosition(position);
            if (mActionModeHandler == null) {
                mActionModeHandler = new ActionModeHandler();
            }

            mActionModeHandler.setSelectedItem(item);

            if (mActionMode != null) {
                // don't acknowledge the long press if a CAB is already active.
                // NOTE: this won't work with multi select (which we will have to change anyway)
                // but will give a nicer behavior for single select mode.
                return false;
            }

            mActionMode = getActivity().startActionMode(mActionModeHandler);
            mListView.setItemChecked(position, true);

            return true;
        }
    }

    public void refresh() {
        getDelegate().refreshData(getLoaderManager(), mUniqueId);
    }

    public int getUniqueId() {
        return mUniqueId;
    }

    private void delayedClearListChoices() {
        if (cabClearRunnable == null) {
            cabClearRunnable = new Runnable() {
                @Override
                public void run() {
                    if (mActionMode != null) {
                        // this is when user enabled the CAB, and then tap on an item to open
                        // in this case, simply close the CAB, which will clear the
                        // list selection.
                        mActionMode.finish();
                    } else {
                        // This is the case where user simply clicks on an item.
                        // Simply clear the choices.
                        ListFragment.this.clearListChoices();
                    }
                }
            };
        }
        mHandler.postDelayed(cabClearRunnable, 150);
    }

    private void clearListChoices() {
        mListView.clearChoices();
        mListView.requestLayout();
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt(KEY_UUID_STATE, mUniqueId);
    }

    /**
     * ActionMode Callback implementation. This is the class that will listen to all action mode
     * events, and then delegate the task to our CAB delegate (e.g. to populate menu, and perform
     * action when the menu item is clicked by the user).
     * 
     * @author dsutedja
     */
    private class ActionModeHandler implements ActionMode.Callback {
        ListUICabDelegate mDelegate;
        Object mSelectedItem;

        ActionModeHandler() {
            mDelegate = getDelegate().getCabDelegate();
        }

        void setSelectedItem(Object item) {
            mSelectedItem = item;
        }

        @Override
        public boolean onCreateActionMode(ActionMode mode, Menu menu) {
            tellListView(true);
            mDelegate.setSelectedItem(mSelectedItem);
            return mDelegate.populateMenu(menu);
        }

        @Override
        public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
            // TODO Auto-generated method stub
            return false;
        }

        @Override
        public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
            boolean retVal = mDelegate.onMenuItemClicked(item);
            ListFragment.this.delayedClearListChoices();
            return retVal;
        }

        @Override
        public void onDestroyActionMode(ActionMode mode) {
            ListFragment.this.clearListChoices();
            ListFragment.this.mActionMode = null;
            tellListView(false);
        }

        private void tellListView(boolean actionModeStarted) {
            if (ListFragment.this.mListView instanceof ActionModeStateListener) {
                ActionModeStateListener listener =
                        (ActionModeStateListener) ListFragment.this.mListView;
                if (actionModeStarted) {
                    listener.actionModeStarted();
                } else {
                    listener.actionModeEnded();
                }
            }
        }
    }
}
