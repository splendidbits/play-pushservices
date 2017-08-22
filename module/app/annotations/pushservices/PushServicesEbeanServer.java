package annotations.pushservices;

import com.google.inject.BindingAnnotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * GNU General Public License v3.0.
 * (This means you can use it as you wish, host and share modifications.)
 * Copyright 5/26/16 Splendid Bits.
 */
@BindingAnnotation
@Target({ElementType.FIELD, ElementType.PARAMETER, ElementType.METHOD, ElementType.CONSTRUCTOR }) @Retention(RetentionPolicy.RUNTIME)
public @interface PushServicesEbeanServer {}

