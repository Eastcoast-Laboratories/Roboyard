/*  DriftingDroids - yet another Ricochet Robots solver program.
    Copyright (C) 2011-2014 Michael Henke

    This program is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with this program.  If not, see <http://www.gnu.org/licenses/>.
*/

package driftingdroids.model;

import java.util.Arrays;



/**
 * This class is a minimal <code>Map&ltK,V&gt</code> implementation for primitive
 * <code>int</code> or <code>long</code> keys K and <tt>byte</tt> values V,
 *  based on a trie (prefix tree) data structure.
 * <p>
 * The aim is to balance a fast recognition of duplicate keys
 * and a compact storage of data.
 * <p>
 * This class is called "special" because it's specialized to work with keys
 * that are generated by the <code>KeyMakerInt</code> or <code>KeyMakerLong</code>
 * classes, only. That means that it relies on the specific properties of these keys,
 * which are:<br>
 * * keys consist of N elements: the positions of the N robots on the board<br>
 * * all elements 1...N of a key are unique: no two robots are on the same position<br>
 * * elements 1...N-1 of a key are sorted: non-goal robots can be substituted for each other<br>
 *
 */
public class KeyDepthMapTrieSpecial implements KeyDepthMap {

    protected static final int NODE_ARRAY_SHIFT = 20; // 20 == 4MB
    protected static final int NODE_ARRAY_SIZE = 1 << NODE_ARRAY_SHIFT;
    protected static final int NODE_ARRAY_MASK = NODE_ARRAY_SIZE - 1;
    protected final int[] rootNode;
    protected int[][] nodeArrays;
    protected int numNodeArrays, nextNode, nextNodeArray;

    protected static final int LEAF_ARRAY_SHIFT = 20; // 20 == 1MB
    protected static final int LEAF_ARRAY_SIZE = 1 << LEAF_ARRAY_SHIFT;
    protected static final int LEAF_ARRAY_MASK = LEAF_ARRAY_SIZE - 1;
    protected byte[][] leafArrays;
    protected int numLeafArrays, nextLeaf, nextLeafArray;

    protected final int nodeNumber, nodeNumberUnCompr, nodeShift, nodeMask;
    protected final int leafNodeShift, leafNodeMask, leafNodeSize, leafSize, leafMask;

    protected final int[] nodeSizeLookup;
    protected final int[] elementLookup;
    
    protected int size = 0;

    public static KeyDepthMapTrieSpecial createInstance(final Board board, final boolean useMoreMemoryForSpeedup) {
        if (useMoreMemoryForSpeedup && (8 == board.sizeNumBits) && ((4 == board.getNumRobots()) || (5 == board.getNumRobots()))) {
            return new KeyDepthMapTrieSpecial8Bit(board);
        } else {
            return new KeyDepthMapTrieSpecial(board);
        }
    }

    private KeyDepthMapTrieSpecial(final Board board) {
        this.nodeSizeLookup = new int[board.size];
        for (int i = 0;  i < this.nodeSizeLookup.length;  ++i) {
            this.nodeSizeLookup[i] = board.size - 1 - i;
        }
        this.elementLookup = new int[board.size];
        for (int i = 0;  i < this.elementLookup.length;  ++i) {
            this.elementLookup[i] = i;
        }
        for (int i = 0;  i < board.size;  ++i) {
            if (true == board.isObstacle(i)) {
                for (int j = 0;  j < i;  ++j) {
                    this.nodeSizeLookup[j] -= 1;
                }
                for (int j = i;  j < this.elementLookup.length;  ++j) {
                    this.elementLookup[j] -= 1;
                }
            }
        }
        for (int i = 0;  i < board.size;  ++i) {
            if (true == board.isObstacle(i)) {
                this.nodeSizeLookup[i] = Integer.MIN_VALUE;
                this.elementLookup[i] = Integer.MIN_VALUE;
            }
        }
        this.nodeNumber = board.getNumRobots() - 1;
        this.nodeNumberUnCompr = (board.getNumRobots()*board.sizeNumBits + 8 - 31 + (board.sizeNumBits - 1)) / board.sizeNumBits;
        this.nodeShift = board.sizeNumBits;
        this.nodeMask = (1 << board.sizeNumBits) - 1;

        this.nodeArrays = new int[4][];
        this.rootNode = new int[NODE_ARRAY_SIZE];
        this.nodeArrays[0] = this.rootNode;
        this.numNodeArrays = 1;
        this.nextNode = board.size;             //root node already exists
        this.nextNodeArray = NODE_ARRAY_SIZE;   //first array already exists

        this.leafNodeShift = board.sizeNumBits / 2;
        this.leafNodeMask = (1 << this.leafNodeShift) - 1;
        this.leafNodeSize = this.leafNodeMask + 1;
        this.leafSize = 1 << (board.sizeNumBits - this.leafNodeShift);
        this.leafMask = this.leafSize - 1;
        this.leafArrays = new byte[16][];
        this.numLeafArrays = 0;
        this.nextLeaf = this.leafSize;  //no leaves yet, but skip leaf "0" because this is the special value
        this.nextLeafArray = 0;         //no leaf arrays yet
    }


