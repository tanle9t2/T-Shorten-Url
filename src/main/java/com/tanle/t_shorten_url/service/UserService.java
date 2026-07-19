package com.tanle.t_shorten_url.service;

import com.tanle.t_shorten_url.entity.User;
import java.util.Optional;

public interface UserService {
    Optional<User> findByUsername(String username);
    User save(User user);
}
