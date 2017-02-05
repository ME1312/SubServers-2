package net.ME1312.SubServers.Host.API;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * SubPlugin Class Annotation<br>
 * Classes defined in <u>package.xml</u> and annotated with this will be loaded
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface SubPlugin {
    /**
     * The Name of this Plugin
     *
     * @return Plugin Name
     */
    String name();

    /**
     * The Version of this Plugin
     *
     * @return Plugin Version
     */
    String version();

    /**
     * The Authors of this Plugin
     *
     * @return Authors List
     */
    String[] authors();

    /**
     * The Description of this Plugin
     *
     * @return Plugin Description
     */
    String description() default "";

    /**
     * The Authors' Website
     *
     * @return Authors' Website
     */
    String website() default "";

    /**
     * Load Before Plugins List
     *
     * @return Load Before List
     */
    String[] loadBefore() default {};

    /**
     * Dependencies List
     *
     * @return Dependencies List
     */
    String[] depend() default {};

    /**
     * Soft Dependencies List
     *
     * @return Soft Dependencies List
     */
    String[] softDepend() default {};
}