    /* (non-Javadoc)
     * @see driftingdroids.model.KeyDepthMap#putIfGreater(int, int)
     */
    @Override
    public boolean putIfGreater(int key, final int byteValue) {
        //root node
        int nidx = key & this.nodeMask;
        int[] nodeArray = this.rootNode;
        int elementThis = nidx;
        int elementThisLookup = this.elementLookup[nidx];
        //go through nodes (without compression because (key<<8)+value is greater than "int")
        int nodeIndex, i;   //used by both for() loops
        for (i = 1;  i < this.nodeNumberUnCompr;  ++i) {
            nodeIndex = nodeArray[nidx];
            key >>>= this.nodeShift;
            if (0 == nodeIndex) {
                //create a new node
                final int nodeSize = this.nodeSizeLookup[elementThis];
                if (this.nextNode + nodeSize > this.nextNodeArray) {
                    if (this.nodeArrays.length <= this.numNodeArrays) {
                        this.nodeArrays = Arrays.copyOf(this.nodeArrays, this.nodeArrays.length << 1);
                    }
                    this.nodeArrays[this.numNodeArrays++] = new int[NODE_ARRAY_SIZE];
                    this.nextNode = this.nextNodeArray;
                    this.nextNodeArray += NODE_ARRAY_SIZE;
                }
                nodeIndex = this.nextNode;
                this.nextNode += nodeSize;
                nodeArray[nidx] = nodeIndex;
            }
            elementThis = key & this.nodeMask;
            nidx = (nodeIndex & NODE_ARRAY_MASK) - elementThisLookup - 1;
            elementThisLookup = this.elementLookup[elementThis];
            nodeArray = this.nodeArrays[nodeIndex >>> NODE_ARRAY_SHIFT];
            nidx += elementThisLookup;
        }
        //go through nodes (with compression because (key<<8)+value is inside "int" range now)
        for ( ;  i < this.nodeNumber;  ++i) {
            nodeIndex = nodeArray[nidx];
            key >>>= this.nodeShift;
            if (0 == nodeIndex) {
                // -> node index is null = unused
                //write current key+value as a "compressed branch" (negative node index)
                //exit immediately because no further nodes and no leaf need to be stored
                nodeArray[nidx] = ((~key) << 8) | byteValue;    //negative
                return true;
            } else if (0 > nodeIndex) {
                // -> node index is negative = used by a single "compressed branch"
                final int prevKey = (~nodeIndex) >> 8;
                final int prevVal = 0xff & nodeIndex;
                //previous and current keys are equal (duplicate key)
                if (prevKey == key) {
                    if (byteValue > prevVal) {  //putIfGreater
                        nodeArray[nidx] = (nodeIndex ^ prevVal) | byteValue;    //negative
                        return true;
                    }
                    return false;
                }
                //previous and current keys are not equal
                //create a new node
                final int nodeSize = this.nodeSizeLookup[elementThis];
                if (this.nextNode + nodeSize > this.nextNodeArray) {
                    if (this.nodeArrays.length <= this.numNodeArrays) {
                        this.nodeArrays = Arrays.copyOf(this.nodeArrays, this.nodeArrays.length << 1);
                    }
                    this.nodeArrays[this.numNodeArrays++] = new int[NODE_ARRAY_SIZE];
                    this.nextNode = this.nextNodeArray;
                    this.nextNodeArray += NODE_ARRAY_SIZE;
                }
                nodeIndex = this.nextNode;
                this.nextNode += nodeSize;
                nodeArray[nidx] = nodeIndex;
                //push previous "compressed branch" one node further
                nodeArray = this.nodeArrays[nodeIndex >>> NODE_ARRAY_SHIFT];
                elementThis = key & this.nodeMask;
                nidx = (nodeIndex & NODE_ARRAY_MASK) - elementThisLookup - 1;
                elementThisLookup = this.elementLookup[elementThis];
                nodeArray[nidx + this.elementLookup[prevKey & this.nodeMask]] = (~(prevKey >>> this.nodeShift) << 8) | prevVal;
            } else {
                // -> node index is positive = go to next node
                elementThis = key & this.nodeMask;
                nidx = (nodeIndex & NODE_ARRAY_MASK) - elementThisLookup - 1;
                elementThisLookup = this.elementLookup[elementThis];
                nodeArray = this.nodeArrays[nodeIndex >>> NODE_ARRAY_SHIFT];
            }
            nidx += elementThisLookup;
        }
        //go through leaf node (with compression)
        nodeIndex = nodeArray[nidx];
        key >>>= this.nodeShift;
        if (0 == nodeIndex) {
            // -> node index is null = unused
            //write current key+value as a "compressed branch" (negative node index)
            //exit immediately because no further nodes and no leaf need to be stored
            nodeArray[nidx] = ((~key) << 8) | byteValue;    //negative
            return true;
        } else if (0 > nodeIndex) {
            // -> node index is negative = used by a single "compressed branch"
            final int prevKey = (~nodeIndex) >> 8;
            final int prevVal = 0xff & nodeIndex;
            //previous and current keys are equal (duplicate key)
            if (prevKey == key) {
                if (byteValue > prevVal) {  //putIfGreater
                    nodeArray[nidx] = (nodeIndex ^ prevVal) | byteValue;    //negative
                    return true;
                }
                return false;
            }
            //previous and current keys are not equal
            //create a new node
            if (this.nextNode + this.leafNodeSize > this.nextNodeArray) {
                if (this.nodeArrays.length <= this.numNodeArrays) {
                    this.nodeArrays = Arrays.copyOf(this.nodeArrays, this.nodeArrays.length << 1);
                }
                this.nodeArrays[this.numNodeArrays++] = new int[NODE_ARRAY_SIZE];
                this.nextNode = this.nextNodeArray;
                this.nextNodeArray += NODE_ARRAY_SIZE;
            }
            nodeIndex = this.nextNode;
            this.nextNode += this.leafNodeSize;
            nodeArray[nidx] = nodeIndex;
            //push previous "compressed branch" one node further
            nodeArray = this.nodeArrays[nodeIndex >>> NODE_ARRAY_SHIFT];
            nidx = (nodeIndex & NODE_ARRAY_MASK) + (prevKey & this.leafNodeMask);
            nodeArray[nidx] = (~(prevKey >>> this.leafNodeShift) << 8) | prevVal;    //negative
        } else {
            // -> node index is positive = go to next node
            nodeArray = this.nodeArrays[nodeIndex >>> NODE_ARRAY_SHIFT];
        }
        nidx = (nodeIndex & NODE_ARRAY_MASK) + (key & this.leafNodeMask);
        //get leaf (with compression)
        int leafIndex = nodeArray[nidx];
        key >>>= this.leafNodeShift;
        if (0 == leafIndex) {
            // -> leaf index is null = unused
            //write current value as a "compressed branch" (negative leaf index)
            //exit immediately because no leaf needs to be stored
            nodeArray[nidx] = ((~key) << 8) | byteValue;    //negative
            return true;
        } else if (0 > leafIndex) {
            // -> leaf index is negative = used by a single "compressed branch"
            final int prevKey = (~leafIndex) >> 8;
            final int prevVal = 0xff & leafIndex;
            //previous and current keys are equal (duplicate key)
            if (prevKey == key) {
                if (byteValue > prevVal) {  //putIfGreater
                    nodeArray[nidx] = (leafIndex ^ prevVal) | byteValue;    //negative
                    return true;
                }
                return false;
            }
            //previous and current keys are not equal
            //create a new leaf
            if (this.nextLeaf >= this.nextLeafArray) {
                if (this.leafArrays.length <= this.numLeafArrays) {
                    this.leafArrays = Arrays.copyOf(this.leafArrays, this.leafArrays.length << 1);
                }
                final byte[] newLeafArray = new byte[LEAF_ARRAY_SIZE];
                this.leafArrays[this.numLeafArrays++] = newLeafArray;
                this.nextLeafArray += LEAF_ARRAY_SIZE;
            }
            leafIndex = this.nextLeaf;
            this.nextLeaf += this.leafSize;
            nodeArray[nidx] = leafIndex;
            //push the previous "compressed branch" further to the leaf
            final int lidx = (leafIndex & LEAF_ARRAY_MASK) + (prevKey & this.leafMask);
            this.leafArrays[leafIndex >>> LEAF_ARRAY_SHIFT][lidx] = (byte)prevVal;
        }
        final byte[] leafArray = this.leafArrays[leafIndex >>> LEAF_ARRAY_SHIFT];
        final int lidx = (leafIndex & LEAF_ARRAY_MASK) + (key & this.leafMask);
        final byte prevVal = leafArray[lidx];
        if (byteValue > (0xff & prevVal)) {  //putIfGreater
            leafArray[lidx] = (byte)byteValue;
            return true;
        }
        return false;
    }


