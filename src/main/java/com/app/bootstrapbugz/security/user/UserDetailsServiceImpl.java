package com.app.bootstrapbugz.security.user;

import com.app.bootstrapbugz.constant.ErrorDomain;
import com.app.bootstrapbugz.error.exception.ResourceNotFound;
import com.app.bootstrapbugz.model.user.User;
import com.app.bootstrapbugz.repository.user.UserRepository;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserDetailsServiceImpl implements UserDetailsService {
    private final UserRepository userRepository;
    private final MessageSource messageSource;

    public UserDetailsServiceImpl(UserRepository userRepository, MessageSource messageSource) {
        this.userRepository = userRepository;
        this.messageSource = messageSource;
    }

    @Override
    @Transactional
    public UserDetails loadUserByUsername(String username) throws ResourceNotFound {
        User user = userRepository.findByUsername(username).orElseThrow(
                () -> new ResourceNotFound(messageSource.getMessage("user.notFound", null, LocaleContextHolder.getLocale()), ErrorDomain.AUTH)
        );
        return UserPrincipal.create(user);
    }
}
