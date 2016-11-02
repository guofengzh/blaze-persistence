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

package com.blazebit.persistence.view.impl;

import com.blazebit.persistence.CommonQueryBuilder;
import com.blazebit.persistence.view.CorrelationProvider;

import java.util.Map;

public class SimpleCorrelationProviderFactory implements CorrelationProviderFactory {

    private final Class<? extends CorrelationProvider> clazz;

    public SimpleCorrelationProviderFactory(Class<? extends CorrelationProvider> clazz) {
        this.clazz = clazz;
    }

    @Override
    public boolean isParameterized() {
        return false;
    }

    @Override
    public CorrelationProvider create(CommonQueryBuilder<?> queryBuilder, Map<String, Object> optionalParameters) {
        try {
            return clazz.newInstance();
        } catch (Exception ex) {
            throw new IllegalArgumentException("Could not instantiate the correlation provider: " + clazz.getName(), ex);
        }
    }

}
