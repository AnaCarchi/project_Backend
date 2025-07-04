package com.tienda.ropa.service;

import com.tienda.ropa.model.Role;
import com.tienda.ropa.model.User;
import com.tienda.ropa.model.UserProfile;
import com.tienda.ropa.repository.RoleRepository;
import com.tienda.ropa.repository.UserRepository;
import com.tienda.ropa.dto.UserDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserService implements UserDetailsService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("Usuario no encontrado: " + username));

        if (!user.getEnabled()) {
            throw new UsernameNotFoundException("Usuario deshabilitado: " + username);
        }

        if (user.getLocked()) {
            throw new UsernameNotFoundException("Usuario bloqueado: " + username);
        }

        Set<GrantedAuthority> authorities = user.getRoles().stream()
                .map(role -> new SimpleGrantedAuthority(role.getName().name()))
                .collect(Collectors.toSet());

        return org.springframework.security.core.userdetails.User.builder()
                .username(user.getUsername())
                .password(user.getPassword())
                .authorities(authorities)
                .accountExpired(false)
                .accountLocked(user.getLocked())
                .credentialsExpired(false)
                .disabled(!user.getEnabled())
                .build();
    }

    @Transactional
    public User createUser(String username, String email, String password, Role.RoleName roleName) {
        if (userRepository.existsByUsername(username)) {
            throw new RuntimeException("El nombre de usuario ya existe");
        }

        if (userRepository.existsByEmail(email)) {
            throw new RuntimeException("El email ya está registrado");
        }

        User user = new User();
        user.setUsername(username);
        user.setEmail(email);
        user.setPassword(passwordEncoder.encode(password));
        user.setEnabled(true);
        user.setLocked(false);

        Role role = roleRepository.findByName(roleName)
                .orElseThrow(() -> new RuntimeException("Rol no encontrado: " + roleName));

        Set<Role> roles = new HashSet<>();
        roles.add(role);
        user.setRoles(roles);

        // Crear perfil de usuario (relación 1:1)
        UserProfile userProfile = new UserProfile();
        userProfile.setUser(user);
        user.setUserProfile(userProfile);

        User savedUser = userRepository.save(user);
        log.info("Usuario creado: {} con rol: {}", username, roleName);
        
        return savedUser;
    }

    @Transactional(readOnly = true)
    public List<User> getAllUsers() {
        return userRepository.findAll();
    }

    @Transactional(readOnly = true)
    public Optional<User> getUserById(Long id) {
        return userRepository.findById(id);
    }

    @Transactional(readOnly = true)
    public Optional<User> getUserByUsername(String username) {
        return userRepository.findByUsername(username);
    }

    @Transactional
    public User updateUser(Long id, UserDto userDto) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

        if (userDto.getEmail() != null && !userDto.getEmail().equals(user.getEmail())) {
            if (userRepository.existsByEmail(userDto.getEmail())) {
                throw new RuntimeException("El email ya está registrado");
            }
            user.setEmail(userDto.getEmail());
        }

        user.setEnabled(userDto.getEnabled());
        user.setLocked(userDto.getLocked());

        return userRepository.save(user);
    }

    @Transactional
    public void changePassword(Long userId, String currentPassword, String newPassword) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

        if (!passwordEncoder.matches(currentPassword, user.getPassword())) {
            throw new RuntimeException("Contraseña actual incorrecta");
        }

        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);
        log.info("Contraseña cambiada para usuario: {}", user.getUsername());
    }

    @Transactional
    public void toggleUserLock(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

        user.setLocked(!user.getLocked());
        userRepository.save(user);
        log.info("Usuario {} {}", user.getUsername(), user.getLocked() ? "bloqueado" : "desbloqueado");
    }

    @Transactional
    public void deleteUser(Long id) {
        if (!userRepository.existsById(id)) {
            throw new RuntimeException("Usuario no encontrado");
        }
        userRepository.deleteById(id);
        log.info("Usuario con ID {} eliminado", id);
    }

    @Transactional(readOnly = true)
    public List<User> getUsersByRole(Role.RoleName roleName) {
        return roleName == Role.RoleName.ROLE_ADMIN ? 
                userRepository.findAllAdmins() : 
                userRepository.findAllRegularUsers();
    }

    @Transactional(readOnly = true)
    public Long countUsersByRole(Role.RoleName roleName) {
        return userRepository.countByRole(roleName.name());
    }
}
