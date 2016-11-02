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
package com.blazebit.persistence.impl.transform;

import com.blazebit.persistence.impl.ClauseType;
import com.blazebit.persistence.impl.expression.Expression;
import com.blazebit.persistence.impl.expression.modifier.ExpressionModifier;

/**
 *
 * @author Moritz Becker
 * @since 1.0
 */
public interface ExpressionTransformer {

    public Expression transform(Expression original, ClauseType fromClause, boolean joinRequired);

    public Expression transform(ExpressionModifier<? extends Expression> parentModifier, Expression original, ClauseType fromClause, boolean joinRequired);

}
