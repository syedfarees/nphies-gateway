package com.amins.nphies.user;

import com.amins.nphies.user.dto.RegisterRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserService implements UserDetailsService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final InvitationService invitationService;

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        return userRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + email));
    }

    @Transactional
    public User register(RegisterRequest req) {
        if (userRepository.existsByEmailIgnoreCase(req.getEmail())) {
            throw new IllegalArgumentException("Email already registered");
        }
        // Role comes from the admin-issued invitation, never the client
        UserInvitation invitation = invitationService.redeem(req.getEmail(), req.getOtp());
        User user = new User();
        user.setName(req.getName());
        user.setEmail(req.getEmail().toLowerCase());
        user.setPasswordHash(passwordEncoder.encode(req.getPassword()));
        user.setRole(invitation.getRole());
        return userRepository.save(user);
    }

    public User findByEmail(String email) {
        return userRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + email));
    }
}
