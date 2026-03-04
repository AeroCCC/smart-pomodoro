package com.pomotodo.service;

import com.pomotodo.dto.AuthRequest;
import com.pomotodo.exception.ApiException;
import com.pomotodo.entity.User;
import com.pomotodo.repository.UserRepository;
import com.pomotodo.security.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthService {
    
    private final AuthenticationManager authenticationManager;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider tokenProvider;
    
    @Transactional
    public AuthRequest.AuthResponse register(AuthRequest.RegisterRequest request) {
        // Check if username exists
        if (userRepository.existsByUsername(request.getUsername())) {
            throw ApiException.badRequest("USERNAME_TAKEN", "Username is already taken");
        }
        
        // Check if email exists
        if (userRepository.existsByEmail(request.getEmail())) {
            throw ApiException.badRequest("EMAIL_IN_USE", "Email is already in use");
        }
        
        // Create new user
        User user = User.builder()
                .username(request.getUsername())
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .role("USER")
                .enabled(true)
                .build();
        
        User savedUser = userRepository.save(user);
        
        // Generate token
        String token = tokenProvider.generateToken(savedUser.getUsername());
        
        return new AuthRequest.AuthResponse(
                token,
                savedUser.getId(),
                savedUser.getUsername(),
                savedUser.getEmail(),
                savedUser.getAvatar()
        );
    }
    
    public AuthRequest.AuthResponse login(AuthRequest.LoginRequest request) {
        try {
            Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                    request.getUsernameOrEmail(),
                    request.getPassword()
                )
            );
            
            SecurityContextHolder.getContext().setAuthentication(authentication);
            
            String token = tokenProvider.generateToken(authentication);
            
            User user = userRepository.findByUsernameOrEmail(
                    request.getUsernameOrEmail(),
                    request.getUsernameOrEmail()
            ).orElseThrow(() -> ApiException.unauthorized("INVALID_CREDENTIALS", "Invalid username/email or password"));
            
            return new AuthRequest.AuthResponse(
                    token,
                    user.getId(),
                    user.getUsername(),
                    user.getEmail(),
                    user.getAvatar()
            );
        } catch (BadCredentialsException e) {
            throw ApiException.unauthorized("INVALID_CREDENTIALS", "Invalid username/email or password");
        }
    }
    
    public AuthRequest.UserProfileResponse getCurrentUser(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> ApiException.notFound("USER_NOT_FOUND", "User not found"));
        
        AuthRequest.UserProfileResponse response = new AuthRequest.UserProfileResponse();
        response.setId(user.getId());
        response.setUsername(user.getUsername());
        response.setEmail(user.getEmail());
        response.setAvatar(user.getAvatar());
        response.setRole(user.getRole());
        return response;
    }
}