    /* (non-Javadoc)
     * @see driftingdroids.model.KeyDepthMap#putIfGreater(long, int)
     */
    @Override
    public boolean putIfGreater(long key, final int byteValue) {
        //this method is copy&paste from putIfGreater(int,int) with only a few (int) casts added where required.
        //root node
        int nidx = (int)key & this.nodeMask;
        int[] nodeArray = this.rootNode;
        int elementThis = nidx;
        int elementThisLookup = this.elementLookup[nidx];
        //go through nodes (without compression because (key<<8)+value is greater than "int")
        int nodeIndex, i;   //used by both for() loops
        for (i = 1;  i < this.nodeNumberUnCompr;  ++i) {
            nodeIndex = nodeArray[nidx];
            key >>>= this.nodeShift;
            if (0 == nodeIndex) {
                //create a new node
                final int nodeSize = this.nodeSizeLookup[elementThis];
                if (this.nextNode + nodeSize > this.nextNodeArray) {
                    if (this.nodeArrays.length <= this.numNodeArrays) {
                        this.nodeArrays = Arrays.copyOf(this.nodeArrays, this.nodeArrays.length << 1);
                    }
                    this.nodeArrays[this.numNodeArrays++] = new int[NODE_ARRAY_SIZE];
                    this.nextNode = this.nextNodeArray;
                    this.nextNodeArray += NODE_ARRAY_SIZE;
                }
                nodeIndex = this.nextNode;
                this.nextNode += nodeSize;
                nodeArray[nidx] = nodeIndex;
            }
            elementThis = (int)key & this.nodeMask;
            nidx = (nodeIndex & NODE_ARRAY_MASK) - elementThisLookup - 1;
            elementThisLookup = this.elementLookup[elementThis];
            nodeArray = this.nodeArrays[nodeIndex >>> NODE_ARRAY_SHIFT];
            nidx += elementThisLookup;
        }
        //go through nodes (with compression because (key<<8)+value is inside "int" range now)
        for ( ;  i < this.nodeNumber;  ++i) {
            nodeIndex = nodeArray[nidx];
            key >>>= this.nodeShift;
            if (0 == nodeIndex) {
                // -> node index is null = unused
                //write current key+value as a "compressed branch" (negative node index)
                //exit immediately because no further nodes and no leaf need to be stored
                nodeArray[nidx] = ((~(int)key) << 8) | byteValue;    //negative
                return true;
            } else if (0 > nodeIndex) {
                // -> node index is negative = used by a single "compressed branch"
                final int prevKey = (~nodeIndex) >> 8;
                final int prevVal = 0xff & nodeIndex;
                //previous and current keys are equal (duplicate key)
                if (prevKey == (int)key) {
                    if (byteValue > prevVal) {  //putIfGreater
                        nodeArray[nidx] = (nodeIndex ^ prevVal) | byteValue;    //negative
                        return true;
                    }
                    return false;
                }
                //previous and current keys are not equal
                //create a new node
                final int nodeSize = this.nodeSizeLookup[elementThis];
                if (this.nextNode + nodeSize > this.nextNodeArray) {
                    if (this.nodeArrays.length <= this.numNodeArrays) {
                        this.nodeArrays = Arrays.copyOf(this.nodeArrays, this.nodeArrays.length << 1);
                    }
                    this.nodeArrays[this.numNodeArrays++] = new int[NODE_ARRAY_SIZE];
                    this.nextNode = this.nextNodeArray;
                    this.nextNodeArray += NODE_ARRAY_SIZE;
                }
                nodeIndex = this.nextNode;
                this.nextNode += nodeSize;
                nodeArray[nidx] = nodeIndex;
                //push previous "compressed branch" one node further
                nodeArray = this.nodeArrays[nodeIndex >>> NODE_ARRAY_SHIFT];
                elementThis = (int)key & this.nodeMask;
                nidx = (nodeIndex & NODE_ARRAY_MASK) - elementThisLookup - 1;
                elementThisLookup = this.elementLookup[elementThis];
                nodeArray[nidx + this.elementLookup[prevKey & this.nodeMask]] = (~(prevKey >>> this.nodeShift) << 8) | prevVal;
            } else {
                // -> node index is positive = go to next node
                elementThis = (int)key & this.nodeMask;
                nidx = (nodeIndex & NODE_ARRAY_MASK) - elementThisLookup - 1;
                elementThisLookup = this.elementLookup[elementThis];
                nodeArray = this.nodeArrays[nodeIndex >>> NODE_ARRAY_SHIFT];
            }
            nidx += elementThisLookup;
        }
        //go through leaf node (with compression)
        nodeIndex = nodeArray[nidx];
        key >>>= this.nodeShift;
        if (0 == nodeIndex) {
            // -> node index is null = unused
            //write current key+value as a "compressed branch" (negative node index)
            //exit immediately because no further nodes and no leaf need to be stored
            nodeArray[nidx] = ((~(int)key) << 8) | byteValue;    //negative
            return true;
        } else if (0 > nodeIndex) {
            // -> node index is negative = used by a single "compressed branch"
            final int prevKey = (~nodeIndex) >> 8;
            final int prevVal = 0xff & nodeIndex;
            //previous and current keys are equal (duplicate key)
            if (prevKey == (int)key) {
                if (byteValue > prevVal) {  //putIfGreater
                    nodeArray[nidx] = (nodeIndex ^ prevVal) | byteValue;    //negative
                    return true;
                }
                return false;
            }
            //previous and current keys are not equal
            //create a new node
            if (this.nextNode + this.leafNodeSize > this.nextNodeArray) {
                if (this.nodeArrays.length <= this.numNodeArrays) {
                    this.nodeArrays = Arrays.copyOf(this.nodeArrays, this.nodeArrays.length << 1);
                }
                this.nodeArrays[this.numNodeArrays++] = new int[NODE_ARRAY_SIZE];
                this.nextNode = this.nextNodeArray;
                this.nextNodeArray += NODE_ARRAY_SIZE;
            }
            nodeIndex = this.nextNode;
            this.nextNode += this.leafNodeSize;
            nodeArray[nidx] = nodeIndex;
            //push previous "compressed branch" one node further
            nodeArray = this.nodeArrays[nodeIndex >>> NODE_ARRAY_SHIFT];
            nidx = (nodeIndex & NODE_ARRAY_MASK) + (prevKey & this.leafNodeMask);
            nodeArray[nidx] = (~(prevKey >>> this.leafNodeShift) << 8) | prevVal;    //negative
        } else {
            // -> node index is positive = go to next node
            nodeArray = this.nodeArrays[nodeIndex >>> NODE_ARRAY_SHIFT];
        }
        nidx = (nodeIndex & NODE_ARRAY_MASK) + ((int)key & this.leafNodeMask);
        //get leaf (with compression)
        int leafIndex = nodeArray[nidx];
        key >>>= this.leafNodeShift;
        if (0 == leafIndex) {
            // -> leaf index is null = unused
            //write current value as a "compressed branch" (negative leaf index)
            //exit immediately because no leaf needs to be stored
            nodeArray[nidx] = ((~(int)key) << 8) | byteValue;    //negative
            return true;
        } else if (0 > leafIndex) {
            // -> leaf index is negative = used by a single "compressed branch"
            final int prevKey = (~leafIndex) >> 8;
            final int prevVal = 0xff & leafIndex;
            //previous and current keys are equal (duplicate key)
            if (prevKey == (int)key) {
                if (byteValue > prevVal) {  //putIfGreater
                    nodeArray[nidx] = (leafIndex ^ prevVal) | byteValue;    //negative
                    return true;
                }
                return false;
            }
            //previous and current keys are not equal
            //create a new leaf
            if (this.nextLeaf >= this.nextLeafArray) {
                if (this.leafArrays.length <= this.numLeafArrays) {
                    this.leafArrays = Arrays.copyOf(this.leafArrays, this.leafArrays.length << 1);
                }
                final byte[] newLeafArray = new byte[LEAF_ARRAY_SIZE];
                this.leafArrays[this.numLeafArrays++] = newLeafArray;
                this.nextLeafArray += LEAF_ARRAY_SIZE;
            }
            leafIndex = this.nextLeaf;
            this.nextLeaf += this.leafSize;
            nodeArray[nidx] = leafIndex;
            //push the previous "compressed branch" further to the leaf
            final int lidx = (leafIndex & LEAF_ARRAY_MASK) + (prevKey & this.leafMask);
            this.leafArrays[leafIndex >>> LEAF_ARRAY_SHIFT][lidx] = (byte)prevVal;
        }
        final byte[] leafArray = this.leafArrays[leafIndex >>> LEAF_ARRAY_SHIFT];
        final int lidx = (leafIndex & LEAF_ARRAY_MASK) + ((int)key & this.leafMask);
        final byte prevVal = leafArray[lidx];
        if (byteValue > (0xff & prevVal)) {  //putIfGreater
            leafArray[lidx] = (byte)byteValue;
            return true;
        }
        return false;
    }


