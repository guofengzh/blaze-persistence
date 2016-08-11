/*
 * Copyright 2014 Blazebit.
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
package com.blazebit.persistence.impl;

import java.util.ArrayList;
import java.util.List;

import com.blazebit.persistence.CaseWhenStarterBuilder;
import com.blazebit.persistence.MultipleSubqueryInitiator;
import com.blazebit.persistence.RestrictionBuilder;
import com.blazebit.persistence.SimpleCaseWhenStarterBuilder;
import com.blazebit.persistence.SubqueryInitiator;
import com.blazebit.persistence.impl.builder.expression.CaseWhenBuilderImpl;
import com.blazebit.persistence.impl.builder.expression.ExpressionBuilder;
import com.blazebit.persistence.impl.builder.expression.ExpressionBuilderEndedListener;
import com.blazebit.persistence.impl.builder.expression.SimpleCaseWhenBuilderImpl;
import com.blazebit.persistence.impl.builder.predicate.CaseExpressionBuilderListener;
import com.blazebit.persistence.impl.builder.predicate.LeftHandsideSubqueryPredicateBuilderListener;
import com.blazebit.persistence.impl.builder.predicate.RestrictionBuilderImpl;
import com.blazebit.persistence.impl.builder.predicate.RightHandsideSubqueryPredicateBuilder;
import com.blazebit.persistence.impl.builder.predicate.RootPredicate;
import com.blazebit.persistence.impl.builder.predicate.SuperExpressionLeftHandsideSubqueryPredicateBuilder;
import com.blazebit.persistence.impl.expression.*;
import com.blazebit.persistence.impl.predicate.*;

/**
 *
 * @author Moritz Becker
 * @since 1.0
 */
public abstract class PredicateManager<T> extends AbstractManager {

    protected final SubqueryInitiatorFactory subqueryInitFactory;
    protected final ExpressionFactory expressionFactory;
    protected final RootPredicate rootPredicate;
    @SuppressWarnings({ "unchecked", "rawtypes" })
    private final SubqueryBuilderListenerImpl<RestrictionBuilder<T>> leftSubqueryPredicateBuilderListener = new LeftHandsideSubqueryPredicateBuilderListener();
    private SubqueryBuilderListenerImpl<T> rightSubqueryPredicateBuilderListener;
    private SubqueryBuilderListenerImpl<RestrictionBuilder<T>> superExprLeftSubqueryPredicateBuilderListener;
    private CaseExpressionBuilderListener caseExpressionBuilderListener;
    private MultipleSubqueryInitiator<?> currentMultipleSubqueryInitiator;

    PredicateManager(ResolvingQueryGenerator queryGenerator, ParameterManager parameterManager, SubqueryInitiatorFactory subqueryInitFactory, ExpressionFactory expressionFactory) {
        super(queryGenerator, parameterManager);
        this.rootPredicate = new RootPredicate(parameterManager);
        this.subqueryInitFactory = subqueryInitFactory;
        this.expressionFactory = expressionFactory;
    }

    @SuppressWarnings("unchecked")
    RestrictionBuilder<T> restrict(AbstractCommonQueryBuilder<?, ?, ?, ?, ?> builder, Expression expr) {
        return rootPredicate.startBuilder(new RestrictionBuilderImpl<T>((T) builder, rootPredicate, expr, subqueryInitFactory, expressionFactory, parameterManager));
    }

    CaseWhenStarterBuilder<RestrictionBuilder<T>> restrictCase(AbstractCommonQueryBuilder<?, ?, ?, ?, ?> builder) {
        @SuppressWarnings("unchecked")
        RestrictionBuilder<T> restrictionBuilder = rootPredicate.startBuilder(new RestrictionBuilderImpl<T>((T) builder, rootPredicate, subqueryInitFactory, expressionFactory, parameterManager));
        caseExpressionBuilderListener = new CaseExpressionBuilderListener((RestrictionBuilderImpl<T>) restrictionBuilder);
        return caseExpressionBuilderListener.startBuilder(new CaseWhenBuilderImpl<RestrictionBuilder<T>>(restrictionBuilder, caseExpressionBuilderListener, subqueryInitFactory, expressionFactory, parameterManager));
    }

