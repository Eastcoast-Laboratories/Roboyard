/*  DriftingDroids - yet another Ricochet Robots solver program.
    Copyright (C) 2011-2025 Michael Henke

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
package driftingdroids.model

import java.util.Arrays

/**
 * This class is a minimal `Map&ltK,V&gt` implementation for primitive
 * `int` or `long` keys K and <tt>byte</tt> values V,
 * based on a trie (prefix tree) data structure.
 * 
 * 
 * The aim is to balance a fast recognition of duplicate keys
 * and a compact storage of data.
 */
class KeyDepthMapTrieGeneric(keyBits: Int) : KeyDepthMap {
    private val rootNode: IntArray
    private var nodeArrays: Array<IntArray>
    private var numNodeArrays: Int
    private var nextNode: Int
    private var nextNodeArray: Int

    private var leafArrays: Array<ByteArray>
    private var numLeafArrays: Int
    private var nextLeaf: Int
    private var nextLeafArray: Int

    private val nodeBits = 4
    private val nodeNumber: Int
    private val nodeNumberUnCompr: Int
    private val nodeSize: Int
    private val nodeMask: Int
    private val leafBits = 4
    private val leafSize: Int
    private val leafMask: Int


    /**
     * Constructs an empty map that is tuned to the expected bit-size of keys.
     * 
     * @param keyBits the maximum number of bits used by any key that will be put into the map.
     * (e.g. specify 32 if your keys are of type <tt>int</tt>, or specify a lower number
     * if you are sure that your application uses only a subset of all <tt>int</tt> keys)
     */
    init {
        //tuning parameter: number of value bits per internal node
        //tuning parameter: number of value bits per leaf

        this.nodeNumber = (keyBits - this.leafBits + (this.nodeBits - 1)) / this.nodeBits
        this.nodeNumberUnCompr = (keyBits + 8 - 31 + (this.nodeBits - 1)) / this.nodeBits
        this.nodeSize = 1 shl this.nodeBits
        this.nodeMask = this.nodeSize - 1
        this.leafSize = 1 shl this.leafBits
        this.leafMask = this.leafSize - 1

        this.nodeArrays = Array(32) { IntArray(NODE_ARRAY_SIZE) }
        this.rootNode = this.nodeArrays[0]!!
        this.numNodeArrays = 1
        this.nextNode = this.nodeSize //root node already exists
        this.nextNodeArray = NODE_ARRAY_SIZE //first array already exists

        this.leafArrays = Array(32) { ByteArray(LEAF_ARRAY_SIZE) }
        this.numLeafArrays = 0
        this.nextLeaf =
            this.leafSize //no leaves yet, but skip leaf "0" because this is the special value
        this.nextLeafArray = 0 //no leaf arrays yet
    }


