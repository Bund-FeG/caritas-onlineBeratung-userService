package de.caritas.cob.UserService.api.service.helper.aspect;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Custom annotation for {@link KeycloakAdminClientLogoutAspect}
 *
 */

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface KeycloakAdminClientLogout {

}
