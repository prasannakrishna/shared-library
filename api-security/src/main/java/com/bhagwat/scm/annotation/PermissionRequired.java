package com.bhagwat.scm.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Fine-grained permission check. Validates that the user's role permissions
 * map (from JWT or fetched from userService) contains the specified permission key.
 *
 * Usage:
 *   PermissionRequired("inventory.write")
 *   PermissionRequired(value = "shipment.cancel", failMessage = "Cannot cancel shipments")
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface PermissionRequired {
    /** Permission key to check, e.g. "inventory.write", "order.approve" */
    String value();

    /** Custom denial message */
    String failMessage() default "Access denied: insufficient permission";
}
