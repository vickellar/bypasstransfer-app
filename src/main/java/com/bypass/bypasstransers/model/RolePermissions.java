
package com.bypass.bypasstransers.model;

import com.bypass.bypasstransers.enums.Permission;
import java.util.Set;
import com.bypass.bypasstransers.enums.Role;

public class RolePermissions {

    public static Set<Permission> permissions(Role role) {

        return switch (role) {
            case SUPER_ADMIN -> Set.of(Permission.values());
            case ADMIN -> Set.of(
                Permission.CREATE_TRANSACTION,
                Permission.APPROVE_TRANSACTION,
                Permission.VIEW_REPORTS,
                Permission.EXPORT_DATA
            );
            case SUPERVISOR -> Set.of(
                Permission.APPROVE_TRANSACTION,
                Permission.VIEW_REPORTS
            );
            case STAFF -> Set.of(
                Permission.CREATE_TRANSACTION
            );
        };
    }
}

