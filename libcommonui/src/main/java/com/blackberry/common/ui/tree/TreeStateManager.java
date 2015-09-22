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

import android.database.DataSetObserver;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * Manages information about state of the tree. It only keeps information about tree elements, not
 * the elements themselves.
 * 
 * @param <NODE_ID> type of the identifier used by the tree
 */
public interface TreeStateManager<NODE_ID> extends Serializable {

    /**
     * Returns array of integers showing the location of the node in hierarchy. It corresponds to
     * heading numbering. {0,0,0} in 3 level node is the first node {0,0,1} is second leaf (assuming
     * that there are two leaves in first subnode of the first node).
     * 
     * @param id id of the node
     * @return textual description of the hierarchy in tree for the node.
     */
    Integer[] getHierarchyDescription(NODE_ID id);

    /**
     * Returns level of the node.
     * 
     * @param id id of the node
     * @return level in the tree
     */
    int getLevel(NODE_ID id);

    /**
     * Returns information about the node.
     * 
     * @param id node id
     * @return node info
     */
    TreeNodeInfo<NODE_ID> getNodeInfo(NODE_ID id);

    /**
     * Returns children of the node.
     * 
     * @param id id of the node or null if asking for top nodes
     * @return children of the node
     */
    List<NODE_ID> getChildren(NODE_ID id);

    /**
     * Returns parent of the node.
     * 
     * @param id id of the node
     * @return parent id or null if no parent
     */
    NODE_ID getParent(NODE_ID id);

    /**
     * Adds the node before child or at the beginning.
     * 
     * @param parent id of the parent node. If null - adds at the top level
     * @param newChild new child to add if null - adds at the beginning.
     * @param beforeChild child before which to add the new child
     */
    void addBeforeChild(NODE_ID parent, NODE_ID newChild, NODE_ID beforeChild);

    /**
     * Adds the node after child or at the end.
     * 
     * @param parent id of the parent node. If null - adds at the top level.
     * @param newChild new child to add. If null - adds at the end.
     * @param afterChild child after which to add the new child
     */
    void addAfterChild(NODE_ID parent, NODE_ID newChild, NODE_ID afterChild);

    /**
     * Removes the node and all children from the tree.
     * 
     * @param id id of the node to remove or null if all nodes are to be removed.
     */
    ArrayList<NODE_ID> removeNodeRecursively(NODE_ID id);

    /**
     * Expands all children of the node.
     * 
     * @param id node which children should be expanded. cannot be null (top nodes are always
     *            expanded!).
     */
    void expandDirectChildren(NODE_ID id);

    /**
     * Expands everything below the node specified. Might be null - then expands all.
     * 
     * @param id node which children should be expanded or null if all nodes are to be expanded.
     */
    void expandEverythingBelow(NODE_ID id);

    /**
     * Collapse children.
     * 
     * @param id id collapses everything below node specified. If null, collapses everything but
     *            top-level nodes.
     */
    void collapseChildren(NODE_ID id);

    /**
     * Returns next sibling of the node (or null if no further sibling).
     * 
     * @param id node id
     * @return the sibling (or null if no next)
     */
    NODE_ID getNextSibling(NODE_ID id);

    /**
     * Returns previous sibling of the node (or null if no previous sibling).
     * 
     * @param id node id
     * @return the sibling (or null if no previous)
     */
    NODE_ID getPreviousSibling(NODE_ID id);

    /**
     * Checks if given node is already in tree.
     * 
     * @param id id of the node
     * @return true if node is already in tree.
     */
    boolean isInTree(NODE_ID id);

    /**
     * Count visible elements.
     * 
     * @return number of currently visible elements.
     */
    int getVisibleCount();

    /**
     * Returns visible node list.
     * 
     * @return return the list of all visible nodes in the right sequence
     */
    List<NODE_ID> getVisibleList();

    /**
     * Registers observers with the manager.
     * 
     * @param observer observer
     */
    void registerDataSetObserver(final DataSetObserver observer);

    /**
     * Unregisters observers with the manager.
     * 
     * @param observer observer
     */
    void unregisterDataSetObserver(final DataSetObserver observer);

    /**
     * Cleans tree stored in manager. After this operation the tree is empty.
     */
    void clear();

    /**
     * Refreshes views connected to the manager.
     */
    void refresh();
}
