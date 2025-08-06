package project.moonki.security;

import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import project.moonki.domain.user.entity.MUser;
import project.moonki.dto.login.MUserDetailsDto;
import project.moonki.repository.user.MuserRepository;

import java.io.IOException;

@Component
public class JwtAuthenticationFilter extends GenericFilter {

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @Autowired
    private MuserRepository muserRepository; // 유저 DB 조회용

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        String token = resolveToken((HttpServletRequest) request);

        if (token != null && jwtTokenProvider.validateToken(token)) {
            String userId = jwtTokenProvider.getUserId(token);
            // DB에서 유저 조회 (optional, 필요시)
            MUser user = muserRepository.findByUserId(userId).orElse(null);
            if (user != null) {
                MUserDetailsDto userDetails = new MUserDetailsDto(user);
                // 인증 객체 생성(실무는 UserDetails, 여기선 간단히 userId만)
                UsernamePasswordAuthenticationToken authentication =
                        new UsernamePasswordAuthenticationToken(new MUserDetailsDto(user), null, userDetails.getAuthorities());
                authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails((HttpServletRequest) request));
                SecurityContextHolder.getContext().setAuthentication(authentication);
            }
        }
        chain.doFilter(request, response);
    }

    private String resolveToken(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }
        return null;
    }
}
