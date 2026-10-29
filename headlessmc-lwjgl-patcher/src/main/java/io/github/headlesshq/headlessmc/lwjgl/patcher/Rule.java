package io.github.headlesshq.headlessmc.lwjgl.patcher;

/**
 * Decides the {@link Behaviour} of a family of methods.
 */
public interface Rule {
    /**
     * @return a stable name, written to the lock file.
     */
    String name();

    /**
     * @return {@code true} if every method resolved by this rule is listed individually in the lock file,
     * because the behaviour is a guess that should be reviewed per method.
     */
    default boolean listed() {
        return false;
    }

    /**
     * @param method the method to patch.
     * @return the behaviour of the method, or {@code null} if this rule does not apply.
     */
    Behaviour apply(MethodInfo method);

}
