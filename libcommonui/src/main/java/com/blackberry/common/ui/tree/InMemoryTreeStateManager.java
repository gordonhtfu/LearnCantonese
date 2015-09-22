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

import android.database.DataSetObservable;
import android.database.DataSetObserver;
import android.util.Log;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * In-memory manager of tree state.
 * 
 * @param <NODE_ID> type of the identifier used by the tree
 */
public class InMemoryTreeStateManager<NODE_ID> implements TreeStateManager<NODE_ID> {
    private static final String TAG = InMemoryTreeStateManager.class.getSimpleName();
    private static final long serialVersionUID = 1L;
    private final Map<NODE_ID, InMemoryTreeNode<NODE_ID>> mAllNodes =
            new HashMap<NODE_ID, InMemoryTreeNode<NODE_ID>>();
    private final InMemoryTreeNode<NODE_ID> mTopSentinel = new InMemoryTreeNode<NODE_ID>(
            null, null, -1, true);
    private transient List<NODE_ID> mVisibleListCache = null; // lasy initialised
    private transient List<NODE_ID> mUnmodifiableVisibleList = null;
    private boolean mVisibleByDefault = true;
    private final transient DataSetObservable mDataSetObservable = new DataSetObservable();

    private synchronized void internalDataSetChanged() {
        mVisibleListCache = null;
        mUnmodifiableVisibleList = null;
        mDataSetObservable.notifyChanged();
    }

    /**
     * If true new nodes are visible by default.
     * 
     * @param visibleByDefault if true, then newly added nodes are expanded by default
     */
    public void setVisibleByDefault(final boolean visibleByDefault) {
        this.mVisibleByDefault = visibleByDefault;
    }

    private InMemoryTreeNode<NODE_ID> getNodeFromTreeOrThrow(final NODE_ID id) {
        if (id == null) {
            throw new NodeNotInTreeException("(null)");
        }
        final InMemoryTreeNode<NODE_ID> node = mAllNodes.get(id);
        if (node == null) {
            throw new NodeNotInTreeException(id.toString());
        }
        return node;
    }

    private InMemoryTreeNode<NODE_ID> getNodeFromTreeOrThrowAllowRoot(final NODE_ID id) {
        if (id == null) {
            return mTopSentinel;
        }
        return getNodeFromTreeOrThrow(id);
    }

    private void expectNodeNotInTreeYet(final NODE_ID id) {
        final InMemoryTreeNode<NODE_ID> node = mAllNodes.get(id);
        if (node != null) {
            throw new NodeAlreadyInTreeException(id.toString(), node.toString());
        }
    }

    @Override
    public synchronized TreeNodeInfo<NODE_ID> getNodeInfo(final NODE_ID id) {
        return getNodeFromTreeOrThrow(id);
    }

    @Override
    public synchronized List<NODE_ID> getChildren(final NODE_ID id) {
        final InMemoryTreeNode<NODE_ID> node = getNodeFromTreeOrThrowAllowRoot(id);
        return node.getChildIdList();
    }

    @Override
    public synchronized NODE_ID getParent(final NODE_ID id) {
        final InMemoryTreeNode<NODE_ID> node = getNodeFromTreeOrThrowAllowRoot(id);
        return node.getParent();
    }

    private boolean getChildrenVisibility(final InMemoryTreeNode<NODE_ID> node) {
        boolean visibility;
        final List<InMemoryTreeNode<NODE_ID>> children = node.getChildren();
        if (children.isEmpty()) {
            visibility = mVisibleByDefault;
        } else {
            visibility = children.get(0).isVisible();
        }
        return visibility;
    }

    @Override
    public synchronized void addBeforeChild(final NODE_ID parent, final NODE_ID newChild,
            final NODE_ID beforeChild) {
        expectNodeNotInTreeYet(newChild);
        final InMemoryTreeNode<NODE_ID> node = getNodeFromTreeOrThrowAllowRoot(parent);
        final boolean visibility = getChildrenVisibility(node);
        // top nodes are always expanded.
        if (beforeChild == null) {
            final InMemoryTreeNode<NODE_ID> added = node.add(0, newChild, visibility);
            mAllNodes.put(newChild, added);
        } else {
            final int index = node.indexOf(beforeChild);
            final InMemoryTreeNode<NODE_ID> added = node.add(index == -1 ? 0 : index,
                    newChild, visibility);
            mAllNodes.put(newChild, added);
        }
        if (visibility) {
            internalDataSetChanged();
        }
    }