    /* (non-Javadoc)
     * @see driftingdroids.model.KeyDepthMap#allocatedBytes()
     */
    @Override
    public long allocatedBytes() {
        long result = (this.nodeArrays.length + this.leafArrays.length) * 8;
        for (int i = 0;  i < this.numNodeArrays;  ++i) {
            result += this.nodeArrays[i].length * 4;
        }
        for (int i = 0;  i < this.numLeafArrays;  ++i) {
            result += this.leafArrays[i].length * 1;
        }
        return result;
    }


    /* (non-Javadoc)
     * @see driftingdroids.model.KeyDepthMap#size()
     */
    @Override
    public int size() {
        return this.size;
    }


    /**
     * this class is a further specialization which trades some more memory for a speedup.
     * it can only be used for board size of 8 bits (256 == 16x16) and with 4 or 5 robots.
     */
    private static class KeyDepthMapTrieSpecial8Bit extends KeyDepthMapTrieSpecial {

        private static final int LOOKUP_SHIFT = 3 * 8;
        private static final int LOOKUP_SHIFT_2 = 2 * 8;
        private static final int LOOKUP_MASK = (1 << LOOKUP_SHIFT) - 1;
        private final int[] lookupArray;

        private KeyDepthMapTrieSpecial8Bit(final Board board) {
            super(board);
            this.lookupArray = new int[LOOKUP_MASK + 1]; // 64 MiB
        }

