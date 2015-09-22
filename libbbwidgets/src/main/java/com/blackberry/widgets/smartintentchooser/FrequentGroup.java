
package com.blackberry.widgets.smartintentchooser;

import android.content.Context;
import android.database.DataSetObserver;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Adapter;
import android.widget.FrameLayout;

import com.blackberry.widgets.R;

/**
 * A specifically-laid out View containing up to 5 square elements. They are
 * laid out on the screen in two equally-sized columns. In the left column is
 * one large square View while the right column has 4 smaller square Views. The
 * child Views are populated by the Adapter provided to this View.
 */
public class FrequentGroup extends FrameLayout {

    private int mOldLayout = -1;
    private SquareFrameLayout[] frequentPositions = new SquareFrameLayout[MAX_LOCATIONS];
    private Adapter mAdapter;
    private DataSetObserver adapterDataSetObserver = new DataSetObserver() {
        @Override
        public void onChanged() {
            updateView();
        }

        @Override
        public void onInvalidated() {
            updateView();
        }
    };

    /**
     * How many locations we have
     */
    private static final int MAX_LOCATIONS = 10;

    /**
     * Constructor.
     * 
     * @param context The context
     * @param attrs The xml attributes
     */
    public FrequentGroup(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    private void loadProperLayout() {
        if ((mAdapter == null) || (mAdapter.getCount() == 0)) {
            removeAllViews();
            for (int i = 0; i < MAX_LOCATIONS; i += 1) {
                frequentPositions[i] = null;
            }
            mOldLayout = -1;
        }
        int layout;
        switch (mAdapter.getCount()) {
            case 1:
                layout = R.layout.frequent_group_1;
                break;
            case 2:
                layout = R.layout.frequent_group_2;
                break;
            case 3:
                layout = R.layout.frequent_group_3;
                break;
            case 4:
                layout = R.layout.frequent_group_4;
                break;
            case 5:
                layout = R.layout.frequent_group_5;
                break;
            case 6:
                layout = R.layout.frequent_group_6;
                break;
            case 7:
                layout = R.layout.frequent_group_7;
                break;
            case 8:
                layout = R.layout.frequent_group_8;
                break;
            case 9:
                layout = R.layout.frequent_group_9;
                break;
            default:
                // default is to grab 10
                layout = R.layout.frequent_group_10;
                break;
        }

        if (mOldLayout != layout) {
            removeAllViews();

            LayoutInflater inflater =
                    (LayoutInflater) getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            inflater.inflate(layout, this, true);

            frequentPositions[0] = (SquareFrameLayout) findViewById(R.id.fgLocation1);
            frequentPositions[1] = (SquareFrameLayout) findViewById(R.id.fgLocation2);
            frequentPositions[2] = (SquareFrameLayout) findViewById(R.id.fgLocation3);
            frequentPositions[3] = (SquareFrameLayout) findViewById(R.id.fgLocation4);
            frequentPositions[4] = (SquareFrameLayout) findViewById(R.id.fgLocation5);
            frequentPositions[5] = (SquareFrameLayout) findViewById(R.id.fgLocation6);
            frequentPositions[6] = (SquareFrameLayout) findViewById(R.id.fgLocation7);
            frequentPositions[7] = (SquareFrameLayout) findViewById(R.id.fgLocation8);
            frequentPositions[8] = (SquareFrameLayout) findViewById(R.id.fgLocation9);
            frequentPositions[9] = (SquareFrameLayout) findViewById(R.id.fgLocation10);

            mOldLayout = layout;
        }
    }

    /**
     * Sets the {@link Adapter}.
     * 
     * @param adapter The adapter used to provide the Views to display
     */
    public void setAdapter(Adapter adapter) {
        if (mAdapter != null) {
            mAdapter.unregisterDataSetObserver(adapterDataSetObserver);
        }
        mAdapter = adapter;
        updateView();
        if (mAdapter != null) {
            mAdapter.registerDataSetObserver(adapterDataSetObserver);
        }
    }

    /**
     * Update the view.
     */
    private void updateView() {
        loadProperLayout();
        int itemCount = mAdapter == null ? 0 : mAdapter.getCount();
        for (int i = 0; i < MAX_LOCATIONS; i += 1) {
            if (frequentPositions[i] != null) {
                if (i >= itemCount) {
                    frequentPositions[i].removeAllViews();
                } else {
                    View oldView = frequentPositions[i].getChildAt(0);
                    View newView = mAdapter.getView(i, oldView, frequentPositions[i]);
                    if (oldView != newView) {
                        if (oldView != null) {
                            frequentPositions[i].removeAllViews();
                        }
                        frequentPositions[i].addView(newView);
                    }
                }
            }
        }
        // hide the control if the height or width is supposed to be wrapped and
        // there is
        // nothing to show
        if (itemCount > 0) {
            setVisibility(VISIBLE);
        } else {
            ViewGroup.LayoutParams lp = getLayoutParams();
            if ((lp == null) || (lp.height == ViewGroup.LayoutParams.WRAP_CONTENT)
                    || (lp.width == ViewGroup.LayoutParams.WRAP_CONTENT)) {
                setVisibility(GONE);
            } else {
                setVisibility(VISIBLE);
            }
        }
    }
}
