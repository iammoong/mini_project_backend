package project.moonki.dto.login;

import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import project.moonki.domain.user.entity.MUser;

import java.util.Collection;
import java.util.Collections;

@Getter
public class MUserDetailsDto implements UserDetails {
    private final MUser user;

    public MUserDetailsDto(MUser user) {
        this.user = user;
    }

    public MUser getUser() {
        return user;
    }

    // UserDetails 필수 메소드 구현
    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        // 예시: ROLE 정보 반환
        return Collections.singleton(new SimpleGrantedAuthority(user.getRole().name()));
    }

    @Override
    public String getPassword() {
        return "";
    }

    @Override
    public String getUsername() {
        return user.getUserId(); //
    }
    @Override
    public boolean isAccountNonExpired() {
        return UserDetails.super.isAccountNonExpired();
    }

    @Override
    public boolean isAccountNonLocked() {
        return UserDetails.super.isAccountNonLocked();
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return UserDetails.super.isCredentialsNonExpired();
    }

    @Override
    public boolean isEnabled() {
        return UserDetails.super.isEnabled();
    }
}