    @Override
    public synchronized void addAfterChild(final NODE_ID parent, final NODE_ID newChild,
            final NODE_ID afterChild) {
        expectNodeNotInTreeYet(newChild);
        final InMemoryTreeNode<NODE_ID> node = getNodeFromTreeOrThrowAllowRoot(parent);
        final boolean visibility = getChildrenVisibility(node);
        if (afterChild == null) {
            final InMemoryTreeNode<NODE_ID> added = node.add(
                    node.getChildrenListSize(), newChild, visibility);
            mAllNodes.put(newChild, added);
        } else {
            final int index = node.indexOf(afterChild);
            final InMemoryTreeNode<NODE_ID> added = node.add(
                    index == -1 ? node.getChildrenListSize() : index + 1, newChild,
                    visibility);
            mAllNodes.put(newChild, added);
        }
        if (visibility) {
            internalDataSetChanged();
        }
    }

    @Override
    public synchronized ArrayList<NODE_ID> removeNodeRecursively(final NODE_ID id) {
        final InMemoryTreeNode<NODE_ID> node = getNodeFromTreeOrThrowAllowRoot(id);
        final ArrayList<NODE_ID> removedNodes = new ArrayList<NODE_ID>();
        final boolean visibleNodeChanged = removeNodeRecursively(node, removedNodes);
        final NODE_ID parent = node.getParent();
        final InMemoryTreeNode<NODE_ID> parentNode = getNodeFromTreeOrThrowAllowRoot(parent);
        parentNode.removeChild(id);
        if (visibleNodeChanged) {
            internalDataSetChanged();
        }
        return removedNodes;
    }

    private boolean removeNodeRecursively(final InMemoryTreeNode<NODE_ID> node,
            ArrayList<NODE_ID> removedNodes) {
        boolean visibleNodeChanged = false;
        for (final InMemoryTreeNode<NODE_ID> child : node.getChildren()) {
            if (removeNodeRecursively(child, removedNodes)) {
                visibleNodeChanged = true;
            }
        }
        node.clearChildren();
        if (node.getId() != null) {
            mAllNodes.remove(node.getId());
            removedNodes.add(node.getId());
            if (node.isVisible()) {
                visibleNodeChanged = true;
            }
        }
        return visibleNodeChanged;
    }

    private void setChildrenVisibility(final InMemoryTreeNode<NODE_ID> node,
            final boolean visible, final boolean recursive) {
        for (final InMemoryTreeNode<NODE_ID> child : node.getChildren()) {
            child.setVisible(visible);
            if (recursive) {
                setChildrenVisibility(child, visible, true);
            }
        }
    }

    @Override
    public synchronized void expandDirectChildren(final NODE_ID id) {
        Log.d(TAG, "Expanding direct children of " + id);
        final InMemoryTreeNode<NODE_ID> node = getNodeFromTreeOrThrowAllowRoot(id);
        setChildrenVisibility(node, true, false);
        internalDataSetChanged();
    }

    @Override
    public synchronized void expandEverythingBelow(final NODE_ID id) {
        Log.d(TAG, "Expanding all children below " + id);
        final InMemoryTreeNode<NODE_ID> node = getNodeFromTreeOrThrowAllowRoot(id);
        setChildrenVisibility(node, true, true);
        internalDataSetChanged();
    }

    @Override
    public synchronized void collapseChildren(final NODE_ID id) {
        final InMemoryTreeNode<NODE_ID> node = getNodeFromTreeOrThrowAllowRoot(id);
        if (node == mTopSentinel) {
            for (final InMemoryTreeNode<NODE_ID> n : mTopSentinel.getChildren()) {
                setChildrenVisibility(n, false, true);
            }
        } else {
            setChildrenVisibility(node, false, true);
        }
        internalDataSetChanged();
    }

    @Override
    public synchronized NODE_ID getNextSibling(final NODE_ID id) {
        final NODE_ID parent = getParent(id);
        final InMemoryTreeNode<NODE_ID> parentNode = getNodeFromTreeOrThrowAllowRoot(parent);
        boolean returnNext = false;
        for (final InMemoryTreeNode<NODE_ID> child : parentNode.getChildren()) {
            if (returnNext) {
                return child.getId();
            }
            if (child.getId().equals(id)) {
                returnNext = true;
            }
        }
        return null;
    }

