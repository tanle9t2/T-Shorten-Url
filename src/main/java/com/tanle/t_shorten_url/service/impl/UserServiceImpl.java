package com.tanle.t_shorten_url.service.impl;

import com.tanle.t_shorten_url.entity.User;
import com.tanle.t_shorten_url.repository.UserRepository;
import com.tanle.t_shorten_url.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserServiceImpl implements UserService {
    
    private final UserRepository userRepository;
    
    @Override
    public Optional<User> findByUsername(String username) {
        log.info("Finding User by username: {}", username);
        return userRepository.findByUsername(username);
    }
    
    @Override
    public User save(User user) {
        log.info("Saving User: {}", user.getUsername());
        return userRepository.save(user);
    }
}