    void restrictExpression(AbstractCommonQueryBuilder<?, ?, ?, ?, ?> builder, Predicate predicate) {
        rootPredicate.verifyBuilderEnded();
        predicate.accept(parameterManager.getParameterRegistrationVisitor());

        List<Predicate> children = rootPredicate.getPredicate().getChildren();
        children.clear();
        children.add(predicate);
    }

    SimpleCaseWhenStarterBuilder<RestrictionBuilder<T>> restrictSimpleCase(AbstractCommonQueryBuilder<?, ?, ?, ?, ?> builder, Expression caseOperand) {
        @SuppressWarnings("unchecked")
        RestrictionBuilder<T> restrictionBuilder = rootPredicate.startBuilder(new RestrictionBuilderImpl<T>((T) builder, rootPredicate, subqueryInitFactory, expressionFactory, parameterManager));
        caseExpressionBuilderListener = new CaseExpressionBuilderListener((RestrictionBuilderImpl<T>) restrictionBuilder);
        return caseExpressionBuilderListener.startBuilder(new SimpleCaseWhenBuilderImpl<RestrictionBuilder<T>>(restrictionBuilder, caseExpressionBuilderListener, expressionFactory, caseOperand));
    }

    SubqueryInitiator<RestrictionBuilder<T>> restrict(AbstractCommonQueryBuilder<?, ?, ?, ?, ?> builder) {
        @SuppressWarnings("unchecked")
        RestrictionBuilder<T> restrictionBuilder = (RestrictionBuilder<T>) rootPredicate.startBuilder(new RestrictionBuilderImpl<T>((T) builder, rootPredicate, subqueryInitFactory, expressionFactory, parameterManager));
        return subqueryInitFactory.createSubqueryInitiator(restrictionBuilder, leftSubqueryPredicateBuilderListener);
    }

    MultipleSubqueryInitiator<RestrictionBuilder<T>> restrictSubqueries(AbstractCommonQueryBuilder<?, ?, ?, ?, ?> builder, String expression) {
        Expression expr = expressionFactory.createSimpleExpression(expression, true);
        @SuppressWarnings("unchecked")
        RestrictionBuilderImpl<T> restrictionBuilder = rootPredicate.startBuilder(new RestrictionBuilderImpl<T>((T) builder, rootPredicate, subqueryInitFactory, expressionFactory, parameterManager));
        // We don't need a listener or marker here, because the resulting restriction builder can only be ended, when the initiator is ended
        MultipleSubqueryInitiator<RestrictionBuilder<T>> initiator = new MultipleSubqueryInitiatorImpl<RestrictionBuilder<T>>(restrictionBuilder, expr, new RestrictionBuilderExpressionBuilderListener(restrictionBuilder), subqueryInitFactory);
        return initiator;
    }

