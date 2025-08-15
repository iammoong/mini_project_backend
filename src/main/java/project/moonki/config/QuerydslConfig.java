package project.moonki.config;

import com.querydsl.jpa.impl.JPAQueryFactory;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration class for setting up QueryDSL components.
 *
 * This class defines a bean for {@link JPAQueryFactory}, which is used
 * as a QueryDSL abstraction to construct type-safe SQL-like queries in a Java environment.
 * The {@link EntityManager} is injected via the `@PersistenceContext`
 * annotation and used to initialize the QueryFactory.
 *
 * Beans provided by this configuration:
 * - {@link JPAQueryFactory}: Configures and provides the query factory
 *   for executing QueryDSL queries.
 *
 * Usage:
 * Autowire this configuration in dependent components needing a pre-configured JPAQueryFactory.
 */
@Configuration
public class QuerydslConfig {

    @PersistenceContext
    private EntityManager em;

    @Bean
    public JPAQueryFactory jpaQueryFactory() {
        return new JPAQueryFactory(em);
    }
}
