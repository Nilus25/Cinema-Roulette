package com.imdbselector.application.port.out;

import com.imdbselector.domain.model.User;
import com.imdbselector.domain.valueobject.UserId;

import java.util.List;
import java.util.Optional;

public interface UserRepository {
    List<User> findAll();

    Optional<User> findById(UserId id);

    Optional<User> findByName(String name);

    User save(User user);

    void deleteById(UserId id);
}
