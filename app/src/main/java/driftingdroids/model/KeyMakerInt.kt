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

abstract class KeyMakerInt {
    /**
     * Creates the <tt>int</tt> key from the values of the given <tt>state</tt>.
     * 
     * @param state array of int values (positions of the robots on the board)
     * @return the key
     */
    abstract fun run(state: IntArray): Int


    private class KeyMakerIntAll(
        boardNumRobots: Int,
        boardSizeNumBits: Int,
        isBoardGoalWildcard: Boolean
    ) : KeyMakerInt() {
        private val tmpState: IntArray
        private val idxSort: Int
        private val idxLen1: Int
        private val idxLen2: Int
        private val s1: Int

        init {
            this.tmpState = IntArray(boardNumRobots)
            this.idxSort = this.tmpState.size - (if (isBoardGoalWildcard) 0 else 1)
            this.idxLen1 = this.tmpState.size - 1
            this.idxLen2 = this.tmpState.size - 2
            this.s1 = boardSizeNumBits
        }

        override fun run(state: IntArray): Int {
            assert(this.tmpState.size == state.size) { state.size }
            //copy and sort state
            System.arraycopy(state, 0, this.tmpState, 0, state.size)
            Arrays.sort(this.tmpState, 0, this.idxSort)
            //pack state into a single int value
            var result = this.tmpState[this.idxLen1]
            for (i in this.idxLen2 downTo 0) {
                result = (result shl this.s1) or this.tmpState[i]
            }
            return result
        }
    }


    private class KeyMakerInt11 : KeyMakerInt() {
        //sort 1 of 1 elements
        override fun run(state: IntArray): Int {
            assert(1 == state.size) { state.size }
            return state[0]
        }
    }


    private class KeyMakerInt21(//sort 1 of 2 elements
        private val s1: Int
    ) : KeyMakerInt() {
        override fun run(state: IntArray): Int {
            assert(2 == state.size) { state.size }
            return state[0] or (state[1] shl this.s1)
        }
    }


    private class KeyMakerInt22(//sort 2 of 2 elements
        private val s1: Int
    ) : KeyMakerInt() {
        override fun run(state: IntArray): Int {
            assert(2 == state.size) { state.size }
            val result: Int
            if (state[0] < state[1]) {
                result = state[0] or (state[1] shl this.s1)
            } else {
                result = state[1] or (state[0] shl this.s1)
            }
            return result
        }
    }


    private class KeyMakerInt32(//sort 2 of 3 elements
        private val s1: Int
    ) : KeyMakerInt() {
        private val s2: Int

        init {
            this.s2 = s1 * 2
        }

        override fun run(state: IntArray): Int {
            assert(3 == state.size) { state.size }
            val result: Int
            if (state[0] < state[1]) {
                result = state[0] or (state[1] shl this.s1)
            } else {
                result = state[1] or (state[0] shl this.s1)
            }
            return result or (state[2] shl this.s2)
        }
    }


    private class KeyMakerInt33(//sort 3 of 3 elements
        private val s1: Int, private val len: Int
    ) : KeyMakerInt() {
        private val s2: Int

        init {
            this.s2 = s1 * 2
        }

        override fun run(state: IntArray): Int {
            assert(this.len == state.size) { state.size }
            val a = state[0]
            val b = state[1]
            val c = state[2]
            val result: Int
            if (a < b) {
                if (a < c) {
                    if (b < c) {
                        result = a or (b shl this.s1) or (c shl this.s2)
                    } else {
                        result = a or (c shl this.s1) or (b shl this.s2)
                    }
                } else {
                    result = c or (a shl this.s1) or (b shl this.s2)
                }
            } else {
                if (b < c) {
                    if (a < c) {
                        result = b or (a shl this.s1) or (c shl this.s2)
                    } else {
                        result = b or (c shl this.s1) or (a shl this.s2)
                    }
                } else {
                    result = c or (b shl this.s1) or (a shl this.s2)
                }
            }
            return result
        }
    }


