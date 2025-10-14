package de.marcandreher.fusionkit.core.javalin.engine;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Proxy;

import io.javalin.http.Context;

public class FusionContextWrapper {

    /**
     * Creates a FusionContext instance by wrapping the given Context.
     * Uses dynamic proxy to delegate all method calls to the wrapped context,
     * except for methods overridden by the FusionContext interface (like ip()).
     */
    public static FusionContext create(Context context) {
        return (FusionContext) Proxy.newProxyInstance(
                FusionContext.class.getClassLoader(),
                new Class<?>[] { FusionContext.class },
                (proxy, method, args) -> {
                    // Check if the method has a default implementation in FusionContext
                    if (method.isDefault()) {
                        // Use the FusionContext default implementation
                        return InvocationHandler.invokeDefault(proxy, method, args);
                    }
                    
                    // Delegate method calls to the wrapped context
                    return method.invoke(context, args);
                }
        );
    }


}