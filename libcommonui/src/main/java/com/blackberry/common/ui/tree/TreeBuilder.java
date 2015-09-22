/*
 * Copyright (c) 2011, Polidea
 * 
All rights reserved. Redistribution and use in source and binary forms, with or 
without modification,  are permitted provided that the following conditions are 
met: Redistributions of source code must retain the above copyright notice, this 
list of conditions and the following disclaimer. Redistributions in binary form 
must reproduce the above copyright notice, this list of conditions and the 
following disclaimer in the documentation and/or other materials provided with 
the distribution.

THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND 
ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. 
IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, 
INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, 
BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA,
OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, 
WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) 
ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE 
POSSIBILITY OF SUCH DAMAGE.

https://github.com/Polidea/tree-view-list-android/blob/master/LICENCE.txt
 */

package com.blackberry.common.ui.tree;

import android.util.Log;

/**
 * Allows to build tree easily in sequential mode (you have to know levels of all the tree elements
 * upfront). You should rather use this class rather than manager if you build initial tree from
 * some external data source.
 * <p>
 * Note, that all ids must be unique. IDs are used to find nodes in the whole tree, so they cannot
 * repeat even if they are in different sub-trees.
 * 
 * @param <NODE_ID> type of the identifier used by the tree
 */
public class TreeBuilder<NODE_ID> {
    private static final String TAG = TreeBuilder.class.getSimpleName();

    private final TreeStateManager<NODE_ID> manager;

    private NODE_ID lastAddedId = null;
    private int lastLevel = -1;

    public TreeBuilder(final TreeStateManager<NODE_ID> manager) {
        this.manager = manager;
    }

    public void clear() {
        manager.clear();
        lastAddedId = null;
        lastLevel = -1;
    }

    /**
     * Adds new relation to existing tree. Child is set as the last child of the parent node. Parent
     * has to already exist in the tree, child cannot yet exist. This method is mostly useful in
     * case you add entries layer by layer - i.e. first top level entries, then children for all
     * parents, then grand-children and so on.
     * 
     * @param parent parent id
     * @param child child id
     */
    public synchronized void addRelation(final NODE_ID parent, final NODE_ID child) {
        Log.d(TAG, "Adding relation parent:" + parent + " -> child: " + child);
        manager.addAfterChild(parent, child, null);
        lastAddedId = child;
        lastLevel = manager.getLevel(child);
    }

    /**
     * Adds sequentially new node. Using this method is the simplest way of building tree - if you
     * have all the elements in the sequence as they should be displayed in fully-expanded tree. You
     * can combine it with add relation - for example you can add information about few levels using
     * {@link addRelation} and then after the right level is added as parent, you can continue
     * adding them using sequential operation.
     * 
     * @param id id of the node
     * @param level its level
     */
    public synchronized void sequentiallyAddNextNode(final NODE_ID id, final int level) {
        Log.d(TAG, "Adding sequentiall node " + id + " at level " + level);
        if (lastAddedId == null) {
            addNodeToParentOneLevelDown(null, id, level);
        } else {
            if (level <= lastLevel) {
                final NODE_ID parent = findParentAtLevel(lastAddedId, level - 1);
                addNodeToParentOneLevelDown(parent, id, level);
            } else {
                addNodeToParentOneLevelDown(lastAddedId, id, level);
            }
        }
    }

    /**
     * Find parent of the node at the level specified.
     * 
     * @param node node from which we start
     * @param levelToFind level which we are looking for
     * @return the node found (null if it is topmost node).
     */
    private NODE_ID findParentAtLevel(final NODE_ID node, final int levelToFind) {
        NODE_ID parent = manager.getParent(node);
        while (parent != null) {
            if (manager.getLevel(parent) == levelToFind) {
                break;
            }
            parent = manager.getParent(parent);
        }
        return parent;
    }

    /**
     * Adds note to parent at the level specified. But it verifies that the level is one level down
     * than the parent!
     * 
     * @param parent parent parent
     * @param id new node id
     * @param level should always be parent's level + 1
     */
    private void addNodeToParentOneLevelDown(final NODE_ID parent, final NODE_ID id,
            final int level) {
        if (parent == null && level != 0) {
            throw new TreeConfigurationException("Trying to add new id " + id
                    + " to top level with level != 0 (" + level + ")");
        }
        if (parent != null && manager.getLevel(parent) != level - 1) {
            throw new TreeConfigurationException("Trying to add new id " + id
                    + " <" + level + "> to " + parent + " <"
                    + manager.getLevel(parent)
                    + ">. The difference in levels up is bigger than 1.");
        }
        manager.addAfterChild(parent, id, null);
        setLastAdded(id, level);
    }

    private void setLastAdded(final NODE_ID id, final int level) {
        lastAddedId = id;
        lastLevel = level;
    }

}
