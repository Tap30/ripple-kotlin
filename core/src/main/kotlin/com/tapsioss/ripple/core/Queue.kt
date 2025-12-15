package com.tapsioss.ripple.core

/**
 * Efficient FIFO queue using linked list for O(1) operations
 */
class Queue<T> {
    private var head: Node<T>? = null
    private var tail: Node<T>? = null
    private var count = 0

    private data class Node<T>(
        val value: T,
        var next: Node<T>? = null
    )

    /**
     * Add item to tail (O(1))
     */
    fun enqueue(value: T) {
        val newNode = Node(value)
        
        if (tail == null) {
            head = newNode
            tail = newNode
        } else {
            tail?.next = newNode
            tail = newNode
        }
        count++
    }

    /**
     * Remove item from head (O(1))
     */
    fun dequeue(): T? {
        val current = head ?: return null
        
        head = current.next
        if (head == null) {
            tail = null
        }
        count--
        
        return current.value
    }

    /**
     * Convert to list for serialization
     */
    fun toList(): List<T> {
        val result = mutableListOf<T>()
        var current = head
        
        while (current != null) {
            result.add(current.value)
            current = current.next
        }
        
        return result
    }

    /**
     * Bulk load from list
     */
    fun fromList(items: List<T>) {
        clear()
        items.forEach { enqueue(it) }
    }

    /**
     * Get queue size
     */
    fun size(): Int = count

    /**
     * Check if queue is empty
     */
    fun isEmpty(): Boolean = count == 0

    /**
     * Clear all items
     */
    fun clear() {
        head = null
        tail = null
        count = 0
    }
}