        @Override
        public boolean putIfGreater(int key, int byteValue) { // for 4 robots
            int nidx = key & LOOKUP_MASK;
            int nodeIndex = this.lookupArray[nidx];
            int[] nodeArray;
            key >>>= LOOKUP_SHIFT;
            if (0 == nodeIndex) {
                // -> node index is null = unused
                //write current key+value as a "compressed branch" (negative node index)
                //exit immediately because no further nodes and no leaf need to be stored
                this.lookupArray[nidx] = ((~key) << 8) | byteValue;    //negative
                return true;
            } else if (0 > nodeIndex) {
                // -> node index is negative = used by a single "compressed branch"
                final int prevKey = (~nodeIndex) >> 8;
                final int prevVal = 0xff & nodeIndex;
                //previous and current keys are equal (duplicate key)
                if (prevKey == key) {
                    if (byteValue > prevVal) {  //putIfGreater
                        this.lookupArray[nidx] = (nodeIndex ^ prevVal) | byteValue;    //negative
                        return true;
                    }
                    return false;
                }
                //previous and current keys are not equal
                //create a new node
                if (this.nextNode + this.leafNodeSize > this.nextNodeArray) {
                    if (this.nodeArrays.length <= this.numNodeArrays) {
                        this.nodeArrays = Arrays.copyOf(this.nodeArrays, this.nodeArrays.length << 1);
                    }
                    this.nodeArrays[this.numNodeArrays++] = new int[NODE_ARRAY_SIZE];
                    this.nextNode = this.nextNodeArray;
                    this.nextNodeArray += NODE_ARRAY_SIZE;
                }
                nodeIndex = this.nextNode;
                this.nextNode += this.leafNodeSize;
                this.lookupArray[nidx] = nodeIndex;
                //push previous "compressed branch" one node further
                nodeArray = this.nodeArrays[nodeIndex >>> NODE_ARRAY_SHIFT];
                nidx = (nodeIndex & NODE_ARRAY_MASK) + (prevKey & this.leafNodeMask);
                nodeArray[nidx] = (~(prevKey >>> this.leafNodeShift) << 8) | prevVal;    //negative
            } else {
                // -> node index is positive = go to next node
                nodeArray = this.nodeArrays[nodeIndex >>> NODE_ARRAY_SHIFT];
            }
            nidx = (nodeIndex & NODE_ARRAY_MASK) + (key & this.leafNodeMask);
            //get leaf (with compression)
            int leafIndex = nodeArray[nidx];
            key >>>= this.leafNodeShift;
            if (0 == leafIndex) {
                // -> leaf index is null = unused
                //write current value as a "compressed branch" (negative leaf index)
                //exit immediately because no leaf needs to be stored
                nodeArray[nidx] = ((~key) << 8) | byteValue;    //negative
                return true;
            } else if (0 > leafIndex) {
                // -> leaf index is negative = used by a single "compressed branch"
                final int prevKey = (~leafIndex) >> 8;
                final int prevVal = 0xff & leafIndex;
                //previous and current keys are equal (duplicate key)
                if (prevKey == key) {
                    if (byteValue > prevVal) {  //putIfGreater
                        nodeArray[nidx] = (leafIndex ^ prevVal) | byteValue;    //negative
                        return true;
                    }
                    return false;
                }
                //previous and current keys are not equal
                //create a new leaf
                if (this.nextLeaf >= this.nextLeafArray) {
                    if (this.leafArrays.length <= this.numLeafArrays) {
                        this.leafArrays = Arrays.copyOf(this.leafArrays, this.leafArrays.length << 1);
                    }
                    final byte[] newLeafArray = new byte[LEAF_ARRAY_SIZE];
                    this.leafArrays[this.numLeafArrays++] = newLeafArray;
                    this.nextLeafArray += LEAF_ARRAY_SIZE;
                }
                leafIndex = this.nextLeaf;
                this.nextLeaf += this.leafSize;
                nodeArray[nidx] = leafIndex;
                //push the previous "compressed branch" further to the leaf
                final int lidx = (leafIndex & LEAF_ARRAY_MASK) + (prevKey & this.leafMask);
                this.leafArrays[leafIndex >>> LEAF_ARRAY_SHIFT][lidx] = (byte)prevVal;
            }
            final byte[] leafArray = this.leafArrays[leafIndex >>> LEAF_ARRAY_SHIFT];
            final int lidx = (leafIndex & LEAF_ARRAY_MASK) + (key & this.leafMask);
            final byte prevVal = leafArray[lidx];
            if (byteValue > (0xff & prevVal)) {  //putIfGreater
                leafArray[lidx] = (byte)byteValue;
                return true;
            }
            return false;
        }

