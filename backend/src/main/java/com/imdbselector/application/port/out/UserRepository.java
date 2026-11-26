package com.imdbselector.application.port.out;

import com.imdbselector.domain.model.User;
import com.imdbselector.domain.valueobject.UserId;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

public interface UserRepository {
    List<User> findAll();

    Map<UserId, String> findNamesByIds(Set<UserId> ids);

    Optional<User> findById(UserId id);

    Optional<User> findByName(String name);

    User save(User user);

    void deleteById(UserId id);
}
