package project.moonki.repository.user.custom;

import project.moonki.domain.user.entity.MUser;

import java.util.List;

public interface MuserRepositoryCustom {
    List<MUser> searchUsers(Long meId, String q, int limit);
}