    <X> MultipleSubqueryInitiator<X> restrictExpressionSubqueries(X builder, Predicate predicate) {
        rootPredicate.verifyBuilderEnded();
        predicate.accept(parameterManager.getParameterRegistrationVisitor());
        
        MultipleSubqueryInitiator<X> initiator = new MultipleSubqueryInitiatorImpl<X>(builder, predicate, new ExpressionBuilderEndedListener() {
            
            @Override
            public void onBuilderEnded(ExpressionBuilder builder) {
                List<Predicate> children = rootPredicate.getPredicate().getChildren();
                children.clear();
                children.add((Predicate) builder.getExpression());
                currentMultipleSubqueryInitiator = null;
            }
            
        }, subqueryInitFactory);
        currentMultipleSubqueryInitiator = initiator;
        return initiator;
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    SubqueryInitiator<RestrictionBuilder<T>> restrict(AbstractCommonQueryBuilder<?, ?, ?, ?, ?> builder, String subqueryAlias, String expression) {
        Expression expr = expressionFactory.createSimpleExpression(expression, true);
        superExprLeftSubqueryPredicateBuilderListener = new SuperExpressionLeftHandsideSubqueryPredicateBuilder(subqueryAlias, expr);
        RestrictionBuilder<T> restrictionBuilder = (RestrictionBuilder<T>) rootPredicate.startBuilder(new RestrictionBuilderImpl<T>((T) builder, rootPredicate, subqueryInitFactory, expressionFactory, parameterManager));
        return subqueryInitFactory.createSubqueryInitiator(restrictionBuilder, superExprLeftSubqueryPredicateBuilderListener);
    }

    SubqueryInitiator<T> restrictExists(T result) {
        rightSubqueryPredicateBuilderListener = rootPredicate.startBuilder(new RightHandsideSubqueryPredicateBuilder<T>(rootPredicate, new ExistsPredicate()));
        return subqueryInitFactory.createSubqueryInitiator(result, rightSubqueryPredicateBuilderListener);
    }

    SubqueryInitiator<T> restrictNotExists(T result) {
        rightSubqueryPredicateBuilderListener = rootPredicate.startBuilder(new RightHandsideSubqueryPredicateBuilder<T>(rootPredicate, new ExistsPredicate(true)));
        return subqueryInitFactory.createSubqueryInitiator(result, rightSubqueryPredicateBuilderListener);
    }

    void applyTransformer(ExpressionTransformer transformer) {
        // carry out transformations
        rootPredicate.getPredicate().accept(new TransformationVisitor(transformer, getClauseType()));
    }

    void verifyBuilderEnded() {
        rootPredicate.verifyBuilderEnded();
        leftSubqueryPredicateBuilderListener.verifySubqueryBuilderEnded();
        if (rightSubqueryPredicateBuilderListener != null) {
            rightSubqueryPredicateBuilderListener.verifySubqueryBuilderEnded();
        }
        if (superExprLeftSubqueryPredicateBuilderListener != null) {
            superExprLeftSubqueryPredicateBuilderListener.verifySubqueryBuilderEnded();
        }
        if (caseExpressionBuilderListener != null) {
            caseExpressionBuilderListener.verifyBuilderEnded();
        }
        if (currentMultipleSubqueryInitiator != null) {
            throw new BuilderChainingException("A builder was not ended properly.");
        }
    }

    void acceptVisitor(Expression.Visitor v) {
        rootPredicate.getPredicate().accept(v);
    }

    <X> X acceptVisitor(Expression.ResultVisitor<X> v) {
        return rootPredicate.getPredicate().accept(v);
    }

    boolean hasPredicates() {
        return rootPredicate.getPredicate().getChildren().size() > 0;
    }

    void buildClause(StringBuilder sb) {
        if (!hasPredicates()) {
            return;
        }
        
        queryGenerator.setQueryBuffer(sb);
        sb.append(' ').append(getClauseName()).append(' ');
        applyPredicate(queryGenerator);
    }

    void buildClausePredicate(StringBuilder sb) {
        queryGenerator.setQueryBuffer(sb);
        applyPredicate(queryGenerator);
    }

    protected abstract String getClauseName();

    protected abstract ClauseType getClauseType();

    void applyPredicate(ResolvingQueryGenerator queryGenerator) {
        SimpleQueryGenerator.BooleanLiteralRenderingContext oldBooleanLiteralRenderingContext = queryGenerator.setBooleanLiteralRenderingContext(SimpleQueryGenerator.BooleanLiteralRenderingContext.PREDICATE);
        rootPredicate.getPredicate().accept(queryGenerator);
        queryGenerator.setBooleanLiteralRenderingContext(oldBooleanLiteralRenderingContext);
    }

    // TODO: needs equals-hashCode implementation
    static class TransformationVisitor extends VisitorAdapter {

        private final ExpressionTransformer transformer;
        private final ClauseType fromClause;
        private boolean joinRequired;

        public TransformationVisitor(ExpressionTransformer transformer, ClauseType fromClause) {
            this.transformer = transformer;
            this.fromClause = fromClause;
            // By default we require joins
            this.joinRequired = true;
        }

        @Override
        public void visit(BetweenPredicate predicate) {
            predicate.setStart(transformer.transform(predicate.getStart(), fromClause, joinRequired));
            predicate.setLeft(transformer.transform(predicate.getLeft(), fromClause, joinRequired));
            predicate.setEnd(transformer.transform(predicate.getEnd(), fromClause, joinRequired));
        }

        @Override
        public void visit(GePredicate predicate) {
            predicate.setLeft(transformer.transform(predicate.getLeft(), fromClause, joinRequired));
            predicate.setRight(transformer.transform(predicate.getRight(), fromClause, joinRequired));
        }

        @Override
        public void visit(GtPredicate predicate) {
            predicate.setLeft(transformer.transform(predicate.getLeft(), fromClause, joinRequired));
            predicate.setRight(transformer.transform(predicate.getRight(), fromClause, joinRequired));
        }

        @Override
        public void visit(LikePredicate predicate) {
            predicate.setLeft(transformer.transform(predicate.getLeft(), fromClause, joinRequired));
            predicate.setRight(transformer.transform(predicate.getRight(), fromClause, joinRequired));
        }

        @Override
        public void visit(EqPredicate predicate) {
    		boolean original = joinRequired;
    		joinRequired = false;
            predicate.setLeft(transformer.transform(predicate.getLeft(), fromClause, joinRequired));
            predicate.setRight(transformer.transform(predicate.getRight(), fromClause, joinRequired));
            joinRequired = original;
        }

        @Override
        public void visit(LePredicate predicate) {
            predicate.setLeft(transformer.transform(predicate.getLeft(), fromClause, joinRequired));
            predicate.setRight(transformer.transform(predicate.getRight(), fromClause, joinRequired));
        }

        @Override
        public void visit(LtPredicate predicate) {
            predicate.setLeft(transformer.transform(predicate.getLeft(), fromClause, joinRequired));
            predicate.setRight(transformer.transform(predicate.getRight(), fromClause, joinRequired));
        }

        @Override
        public void visit(InPredicate predicate) {
    		boolean original = joinRequired;
    		joinRequired = false;
            predicate.setLeft(transformer.transform(predicate.getLeft(), fromClause, joinRequired));
            List<Expression> transformedRight = new ArrayList<Expression>();
            for (Expression right : predicate.getRight()) {
                transformedRight.add(transformer.transform(right, fromClause, joinRequired));
            }
            predicate.setRight(transformedRight);
            joinRequired = original;
        }

        @Override
        public void visit(ExistsPredicate predicate) {
            predicate.setExpression(transformer.transform(predicate.getExpression(), fromClause, joinRequired));
        }

        @Override
        public void visit(MemberOfPredicate predicate) {
    		boolean original = joinRequired;
    		joinRequired = false;
            predicate.setLeft(transformer.transform(predicate.getLeft(), fromClause, joinRequired));
            predicate.setRight(transformer.transform(predicate.getRight(), fromClause, joinRequired));
            joinRequired = original;
        }

        @Override
        public void visit(IsEmptyPredicate predicate) {
    		boolean original = joinRequired;
    		joinRequired = false;
            predicate.setExpression(transformer.transform(predicate.getExpression(), fromClause, joinRequired));
            joinRequired = original;
        }

        @Override
        public void visit(IsNullPredicate predicate) {
    		boolean original = joinRequired;
    		joinRequired = false;
            predicate.setExpression(transformer.transform(predicate.getExpression(), fromClause, joinRequired));
            joinRequired = original;
        }
    }
}
