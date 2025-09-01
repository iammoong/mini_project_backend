package project.moonki.repository.user.impl;

import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.types.OrderSpecifier;
import com.querydsl.core.types.dsl.Expressions;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import project.moonki.domain.user.entity.MUser;
import project.moonki.domain.user.entity.QMUser;
import project.moonki.repository.user.custom.MuserRepositoryCustom;

import java.util.List;

@Repository
@RequiredArgsConstructor
public class MuserRepositoryImpl implements MuserRepositoryCustom {

    private final JPAQueryFactory query;

    /**
     *
     * @param meId
     * @param q
     * @param limit
     * @return
     */
    @Override
    public List<MUser> searchUsers(Long meId, String q, int limit) {
        QMUser u = QMUser.mUser;

        BooleanBuilder where = new BooleanBuilder();
        if (meId != null) {
            where.and(u.id.ne(meId));
        }
        if (q != null && !q.isBlank()) {
            String kw = q.trim();
            // 닉네임 또는 이름에 부분일치(대소문자 무시)
            where.and(
                    u.nickname.containsIgnoreCase(kw)
                            .or(u.username.containsIgnoreCase(kw))
            );
        }

        // coalesce(nickname, username) ASC 정렬
        OrderSpecifier<String> byName =
                Expressions.stringTemplate("coalesce({0},{1})", u.nickname, u.username).asc();

        return query
                .selectFrom(u)
                .where(where)
                .orderBy(byName)
                .limit(limit > 0 ? limit : 50)
                .fetch();
    }
}