        @Override
        public boolean putIfGreater(long key, int byteValue) { // for 5 robots
            int nidx = (int)key & LOOKUP_MASK;
            int nodeIndex = this.lookupArray[nidx];
            int[] nodeArray;
            key >>>= LOOKUP_SHIFT;
            int elementThis = (nidx >>> LOOKUP_SHIFT_2);
            int elementThisLookup = this.elementLookup[elementThis];
            //go through nodes (with compression because (key<<8)+value is inside "int" range now)
            if (0 == nodeIndex) {
                // -> node index is null = unused
                //write current key+value as a "compressed branch" (negative node index)
                //exit immediately because no further nodes and no leaf need to be stored
                this.lookupArray[nidx] = ((~(int)key) << 8) | byteValue;    //negative
                return true;
            } else if (0 > nodeIndex) {
                // -> node index is negative = used by a single "compressed branch"
                final int prevKey = (~nodeIndex) >> 8;
                final int prevVal = 0xff & nodeIndex;
                //previous and current keys are equal (duplicate key)
                if (prevKey == (int)key) {
                    if (byteValue > prevVal) {  //putIfGreater
                        this.lookupArray[nidx] = (nodeIndex ^ prevVal) | byteValue;    //negative
                        return true;
                    }
                    return false;
                }
                //previous and current keys are not equal
                //create a new node
                final int nodeSize = this.nodeSizeLookup[elementThis];
                if (this.nextNode + nodeSize > this.nextNodeArray) {
                    if (this.nodeArrays.length <= this.numNodeArrays) {
                        this.nodeArrays = Arrays.copyOf(this.nodeArrays, this.nodeArrays.length << 1);
                    }
                    this.nodeArrays[this.numNodeArrays++] = new int[NODE_ARRAY_SIZE];
                    this.nextNode = this.nextNodeArray;
                    this.nextNodeArray += NODE_ARRAY_SIZE;
                }
                nodeIndex = this.nextNode;
                this.nextNode += nodeSize;
                this.lookupArray[nidx] = nodeIndex;
                //push previous "compressed branch" one node further
                nodeArray = this.nodeArrays[nodeIndex >>> NODE_ARRAY_SHIFT];
                elementThis = (int)key & this.nodeMask;
                nidx = (nodeIndex & NODE_ARRAY_MASK) - elementThisLookup - 1;
                elementThisLookup = this.elementLookup[elementThis];
                nodeArray[nidx + this.elementLookup[prevKey & this.nodeMask]] = (~(prevKey >>> this.nodeShift) << 8) | prevVal;
            } else {
                // -> node index is positive = go to next node
                elementThis = (int)key & this.nodeMask;
                nidx = (nodeIndex & NODE_ARRAY_MASK) - elementThisLookup - 1;
                elementThisLookup = this.elementLookup[elementThis];
                nodeArray = this.nodeArrays[nodeIndex >>> NODE_ARRAY_SHIFT];
            }
            nidx += elementThisLookup;
            //go through leaf node (with compression)
            nodeIndex = nodeArray[nidx];
            key >>>= this.nodeShift;
            if (0 == nodeIndex) {
                // -> node index is null = unused
                //write current key+value as a "compressed branch" (negative node index)
                //exit immediately because no further nodes and no leaf need to be stored
                nodeArray[nidx] = ((~(int)key) << 8) | byteValue;    //negative
                return true;
            } else if (0 > nodeIndex) {
                // -> node index is negative = used by a single "compressed branch"
                final int prevKey = (~nodeIndex) >> 8;
                final int prevVal = 0xff & nodeIndex;
                //previous and current keys are equal (duplicate key)
                if (prevKey == (int)key) {
                    if (byteValue > prevVal) {  //putIfGreater
                        nodeArray[nidx] = (nodeIndex ^ prevVal) | byteValue;    //negative
                        return true;
                    }
                    return false;
                }
                //previous and current keys are not equal
                //create a new node
                if (this.nextNode + this.leafNodeSize > this.nextNodeArray) {
                    if (this.nodeArrays.length <= this.numNodeArrays) {
                        this.nodeArrays = Arrays.copyOf(this.nodeArrays, this.nodeArrays.length << 1);
                    }
                    this.nodeArrays[this.numNodeArrays++] = new int[NODE_ARRAY_SIZE];
                    this.nextNode = this.nextNodeArray;
                    this.nextNodeArray += NODE_ARRAY_SIZE;
                }
                nodeIndex = this.nextNode;
                this.nextNode += this.leafNodeSize;
                nodeArray[nidx] = nodeIndex;
                //push previous "compressed branch" one node further
                nodeArray = this.nodeArrays[nodeIndex >>> NODE_ARRAY_SHIFT];
                nidx = (nodeIndex & NODE_ARRAY_MASK) + (prevKey & this.leafNodeMask);
                nodeArray[nidx] = (~(prevKey >>> this.leafNodeShift) << 8) | prevVal;    //negative
            } else {
                // -> node index is positive = go to next node
                nodeArray = this.nodeArrays[nodeIndex >>> NODE_ARRAY_SHIFT];
            }
            nidx = (nodeIndex & NODE_ARRAY_MASK) + ((int)key & this.leafNodeMask);
            //get leaf (with compression)
            int leafIndex = nodeArray[nidx];
            key >>>= this.leafNodeShift;
            if (0 == leafIndex) {
                // -> leaf index is null = unused
                //write current value as a "compressed branch" (negative leaf index)
                //exit immediately because no leaf needs to be stored
                nodeArray[nidx] = ((~(int)key) << 8) | byteValue;    //negative
                return true;
            } else if (0 > leafIndex) {
                // -> leaf index is negative = used by a single "compressed branch"
                final int prevKey = (~leafIndex) >> 8;
                final int prevVal = 0xff & leafIndex;
                //previous and current keys are equal (duplicate key)
                if (prevKey == (int)key) {
                    if (byteValue > prevVal) {  //putIfGreater
                        nodeArray[nidx] = (leafIndex ^ prevVal) | byteValue;    //negative
                        return true;
                    }
                    return false;
                }
                //previous and current keys are not equal
                //create a new leaf
                if (this.nextLeaf >= this.nextLeafArray) {
                    if (this.leafArrays.length <= this.numLeafArrays) {
                        this.leafArrays = Arrays.copyOf(this.leafArrays, this.leafArrays.length << 1);
                    }
                    final byte[] newLeafArray = new byte[LEAF_ARRAY_SIZE];
                    this.leafArrays[this.numLeafArrays++] = newLeafArray;
                    this.nextLeafArray += LEAF_ARRAY_SIZE;
                }
                leafIndex = this.nextLeaf;
                this.nextLeaf += this.leafSize;
                nodeArray[nidx] = leafIndex;
                //push the previous "compressed branch" further to the leaf
                final int lidx = (leafIndex & LEAF_ARRAY_MASK) + (prevKey & this.leafMask);
                this.leafArrays[leafIndex >>> LEAF_ARRAY_SHIFT][lidx] = (byte)prevVal;
            }
            final byte[] leafArray = this.leafArrays[leafIndex >>> LEAF_ARRAY_SHIFT];
            final int lidx = (leafIndex & LEAF_ARRAY_MASK) + ((int)key & this.leafMask);
            final byte prevVal = leafArray[lidx];
            if (byteValue > (0xff & prevVal)) {  //putIfGreater
                leafArray[lidx] = (byte)byteValue;
                return true;
            }
            return false;
        }

        @Override
        public long allocatedBytes() {
            return super.allocatedBytes() + this.lookupArray.length * 4;
        }
    }
}