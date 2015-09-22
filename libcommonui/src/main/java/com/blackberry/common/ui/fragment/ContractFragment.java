
package com.blackberry.common.ui.fragment;

import android.app.Activity;
import android.app.Fragment;

/**
 * Base {@link Fragment} to ensure the parent activity implements a contract interface.
 * 
 * @param <T> The contract interface type.
 */
public class ContractFragment<T> extends Fragment {
    private T mContract;
    private Class<T> mClazz;

    /**
     * Constructor.
     * 
     * @param clazz The class type of contract interface.
     */
    public ContractFragment(Class<T> clazz) {
        mClazz = clazz;
    }

    /**
     * {@inheritDoc}
     * <p>
     * Ensure that the parent activity implements the contract interface the {@link Fragment}
     * expects.
     */
    @Override
    public void onAttach(Activity activity) {
        mContract = safeCast(activity);
        super.onAttach(activity);
    }

    private T safeCast(Activity activity) {
        try {
            return mClazz.cast(activity);
        } catch (ClassCastException e) {
            throw new IllegalStateException(activity.getClass().getSimpleName()
                    + " does not implement " + getClass().getSimpleName()
                    + "'s contract interface.", e);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onDetach() {
        super.onDetach();
        mContract = null;
    }

    /**
     * Gets the contract.
     * 
     * @return the contract interface
     */
    public final T getContract() {
        if (mContract != null) {
            return mContract;
        } else {
            throw new NullPointerException("Contract not attached!");
        }
    }
}