    /* (non-Javadoc)
     * @see driftingdroids.model.KeyDepthMap#putIfGreater(int, int)
     */
    override fun putIfGreater(key: Int, byteValue: Int): Boolean {
        //root node
        var key = key
        var nodeArray = this.rootNode
        var nidx = key and this.nodeMask
        var nodeIndex: Int
        var i: Int //used by both for() loops
        //go through nodes (without compression because (key<<8)+value is greater than "int")
        i = 1
        while (i < this.nodeNumberUnCompr) {
            nodeIndex = nodeArray[nidx]
            key = key ushr this.nodeBits
            if (0 == nodeIndex) {
                //create a new node
                if (this.nextNode >= this.nextNodeArray) {
                    if (this.nodeArrays.size <= this.numNodeArrays) {
                        this.nodeArrays =
                            this.nodeArrays.copyOf<IntArray>(this.nodeArrays.size shl 1) as Array<IntArray>
                    }
                    this.nodeArrays[this.numNodeArrays++] = IntArray(NODE_ARRAY_SIZE)
                    this.nextNodeArray += NODE_ARRAY_SIZE
                }
                nodeIndex = this.nextNode
                this.nextNode += this.nodeSize
                nodeArray[nidx] = nodeIndex
            }
            nodeArray = this.nodeArrays[nodeIndex ushr NODE_ARRAY_SHIFT]
            nidx = (nodeIndex and NODE_ARRAY_MASK) + (key and this.nodeMask)
            ++i
        }
        //go through nodes (with compression because (key<<8)+value is inside "int" range now)
        while (i < this.nodeNumber) {
            nodeIndex = nodeArray[nidx]
            key = key ushr this.nodeBits
            if (0 == nodeIndex) {
                // -> node index is null = unused
                //write current key+value as a "compressed branch" (negative node index)
                //exit immediately because no further nodes and no leaf need to be stored
                nodeArray[nidx] = ((key.inv()) shl 8) or byteValue //negative
                return true
            } else if (0 > nodeIndex) {
                // -> node index is negative = used by a single "compressed branch"
                val prevKey = (nodeIndex.inv()) shr 8
                val prevVal = 0xff and nodeIndex
                //previous and current keys are equal (duplicate key)
                if (prevKey == key) {
                    if (byteValue > prevVal) {  //putIfGreater
                        nodeArray[nidx] = (nodeIndex xor prevVal) or byteValue //negative
                        return true
                    }
                    return false
                }
                //previous and current keys are not equal
                //create a new node
                if (this.nextNode >= this.nextNodeArray) {
                    if (this.nodeArrays.size <= this.numNodeArrays) {
                        this.nodeArrays =
                            this.nodeArrays.copyOf<IntArray>(this.nodeArrays.size shl 1) as Array<IntArray>
                    }
                    this.nodeArrays[this.numNodeArrays++] = IntArray(NODE_ARRAY_SIZE)
                    this.nextNodeArray += NODE_ARRAY_SIZE
                }
                nodeIndex = this.nextNode
                this.nextNode += this.nodeSize
                nodeArray[nidx] = nodeIndex
                //push previous "compressed branch" one node further
                nodeArray = this.nodeArrays[nodeIndex ushr NODE_ARRAY_SHIFT]
                nidx = (nodeIndex and NODE_ARRAY_MASK) + (prevKey and this.nodeMask)
                nodeArray[nidx] = ((prevKey ushr this.nodeBits).inv() shl 8) or prevVal //negative
            } else {
                // -> node index is positive = go to next node
                nodeArray = this.nodeArrays[nodeIndex ushr NODE_ARRAY_SHIFT]
            }
            nidx = (nodeIndex and NODE_ARRAY_MASK) + (key and this.nodeMask)
            ++i
        }
        //get leaf (with compression)
        var leafIndex = nodeArray[nidx]
        key = key ushr this.nodeBits
        if (0 == leafIndex) {
            // -> leaf index is null = unused
            //write current value as a "compressed branch" (negative leaf index)
            //exit immediately because no leaf needs to be stored
            nodeArray[nidx] = ((key.inv()) shl 8) or byteValue //negative
            return true
        } else if (0 > leafIndex) {
            // -> leaf index is negative = used by a single "compressed branch"
            val prevKey = (leafIndex.inv()) shr 8
            val prevVal = 0xff and leafIndex
            //previous and current keys are equal (duplicate key)
            if (prevKey == key) {
                if (byteValue > prevVal) {  //putIfGreater
                    nodeArray[nidx] = (leafIndex xor prevVal) or byteValue //negative
                    return true
                }
                return false
            }
            //previous and current keys are not equal
            //create a new leaf
            if (this.nextLeaf >= this.nextLeafArray) {
                if (this.leafArrays.size <= this.numLeafArrays) {
                    this.leafArrays = this.leafArrays.copyOf<ByteArray>(this.leafArrays.size shl 1) as Array<ByteArray>
                }
                val newLeafArray = ByteArray(LEAF_ARRAY_SIZE)
                Arrays.fill(newLeafArray, DEFAULT_VALUE)
                this.leafArrays[this.numLeafArrays++] = newLeafArray
                this.nextLeafArray += LEAF_ARRAY_SIZE
            }
            leafIndex = this.nextLeaf
            this.nextLeaf += this.leafSize
            nodeArray[nidx] = leafIndex
            //push the previous "compressed branch" further to the leaf
            val lidx = (leafIndex and LEAF_ARRAY_MASK) + (prevKey and this.leafMask)
            this.leafArrays[leafIndex ushr LEAF_ARRAY_SHIFT][lidx] = prevVal.toByte()
        }
        val leafArray = this.leafArrays[leafIndex ushr LEAF_ARRAY_SHIFT]
        val lidx = (leafIndex and LEAF_ARRAY_MASK) + (key and this.leafMask)
        val prevVal = leafArray[lidx]
        if (byteValue > prevVal) {  //putIfGreater
            leafArray[lidx] = byteValue.toByte()
            return true
        }
        return false
    }


