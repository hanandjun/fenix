package com.blinkfox.fenix.jpa;

import jakarta.persistence.EntityManager;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.jpa.provider.QueryExtractor;
import org.springframework.data.jpa.repository.query.DefaultJpaQueryMethodFactory;
import org.springframework.data.jpa.repository.query.EscapeCharacter;
import org.springframework.data.jpa.repository.query.JpaQueryLookupStrategy;
import org.springframework.data.jpa.repository.query.QueryRewriterProvider;
import org.springframework.data.projection.ProjectionFactory;
import org.springframework.data.repository.core.NamedQueries;
import org.springframework.data.repository.core.RepositoryMetadata;
import org.springframework.data.repository.query.CachingValueExpressionDelegate;
import org.springframework.data.repository.query.QueryLookupStrategy;
import org.springframework.data.repository.query.RepositoryQuery;
import org.springframework.data.repository.query.ValueExpressionDelegate;

import java.lang.reflect.Method;

/**
 * 定义用来处理 {@link QueryFenix} 注解的查找策略类，该策略类实现了 {@link QueryLookupStrategy} 接口.
 * <p>该类主要重写了 {@link #resolveQuery} 这个方法，用来监测 JPA 的接口方法是否标注了 {@link QueryFenix} 注解.</p>
 * <ul>
 *     <li>如果标注了 {@link QueryFenix} 注解，就需要本 Fenix 扩展类库识别处理 XML 文件或 Java 中的 JPQL 语句；</li>
 *     <li>如果没有标注 {@link QueryFenix} 注解，就使用 JPA 默认的 {@link QueryLookupStrategy}.</li>
 * </ul>
 *
 * @author blinkfox on 2019-08-04.
 * @since v1.0.0
 */
@Slf4j
public class FenixQueryLookupStrategy implements QueryLookupStrategy {

    /**
     * EntityManager 实体管理器.
     */
    private final EntityManager entityManager;

    /**
     * QueryExtractor 查询提取器.
     */
    private final QueryExtractor extractor;

    /**
     * JPA 默认的 Query 查找策略实例.
     */
    private final QueryLookupStrategy jpaQueryLookupStrategy;

    public FenixQueryLookupStrategy(EntityManager entityManager, Key key, ValueExpressionDelegate valueExpressionDelegate, QueryExtractor extractor) {
        this.entityManager = entityManager;
        this.extractor = extractor;
        this.jpaQueryLookupStrategy = JpaQueryLookupStrategy.create(entityManager, new DefaultJpaQueryMethodFactory(extractor), key, new CachingValueExpressionDelegate(valueExpressionDelegate), QueryRewriterProvider.simple(), EscapeCharacter.DEFAULT);
    }

    public static QueryLookupStrategy create(EntityManager entityManager, Key key, ValueExpressionDelegate valueExpressionDelegate, QueryExtractor extractor) {
        return new FenixQueryLookupStrategy(entityManager, key, valueExpressionDelegate, extractor);
    }

    /**
     * 判断执行的方法上是否有 {@link QueryFenix} 注解，如果有的话，就构造 {@link FenixJpaQuery} 实例，否则就是用 JPA 默认的处理方式.
     *
     * @param method       will never be {@literal null}.
     * @param metadata     will never be {@literal null}.
     * @param factory      will never be {@literal null}.
     * @param namedQueries will never be {@literal null}.
     * @return RepositoryQuery
     */
    @Override
    public RepositoryQuery resolveQuery(Method method, RepositoryMetadata metadata, ProjectionFactory factory, NamedQueries namedQueries) {
        // 如果没有 QueryFenix 注解，就是用默认的 jpaQueryLookupStrategy.resolveQuery 来构造 RepositoryQuery 实例.
        QueryFenix queryFenixAnnotation = method.getAnnotation(QueryFenix.class);
        if (queryFenixAnnotation == null) {
            return this.jpaQueryLookupStrategy.resolveQuery(method, metadata, factory, namedQueries);
        }

        // 如果有 QueryFenix 注解，就构造 FenixJpaQuery 实例，并注入 QueryFenix 和调用方法的 class 到该实例中，便于后续使用.
        FenixJpaQuery fenixJpaQuery = new FenixJpaQuery((new DefaultJpaQueryMethodFactory(extractor)).build(method, metadata, factory), this.entityManager);

        fenixJpaQuery.setQueryFenix(queryFenixAnnotation);
        fenixJpaQuery.setQueryClass(method.getDeclaringClass());
        return fenixJpaQuery;
    }

}
