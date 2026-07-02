package com.minispring.scanner;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;

/**
 * 类路径扫描器
 * 扫描指定包下的所有类
 */
public class ClassPathScanner {

    private final String basePackage;

    public ClassPathScanner(String basePackage) {
        this.basePackage = basePackage;
    }

    /**
     * 扫描指定包下的所有类
     */
    public List<Class<?>> scan() {
        List<Class<?>> classes = new ArrayList<>();

        try {
            String packagePath = basePackage.replace('.', '/');
            Enumeration<URL> resources = getClass().getClassLoader().getResources(packagePath);

            while (resources.hasMoreElements()) {
                URL url = resources.nextElement();
                if (url.getProtocol().equals("file")) {
                    findClasses(new File(url.getFile()), basePackage, classes);
                }
            }
        } catch (IOException e) {
            throw new RuntimeException("Failed to scan package: " + basePackage, e);
        }

        return classes;
    }

    /**
     * 递归查找类文件
     */
    private void findClasses(File directory, String packageName, List<Class<?>> classes) {
        File[] files = directory.listFiles();
        if (files == null) {
            return;
        }

        for (File file : files) {
            if (file.isDirectory()) {
                findClasses(file, packageName + "." + file.getName(), classes);
            } else if (file.getName().endsWith(".class")) {
                try {
                    String className = packageName + '.' + file.getName().substring(0, file.getName().length() - 6);
                    classes.add(Class.forName(className));
                } catch (ClassNotFoundException e) {
                    // Skip class
                }
            }
        }
    }
}
