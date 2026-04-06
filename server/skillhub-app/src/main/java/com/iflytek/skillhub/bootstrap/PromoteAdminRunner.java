package com.iflytek.skillhub.bootstrap;

import com.iflytek.skillhub.auth.entity.Role;
import com.iflytek.skillhub.auth.entity.UserRoleBinding;
import com.iflytek.skillhub.auth.repository.RoleRepository;
import com.iflytek.skillhub.auth.repository.UserRoleBindingRepository;
import com.iflytek.skillhub.domain.namespace.Namespace;
import com.iflytek.skillhub.domain.namespace.NamespaceMember;
import com.iflytek.skillhub.domain.namespace.NamespaceMemberRepository;
import com.iflytek.skillhub.domain.namespace.NamespaceRepository;
import com.iflytek.skillhub.domain.namespace.NamespaceRole;
import com.iflytek.skillhub.domain.user.UserAccount;
import com.iflytek.skillhub.domain.user.UserAccountRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * One-time CLI runner that promotes an existing user to SUPER_ADMIN.
 * Only activated when {@code skillhub.admin.promote.target-user-id} is set.
 *
 * Usage:
 *   java -jar skillhub-app.jar \
 *     --skillhub.admin.promote.target-user-id=ou_xxxxx
 */
@Component
@ConditionalOnProperty("skillhub.admin.promote.target-user-id")
public class PromoteAdminRunner implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(PromoteAdminRunner.class);

    private final ConfigurableApplicationContext context;
    private final TransactionTemplate txTemplate;
    private final UserAccountRepository userAccountRepository;
    private final RoleRepository roleRepository;
    private final UserRoleBindingRepository userRoleBindingRepository;
    private final NamespaceRepository namespaceRepository;
    private final NamespaceMemberRepository namespaceMemberRepository;
    private final String targetUserId;

    public PromoteAdminRunner(ConfigurableApplicationContext context,
                              TransactionTemplate txTemplate,
                              UserAccountRepository userAccountRepository,
                              RoleRepository roleRepository,
                              UserRoleBindingRepository userRoleBindingRepository,
                              NamespaceRepository namespaceRepository,
                              NamespaceMemberRepository namespaceMemberRepository,
                              org.springframework.core.env.Environment env) {
        this.context = context;
        this.txTemplate = txTemplate;
        this.userAccountRepository = userAccountRepository;
        this.roleRepository = roleRepository;
        this.userRoleBindingRepository = userRoleBindingRepository;
        this.namespaceRepository = namespaceRepository;
        this.namespaceMemberRepository = namespaceMemberRepository;
        this.targetUserId = env.getRequiredProperty("skillhub.admin.promote.target-user-id");
    }

    @Override
    public void run(ApplicationArguments args) {
        log.info("Promote-admin runner started for target user: {}", targetUserId);

        Integer exitCode = txTemplate.execute(status -> {
            UserAccount user = userAccountRepository.findById(targetUserId).orElse(null);
            if (user == null) {
                log.error("User not found: {}", targetUserId);
                return 1;
            }

            boolean hasRole = userRoleBindingRepository.findByUserId(user.getId()).stream()
                    .anyMatch(b -> b.getRole().getCode().equals("SUPER_ADMIN"));
            if (hasRole) {
                log.info("User {} ({}) is already SUPER_ADMIN, nothing to do", user.getId(), user.getDisplayName());
                return 0;
            }

            Role superAdmin = roleRepository.findByCode("SUPER_ADMIN")
                    .orElseThrow(() -> new IllegalStateException("Missing built-in role: SUPER_ADMIN"));
            userRoleBindingRepository.save(new UserRoleBinding(user.getId(), superAdmin));
            log.info("Assigned SUPER_ADMIN role to user {} ({})", user.getId(), user.getDisplayName());

            Namespace globalNs = namespaceRepository.findBySlug("global")
                    .orElseThrow(() -> new IllegalStateException("Missing built-in global namespace"));
            if (namespaceMemberRepository.findByNamespaceIdAndUserId(globalNs.getId(), user.getId()).isEmpty()) {
                namespaceMemberRepository.save(new NamespaceMember(globalNs.getId(), user.getId(), NamespaceRole.OWNER));
                log.info("Added user to global namespace as OWNER");
            }

            log.info("Promote-admin completed successfully");
            return 0;
        });

        SpringApplication.exit(context, () -> exitCode != null ? exitCode : 1);
    }
}
