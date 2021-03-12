package com.example.smartshrimper

class RingBuffer(val capacity: Int)  {

    private val storage = IntArray(capacity)
    private var writeIndex = this.myIterator()
    private var readIndex = this.myIterator()


    public fun readNext() : Int {
        return(storage[readIndex.next()])
    }
    public fun writeNext(value : Int) {
        storage[writeIndex.next()] = value
    }

    /**
     * Iterator for [RingBuffer]. This reads data from buffer circularly. [hasNext] always returns true.
     *
     */
    inner class myIterator : kotlin.collections.Iterator<IntArray> {

        private var startPosition: Int = 0

        /**
         * Always returns true.
         */
        override fun hasNext(): Boolean = true

        /**
         * Read value from buffer.
         */
        override fun next(): Int {
            val retVal = startPosition++
            if (startPosition > storage.lastIndex) startPosition = 0
            return(retVal)
        }
    }
}
