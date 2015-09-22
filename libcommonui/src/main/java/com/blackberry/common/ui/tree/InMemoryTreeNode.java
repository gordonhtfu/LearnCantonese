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

import java.io.Serializable;
import java.util.LinkedList;
import java.util.List;

/**
 * Node. It is package protected so that it cannot be used outside.
 * 
 * @param <NODE_ID> type of the identifier used by the tree
 */
class InMemoryTreeNode<NODE_ID> implements TreeNodeInfo<NODE_ID>, Serializable {
    private static final long serialVersionUID = 1L;
    private final NODE_ID mId;
    private final NODE_ID mParent;
    private final int mLevel;
    private boolean mVisible = true;
    private final List<InMemoryTreeNode<NODE_ID>> mChildren = new LinkedList<InMemoryTreeNode<NODE_ID>>();
    private List<NODE_ID> mChildIdListCache = null;

    public InMemoryTreeNode(final NODE_ID id, final NODE_ID parent, final int level,
            final boolean visible) {
        super();
        mId = id;
        mParent = parent;
        mLevel = level;
        mVisible = visible;
    }

    public int indexOf(final NODE_ID id) {
        return getChildIdList().indexOf(id);
    }

    /**
     * Cache is built lasily only if needed. The cache is cleaned on any structure change for that
     * node!).
     * 
     * @return list of ids of children
     */
    public synchronized List<NODE_ID> getChildIdList() {
        if (mChildIdListCache == null) {
            mChildIdListCache = new LinkedList<NODE_ID>();
            for (final InMemoryTreeNode<NODE_ID> n : mChildren) {
                mChildIdListCache.add(n.getId());
            }
        }
        return mChildIdListCache;
    }

    @Override
    public boolean isVisible() {
        return mVisible;
    }

    public void setVisible(final boolean visible) {
        mVisible = visible;
    }

    public int getChildrenListSize() {
        return mChildren.size();
    }

    public synchronized InMemoryTreeNode<NODE_ID> add(final int index, final NODE_ID child,
            final boolean visible) {
        mChildIdListCache = null;
        // Note! top levell children are always visible (!)
        final InMemoryTreeNode<NODE_ID> newNode = new InMemoryTreeNode<NODE_ID>(child,
                getId(), getLevel() + 1, getId() == null ? true : visible);
        mChildren.add(index, newNode);
        return newNode;
    }

    /**
     * Note. This method should technically return unmodifiable collection, but for performance
     * reason on small devices we do not do it.
     * 
     * @return children list
     */
    public List<InMemoryTreeNode<NODE_ID>> getChildren() {
        return mChildren;
    }

    public synchronized void clearChildren() {
        mChildren.clear();
        mChildIdListCache = null;
    }

    public synchronized void removeChild(final NODE_ID child) {
        final int childIndex = indexOf(child);
        if (childIndex != -1) {
            mChildren.remove(childIndex);
            mChildIdListCache = null;
        }
    }

    @Override
    public String toString() {
        return "InMemoryTreeNode [id=" + getId() + ", parent=" + getParent()
                + ", level=" + getLevel() + ", visible=" + mVisible
                + ", children=" + mChildren + ", childIdListCache="
                + mChildIdListCache + "]";
    }

    @Override
    public NODE_ID getId() {
        return mId;
    }

    @Override
    public NODE_ID getParent() {
        return mParent;
    }

    @Override
    public int getLevel() {
        return mLevel;
    }

    @Override
    public boolean isWithChildren() {
        return !mChildren.isEmpty();
    }

    @Override
    public boolean isExpanded() {
        boolean expanded = false;
        if (!mChildren.isEmpty() && mChildren.get(0).isVisible()) {
            expanded = true;
        }
        return expanded;
    }

}
