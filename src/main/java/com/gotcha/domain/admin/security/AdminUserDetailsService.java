package com.gotcha.domain.admin.security;

import com.gotcha.domain.user.entity.User;
import com.gotcha.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AdminUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        User user = userRepository.findByEmailAndIsDeletedFalse(email)
                .filter(User::isAdmin)
                .orElseThrow(() -> new UsernameNotFoundException("관리자 계정을 찾을 수 없습니다: " + email));

        if (user.getAdminPassword() == null) {
            throw new UsernameNotFoundException("관리자 비밀번호가 설정되지 않았습니다: " + email);
        }

        return new AdminUserDetails(user);
    }
}