    private class KeyMakerInt43(//sort 3 of 4 elements
        private val s1: Int
    ) : KeyMakerInt() {
        private val s2: Int
        private val s3: Int

        init {
            this.s2 = s1 * 2
            this.s3 = s1 * 3
        }

        override fun run(state: IntArray): Int {
            assert(4 == state.size) { state.size }
            val a = state[0]
            val b = state[1]
            val c = state[2]
            val result: Int
            if (a < b) {
                if (a < c) {
                    if (b < c) {
                        result = a or (b shl this.s1) or (c shl this.s2)
                    } else {
                        result = a or (c shl this.s1) or (b shl this.s2)
                    }
                } else {
                    result = c or (a shl this.s1) or (b shl this.s2)
                }
            } else {
                if (b < c) {
                    if (a < c) {
                        result = b or (a shl this.s1) or (c shl this.s2)
                    } else {
                        result = b or (c shl this.s1) or (a shl this.s2)
                    }
                } else {
                    result = c or (b shl this.s1) or (a shl this.s2)
                }
            }
            return result or (state[3] shl this.s3)
        }
    }


    private class KeyMakerInt44(//sort 4 of 4 elements
        private val s1: Int, private val len: Int
    ) : KeyMakerInt() {
        private val s2: Int
        private val s3: Int

        init {
            this.s2 = s1 * 2
            this.s3 = s1 * 3
        }

        override fun run(state: IntArray): Int {
            assert(this.len == state.size) { state.size }
            val a = state[0]
            val b = state[1]
            val c = state[2]
            val d = state[3]
            val result: Int
            if (a <= b) {
                if (c <= d) {
                    if (a <= c) {
                        if (b <= d) {
                            if (b <= c) {
                                result = a or (b shl this.s1) or (c shl this.s2) or (d shl this.s3)
                            } else {
                                result = a or (c shl this.s1) or (b shl this.s2) or (d shl this.s3)
                            }
                        } else {
                            result = a or (c shl this.s1) or (d shl this.s2) or (b shl this.s3)
                        }
                    } else {
                        if (b <= d) {
                            result = c or (a shl this.s1) or (b shl this.s2) or (d shl this.s3)
                        } else {
                            if (a <= d) {
                                result = c or (a shl this.s1) or (d shl this.s2) or (b shl this.s3)
                            } else {
                                result = c or (d shl this.s1) or (a shl this.s2) or (b shl this.s3)
                            }
                        }
                    }
                } else {
                    if (a <= d) {
                        if (b <= c) {
                            if (b <= d) {
                                result = a or (b shl this.s1) or (d shl this.s2) or (c shl this.s3)
                            } else {
                                result = a or (d shl this.s1) or (b shl this.s2) or (c shl this.s3)
                            }
                        } else {
                            result = a or (d shl this.s1) or (c shl this.s2) or (b shl this.s3)
                        }
                    } else {
                        if (b <= c) {
                            result = d or (a shl this.s1) or (b shl this.s2) or (c shl this.s3)
                        } else {
                            if (a <= c) {
                                result = d or (a shl this.s1) or (c shl this.s2) or (b shl this.s3)
                            } else {
                                result = d or (c shl this.s1) or (a shl this.s2) or (b shl this.s3)
                            }
                        }
                    }
                }
            } else {
                if (c <= d) {
                    if (b <= c) {
                        if (a <= d) {
                            if (a <= c) {
                                result = b or (a shl this.s1) or (c shl this.s2) or (d shl this.s3)
                            } else {
                                result = b or (c shl this.s1) or (a shl this.s2) or (d shl this.s3)
                            }
                        } else {
                            result = b or (c shl this.s1) or (d shl this.s2) or (a shl this.s3)
                        }
                    } else {
                        if (a <= d) {
                            result = c or (b shl this.s1) or (a shl this.s2) or (d shl this.s3)
                        } else {
                            if (b <= d) {
                                result = c or (b shl this.s1) or (d shl this.s2) or (a shl this.s3)
                            } else {
                                result = c or (d shl this.s1) or (b shl this.s2) or (a shl this.s3)
                            }
                        }
                    }
                } else {
                    if (b <= d) {
                        if (a <= c) {
                            if (a <= d) {
                                result = b or (a shl this.s1) or (d shl this.s2) or (c shl this.s3)
                            } else {
                                result = b or (d shl this.s1) or (a shl this.s2) or (c shl this.s3)
                            }
                        } else {
                            result = b or (d shl this.s1) or (c shl this.s2) or (a shl this.s3)
                        }
                    } else {
                        if (a <= c) {
                            result = d or (b shl this.s1) or (a shl this.s2) or (c shl this.s3)
                        } else {
                            if (b <= c) {
                                result = d or (b shl this.s1) or (c shl this.s2) or (a shl this.s3)
                            } else {
                                result = d or (c shl this.s1) or (b shl this.s2) or (a shl this.s3)
                            }
                        }
                    }
                }
            }
            return result
        }
    }


