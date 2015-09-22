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

/**
 * Information about the node.
 * 
 * @param <NODE_ID> type of the identifier used by the tree
 */
public interface TreeNodeInfo<NODE_ID> {

    /**
     * Get the ID of the node.
     * 
     * @return the ID of the node
     */
    NODE_ID getId();

    /**
     * Get the ID of the node's parent.
     * 
     * @return the ID of the node's parent
     */
    NODE_ID getParent();

    /**
     * Check if the node has children.
     * 
     * @return true if node has children, false otherwise.
     */
    boolean isWithChildren();

    /**
     * Check if the node is visible.
     * 
     * @return true if node is visible, false otherwise.
     */
    boolean isVisible();

    /**
     * Check if the node is expanded.
     * 
     * @return true if node is expanded, false otherwise.
     */
    boolean isExpanded();

    /**
     * Get the level of the node.
     * 
     * @return the level of the node
     */
    int getLevel();
}
