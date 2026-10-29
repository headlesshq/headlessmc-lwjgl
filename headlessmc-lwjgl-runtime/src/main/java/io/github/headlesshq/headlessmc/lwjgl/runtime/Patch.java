package io.github.headlesshq.headlessmc.lwjgl.runtime;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a static method as the implementation of LWJGL methods.
 * The patcher replaces the body of every matching LWJGL method with a call to the annotated method.
 * <p>The annotated method must have the same parameters and return type as the patched method.
 * If the patched method is an instance method, the annotated method takes the instance as an additional first
 * parameter, typed as the declaring class of the patched method or any of its supertypes.</p>
 */
@Retention(RetentionPolicy.CLASS)
@Target(ElementType.METHOD)
public @interface Patch {
    /**
     * @return the internal names of the classes declaring the patched method, e.g. {@code org/lwjgl/opengl/GL11C}.
     */
    String[] owner();

    /**
     * @return the name of the patched method, defaults to the name of the annotated method.
     */
    String name() default "";

}
