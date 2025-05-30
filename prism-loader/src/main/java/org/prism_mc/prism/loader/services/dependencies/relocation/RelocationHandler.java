/*
 * This file is part of LuckPerms, licensed under the MIT License.
 * It has been modified for use in Prism.
 *
 *  Copyright (c) lucko (Luck) <luck@lucko.me>
 *  Copyright (c) contributors
 *
 *  Permission is hereby granted, free of charge, to any person obtaining a copy
 *  of this software and associated documentation files (the "Software"), to deal
 *  in the Software without restriction, including without limitation the rights
 *  to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 *  copies of the Software, and to permit persons to whom the Software is
 *  furnished to do so, subject to the following conditions:
 *
 *  The above copyright notice and this permission notice shall be included in all
 *  copies or substantial portions of the Software.
 *
 *  THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 *  IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 *  FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 *  AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 *  LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 *  OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 *  SOFTWARE.
 */

package org.prism_mc.prism.loader.services.dependencies.relocation;

import java.io.File;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.nio.file.Path;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.prism_mc.prism.loader.services.dependencies.Dependency;
import org.prism_mc.prism.loader.services.dependencies.DependencyService;
import org.prism_mc.prism.loader.services.dependencies.loader.IsolatedClassLoader;

/**
 * Handles class runtime relocation of packages in downloaded dependencies.
 */
public class RelocationHandler {

    /**
     * The core dependencies.
     */
    public static final Set<Dependency> DEPENDENCIES = EnumSet.of(
        Dependency.ASM,
        Dependency.ASM_COMMONS,
        Dependency.JAR_RELOCATOR
    );

    /**
     * The jar-relocator package.
     */
    private static final String JAR_RELOCATOR_CLASS = "me.lucko.jarrelocator.JarRelocator";

    /**
     * The jar relocator run method name.
     */
    private static final String JAR_RELOCATOR_RUN_METHOD = "run";

    /**
     * The jar relocator constructor.
     */
    private final Constructor<?> jarRelocatorConstructor;

    /**
     * The jar relocation run method.
     */
    private final Method jarRelocatorRunMethod;

    /**
     * Constructor.
     *
     * @param dependencyService The dependency service
     */
    public RelocationHandler(DependencyService dependencyService) {
        try {
            // Download the required dependencies for remapping
            dependencyService.loadDependencies(DEPENDENCIES);
            // get a classloader containing the required dependencies as sources
            IsolatedClassLoader classLoader = dependencyService.obtainClassLoaderWith(DEPENDENCIES);

            // load the relocator class
            Class<?> jarRelocatorClass = classLoader.loadClass(JAR_RELOCATOR_CLASS);

            // prepare the the reflected constructor & method instances
            this.jarRelocatorConstructor = jarRelocatorClass.getDeclaredConstructor(File.class, File.class, Map.class);
            this.jarRelocatorConstructor.setAccessible(true);

            this.jarRelocatorRunMethod = jarRelocatorClass.getDeclaredMethod(JAR_RELOCATOR_RUN_METHOD);
            this.jarRelocatorRunMethod.setAccessible(true);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Remap a jar.
     *
     * @param input The input path
     * @param output The output path
     * @param relocations The relocations
     * @throws Exception The exception
     */
    public void remap(Path input, Path output, List<Relocation> relocations) throws Exception {
        Map<String, String> mappings = new HashMap<>();
        for (Relocation relocation : relocations) {
            mappings.put(relocation.getPattern(), relocation.getRelocatedPattern());
        }

        // create and invoke a new relocator
        Object relocator = this.jarRelocatorConstructor.newInstance(input.toFile(), output.toFile(), mappings);
        this.jarRelocatorRunMethod.invoke(relocator);
    }
}
