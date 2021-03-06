/**
 *
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.activemq.kaha.impl;

/**
 * A linked list used by IndexItems
 * 
 * @version $Revision: 1.2 $
 */
final class IndexLinkedList implements Cloneable{
    private transient IndexItem root;
    private transient int size=0;

   
    /**
     * Constructs an empty list.
     */
    IndexLinkedList(IndexItem header){
        this.root = header;
        this.root.next=root.prev=root;
    }
    
    IndexItem getRoot(){
        return root;
    }
    


    /**
     * Returns the first element in this list.
     * 
     * @return the first element in this list.
     */
    IndexItem getFirst(){
        if(size==0)
            return null;
        return root.next;
    }

    /**
     * Returns the last element in this list.
     * 
     * @return the last element in this list.
     */
    IndexItem getLast(){
        if(size==0)
            return null;
        return root.prev;
    }

    /**
     * Removes and returns the first element from this list.
     * 
     * @return the first element from this list.
     */
    IndexItem removeFirst(){
        if(size==0){
            return null;
        }
        IndexItem result=root.next;
        remove(root.next);
        return result;
    }

    /**
     * Removes and returns the last element from this list.
     * 
     * @return the last element from this list.
     */
    Object removeLast(){
        if(size==0)
            return null;
        IndexItem result=root.prev;
        remove(root.prev);
        return result;
    }

    /**
     * Inserts the given element at the beginning of this list.
     * 
     * @param o the element to be inserted at the beginning of this list.
     */
    void addFirst(IndexItem item){
        addBefore(item,root.next);
    }

    /**
     * Appends the given element to the end of this list. (Identical in function to the <tt>add</tt> method; included
     * only for consistency.)
     * 
     * @param o the element to be inserted at the end of this list.
     */
    void addLast(IndexItem item){
        addBefore(item,root);
    }

    /**
     * Returns the number of elements in this list.
     * 
     * @return the number of elements in this list.
     */
    int size(){
        return size;
    }

    /**
     * is the list empty?
     * 
     * @return true if there are no elements in the list
     */
    boolean isEmpty(){
        return size==0;
    }

    /**
     * Appends the specified element to the end of this list.
     * 
     * @param o element to be appended to this list.
     * @return <tt>true</tt> (as per the general contract of <tt>Collection.add</tt>).
     */
    boolean add(IndexItem item){
        addBefore(item,root);
        return true;
    }

    /**
     * Removes all of the elements from this list.
     */
    void clear(){
        root.next=root.prev=root;
        size=0;
    }

    // Positional Access Operations
    /**
     * Returns the element at the specified position in this list.
     * 
     * @param index index of element to return.
     * @return the element at the specified position in this list.
     * 
     * @throws IndexOutOfBoundsException if the specified index is is out of range (<tt>index &lt; 0 || index &gt;= size()</tt>).
     */
    IndexItem get(int index){
        return entry(index);
    }

    /**
     * Inserts the specified element at the specified position in this list. Shifts the element currently at that
     * position (if any) and any subsequent elements to the right (adds one to their indices).
     * 
     * @param index index at which the specified element is to be inserted.
     * @param element element to be inserted.
     * 
     * @throws IndexOutOfBoundsException if the specified index is out of range (<tt>index &lt; 0 || index &gt; size()</tt>).
     */
    void add(int index,IndexItem element){
        addBefore(element,(index==size?root:entry(index)));
    }

    /**
     * Removes the element at the specified position in this list. Shifts any subsequent elements to the left (subtracts
     * one from their indices). Returns the element that was removed from the list.
     * 
     * @param index the index of the element to removed.
     * @return the element previously at the specified position.
     * 
     * @throws IndexOutOfBoundsException if the specified index is out of range (<tt>index &lt; 0 || index &gt;= size()</tt>).
     */
    Object remove(int index){
        IndexItem e=entry(index);
        remove(e);
        return e;
    }

    /**
     * Return the indexed entry.
     */
    private IndexItem entry(int index){
        if(index<0||index>=size)
            throw new IndexOutOfBoundsException("Index: "+index+", Size: "+size);
        IndexItem e=root;
        if(index<size/2){
            for(int i=0;i<=index;i++)
                e=e.next;
        }else{
            for(int i=size;i>index;i--)
                e=e.prev;
        }
        return e;
    }

    // Search Operations
    /**
     * Returns the index in this list of the first occurrence of the specified element, or -1 if the List does not
     * contain this element. More formally, returns the lowest index i such that
     * <tt>(o==null ? get(i)==null : o.equals(get(i)))</tt>, or -1 if there is no such index.
     * 
     * @param o element to search for.
     * @return the index in this list of the first occurrence of the specified element, or -1 if the list does not
     *         contain this element.
     */
    int indexOf(IndexItem o){
        int index=0;
        for(IndexItem e=root.next;e!=root;e=e.next){
            if(o==e){
                return index;
            }
            index++;
        }
        return -1;
    }

    /**
     * Retrieve the next entry after this entry
     * 
     * @param entry
     * @return next entry
     */
    IndexItem getNextEntry(IndexItem entry){
        return entry.next != root ? entry.next : null;
    }

    /**
     * Retrive the prev entry after this entry
     * 
     * @param entry
     * @return prev entry
     */
    IndexItem getPrevEntry(IndexItem entry){
        return entry.prev != root ? entry.prev : null;
    }

    /**
     * Insert an Entry before this entry
     * 
     * @param o the elment to insert
     * @param e the Entry to insert the object before
     * 
     */
    void addBefore(IndexItem insert,IndexItem e){
        insert.next=e;
        insert.prev=e.prev;
        insert.prev.next=insert;
        insert.next.prev=insert;
        size++;
    }

    void remove(IndexItem e){
        if(e==root)
            return;
        e.prev.next=e.next;
        e.next.prev=e.prev;
        size--;
    }
    
    /**
     *@return clone
     */
    public Object clone(){
        IndexLinkedList clone=new IndexLinkedList(this.root);
        for(IndexItem e=root.next;e!=root;e=e.next)
            clone.add(e);
        return clone;
    }
}
