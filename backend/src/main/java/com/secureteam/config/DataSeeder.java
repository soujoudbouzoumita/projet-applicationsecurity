package com.secureteam.config;

import com.secureteam.auth.PasswordService;
import com.secureteam.model.User;
import jakarta.annotation.PostConstruct;
import jakarta.ejb.Singleton;
import jakarta.ejb.Startup;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.jboss.logging.Logger;

@Singleton
@Startup
public class DataSeeder {

    private static final Logger LOG = Logger.getLogger(DataSeeder.class);

    @PersistenceContext(unitName = "secureteamPU")
    private EntityManager em;

    @Inject
    private PasswordService passwordService;

    @PostConstruct
    public void init() {
        LOG.info("Checking for default admin user...");
        try {
            Long count = em.createQuery("SELECT COUNT(u) FROM User u WHERE u.username = 'admin'", Long.class)
                    .getSingleResult();

            if (count == 0) {
                LOG.info("Admin user not found. Creating default admin...");
                User admin = new User();
                admin.setUsername("admin");
                admin.setDepartment("security");
                admin.setMfaEnabled(false); // Can setup MFA later

                PasswordService.HashResult hash = passwordService.hash("SecureAdmin123!");
                admin.setPasswordHash(hash.hashBase64);
                admin.setPasswordSalt(hash.saltBase64);

                em.persist(admin);
                LOG.info("Default admin created: admin / SecureAdmin123!");
            } else {
                LOG.info("Admin user already exists.");
            }
        } catch (Exception e) {
            LOG.error("Failed to seed admin user", e);
        }
    }
}
