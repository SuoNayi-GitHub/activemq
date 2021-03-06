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
package org.apache.activemq.kaha;

import java.util.List;
import java.util.NoSuchElementException;
/**
 *Represents a container of persistent objects in the store
 *Acts as a map, but values can be retrieved in insertion order
 * 
 * @version $Revision: 1.2 $
 */
public interface ListContainer extends List{
    /**
     * The container is created or retrieved in 
     * an unloaded state.
     * load populates the container will all the indexes used etc
     * and should be called before any operations on the container
     */
    public void load();
    
    /**
     * unload indexes from the container
     *
     */
    public void unload();
    
    /**
     * @return true if the indexes are loaded
     */
    public boolean isLoaded();
    
    
    /**
     * For homogenous containers can set a custom marshaller for loading values
     * The default uses Object serialization
     * @param marshaller 
     */
    public void setMarshaller(Marshaller marshaller);
    /**
     * @return the id the MapContainer was create with
     */
    public Object getId();

    /**
     * @return the number of values in the container
     */
    public int size();
    
    /**
     * Inserts the given element at the beginning of this list.
     *
     * @param o the element to be inserted at the beginning of this list.
     */
    public void addFirst(Object o);

    /**
     * Appends the given element to the end of this list.  (Identical in
     * function to the <tt>add</tt> method; included only for consistency.)
     *
     * @param o the element to be inserted at the end of this list.
     */
    public void addLast(Object o);
    
    /**
     * Removes and returns the first element from this list.
     *
     * @return the first element from this list.
     * @throws    NoSuchElementException if this list is empty.
     */
    public Object removeFirst();

    /**
     * Removes and returns the last element from this list.
     *
     * @return the last element from this list.
     * @throws    NoSuchElementException if this list is empty.
     */
    public Object removeLast();
    
    
    /**
     * remove an objecr from the list without retrieving the old value from the store
     * @param position
     * @return true if successful
     */
    public boolean doRemove(int position);
        
    
}
