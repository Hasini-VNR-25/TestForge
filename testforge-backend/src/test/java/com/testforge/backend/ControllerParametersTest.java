package com.testforge.backend;

import org.junit.jupiter.api.Test;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ControllerParametersTest {

    @Test
    void testAllPathVariablesAndRequestParamsHaveExplicitNames() {
        List<Class<?>> controllers = List.of(
                com.testforge.backend.controller.DashboardController.class,
                com.testforge.backend.controller.DatabaseExplorerController.class,
                com.testforge.backend.controller.DatabaseTestController.class,
                com.testforge.backend.controller.HealthController.class,
                com.testforge.backend.controller.ProblemController.class,
                com.testforge.backend.controller.TestCaseController.class,
                com.testforge.backend.controller.TestGenerationController.class
        );

        int checkedParams = 0;
        for (Class<?> clazz : controllers) {
            for (Method method : clazz.getDeclaredMethods()) {
                for (Parameter param : method.getParameters()) {
                    // Check if compiler preserved parameter names
                    assertTrue(param.isNamePresent(),
                            "Parameter names must be present via reflection in " + clazz.getSimpleName() + "." + method.getName());

                    PathVariable pv = param.getAnnotation(PathVariable.class);
                    if (pv != null) {
                        String name = pv.value().isEmpty() ? pv.name() : pv.value();
                        assertFalse(name.trim().isEmpty(),
                                "Method " + method.getName() + " in " + clazz.getSimpleName()
                                        + " has @PathVariable without explicit name on parameter " + param.getName());
                        checkedParams++;
                    }

                    RequestParam rp = param.getAnnotation(RequestParam.class);
                    if (rp != null) {
                        String name = rp.value().isEmpty() ? rp.name() : rp.value();
                        assertFalse(name.trim().isEmpty(),
                                "Method " + method.getName() + " in " + clazz.getSimpleName()
                                        + " has @RequestParam without explicit name on parameter " + param.getName());
                        checkedParams++;
                    }
                }
            }
        }
        assertTrue(checkedParams > 0, "Should have checked at least one annotated parameter");
    }
}
