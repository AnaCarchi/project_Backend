package com.tienda.ropa.config;

import com.tienda.ropa.model.Role;
import com.tienda.ropa.repository.RoleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
@Slf4j
public class DataInitializer implements CommandLineRunner {

    private final RoleRepository roleRepository;

    @Override
    @Transactional
    public void run(String... args) throws Exception {
        try {
            loadRoles();
            log.info("Inicialización completada - Roles creados. Administradores se crean vía registro con código.");
        } catch (Exception e) {
            log.error("Error durante la inicialización de datos: {}", e.getMessage());
        }
    }

    private void loadRoles() {
        try {
            if (roleRepository.count() == 0) {
                Role userRole = new Role();
                userRole.setName(Role.RoleName.ROLE_USER);
                roleRepository.save(userRole);

                Role adminRole = new Role();
                adminRole.setName(Role.RoleName.ROLE_ADMIN);
                roleRepository.save(adminRole);

                log.info("Roles iniciales creados: ROLE_USER, ROLE_ADMIN");
            } else {
                log.info("Roles ya existen en la base de datos");
            }
        } catch (Exception e) {
            log.error("Error creando roles: {}", e.getMessage());
        }
    }

    /*
    // MÉTODO COMENTADO - Para crear admin por defecto si es necesario
    private void createDefaultAdmin() {
        try {
            if (userService.getUserByUsername("admin").isEmpty()) {
                userService.createUser(
                    "admin",
                    "admin@tiendaropa.com",
                    "admin123",
                    Role.RoleName.ROLE_ADMIN
                );
                log.info("Usuario administrador por defecto creado: admin/admin123");
            } else {
                log.info("Usuario administrador ya existe");
            }
        } catch (Exception e) {
            log.warn("No se pudo crear el usuario administrador por defecto: {}", e.getMessage());
        }
    }
    */
}