    /* (non-Javadoc)
     * @see driftingdroids.model.KeyDepthMap#putIfGreater(long, int)
     */
    override fun putIfGreater(key: Long, byteValue: Int): Boolean {
        //this method is copy&paste from put(int,byte) with only a few (int) casts added where required.
        //those lines are marked with //(int)
        //root node
        var key = key
        var nodeArray = this.rootNode
        var nidx = key.toInt() and this.nodeMask //(int)
        var nodeIndex: Int
        var i: Int //used by both for() loops
        //go through nodes (without compression because (key<<8)+value is greater than "int")
        i = 1
        while (i < this.nodeNumberUnCompr) {
            nodeIndex = nodeArray[nidx]
            key = key ushr this.nodeBits
            if (0 == nodeIndex) {
                //create a new node
                if (this.nextNode >= this.nextNodeArray) {
                    if (this.nodeArrays.size <= this.numNodeArrays) {
                        this.nodeArrays =
                            this.nodeArrays.copyOf<IntArray>(this.nodeArrays.size shl 1) as Array<IntArray>
                    }
                    this.nodeArrays[this.numNodeArrays++] = IntArray(NODE_ARRAY_SIZE)
                    this.nextNodeArray += NODE_ARRAY_SIZE
                }
                nodeIndex = this.nextNode
                this.nextNode += this.nodeSize
                nodeArray[nidx] = nodeIndex
            }
            nodeArray = this.nodeArrays[nodeIndex ushr NODE_ARRAY_SHIFT]
            nidx = (nodeIndex and NODE_ARRAY_MASK) + (key.toInt() and this.nodeMask) //(int)
            ++i
        }
        //go through nodes (with compression because (key<<8)+value is inside "int" range now)
        while (i < this.nodeNumber) {
            nodeIndex = nodeArray[nidx]
            key = key ushr this.nodeBits
            if (0 == nodeIndex) {
                // -> node index is null = unused
                //write current key+value as a "compressed branch" (negative node index)
                //exit immediately because no further nodes and no leaf need to be stored
                nodeArray[nidx] = ((key.toInt().inv()) shl 8) or byteValue //negative  //(int)
                return true
            } else if (0 > nodeIndex) {
                // -> node index is negative = used by a single "compressed branch"
                val prevKey = (nodeIndex.inv()) shr 8
                val prevVal = 0xff and nodeIndex
                //previous and current keys are equal (duplicate key)
                if (prevKey == key.toInt()) {  //(int)
                    if (byteValue > prevVal) {  //putIfGreater
                        nodeArray[nidx] = (nodeIndex xor prevVal) or byteValue //negative
                        return true
                    }
                    return false
                }
                //previous and current keys are not equal
                //create a new node
                if (this.nextNode >= this.nextNodeArray) {
                    if (this.nodeArrays.size <= this.numNodeArrays) {
                        this.nodeArrays =
                            this.nodeArrays.copyOf<IntArray>(this.nodeArrays.size shl 1) as Array<IntArray>
                    }
                    this.nodeArrays[this.numNodeArrays++] = IntArray(NODE_ARRAY_SIZE)
                    this.nextNodeArray += NODE_ARRAY_SIZE
                }
                nodeIndex = this.nextNode
                this.nextNode += this.nodeSize
                nodeArray[nidx] = nodeIndex
                //push previous "compressed branch" one node further
                nodeArray = this.nodeArrays[nodeIndex ushr NODE_ARRAY_SHIFT]
                nidx = (nodeIndex and NODE_ARRAY_MASK) + (prevKey and this.nodeMask)
                nodeArray[nidx] = ((prevKey ushr this.nodeBits).inv() shl 8) or prevVal //negative
            } else {
                // -> node index is positive = go to next node
                nodeArray = this.nodeArrays[nodeIndex ushr NODE_ARRAY_SHIFT]
            }
            nidx = (nodeIndex and NODE_ARRAY_MASK) + (key.toInt() and this.nodeMask) //(int)
            ++i
        }
        //get leaf (with compression)
        var leafIndex = nodeArray[nidx]
        key = key ushr this.nodeBits
        if (0 == leafIndex) {
            // -> leaf index is null = unused
            //write current value as a "compressed branch" (negative leaf index)
            //exit immediately because no leaf needs to be stored
            nodeArray[nidx] = ((key.toInt().inv()) shl 8) or byteValue //negative  //(int)
            return true
        } else if (0 > leafIndex) {
            // -> leaf index is negative = used by a single "compressed branch"
            val prevKey = (leafIndex.inv()) shr 8
            val prevVal = 0xff and leafIndex
            //previous and current keys are equal (duplicate key)
            if (prevKey == key.toInt()) {  //(int)
                if (byteValue > prevVal) {  //putIfGreater
                    nodeArray[nidx] = (leafIndex xor prevVal) or byteValue //negative
                    return true
                }
                return false
            }
            //previous and current keys are not equal
            //create a new leaf
            if (this.nextLeaf >= this.nextLeafArray) {
                if (this.leafArrays.size <= this.numLeafArrays) {
                    this.leafArrays = this.leafArrays.copyOf<ByteArray>(this.leafArrays.size shl 1) as Array<ByteArray>
                }
                val newLeafArray = ByteArray(LEAF_ARRAY_SIZE)
                Arrays.fill(newLeafArray, DEFAULT_VALUE)
                this.leafArrays[this.numLeafArrays++] = newLeafArray
                this.nextLeafArray += LEAF_ARRAY_SIZE
            }
            leafIndex = this.nextLeaf
            this.nextLeaf += this.leafSize
            nodeArray[nidx] = leafIndex
            //push the previous "compressed branch" further to the leaf
            val lidx = (leafIndex and LEAF_ARRAY_MASK) + (prevKey and this.leafMask)
            this.leafArrays[leafIndex ushr LEAF_ARRAY_SHIFT][lidx] = prevVal.toByte()
        }
        val leafArray = this.leafArrays[leafIndex ushr LEAF_ARRAY_SHIFT]
        val lidx = (leafIndex and LEAF_ARRAY_MASK) + (key.toInt() and this.leafMask) //(int)
        val prevVal = leafArray[lidx]
        if (byteValue > prevVal) {  //putIfGreater
            leafArray[lidx] = byteValue.toByte()
            return true
        }
        return false
    }


