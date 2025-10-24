package de.marcandreher.fusionkit.core.javalin.engine;

import io.javalin.http.Context;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

public class FusionContextWrapper {

    /**
     * Creates a FusionContext instance by wrapping the given Javalin Context.
     * Delegates all calls to the wrapped Context, unless the method has a
     * default implementation defined in FusionContext.
     */
    public static FusionContext create(Context context) {
        return (FusionContext) Proxy.newProxyInstance(
                FusionContext.class.getClassLoader(),
                new Class[]{FusionContext.class},
                new InvocationHandler() {
                    @Override
                    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
                        // Handle default methods defined in FusionContext
                        if (method.isDefault()) {
                            return InvocationHandler.invokeDefault(proxy, method, args);
                        }

                        // Try to find an equivalent method on the wrapped Javalin Context
                        try {
                            Method targetMethod = findMatchingMethod(context.getClass(), method);
                            if (targetMethod != null) {
                                Object result = targetMethod.invoke(context, args);
                                return result;
                            }
                        } catch (NoSuchMethodException e) {
                            // Method not found, will throw below
                        } catch (Exception e) {
                            // Re-throw the actual exception from the invoked method
                            throw e.getCause() != null ? e.getCause() : e;
                        }

                        // Fallback: method only exists in FusionContext (custom)
                        throw new UnsupportedOperationException(
                                "Method not found on Javalin Context: " + method.getName() + 
                                " with parameters: " + java.util.Arrays.toString(method.getParameterTypes())
                        );
                    }
                    
                    /**
                     * Finds a matching method on the target class, including parent classes.
                     * This handles cases where the method might be inherited.
                     */
                    private Method findMatchingMethod(Class<?> targetClass, Method method) throws NoSuchMethodException {
                        try {
                            // First try exact match
                            return targetClass.getMethod(method.getName(), method.getParameterTypes());
                        } catch (NoSuchMethodException e) {
                            // Try to find method with compatible parameters (handles inheritance)
                            for (Method candidate : targetClass.getMethods()) {
                                if (candidate.getName().equals(method.getName()) &&
                                    candidate.getParameterCount() == method.getParameterCount() &&
                                    parametersMatch(method.getParameterTypes(), candidate.getParameterTypes())) {
                                    return candidate;
                                }
                            }
                            throw e;
                        }
                    }
                    
                    /**
                     * Check if parameters are compatible (exact match or assignable)
                     */
                    private boolean parametersMatch(Class<?>[] expected, Class<?>[] actual) {
                        if (expected.length != actual.length) {
                            return false;
                        }
                        for (int i = 0; i < expected.length; i++) {
                            if (!expected[i].equals(actual[i]) && !actual[i].isAssignableFrom(expected[i])) {
                                return false;
                            }
                        }
                        return true;
                    }
                }
        );
    }
}