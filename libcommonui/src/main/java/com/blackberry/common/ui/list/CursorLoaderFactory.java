
package com.blackberry.common.ui.list;

import android.content.CursorLoader;
import android.os.Bundle;

/**
 * Factory interface for creating {@link CursorLoaders}.
 */
public interface CursorLoaderFactory {

    /**
     * Instantiate and return a new CursorLoader for the given Id.
     * 
     * @param id The Id of the loader is to be created.
     * @param args Any arguments supplied by the caller.
     * @return Return a new CursorLoader instance.
     */
    CursorLoader createLoader(int id, Bundle args);

}