    /* (non-Javadoc)
     * @see driftingdroids.model.KeyDepthMap#size()
     */
    override fun size(): Int {
        var size = 0
        var i = 0
        while (this.nodeSize > i) {
            val nextNodeIndex = this.rootNode[i]
            if (0 > nextNodeIndex) {
                // -> node index is negative = used by a single "compressed branch"
                ++size
            } else if (0 < nextNodeIndex) {
                // -> node index is positive = go to next node
                size += this.sizeRecursion(2, nextNodeIndex)
            }
            ++i
        }
        return size
    }

    private fun sizeRecursion(thisNodeDepth: Int, thisNodeIndex: Int): Int {
        assert(0 < thisNodeIndex) { thisNodeIndex }
        var size = 0
        val nodeArray = this.nodeArrays[thisNodeIndex ushr NODE_ARRAY_SHIFT]
        var nidx = thisNodeIndex and NODE_ARRAY_MASK
        var i = 0
        while (this.nodeSize > i) {
            val nextNodeIndex = nodeArray[nidx]
            if (0 > nextNodeIndex) {
                // -> node index is negative = used by a single "compressed branch"
                ++size
            } else if (0 < nextNodeIndex) {
                if (thisNodeDepth < this.nodeNumber) {
                    // -> node index is positive = go to next node
                    size += this.sizeRecursion(thisNodeDepth + 1, nextNodeIndex)
                } else {
                    // -> node index is positive = go to leaf node
                    val leafArray = this.leafArrays[nextNodeIndex ushr LEAF_ARRAY_SHIFT]
                    var lidx = nextNodeIndex and LEAF_ARRAY_MASK
                    var j = 0
                    while (this.leafSize > j) {
                        if (DEFAULT_VALUE != leafArray[lidx++]) {
                            ++size
                        }
                        ++j
                    }
                }
            }
            ++i
            ++nidx
        }
        return size
    }


    /* (non-Javadoc)
     * @see driftingdroids.model.KeyDepthMap#allocatedBytes()
     */
    override fun allocatedBytes(): Long {
        var result: Long = 0
        for (i in 0..<this.numNodeArrays) {
            result += this.nodeArrays[i].size * 4L
        }
        //        final long nodeResult = result;
        for (i in 0..<this.numLeafArrays) {
            result += this.leafArrays[i].size.toLong()
        }
        //        Logger.println("getBytesAllocated TrieMapByte  nodes = " + nodeResult);
//        Logger.println("getBytesAllocated TrieMapByte leaves = " + (result - nodeResult));
        return result
    }

    companion object {
        val DEFAULT_VALUE: Byte = -1 //unsigned byte: 255

        private const val NODE_ARRAY_SHIFT = 16
        private val NODE_ARRAY_SIZE = 1 shl NODE_ARRAY_SHIFT
        private val NODE_ARRAY_MASK: Int = NODE_ARRAY_SIZE - 1
        private const val LEAF_ARRAY_SHIFT = 16
        private val LEAF_ARRAY_SIZE = 1 shl LEAF_ARRAY_SHIFT
        private val LEAF_ARRAY_MASK: Int = LEAF_ARRAY_SIZE - 1
    }
}
