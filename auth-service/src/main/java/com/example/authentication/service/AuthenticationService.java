package com.example.authentication.service;

import com.example.authentication.Dtos.AuthenticationResponse;
import com.example.authentication.Dtos.SignInRequest;
import com.example.authentication.Dtos.SignUpRequest;
import com.example.authentication.Dtos.UserResponse;
import com.example.authentication.entities.UserEntity;
import com.example.authentication.repositories.UserRepository;
import com.example.authentication.security.JwtGenerator;
import org.modelmapper.ModelMapper;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;

@Service
public class AuthenticationService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtGenerator jwtGenerator;
    private final ModelMapper modelMapper;

    public AuthenticationService(UserRepository userRepository, PasswordEncoder passwordEncoder, AuthenticationManager authenticationManager, JwtGenerator jwtGenerator, ModelMapper modelMapper) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.authenticationManager = authenticationManager;
        this.jwtGenerator = jwtGenerator;
        this.modelMapper = modelMapper;
    }

    public AuthenticationResponse signUp(SignUpRequest signUpRequest){
        if(userRepository.existsByEmail(signUpRequest.getEmail())){
            throw new IllegalArgumentException("User with this email already exists");
        }

        UserEntity userEntity=modelMapper.map(signUpRequest,UserEntity.class);
        userEntity.setPassword(passwordEncoder.encode(signUpRequest.getPassword()));
        userRepository.save(userEntity);
        UserResponse userResponse=modelMapper.map(userEntity,UserResponse.class);

        return AuthenticationResponse.builder().user(userResponse).build();

    }


    public AuthenticationResponse signIn(SignInRequest signInRequest){
        UserEntity userFound=userRepository.findByEmail(signInRequest.getEmail()).orElseThrow(()->new IllegalArgumentException("User not found"));
        if(!userFound.isEnabled()){
            throw new HttpClientErrorException(HttpStatus.BAD_REQUEST,"User is not enabled");
        }
        Authentication authentication=authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(signInRequest.getEmail(),signInRequest.getPassword()));
        SecurityContextHolder.getContext().setAuthentication(authentication);
        String token=jwtGenerator.generateToken(authentication);
        UserResponse userResponse=modelMapper.map(userFound,UserResponse.class);
        return AuthenticationResponse.builder().token(token).user(userResponse).build();
    }
}
