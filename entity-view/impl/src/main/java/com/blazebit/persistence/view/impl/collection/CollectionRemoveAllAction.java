/*
 * Copyright 2014 - 2016 Blazebit.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.blazebit.persistence.view.impl.collection;

import java.util.ArrayList;
import java.util.Collection;

public class CollectionRemoveAllAction<C extends Collection<E>, E> implements CollectionAction<C> {

    private final Collection<?> elements;
    
    public CollectionRemoveAllAction(Collection<?> collection) {
        this.elements = new ArrayList<Object>(collection);
    }

    @Override
    public void doAction(C collection) {
        collection.removeAll(elements);
    }

}
