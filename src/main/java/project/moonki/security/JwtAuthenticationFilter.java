package project.moonki.security;

import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;
import project.moonki.domain.user.entity.MUser;
import project.moonki.dto.login.MUserDetailsDto;
import project.moonki.repository.user.MuserRepository;
import project.moonki.utils.LogUtil;

import java.io.IOException;

@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtTokenProvider jwtTokenProvider;
    private final MuserRepository muserRepository;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain) {
        try {
            String token = resolveToken(request);

            if (token != null && jwtTokenProvider.validateToken(token)) {
                String userId = jwtTokenProvider.getUserId(token);

                muserRepository.findByUserId(userId).ifPresent(user -> {
                    MUserDetailsDto userDetails = new MUserDetailsDto(user);
                    UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                            userDetails, null, userDetails.getAuthorities());
                    auth.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                    SecurityContextHolder.getContext().setAuthentication(auth);
                });
            }
            chain.doFilter(request, response);
        } catch (Exception e) {
            LogUtil.error(log, JwtAuthenticationFilter.class, e);
            throw new RuntimeException(e);
        }

    }

    private String resolveToken(HttpServletRequest request) {
        String bearer = request.getHeader("Authorization");
        return (StringUtils.hasText(bearer) && bearer.startsWith("Bearer "))
                ? bearer.substring(7) : null;
    }
}
