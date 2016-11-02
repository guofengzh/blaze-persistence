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

package com.blazebit.persistence.impl.expression;

import com.blazebit.persistence.impl.expression.modifier.ExpressionListModifier;
import com.blazebit.persistence.impl.expression.modifier.ExpressionModifier;

import java.util.List;

/**
 * Created
 * by Moritz Becker (moritz.becker@gmx.at)
 * on 22.09.2016.
 */
public class ExpressionModifyingResultVisitorAdapter extends PredicateModifyingResultVisitorAdapter {

    @Override
    public Expression visit(WhenClauseExpression expression) {
        expression.getCondition().accept(this);
        parentModifier = expressionModifiers.getWhenClauseExpressionModifier(expression);
        expression.setResult(expression.getResult().accept(this));
        return expression;
    }

    @Override
    public Expression visit(GeneralCaseExpression expression) {
        List<WhenClauseExpression> expressions = expression.getWhenClauses();
        int size = expressions.size();
        for (int i = 0; i < size; i++) {
            expressions.get(i).accept(this);
        }
        parentModifier = expressionModifiers.getGeneralCaseExpressionModifier(expression);
        expression.setDefaultExpr(expression.getDefaultExpr().accept(this));
        return expression;
    }

    @Override
    public Expression visit(SimpleCaseExpression expression) {
        return visit((GeneralCaseExpression) expression);
    }

    @Override
    public Expression visit(PathExpression expression) {
        ExpressionListModifier<PathElementExpression> listModifier;
        parentModifier = (ExpressionModifier<Expression>) (ExpressionModifier) (listModifier = expressionModifiers.getExpressionListModifier(expression.getExpressions()));
        for (int i = 0; i < expression.getExpressions().size(); i++) {
            listModifier.setModificationnIndex(i);
            expression.getExpressions().set(i, (PathElementExpression) expression.getExpressions().get(i).accept(this));
        }
        return expression;
    }

    @Override
    public Expression visit(ArithmeticExpression expression) {
        parentModifier = expressionModifiers.getArithmeticLeftExpressionModifier(expression);
        expression.setLeft(expression.getLeft().accept(this));
        parentModifier = expressionModifiers.getArithmeticRightExpressionModifier(expression);
        expression.setRight(expression.getRight().accept(this));
        return expression;
    }

}