    @Override
    public synchronized NODE_ID getPreviousSibling(final NODE_ID id) {
        final NODE_ID parent = getParent(id);
        final InMemoryTreeNode<NODE_ID> parentNode = getNodeFromTreeOrThrowAllowRoot(parent);
        NODE_ID previousSibling = null;
        for (final InMemoryTreeNode<NODE_ID> child : parentNode.getChildren()) {
            if (child.getId().equals(id)) {
                return previousSibling;
            }
            previousSibling = child.getId();
        }
        return null;
    }

    @Override
    public synchronized boolean isInTree(final NODE_ID id) {
        return mAllNodes.containsKey(id);
    }

    @Override
    public synchronized int getVisibleCount() {
        return getVisibleList().size();
    }

    @Override
    public synchronized List<NODE_ID> getVisibleList() {
        NODE_ID currentId = null;
        if (mVisibleListCache == null) {
            mVisibleListCache = new ArrayList<NODE_ID>(mAllNodes.size());
            do {
                currentId = getNextVisible(currentId);
                if (currentId == null) {
                    break;
                } else {
                    mVisibleListCache.add(currentId);
                }
            } while (true);
        }
        if (mUnmodifiableVisibleList == null) {
            mUnmodifiableVisibleList = Collections
                    .unmodifiableList(mVisibleListCache);
        }
        return mUnmodifiableVisibleList;
    }

    public synchronized NODE_ID getNextVisible(final NODE_ID id) {
        final InMemoryTreeNode<NODE_ID> node = getNodeFromTreeOrThrowAllowRoot(id);
        if (!node.isVisible()) {
            return null;
        }
        final List<InMemoryTreeNode<NODE_ID>> children = node.getChildren();
        if (!children.isEmpty()) {
            final InMemoryTreeNode<NODE_ID> firstChild = children.get(0);
            if (firstChild.isVisible()) {
                return firstChild.getId();
            }
        }
        final NODE_ID sibl = getNextSibling(id);
        if (sibl != null) {
            return sibl;
        }
        NODE_ID parent = node.getParent();
        do {
            if (parent == null) {
                return null;
            }
            final NODE_ID parentSibling = getNextSibling(parent);
            if (parentSibling != null) {
                return parentSibling;
            }
            parent = getNodeFromTreeOrThrow(parent).getParent();
        } while (true);
    }

    @Override
    public synchronized void registerDataSetObserver(final DataSetObserver observer) {
        mDataSetObservable.registerObserver(observer);
    }

    @Override
    public synchronized void unregisterDataSetObserver(final DataSetObserver observer) {
        mDataSetObservable.unregisterObserver(observer);
    }

    @Override
    public int getLevel(final NODE_ID id) {
        return getNodeFromTreeOrThrow(id).getLevel();
    }

    @Override
    public Integer[] getHierarchyDescription(final NODE_ID id) {
        final int level = getLevel(id);
        final Integer[] hierarchy = new Integer[level + 1];
        int currentLevel = level;
        NODE_ID currentId = id;
        NODE_ID parent = getParent(currentId);
        while (currentLevel >= 0) {
            hierarchy[currentLevel--] = getChildren(parent).indexOf(currentId);
            currentId = parent;
            parent = getParent(parent);
        }
        return hierarchy;
    }

    private void appendToSb(final StringBuilder sb, final NODE_ID id) {
        if (id != null) {
            final TreeNodeInfo<NODE_ID> node = getNodeInfo(id);
            final int indent = node.getLevel() * 4;
            final char[] indentString = new char[indent];
            Arrays.fill(indentString, ' ');
            sb.append(indentString);
            sb.append(node.toString());
            sb.append(Arrays.asList(getHierarchyDescription(id)).toString());
            sb.append("\n");
        }
        final List<NODE_ID> children = getChildren(id);
        for (final NODE_ID child : children) {
            appendToSb(sb, child);
        }
    }

    @Override
    public synchronized String toString() {
        final StringBuilder sb = new StringBuilder();
        appendToSb(sb, null);
        return sb.toString();
    }

    @Override
    public synchronized void clear() {
        mAllNodes.clear();
        mTopSentinel.clearChildren();
        internalDataSetChanged();
    }

    @Override
    public void refresh() {
        internalDataSetChanged();
    }

}