    companion object {
        /**
         * Creates an instance of <tt>KeyMakerInt</tt> that is tailored to the given parameters.
         * 
         * @param boardNumRobots number of robots on the board (length of parameter <tt>state</tt> of method <tt>run</tt>)
         * @param boardSizeNumBits number of bits required to store the size of the board (the 16x16 board need 8 bits)
         * @param isBoardGoalWildcard true if the current goal is a wildcard goal (can be reached by any robot)
         * @return the instance of KeyMakerInt created
         */
        fun createInstance(
            boardNumRobots: Int,
            boardSizeNumBits: Int,
            isBoardGoalWildcard: Boolean
        ): KeyMakerInt {
            val keyMaker: KeyMakerInt
            when (boardNumRobots) {
                1 -> keyMaker = KeyMakerInt11()
                2 -> keyMaker =
                    (if (isBoardGoalWildcard) KeyMakerInt22(boardSizeNumBits) else KeyMakerInt21(
                        boardSizeNumBits
                    ))

                3 -> keyMaker =
                    (if (isBoardGoalWildcard) KeyMakerInt33(boardSizeNumBits, 3) else KeyMakerInt32(
                        boardSizeNumBits
                    ))

                4 -> keyMaker =
                    (if (isBoardGoalWildcard) KeyMakerInt44(boardSizeNumBits, 4) else KeyMakerInt43(
                        boardSizeNumBits
                    ))

                else -> keyMaker =
                    KeyMakerIntAll(boardNumRobots, boardSizeNumBits, isBoardGoalWildcard)
            }
            return keyMaker
        }


        /**
         * Creates an instance of <tt>KeyMakerInt</tt> that is tailored to the given parameters.
         * 
         * @param boardNumRobots number of robots on the board (length of parameter <tt>state</tt> of method <tt>run</tt>)
         * @param boardSizeNumBits number of bits required to store the size of the board (the 16x16 board need 8 bits)
         * @param isBoardGoalWildcard true if the current goal is a wildcard goal (can be reached by any robot)
         * @param isSolution01 true if the active robot can reach the goal in one move
         * @return the instance of KeyMakerInt created
         */
        fun createInstance(
            boardNumRobots: Int,
            boardSizeNumBits: Int,
            isBoardGoalWildcard: Boolean,
            isSolution01: Boolean
        ): KeyMakerInt? {
            val keyMaker: KeyMakerInt?
            if (isSolution01) {
                when (boardNumRobots) {
                    4 -> keyMaker = KeyMakerInt33(boardSizeNumBits, 4)
                    5 -> keyMaker = KeyMakerInt44(boardSizeNumBits, 5)
                    else -> keyMaker = null // other numbers of robots are not supported!
                }
            } else {
                keyMaker = createInstance(boardNumRobots, boardSizeNumBits, isBoardGoalWildcard)
            }
            return keyMaker
        }
    }
}

